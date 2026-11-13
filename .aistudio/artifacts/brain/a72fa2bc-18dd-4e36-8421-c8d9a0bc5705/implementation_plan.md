# Nav Grocery Store — Pure Public Storefront & SKU Photo Alignment Plan

Transform the storefront into a dedicated, 100% public **Nav Grocery Store** experience for Main Bazaar, Sandhaur (148020) by stripping all NavKart and Admin code, enforcing the Public Privacy Shield, upgrading the Store AI helper to trilingual support, and locking every product image from `NAV001` to `NAV308` to its exact SKU and packaging category.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The decisions below reflect your instructions and confirmed choices. Please review and approve to proceed with code updates.

- **Public Privacy Shield & GSTIN Placement (Confirmed)**: Owner personal name, personal email, bank account number, and IFSC code are completely removed from the public codebase. `GSTIN: 03AHPPU6185N1ZF` is hidden across all public storefront pages and the footer, appearing exclusively on the post-order customer digital bill receipt.
- **Complete Removal of NavKart & Admin Code (Confirmed)**: All NavKart marketplace components, banners, data files, and links, as well as all Admin portals, PIN `148020` gates, and builder scripts, will be permanently deleted from this public storefront codebase.
- **Immutable SKU Photo Binding (Default Recommended)**: Because legacy `batch_*` image paths on the server had shifted indices (causing `NAV004`, `NAV005`, and `NAV006` mismatches), every SKU (`NAV001`–`NAV308`) will be bound by immutable SKU ID and product-title classification to verified brand/packaging photography rather than raw array index order.

---

## 1. Overview & Core Concept

- **What It Does**: A clean, fast, customer-only digital grocery storefront for **Nav Grocery Store** serving Main Bazaar, Sandhaur, Distt. Malerkotla, Punjab `148020`. Shoppers browse all 308 verified sealed packaged groceries and `-18°C` cold-chain frozen essentials at a flat 12% wholesale discount off MRP, choose 2-hour local delivery or 30-minute store pickup, and check out via Dynamic UPI QR, COD, or WhatsApp dispatch.
- **Target Audience**: Local households and bulk buyers in Sandhaur `148020` and surrounding villages shopping on mobile phones, tablets, and desktops in English, Punjabi, or Hindi.
- **Key Value**: Eliminates internal marketplace/admin clutter, guarantees customer trust through verified public contact details and strict privacy shielding, and fixes every mismatched product photo across all 308 SKUs so shoppers always see the exact product packaging they are ordering.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Discovery & Browsing**: Shoppers land directly on the Nav Grocery Store storefront with a sticky top bar (Logo, Sandhaur `148020` badge, instant predictive search, 2-Hour Delivery / 30-Min Pickup switcher, WhatsApp OTP Login, and Basket counter). Zero admin or external marketplace distractions appear anywhere.
  2. **Aisle Filtering & Chunked Grid**: Auto-sliding hero banners route shoppers straight into grocery aisles (*All 308 Items*, *12% Off Deals*, *Frozen Veg & Snacks -18°C*, *Atta, Rice & Dal*, *Desi Ghee & Oils*, *Spices & Masale*, *Tea, Beverages & Biscuits*, *Household & Personal Care*). Products render in batches of 24 with CSS shimmer skeletons and instant in-memory search/sort.
  3. **Trilingual Store AI Helper**: A floating, zero-key rule-based assistant supports **English**, **ਪੰਜਾਬੀ (Gurmukhi)**, and **हिन्दी (Devanagari)** for product lookup, 2-hour delivery / 30-minute pickup guidance, and direct WhatsApp helpline links (`+91 8847370905` / `+91 9915752905`).
  4. **Checkout & Digital Bill Receipt**: Multi-step slide-over basket drawer calculates 12% savings, enforces COD rules, renders a scannable Dynamic UPI QR code, saves the order to Firebase RTDB, displays the customer's printable digital bill receipt, and opens a pre-formatted WhatsApp order summary.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Crisp, heritage-meets-modern neighbourhood supermarket aesthetic combining warm editorial restraint with high-clarity retail scannability.
  - *Color Palette*: Primary Forest Green (`#0F3D28`), Warm Cream Canvas (`#F8F7F2`), Pure White product surfaces (`#FFFFFF`), and Accent Gold (`#E6A817`) strictly reserved for primary CTAs and `-12% OFF` highlights.
  - *Typography*: `Playfair Display` for display headlines, `Plus Jakarta Sans` for UI copy and navigation, and `JetBrains Mono` with `tabular-nums` for SKU codes, prices, and bill figures.
  - *Component Discipline*: Uniform 1:1 product card aspect ratios, single-elevation cards, unboxed metadata separators (`·`), and zero broken images via deterministic brand packaging SVG fallbacks.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Strict Removal of Admin & NavKart Code**
  - *Chosen Approach*: Delete all Admin and NavKart views, standalone HTML portals, and cross-links from this codebase while keeping URL rewrites routed cleanly to the public storefront.
  - *Why*: Guarantees zero accidental exposure of private owner tools, PIN screens, or non-grocery marketplace items on `www.navgrocerystore.com`.
  - *Trade-Off*: Store administration and NavKart management must be accessed from their separate dedicated deployments rather than inside this repo.

- **Decision 2: Immutable SKU-Keyed Photo Binding vs Legacy `batch_*` Paths**
  - *Chosen Approach*: Replace shifted `images/batch_*` reliance with an explicit SKU-keyed and product-type-verified image mapping table for `NAV001`–`NAV308`. Every SKU is keyed by its immutable ID (`NAV001`–`NAV308`) and verified against its product type (`Atta`, `Basmati Rice`, `Dal`, `Desi Ghee`, `Cooking Oil`, `Whole/Ground Spices`, `Frozen -18°C`, `Tea/Beverages`, `Biscuits/Snacks`, `Household/Personal Care`) before choosing a photo, explicitly correcting `NAV004`, `NAV005`, `NAV006`, and all shifted rows.
  - *Why*: Legacy `batch_1/4.png` (`NAV004` showing black pepper), `batch_1/5.png` (`NAV005` showing atta), and `batch_1/6.png` (`NAV006` showing cumin) were misnumbered in the original image dump, so relying solely on sequential filenames propagates errors.
  - *Alternatives Considered*: Using sequential index matching (`index + 1`), which directly caused the shift bug and is permanently eliminated.

- **Decision 3: Official Public Contact & Privacy Shield**
  - *Chosen Approach*: Hardcode only verified public customer details across the storefront (`Main Bazaar, Sandhaur, Distt. Malerkotla, Punjab 148020`, `+91 8847370905`, `+91 9915752905`, `www.navgrocerystore.com`, `orders@navgrocerystore.com`, Instagram `@nav_grocery_store`, YouTube `@shopatnav`), strip all personal emails/names/bank details, and isolate GSTIN strictly to the customer's post-order digital bill receipt.
  - *Why*: Protects owner PII while maintaining legal bill transparency upon purchase.

---

## 4. Technical Architecture & Data Strategy

- **Architecture & Data Flow**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Public Customer Browser (Sandhaur 148020)            │
│                                                                         │
│  ┌───────────────────────┐   ┌──────────────────┐   ┌────────────────┐  │
│  │ Public Header & Aisles│   │ 24-Batch Catalog │   │ Trilingual AI  │  │
│  │ 2h Delivery / 30m Pick│   │ 308 Grocery SKUs │   │ EN · PA · HI   │  │
│  └───────────┬───────────┘   └────────┬─────────┘   └───────┬────────┘  │
│              │                        │                     │           │
│              ▼                        ▼                     ▼           │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │     Immutable SKU-Locked Image Resolver (Keyed by NAV001–NAV308)  │  │
│  │  Exact SKU Lock ─► Brand/Type Verified Photo ─► Branded SVG Pack  │  │
│  └──────────────────────────────────┬────────────────────────────────┘  │
│                                     │                                   │
│  ┌──────────────────────────────────▼────────────────────────────────┐  │
│  │        Basket Drawer, Dynamic UPI QR, COD & Digital Receipt       │  │
│  └───────────┬──────────────────────────────────────────┬────────────┘  │
└──────────────┼──────────────────────────────────────────┼───────────────┘
               │ (Read 308 SKUs & Settings; Write Orders) │ (WhatsApp Receipt)
               ▼                                          ▼
┌──────────────────────────────┐           ┌──────────────────────────────┐
│  Firebase Realtime Database  │           │   Customer WhatsApp Launch   │
│  products / settings / orders│           │   +91 8847370905 / 9915752905│
└──────────────────────────────┘           └──────────────────────────────┘
```

- **Data Model & State**:
  - **Catalog State**: Indexed strictly as a `Map<string, Product>` keyed by immutable SKU ID (`NAV001`–`NAV308`), never matched by array index. Every SKU enforces `sale = Math.round(mrp * 0.88)` and blocks fresh produce terms.
  - **Image Verification Layer**: Evaluates each SKU's immutable ID, brand, and product title keywords (e.g., separating `Atta` flour bags vs `Basmati Rice` bags vs `Whole Spices` vs `Ghee/Oils` vs `Frozen -18°C`) to prevent cross-category photo leakage (explicitly fixing `NAV004`, `NAV005`, `NAV006`, and auditing all 308 items).
  - **Public Settings & Orders**: Reads store branding/payment settings (`upiId`, `codMinOrder`, `deliveryFee`, `freeDeliveryAbove`, `razorpayEnabled`) from Firebase RTDB with safe public defaults and zero owner PII fallback strings; writes placed customer orders to `orders/` and builds printable digital bill receipts.
- **Interactive Component & State Mapping**:
  - *Header Search & Aisle Filter* updates in-memory filtered SKU slice and resets batch pagination to 24 cards.
  - *Fulfillment Toggle* switches delivery fee and time-slot options between **2-Hour Local Delivery (Sandhaur 148020)** and **30-Minute Store Pickup (Main Bazaar)**.
  - *Trilingual Store AI* switches between English, Punjabi (`ਪੰਜਾਬੀ`), and Hindi (`हिन्दी`) responses, queries the 308-SKU catalog in-memory, and triggers direct basket additions or WhatsApp helpline links.
  - *Checkout Drawer* generates Dynamic UPI QR codes, validates UTR/COD thresholds, persists orders, and transitions cleanly to the printable customer receipt state.
