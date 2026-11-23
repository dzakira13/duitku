const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("wallets").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      email: "h@example.com",
      currencyCode: "IDR",
      themeMode: "SYSTEM",
      budgetCycleDay: 1,
      budgetAlertPercent: 80,
      privacyBalanceHidden: false,
      budgetAlertsEnabled: true,
      weeklyDigestEnabled: false,
      profileSetupDone: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("UserProfile: owner can create, read, update profile, contact info & preferences; ghost fields and cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sari Wijaya",
      bio: "Saving for home",
      avatarData: "preset:emerald",
      email: "sari@example.com",
      phone: "+62 812 3456 7890",
      location: "Jakarta",
      currencyCode: "IDR",
      themeMode: "SYSTEM",
      budgetCycleDay: 25,
      budgetAlertPercent: 80,
      privacyBalanceHidden: false,
      budgetAlertsEnabled: true,
      weeklyDigestEnabled: true,
      profileSetupDone: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update of contact info & preferences preserves createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phone: "+62 811 2222 3333",
      currencyCode: "USD",
      privacyBalanceHidden: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject cross-user read & write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Bob",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(aliceDb.collection("users").get());
});

test("Wallets, Transactions & Budgets: owner CRUD succeeds, invalid payloads & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty subcollection list succeeds for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("wallets").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("budgets").get());

  // Create Wallet (no id field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallets").doc("wallet_1").set({
      userId: ALICE_UID,
      name: "Main Bank",
      walletType: "BANK",
      balance: 5000000,
      colorKey: "EMERALD",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject extra `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wallets").doc("wallet_2").set({
      id: "wallet_2",
      userId: ALICE_UID,
      name: "Cash",
      walletType: "CASH",
      balance: 200000,
      colorKey: "GOLD",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create Transaction
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      title: "Kopi Kenangan",
      amount: 28000,
      transactionType: "EXPENSE",
      category: "Food & Dining",
      walletId: "wallet_1",
      walletName: "Main Bank",
      note: "Morning coffee",
      occurredAtMillis: Date.now() - 5000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject non-positive transaction amount
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      userId: ALICE_UID,
      title: "Zero",
      amount: 0,
      transactionType: "EXPENSE",
      category: "Food & Dining",
      walletId: "wallet_1",
      walletName: "Main Bank",
      occurredAtMillis: Date.now() - 5000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create Budget
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("b_1").set({
      userId: ALICE_UID,
      category: "Food & Dining",
      monthlyLimit: 1500000,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user subcollection isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wallets").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("budgets").doc("b_1").delete());
});
