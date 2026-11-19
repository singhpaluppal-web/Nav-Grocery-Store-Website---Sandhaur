# Nav Grocery Store & NavKart Unified Storefront Plan

A unified Android and web commerce platform combining **Nav Grocery Store** (100% factory-sealed packaged groceries, dairy, spices, and cold-chain frozen foods with zero fresh produce) and **NavKart Supermall** (electronics, apparel, footwear, and home essentials) for Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020 (`+91 8847370905`). This upgrade introduces one-click Google Gmail authentication, owner-only saved delivery addresses, tamper-resistant wholesale checkout with digital receipts, and strict customer-vs-admin route isolation under the Forest Green, Warm Cream, and Gold visual identity.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> All four foundational product decisions have been confirmed and will govern the implementation:
> - **Confirmed Decision 1 — Full 808-SKU Catalog Scope**: Keep all 308 verified Nav Grocery Store SKUs (`NAV001–NAV308`) and their verified packaging photography intact alongside all 500 expanded supermall SKUs (`NAV309–NAV808`), enforcing a strict zero-fresh-produce policy and flat 12% wholesale discount off MRP.
> - **Confirmed Decision 2 — Guest Browsing + Gmail Auth Gate**: Shoppers can browse, search, switch languages, inspect Quick View zoom details, and manage their cart as guests, while one-click Google Gmail Sign-In is required to access the Account Dashboard, Saved Delivery Addresses, Live Order Tracker, and final Checkout submission.
> - **Confirmed Decision 3 — Firebase Auth Provisioning + RTDB Rules**: Firebase Authentication (Google Sign-In via Jetpack Credential Manager on Android and Firebase Auth on Web) will be provisioned for this project, paired with a strict Firebase Realtime Database ruleset enforcing public read-only catalog access, owner-only customer profiles/addresses/orders, and admin-locked pricing and catalog controls.
> - **Confirmed Decision 4 — Unified Brand Palette**: Both Android and Web storefronts will adopt the standardized palette tokens: Forest Green (`#0F3D28`), Warm Cream (`#F8F7F2`), and Gold (`#E6A817`).

---

## 1. Overview & Core Concept

- **What It Does**: Unifies two complementary retail catalogs—verified everyday pantry/frozen staples (**Nav Grocery Store**) and multi-category lifestyle/electronics (**NavKart Supermall**)—into a single bilingual (English/Punjabi, plus Hindi toggle on web) shopping experience with unified search, unified basket, verified customer accounts, saved delivery addresses, and secure UPI/COD/WhatsApp checkout.
- **Target Audience & Context**: Households, kirana bulk buyers, and shoppers across Sandhaur and nearby Malerkotla/Sangrur villages (`PIN 148020` and neighboring delivery zones) looking for trusted wholesale savings without leaving home, plus the store owner managing pricing, visual audits, and internal GST billing privately.
- **Key Value**: Customers gain seamless Gmail sign-in, reusable Sandhaur-aware delivery addresses, 15-minute express or 2-hour local delivery slots, transparent itemized bills, and live order status tracking, while the business eliminates client-side price tampering and isolates internal POS/GSTIN (`03AHPPU6185N1ZF`) workflows from public shoppers.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Unified Guest Discovery & Dual-Storefront Switching**: Shoppers land directly on the unified home experience featuring a 4-slide lifestyle hero carousel, unified smart search (matching SKU ID, title, brand, barcode), department toggle/pills separating **Nav Grocery (Sealed & Frozen)** and **NavKart Supermall**, and SKU-locked product cards with real-time wholesale pricing.
2. **Product Inspection & Basket Building**: Selecting any item opens Quick View with 2.2x hover/tap label zoom, one-tap SKU copy for WhatsApp sharing, variant selectors (size/color where applicable), and unified basket quantity controls.
3. **One-Click Google Gmail Sign-In**: From the top navigation bar's **Account** / **My Orders** control—or automatically when proceeding from cart to checkout while signed out—shoppers sign in with one tap using Google Sign-In. Returning Android users benefit from silent Credential Manager session restoration.
4. **Delivery Address Management**: Signed-in customers manage multiple addresses inside their **Account Profile** and directly inside the **Slide-Over Checkout Drawer / Sheet**. Fields validate Full Name, 10-digit Indian Mobile Number, House/Street Address, Landmark, Village/City (defaulting to `Sandhaur, District Malerkotla, Punjab`), and 6-digit PIN Code (defaulting to `148020` with nearby zone hints). Selecting or changing a default address immediately pre-fills checkout.
5. **Fulfillment, Payment & Tamper-Proof Receipt**: Customers pick **15-min Express Delivery**, **2-hr Local Delivery**, or **30-min Main Bazaar Store Pickup**, review master-verified 12% wholesale math, pay via dynamically generated UPI QR / UPI App Intent or Cash on Delivery, receive a verifiable order ID (`NAV-YYYYMMDD-...`) and printable/shareable itemized receipt, and track live fulfillment status inside **My Orders**.

### Visual Identity & Theme
- **Aesthetic Direction**: Refined regional wholesale flagship—warm, trustworthy, high-contrast retail clarity balancing heritage Punjab merchant credibility with clean modern mobile ergonomics.
- **Color Palette & Tokens**:
  - *Primary (`--primary`)*: Forest Green `#0F3D28` (headers, primary actions, active navigation pills, price emphasis) with Deep Forest `#092819` for high-contrast footers and dark top accents.
  - *Surface / Canvas (`--background`)*: Warm Cream `#F8F7F2` paired with crisp `#FFFFFF` product cards and `#EFECE3` subtle borders to let packaging photos stand out cleanly.
  - *Accent (`--accent`)*: Gold `#E6A817` (12% wholesale badges, hero CTAs, savings highlights, default-address badges) with soft gold surface tints (`#FDF4D7`) for trust callouts.
  - *Semantic Status*: Discount/Alert Crimson (`#C62828`) for MRP savings tags and Verified Emerald (`#1B6B44`) for sealed-stock and paid badges.
- **Typography & Hierarchy**: Clean sans-serif display headings (`ExtraBold`/`Bold`) for wholesale prices, section headers, and bilingual English/Punjabi labels (`sp`-scaled on Android), paired with tabular numeric alignment on SKU IDs, MRP strikethroughs, and totals.
- **Component Styling & Accessibility**: Material Design 3 surfaces, `48dp` minimum touch targets, edge-to-edge inset handling, responsive compact/tablet multi-column checkout layouts, instant language switching, and clean loading/empty/signed-out states.

### Interactive Feedback & Motion
- Smooth carousel transitions with dot indicators, 2.2x zoom lens cursor/tap feedback, snackbars/toasts on SKU copy and address updates, subtle loading indicators during Gmail OAuth and server-validated order submission, and duplicate-submit button cooldown locks with countdown feedback.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Master-Catalog Total Recalculation at Checkout**:
  - *Chosen Approach*: Before generating UPI QR payloads, opening WhatsApp summaries, or writing an order record to Firebase, the checkout validator strips client-submitted prices, looks up each SKU ID against the locked 808-item master catalog (`NAV001–NAV808`), and recomputes `Math.round(mrp * 0.88) * quantity`. Orders containing unknown SKUs, negative/zero quantities, or tampered totals are rejected cleanly.
  - *Why*: Eliminates browser DevTools or memory tampering risks on wholesale totals before any payment QR or order confirmation occurs.
  - *Alternatives Considered*: Trusting client cart totals directly (unsafe for live UPI amounts and order ledgers).

- **Decision 2 — Guest Browsing with Mandatory Gmail Auth Gate at Account & Checkout**:
  - *Chosen Approach*: Catalog discovery and cart assembly require zero friction for first-time visitors, while Checkout, Saved Addresses, and My Orders require an authenticated Firebase Google user session (`uid` + Gmail address).
  - *Why*: Maximizes catalog conversion while guaranteeing every order and delivery address belongs to an owner-verified Firebase UID for strict security rules.
  - *Alternatives Considered*: Mandatory login wall on app launch (high bounce rate) or anonymous orders (cannot isolate `/users/$uid` securely).

- **Decision 3 — Owner-Only `/users/$uid` Isolation & Public Privacy Shield**:
  - *Chosen Approach*: Customer profiles, saved delivery addresses, and personal order histories live strictly under `/users/$uid` with Firebase rules allowing read/write only when `auth != null && auth.uid == $uid`. Public storefront views never expose owner private banking/GSTIN PII or POS billing tools, which remain behind the SHA-256 + device-bound Admin Portal.
  - *Why*: Protects customer phone/address privacy between shoppers and protects merchant compliance data from public scraping.
  - *Alternatives Considered*: Shared flat `/orders` public node (leaks customer names, phones, and addresses across users).

- **Decision 4 — Strict Sanitization & Submission Rate Limiting**:
  - *Chosen Approach*: Every customer-editable text field (names, addresses, landmarks, notes, search queries) is HTML/script-escaped before DOM rendering and trimmed/validated prior to persistence; checkout enforces an active submission lock plus duplicate-order cooldown window.
  - *Why*: Prevents stored/reflected XSS on web storefronts and stops double-taps from generating duplicate deliveries or duplicate WhatsApp dispatches.

---

## 4. Technical Architecture & Data Strategy

### System Architecture & Data Flow

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   UNIFIED PUBLIC STOREFRONT (ANDROID + WEB)                │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐  │
│  │ Unified Catalog View │  │  Quick View & Cart   │  │ Account & Orders │  │
│  │ • Nav Grocery (308)  │  │ • 2.2x Image Zoom    │  │ • Gmail Profile  │  │
│  │ • NavKart Mall (500) │  │ • Copy SKU Button    │  │ • Address Book   │  │
│  │ • EN/PA (+HI Web)    │  │ • Variant Selectors  │  │ • Live Tracker   │  │
│  └──────────┬───────────┘  └──────────┬───────────┘  └────────┬─────────┘  │
│             └─────────────────────────┼───────────────────────┘            │
│                                       ▼                                    │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │               GATED CHECKOUT & SECURITY VALIDATION LAYER             │  │
│  │ • Gmail Auth Guard  • Address Autofill  • Sanitizer & Rate Limiter   │  │
│  │ • Master-Catalog Re-Pricer: Math.round(mrp * 0.88) per locked SKU    │  │
│  │ • UPI QR / Intent + COD + Verifiable Order ID + Itemized Receipt     │  │
│  └───────────────┬─────────────────────────────────────┬────────────────┘  │
└──────────────────┼─────────────────────────────────────┼───────────────────┘
                   │                                     │
                   ▼                                     ▼
┌─────────────────────────────────────┐   ┌──────────────────────────────────┐
│      FIREBASE BACKEND SERVICES      │   │  ISOLATED ADMIN CONTROL CENTER   │
│ • Firebase Auth (Google / Gmail)    │   │ • SHA-256 PIN + Device Lock      │
│ • RTDB /products (Public Read-Only) │   │ • POS & GSTIN (03AHPPU6185N1ZF)  │
│ • RTDB /users/$uid/profile          │   │ • Photo Audit & Hero Carousel    │
│ • RTDB /users/$uid/addresses        │   │ • Order Status Fulfillment Sync  │
│ • RTDB /users/$uid/orders           │   │ • Exclusive Owner AI Copilot     │
│ • Strict Security Rules (Auth/Admin)│   └──────────────────────────────────┘
└─────────────────────────────────────┘
```

### Data Model & State
- **Auth Session State**: `SignedOut` vs `SignedIn(uid, email, displayName, photoUrl)` synchronized across Android Credential Manager + Firebase Auth and Web Firebase Auth SDK.
- **Customer Profile (`/users/$uid/profile`)**: Authenticated user metadata, preferred language (`EN`/`PA`), default address ID, and last updated timestamp.
- **Delivery Address (`/users/$uid/addresses/$addressId`)**: `id`, `fullName` (2–80 sanitized chars), `mobile` (strict 10-digit Indian `[6-9]\d{9}`), `streetAddress`, `landmark`, `villageOrCity` (default `Sandhaur, District Malerkotla, Punjab`), `pinCode` (6-digit numeric, default `148020`), and `isDefault` boolean.
- **Master Catalog SKU (`/products/$key`)**: Immutable `id` (`NAV001–NAV808`), `name`, `brand`, `category`, `department` (`Packaged Groceries` vs `NavKart Supermall`), `mrp`, server/rule-locked `sale = round(mrp * 0.88)`, `image` strictly bound by SKU ID, and policy flags guaranteeing zero fresh produce.
- **Verified Order & Receipt (`/users/$uid/orders/$orderId` + admin fulfillment ledger)**: Tamper-proof `orderId`, `uid`, `customerEmail`, sanitized delivery address snapshot, fulfillment tier (`EXPRESS_15_MIN`, `LOCAL_2_HR`, `STORE_PICKUP_30_MIN`), server-verified line items (`sku`, `name`, `qty`, locked `mrp`, locked `salePrice`), total MRP, 12% savings, final payable amount, payment method (`UPI` / `COD`), payment verification reference, itemized receipt timestamp, and live status (`Placed`, `Packed`, `Out for Delivery`, `Delivered`).

### Interactive Component & State Mapping
- **Top Navigation Bar**: Displays brand identity (`Nav Grocery & NavKart`), bilingual toggle, Account avatar pill (triggers Google Gmail Sign-In when signed out, opens Account Dashboard when signed in), **My Orders** shortcut with active order badge, and Cart counter.
- **Unified Catalog & Department Switcher**: Switches seamlessly between **All 808 SKUs**, **Nav Grocery (Sealed & Frozen — 308+)**, and **NavKart Supermall (Electronics & Lifestyle)** while keeping search, category chips, brand filters, and sort synced without shifting SKU-locked images.
- **Account Dashboard & Address Book**: Allows signed-in shoppers to view their Gmail identity, add/edit/delete saved delivery addresses with inline Sandhaur/148020 field validation, mark a primary default address, and sign out cleanly.
- **Slide-Over Checkout Sheet / Drawer**:
  - If unauthenticated, renders an inline one-click **Continue with Google (Gmail)** prompt that resumes checkout seamlessly upon success.
  - Auto-populates the default saved address (or offers a quick address switcher + inline **+ Add / Edit Address** form saved straight to `/users/$uid/addresses`).
  - Lets the user toggle between **15-min Express Delivery**, **2-hr Local Delivery**, and **30-min Main Bazaar Pickup**.
  - Runs the master-catalog price recalculator, validates address/phone rules, enforces a 5-second duplicate submission lock, generates a live UPI QR code (or Cash on Delivery confirmation), saves the order to the user's owner-scoped node, opens a printable/WhatsApp-ready itemized receipt, and updates **My Orders**.
- **Security Rules & Admin Isolation**: Generates deployable Realtime Database security rules JSON locking `/products`, `/hero_banners`, and `/system_locks` against unauthenticated/non-admin writes while restricting `/users/$uid` strictly to `auth.uid === $uid`, and keeps Admin POS/GSTIN (`03AHPPU6185N1ZF`) isolated behind SHA-256 PIN + device-lock verification.
