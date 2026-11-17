# RoomReady Verification

How RoomReady is proven correct: the automated suite (what each area covers
and where), the performance and security audits, and the live acceptance
script the founder runs against the real PMS (demo13, Krishnachura Retreat).

Toolchain: AGP 9.1.1, Kotlin 2.3.21, compileSdk 36.1, minSdk 24.
Gates (separate commands, from the repo root):

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=$HOME/Library/Android/sdk \
  sh gradlew :app:testDebugUnitTest
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=$HOME/Library/Android/sdk \
  sh gradlew :app:lintDebug
```

Latest green: **298 tests, 0 failures** · **lint 0 errors / 46 warnings**
(45 since R3.1 plus one VectorRaster note on the new animated splash base —
288dp is the platform splash spec, same as the pre-existing splash mark).

## 1. Automated verification matrix

| Area | Tests | What it proves |
|---|---|---|
| State machine | `domain/HousekeepingStateMachineTest` (9), `domain/PmsMappingTest` (6) | Exhaustive from×to×role matrix in the domain layer; supervisor/housekeeper separation; no self-transitions; Ready terminal; Dirty→Cleaning→Cleaned→Ready only; PMS map DIRTY/CLEAN/INSPECTED; Cleaning reports as DIRTY |
| Checklist rules | `ui/RoomDetailTest` (8), `data/ChecklistDurabilityTest` (1), machine finish tests | All 8 verbatim steps render and toggle; finish requires 8/8; finish without photo AND without reason is impossible; progress survives process death (kill-and-reopen restores exactly) |
| Ticket lifecycle | `domain/TicketLifecycleTest` (4), `ui/TicketsInspectTest` (9), `data/pms/ServerTicketsTest` (5), `data/pms/TicketServerIdsTest` (2), `data/pms/TicketTitlesTest` (4) | Raise (room, category, note, photo ref) → open → done; one-tap done; server-id remap (PATCH uses the server id, never the local id); retired categories degrade to Other; reconcile is one-way OPEN→DONE with doneBy/doneAt preserved |
| Dashboard counts | `ui/ConnectedScreensTest` (4), `ui/ShowcaseScreensTest` (7), `ui/state/BoardActionsTest` (7) | 19 replay units grouped by type (4/6/4/3/2); hero progress X-of-Y; needs-attention ordering (Dirty longest-waiting first); honest sync pill; role-correct primary actions |
| Sync merge matrix | `data/OfflineEngineTest` (19), `data/sync/SyncParkTest` (9), `data/pms/SyncOrchestratorTest` (12), `data/pms/PmsOutboundTransportTest` (14), `data/pms/OutboundMapperTest` (13), `data/SyncRetryPolicyTest` (6), `data/SyncPayloadsTest` (8) | Queue ordering + retry order; poison entries park without blocking the drain; bootstrap preserves local state on code match or unsynced mutations; 401 stops the drain and wipes only the key; 403/404/422 park + flag; 409 parks + schedules a refresh (never blind-retried); legacy R3 entries translate or park visibly; exact wire shape per op (method, path, body field-for-field, no `actor` anywhere, `note` omitted when blank, `priority: "NORMAL"`) |
| Parser tolerance | `data/pms/BootstrapParserTest` (12), `data/pms/LenientTimestampsTest` (5), `data/pms/BootstrapReplayTest` (1), `data/pms/BootstrapLiveShapeTest` (4) | Missing/unparseable `housekeepingAt` degrades to null, never fails; unknown enums degrade to safe defaults with local warnings; malformed units/staff skipped, never fatal; contract mismatch fails typed; live demo13 shape (`unitNumber`, `unitStatus`, type `title`, absent staff/checklist) parses to friendly names with legacy-key fallbacks; replay proof ends at 5 types / 19 units from `bootstrap_replay.json` (test fixture only, never shipped) |
| Language completeness | `ExampleRobolectricTest` parity (3), `ui/BengaliTabsTest` (16), `notifications/NotificationStringsTest` (4) | 209 keys (200 strings + 9 plurals) with full EN+BN parity incl. format-arg match; every tab, setup, detail, inspect, ticket form, all 5 empty states, and both camera rationales render in Bengali (a missing string would crash the render) |
| Airplane silence | `data/OfflineEngineTest` airplane test | Every local action (start, steps, finish, inspect, tickets) runs with zero transport calls — local actions take no transport parameter, so network is structurally impossible |
| Offline proof UI | `ui/ActivityProfileTest` (7), `ui/state/SummarizeTodayTest` (4) | Sync log (last synced, pending, parked reasons), today's offline work counts, profile disconnect confirm flow |
| Wipe | `data/DataWipeTest` (1) | Disconnect empties vault (first), connection, ticket ids, photos, and all tables — verified, not assumed |
| Contrast | `ui/theme/ContrastTest` (5) | WCAG math over the real tokens: all text pairings ≥ 4.5, UI pairings ≥ 3.0 |
| Font scale | `ui/FontScaleTest` (24), `ui/ShowcaseScreensTest` 1.3× | All 5 tabs + setup + detail + inspect + ticket form + empty + both rationales compose at 1.3× and 1.5× |
| Vault masking | `security/ApiKeyMaskTest` (5), `data/photos/PhotoStoreTest` (5) | Last-4 masking incl. short/missing keys; photo compression bounds, traversal guard, pending-ref durability |
| Address + QR + key format | `data/pms/PmsAddressValidatorTest` (6), `data/pms/QrPairingParserTest` (6), `data/pms/ApiKeyFormatTest` (4), `data/pms/HttpPmsApiTest` (13), cleartext config test (1) | https-only (loopback exception needs an explicit test flag); QR shape `roomready://connect?v=1&url=&key=` else clear rejection; `rk.<keyId>.<secret>` format; bootstrap contractVersion 1; platform `network_security_config` blocks cleartext |
| Notifications | `notifications/NotificationLogicTest` (10) | Payloads stay silent with no work; alarm boundaries (later-today / tomorrow / exact-minute) |

## 2. Performance (R9 audit)

Budget: feel instant on a 2 GB device. Measured with a scratch file-backed
probe (deleted after use; timing assertions are not committed since CI
timing is unstable).

| Check | Before | After |
|---|---|---|
| Bootstrap parse (19 units, org.json) | ~2.2 ms | ~2.2 ms (no change needed) |
| Bootstrap import, file-backed DB | ~31–36 ms steady state (161 ms first run incl. schema) | ~17 ms steady state (131 ms first run) — `importBootstrap` now runs in one Room transaction instead of ~40 |
| Cold start | Fully lazy graph; no eager DB/vault/network at `Application.onCreate` | Same, plus: vault reads moved off Main in `ConnectionViewModel.refresh` (2 reads → 1) and `ProfileRoute` (Keystore unlock no longer janks first profile open) |
| Board scroll (19 units) | Keyed `LazyColumn`, single `observeAll` query, no N+1 | No change needed; 60-second clock tick is the only periodic recomposition |
| Images | Bounded by construction | Verified: capture downscales to 1280px/JPEG-70; worst transient peak ≈ 13 MB (two 1280² ARGB buffers during EXIF rotation, IO thread); thumbnails ≤ 512px (≈1 MB), one held per visible screen, decoded on IO and cached by ref; stored files ≈ 150–300 KB |

Deliberately not done: a Baseline Profile needs on-device macrobenchmark
runs, which this environment cannot do — noted as future work, not faked.

## 3. Security (R9 review)

- **Key storage:** API key + staff identity live only in
  `KeystoreCredentialStore` (MasterKey AES256-GCM, values AES256-GCM,
  keys AES256-SIV). Plain prefs hold URL/timestamps/property/server
  ticket ids — verified no key material. Key travels header-only
  (`Authorization: Bearer`), never in a payload (wire-shape tests
  assert no `actor`/key field anywhere).
- **Logs:** 4 `Log.w` calls, all fixed strings; error details are
  fixed strings + HTTP codes (`PmsError` doc: "log-only and never
  contain secrets"). No QR payload, URL query, or key is ever logged.
- **Repo:** sweep finds only fake fixture keys (`rk.k597.s3cret` and
  friends in tests). `debug.keystore` is gitignored. No `.jks`,
  no `google-services.json`.
- **Cleartext:** setup + QR both validate through
  `PmsAddressValidator` (https-only; the loopback exception requires an
  explicit flag no production caller passes). R9 adds
  `network_security_config.xml` with `cleartextTrafficPermitted=false`
  as platform-level defense in depth (matters on API 24–27).
- **QR validation:** scheme/host/version/fields all checked;
  non-RoomReady shapes, bad versions, missing fields, bad URLs, and bad
  keys each fail with their own message (6 parser tests).
- **Manifest:** exactly one exported component (`MainActivity`,
  launcher). All 3 notification receivers `exported=false`.
  No providers, services, WebViews, or FileProviders. Both
  PendingIntents `FLAG_IMMUTABLE`.
- **Backup:** vault (`roomready_credentials.xml`, filename verified),
  photo prefs, and both photo dirs are excluded from cloud backup,
  device transfer, and full backup.
- **Result: no known high-severity issue open.** One low note:
  security-crypto marks `MasterKey`/`EncryptedSharedPreferences`
  deprecated in 1.1.0 (already noted in code); migrate when AndroidX
  ships its replacement — not a vulnerability today.

## 4. Live acceptance script (founder, against demo13)

Use two real keys: a HOUSEKEEPER key and a SUPERVISOR key for
Krishnachura Retreat. Staff names below are examples — use real ones;
the PMS records the actor as `app:<keyId>:<staffName>`.

### A. Housekeeper: connect → sync → clean one room

1. Fresh install → Setup screen (connect-first, no demo).
2. Enter PMS address `https://<demo13-host>`, the HOUSEKEEPER key,
   staff name (e.g. `Ayesha`) → **Connect & sync**.
3. Expect: Home shows the day board; **Rooms** tab shows **19 units**
   grouped in 5 types (Terracotta Villa ×4, Bamboo Cottage ×6,
   Clay House Suite ×4, Palm Loft ×3, Earth Lodge ×2).
4. **Rooms** → tap a Dirty card → **Start cleaning** (header turns
   Cleaning, elapsed timer runs).
5. Tap all 8 checklist steps → **Finish** → **Take photo** (or
   **Skip with a reason** → pick a preset) → **Finish** → mint
   success moment, room is Cleaned.
6. **Profile** → **Sync now** (or wait for auto-sync).
7. **PMS check:** the room shows **Clean** with actor
   `app:<keyId>:Ayesha`. If a photo was taken, nothing photo-shaped
   left the device (only the skip reason travels, as `note`).

### B. Supervisor: inspect → Ready

8. **Profile** → **Disconnect** → confirm (vault + local data wiped;
   Setup returns).
9. Connect with the SUPERVISOR key, staff name (e.g. `Rahim`).
10. **Rooms** → tap the Cleaned card → **Inspect** → review the
    checklist + photo (or skip reason) → **Pass**.
11. Sync; **PMS check:** the room shows **INSPECTED** with actor
    `app:<keyId>:Rahim`.
12. Send-back path: inspect another Cleaned room → **Send back** →
    enter a reason (mandatory) → room returns to Dirty and the
    housekeeper sees the reason on the room.

### C. Wrong-key refusal

13. Disconnect; connect with a mistyped key → expect the plain
    wrong-key message ("That API key was refused…"), distinct from
    the server-down and bad-address messages. Nothing is stored.

### D. Ticket round-trip

14. Reconnect (either key). **Tickets** → **+** → pick room 101 →
    category **Plumbing** → note `Tap leaks` → **Raise ticket**.
15. Sync; **PMS check:** the ticket appears on the PMS dashboard for
    unit 101 titled `Plumbing`.
16. In-app, mark it done with one tap. Note: closing a ticket on the
    PMS needs a supervisor key — under a housekeeper key the app says
    so on Activity and keeps the local Done state.

### E. Offline proof (airplane mode)

17. Enable airplane mode. Start a cleaning, tick steps, raise a
    ticket — everything works.
18. **Activity** shows the pending count and today's offline work in
    plain language. Disable airplane mode, sync, and watch the log
    drain on the PMS side.

## 5. Known gaps (on-device only, not automatable here)

- Live PMS round-trip (this script), QR/camera capture feel, alarm +
  permissioned notification delivery, animation feel at 60 fps.
- DB migrations run trivially pre-release (zero installs) and are
  untested. BN copy is the agent's own — a native speaker should
  proof it before the demo video.
