package com.example.data.ai

import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.FirestoreOrderRecord
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StoreProfile
import com.example.data.model.UserAccount
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

/**
 * Customer-scoped Trilingual Smart Helper powered by Firebase AI Logic (`Firebase.ai`)
 * using `gemini-3.8-flash` and App Check attestation (zero client-side API keys).
 */
class SmartHelperAiService {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.45f
                maxOutputTokens = 700
            },
            systemInstruction = content {
                text(SYSTEM_INSTRUCTION)
            }
        )
    }

    sealed interface StreamChunk {
        data class Partial(val accumulatedText: String) : StreamChunk
        data class Completed(
            val cleanText: String,
            val recommendedSkuIds: List<String>,
            val detectedLanguage: AppLanguage
        ) : StreamChunk
    }

    fun streamReply(
        userPrompt: String,
        preferredLanguage: AppLanguage,
        historyPairs: List<Pair<String, String>>,
        allProducts: List<Product>,
        offersConfig: StoreOffersConfig,
        categoryDiscounts: Map<String, Double>,
        userAccount: UserAccount?,
        localOrders: List<Order>,
        cloudOrders: List<FirestoreOrderRecord>
    ): Flow<StreamChunk> = flow {
        val detectedInputLang = AppLanguage.detectFromText(userPrompt, preferredLanguage)
        val matchedCatalog = findRelevantProducts(userPrompt, allProducts, limit = 14)
        val contextBlock = buildCustomerContextPrompt(
            userPrompt = userPrompt,
            targetLanguage = detectedInputLang,
            matchedProducts = matchedCatalog,
            offersConfig = offersConfig,
            categoryDiscounts = categoryDiscounts,
            userAccount = userAccount,
            localOrders = localOrders,
            cloudOrders = cloudOrders
        )

        val recentHistoryContents: List<Content> = historyPairs
            .takeLast(6)
            .map { (sender, msg) ->
                val role = if (sender == "USER") "user" else "model"
                content(role = role) { text(msg.take(450)) }
            }

        val requestContents = recentHistoryContents + listOf(
            content(role = "user") { text(contextBlock) }
        )

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                model.generateContentStream(requestContents).collect { response ->
                    val piece = response.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        val (cleanPartial, _) = parseRecommendedSkuIds(accumulated.toString())
                        emit(StreamChunk.Partial(cleanPartial))
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Smart Helper Gemini generation timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Smart Helper Gemini generation failed", e)
            throw e
        }

        val fullRaw = accumulated.toString().trim()
        if (fullRaw.isBlank()) {
            throw IllegalStateException("Smart Helper returned an empty response.")
        }
        val (cleanFinal, parsedSkus) = parseRecommendedSkuIds(fullRaw)
        val replyLang = AppLanguage.detectTtsLanguage(cleanFinal, detectedInputLang)
        emit(
            StreamChunk.Completed(
                cleanText = cleanFinal,
                recommendedSkuIds = parsedSkus,
                detectedLanguage = replyLang
            )
        )
    }

    companion object {
        private const val TAG = "SmartHelperAiService"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val AI_TIMEOUT_MS = 60_000L

        internal val SYSTEM_INSTRUCTION = """
            You are "NavKart Smart Helper", the friendly public customer shopping & support assistant for Nav Grocery Store Navkart (${StoreProfile.LOCATION}).

            STRICT TRILINGUAL RULE:
            - Automatically detect the customer's language and respond naturally in that exact script/language:
              1. Hindi in Devanagari script (हिंदी), e.g., when the customer writes or speaks Hindi / Hinglish or selects Hindi.
              2. Punjabi in Gurmukhi script (ਪੰਜਾਬੀ), e.g., when the customer writes or speaks Punjabi or selects Punjabi.
              3. English, when the customer writes or speaks English or selects English.
            - Keep answers concise, warm, accurate, and easy to read on a mobile screen.

            STRICT CUSTOMER-ONLY SCOPE & SECURITY GUARDRAILS:
            - Only assist the customer with:
              1. Browsing A-to-Z products in the live store catalog (Packaged Groceries, Cold-Chain Frozen Foods, Atta & Dals, Desi Ghee & Oils, Spices, Cosmetics & Personal Care, Soaps & Household Cleaning, Electronics & Mobiles, Clothing & Apparel, Footwear & Shoes, Home & Kitchen).
              2. Explaining current live Firebase-synced discounts, active offers, Peti carton deals, and Nav Coins earn/redemption balance & rules.
              3. Checking ONLY the currently signed-in customer's own orders listed in CUSTOMER_OWN_ORDERS.
              4. Store timings (${StoreProfile.STORE_TIMINGS}), delivery zones (Sandhaur 148020 15-min Express, ~2 hr nearby villages, 30-min Main Bazaar pickup), return/replacement policy (7-day replacement for Electronics/Clothing/Footwear; immediate replacement for damaged sealed groceries), and public helpline (${StoreProfile.HELPLINE_PHONE}).
            - Nav Grocery Store Navkart NEVER sells fresh fruits or fresh vegetables (only 100% sealed packaged & cold-chain frozen goods and retail marketplace items). Mention this politely if asked.
            - NEVER mention, reveal, or invent any admin panel, POS billing tools, Digital Khata, Navo Hub, internal staff workflows, API keys, secrets, tokens, or other customers' information. If asked about admin/POS/internal tools, politely refuse and offer customer shopping assistance instead.
            - NEVER invent prices, fake products, or fake order IDs. Rely strictly on the LIVE_STORE_CONTEXT provided in each message.
            - When recommending specific products from MATCHED_CATALOG, append `[SKU:ID1,ID2]` at the very end of your response using only exact IDs from MATCHED_CATALOG (maximum 4 SKUs). Do not put anything after `[SKU:...]`.
        """.trimIndent()

        private val TRILINGUAL_SYNONYMS: Map<String, List<String>> = mapOf(
            "आटा" to listOf("atta", "flour", "chakki", "aashirvaad"),
            "घी" to listOf("ghee", "desi ghee", "verka", "amul", "butter"),
            "तेल" to listOf("oil", "mustard", "sunflower", "fortune"),
            "दाल" to listOf("dal", "toor", "moong", "chana", "lentil", "rajma"),
            "चावल" to listOf("rice", "basmati", "pulao"),
            "मसाला" to listOf("masala", "spice", "mdh", "everest", "haldi", "mirch", "salt", "namak"),
            "चाय" to listOf("tea", "chai", "tata", "coffee"),
            "बिस्कुट" to listOf("biscuit", "cookies", "britannia", "parle", "snacks", "noodles"),
            "साबुन" to listOf("soap", "detergent", "surf", "ariel", "rin", "harpic", "lizol", "cleaner", "household"),
            "क्रीम" to listOf("cream", "lotion", "face", "cosmetic", "personal care", "shampoo", "toothpaste"),
            "कपड़े" to listOf("suit", "kurta", "kurti", "shirt", "jeans", "clothing", "apparel", "fashion"),
            "सूट" to listOf("suit", "kurti", "punjabi", "clothing"),
            "जूते" to listOf("shoe", "shoes", "footwear", "sneaker", "puma", "bata", "sandal"),
            "मोबाइल" to listOf("phone", "smartphone", "5g", "mobile", "earbuds", "headphone", "watch", "electronics", "charger"),
            "फोन" to listOf("phone", "smartphone", "5g", "mobile", "electronics"),
            "फ्रोज़न" to listOf("frozen", "mccain", "safal", "peas", "paneer", "cold"),
            "पनीर" to listOf("paneer", "butter", "dairy"),
            "आटा" to listOf("atta", "chakki", "flour"),
            "ਆਟਾ" to listOf("atta", "flour", "chakki"),
            "ਘਿਓ" to listOf("ghee", "butter", "amul", "verka"),
            "ਕੱਪੜੇ" to listOf("suit", "kurta", "shirt", "clothing"),
            "ਜੁੱਤੀ" to listOf("shoe", "footwear", "puma", "bata"),
            "ਮੋਬਾਈਲ" to listOf("phone", "5g", "mobile", "electronics"),
            "ਫ਼ੋਨ" to listOf("phone", "5g", "mobile", "electronics"),
            "ਫ੍ਰੋਜ਼ਨ" to listOf("frozen", "mccain", "safal", "paneer"),
            "ਸਾਬਣ" to listOf("soap", "detergent", "surf", "household")
        )

        fun findRelevantProducts(
            prompt: String,
            allProducts: List<Product>,
            limit: Int = 14
        ): List<Product> {
            if (allProducts.isEmpty()) return emptyList()
            val normalized = prompt.lowercase()
            val expandedTokens = mutableSetOf<String>()

            normalized.split(Regex("[\\s,?!।.|]+"))
                .map { it.trim() }
                .filter { it.length >= 2 }
                .forEach { expandedTokens.add(it) }

            TRILINGUAL_SYNONYMS.forEach { (nativeWord, englishTerms) ->
                if (normalized.contains(nativeWord)) {
                    expandedTokens.addAll(englishTerms)
                }
            }

            if (expandedTokens.isEmpty()) {
                return allProducts.take(limit)
            }

            val scored = allProducts.map { prod ->
                var score = 0
                val haystack = "${prod.id} ${prod.name} ${prod.brand} ${prod.category} ${prod.department} ${prod.description}".lowercase()
                for (tok in expandedTokens) {
                    if (prod.name.contains(tok, ignoreCase = true)) score += 5
                    else if (prod.category.contains(tok, ignoreCase = true) || prod.department.contains(tok, ignoreCase = true)) score += 4
                    else if (prod.brand.contains(tok, ignoreCase = true)) score += 3
                    else if (haystack.contains(tok)) score += 2
                }
                prod to score
            }.filter { it.second > 0 }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(limit)

            return scored.ifEmpty { allProducts.take(minOf(limit, 8)) }
        }

        fun buildCustomerContextPrompt(
            userPrompt: String,
            targetLanguage: AppLanguage,
            matchedProducts: List<Product>,
            offersConfig: StoreOffersConfig,
            categoryDiscounts: Map<String, Double>,
            userAccount: UserAccount?,
            localOrders: List<Order>,
            cloudOrders: List<FirestoreOrderRecord>
        ): String = buildString {
            appendLine("CUSTOMER_MESSAGE: $userPrompt")
            appendLine("TARGET_LANGUAGE_HINT: ${targetLanguage.name} (${targetLanguage.chipLabel} / ${targetLanguage.speechLocaleTag})")
            appendLine()
            appendLine("LIVE_STORE_OFFERS:")
            appendLine("- Storewide Discount: ${offersConfig.discountLabelPercent}% OFF MRP")
            appendLine("- Active Promo Banner: ${if (offersConfig.isOfferActive) offersConfig.offerBannerText else "None"}")
            appendLine("- Bulk Peti Extra Discount: ${offersConfig.petiLabelPercent}%")
            appendLine(
                "- Nav Coins Config: enabled=${offersConfig.coinConfig.isCoinsEnabled}, " +
                    "earn=${StoreOffersConfig.formatPercent(offersConfig.coinConfig.earnRatePercent)}%, " +
                    "1_coin_value=₹${StoreOffersConfig.formatPercent(offersConfig.coinConfig.coinValueInr)}, " +
                    "max_redeem_cap=${StoreOffersConfig.formatPercent(offersConfig.coinConfig.maxRedeemPercent)}%_per_order"
            )
            appendLine("- Customer Coin Balance: ${userAccount?.navCoins ?: 0L} Nav Coins | Signed In: ${userAccount != null}")
            appendLine()
            appendLine("CUSTOMER_OWN_ORDERS:")
            if (cloudOrders.isEmpty() && localOrders.isEmpty()) {
                appendLine("- No orders found for this customer session.")
            } else {
                cloudOrders.take(8).forEach { ord ->
                    appendLine("- ${ord.orderId}: status=${ord.status}, total=₹${ord.finalTotal}, tier=${ord.deliveryTier}, coinsEarned=${ord.coinsEarned}")
                }
                localOrders.filter { l -> cloudOrders.none { it.orderId == l.orderId } }.take(6).forEach { ord ->
                    appendLine("- ${ord.orderId}: status=${ord.status}, total=₹${ord.finalTotal}, tier=${ord.deliveryType}")
                }
            }
            appendLine()
            appendLine("MATCHED_CATALOG (${matchedProducts.size} items):")
            matchedProducts.forEach { p ->
                val effDisc = p.effectiveDiscountPercent(categoryDiscounts[p.category], offersConfig)
                val price = p.sellingPrice(effDisc)
                appendLine(
                    "- ID=${p.id} | ${p.name} | Brand=${p.brand} | Cat=${p.category} | Price=₹$price (MRP ₹${p.mrp}, ${StoreOffersConfig.formatPercent(effDisc)}% OFF) | Seller=${p.sellerName} | Stock=${p.stock}"
                )
            }
        }

        private val SKU_TAG_REGEX = Regex("\\[SKU\\s*:([^\\]]+)\\]", RegexOption.IGNORE_CASE)

        fun parseRecommendedSkuIds(rawText: String): Pair<String, List<String>> {
            val matches = SKU_TAG_REGEX.findAll(rawText).toList()
            if (matches.isEmpty()) return rawText.trim() to emptyList()
            val skuIds = matches
                .flatMap { m -> m.groupValues[1].split(",", " ", ";") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(4)
            val cleaned = SKU_TAG_REGEX.replace(rawText, "").trim()
            return cleaned to skuIds
        }

        fun sanitizeForTts(
            displayText: String,
            language: AppLanguage = AppLanguage.ENGLISH
        ): String {
            val rupeeWord = when (language) {
                AppLanguage.HINDI -> " रुपये "
                AppLanguage.PUNJABI -> " ਰੁਪਏ "
                AppLanguage.ENGLISH -> " rupees "
            }
            return displayText
                .replace(SKU_TAG_REGEX, "")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace(Regex("^[#*•-]+\\s*", RegexOption.MULTILINE), "")
                .replace("₹", rupeeWord)
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
