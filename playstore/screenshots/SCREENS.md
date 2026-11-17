# Screenshot set (6 phone shots)

Captured on-device from the real app on Krishnachura data — never mocked.
Order follows the store rule: hero → differentiator → depth.

| # | File | Screen | Caption (framed) |
|---|---|---|---|
| 1 | `raw-1-home.png` | Home hero, day in progress | Every room ready, at a glance |
| 2 | `raw-2-activity-offline.png` | Activity in airplane mode (status-bar icon visible) | Works without internet |
| 3 | `raw-3-rooms.png` | Rooms board, mixed states | Your whole floor, live |
| 4 | `raw-4-checklist.png` | Room detail, checklist mid-flow | Eight steps. Zero misses. |
| 5 | `raw-5-inspect.png` | Inspect with evidence photo | Inspect and pass in seconds |
| 6 | `raw-6-profile-bn.png` | Profile in Bengali | সম্পূর্ণ বাংলায় |

## How to capture

1. Connect the demo phone (`adb devices`), DND on, full brightness.
2. Fresh install, connect with the demo13 housekeeper key, sync (19 units).
3. Run `./capture-screenshots.sh` — it pauses before each shot so you can
   set up the screen, then pulls `raw-N-*.png` here at device resolution.
4. Frame them: `python3 frame-shot.py` — adds the ink caption band and
   normalizes every shot to 1080×1920 (`framed-N-*.png`, no alpha).
5. Upload `framed-*.png` to Play Console in numeric order.

Phone screenshots must be 16:9 or 9:16, 320–3840px per side, PNG/JPEG
without alpha. The framing script guarantees this.
