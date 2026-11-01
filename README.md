# 5 Star – Daily Synergy & Motivation

Two people track their daily goals together. Each person rates their own categories on a 5-star scale; the partner sees everything in real time. When the daily threshold is reached or missed, a reward or penalty task is created for the affected person, which the partner confirms.

## Features

- **Daily rating:** 5-star input per category. Each person manages their own categories with weights (1–5).
- **Score:** `Σ(weight × stars) / Σ(weight × 5) × 100`. Unrated categories count as 0 stars.
- **Threshold:** 80% daily. Score ≥ 80% → reward task. Score < 80% → penalty task. Both cases are always created.
- **Tasks:** Reward/penalty tasks per affected person. Owner chooses an action from their own catalog; partner confirms. Without confirmation after 48 hours the task is auto-confirmed.
- **Partner visibility:** Partner sees categories, stars, score and open tasks. Connection shown as “Connected with …”. Offline: last known state is shown.
- **Partner tab:** Jeder Partner hat eigenen Tab mit seinen Bewertungen (Score, Kategorien, Counters, Offline-Stand).
- **History:** Past days with scores, task statuses (open, confirmed, auto-confirmed), time filters (7 days, 30 days, this year, all). Archived categories remain visible via snapshots.
- **Categories:** Owner-managed, weights 1–5, kind: `STARS`, `ALCOHOL`, `WATER`. Applies to me / partner / both.
- **Counters:** Alcohol: pint/demi counters with daily limit in liters; rating proportional to limit. Water: glasses + liters counter, daily goal in liters; rating proportional to goal.
- **Settings:** Category management, rewards/penalty catalog, reminder time (default 20:00), display name, sync status, manual sync.
- **Reminder:** Local notification via WorkManager; only if no rating exists for today.
- **Sync:** Firestore real-time listener + manual sync button; offline indication.
- **i18n:** Deutsch, English, Français; flag-based language switcher in top right; stored via AppCompat locale delegation.

## Tech Stack

- Android native, Kotlin, Jetpack Compose.
- Firebase Auth (Google + Email/Password) and Cloud Firestore with persistent cache.
- Firestore security rules (pair membership, task auto-confirm after 48h).
- WorkManager only for local reminder; sync is client-driven.
- Pure Kotlin scoring/threshold/confirmation logic, tested with JUnit.
- Firestore rules tested with `@firebase/rules-unit-testing` and the Firestore emulator.

## Setup

- Firebase project: `starapp-99d8e`.
- Firestore database: `(default)` in `europe-west3`, Spark (free) tier.
- Auth providers: Google and Email/Password enabled.
- Firestore rules deployed to firestore.
- Android app registered in Firebase with SHA-1 `25:6E:F5:E2:8F:DE:F6:11:07:65:36:FA:8F:E1:D1:5A:11:E8:5D:99`.
- `app/google-services.json` must be present locally and is not committed.

## Build & Test

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- Firestore rules tests: `firebase emulators:exec --only firestore --project starapp-99d8e "cd rules-tests && npm test"`

## Releases

- Latest: `v0.6.1` (counters in history and partner view, invite deletion limited to the couple).
- Previous: `v0.6.0` (version bump to 0.6.0, README in English, version visible in app), `v0.5.1` (i18n DE/EN/FR + all features), `v0.5.0`, `v0.3.1`, `v0.3.0`, `v0.2.0`, `v0.1.0`.

## Notes

- Weeks targets are intentionally removed; only the daily threshold applies.
- Sensitive categories (e.g. alcohol) are visible to the partner by design, but should be discussed at pairing time.

---

Language: English (this README).
