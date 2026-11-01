# 5 Star – Daily Synergy & Motivation

Two people (e.g. **Rosie & Andi**) track their daily goals together. Each person rates their own categories on a 5-star scale; the partner sees everything in real time.

## Features (v0.7.0)

- **Priority & Weight Concept (1–5):**
  - Categories are sorted by priority and display both their priority tier and their exact **percentage share of the daily score** (`weightSharePercent`):
    - **Weight 5 – Top Priority / Food (`WEIGHT_CRITICAL`):** Eating regularly / Dinner before 20h has the strongest impact on the score.
    - **Weight 4 – Very Important (`WEIGHT_VERY_HIGH`):** Alcohol limit.
    - **Weight 3 – Important (`WEIGHT_IMPORTANT`):** Drink water (≥ 2.5 L) & Alternating tasks.
    - **Weight 2 – Daily / Alternating (`WEIGHT_DAILY`):** Everyday tasks.
    - **Weight 1 – Minor / Bonus (`WEIGHT_MINOR`):** Small bonus habits.
- **Tailored Categories for Rosie & Andi (with 1-Click Presets in Settings):**
  1. **Dinner before 20h (`CategoryKind.DINNER_TIME`, Weight 5, Both Partners):**
     - **Before 20:00** = 5 stars · **Before 21:00 (20:00–20:59)** = 1 star · **21:00 or later** = 0 stars.
     - The time is entered by **Andi** and automatically synced and rated in **both apps** in real time.
  2. **Drink Water (`CategoryKind.WATER`, Weight 3, Goal ≥ 2.5 L):**
     - Progress is tracked throughout the day via **glasses (0.25 L each)** and/or **decimal liters** to accumulate a daily total (`waterLiters`).
     - At **2.5 L or more** → 5 stars; below 2.5 L → proportionally fewer stars, rounded down to whole stars (`floor(5 × liters / 2.5)`).
  3. **Alcohol Limit (`CategoryKind.ALCOHOL`, Weight 4):**
     - Tracked via **Demi (0.25 L)**, **Pint (0.5 L)** counters, or **additional decimal liters** (`alcoholLiters`).
     - **0.0 L** = 5 stars; above 0 L → proportionally fewer stars (rounded down) up to the limit.
  4. **Alternating Weekly Tasks (`CategoryKind.ROTATION`, Weight 3):**
     - Rotating pool so not everything has to be done every day:
       - *Plan lessons (Unterricht planen)*: **3× per week** (most frequent)
       - *Tidy up apartment (Wohnung aufräumen)*: **1× per week**
       - *Take out trash (Müll runterbringen)*: **1× per week**
       - *Cook meal (Kochen)*: **1× per week**
       - *Wash dishes (Geschirr spülen)*: **1× per week**
     - Tasks whose weekly quota is already met step back automatically so the remaining weekly tasks are suggested and selected in alternation.
- **Daily Target (< 80% → Penalty) & 7-Day Streak (7 days in a row ≥ 80% → Reward):**
  - Missing the daily 80% threshold triggers a **daily penalty** task.
  - Achieving ≥ 80% for **7 consecutive days** unlocks a **reward** task.
- **Depleting Penalty & Reward Dropdown:**
  - The user chooses their penalty/reward via a **dropdown menu**.
  - Once a penalty is chosen, it **disappears from the dropdown** until all penalties in the pool have been used up, after which the cycle resets.
- **Partner Confirmation & Archive (Penalty Date + Redemption Date):**
  - Open penalties remain active until the partner explicitly confirms their redemption.
  - Upon confirmation, the task moves to the **Archive** (`Streak / Archive` tab), tracking both the **date of the penalty (`dayKey`)** and the **date of redemption (`redeemedDayKey`)**.
- **Category Images & Personalized Push Notifications:**
  - 8 expressive vector illustrations (`FOOD`, `WATER`, `ALCOHOL`, `FITNESS`, `HOME`, `MIND`, `DETOX`, `STAR`).
  - Each category supports **one or multiple custom push notification messages** (`notifications: List<String>`), sent daily via `WorkManager` (plus an instant test button in Settings).
- **Full i18n (DE / EN / FR):**
  - All UI texts, category presets, alternating tasks, and priority tiers are translated into German, English, and French; standard category names translate automatically when switching languages.
- **App Version in Settings:**
  - `versionName = "0.7.0"` is displayed as the very last item at the bottom of Settings, matching the GitHub release.

## Tech Stack

- Android native, Kotlin, Jetpack Compose (Material 3).
- Firebase Auth (Google + Email/Password) and Cloud Firestore with persistent cache.
- Firestore security rules (pair membership, partner confirmation with redemption date).
- WorkManager for personalized per-category push notifications.
- Pure Kotlin scoring, 7-day streak, rotation, and depleting pool logic, tested with JUnit.

## Build & Test

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- Firestore rules tests: `firebase emulators:exec --only firestore --project starapp-99d8e "cd rules-tests && npm test"`

## Releases

- **Latest: `v0.7.0`** – Priority weight concept (Food = 5), Rosie & Andi categories (Water 2.5 L in glasses + decimal liters, Alcohol 0 L = 5★ with pint/demi/decimal liters, Alternating weekly tasks with Lesson planning 3×/week & household 1×/week, Dinner before 20h entered by Andi & synced to both apps), 7-day streak reward, depleting penalty dropdown, archive with penalty & redemption dates, category images & multi-message push notifications, full DE/EN/FR translations.
- Previous: `v0.6.1`, `v0.6.0`, `v0.5.1`, `v0.5.0`, `v0.3.1`, `v0.3.0`, `v0.2.0`, `v0.1.0`.
