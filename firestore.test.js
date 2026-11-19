const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_sandhaur_01";
const BOB_UID = "bob_malerkotla_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 2000);
  return {
    uid,
    email: `${uid}@gmail.com`,
    displayName: "Gurpreet Singh",
    navCoins: 0,
    lastSpinAtMs: 0,
    spinCount: 0,
    activeSpinCouponCode: "",
    activeSpinCouponLabel: "",
    activeSpinCouponDiscountRupees: 0,
    activeSpinFreeExpress: false,
    activeSpinFreeLocal: false,
    activeSpinUsed: false,
    hasOrderedGrocery: false,
    hasOrderedElectronics: false,
    coldChainStaplesCount: 0,
    weeklyOrdersCount: 0,
    weeklyWindowStartMs: 0,
    totalOrdersCount: 0,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write profiles, addresses, or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
});

test("Owner can create profile and save valid Sandhaur 148020 delivery address", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  const past = new Date(Date.now() - 1500);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1").set({
      uid: ALICE_UID,
      fullName: "Gurpreet Singh",
      mobile: "9876543210",
      streetAddress: "Main Bazaar near Gurdwara",
      landmark: "Opposite Grain Market",
      villageOrCity: "Sandhaur, District Malerkotla, Punjab",
      pinCode: "148020",
      defaultAddress: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // List own addresses
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("addresses").get());
});

test("Cross-user isolation blocks Bob from reading or modifying Alice data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ navCoins: 200 }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").get());
});

test("Strict 24-hour Spin-the-Wheel cooldown enforced by Firestore rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // First spin succeeds (lastSpinAtMs == 0)
  const nowMs = Date.now() - 1000;
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lastSpinAtMs: nowMs,
      spinCount: 1,
      navCoins: 50,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immediate second spin fails (< 24h)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lastSpinAtMs: Date.now() - 200,
      spinCount: 2,
      navCoins: 150,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Spin after 24h+ seeded via admin bypass succeeds
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      lastSpinAtMs: Date.now() - 86_500_000,
    });
  });

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lastSpinAtMs: Date.now() - 500,
      spinCount: 2,
      activeSpinCouponCode: "SPIN20",
      activeSpinCouponLabel: "Extra ₹20 Off Next Order",
      activeSpinCouponDiscountRupees: 20,
      activeSpinUsed: false,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Verified order creation succeeds while invalid phone or ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_1001").set({
      orderId: "NAV-1001",
      uid: ALICE_UID,
      customerName: "Gurpreet Singh",
      customerPhone: "9876543210",
      customerEmail: "alice_sandhaur_01@gmail.com",
      deliveryTier: "EXPRESS_15_MIN",
      addressSnapshot: "Main Bazaar, Sandhaur, District Malerkotla, Punjab, PIN 148020",
      paymentMethod: "UPI_INTENT",
      status: "Order Placed",
      totalMrp: 500,
      wholesaleDiscount: 60,
      spinCouponDiscountRupees: 20,
      spinCouponCode: "SPIN20",
      coinsRedeemed: 0,
      deliveryFee: 25,
      finalTotal: 445,
      coinsEarned: 15,
      integrityDigest: "a".repeat(32),
      items: [{ productId: "NAV001", name: "Sealed Atta", qty: 1, mrp: 500, wholesalePrice: 440 }],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject ghost field / invalid phone
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_BAD").set({
      id: "ORD_BAD",
      orderId: "NAV-BAD",
      uid: ALICE_UID,
      customerName: "Bad",
      customerPhone: "12345",
      deliveryTier: "LOCAL_2_HR",
      addressSnapshot: "Sandhaur",
      paymentMethod: "UPI_INTENT",
      status: "Order Placed",
      totalMrp: 100,
      wholesaleDiscount: 12,
      spinCouponDiscountRupees: 0,
      coinsRedeemed: 0,
      deliveryFee: 0,
      finalTotal: 88,
      coinsEarned: 5,
      integrityDigest: "b".repeat(32),
      items: [{ productId: "NAV001", qty: 1 }],
      createdAt: past,
    })
  );
});

test("Customer can cancel Order Placed but cannot cancel Packed or tamper with prices", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 1000);
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_CANCEL_1");

  await assertSucceeds(
    orderRef.set({
      orderId: "NAV-CANCEL-1",
      uid: ALICE_UID,
      customerName: "Gurpreet Singh",
      customerPhone: "9876543210",
      pinCode: "148020",
      deliveryTier: "LOCAL_2_HR",
      addressSnapshot: "Main Bazaar, Sandhaur, Punjab 148020",
      paymentMethod: "EMI_INSTALLMENTS",
      status: "Order Placed",
      totalMrp: 4000,
      wholesaleDiscount: 480,
      spinCouponDiscountRupees: 0,
      coinsRedeemed: 0,
      deliveryFee: 0,
      finalTotal: 3520,
      coinsEarned: 70,
      integrityDigest: "c".repeat(32),
      items: [{ productId: "NAV400", qty: 1 }],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject price tampering
  await assertFails(orderRef.update({ finalTotal: 1, updatedAt: new Date(Date.now() - 200) }));

  // Cancel while Order Placed succeeds
  await assertSucceeds(orderRef.update({ status: "Cancelled", updatedAt: new Date(Date.now() - 200) }));

  // Seed Packed order and ensure customer cannot cancel after packing
  const packedRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_PACKED_1");
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("orders").doc("ORD_PACKED_1").set({
      orderId: "NAV-PACKED-1",
      uid: ALICE_UID,
      customerName: "Gurpreet Singh",
      customerPhone: "9876543210",
      deliveryTier: "STORE_PICKUP",
      addressSnapshot: "Store Pickup Main Bazaar Sandhaur",
      paymentMethod: "CASH_ON_DELIVERY",
      status: "Packing",
      totalMrp: 500,
      wholesaleDiscount: 60,
      spinCouponDiscountRupees: 0,
      coinsRedeemed: 0,
      deliveryFee: 0,
      finalTotal: 440,
      coinsEarned: 8,
      integrityDigest: "d".repeat(32),
      items: [{ productId: "NAV001", qty: 1 }],
      createdAt: past,
      updatedAt: past,
    });
  });

  await assertFails(packedRef.update({ status: "Cancelled", updatedAt: new Date(Date.now() - 200) }));
});

test("DPDP account deletion allows owner to delete profile and anonymize orders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 1000);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fcmToken: "fcm_token_sample_123",
      fcmTokenUpdatedAtMs: Date.now() - 500,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ORD_ANON_1");
  await assertSucceeds(
    orderRef.set({
      orderId: "NAV-ANON-1",
      uid: ALICE_UID,
      customerName: "Gurpreet Singh",
      customerPhone: "9876543210",
      customerEmail: "alice@gmail.com",
      deliveryTier: "LOCAL_2_HR",
      addressSnapshot: "Main Bazaar, Sandhaur 148020",
      paymentMethod: "UPI_INTENT",
      status: "Order Placed",
      totalMrp: 500,
      wholesaleDiscount: 60,
      spinCouponDiscountRupees: 0,
      coinsRedeemed: 0,
      deliveryFee: 0,
      finalTotal: 440,
      coinsEarned: 8,
      integrityDigest: "e".repeat(32),
      items: [{ productId: "NAV001", qty: 1 }],
      createdAt: past,
    })
  );

  await assertSucceeds(
    orderRef.update({
      customerName: "Deleted Customer",
      customerPhone: "",
      customerEmail: "",
      nriBuyerEmail: "",
      addressSnapshot: "Redacted (DPDP 2023)",
      anonymized: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});

