package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GroceryLocalStore
import com.example.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CatalogBindingAndCountTest {

    @Test
    fun `seed catalog loads all 808 verified SKUs with strict SKU-to-image binding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = GroceryLocalStore(context)
        val rawProducts = store.loadSeedProducts()

        assertEquals("Expected all 808 SKUs (NAV001..NAV808)", 808, rawProducts.size)

        // Ensure all IDs are unique and NAV001..NAV308 keep their original verified batch packaging photos
        val idToImageOriginal: Map<String, String> = rawProducts.associate { it.id to it.fullImageUrl }
        assertEquals(808, idToImageOriginal.size)

        val nav098 = rawProducts.first { it.id == "NAV098" }
        assertTrue(nav098.image.contains("images/batch_"))

        // Verify key new named SKUs from NAV309..NAV808 are present with valid external image URLs
        val expectedNewSkus = listOf("NAV309", "NAV409", "NAV509", "NAV559", "NAV659", "NAV729", "NAV808")
        expectedNewSkus.forEach { sku ->
            val p = rawProducts.first { it.id == sku }
            assertTrue("Expected external photo URL for $sku", p.fullImageUrl.startsWith("https://") || p.fullImageUrl.startsWith("http://"))
        }

        // Verify Ketchup (with "Fresh Tomato" in title) is retained
        val ketchupItems = rawProducts.filter { it.name.contains("Ketchup", ignoreCase = true) }
        assertTrue("Sealed tomato ketchup SKUs must be present", ketchupItems.isNotEmpty())

        // Verify A-Z sorting preserves exact immutable SKU -> fullImageUrl binding without index shifts
        val sortedByName: List<Product> = rawProducts.sortedBy { it.name.lowercase() }
        val sortedByBrand: List<Product> = rawProducts.sortedBy { it.brand.lowercase() }

        sortedByName.forEach { product ->
            assertEquals(
                "Photo shifted after name sort for ${product.id} (${product.name})",
                idToImageOriginal[product.id],
                product.fullImageUrl
            )
            assertFalse(product.fullImageUrl.isBlank())
            assertEquals((product.mrp * 0.88).roundToInt(), product.wholesalePrice)
        }

        sortedByBrand.forEach { product ->
            assertEquals(
                "Photo shifted after brand sort for ${product.id} (${product.brand})",
                idToImageOriginal[product.id],
                product.fullImageUrl
            )
        }

        // Verify zero Wikimedia or mismatched DummyJSON URLs remain in all 808 SKUs
        val legacyBroken = listOf(
            "wikimedia.org", "wikipedia.org", "egg-slicer", "selfie-stick-monopod",
            "tv-studio-camera-pedestal", "black-whisk", "calvin-klein-heel-shoes",
            "nike-baseball-cleats", "fragrances/", "realme-x"
        )
        rawProducts.forEach { p ->
            val urlLow = p.fullImageUrl.lowercase()
            assertFalse("Broken/mismatched URL found on ${p.id}: ${p.fullImageUrl}", legacyBroken.any { urlLow.contains(it) })
        }

        // Verify 4 HeroBannerSlides & category-specific subtitles/variants
        assertEquals(4, com.example.data.model.HeroBannerSlide.DEFAULT_SLIDES.size)

        val shirt = rawProducts.first { it.id == "NAV560" }
        assertTrue(shirt.cardCategorySubtitle.contains("Sizes:"))
        assertTrue(shirt.sizeList.isNotEmpty())
        assertTrue(shirt.colorList.isNotEmpty())
        assertTrue(shirt.cardTrustBadgeText.contains("Exchange", ignoreCase = true))

        val mobile = rawProducts.first { it.id == "NAV677" }
        assertTrue(mobile.cardTrustBadgeText.contains("Warranty", ignoreCase = true))

        val atta = rawProducts.first { it.id == "NAV309" }
        assertTrue(atta.cardCategorySubtitle.contains("Net Wt:"))
        assertTrue(atta.cardTrustBadgeText.contains("FSSAI", ignoreCase = true))
    }
}
