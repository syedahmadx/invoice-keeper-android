# Invoice Keeper (Android)

Pick a supplier invoice from your phone, let an AI model read it, have a second AI pass review
what it read, correct anything wrong, then post the confirmed JSON to your own webhook.

---

## The problem

Small business owners and freelancers receive supplier invoices as photos and PDF attachments.
Typing them into accounting software by hand is slow and error-prone, and the mistakes only
surface weeks later at reconciliation, when the person fixing them no longer remembers the
invoice.

## Target user

A freelancer or small business owner who already has an accounting workflow — Xero, QuickBooks, a
Make.com or Zapier scenario — and wants to stop keying invoices in by hand. On a phone the invoice
is usually already in the camera roll or arrived as a PDF attachment, so the app starts from the
system file picker rather than from a camera.

The app deliberately stops at the webhook. It does not try to be the accounting system; it
produces one clean, human-confirmed JSON object and hands it to whatever the user already runs.

---

## Architecture

Kotlin, Jetpack Compose, Material 3, MVVM with a repository layer.

```
com.example.invoicekeeper
├── MainActivity.kt                  edge-to-edge host, sets the theme and the nav graph
├── InvoiceKeeperApplication.kt      manual DI: one repository, built lazily
│
├── data/
│   ├── remote/
│   │   ├── ApiService.kt            Retrofit interface; @Url so the base URL is runtime-settable
│   │   ├── ApiClient.kt             OkHttp + Retrofit + kotlinx.serialization; TimeoutInterceptor
│   │   ├── Dto.kt                   request/response bodies, error envelope
│   │   ├── NetworkResult.kt         sealed result with typed failures
│   │   └── SafeApiCall.kt           folds HTTP codes and exceptions into NetworkResult
│   ├── local/
│   │   ├── InvoiceEntity.kt         Room entity + mapping to/from the domain model
│   │   ├── InvoiceDao.kt            Flow-returning queries
│   │   ├── AppDatabase.kt           Room database (schema exported to app/schemas/)
│   │   └── SettingsStore.kt         DataStore Preferences: webhook URL, base URL
│   └── repository/
│       └── InvoiceRepository.kt     the only seam between ViewModels and network/storage
│
├── domain/model/                    ExtractedInvoice, LineItem, ReviewFinding, SavedInvoice
│
├── ui/
│   ├── InvoiceKeeperApp.kt          NavHost + bottom navigation bar
│   ├── ViewModelFactory.kt          CreationExtras extensions for the app graph
│   ├── navigation/Destinations.kt   routes and the four top-level destinations
│   ├── theme/                       Color, SemanticColors, Type (tabular figures), Theme
│   ├── components/                  MessageCard, ErrorState, LoadingState, EmptyState,
│   │                                InvoiceCard, FindingRow, ProviderBadge, StatusChip
│   └── screens/
│       ├── home/                    HomeScreen + HomeViewModel
│       ├── scan/                    ScanScreen, ScanComponents, ScanUiState, ScanViewModel
│       ├── invoices/                InvoicesScreen, InvoiceDetailScreen + their ViewModels
│       └── settings/                SettingsScreen + SettingsViewModel
│
└── util/                            FilePicker (SAF + base64), Format (money, dates, URLs)
```

### Rules the code follows

- **No network code in composables.** Every screen has a ViewModel exposing a single immutable
  `UiState` through `StateFlow`. Composables read state and emit events; they never call the
  repository's network methods directly, and never touch Retrofit, Room or DataStore.
- **Repository as the only seam.** `InvoiceRepository` wraps the API service, the DAO and the
  settings store. ViewModels depend on it and nothing below it.
- **Coroutines in `viewModelScope`, IO on `Dispatchers.IO`.** `safeApiCall` and every Room write
  switch to `Dispatchers.IO` themselves, so no call site has to remember.
- **A sealed result with typed errors.** Every call returns
  `NetworkResult.Success | NoConnection | Timeout | Unauthorized | RateLimited | ServerError |
  InvalidRequest | Unknown`. Each failure carries a user-facing `message` and a `retryable` flag,
  so the UI knows whether a Retry button could actually help — a 401 will not fix itself, a 503
  might.
- **Per-call timeouts.** Extraction runs a vision model over a whole document and gets 60s;
  review, send and config get 30s. A `@Tag` on the Retrofit method carries the value and
  `TimeoutInterceptor` applies it, so one OkHttp client serves both.
- **State survives rotation** because it lives in the ViewModel, not in `remember`. The only
  things held in composition are scroll positions, which are `rememberSaveable`-backed already.

### Storage: Room, with the invoice kept whole

Saved invoices use **Room** (`app/schemas/` has the exported schema). The columns are exactly what
the list screen needs to search, filter, sort and total — supplier, invoice number, currency,
total, status, timestamp. The invoice body and the review findings are stored as their JSON
documents in two text columns rather than being shredded into child tables.

That is a deliberate choice, not laziness. The invoice is always read and written whole, it is
never queried field-by-field, and keeping it intact means the payload that reaches the webhook is
the same object that was confirmed on screen. A `line_items` table would add a join, a migration
surface and a mapping layer for no query that the app actually performs.

The webhook URL and base URL are two short strings read on several screens and written from one,
so they live in **DataStore Preferences**.

---

## The two AI features

Both run on the backend. The app labels each result with the `provider` string the API returned,
never with a name hardcoded in the app — if the backend switches models, the UI follows.

| Feature | Endpoint | What it does | Provider on the deployed backend |
|---|---|---|---|
| **Extraction** | `POST /api/extract` | Reads the image or PDF and returns structured invoice fields | Gemini |
| **Review** | `POST /api/review` | A second pass over the extracted data, returning findings by severity | Gemini |

The backend reports both provider names from `GET /api/config`, and Settings shows them.

Review findings come back as `error` / `warning` / `info` and are colour-coded accordingly. In
testing, an invoice whose subtotal and tax did not add up to the stated total produced:

```json
{"severity":"error","field":"total",
 "message":"Subtotal of 45 plus tax of 9 equals 54, but stated total is 60."}
```

**Editing any field after a review clears the findings**, and the UI says why: the findings
described the old numbers, and leaving them on screen next to changed figures would be worse than
showing nothing.

### AI transparency

- Every extracted panel and every findings list is labelled with the provider that produced it.
- A standing disclaimer sits directly under the extracted fields: this is a reading of a document,
  not a guaranteed fact, and every figure should be checked against the original before sending.
- The app computes its own arithmetic checks independently of the model — the live line-items
  total, quantity × unit price per row, and subtotal + tax against the stated total — so the user
  has a non-AI cross-check on the AI's output.

---

## How the proxy protects the keys

**This app contains no AI API key.** Not in source, not in resources, not in `BuildConfig`, not in
the APK.

The AI credentials are server-side environment variables on the Next.js companion app at
`https://invoice-keeper-web.vercel.app`. The Android app only ever talks to that base URL:

```
Android app  ──HTTPS──>  Invoice Keeper backend  ──key attached server-side──>  AI provider
```

`GET /api/config` reports only *whether* a key is configured (and which providers are in use). It
never returns a key, and the app has no field capable of holding one — `ConfigResponse` has no
key property at all. Settings surfaces this as a status line, deliberately phrased so it cannot be
mistaken for displaying a credential.

Verified on the built APK: the only external host string in the dex is
`invoice-keeper-web.vercel.app`, and the manifest declares only `android.permission.INTERNET`.

Also, because request bodies contain base64 document contents and supplier data, **there is no
logging interceptor** in the OkHttp stack and no request or file contents are logged anywhere.

---

## The screens

Four top-level destinations in a bottom navigation bar, plus a detail screen.

**Home** — the app name and what it is for, a prominent *Scan an invoice* button, and the three
most recent saved invoices as tappable cards. A warning card appears when no webhook is configured
that links straight to Settings. An empty state covers the first run, and a footer states plainly
what is sent where.

**Scan** — the core workflow, one scrolling screen with a four-dot step indicator:

1. **Pick a file** via `ActivityResultContracts.OpenDocument` filtered to `image/*` and
   `application/pdf`. No storage permission is requested, because SAF does not need one. The name,
   size and human-readable type are shown. Wrong types and files over 8 MB are rejected with a
   specific message, *before* the file is read into memory — an oversized file never becomes an
   8 MB base64 string.
2. **Extract**, disabled while any request is in flight so repeated taps cannot start two
   requests, with a loading indicator that says what is happening and how long it may take.
3. **An editable form** for every field, with line items in an add/remove list and a
   **live-recalculated line-items total** that turns red and offers a one-tap fix when it stops
   matching the subtotal. Per-row it flags when quantity × unit price disagrees with the amount,
   and below the totals it flags when subtotal + tax disagrees with the total. The panel is
   labelled with the provider name.
4. **Review**, showing findings colour-coded by severity and labelled with the provider. Editing
   afterwards clears them, with an explanation.
5. **Send to webhook**, disabled with a stated reason when it cannot run — no webhook configured
   (with a shortcut to Settings), a field that is not a number, or a missing required field. On
   success the invoice is saved with status *Sent* and the form locks.
6. **Save as draft** stores it without sending.

**Invoices** — every saved invoice with date, supplier, number, total and status. A search field
filters on supplier or invoice number, and a status filter narrows to drafts or sent. The count
and total are **grouped by currency and never summed across them**: two GBP invoices and one EUR
invoice show as two rows, because one combined number would be meaningless. Separate empty states
distinguish "nothing saved yet" from "nothing matches your filter". Tapping through opens a detail
screen with the full extracted data, the findings, the webhook response, delete with a
confirmation dialog, and send for drafts.

**Settings** — webhook URL with https validation and a save confirmation; *Send a test payload*,
which posts a small sample through `/api/send`; the backend base URL with a reset-to-default; a
server status line that calls `/api/config` and reports whether a key is configured and which
providers are in use, without ever displaying a key; a plain-language list of what is sent to the
AI provider and what is stored on the device; and *Clear all local data* behind a confirmation
dialog.

### States

Every screen that makes a request handles idle, validation error, loading, success, empty, no
connection, timeout, rate limited and API error. Retry is offered only where retrying could
succeed. Submit buttons are disabled while a request is running, and where a button is disabled
the reason is written next to it rather than left for the user to guess.

---

## Design

Material 3 with a hand-picked palette, no dynamic colour. A tool for handling money should look
the same on every device so its semantic colours always mean the same thing.

- **Neutral graphite paper** with a **single deep-teal accent** (`#0E6E68`) reserved for primary
  actions.
- **Semantic colours used only as semantics**: error red, warning amber, success green, info blue.
  Material 3 ships an error role but no warning/success/info, so those travel alongside the colour
  scheme in a `SemanticColors` CompositionLocal.
- **Tabular figures** (`fontFeatureSettings = "tnum"`) on every money value, so columns align
  regardless of which digits appear.
- Light and dark schemes, generous spacing, one card shape and one message-card shape reused
  everywhere.
- **Built for a narrow portrait phone.** The line-items editor is stacked, not tabular: a
  four-column table of editable fields is unusable at phone widths, so each item gets a full-width
  description row with quantity, unit price and amount sharing the row below it.

---

## Building the APK

Requires the Android SDK with platform 37 and a JDK 25 (Android Studio's bundled JBR works).

```bash
./gradlew assembleDebug
```

The APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Unit tests:

```bash
./gradlew testDebugUnitTest
```

### Toolchain note

This project uses AGP 9.4.0, which has **built-in Kotlin support**. The separate
`org.jetbrains.kotlin.android` plugin must not be applied — AGP rejects it with an explicit error.
The Compose, serialization and KSP plugins are applied normally.

---

## What was tested

**Backend contracts, probed directly against the deployed Vercel app:**

- `POST /api/extract` with a generated PNG invoice returned `{provider, data}` with correctly
  extracted supplier, tax ID, invoice number, dates, currency, three line items and totals —
  matching the DTOs exactly.
- `POST /api/review` returned `{provider, findings}` with an `error` finding for a deliberately
  broken total and a `warning` for a missing tax ID — matching the DTOs exactly.
- `POST /api/extract` with an empty body returned HTTP 400 `{"error":"...","kind":"request"}`,
  confirming the error-envelope key the app parses.
- `GET /api/config` returned `{"apiKeyConfigured":true,"extractionProvider":"Gemini",
  "reviewProvider":"Gemini"}` — **not** the `{"configured": boolean}` in the written spec. The DTO
  now accepts both shapes and additionally surfaces the provider names.

**Automated tests** — 22 unit tests, all passing:

- money and quantity formatting, lenient amount parsing (rejects text rather than silently
  producing zero), float-noise-tolerant money comparison, https and base-URL validation;
- invoice form round-tripping, blank optionals becoming null, live line-items recalculation,
  number-error and missing-field detection, currency normalisation, send gating and its stated
  reason, step-indicator progression;
- invoices filtering: currency grouping that never sums across currencies, case-insensitive search
  on supplier and number, status filtering, search and filter combined, and the distinction
  between "nothing saved" and "nothing matches".

**Build and APK:**

- `./gradlew assembleDebug` — BUILD SUCCESSFUL, zero compiler warnings, zero errors.
- `./gradlew lintDebug` — 0 issues (the four warnings the first run found were all fixed:
  a locale captured in a static `SimpleDateFormat`, a `modifier` parameter out of position, a
  redundant activity label, and two stale test dependencies).
- APK manifest declares only `android.permission.INTERNET` (plus AGP's debug-only
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`). No storage permission.
- Dex scan for `sk-ant-`, `sk-proj-`, `sk-<20+ chars>` and `AIza...` key patterns: no matches. The
  only external host in the dex is `invoice-keeper-web.vercel.app`.
- Source scan for `apiKey`, `api_key`, `Bearer`, `Authorization` and `BuildConfig.`: no matches.

**On device** — a full manual pass on a physical Infinix X6850 running Android 16 (API 36),
1080x2436 at 440dpi (393dp wide, the narrow-portrait target), in dark theme, against the live
backend:

- **Home** — empty state, the no-webhook warning card, bottom navigation.
- **Scan step 1** — the SAF picker opened with no permission prompt; picking a PNG showed
  `sample-invoice.png / PNG image - 24 KB` and ticked step 1.
- **Scan step 2** — Extract disabled with "Pick a file first." until a file was chosen; during the
  request the button greyed out and the spinner read "Reading the document with an AI model."
- **Extraction** — returned in about 20s, labelled with the provider badge **Gemini** taken from
  the API response. Supplier, tax ID, invoice number, both dates, currency, three line items and
  all totals were correct against the source document.
- **Editing** — changing line item 3's amount from 7.50 to 9.50 immediately turned the live
  line-items total red (177.00 to 179.00), surfaced "This does not match the subtotal below." with
  a one-tap **Use it**, and flagged the row with "Quantity times unit price is 7.50."
- **Review** — returned one `error` finding, colour-coded red and badged **Gemini**: "The sum of
  the line item amounts is 179, which does not match the stated subtotal of 177." It caught
  exactly the edit that had been made.
- **Findings cleared on edit** — tapping *Use it* set the subtotal to 179.00, cleared the findings
  and explained why ("You edited a field, so the previous findings were cleared. They described
  the old numbers."), while the independent arithmetic check raised the knock-on warning
  "Subtotal plus tax is 214.40, but the total says 212.40." All three severity colours (error,
  warning, info) rendered as intended.
- **Send gating** — with no webhook configured, *Send to webhook* stayed disabled throughout with
  "No webhook URL is configured. Add one in Settings." beside it and a Settings shortcut.
- **Save as draft** — saved and confirmed; the invoice appeared on Home under Recent, and in
  Invoices with the currency-grouped summary "1 invoice in GBP - 212.40" and a Draft chip.
- **Filtering** — the Sent filter showed "Nothing matches / No saved invoice has that status.",
  correctly distinct from the nothing-saved-yet state.
- **Detail** — full extracted data, both provider badges (Extract: Gemini, Review: Gemini), the
  source file name, the edited 9.50 line item, and the draft-only *Send to webhook* action. The
  bottom navigation is correctly hidden on this non-top-level screen.
- **Rotation** — rotated to landscape and back at Home, at file-picked, mid-edit, and on the
  detail screen. Every time the layout reflowed and all state survived: the picked file, the
  edited 9.50, the red 179.00, the row warning and the *Use it* action were all still there. No
  crash and the same process id throughout.
- **Backgrounding** — Home key from a half-filled Scan form, then relaunch: scroll position,
  edited values, warning card, cleared-findings notice and draft confirmation all restored.
- **Back navigation** — Back from the detail screen returned to Invoices with its filter state
  intact; Back from a tab returned to Home; Back from Home left the app to the launcher cleanly.
- **No `AndroidRuntime` exception was logged at any point during the entire pass.**
