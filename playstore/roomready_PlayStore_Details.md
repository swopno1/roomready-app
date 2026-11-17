# RoomReady — Play Store Details

> Copy-paste source for the Play Console listing. Keep in sync with the app.

## Title (22/30 chars)

```
RoomReady Housekeeping
```

## Short description (76/80 chars)

```
Offline-first room turnover for resort housekeepers. Works without internet.
```

## Full description

```
RoomReady turns resort room turnover into a calm checklist: Dirty → Cleaning → Cleaned → Ready, with photos, maintenance tickets, and supervisor inspection built in.

BUILT FOR THE FLOOR, NOT THE DESK
• Works fully offline — every action queues and syncs when connectivity returns
• 8-step turnover checklist with photo evidence or skip-with-reason
• One-tap maintenance tickets (plumbing, electrical, AC, furniture) with photos
• Supervisor inspection: pass to Ready or send back with a reason
• Morning dirty-room summary and inspection reminders, all on-device

LIVE PMS SYNC
Connects to your property management system over encrypted HTTPS. Room inventory, statuses, and tickets stay in sync both ways; a sync log shows exactly what went through and what is still waiting.

ENGLISH + বাংলা
Full Bengali parity across every screen, designed for low-literacy-friendly operation with large type and clear status colours.

PRIVATE BY DESIGN
Your property API key lives in hardware-backed encrypted storage, is never logged, and travels only in the auth header. Local notifications only — no push, no tracking SDKs, no ads.

RoomReady is a companion for resort housekeeping teams. It requires a compatible property management system address and API key from your property manager.
```

## Category

Business

## Data safety answers

| Question | Answer |
|---|---|
| Does the app collect or share any user data? | Collects limited data required for its core function; shares nothing with third parties |
| Data collected | Name (staff name, for PMS sync attribution); App activity (room statuses, checklist completions, maintenance tickets — synced to the resort's own PMS only) |
| Is collected data shared with third parties? | No |
| Is data encrypted in transit? | Yes (HTTPS only; cleartext blocked at platform level) |
| Can users request data deletion? | Yes — Profile → Disconnect wipes the API key, identity, rooms, tickets, and photos from the device |
| Account required? | Yes — a property API key from the resort's property manager |
| Target audience | Resort housekeeping staff (workforce tool, not for children) |
| Ads / analytics / tracking SDKs | None |

## Graphic assets (in this folder)

| Asset | File | Spec |
|---|---|---|
| App icon | `icon-512.png` | 512×512, 32-bit PNG |
| Feature graphic | `feature-graphic.png` | 1024×500, no alpha |
| Phone screenshots (6) | `screenshots/framed-*.png` | 1080×1920, no alpha — captured on-device, see `screenshots/SCREENS.md` |
