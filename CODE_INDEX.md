# CODE_INDEX.md – AI-Readable Architecture & Code Directory (5 Star App)

> **Purpose for AI Agents & Developers:**  
> This document is the canonical, structured table of contents and architectural reference for the **5 Star – Daily Synergy & Motivation** Android application (`com.trenits.fivestar`). Keep this file updated whenever files, domain rules, or Firestore schemas change.

---

## 1. Repository & Build Overview

| Path | Role & Explanation |
|---|---|
| `settings.gradle.kts` | Gradle root configuration (`rootProject.name = "5_Star_App"`, module `:app`). |
| `build.gradle.kts` | Top-level Gradle plugins (`android.application`, `kotlin.android`, `kotlin.compose`, `google.services`). |
| `gradle/libs.versions.toml` | Version catalog defining AGP, Kotlin, Jetpack Compose BOM, Material 3, Firebase BOM (Auth + Firestore), Credential Manager, and WorkManager versions. |
| `app/build.gradle.kts` | App module configuration (`namespace = "com.trenits.fivestar"`, `compileSdk = 36`, `minSdk = 26`, `versionCode`, `versionName` matching the latest release `v1.[ddMMyyyy_HHmm]`). |
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
| `Models.kt` | `Category`, `CategoryKind`, `MealLevel`, `RotationTask`, `RelationshipAspect`, `CatalogItem`, `waterStars`, `alcoholStars`, `dinnerStars`, `mealStars`, `readingStars`, `sportStars`, `sportDaysInWeek`, `counterStars`, `buildDayRecord`, `missingCategoryIds`, `missingRelationshipAspects`, `consecutiveSuccessDays`, `isWithinQuietHours` | Core domain models and pure scoring formulas:<br>• **Water (`WATER`):** Goal `2.5 L` (`GLASS_LITERS = 0.25` + decimal `LITERS`). `≥ 2.5 L = 5 ★`, below proportionally rounded down (`floor`).<br>• **Alcohol (`ALCOHOL`):** Benchmark `2.5 L` (`DEMI = 0.25 L`, `PINT = 0.5 L` + decimal `LITERS`). `0 L = 5 ★`, above proportionally fewer stars (`floor`), `≥ 2.5 L = 0 ★`.<br>• **Alternating Tasks (`ROTATION`):** `LESSON_PLANNING` (3×/week), `CLEAN_APARTMENT` (1×/week), `TAKE_OUT_TRASH` (1×/week), `COOKING` (1×/week), `WASH_DISHES` (1×/week). `Yes = 5 ★`, `No = 0 ★`.<br>• **Dinner before 20h (`DINNER_TIME`):** `< 20:00 = 5 ★`, `20:00–20:59 = 1 ★`, `≥ 21:00 = 0 ★`. Entered by Andi, synced across both apps.<br>• **Meals (`MEALS`):** Breakfast, Lunch, Dinner rated `NONE (0)`, `LITTLE (1)`, `MEDIUM (2)`, `HEALTHY (3)`. `9/9 (all healthy) = 5 ★`, else proportional (`floor`).<br>• **Reading (`READING`):** `0–5 pages = 0 ★`, `> 50 pages = 5 ★`, `6–50 pages = 1–4 ★` proportionally.<br>• **Sport (`SPORT`):** 3 days/week at least `45 min` each, daily minutes input (`SPORT_MINUTES`). `≥ 45 min = 5 ★`, `1–44 min` proportional (`floor`), logged rest days (`0 min`) up to 4 rest days/week = `5 ★`.<br>• **Relationship Tracker:** 5 equally weighted aspects (`MOOD_GENERAL`, `MOOD_RELATIONSHIP`, `COMMUNICATION`, `INTIMACY`, `SEX`), 1–5 ★ each. |
| `Score.kt` | `WeightedCategory`, `THRESHOLD_PERCENT = 80.0`, `weightedScorePercent`, `dayOutcome`, `weightSharePercent` | Computes weighted daily percentage (`0..100 %`) from category weights (`1..5`) and star ratings (`0..5`). `≥ 80 %` → `DayOutcome.REWARD`; `< 80 %` → `DayOutcome.PENALTY`. |
| `DayRecord.kt` | `DayRecord`, `CategorySnapshot`, `Counters` | Immutable snapshot of one user's daily category ratings, counter values, weighted score percent, and optional confirmation timestamp (`confirmedAt`). |
| `Evaluation.kt` | `Period`, `dayOutcomeFor` | Evaluates a `DayRecord` against active categories; missing days default to `0 ★` (`DayOutcome.PENALTY`). |
| `Task.kt` | `Task`, `TaskType` (`REWARD`, `PENALTY`), `TaskStatus` (`OPEN`, `CONFIRMED`, `AUTO_CONFIRMED`), `availablePoolOptions`, `penaltyCycleStartDayKey`, `needsPartnerConfirmationReminder`, `withAutoConfirmation` | Reward & Penalty lifecycle:<br>• Dropdown pool excludes already-used options in the current cycle until all options have been used.<br>• Penalty pool resets every Sunday evening after the weekly review.<br>• Partner confirmation reminder push triggers **3 hours** after selection (`needsPartnerConfirmationReminder`).<br>• Automatic confirmation (`AUTO_CONFIRMED`) triggers **48 hours** after creation (`withAutoConfirmation`).<br>• Tracks both creation/penalty date (`dayKey`) and redemption date (`redeemedDayKey`). |
| `Pairing.kt` | `generateInviteCode`, `normalizeInviteCode` | Generates and normalizes 6-character alphanumeric invitation codes for connecting two partner accounts. |

---

### 2.3 Data & Persistence Layer (`com.trenits.fivestar.data`)

| File | Key Symbols | Responsibility |
|---|---|---|
| `AuthRepository.kt` | `AuthRepository` | Firebase Authentication via Email + Invitation Code (`signInWithEmailAndCode`, without a password prompt) and Google Credential Manager (`GetSignInWithGoogleOption`). |
| `UserRepository.kt` | `UserRepository`, `UserProfile`, `InviteState` | Manages `/users/{uid}` and `/invites/{code}` in Firestore:<br>• Automatic partner pairing: inviter creates/reuses code (and sends via email); invitee enters code during login/registration (`LoginScreen`) or on `PairingScreen` (`requestJoin(code)`), which validates `/invites/{code}`, adds the partner to `couples/{coupleId}.members`, and sets `status = "confirmed"`. The inviter's app detects `status == "confirmed"`, sends a push notification & toast (`sendPartnerJoinedNotification`), and cleans up the invite.<br>• Supports `removePartner` for testing/unpairing. |
| `CoupleRepository.kt` | `CoupleRepository`, `Listed<T>` | Real-time Firestore listeners and writes for `/couples/{coupleId}` subcollections: `categories`, `catalog`, `days`, and `tasks` (including `confirmTask` and `autoConfirmTask` with `redeemedDayKey`). |
| `LocalProfileStore.kt` | `LocalProfileStore` | Local `SharedPreferences` + `StateFlow` store for the user's own display name (`Rosie` / `Andi`), partner display name, Base64 avatar photos, and pending invitation code entered on `LoginScreen`. |

---

### 2.4 Background Reminders & Push Notifications (`com.trenits.fivestar.reminder`)

| File | Key Symbols | Responsibility |
|---|---|---|
| `Reminder.kt` | `ReminderPrefs`, `ReminderScheduler`, `ReminderWorker` | WorkManager periodic/daily scheduler for push notifications:<br>• Default daily reminder at **22:00** (configurable in Settings, or interval of 1–4 hours).<br>• Respects global quiet hours (default **23:00 – 09:00**).<br>• Sends category-specific reminder messages (including category name) when today's rating is incomplete, plus 3-hour partner confirmation reminders (*"[Name] is waiting for your confirmation!"*). |

---

### 2.5 UI & ViewModel Layer (`com.trenits.fivestar.ui`) — Jetpack Compose (Material 3)

| File | Key Symbols | Responsibility |
|---|---|---|
| `AppViewModel.kt` | `AppViewModel` | Central reactive state holder:<br>• Resolves role (`"rosie"` vs. `"andi"`) from profile/name and provides canonical categories (`canonicalRosieCategories`, `canonicalAndiCategories`) and canonical rewards/penalties (`canonicalRosieCatalog`, `canonicalAndiCatalog`).<br>• Supports editing yesterday's ratings until **06:00 AM** (`canEditYesterday`, `editingYesterday`, `activeDayKey`); fills missing yesterday records with `0 ★` after 06:00 AM (`fillDeadline`).<br>• Triggers daily penalty tasks when `< 80 %` and 7-day streak rewards when `7 consecutive days ≥ 80 %`.<br>• Emits `toastMessage` (`"Saved and synced."`) on every save action. |
| `AppRoot.kt` | `AppRoot`, `MainTabs` | Root navigation scaffold with dynamic partner-named bottom tabs (`[My Name]`, `[Streak / Archive]`, `[Partner Name]`, `[Settings]`) and global save-confirmation Toast collector. |
| `DashboardScreen.kt` | `DashboardScreen`, `PartnerTodayScreen`, `CatalogOverviewCard`, `TaskCard`, `SportCategoryCard`, `ReadingCategoryCard`, `MealsCategoryCard`, `DinnerTimeCard`, `RotationCategoryCard`, `CounterCard`, `ConfirmTodayCard` | • **Today Tab (`DashboardScreen`):** Header with avatar, Today/Yesterday toggle (until 06:00 AM), Daily Status card (80% target), Weekly Status banner (7-day streak), 5-aspect Relationship Tracker, role-assigned Category cards, Open Penalties & Rewards (`TaskCard` with dropdown selection + `Save selection` + `Confirm redemption`), expandable `CatalogOverviewCard` showing the user's active rewards & penalties, and `Confirm today` card listing any still-missing entries.<br>• **Partner Tab (`PartnerTodayScreen`):** Read-only partner score, relationship tracker, partner category summaries, open tasks for confirmation, and expandable `CatalogOverviewCard` showing the partner's active rewards & penalties. |
| `WeeklyScreen.kt` | `WeeklyScreen`, `ExpandableArchiveDayCard`, `ArchivedTaskCard` | **Streak / Archive Tab:**<br>• 7-day streak progress card.<br>• Last 7 days list where each day is **expandable** to inspect all category ratings, counters, and tasks (shows *"No entry available"* if a day has no data).<br>• Open penalties & rewards section for quick selection/confirmation.<br>• Archive of confirmed & auto-confirmed penalties and rewards showing both **penalty/reward date (`dayKey`)** and **redemption date (`redeemedDayKey`)**. |
| `SettingsScreen.kt` | `SettingsScreen` | **Restricted Settings Tab:**<br>• Signed-in account display, profile photo upload/crop (128×128 Base64), own name (`Rosie` / `Andi`) and partner name with `Save` buttons.<br>• Partner email invitation & test unpair button.<br>• Global push notification switch, daily reminder time picker (default 22:00), interval selector, quiet hours (23:00–09:00), and test push button.<br>• Sync status & Sign out.<br>• App version (`BuildConfig.VERSION_NAME`) as the very last item. |
| `PairingScreen.kt` | `PairingScreen` | Initial couple connection screen: create invitation code, send code via email, or enter partner's code to automatically pair. |
| `LoginScreen.kt` | `LoginScreen` | Passwordless sign-in screen with **Email + Invitation code** (`Sign in with Email & Code`) and **Google Sign-In**. |
| `Format.kt` | `localizedCategoryName`, `localizedCategorySnapshotName`, `localizedCatalogTitle`, `translateCatalogTitle`, `UserAvatar`, `encodeAvatarUri`, `launchInviteEmail` | Formatting, English localization of legacy German category/catalog titles, avatar bitmap cropping/encoding, and `mailto:` intent launcher. |
| `CounterSummary.kt` | `CounterSummary` | Compact read-only summary of counter values (Alcohol Demi/Pint/L, Water glasses/L, Rotation task Yes/No, Dinner time, Meals levels, Reading pages, Sport minutes). |

---

## 3. Canonical Role Profiles (Rosie vs. Andi)

### 3.1 Rosie
- **Categories:**
  1. **Drink water (`3×`, `WATER`):** Min. `2.5 L` (glasses `0.25 L` or decimal `L`); `2.5 L = 5 ★`, below proportionally rounded down.
  2. **Alcohol limit (`2×`, `ALCOHOL`):** `0 L = 5 ★`, benchmark `2.5 L`, counters for `Demi (0.25 L)`, `Pint (0.5 L)`, and decimal `L`.
  3. **Other tasks (`1×`, `ROTATION`):** Alternating tasks (`Plan lessons` 3×/week; `Tidy up apartment`, `Take out trash`, `Cook meal`, `Wash dishes` 1×/week each). `Yes = 5 ★`, `No = 0 ★`.
  4. **Dinner before 20h (`3×`, `DINNER_TIME`):** `< 20h = 5 ★`, `< 21h = 1 ★`, else `0 ★`. Time entered by Andi, tracked in both apps.
  5. **Meals (`3×`, `MEALS`):** Breakfast, Lunch, Dinner (`nothing`, `little`, `medium`, `healthy`); all 3 `healthy = 5 ★`, else proportional.
- **Rewards (5):** Vinted Budget (10 € from Andi), Flowers (from Andi), Massage for 30 minutes, Eat out (choose restaurant), Cooking of choice (prepared by Andi).
- **Penalties (3):** No orgasm (sex happens, but Rosie has no orgasm), No bar for two days, Phone handover (1 hour to Andi).

### 3.2 Andi
- **Categories (3 only):**
  1. **Sport (`3×`, `SPORT`):** 3 days/week at least `45 min` each; sport duration in minutes entered daily.
  2. **Drink water (`2×`, `WATER`):** Min. `2.5 L` (glasses `0.25 L` or decimal `L`); `2.5 L = 5 ★`, below proportionally rounded down.
  3. **Reading (`2×`, `READING`):** Pages read entered daily; `0–5 pages = 0 ★`, `> 50 pages = 5 ★`, `6–50 pages = 1–4 ★` proportionally.
- **Rewards (7):** Cuddling for 30 minutes, Massage for 30 minutes, Edging, Blowjob, Handjob, Video, Favourite dish (Rosie cooks Andi's favourite meal).
- **Penalties (1):** Phone handover (1 hour to Rosie).
