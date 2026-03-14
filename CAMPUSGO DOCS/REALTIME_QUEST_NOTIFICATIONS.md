# Real-time notifications (quests, store, new quests)

Yes, **real-time (or near–real-time) notifications are possible** for this app. This doc covers quest progress, stage unlock, **new store items**, and **new quests that target the user (or have no target group)**.

---

## 1. Scenarios

### A. Participant proceeded (from “waiting for result” to next stage / winner)

- **When:** After submit on an **elimination** stage, the backend sets `awaiting_ranking: true`. When ranking runs, the participant gets `outcome`: `advanced` | `eliminated` | `completed`.
- **Today:** The app shows “Waiting for results” but does **not** poll in AR; the comment in `ArActivity` says “the backend will send a push notification” — that is not implemented yet.
- **Goal:** As soon as the backend resolves ranking, the user should be notified so they can leave AR or see “You advanced!” / “You were eliminated.” / “You won!” without manually refreshing.

### B. New stage unlocked for the user’s quest

- **When:** The current stage is **locked** by `stage_start` (e.g. “Stage 2 opens at 14:00”). When that time passes, the backend considers the stage unlocked (`stage_locked: false`).
- **Goal:** Notify the user when the next stage is open so they know they can go to the next location / open the quest again.

### C. New items in the store

- **When:** An admin adds a new store item, or an existing item becomes available (e.g. its `start_date` is reached, or it is made visible). The user currently only sees new items when they open the Store screen or refresh.
- **Goal:** Notify the user that new items are available so they can open the Store (e.g. “New reward in the store!”).

### D. New quests that can target the user (or no target group)

- **When:** A new quest is approved and is **available to this user**: it passes target-group and enrollment rules (see [quests.md](api-docs/quests.md)) — e.g. quest has no target group (everyone), or the user’s course/year/section is in the target group, and the quest is upcoming/ongoing and joinable. The user currently only sees new quests when they open Discover / Quests or refresh.
- **Goal:** Notify the user that a new quest they can join is available (e.g. “New quest: Campus Hunt is open!”).

---

## 2. Implementation options

### Option 1 — In-app only (no backend push)

**What:** While the app is in the foreground, **poll** the existing APIs so the UI updates without the user leaving the screen.

- **Awaiting ranking:** When the UI shows “Waiting for results”, start polling **GET /api/participants/{participant}/status** every 5–10 seconds (as in [quests.md](api-docs/quests.md)). When `awaiting_ranking === false`, call **GET .../play** and update the card (advanced / eliminated / winner).
- **Stage unlock:** When the UI shows “Next stage opens at {time}”, poll **GET .../play** on an interval (e.g. every 30–60 seconds, or once when `next_stage_opens_at` has passed). When `stage_locked === false`, refresh and show the new stage / location.
- **New store items:** On Home or Store (or a background worker), periodically call **GET /api/store**. Compare `items.size` or item IDs with the last fetched list; if there are new items, show an in-app badge or a small “New items available” banner and refresh the list.
- **New quests:** Periodically call **GET /api/quests**. Compare `quests.size` or quest IDs with the last fetched list; if there are new quests the user can join, show a badge on the Quests/Discover tab or a “New quests available” hint.

**Pros:** No new backend work, no FCM.  
**Cons:** Only works while the app is open; no notification if the user leaves or closes the app.

**Where to add polling:**

- **ArActivity:** When `resolveAndShowOutcome` leaves the user in `AwaitingRanking`, start a coroutine that polls `QuestsRepository.getStatus(participantId)` every 5–10 s until `!awaitingRanking`, then call `getPlayState` and run the same outcome logic (advanced / eliminated / winner). Optionally also poll when showing “Stage opens at …” for stage unlock.
- **MyQuestDetailScreen / ViewModel:** When `playState.status === "awaiting_ranking"` or `playState.stageLocked === true`, start a refresh/poll loop (or use a timer keyed to `next_stage_opens_at`) so the screen updates when ranking is resolved or the stage unlocks.
- **Home / Store / Quests:** Optional background or tab-focused refresh: e.g. when Home or Store is visible, refetch **GET /api/store** every N minutes and compare item count; when Quests/Discover is visible, refetch **GET /api/quests** and compare; show a badge or snackbar when “new” items/quests appear.

---

### Option 2 — Push notifications (FCM) — true real-time, works when app is closed

**What:** Use **Firebase Cloud Messaging (FCM)**. The backend sends a push when:
1. Ranking is resolved for a participant (advanced / eliminated / completed), and/or  
2. A stage unlocks for a quest the user is participating in (e.g. when `stage_start` has passed), and/or  
3. **New store items** are added or become available (new item created, or item’s `start_date` reached / made visible), and/or  
4. **New quests** become available to the user (quest approved and targets this user or has no target group; first stage not yet ended).

The user gets a system notification even when the app is in the background or closed; tapping it can open the app (e.g. to quest detail, AR, Store, or Discover).

**Requirements:**

- **App:** Add Firebase + FCM dependency, register for FCM token, send the token to your backend (e.g. after login), handle incoming messages and show a notification (and optional deep link to quest/AR/Store/Quests).
- **Backend:** Store FCM token per user (or per device).  
  - When ranking is resolved for a participant → send push to that user.  
  - When a stage unlock time passes → send push to all participants in that quest.  
  - When a new store item is added or becomes available → send push to all users (or to users who have the app / opted in).  
  - When a new quest is approved and available (targets specific users or everyone) → send push to those users (e.g. resolve target group and enrollment, then send to matching users’ FCM tokens).

**Pros:** Real-time, works when app is backgrounded or closed.  
**Cons:** Backend must integrate FCM (token storage + send API); you need a Firebase project and config in the app.

**High-level app steps:**

1. Add `com.google.firebase:firebase-messaging` (and optionally `firebase-bom`) in `app/build.gradle.kts`.
2. Add `google-services` plugin and `google-services.json` from Firebase Console.
3. Create a `FirebaseMessagingService` that:
   - Overrides `onNewToken` and sends the token to your API (e.g. `PUT /api/me/fcm-token`).
   - Overrides `onMessageReceived`: show a `NotificationCompat` and set a PendingIntent that opens the app. Use a **type** or **action** in the payload (e.g. `quest_result`, `stage_unlock`, `new_store_items`, `new_quests`) and open the right screen: quest detail/AR (`quest_id` / `participant_id`), Store, or Quests/Discover.
4. In your auth flow (e.g. after login), call `FirebaseMessaging.getInstance().token` and upload it to the backend.
5. Backend: when any of the four events above occur, call FCM HTTP v1 API to send to the appropriate user(s)’ token(s).

---

### Option 3 — Hybrid (recommended)

- **In-app:** Add polling in **ArActivity** (and optionally My Quest Detail) for awaiting-ranking and stage-unlock. Optionally refresh Store and Quests lists when the user is on Home/Store/Quests and show a “new items” or “new quests” badge when the list grows.
- **Push:** Add FCM for when the user has left the app or is on another screen, so they get “Results are in”, “Stage 2 is open”, “New items in the store”, or “New quest available” and can tap to open the right screen.

---

## 3. Summary

| Scenario                         | In-app only (polling)        | Push (FCM)                                    |
|---------------------------------|------------------------------|-----------------------------------------------|
| Proceed from awaiting_ranking   | Yes — poll GET .../status    | Yes — backend sends push on resolve           |
| New stage unlocked              | Yes — poll GET .../play      | Yes — backend sends push at unlock            |
| New items in store             | Yes — poll GET /api/store    | Yes — backend sends push when items added/available |
| New quests (target user / no target) | Yes — poll GET /api/quests | Yes — backend sends push when quest approved & available to user |

All four scenarios can be supported. For the best UX: use **in-app polling** where the user is already on the relevant screen (quest play, store, discover), and **FCM** so the backend can notify users when they are elsewhere or the app is closed.
