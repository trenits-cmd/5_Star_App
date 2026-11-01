# 5 Star – Daily Synergy & Motivation

Two people track their daily goals together. Each person rates their own categories on a 5-star scale; the partner sees everything in real time.

## Features (v0.7.0)

- **Daily rating & Score:** 5-star input per category with weights (1–5). Score = `Σ(weight × stars) / Σ(weight × 5) × 100`. Unrated categories count as 0 stars.
- **Daily Target (< 80% → Penalty) & 7-Day Streak (7 days in a row ≥ 80% → Reward):**
  - Missing the daily 80% threshold immediately creates a **daily penalty** task for that day.
  - Achieving ≥ 80% for **7 consecutive days** unlocks a **reward** task.
- **Depleting Penalty & Reward Dropdown:**
  - The user chooses their penalty/reward via a **dropdown menu**.
  - Once a penalty is chosen, it **disappears from the dropdown** until all penalties in the pool have been used up, after which the cycle resets.
- **Partner Confirmation & Archive (Penalty Date + Redemption Date):**
  - Open penalties remain active until the partner explicitly confirms their redemption.
  - Upon confirmation, the task moves to the **Archive** (`Streak / Archive` tab), tracking both the **date of the penalty (`dayKey`)** and the **date of redemption (`redeemedDayKey`)**.
- **Category Images:**
  - Every category displays a meaningful illustration (`FOOD`, `WATER`, `ALCOHOL`, `FITNESS`, `HOME`, `MIND`, `DETOX`, `STAR`).
  - Existing categories automatically resolve a fitting icon by type and name, and any category's image can be customized in Settings.
- **Personalized Push Notifications per Category:**
  - Each category supports **one or multiple custom push notification messages** (`notifications: List<String>`), configurable when creating or editing a category.
  - Sent daily at the configured reminder time via `WorkManager` (plus an instant test button in Settings).
- **Personalized Categories, Penalties & Rewards (`targets`):**
  - Categories as well as catalog rewards/penalties can be assigned to **For me**, **For partner**, or **For both** – both when creating new entries and when editing existing ones.
- **Dedicated Tabs:**
  - **Today:** Daily score, own categories with images, counters (Alcohol/Water), partner summary, and open penalties/rewards.
  - **Streak / Archive:** Current 7-day streak progress, last 7 days overview, and the Archive of confirmed penalties/rewards with penalty date and redemption date.
  - **Partner:** Dedicated tab for the partner's live overview and open tasks (visible once paired).
  - **Settings:** Display name, category management (weight, image, push messages, target person), rewards/penalties catalog, reminder time & push test, sync status, sign out, and the **app version (`0.7.0`) at the very bottom** matching the release.
- **i18n (DE / EN / FR):**
  - Complete translations in German, English, and French; flag-based language switcher in the top right via AppCompat locale delegation.

## Tech Stack

- Android native, Kotlin, Jetpack Compose (Material 3).
- Firebase Auth (Google + Email/Password) and Cloud Firestore with persistent cache.
- Firestore security rules (pair membership, partner confirmation with redemption date).
- WorkManager for personalized per-category push notifications.
- Pure Kotlin scoring, 7-day streak, and depleting pool logic, tested with JUnit.
- Firestore rules tested with `@firebase/rules-unit-testing` and the Firestore emulator.

## Setup

- Firebase project: `starapp-99d8e`.
- Firestore database: `(default)` in `europe-west3`, Spark (free) tier.
- Auth providers: Google and Email/Password enabled.
- Android app registered in Firebase with SHA-1 `25:6E:F5:E2:8F:DE:F6:11:07:65:36:FA:8F:E1:D1:5A:11:E8:5D:99`.
- `app/google-services.json` must be present locally and is not committed.

## Build & Test

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- Firestore rules tests: `firebase emulators:exec --only firestore --project starapp-99d8e "cd rules-tests && npm test"`

## Releases

- **Latest: `v0.7.0`** – Daily penalty (< 80%) & 7-day streak reward, depleting penalty dropdown, partner confirmation with penalty & redemption dates in archive, category images, multiple personalized push notifications per category, full DE/EN/FR translations, app version `0.7.0` at the end of Settings.
- Previous: `v0.6.1`, `v0.6.0`, `v0.5.1`, `v0.5.0`, `v0.3.1`, `v0.3.0`, `v0.2.0`, `v0.1.0`.
