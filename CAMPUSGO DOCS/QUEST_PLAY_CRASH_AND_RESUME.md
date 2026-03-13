# Quest Play — Crash and Resume (Mobile)

**Copy this document into your mobile codebase** (e.g. `docs/` or next to your quest/AR implementation) for behavior and implementation reference.

---

## Overview

This doc describes what happens when the app crashes or is killed **while the user is on the Play screen** (answering MCQ or about to submit), and how to make the flow robust.

---

## 1. Crash before Submit (user was answering, no request sent)

| Where | What happens |
|-------|-------------------------------|
| **Backend** | No submit was received. The participant remains **active** on the **same stage**. No answers are stored. |
| **Device** | Any answers the user had selected exist only in memory. They are **lost** unless the app persists them (see §4). |
| **When user reopens the app** | User can go to **My Quests** → open the same quest → call **GET /api/participants/{id}/play** again. They receive the **same** stage and questions. They can answer and submit again. |

**Takeaway:** No server state is lost. The user can **resume**: My Quests → open quest → Play → answer again → Submit. No special backend handling required.

---

## 2. Crash after Submit (request may or may not have reached the server)

| Scenario | Backend | What to do on reopen |
|----------|---------|----------------------|
| **Request never reached the server** (app died before/during send) | No submit recorded. Participant still on same stage. | User opens Play again; they see same stage and can submit again. |
| **Request reached the server and succeeded, but app crashed before handling the response** | Submit was applied (e.g. moved to next stage or `awaiting_ranking`). | On reopen, call **GET /api/participants/{id}/play** or **GET /api/participants/{id}/status**. Response reflects the **updated** state (e.g. "Waiting for results…" or next stage). Show that UI; do **not** auto-retry submit without idempotency (see §3). |

**Takeaway:** Always refresh play/status when entering the quest flow (e.g. from My Quests or after app resume). Use the response to show the correct screen (Play, Awaiting ranking, or Result).

---

## 3. Idempotency-Key on Submit (recommended)

To handle “we’re not sure if submit went through” (e.g. timeout or crash after sending request):

- On **POST /api/participants/{id}/submit**, send a header: **`Idempotency-Key: <uuid>`** (one UUID per logical submit attempt, e.g. per tap of “Submit”).
- If the server already processed that key (within 24h), it returns the **same outcome** (200 + same body) and does **not** apply the submit again.
- If the app crashes after sending but before receiving the response, **retry** the same request with the **same** Idempotency-Key. The server will return the stored response; the app can then show the correct state (e.g. awaiting_ranking or next stage) without double-submit.

**Implementation note:** Generate a new UUID for each new “Submit” tap. If the user reopens the app and you don’t know whether the previous submit succeeded, you can either (a) just refresh play/status and show whatever the server says, or (b) retry the last submit with the same key if you persisted it; the server will return the cached result.

---

## 4. Optional: Draft answer persistence

To improve UX when the app is killed **while the user is still answering** (before Submit):

- **Persist draft answers** (e.g. selected choice IDs per question, or “stage completed” for QR) in local state or storage, keyed by `participant_id` (and optionally `current_stage`).
- When loading the Play screen, if you have a draft for this participant/stage, pre-fill the form. User can change and submit as normal.
- Clear the draft when submit succeeds (or when play/status shows a different stage or outcome).

This is optional; the backend does not require it. Without it, the user simply re-enters answers after a crash.

---

## 5. Recommended flows (summary)

1. **On entering Play** (from Join or from My Quests): always call **GET /api/participants/{id}/play** (or **GET /api/participants/{id}/status** if you only need light status). Use the response to decide: show questions, show “Waiting for results…”, or show Result (rewards / eliminated).
2. **On Submit:** send **POST /api/participants/{id}/submit** with **Idempotency-Key: &lt;uuid&gt;** (new UUID per submit tap). On success, show the returned state (e.g. awaiting_ranking or next stage). On network error or app crash, on next open refresh play/status; optionally retry submit with the same key.
3. **After crash:** do not assume “submit failed.” Refresh play/status first; then show the appropriate screen. If you had a pending submit with an Idempotency-Key, retrying with that key is safe.

---

## 6. API reference (quick)

| Endpoint | Purpose |
|----------|---------|
| **GET /api/participants/{id}/play** | Full play state: stage, questions, location, `can_quit`, etc. Use when showing the Play screen. |
| **GET /api/participants/{id}/status** | Light status: `status`, `current_stage`, `awaiting_ranking`, outcome. Use for polling or after resume. |
| **POST /api/participants/{id}/submit** | Submit MCQ answers or QR stage completed. Optional header: **Idempotency-Key: &lt;uuid&gt;** (24h cache). |

Full API details: see `api-docs/quests.md` (Participants — Play, Submit, Status, Quit) in the backend docs.
