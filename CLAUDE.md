# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working guidelines

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## Project

M3 Platform — an Android app (Kotlin, Jetpack Compose, Material 3) for an ICMR-funded malaria-surveillance project with ICMR–NIMR Goa. It collects questionnaire ("proforma") data from migrant workers and five professional roles (Clinician, Pathology, Health Worker, Research Lab, Contractor). Package: `com.varun.malariatracker`; minSdk 30, targetSdk 36.

**Questions and options must follow the NIMR proformas exactly.** Deviate only with explicit approval. Approved deviations so far: gender keeps "Other"; the worker form keeps a Govt ID number field (with 12-digit Aadhaar validation); proforma dropdowns without supplied value lists stay as typed fields.

## Commands

The shell's `JAVA_HOME` is invalid; use Android Studio's bundled JDK:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
./gradlew :app:compileDebugKotlin                     # fast compile check
./gradlew :app:assembleDebug                          # APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug                           # install on a running emulator/device
./gradlew :app:lintDebug                              # lint
./gradlew :app:testDebugUnitTest                      # unit tests
./gradlew :app:testDebugUnitTest --tests "com.varun.malariatracker.ExampleUnitTest"   # single test class
./gradlew :app:connectedDebugAndroidTest              # instrumented tests (needs a device)
```

Only placeholder example tests exist; behaviour is verified by running the app on the `Pixel_5` emulator.

AGP is pinned to **9.1.1** in `gradle/libs.versions.toml` because the installed Android Studio does not support 9.2.x.

Reading the app's CSVs from the emulator (plain `adb shell cat` and `run-as` are denied; the emulator image allows `su`):

```bash
adb shell "su 0 cat /storage/emulated/0/Android/data/com.varun.malariatracker/files/Download/Workers.csv"
```

Testing the database schema (no local MySQL; Docker is available):

```bash
docker run -d --name m3-schema-test -e MYSQL_ROOT_PASSWORD=test mysql:8.0
docker exec -i m3-schema-test mysql -uroot -ptest < database/schema.sql
```

## Architecture

**Navigation** uses Navigation Compose with string routes, in a single `NavHost` in `MainActivity`. Back is a real back stack (`popBackStack()`), and every page allows Back. `toLogin()` clears the stack and the session. Inside a multi-page form, `FormPager`'s own `BackHandler` steps back a page first; it takes priority because it is composed inside the destination. The top-left "← Back" calls the activity's `onBackPressedDispatcher`, so it behaves like the system Back button. Tapping outside a field clears focus, which closes the keyboard.

**Drafts (partial saving):** every form answer is held in SharedPreferences via `Drafts.kt` (`rememberDraft`, `rememberDraftSet`, `rememberDraftPage`), so answers survive Back, rotation and the app being closed. Draft keys:
- `worker:<phone>` — the labourer proforma
- `visit:<phone>` — a returning worker's Symptoms
- `pro:<phone>:<role>` — a professional form

A draft is cleared on final save, Cancel registration, or Clear form.

**Login** (`LoginScreen`) is by phone number. The phone is looked up in `Workers.csv` and `Professionals.csv`: no match starts registration, one match routes straight in, and more than one shows a "Sign in as…" chooser. Professionals register once via `ProfessionalRegistrationScreen`.

**Worker proforma** (page order: Demographics → Medical History → Travel History → Section V → Symptoms) has three states, stored in `Workers.csv` (`Phone,PatientID,Status,Name`):
- **Not registered:** fill Demographics; "Save & Register" after Q6 assigns the Patient ID and writes status `REGISTERED` plus the Q1–6 answers.
- **`REGISTERED`:** login resumes at the last screen reached. The back stack is rebuilt through the earlier routes, and Q1–6 are read-only.
- **`COMPLETE`:** set by "Save & Finish" on Symptoms. Until then, each screen stores its finished CSV row in the draft (`row:<File>`), and completion writes them all with `CsvHelper.upsertRow`. From then on, login goes straight to Symptoms, and each visit appends a row.

**Forms** are built from shared pieces in `FormComponents.kt`:
- `FormPager` splits a form into pages, with per-page validation and Next/Previous.
- `RadioGroup` and `CheckboxGroup` render options; multi-select state is a `Set<String>` so it works with `rememberSaveable`.
- `LabeledDropdown` is a read-only dropdown.
- `FormField` is a single-line text field whose keyboard Next key moves to the next field.
- Helpers: `withOther()`, `joinInOrder()`, `cleanPhone()`, `isValidPhone()`, `isValidAadhaar()`, `patientError()`.

On the professional forms the patient is identified by **mobile number**, which must belong to a registered worker (`patientError()`); the CSV stores both `PatientMobile` and the linked `PatientID`.

Professional forms wrap their content in `key(formKey)`. "Clear form" clears the draft and increments `formKey`, which resets every field. Their admin fields are pre-filled from the professional's registration.

Form conventions:
- Use the draft helpers for all form state.
- Questions that depend on a "Yes" answer are hidden otherwise, and their answers are saved blank, never stale.
- Multi-select answers are joined with `"; "`; "Other" text is saved as `"Main (text)"`.

**Storage is local CSV only; the app is not connected to a server yet.** `CsvHelper` writes files in the app-scoped external `Download` directory. `saveToCsv` appends a row; `upsertRow` replaces the row whose first column matches, or appends one. File headers for the worker proforma are constants in `CsvHelper`.
- `escapeCsv` quotes every field; `doesIdExist` unquotes before comparing.
- Index files `Workers.csv` (`Phone,PatientID,Name`) and `Professionals.csv` (`Phone,Role,Name,HealthCentre`) write their key columns first and unquoted, so lookups use a plain `split(",")`.
- A header is written only when a file is first created, so any column change requires deleting old CSVs (or reinstalling the app) before testing.
- Patient IDs are sequential (`P0001`, `P0002`, …), linked to the phone number. The 4-digit padding is a minimum width, not a cap.

**Database** (`database/schema.sql`): the MySQL 8 schema for the planned Cloud SQL backend. It has a single `users` table plus `user_roles` (a person can hold several roles); phone stored only in `users` (hashed for lookup, encrypted at rest); workers keyed by `patient_code` from the `next_patient_code` procedure; CHAR(36) UUID record IDs; `created_at`/`updated_at`/`created_by`/`deleted_at` (soft delete) on data tables.

`Models.kt` and `StakeholderHomeScreen.kt` are unused.

## Repository conventions

- Commit messages must not include a Claude co-author trailer.
- Active branch: `feature/phone-login-workflow`. `.idea/deploymentTargetSelector.xml` is intentionally left untracked.
