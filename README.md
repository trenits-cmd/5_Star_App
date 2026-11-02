# 5 Star – Daily Synergy & Motivation

Two people (e.g. **Rosie & Andi**) track their daily goals and relationship together. Each person rates their own categories and relationship dimensions on a star scale; the partner sees everything in real time.

## Features (v1.02112026_0949)

- **Profile & Names:**
  - Each user chooses their own name (e.g. *Rosie* or *Andi*), used as a variable across all screens, tabs, score cards, target chips, and tasks.
  - Each user can upload a **profile photo** from the gallery (cropped & scaled to a 128×128 avatar, synced via Firestore), shown as icon in headers, partner cards, and the Partner tab.
- **Invite Partner via Email & Remove Partner for Testing:**
  - Invite the partner by **email** (creates an invitation code and opens the mail app with a pre-filled message).
  - Remove/unpair the partner at any time in Settings (*Remove partner / Unpair (Test)*).
- **Daily Status, Weekly Status & Relationship Mood (Dashboard):**
  - **Daily Status:** today's weighted score vs. the 80 % threshold (below 80 % triggers a daily penalty).
  - **Weekly Status:** current **7-day streak** (`X / 7 days in a row ≥ 80 %`) required to unlock a weekly reward.
  - **Relationship Mood:** the 5 relationship aspects are rated 0–5 stars, weighted equally, and the daily mood is shown separately on the Dashboard.
- **Categories, Weights & Scoring:**
  - Each category has a weight (1–5) that flows into the daily and weekly target. Food is weighted highest.
  - **Rosie:**
    1. **Meals / Essen (3×):** breakfast, lunch and dinner each rated *nothing / little / medium / healthy*. All three *healthy* = **5 ★**, otherwise proportionally fewer.
    2. **Dinner before 20h (3×):** before 20:00 = **5 ★**, before 21:00 = **1 ★**, otherwise **0 ★**. The time is entered by **Andi** and synced in both apps.
    3. **Drink water (3×):** glasses (0.25 L each) and/or decimal liters are summed over the day. **2.5 L = 5 ★**, less proportionally below, rounded down.
    4. **Alcohol limit (2×):** counters for *Demi (0.25 L)* and *Pinte (0.5 L)* or decimal liters. **0 L = 5 ★**; above that, stars drop proportionally with **2.5 L as the benchmark** (`DEFAULT_ALCOHOL_LIMIT = 2.5`).
    5. **Alternating tasks (1×):** *Plan lessons* (3×/week), *Tidy up apartment*, *Take out trash*, *Cook meal*, *Wash dishes* (1×/week each), rotated automatically. Each task is answered **Ja = 5 ★** or **Nein = 0 ★** (no individual star rating).
  - **Andi:**
    1. **Sport (3×)**
    2. **Drink water (2×, ≥ 2.5 L)**
    3. **Reading / Lesen (2×):** number of pages. 0–5 pages = 0 ★, more than 50 pages = 5 ★, in between proportional (1–4 ★).
    4. **Dinner before 20h (3×, shared)**
- **Rewards (unlocked by a 7-day streak, confirmed by the partner, archived):**
  - **Rosie receives:** Vinted budget (10 € from Andi), flowers, 30-minute massage, eating out (restaurant of her choice), shared cooking of a dish she loves (prepared by Andi).
  - **Andi receives:** cuddling for 30 minutes, 30-minute massage, edging, blowjob, handjob, video, and a favourite dish cooked by Rosie.
- **Penalties (chosen from a dropdown by the person who has the penalty):**
  - **Rosie:** *no orgasm: sex happens, but Rosie has no orgasm*; *no bar for two days*; *phone handover: smartphone to Andi for one hour*.
  - **Andi:** *phone handover: smartphone to Rosie for one hour*.
  - A chosen penalty disappears from the dropdown until the whole pool is used up. The pool **resets every Sunday evening after the weekly review**.
- **Confirmation, Auto-Confirm & Archive:**
  - Rewards and penalties must be confirmed by the partner and then move to the **Archive** (tab *Streak / Archive*), with the **date of the penalty (`dayKey`)** and the **date of redemption (`redeemedDayKey`)**.
  - **3 hours** after a selected action, the partner gets the push *„Rosie wartet auf deine Bestätigung!“* (or the respective name).
  - **48 hours** without confirmation → automatic confirmation into the archive.
- **Relationship Tracker:**
  - Both partners rate each of the five aspects with 0–5 stars: *mood independent of relationship*, *mood within the relationship*, *communication*, *intimacy*, *sex*. Each aspect has the same weight.
- **Push Notifications:**
  - Global and per-category switches (`Switch`).
  - Either a fixed daily time or an interval of 1, 2, 3 or 4 hours (e.g. drinking water every 2 hours).
  - **Quiet hours** (default 23:00 – 09:00) for all notifications, adjustable globally.
  - Several personalised messages per category.
- **Category images:** illustrated icons for food, water, alcohol, fitness, home, mind, detox, stars, relationship and reading.
- **Full i18n (DE / EN / FR):** every text is translated. The language switch remains top right.
- **App version** is shown as the last item in Settings and matches the release tag.

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

- **Latest: `v1.02112026_0949`** – Alcohol benchmark 2.5 L, alternating tasks Ja/Nein (5 ★ / 0 ★), Andi's expanded rewards (edging, blowjob, handjob, video, favourite dish), Rosie's rewards and penalties, Andi's penalty to Rosie, full DE/EN/FR translations.
- Previous: `v0.7.0` (features as listed above, earlier revision), `v0.6.1`, `v0.6.0`, `v0.5.1`, `v0.5.0`, `v0.3.1`, `v0.3.0`, `v0.2.0`, `v0.1.0`.
