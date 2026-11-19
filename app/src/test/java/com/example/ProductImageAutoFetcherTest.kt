package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GroceryLocalStore
import com.example.data.model.Product
import com.example.data.util.ProductImageAutoFetcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProductImageAutoFetcherTest {

    @Test
    fun `every SKU NAV309 to NAV808 has a valid mapped URL by product name and SKU`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val seedProducts = GroceryLocalStore(context).loadSeedProducts()
        val nav309To808 = seedProducts.filter { ProductImageAutoFetcher.isNav309To808Sku(it.id) }

        assertEquals(500, nav309To808.size)
        assertEquals(500, ProductImageAutoFetcher.productNameToImageUrl.size)
        assertEquals(500, ProductImageAutoFetcher.skuToImageUrl.size)

        nav309To808.forEach { product ->
            // Simulate Firebase returning a missing/blank image field
            val missingFirebaseProduct = product.copy(image = "")
            val resolvedUrl = ProductImageAutoFetcher.resolveStableImageUrl(missingFirebaseProduct)

            assertTrue(
                "Expected HTTPS/HTTP URL for ${product.id} (${product.name}) when missing from Firebase, got: $resolvedUrl",
                resolvedUrl.startsWith("https://") || resolvedUrl.startsWith("http://")
            )
            assertFalse(
                "Must not fall back to unbacked images/verified path for ${product.id}",
                resolvedUrl.contains("/images/verified/${product.id}")
            )
            assertEquals(product.image, resolvedUrl)

            val ensured = ProductImageAutoFetcher.ensureProductHasImage(missingFirebaseProduct)
            assertEquals(product.image, ensured.image)
            assertEquals(product.image, missingFirebaseProduct.fullImageUrl)
        }
    }

    @Test
    fun `preserves NAV001 to NAV308 verified batch paths intact`() {
        val nav098 = Product(
            id = "NAV098",
            name = "Britannia Bourbon Chocolate Cream Biscuit",
            mrp = 40,
            image = "images/batch_1/10.png"
        )
        assertFalse(ProductImageAutoFetcher.isMissingOrBrokenImage(nav098.image, nav098.id))
        assertEquals(
            "https://www.navgrocerystore.com/images/batch_1/10.png",
            ProductImageAutoFetcher.resolveStableImageUrl(nav098)
        )
    }

    @Test
    fun `fetchAndCacheMissingPackagingPhotos downloads and caches missing Firebase images offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ProductImageAutoFetcher.getCacheDirectory(context).deleteRecursively()

        val samplePngBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        )
        val networkCalls = java.util.concurrent.atomic.AtomicInteger(0)
        val fakeClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                networkCalls.incrementAndGet()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "image/png")
                    .body(samplePngBytes.toResponseBody("image/png".toMediaType()))
                    .build()
            }
            .build()

        val simulatedCloudProducts = listOf(
            Product(id = "NAV309", name = "Aashirvaad Shudh Chakki Atta 5kg", mrp = 265, image = ""),
            Product(id = "NAV509", name = "Surf Excel Matic Top Load Liquid 2L", mrp = 460, image = "null"),
            Product(id = "NAV808", name = "Milton Thermosteel Flip Lid Flask 1000ml", mrp = 1099, image = "")
        )

        val firstSummary = ProductImageAutoFetcher.fetchAndCacheMissingPackagingPhotos(
            context = context,
            products = simulatedCloudProducts,
            missingFromFirebaseSkus = setOf("NAV309", "NAV509", "NAV808"),
            httpClient = fakeClient,
            imageLoader = null
        )

        assertEquals(3, firstSummary.totalChecked)
        assertEquals(3, firstSummary.missingFromFirebaseResolved)
        assertEquals(3, firstSummary.fetchedAndCachedCount)
        assertEquals(0, firstSummary.alreadyCachedCount)
        assertTrue(firstSummary.failedSkus.isEmpty())
        assertEquals(3, networkCalls.get())

        simulatedCloudProducts.forEach { product ->
            val cachedFile = ProductImageAutoFetcher.getCachedPhotoFile(context, product.id)
            assertNotNull("Expected cached file for ${product.id}", cachedFile)
            assertTrue(cachedFile!!.exists() && cachedFile.length() == samplePngBytes.size.toLong())
            assertEquals(cachedFile, ProductImageAutoFetcher.resolveDisplayImageModel(context, product))
        }

        // Second pass should hit local disk cache cleanly with zero extra network calls
        val secondSummary = ProductImageAutoFetcher.fetchAndCacheMissingPackagingPhotos(
            context = context,
            products = simulatedCloudProducts,
            missingFromFirebaseSkus = setOf("NAV309", "NAV509", "NAV808"),
            httpClient = fakeClient,
            imageLoader = null
        )

        assertEquals(0, secondSummary.fetchedAndCachedCount)
        assertEquals(3, secondSummary.alreadyCachedCount)
        assertEquals(3, networkCalls.get())
    }
}
