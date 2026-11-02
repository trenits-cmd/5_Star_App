# 5 Star – Daily Synergy & Motivation

Two people (e.g. **Rosie & Andi**) track their daily goals and relationship together. Each person rates their own categories and relationship dimensions on a 5-star scale; the partner sees everything in real time.

## Features (v0.7.0)

- **Profile Photo Icon & Dynamic Name Variables:**
  - Each user chooses their own name (e.g. *Rosie* or *Andi*), which is used dynamically as a variable across all screens, tabs, score cards, target chips, and tasks.
  - Each user can upload a **photo of themselves** from the gallery (cropped & scaled to a 128×128 avatar icon, synced via Firestore) displayed in headers, partner cards, the Relationship Tracker, and the Partner tab icon.
- **Invite Partner via Email & Remove Partner for Testing:**
  - Invite the partner directly via **email** (creates an invitation code and opens the email client with pre-filled invitation message).
  - Disconnect/remove the partner at any time for testing purposes via Settings (*Remove partner / Unpair (Test)*).
- **Separate Daily Status & Weekly Status Display:**
  - **Daily Status Banner:** Shows today's weighted score vs. the 80% daily threshold (missing < 80% triggers a daily penalty).
  - **Weekly Status Banner:** Shows the current **7-day consecutive streak** (`X / 7 days in a row ≥ 80%`) required to unlock a weekly reward.
- **Tailored Categories, Weights, Rewards & Penalties for Rosie & Andi (1-Click Presets in Settings):**
  - **Rosie's Categories:**
    1. **Meals / Essen (`CategoryKind.MEALS`, 3× weight):** Breakfast, Lunch, and Dinner are each rated as *nothing (nichts)*, *little (wenig)*, *medium (mittel)*, or *healthy (gesund)*. All 3 *healthy* = **5 stars**, otherwise proportionally fewer stars.
    2. **Dinner before 20h (`CategoryKind.DINNER_TIME`, 3× weight):** **Before 20:00** = 5 stars · **Before 21:00** = 1 star · **Otherwise** = 0 stars. Time is entered by **Andi** and synced in **both apps**.
    3. **Drink Water (`CategoryKind.WATER`, 3× weight, Goal ≥ 2.5 L):** Tracked throughout the day in **glasses (0.25 L each)** and/or **decimal liters** (`waterLiters`). At **2.5 L** = 5 stars, below = proportionally fewer stars rounded down.
    4. **Alcohol Limit (`CategoryKind.ALCOHOL`, 2× weight):** Tracked via **Demi (0.25 L)**, **Pint (0.5 L)** counters, or **additional decimal liters** (`alcoholLiters`). At **0 L** = 5 stars, above = proportionally fewer stars.
    5. **Alternating Tasks (`CategoryKind.ROTATION`, 1× weight):** *Plan lessons (Unterricht planen, 3×/week)* alternating with *Tidy up apartment*, *Take out trash*, *Cook meal*, and *Wash dishes* (1×/week each).
  - **Andi's Categories:**
    1. **Sport (`CategoryKind.STARS`, 3× weight)**
    2. **Dinner before 20h (`CategoryKind.DINNER_TIME`, 3× weight, shared)**
  - **Rosie's Rewards (unlocked at 7-day streak):**
    - *Vinted Budget: Rosie gets 10 € from Andi*
    - *Flowers (Blumen)*
    - *Massage for 30 minutes*
    - *Eat out: choose the restaurant*
  - **Rosie's Penalties:** *no orgasm (kein Orgasmus)*, *no bar (keine Bar)*
  - **Andi's Penalty:** *Cooking*
- **Depleting Penalty & Reward Dropdown + Archive (Penalty Date + Redemption Date):**
  - Chosen penalties disappear from the dropdown until all penalties in the pool have been used up.
  - Open penalties/rewards must be confirmed by the partner to move into the **Archive** (`Streak / Archive` tab), recording both the **date of the penalty/reward (`dayKey`)** and the **date of redemption (`redeemedDayKey`)**.
- **Relationship Tracker (Beziehungstracker):**
  - Both partners rate their relationship daily on a 1–5 star scale across **5 dimensions**:
    1. **Mood independent of relationship** (*Stimmung unabhängig von Beziehung*)
    2. **Mood within the relationship** (*Stimmung in der Beziehung*)
    3. **Communication** (*Kommunikation*)
    4. **Intimacy** (*Intimität*)
    5. **Sex** (*Sex*)
- **Category Images & Personalized Push Notifications (Switch, Interval & Quiet Hours):**
  - Expressive vector illustrations (`FOOD`, `WATER`, `ALCOHOL`, `FITNESS`, `HOME`, `MIND`, `DETOX`, `STAR`, `cat_relationship`).
  - **Toggle Switches (`Switch`):** Enable or disable push notifications globally as well as individually per category.
  - **Daily Time or Multi-Hour Interval:** Configure either a fixed daily reminder time (e.g. `20:00`) or a repeating interval (**every 1, 2, 3, or 4 hours** – e.g. every 2 hours for drinking water).
  - **Global Quiet Hours (Ruhepause, default `23:00 – 09:00`):** No push notifications are sent during the configurable quiet window (`isWithinQuietHours`).
  - Multiple custom push notification messages per category (`notifications: List<String>`) + instant test button in Settings.
- **Full i18n (DE / EN / FR) & App Version (`0.7.0`) at the very bottom of Settings.**

## Tech Stack

- Android native, Kotlin, Jetpack Compose (Material 3).
- Firebase Auth (Google + Email/Password) and Cloud Firestore with persistent cache.
- WorkManager for personalized per-category push notifications.
- Pure Kotlin scoring, meals, relationship index, 7-day streak, rotation, and depleting pool logic, tested with JUnit.

## Build & Test

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- Firestore rules tests: `firebase emulators:exec --only firestore --project starapp-99d8e "cd rules-tests && npm test"`

## Releases

- **Latest: `v0.7.0`** – Email partner invite & test unpair, profile photo avatar upload, dynamic user name variables, separate daily & weekly status cards, Meals category (breakfast/lunch/dinner: nothing, little, medium, healthy), Rosie & Andi categories/weights/rewards/penalties, Relationship Tracker (5 dimensions), 7-day streak reward, depleting penalty dropdown, archive with penalty & redemption dates, category images & multi-message push notifications, full DE/EN/FR translations.
- Previous: `v0.6.1`, `v0.6.0`, `v0.5.1`, `v0.5.0`, `v0.3.1`, `v0.3.0`, `v0.2.0`, `v0.1.0`.
