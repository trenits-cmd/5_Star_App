import { describe, it, before, after, beforeEach } from "node:test";
import { readFileSync } from "node:fs";
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { doc, setDoc, getDoc, updateDoc, deleteDoc, Timestamp, arrayUnion } from "firebase/firestore";

const PROJECT = "demo-starapp";
const HOURS = 3600 * 1000;

let env;

const ALICE = "alice";
const BOB = "bob";
const EVE = "eve";
const COUPLE = "c1";

before(async () => {
  env = await initializeTestEnvironment({
    projectId: PROJECT,
    firestore: { rules: readFileSync("../firestore.rules", "utf8"), host: "127.0.0.1", port: 8080 },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "couples", COUPLE), { members: [ALICE, BOB] });
  });
});

const db = (uid) => env.authenticatedContext(uid).firestore();

function seedTask(createdAt, status = "OPEN") {
  return env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), "couples", COUPLE, "tasks", "t1"), {
      ownerUid: ALICE,
      type: "PENALTY",
      title: "Abwasch",
      status,
      createdAt: Timestamp.fromMillis(createdAt),
    });
  });
}

describe("Paar-Mitgliedschaft", () => {
  it("Mitglied liest das Paar", async () => {
    await assertSucceeds(getDoc(doc(db(ALICE), "couples", COUPLE)));
  });

  it("Fremder liest das Paar nicht", async () => {
    await assertFails(getDoc(doc(db(EVE), "couples", COUPLE)));
  });

  it("Fremder liest keine Tageseinträge", async () => {
    await assertFails(getDoc(doc(db(EVE), "couples", COUPLE, "days", "2026-11-01_alice")));
  });

  it("Unangemeldeter Zugriff ist verboten", async () => {
    const anon = env.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(anon, "couples", COUPLE)));
  });
});

describe("Beitritt zum Paar", () => {
  it("Einzelnes Mitglied darf sich selbst als zweites eintragen", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "couples", "solo"), { members: [ALICE] });
    });
    await assertSucceeds(updateDoc(doc(db(BOB), "couples", "solo"), { members: [ALICE, BOB] }));
  });

  it("Fremder darf ein volles Paar nicht übernehmen", async () => {
    await assertFails(updateDoc(doc(db(EVE), "couples", COUPLE), { members: [ALICE, EVE] }));
  });
});

describe("Eigene Daten", () => {
  it("Mitglied schreibt eigene Kategorie", async () => {
    await assertSucceeds(
      setDoc(doc(db(ALICE), "couples", COUPLE, "categories", "c1"), { ownerUid: ALICE, name: "Sport", weight: 2 }),
    );
  });

  it("Mitglied schreibt keine Kategorie im Namen des Partners", async () => {
    await assertFails(
      setDoc(doc(db(ALICE), "couples", COUPLE, "categories", "c2"), { ownerUid: BOB, name: "Sport", weight: 2 }),
    );
  });

  it("Partner ändert fremde Tageseinträge nicht", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "couples", COUPLE, "days", "d1"), { ownerUid: ALICE, snapshots: [] });
    });
    await assertFails(updateDoc(doc(db(BOB), "couples", COUPLE, "days", "d1"), { snapshots: [] }));
  });
});

describe("Aufgaben", () => {
  it("Partner bestätigt offene Aufgabe", async () => {
    await seedTask(Date.now());
    await assertSucceeds(updateDoc(doc(db(BOB), "couples", COUPLE, "tasks", "t1"), { status: "CONFIRMED" }));
  });

  it("Partner ändert Titel nicht", async () => {
    await seedTask(Date.now());
    await assertFails(updateDoc(doc(db(BOB), "couples", COUPLE, "tasks", "t1"), { title: "Hack" }));
  });

  it("Besitzer bestätigt die eigene Aufgabe nicht selbst", async () => {
    await seedTask(Date.now());
    await assertFails(updateDoc(doc(db(ALICE), "couples", COUPLE, "tasks", "t1"), { status: "CONFIRMED" }));
  });

  it("Automatische Bestätigung vor 48 Stunden ist verboten", async () => {
    await seedTask(Date.now() - 47 * HOURS);
    await assertFails(updateDoc(doc(db(BOB), "couples", COUPLE, "tasks", "t1"), { status: "AUTO_CONFIRMED" }));
  });

  it("Automatische Bestätigung nach 48 Stunden ist erlaubt", async () => {
    await seedTask(Date.now() - 49 * HOURS);
    await assertSucceeds(updateDoc(doc(db(BOB), "couples", COUPLE, "tasks", "t1"), { status: "AUTO_CONFIRMED" }));
  });

  it("Fremder bestätigt keine Aufgabe", async () => {
    await seedTask(Date.now());
    await assertFails(updateDoc(doc(db(EVE), "couples", COUPLE, "tasks", "t1"), { status: "CONFIRMED" }));
  });
});

describe("Beitritt per arrayUnion und Aufgabentitel", () => {
  it("Einzelnes Mitglied tritt per arrayUnion bei", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "couples", "solo2"), { members: [ALICE] });
    });
    await assertSucceeds(updateDoc(doc(db(BOB), "couples", "solo2"), { members: arrayUnion(BOB) }));
  });

  it("Volles Paar lässt keinen dritten Teilnehmer zu", async () => {
    await assertFails(updateDoc(doc(db(EVE), "couples", COUPLE), { members: arrayUnion(EVE) }));
  });

  it("Besitzer wählt den Aufgabentitel", async () => {
    await seedTask(Date.now());
    await assertSucceeds(updateDoc(doc(db(ALICE), "couples", COUPLE, "tasks", "t1"), { title: "Filmabend" }));
  });

  it("Partner setzt keinen Aufgabentitel", async () => {
    await seedTask(Date.now());
    await assertFails(updateDoc(doc(db(BOB), "couples", COUPLE, "tasks", "t1"), { title: "Filmabend" }));
  });
});

describe("Anzeigename des Partners", () => {
  beforeEach(async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, "users", BOB), { coupleId: COUPLE, displayName: "Bob" });
      await setDoc(doc(db, "users", EVE), { coupleId: "other", displayName: "Eve" });
    });
  });

  it("Mitglied liest den Profilnamen des Partners", async () => {
    await assertSucceeds(getDoc(doc(db(ALICE), "users", BOB)));
  });

  it("Fremder liest kein fremdes Profil", async () => {
    await assertFails(getDoc(doc(db(ALICE), "users", EVE)));
  });

  it("Niemand schreibt das Profil des Partners", async () => {
    await assertFails(updateDoc(doc(db(ALICE), "users", BOB), { displayName: "Hack" }));
  });
});

describe("Einladungen", () => {
  it("Mitglied erstellt Einladungscode für das eigene Paar", async () => {
    await assertSucceeds(
      setDoc(doc(db(ALICE), "invites", "ABC123"), { coupleId: COUPLE, createdBy: ALICE }),
    );
  });

  it("Fremder erstellt keinen Einladungscode für fremdes Paar", async () => {
    await assertFails(
      setDoc(doc(db(EVE), "invites", "XYZ789"), { coupleId: COUPLE, createdBy: EVE }),
    );
  });

  it("Mitglied löscht Einladungscode des eigenen Paares", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, "invites", "ABC123"), { coupleId: COUPLE, createdBy: ALICE });
    });
    await assertSucceeds(deleteDoc(doc(db(ALICE), "invites", "ABC123")));
  });

  it("Fremder löscht keine Einladung eines fremden Paares", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, "invites", "ABC123"), { coupleId: COUPLE, createdBy: ALICE });
    });
    await assertFails(deleteDoc(doc(db(EVE), "invites", "ABC123")));
  });
});
