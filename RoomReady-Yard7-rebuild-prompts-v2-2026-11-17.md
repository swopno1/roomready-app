# RoomReady Yard #7 — Rebuild Prompt Pack v2 (10 prompts, graphics-first)

Founder direction, 2026-11-17: rebuild the app inside the Yard #7 window
(Tue Nov 17 00:00 → Sat Nov 21 00:00 Dhaka) with **~10 prompts**, same
product as the Nov 15–16 rehearsal build, but a visibly more professional
build — **branding and graphics are the core focus**, because Hackyard
judges see the presentation, not the code. Icon, logo, splash, UI/UX,
performance and security are all upgraded; nothing functional regresses.

## How to run this pack

- **R1 in Google AI Studio**, R2–R9 in Muse, **R10 in AI Studio**
  (final compatibility pass + push), same flow as before.
- Every prompt is self-contained. Paste the **Shared brief** at the top
  of each new builder session, then the prompt.
- One prompt at a time. Do not start the next until the current one's
  acceptance checks pass and are committed.
- Repo: the recreated `swopno1/roomready-android` (history starts inside
  the window). The old build is a private reference — re-implement, never
  copy files.

## Shared brief (paste before every prompt)

> RoomReady is a native Android app (Kotlin, Jetpack Compose) for resort
> housekeepers, remixed from my own Yard #6 offline housekeeping web app.
> It works fully offline and syncs with a live resort PMS (demo13,
> Krishnachura Retreat) through a housekeeping API.
>
> Non-negotiables:
> - Toolchain: AGP 9.1.1, Kotlin 2.3.21, compileSdk 36.1. No webfonts,
>   no external images, no network calls except PMS sync. Must run well
>   on a 2 GB RAM phone.
> - Flow: Dirty → Cleaning → Cleaned → Ready. PMS states map:
>   DIRTY = Dirty, CLEAN = Cleaned, INSPECTED = Ready.
>   `RoomUnit.status` (occupancy) is read-only in the app.
> - The 8-step turnover checklist, verbatim: change bed linen · fresh
>   towels & toiletries · vacuum & mop · bathroom deep clean · minibar
>   restock · AC/lights/sockets check · balcony tidy · final walkthrough.
> - No Demo Mode. First run is connect-first (PMS address + property API
>   key + staff name, or QR scan). Before any sync, every screen shows an
>   honest empty state. Inventory is NEVER static in shipped code — it
>   comes from the PMS bootstrap payload.
> - Navigation: 5-item bottom bar — Home · Rooms · Tickets · Activity ·
>   Profile.
> - Brand: ViveReply ink **#0F172A**; mint gradient **#00FF94 → #00EEDA**;
>   mint solid **#00F1CE**. Warm off-white surfaces (#FAF8F4 family).
>   Status colours are semantic and never change with branding:
>   Dirty = terracotta, Cleaning = amber, Cleaned = teal, Ready = mint.
> - Languages: English + Bengali, full parity, every string keyed.
> - API keys are secrets: Keystore-backed encrypted storage only, never
>   in logs, never in plain SharedPreferences, masked on screen.
> - After each prompt: run the tests and lint, report exact counts, and
>   commit with a clear message.

---

## R1 — AI Studio: scaffold + brand foundation + design system

Build the RoomReady Android project (Kotlin/Compose, toolchain per the
Shared brief) with the full visual foundation, before any feature logic:

1. **Design system first:** colour tokens (brand + semantic status
   colours), typography scale (large, low-literacy-friendly; body ≥16sp,
   room names ≥20sp), spacing/radius/elevation tokens, light theme only.
   All screens must consume tokens — no hard-coded colours anywhere.
2. **App icon — new original artwork, "Ready Door" evolved:** a white
   door slab slightly ajar with a mint check emerging, ink keyline, on
   the ink #0F172A field with a subtle mint gradient glow. Deliver the
   full adaptive icon set: foreground, background, and monochrome
   (themed-icon) layers, in all densities, inside the safe zone.
3. **Splash screen:** Android 12+ splash API — ink background, centred
   icon mark, no text flash, instant. Below API 31, a matching themed
   launch screen.
4. **Logo lockups:** in-app wordmark "RoomReady" (two capitals) drawn as
   vector: icon mark + wordmark, ink-on-light and white-on-ink variants.
5. **Shell:** 5-item bottom navigation (Home · Rooms · Tickets ·
   Activity · Profile) with branded icons and a mint active indicator;
   each tab shows a designed empty state (vector illustration built from
   Compose shapes — a simple door/room motif — plus one line of copy and
   one clear action). No placeholder-grey boxes anywhere.
6. Project hygiene from day one: `gradlew` executable bit set;
   `.gitignore` includes a bare `keystore.properties` pattern; README
   with the brand banner and one-paragraph product description.

Acceptance: project builds; icon renders correctly in circle, squircle
and square masks; splash shows icon on ink with no white flash; every
tab's empty state looks designed, not default.

## R2 — Muse: domain core, offline storage, secure key vault

Implement the engine under the R1 shell:

1. Room/room-type/unit models; housekeeping state machine
   Dirty → Cleaning → Cleaned → Ready with role rules (HOUSEKEEPER:
   start cleaning, complete checklist, finish; SUPERVISOR: inspect
   Cleaned → Ready, or send back to Dirty with a reason). Illegal
   transitions are impossible in the domain layer, not just hidden in UI.
2. Local persistence (Room DB): units, checklist progress, tickets,
   activity log, sync queue (pending mutations with retry order).
   Everything works with zero connectivity.
3. Ticket model: raise (room, category, note, photo ref), open → done
   lifecycle.
4. **Security:** the property API key and staff identity are stored in
   Keystore-backed encrypted storage; the key is never written to logs,
   analytics or the sync payload beyond the auth header; on-screen it is
   always masked except the last 4 characters.
5. Unit tests for the full state-machine matrix (every from×to×role
   combination) and the sync-queue ordering.

Acceptance: tests green with counts reported; lint 0 errors; airplane
mode on — the app performs every local action with zero network traffic.

## R3 — Muse: connect-first setup, QR pairing, PMS sync

Wire the app to the live PMS housekeeping API (already deployed):

1. **Setup screen:** PMS address (https only), property API key
   (format `rk.<keyId>.<secret>`), staff name. Validate by calling
   `GET /api/housekeeping/bootstrap` (contractVersion 1); on success,
   store credentials in the R2 vault and sync. Errors are plain-language
   (wrong key ≠ server down ≠ bad address — three different messages).
2. **QR pairing:** scan a code of the form
   `roomready://connect?v=1&url=<url-encoded origin>&key=<full key>`;
   any other shape is rejected with a clear message. Scanning fills
   address + key; the user only adds their staff name.
3. **Sync engine:** bootstrap returns room types, units (with PMS
   housekeeping state), staff roster, and the checklist. Parser rules —
   these are hard requirements, learned from live failures:
   a missing `housekeepingAt` field or an unparseable timestamp degrades
   to null and NEVER fails the sync; unknown enum values degrade to a
   safe default and are logged locally.
4. Outbound sync: status changes and tickets push through the sync
   queue; the PMS records the actor as `app:<keyId>:<staffName>`.
   A failed push (e.g. a 400) must not poison the queue — the bad entry
   is parked, flagged on the Activity screen, and the rest still sync.
5. **Replay proof test:** feed the real bootstrap payload shape through
   parser → snapshot writer → board query in a test. It must end with
   **5 room types / 19 units stored** (Terracotta Villa ×4, Bamboo
   Cottage ×6, Clay House Suite ×4, Palm Loft ×3, Earth Lodge ×2).
   These counts are the acceptance test, never shipped seed data.

Acceptance: replay test passes; a wrong key is refused cleanly;
"Last synced" timestamp and pending-count show on the Home screen.

## R4 — Muse: Home dashboard + Rooms board (the showcase screens)

These two screens carry the presentation. Build them to portfolio
quality:

1. **Home:** greeting header with the RoomReady lockup; a status hero
   card (ink background, mint gradient ring or bar showing the day's
   progress: X of Y rooms ready, counts per state as tappable chips);
   "needs attention" list (Dirty longest-waiting first); sync status
   pill (Synced · Pending n · Offline) that is honest at all times.
2. **Rooms board:** rooms grouped by type with a designed type header
   (gradient band + simple vector motif per type — villa, cottage,
   suite, loft, lodge); each unit card shows number/name, status with
   its semantic colour as a bold left rail + chip, time-in-state, and
   the primary action button for the user's role. Cards animate status
   changes (colour + subtle scale) — no jarring list jumps.
3. Filters: All · Dirty · Cleaning · Cleaned · Ready, as a segmented
   control with counts.
4. Every state designed: loading shimmer that matches card geometry,
   empty state (illustration + "All caught up"), error state with retry.

Acceptance: with the replay payload, the board shows all 19 units
grouped correctly; status changes animate; 1.3× font scale does not
break any card.

## R5 — Muse: cleaning flow + checklist + photo evidence

1. Room detail: big status header, Start Cleaning action; the 8-step
   checklist as large tappable rows with mint check animation and a
   progress bar; steps can be unchecked; progress survives process death.
2. Finish requires all 8 steps, then: attach a photo (CameraX, stored
   locally, compressed for a 2 GB device) OR skip-with-reason (a short
   preset list + free text). Finishing moves the room to Cleaned and
   queues the sync, with a short success moment (mint check animation +
   summary: room, time taken).
3. A room mid-cleaning shows elapsed time on the board (from R4 data).

Acceptance: full flow works in airplane mode; kill-and-reopen mid-
checklist restores progress exactly; finish without photo and without
reason is impossible.

## R6 — Muse: supervisor inspection + tickets

1. **Inspection (SUPERVISOR key):** Cleaned rooms queue for inspection;
   inspect screen shows the completed checklist and photo (or skip
   reason); Pass → Ready; Send back → Dirty with a mandatory reason,
   which the housekeeper sees on the room.
2. **Tickets:** raise from a room or the Tickets tab (category chips:
   plumbing, electrical, AC, furniture, other; note; optional photo);
   ticket cards carry status colour and age; mark done with one tap;
   tickets sync to the PMS and appear in its dashboard.
3. Role gating is real: a HOUSEKEEPER session never sees inspect
   actions (domain rule from R2, not just hidden buttons).

Acceptance: supervisor pass/send-back round-trips against the domain
tests; a ticket raised offline syncs when connectivity returns.

## R7 — Muse: Activity, Profile, notifications

1. **Activity:** the offline-proof screen — sync log (last synced time,
   pending count, parked/failed entries with reasons), and a plain-
   language "works without internet" panel listing what was done
   offline today. This screen is what the demo video shows in airplane
   mode; make it read beautifully.
2. **Profile:** staff identity (name, role from the key), property name,
   PMS address, masked key, Disconnect (wipes vault + local data after
   a confirm), language switch EN/বাংলা, app version.
3. **Notifications (local only):** a morning summary notification
   (rooms dirty at day start) and a "room waiting inspection" nudge for
   supervisors. No push, no server.

Acceptance: disconnect fully wipes credentials (verify vault empty);
language switch re-renders every screen with no missing strings.

## R8 — Muse: Bengali, accessibility, motion & graphics polish

A dedicated polish pass over every screen — this is the presentation
prompt:

1. Bengali localisation at 100% parity (239+ keys), natural housekeeping
   vocabulary; mixed EN/BN layout must not clip or overflow anywhere.
2. Accessibility: content descriptions on all icon actions, touch
   targets ≥48dp, contrast checked on every status colour pairing,
   1.3× and 1.5× font-scale passes.
3. Motion: shared-element-free, simple and smooth — status transitions,
   checklist checks, tab transitions, success moments. 60fps on a 2 GB
   device; no animation may block an action.
4. Visual consistency sweep: every screen uses the R1 tokens; spacing
   rhythm identical across tabs; empty/loading/error states all use the
   same illustration language.

Acceptance: a screen-by-screen checklist in the report — every screen
named, with BN render, font-scale and contrast confirmed per screen.

## R9 — Muse: performance, security hardening, full verification

1. Performance: cold-start measurement, board scroll with 19+ units,
   image memory audit; fix the top issues found. Budget: the app must
   feel instant on a 2 GB device; report before/after numbers.
2. Security review: key storage (vault only), no secrets in logs or
   in the repo, https-only networking (cleartext blocked), QR payload
   validation, exported components audit in the manifest.
3. Verification suite: state-machine matrix, checklist rules, ticket
   lifecycle, dashboard counts, sync merge matrix, parser tolerance
   (missing/unparseable fields), language completeness, airplane-mode
   transport silence. Write `VERIFICATION.md` in the repo root.
4. Live acceptance script for the founder (exact tap paths): connect
   with HOUSEKEEPER key → sync → 19 units → clean one room → PMS shows
   Cleaned with actor `app:<keyId>:<staffName>`; SUPERVISOR key →
   inspect → Ready → PMS shows INSPECTED; wrong-key refusal; ticket
   appears in PMS.

Acceptance: tests + lint green with counts; VERIFICATION.md complete;
no known high-severity issue open.

## R10 — AI Studio: release + presentation pack

Final compatibility pass in AI Studio, then produce everything the
hackathon actually judges:

1. Release build: signed AAB (founder provides the production keystore
   locally — never commit it or `keystore.properties`), version name/
   code set, `roomready_PlayStore_Details.md` written (title, short +
   full description EN, category, data-safety answers matching the app:
   name + app activity for Live mode sync).
2. **Screenshot set:** 6 phone screenshots from the real app on the
   Krishnachura data — Home hero, Rooms board, checklist mid-flow,
   inspection, Activity offline-proof (airplane mode visible), Profile
   in Bengali — framed consistently with ink captions.
3. **Feature graphic** (1024×500): ink field, Ready Door mark, wordmark,
   one line: "Every room ready." Plus the Play icon at 512.
4. **Demo video storyboard (60–90s), shot on the real app:** dirty room
   → one tap → checklist → photo finish → supervisor pass → Ready;
   airplane-mode moment on the Activity screen; live sync heartbeat
   (change appears in the PMS); end card with the lockup.
5. README finished: banner, what it is, the remix story, screenshots,
   setup, and the disclosure line — *"The PMS is my existing production
   product; this Android app was built during Yard #7 week and syncs
   with the PMS housekeeping API."* Submission writeup (verbatim,
   already approved): *"I remixed my own Yard #6 entry — RoomReady, an
   offline housekeeping web app — into a native Android app that syncs
   with a live resort PMS. Same soul, new body, and for the first time,
   a heartbeat."*
6. Push from AI Studio; confirm the public repo shows the banner,
   screenshots and README correctly.

Acceptance: AAB built and sized; all six screenshots + feature graphic
in the repo; storyboard delivered; repo page looks like a product
launch, not a homework submission.

---

## Schedule (Dhaka)

- **Tue Nov 17:** R1 (Studio) → R2. Gate: builds, vault + state machine green.
- **Wed Nov 18:** R3 → R4 → R5. Gate: replay proof 19 units; board is showcase-quality.
- **Thu Nov 19:** R6 → R7 → founder live acceptance (R9 §4 script, run early).
- **Fri Nov 20:** R8 → R9 → R10. Submit before Sat Nov 21 00:00; evening = buffer.

## What this pack deliberately keeps from the rehearsal

Same product, same contract, same flow — the rehearsal proved the
engineering. What changes: branding is built first instead of bolted on
(R1), the two showcase screens get their own prompt (R4), polish gets
its own prompt instead of leftovers (R8), and the presentation pack is
a deliverable, not an afterthought (R10).
