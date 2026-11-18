# Nav Grocery Store & Nav Kart — Android Play Store Architecture

Production Kotlin + Jetpack Compose + MVVM codebase designed for Google Play Store deployment (`com.navohub.navkart`).

## Architecture Overview
- **UI Layer (`ui/screens/NavKartAppScreen.kt`)**: Jetpack Compose Material 3 storefront with multi-vertical switching tabs (`Store`, `Supermall`, `Express 15-Min`, `Essentials -18°C`), search filter, 12% off wholesale badges, wishlist toggles, and WhatsApp OTP verification modal.
- **ViewModel (`ui/viewmodel/StoreViewModel.kt`)**: Unidirectional `StateFlow` state management with strict produce-free guardrails (`zero fresh fruits/vegetables`), cart totals (`12%` off MRP + included `5%` GST), and UPI intent (`upi://pay?pa=8847370905@paytm&pn=NavGroceryStore`).
- **Network Sync (`data/remote/NavFirebaseApi.kt`)**: Retrofit client reading live inventory from `https://nav-grocery-store-default-rtdb.firebaseio.com/android_feed.json` and submitting orders to `orders/{orderId}.json`.
- **Offline Cache (`data/local/CatalogCache.kt`)**: SharedPreferences JSON cache storing all 308+ verified SKUs and customer WhatsApp verification state locally for offline browsing in Sandhaur / Malerkotla 148020.

## Play Store Opening Instructions
1. Open the `/android` directory in **Android Studio Ladybug (or newer)**.
2. Verify JDK 17 and Gradle sync (`app/build.gradle.kts`).
3. Add your production Razorpay Key ID in your Android Manifest / Checkout config when building the signed `.aab` release bundle.
