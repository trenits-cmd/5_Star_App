# 5 Star – Daily Synergy & Motivation

Two people (e.g. **Rosie & Andi**) track their daily goals and relationship together. Each person rates their own categories and relationship dimensions on a star scale; the partner sees everything in real time.

## Features (v1.02112026_1600)

- **Confirm today:**
  - A button at the end of the *Today* tab saves and syncs all of today's values.
  - If something is still missing, the app lists exactly which categories or relationship aspects still need an entry (e.g. *Drink water*, *Sport*, *Sex*).
  - The day is only marked as confirmed (with time) once everything is filled in.
  - More values can still be added after confirming. Each change is saved and synced immediately; press *Confirm again* to update the confirmation time.
  - A short confirmation toast appears after every saved entry.

- **Profile & Partner Name:**
  - Each user chooses their own name (e.g. *Rosie* or *Andi*), used as a variable across all screens, tabs, score cards, target chips, and tasks.
  - Each user can choose their partner's name in Settings, displayed as a variable everywhere (partner cards, scores, tasks).
  - Each user can upload a **profile photo** from the gallery (cropped & scaled to a 128×128 avatar, synced via Firestore), shown as icon in headers, partner cards, and the Partner tab.
  - Each person must use **their own account**. Settings shows the signed-in account (*Signed in as …*) so that two phones signed in with the same account can be spotted.

- **Invite Partner via Email & Remove Partner for Testing:**
  - Invite the partner by **email** (creates an invitation code and opens the mail app with a pre-filled message).
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
  - Each category has a weight (1–5) that flows into the daily and weekly target. Food is weighted highest.
  - **Rosie:** ... (unchanged)
  - **Andi:** ... (unchanged)
  - **Dinner before 20h:** time options are shown without brackets (e.g. "Before 20h", "Before 21h", "21h+").
  - Each category has a meaningful illustrated icon (food, water, alcohol, fitness, home, mind, detox, stars, relationship, reading).

- **Rewards & Penalties (managed in Partner tab):**
  - Rewards and penalties are shown in Settings as a **read-only overview** (cannot be created, edited, or deleted in Settings).
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
  - Profile photo & name, partner name, partner invite/remove, presets (Rosie & Andi), push notifications & reminder, quiet hours, sync & sign out, **app version (last item, matches release)**.
  - No category creation/editing/deletion. No rewards/penalties management.
  - Only category assignment (you / partner / both) and push reminder time can be changed.

- **English only:** all UI text is in English.

## Tech Stack

- Android native, Kotlin, Jetpack Compose (Material 3).
- Firebase Auth (Google + Email/Password) and Cloud Firestore with persistent cache.
- WorkManager for push notifications, quiet hours and confirmation reminders.
- Pure Kotlin scoring (categories, meals, reading pages, alcohol/water, relationship mood, streak, rotation, pool logic), covered by JUnit tests.

## Build & Test

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- Firestore rules tests: `firebase emulators:exec --only firestore --project starapp-99d8e "cd rules-tests && npm test"`

## Releases

- **Latest: `v1.02112026_1600`** – Partner name chooseable by user & shown as variable; 0 stars for missing day; entries until 6:00 AM; daily reminder push at configurable time (default 22:00) with category name; "No entry available" in archive; expandable archive days; save confirmation toast; dinner time options without brackets; Settings: read-only rewards/penalties overview, no category CRUD, only push time changeable; app version as last Settings item matching release.
- `v1.02112026_1500` – English only, 2 partner-named tabs ([Rosie] [Andi] [Woche] [Einstellungen]), relationship tracker in partner tab (read-only + 7-day history), Settings restricted (no category CRUD, no rewards/penalties), reminder default 22:00, deadline 06:00 (skipped = 0 stars).
- `v1.02112026_1013` – "Heute bestätigen" button at the end of the Today tab with a list of still-missing entries, confirmation time stored per day and synced; further values can still be added.
- `v1.02112026_1000` – Partner avatar next to partner ratings (Dashboard and Relationship tab), signed-in account shown in Settings, Rosie's flower reward text updated.
- `v1.02112026_0949` – Alcohol benchmark 2.5 L, alternating tasks Ja/Nein (5 ★ / 0 ★), Andi's expanded rewards, Rosie's rewards and penalties, Andi's penalty to Rosie.
- Previous: `v0.7.0`, `v0.6.1`, `v0.6.0`, `v0.5.1`, `v0.5.0`, `v0.3.1`, `v0.3.0`, `v0.2.0`, `v0.1.0`.
