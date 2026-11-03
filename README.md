# 5 Star – Daily Synergy & Motivation

Two people (e.g. **Rosie & Andi**) track their daily goals and relationship together. Each person rates their own categories and relationship dimensions on a star scale; the partner sees everything in real time.

## Features (v1.03112026_1351)

> **AI & Developer Code Directory:** See [`CODE_INDEX.md`](./CODE_INDEX.md) for the complete, AI-readable table of contents and explanation of all source files, domain formulas, and Firestore schemas.

- **Confirm today:**
  - A button at the end of the *Today* tab saves and syncs all of today's values.
  - If something is still missing, the app lists exactly which categories or relationship aspects still need an entry (e.g. *Drink water*, *Sport*, *Sex*).
  - The day is only marked as confirmed (with time) once everything is filled in.
  - More values can still be added after confirming. Each change is saved and synced immediately; press *Confirm again* to update the confirmation time.
  - A short confirmation toast appears after every saved entry and whenever *Save* is clicked across the app.

- **Profile & Partner Name:**
  - Each user chooses their own name (e.g. *Rosie* or *Andi*), used as a variable across all screens, tabs, score cards, target chips, and tasks, and automatically syncs the matching profile categories, rewards, and penalties.
  - Each user can choose their partner's name in Settings, displayed as a variable everywhere (partner cards, scores, tasks).
  - Each user can upload a **profile photo** from the gallery (cropped & scaled to a 128×128 avatar, synced via Firestore), shown as icon in headers, partner cards, and the Partner tab.
  - Each person must use **their own account**. Settings shows the signed-in account (*Signed in as …*) so that two phones signed in with the same account can be spotted.

- **Invite Partner via Email & Automatic Pairing:**
  - Invite the partner by **email** (generates or reuses the invitation code and opens the mail app with instructions).
  - During sign-up/login (or on the pairing screen), the partner enters this code; the system validates `/invites/{code}`, automatically adds the partner to `couples/{coupleId}.members`, sets the invite status to `confirmed`, and sends a push notification & toast to the inviter that the connection is established.
  - Remove/unpair the partner at any time in Settings (*Remove partner / Unpair (Test)*).

- **Daily scoring & 0 stars for missing days:**
  - If a day is not filled in, all categories show **0 stars**.
  - Entries for the previous day can still be added until **6:00 AM**.
  - A **daily reminder push** is sent at the configured time (default **22:00**, adjustable in Settings) if the day is not yet complete. The push message includes the category name.

- **Daily Status, Weekly Status & Relationship Mood (Dashboard):**
  - **Daily Status:** today's weighted score vs. the 80 % threshold (below 80 % triggers a daily penalty).
  - **Weekly Status:** current **7-day streak** (`X / 7 days in a row ≥ 80 %`) required to unlock a weekly reward.
  - **Relationship Mood:** the 5 relationship aspects are rated 0–5 stars, weighted equally, and the daily mood is shown separately on the Dashboard.

- **Categories, Weights & Scoring:**
  - Each category has a weight (1–5) that flows into the daily and weekly target.
  - **Rosie:** Drink water (3×, min. 2.5 L in glasses or decimal liters), Alcohol limit (2×, 0 L = 5 ★, 2.5 L benchmark, Demi 0.25 L / Pint 0.5 L + decimal L), Other tasks / alternating (1×: Tidy up apartment, Take out trash, Wash dishes, Cook meal, Plan lessons – Yes = 5 ★, No = 0 ★), Dinner before 20h (3×: <20h = 5 ★, <21h = 1 ★, else 0 ★; entered by Andi & tracked in both apps), Meals (3×: breakfast, lunch, dinner – nothing/little/medium/healthy; all healthy = 5 ★).
  - **Andi (3 categories only):** Sport (3×: 3 days/week at least 45 min each, sport time in minutes entered daily), Drink water (2×: min. 2.5 L in glasses or decimal liters, 2.5 L = 5 ★, proportionally rounded down below), Reading (2×: pages read entered daily, 0–5 pages = 0 ★, >50 pages = 5 ★, 6–50 proportional).
  - **Dinner before 20h:** time options are shown without brackets (e.g. "Before 20h", "Before 21h", "21h+").
  - Each category has a meaningful illustrated icon (food, water, alcohol, fitness, home, mind, detox, stars, relationship, reading).

- **Rewards & Penalties (managed in Partner tab):**
  - **Rosie Rewards:** Vinted Budget (10 € from Andi), Flowers (from Andi), Massage for 30 minutes, Eat out (choose restaurant), Cooking of choice (prepared by Andi).
  - **Rosie Penalties:** No orgasm (sex happens, but Rosie has no orgasm), No bar for two days, Phone handover (1h to Andi).
  - **Andi Rewards:** Cuddling for 30 minutes, Massage for 30 minutes, Edging, Blowjob, Handjob, Video, Favourite dish (Rosie cooks Andi's favourite meal).
  - **Andi Penalty:** Phone handover (1h to Rosie).
  - Each user's active rewards and penalties overview is displayed directly on their own dashboard tab and on the partner's dashboard tab (`CatalogOverviewCard`), rather than in Settings.
  - Penalties are chosen from a dropdown; a chosen penalty disappears until all penalties have been used. The pool resets every Sunday evening after the weekly review.
  - Penalty redemption must be confirmed by the partner before moving to the Archive. Tracked with **penalty date** (`dayKey`) and **redemption date** (`redeemedDayKey`).
  - **3 hours** after selection, the partner gets the push *"[Name] is waiting for your confirmation!"*.
  - **48 hours** without confirmation → automatic confirmation into the archive.

- **Archive (Streak / Archive tab):**
  - Archive shows confirmed penalties & rewards with penalty date and redemption date.
  - Each day is **expandable** to view all details.
  - If no values exist for a day, *"No entry available"* is shown.

- **Push Notifications:**
  - Global and per-category switches.
  - Default reminder time: **22:00** (adjustable in Settings).
  - Either a fixed daily time or an interval of 1, 2, 3 or 4 hours.
  - Quiet hours (default 23:00 – 09:00), adjustable globally.
  - Personalised messages per category; the reminder push includes the category name.

- **Settings (restricted):**
  - Profile photo & name, partner name, partner invite/remove, push notifications & reminder, quiet hours, sync & sign out, **app version (last item, matches release)**.
  - Quick Start preset switching buttons and the rewards/penalties overview have been removed from Settings (rewards & penalties are shown directly on the user and partner dashboard tabs).
  - No category creation/editing/deletion. No rewards/penalties management.

- **English only:** all UI text, push notifications, categories, rewards, and penalties are in English.

- **Tech Stack**

- Android native, Kotlin, Jetpack Compose (Material 3).
- Firebase Auth (Google + Passwordless Email & Invitation Code) and Cloud Firestore with persistent cache.
- WorkManager for push notifications, quiet hours and confirmation reminders.
- Pure Kotlin scoring (categories, meals, reading pages, alcohol/water, relationship mood, streak, rotation, pool logic), covered by JUnit tests.

## Build & Test

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- Firestore rules tests: `firebase emulators:exec --only firestore --project starapp-99d8e "cd rules-tests && npm test"`

## Releases

- **Latest: `v1.03112026_1351`** – Redesigned **Archive** day cards (`WeeklyScreen`): status chip, score rows with percentage, stars and progress bar per person, sectioned details (categories, counters, penalties & rewards), animated expand/collapse; Rosie's Daily Status percentage on Andi's view fixed (`PartnerTodayScreen`, `AppViewModel.partnerToday`, couple membership auto-repair).
- `v1.03112026_1255` – Safe **Delete account** button & confirmation dialog in Settings (wipes Firestore user/couple data, local profile store, and Firebase Auth account without crashing, followed by automatic sign-out); Daily Status percentage (`0%`–`100%`) and accurate category weight shares displayed on the Partner tab (`PartnerTodayScreen`).
- `v1.03112026_0235` – Weekly Status banner/card text formatting & centered multi-line layout (`DashboardScreen` & `WeeklyScreen`); Partner tab always visible in bottom navigation (`AppRoot`); `Streak / Archive` tab renamed to `Archive`; Remove Partner confirmation dialog and Sync Now status feedback; Sport & Reading counter step size set to 1.
- `v1.03112026_0115` – Password option removed from `LoginScreen`: entering only the **Email address + Invitation code** (or Google Sign-In) signs the user in, validates `/invites/{code}`, automatically pairs the couple, and notifies the inviter.
- `v1.03112026_0105` – Automatic partner pairing via invitation code during registration/login or pairing screen; rewards and penalties overview removed from `SettingsScreen` and moved to dedicated expandable `CatalogOverviewCard` sections on the user and partner dashboard views (`DashboardScreen` & `PartnerTodayScreen`).
- `v1.03112026_0055` – Persistent input saving in category cards (`ReadingCategoryCard`, `SportCategoryCard`, `CounterCard`/`LitersField`) via synchronous in-memory `localDayRecords` state in `AppViewModel`, focus-safe text state hoisting, `ImeAction.Done` / `onFocusChanged` commits, and dedicated *Save* buttons next to input fields.
- `v1.03112026_0045` – Penalty & reward confirmation fixed across all tabs (`Today`, `Partner`, `Streak / Archive`) and deployed to Firestore rules (`starapp-99d8e`); immediate daily penalty selection & confirmation when `< 80 %`; Today/Yesterday switcher until 06:00 AM; expandable 7-day archive cards in `WeeklyScreen` with *"No entry available"* when empty; AI-readable code directory [`CODE_INDEX.md`](./CODE_INDEX.md) added.
- `v1.03112026_0030` – Canonical role-based categories, rewards & penalties for Andi (Sport 3× with 3 days/week × 45 min daily time input, Drink water 2× min. 2.5 L, Reading 2× pages read + 7 Rewards & 1 Penalty) and Rosie (5 Categories + 5 Rewards & 3 Penalties).
- `v1.02112026_2240` – Quick Start preset buttons removed from Settings; automatic role-based sync & cleanup of categories, rewards, and penalties for Rosie & Andi; all remaining German push notifications and legacy catalog/snapshot titles translated to English; confirmation toast shown on every Save button click across all tabs; partner invite via code email & in-app button confirmation by inviter.
- `v1.02112026_1600` – Partner name chooseable by user & shown as variable; 0 stars for missing day; entries until 6:00 AM; daily reminder push at configurable time (default 22:00) with category name; "No entry available" in archive; expandable archive days; save confirmation toast; dinner time options without brackets; Settings: read-only rewards/penalties overview, no category CRUD, only push time changeable; app version as last Settings item matching release.
- `v1.02112026_1500` – English only, 2 partner-named tabs ([Rosie] [Andi] [Woche] [Einstellungen]), relationship tracker in partner tab (read-only + 7-day history), Settings restricted (no category CRUD, no rewards/penalties), reminder default 22:00, deadline 06:00 (skipped = 0 stars).
- `v1.02112026_1013` – "Heute bestätigen" button at the end of the Today tab with a list of still-missing entries, confirmation time stored per day and synced; further values can still be added.
- `v1.02112026_1000` – Partner avatar next to partner ratings (Dashboard and Relationship tab), signed-in account shown in Settings, Rosie's flower reward text updated.
- `v1.02112026_0949` – Alcohol benchmark 2.5 L, alternating tasks Ja/Nein (5 ★ / 0 ★), Andi's expanded rewards, Rosie's rewards and penalties, Andi's penalty to Rosie.
- Previous: `v0.7.0`, `v0.6.1`, `v0.6.0`, `v0.5.1`, `v0.5.0`, `v0.3.1`, `v0.3.0`, `v0.2.0`, `v0.1.0`.
