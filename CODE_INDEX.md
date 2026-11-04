# CODE_INDEX.md – AI-Readable Architecture & Code Directory (5 Star App)

> **Purpose for AI Agents & Developers:**  
> This document is the canonical, structured table of contents and architectural reference for the **5 Star – Daily Synergy & Motivation** Android application (`com.trenits.fivestar`, current release `v1.04112026_1432`, `versionCode = 53`). Keep this file updated whenever source files, UI structure, domain rules, or Firestore schemas change.

---

## 1. Repository & Build Overview

| Path | Role & Explanation |
|---|---|
| `settings.gradle.kts` | Gradle root configuration (`rootProject.name = "5_Star_App"`, module `:app`). |
| `build.gradle.kts` | Top-level Gradle plugins (`android.application`, `kotlin.android`, `kotlin.compose`, `google.services`). |
| `gradle/libs.versions.toml` | Version catalog defining AGP, Kotlin, Jetpack Compose BOM, Material 3, Firebase BOM (Auth + Firestore), Credential Manager, and WorkManager versions. |
| `app/build.gradle.kts` | App module configuration (`namespace = "com.trenits.fivestar"`, `compileSdk = 36`, `minSdk = 26`, `versionCode = 53`, `versionName = "1.04112026_1432"`). |
| `app/src/main/res/values/strings.xml` | Single-language (English) string resources for all tabs, cards, categories, rewards, penalties, dialogs, notifications, and status messages. |
| `firestore.rules` | Cloud Firestore security rules for `/users/{uid}`, `/invites/{code}`, and `/couples/{coupleId}` (`categories`, `catalog`, `days`, `tasks`). |
| `rules-tests/rules.test.mjs` | Node.js unit tests verifying `firestore.rules` against the Firebase Firestore Emulator. |
| `.github/workflows/build-apk.yml` | GitHub Actions CI workflow that restores `app/google-services.json`, runs `./gradlew assembleDebug`, and uploads the APK artifact. |

---

## 2. Source Code Directory (`app/src/main/java/com/trenits/fivestar/`)

### 2.1 Application Entry (`com.trenits.fivestar`)

| File | Key Symbols | Responsibility |
|---|---|---|
| `FiveStarApp.kt` | `FiveStarApp : Application` | Initializes Firebase Firestore persistent disk cache (`PersistentCacheSettings`) for offline-first support. |
| `MainActivity.kt` | `MainActivity : ComponentActivity` | Entry activity; hosts `AuthViewModel` and switches between `LoginScreen` (unauthenticated) and `AppRoot` (authenticated). |

---

### 2.2 Domain Layer (`com.trenits.fivestar.domain`) — Pure Kotlin Business & Scoring Logic

| File | Key Symbols | Responsibility |
|---|---|---|
| `Models.kt` | `Category`, `CategoryKind`, `CategoryIcon`, `MealLevel`, `RotationTask`, `RelationshipAspect`, `CatalogItem`, `waterStars`, `alcoholStars`, `dinnerStars`, `mealStars`, `readingStars`, `sportStars`, `sportDaysInWeek`, `counterStars`, `buildDayRecord`, `missingCategoryIds`, `missingRelationshipAspects`, `consecutiveSuccessDays`, `isWithinQuietHours`, `relationshipScorePercent`, `coupleRelationshipScorePercent` | Core domain models and pure scoring formulas:<br>• **Water (`WATER`):** Goal `2.5 L` (`GLASS_LITERS = 0.25` + decimal `LITERS`). `≥ 2.5 L = 5 ★`, below proportionally rounded down (`floor`).<br>• **Alcohol (`ALCOHOL`):** Benchmark `2.5 L` (`DEMI = 0.25 L`, `PINT = 0.5 L` + decimal `LITERS`). `0 L = 5 ★`, above proportionally fewer stars (`floor`), `≥ 2.5 L = 0 ★`.<br>• **Alternating Tasks (`ROTATION`):** `LESSON_PLANNING` (3×/week), `CLEAN_APARTMENT` (1×/week), `TAKE_OUT_TRASH` (1×/week), `COOKING` (1×/week), `WASH_DISHES` (1×/week). `Yes = 5 ★`, `No = 0 ★`.<br>• **Dinner before 20h (`DINNER_TIME`):** `< 20:00 = 5 ★`, `20:00–20:59 = 1 ★`, `≥ 21:00 = 0 ★`. Entered by Andi, synced across both apps.<br>• **Meals (`MEALS`):** Breakfast, Lunch, Dinner rated `NONE (0)`, `LITTLE (1)`, `MEDIUM (2)`, `HEALTHY (3)`. `9/9 (all healthy) = 5 ★`, else proportional (`floor`).<br>• **Reading (`READING`):** `0–5 pages = 0 ★`, `> 50 pages = 5 ★`, `6–50 pages = 1–4 ★` proportionally (step size `1`).<br>• **Sport (`SPORT`):** 3 days/week at least `45 min` each, daily minutes input (`SPORT_MINUTES`, step size `1`). `≥ 45 min = 5 ★`, `1–44 min` proportional (`floor`), logged rest days (`0 min`) up to 4 rest days/week = `5 ★`.<br>• **Relationship Tracker:** 5 equally weighted aspects (`MOOD_GENERAL`, `MOOD_RELATIONSHIP`, `COMMUNICATION`, `INTIMACY`, `SEX`), 1–5 ★ each. |
| `Score.kt` | `WeightedCategory`, `THRESHOLD_PERCENT = 80.0`, `weightedScorePercent`, `dayOutcome`, `weightSharePercent` | Computes weighted daily percentage (`0..100 %`) from category weights (`1..5`) and star ratings (`0..5`). `≥ 80 %` → `DayOutcome.REWARD`; `< 80 %` → `DayOutcome.PENALTY`. |
| `DayRecord.kt` | `DayRecord`, `CategorySnapshot`, `Counters` | Immutable snapshot of one user's daily category ratings, counter values, weighted score percent, and optional confirmation timestamp (`confirmedAt`). |
| `Evaluation.kt` | `Period`, `dayOutcomeFor` | Evaluates a `DayRecord` against active categories; missing days default to `0 ★` (`DayOutcome.PENALTY`). |
| `Task.kt` | `Task`, `TaskType` (`REWARD`, `PENALTY`), `TaskStatus` (`OPEN`, `CONFIRMED`, `AUTO_CONFIRMED`), `availablePoolOptions`, `penaltyCycleStartDayKey`, `needsPartnerConfirmationReminder`, `withAutoConfirmation` | Reward & Penalty lifecycle:<br>• Dropdown pool excludes already-used options in the current cycle until all options have been used.<br>• Penalty pool resets every Sunday evening after the weekly review.<br>• Partner confirmation reminder push triggers **3 hours** after selection (`needsPartnerConfirmationReminder`).<br>• Automatic confirmation (`AUTO_CONFIRMED`) triggers **48 hours** after creation (`withAutoConfirmation`).<br>• Tracks both creation/penalty date (`dayKey`) and redemption date (`redeemedDayKey`). |
| `Statistics.kt` | `StatsRange` (`WEEK`, `MONTH`, `THREE_MONTHS`, `ALL`), `StatsPoint`, `statsDays` (ALL starts at the earlier of account creation and first entry), `statsPoints` (averages), `statsSums` (sums for counts), `categoryPercentOf`, `overallPercentOf`, `relationshipPercentOf`, `taskCountOf`, `hasCategoryOfKind` | Pure logic for the Statistics tab: builds the calendar days of a period (missing days count as 0), aggregates them into chart points (1 day for week/month, 7 days for 3 months, 7 or 30 days for all time), and converts stars to % (5 ★ = 100 %). Category match uses exact id or kind prefix (`preset_<kind>_`). |
| `Pairing.kt` | `generateInviteCode`, `normalizeInviteCode` | Generates and normalizes 6-character alphanumeric invitation codes for connecting two partner accounts. |

---

### 2.3 Data & Persistence Layer (`com.trenits.fivestar.data`)

| File | Key Symbols | Responsibility |
|---|---|---|
| `AuthRepository.kt` | `AuthRepository` | Firebase Authentication via passwordless **Email + Invitation Code** (`signInWithEmailAndCode`) and **Google Credential Manager** (`GetSignInWithGoogleOption`). |
| `UserRepository.kt` | `UserRepository`, `UserProfile`, `InviteState`, `InvalidInviteException` | Manages `/users/{uid}` and `/invites/{code}` in Firestore:<br>• Automatic partner pairing: inviter creates/reuses code (and sends via email); invitee enters code during login (`LoginScreen`) or on `PairingScreen` (`requestJoin(code, email)`), which validates `/invites/{code}`, adds the partner to `couples/{coupleId}.members`, and sets `status = "confirmed"`. The inviter's app detects `status == "confirmed"`, sends a push notification & toast (`sendPartnerJoinedNotification`), and cleans up the invite.<br>• Supports `removePartner` for testing/unpairing. |
| `CoupleRepository.kt` | `CoupleRepository`, `Listed<T>` | Real-time Firestore listeners and writes for `/couples/{coupleId}` subcollections: `categories`, `catalog`, `days`, and `tasks` (including `confirmTask` and `autoConfirmTask` with `redeemedDayKey`). |
| `AppClock.kt` | `AppClock` (`now()`, `today()`, `setOffsetDays`) | Central clock for all app time. Standard = system time. Test-only date simulator: a stored day offset shifts all time (used by AppViewModel, DashboardScreen, StatisticsScreen and the reminder worker). Remove for release builds. |
| `LocalProfileStore.kt` | `LocalProfileStore` | Local `SharedPreferences` + `StateFlow` store for the user's own display name (`Rosie` / `Andi`), partner display name, Base64 avatar photos, and pending invitation code entered on `LoginScreen`. |

---

### 2.4 Background Reminders & Push Notifications (`com.trenits.fivestar.reminder`)

| File | Key Symbols | Responsibility |
|---|---|---|
| `Reminder.kt` | `ReminderPrefs`, `ReminderScheduler`, `ReminderWorker` (`checkOpenTasks`) | Background worker, runs **hourly even when the app is closed**: category reminders (water every 2 h etc.), 22:00 daily reminder, **3 h partner push** ("… is waiting for your confirmation!") and **48 h auto-confirm** of open penalties/rewards (`checkOpenTasks`). Quiet hours apply to all pushes. WorkManager timing is best-effort (Android Doze may delay). Old description: WorkManager periodic/daily scheduler for push notifications:<br>• Default daily reminder at **22:00** (configurable in Settings, or interval of 1–4 hours).<br>• Respects global quiet hours (default **23:00 – 09:00**).<br>• Sends category-specific reminder messages (including category name) when today's rating is incomplete, plus 3-hour partner confirmation reminders (*"[Name] is waiting for your confirmation!"*) and partner-joined notifications. |

---

### 2.5 UI & ViewModel Layer (`com.trenits.fivestar.ui`) — Jetpack Compose (Material 3)

| File | Key Symbols | Responsibility |
|---|---|---|
| `AppViewModel.kt` | `AppViewModel` | Central reactive state holder:<br>• Resolves the role (`main_user` = inviter, `partner`) via `resolveRole` and provides canonical categories (`canonicalMainUserCategories`, `canonicalPartnerCategories`) and canonical reward catalogs (`canonicalMainUserCatalog`, `canonicalPartnerCatalog`, sorted by tier 3 → 1).<br>• <br>• Maintains synchronous in-memory `localDayRecords` merged with Firestore `days` for immediate, race-free UI updates.<br>• Day cut-off: entries before **06:00 AM** belong to the previous day (`canEditYesterday`, `editingYesterday`, `activeDayKey`, `fillDeadline`).<br>• Reward tiers via `REWARD_TIER_BY_STREAK` (day 3 → category 3, day 5 → category 2, day 7 → category 1), counting restarts after each reward. Penalties are removed: `TaskType.REWARD` only, `DayOutcome.MISSED` replaces PENALTY.<br>• Dinner flow: `activeDinnerConfirmation`, `confirmDinner`, `rejectDinner` (partner enters time, main_user confirms/rejects).<br>• `syncNow()` flushes pending local day records, fetches fresh server snapshots (`Source.SERVER`) and updates `lastSync` / `syncStatus`.<br>• Emits `toastMessage` on every save action. |
| `AuthViewModel.kt` | `AuthViewModel` | Manages authentication state (`uid`), Google Sign-In, and passwordless Email + Invitation Code sign-in. |
| `AppRoot.kt` | `AppRoot`, `MainTabs` | Root navigation scaffold: top bar with app title only (the former sign-out icon is gone; signing out lives in Settings); global save-confirmation Toast collector; dinner-push focus (`NotificationTargets.dinnerDay` opens the Today tab and scrolls to the confirmation card); 5 permanently visible bottom navigation tabs: `[Name / Today]`, `[Partner / Partner]`, `[Week / Archive]`, `[Statistics]`, `[Settings]`. |
| `DashboardScreen.kt` | `DashboardScreen`, `PartnerTodayScreen`, `WeeklyStatusBanner`, `CatalogOverviewCard`, `TaskCard`, `SportCategoryCard`, `ReadingCategoryCard`, `MealsCategoryCard`, `DinnerTimeCard`, `RotationCategoryCard`, `CounterCard`, `ConfirmTodayCard` | • **Today Tab (`DashboardScreen`):** Header with avatar, Today/Yesterday toggle (until 06:00 AM), Daily Status card (`ScoreHero`, 80% target), centered multi-line `WeeklyStatusBanner` (7-day streak with explicit line break `\n`), 5-aspect Relationship Tracker (`RelationshipTodaySection`), role-assigned Category cards (step size `1` for Sport & Reading counters, focus-safe text inputs with `Save` buttons), Open Penalties & Rewards (`TaskCard` with dropdown selection + `Save selection` + `Confirm redemption`), expandable `CatalogOverviewCard` showing the user's active rewards & penalties, and `Confirm today` card listing any still-missing entries.<br>• **Partner Tab (`PartnerTodayScreen`):** Read-only partner score (`ScoreHero`), relationship tracker (`RelationshipTodaySection`), partner category summaries (`PartnerCategorySummaryCard`), open tasks for confirmation, and expandable `CatalogOverviewCard` showing the partner's active rewards & penalties. |
| `WeeklyScreen.kt` | `WeeklyScreen`, `ExpandableArchiveDayCard`, `ArchivedTaskCard`, `ArchiveRelationshipDetails`, `DayStatusChip` | **Archive Tab (`tab_week = "Archive"`):**<br>• Centered multi-line 7-day streak progress card (`R.string.week_streak` with `\n` line break).<br>• Last 7 days list, each day **expandable**: per-person category stars (`Categories of <name>`), per-person counter details (`Details of <name>` incl. bedtime), per-person relationship block (total % + 5 aspects), rewards of the day in the collapsed card, and the reward/task list with tier label.<br>• `DayStatusChip` colour follows `statusGradeColor` (green from 80 %, red→yellow gradient below).<br>• Confirmed rewards archive showing **day (`dayKey`)** and **redemption date (`redeemedDayKey`)** in a stacked layout. |
| `RelationshipScreen.kt` | `RelationshipScreen`, `RelationshipTodaySection` | Reusable 5-dimension relationship tracker section (`editable = true` on Today tab, `editable = false` on Partner tab) with couple average, partner avatar/ratings, and 7-day relationship history. |
| `SettingsScreen.kt` | `SettingsScreen` | **Settings Tab:**<br>• Signed-in account display, profile photo upload/crop (128×128 Base64), own name and partner name with `Save` buttons; partner info is **read-only**.<br>• Read-only category overview (structure is fixed, no create/edit/delete, no audience assignment) and read-only reward overview with tier days (reuses `CatalogOverviewCard`).<br>• Per-category push settings: switch + interval (1–12 h), global by default; quiet hours 23:00–09:00.<br>• Partner email invitation & *Remove partner / Unpair (Test)* button guarded by a confirmation `AlertDialog`.<br>• Global push switch, daily reminder time picker (default 22:00), interval selector and test push button.<br>• Sync section with *Sync now*, `syncStatus` feedback, *Sign out* and *Delete account* (confirmation `AlertDialog`, safe Firebase cleanup, automatic sign-out).<br>• App version (`BuildConfig.VERSION_NAME`) as the very last item. |
| `PairingScreen.kt` | `PairingScreen` | Initial couple connection screen: create invitation code, send code via email, or enter partner's code to automatically pair. |
| `LoginScreen.kt` | `LoginScreen` | Passwordless sign-in screen with **Email + Invitation code** (`Sign in with Email & Code`) and **Google Sign-In**. |
| `Format.kt` | `localizedCategoryName`, `localizedCategorySnapshotName`, `localizedCatalogTitle`, `translateCatalogTitle`, `UserAvatar`, `encodeAvatarUri`, `launchInviteEmail` | Formatting helpers, English localization of legacy German/French category/catalog titles, avatar bitmap cropping/encoding, and `mailto:` intent launcher. |
| `DinnerConfirmation.kt` | `DinnerConfirmation`, `needsWarning`, `withAutoConfirmation` | Dinner-time confirmation model: entered by the partner role, confirmed by main_user (48 h auto, 6 h warning). Stored in `couples/{id}/dinnerConfirmations/{dayKey}`. |
| `StatisticsScreen.kt` | `StatisticsScreen`, `StatsLineChart`, `StatsBarChart`, `StatsSummaryCard`, `StatsCountCard` | **Statistics Tab:** filter chips for period; filter for all, relationship (`FILTER_RELATIONSHIP`), penalties & rewards (`FILTER_TASKS`), or one category. Line chart drawn with `Canvas` (0–100 % y-axis, tap to select a point). Penalties & rewards use a grouped bar chart (counts per period, per person). Partner series is hidden if the partner has no such category. Summary cards: average, best day, days ≥ 80 % or reward/penalty totals. |
| `CounterSummary.kt` | `CounterSummary` | Compact read-only summary of counter values (Alcohol Demi/Pint/L, Water glasses/L, Rotation task Yes/No, Dinner time, Meals levels, Reading pages, Sport minutes). |
| `theme/Theme.kt` | `FiveStarTheme`, `Amber` | Material 3 color scheme and typography definitions. |

---

### 2.6 Unit Tests (`app/src/test/java/com/trenits/fivestar/domain/`)

| File | Coverage |
|---|---|
| `CounterTest.kt` | Water, Alcohol, Dinner time, Meals, Reading pages, Sport minutes, and Rotation task star calculations. |
| `DailyCheckTest.kt` | Missing category & relationship aspect detection and quiet hours evaluation. |
| `DayRecordTest.kt` | `DayRecord` construction and snapshot serialization. |
| `EvaluationTest.kt` | Daily outcome evaluation and consecutive 7-day streak calculation. |
| `ScoreTest.kt` | Weighted percentage calculation and 80 % threshold boundary tests. |
| `TaskTest.kt` | Reward/Penalty dropdown pool filtering, weekly Sunday reset, 3h reminder check, and 48h auto-confirmation. |
| `StatisticsTest.kt` | Period day ranges (week 7, month 30, 3 months → weekly buckets, all time from first record), bucket averages, missing days = 0 %. |

---

## 3. Canonical Role Profiles (roles `main_user` and `partner`)

- `main_user` = the person who created the invitation (stored as `presetRole` in `users/{uid}`). Template: sport, water, reading.
- `partner` = the invited person. Template: water, dinner, meals, alcohol, alternating tasks.
- Names are only taken from Settings. The role decides the template, not the name.

### 3.1 Role `partner`
- **Categories (5):**
  1. **Drink water (`3×`, `WATER`):** Min. `2.5 L` (glasses `0.25 L` or decimal `L`); `2.5 L = 5 ★`, below proportionally rounded down.
  2. **Alcohol limit (`2×`, `ALCOHOL`):** `0 L = 5 ★`, benchmark `2.5 L`, counters for `Demi (0.25 L)`, `Pint (0.5 L)`, and decimal `L`.
  3. **Other tasks (`1×`, `ROTATION`):** Alternating tasks (`Plan lessons` 3×/week; `Tidy up apartment`, `Take out trash`, `Cook meal`, `Wash dishes` 1×/week each). `Yes = 5 ★`, `No = 0 ★`.
  4. **Dinner before 20h (`3×`, `DINNER_TIME`):** `< 20h = 5 ★`, `< 21h = 1 ★`, else `0 ★`. Time entered by Andi, tracked in both apps.
  5. **Meals (`3×`, `MEALS`):** Breakfast, Lunch, Dinner (`nothing`, `little`, `medium`, `healthy`); all 3 `healthy = 5 ★`, else proportional.
- **Rewards (5):** Vinted Budget (10 € from Andi), Flowers (from Andi), Massage for 30 minutes, Eat out (choose restaurant), Cooking of choice (prepared by Andi).
- **Penalties (3):** No orgasm (sex happens, but Rosie has no orgasm), No bar for two days, Phone handover (1 hour to Andi).

### 3.2 Role `main_user`
- **Categories (3 only):**
  1. **Sport (`3×`, `SPORT`):** 3 days/week at least `45 min` each; sport duration in minutes entered daily (step size `1`).
  2. **Drink water (`2×`, `WATER`):** Min. `2.5 L` (glasses `0.25 L` or decimal `L`); `2.5 L = 5 ★`, below proportionally rounded down.
  3. **Reading (`2×`, `READING`):** Pages read entered daily (step size `1`); `0–5 pages = 0 ★`, `> 50 pages = 5 ★`, `6–50 pages = 1–4 ★` proportionally.
- **Rewards (7):** Cuddling for 30 minutes, Massage for 30 minutes, Edging, Blowjob, Handjob, Video, Favourite dish (Rosie cooks Andi's favourite meal).
- **Penalties (1):** Phone handover (1 hour to Rosie).
