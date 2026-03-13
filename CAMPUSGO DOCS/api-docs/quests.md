# Quests

## Quests — List available

**GET** `/api/quests`  
**Auth:** Yes (Bearer)

Returns quests the authenticated user can join: approved, upcoming or ongoing, not already joined, and passing target-group and enrollment rules.

- **If the user is not enrolled in the current semester** (or there is no current semester): only **enrollment** quests for the **current semester** are returned (enrollment quests for other semesters are hidden). If there is no current semester, no quests are returned. The user must complete the current semester’s enrollment quest before they can see or join other quest types.
- **If the user is enrolled in the current semester:** non-enrollment quests (daily, event, custom) are shown, plus enrollment quests only for semesters they are not yet enrolled in. The *current semester* is the one whose date range includes today.

**Response** `200 OK`
```json
{
  "quests": [
    {
      "id": 1,
      "title": "Campus Hunt",
      "description": "Find all QR codes...",
      "quest_type": "event",
      "question_type": "multiple_choice",
      "is_elimination": false,
      "reward_points": 50,
      "reward_custom_prize": null,
      "buy_in_points": 0,
      "max_participants": 0,
      "current_participants": 12,
      "stages_count": 3,
      "status": "ongoing",
      "start_date": "2026-03-12 08:00:00",
      "end_date": "2026-03-15 20:00:00",
      "first_stage_id": 10,
      "first_stage_location_hint": "Near the library"
    }
  ]
}
```
- `quests` is an array; may be empty if none are available.
- `first_stage_id` and `first_stage_location_hint` refer to stage 1 (for QR/location display). When the quest status is `upcoming`, `first_stage_location_hint` is `null` so the app can show that the quest is not yet started without revealing the exact location; once the quest is `ongoing`, the hint is included.

**Errors**
- `401` — Missing or invalid token.

---

## Quests — Join quest (step 2.1)

**POST** `/api/quests/join`  
**Auth:** Yes (Bearer)

Join a quest by scanning the first-stage QR. Creates a `QuestParticipant`, applies target-group and max-participants checks, and deducts buy-in points if required. Use the returned `participant_id` for the play/submit flow.

**Request body** (JSON)
| Field     | Type | Required | Description |
|-----------|------|----------|-------------|
| quest_id  | int  | Yes      | Quest ID (from resolve or list). |
| stage_id  | int  | No       | If provided, must match the quest's first stage ID (validates correct QR). |

**Response** `201 Created`
```json
{
  "participant_id": 42,
  "quest_id": 5,
  "current_stage": 1,
  "status": "active",
  "quest": {
    "id": 5,
    "title": "Campus Treasure Hunt",
    "question_type": "multiple_choice",
    "is_elimination": false
  },
  "stage": {
    "id": 12,
    "stage_number": 1,
    "location_hint": "Near the library"
  }
}
```
- Use `participant_id` when calling the play state or submit endpoints.

**Errors**
- `400` — Quest has no stages; or `stage_id` provided and not the first stage. Body: `{ "message": "..." }`.
- `401` — Missing or invalid token.
- `403` — Not in target group, quest not approved, quest not available (status), wrong stage, not enough points (buy-in), quest full, or **user is not enrolled in the current semester and quest is not an enrollment quest** (message: "You must be enrolled in the current semester before you can join other quests. Complete an enrollment quest first."). Body: `{ "message": "..." }`.
- `404` — Quest not found. Body: `{ "message": "Quest not found." }`.
- `409` — Already joined this quest. Body: `{ "message": "You have already joined this quest." }`.

---

## Quests — Resolve from QR (step 1.3)

**GET** `/api/quests/resolve`  
**Auth:** Yes (Bearer)

Resolve which quest and stage a scanned QR refers to, and whether the authenticated user can join or play that stage.

**QR payload design (step 3.3 — option A):** QR codes must encode quest and stage via a **full URL** whose path is `/quests/{quest_id}/stages/{stage_id}`. Example: `https://your-domain.com/quests/5/stages/12`. The app sends the scanned URL as the `qr` query parameter; the backend parses the path to obtain `quest_id` and `stage_id` and returns a stable response (`quest_id`, `stage_id`, `can_join`, `can_play`, `reason`). Any future QR format (e.g. short codes or signed tokens) would be supported only inside this endpoint, without changing the response contract for the app.

**Query parameters** (one of the following):

| Parameter   | Type   | Required | Description |
|------------|--------|----------|-------------|
| qr         | string | Yes*     | Full URL encoded in the QR (e.g. `https://example.com/quests/5/stages/12`). Backend parses path `/quests/{id}/stages/{id}`. |
| quest_id   | int    | Yes*     | Quest ID (use with `stage_id` if not sending `qr`). |
| stage_id   | int    | Yes*     | Stage ID (use with `quest_id` if not sending `qr`). |

\* Provide either `qr` or both `quest_id` and `stage_id`.

**Response** `200 OK`
```json
{
  "quest_id": 5,
  "quest_title": "Campus Treasure Hunt",
  "stage_id": 12,
  "stage_number": 1,
  "location_hint": "Near the library",
  "can_join": true,
  "can_play": true,
  "question_type": "multiple_choice",
  "is_elimination": false,
  "stage_deadline": "2026-03-15 20:00:00",
  "stage_start": null
}
```
- `can_join`: `true` only for stage 1 when the user is not yet in the quest and is eligible (enrolled in current semester for non-enrollment quests, target group, quest not full, approved, etc.).
- `can_play`: `true` when the user can interact with this stage (e.g. already on this stage and stage is open, or can join via stage 1).
- `reason`: Present when the user cannot join/play; short explanation so the app can show why the scan did not allow join/play (see below).
- `stage_deadline` / `stage_start`: Omitted if null.

**When the user scans a code they are not allowed to use:** The API still returns `200 OK` with the quest/stage info. `can_join` and/or `can_play` will be `false`, and `reason` will be set so the app can show a message. Possible reasons include:
- **Stage 1 (join):** "You are not in the target participants for this quest." | "This quest is not approved yet." | "This quest is not available for joining (status: …)." | "Not enough points to join (need …)." | "This quest is full." | "You must be enrolled in the current semester before you can join other quests. Complete an enrollment quest first." | "You cannot join this quest at this time."
- **Stage 2+ (play):** "You are not a participant in this quest. Join by scanning the first stage QR." | "You are no longer active in this quest." | "This is not your current stage. Your current stage is …." | "This stage opens at …."

**Errors**
- `400` — Invalid or missing input (e.g. malformed `qr`, or neither `qr` nor `quest_id`+`stage_id`). Body: `{ "message": "..." }`.
- `404` — Quest or stage not found, or stage does not belong to the quest. Body: `{ "message": "..." }`.
- `401` — Missing or invalid token.

---

## Quests — List participating (taken quests with preview)

**GET** `/api/quests/participating`  
**Auth:** Yes (Bearer)

Returns quests the user is already in (taken quests). For each participation that is **active** or **awaiting_ranking** (not eliminated), the response includes a **preview**: either the **next location** (current stage's `location_hint`) when the stage is unlocked, or the **date when the stage opens** (`next_stage_opens_at`) when the current stage is locked by `stage_start`. The app can use this to show "Go to: {location}" or "Stage opens at {date}".

**Response** `200 OK`
```json
{
  "participations": [
    {
      "participant_id": 7,
      "quest_id": 5,
      "quest_title": "Campus Treasure Hunt",
      "current_stage": 2,
      "status": "active",
      "total_stages": 3,
      "preview": {
        "next_location_hint": "Near the library",
        "next_stage_number": 2
      }
    },
    {
      "participant_id": 8,
      "quest_id": 6,
      "quest_title": "QR Trail",
      "current_stage": 1,
      "status": "active",
      "total_stages": 2,
      "preview": {
        "next_stage_opens_at": "2026-03-14 09:00:00",
        "next_stage_number": 1
      }
    }
  ]
}
```
- For `active` or `awaiting_ranking`: `preview` is present — either `next_location_hint` (stage unlocked) or `next_stage_opens_at` + `next_stage_number` (stage locked).
- For `completed` or `eliminated`: `preview` is omitted.

**Errors**
- `401` — Missing or invalid token.

---

## Quests — Get quest + stage detail (step 1.4)

**GET** `/api/quests/{quest}`  
**Auth:** Yes (Bearer)

Return quest metadata and one stage's detail (read-only; no join/submit). Intended for the app's quest "show" / preview: **quest name, description, and stage location only**. Questions are not included by default; use `include_questions=1` when loading for AR after the user scans the QR.

- **Stage selection:** If `stage` is omitted and the user is a participant in this quest, the returned stage is their **current stage** (so location matches where they go next). Otherwise stage defaults to **1** (first stage).
- **Questions:** Omitted unless `include_questions=1`. Use that when showing questions in AR after a QR scan (choices still exclude `is_correct`).

**URL**
- `{quest}`: Quest ID (path).

**Query parameters**
| Parameter         | Type   | Required | Default | Description |
|-------------------|--------|----------|---------|-------------|
| stage             | int    | No       | *(see above)* | Stage number (1-based). If omitted, uses participant's current stage or 1. |
| include_questions | bool   | No       | `false` | Set to `1` or `true` to include `stage.questions` (for AR after QR scan). |

**Response** `200 OK` (default — no questions)
```json
{
  "quest": {
    "id": 5,
    "title": "Campus Treasure Hunt",
    "description": "...",
    "question_type": "multiple_choice",
    "is_elimination": false,
    "reward_points": 50,
    "reward_custom_prize": null,
    "buy_in_points": 0,
    "status": "ongoing",
    "start_date": "2026-03-12 08:00:00",
    "end_date": "2026-03-15 20:00:00"
  },
  "stage": {
    "id": 12,
    "stage_number": 1,
    "location_hint": "Near the library",
    "stage_deadline": "2026-03-15 20:00:00",
    "stage_start": null,
    "passing_score": 3
  }
}
```

With `include_questions=1`, `stage` also includes `questions` (array of `id`, `question_text`, `question_type`, `choices` with `id`, `choice_text`, `sort_order`; no `is_correct`).

**Errors**
- `404` — Quest not found (invalid id) or stage number not found for this quest. Body: `{ "message": "Stage not found." }` or Laravel's default 404 for missing quest.
- `401` — Missing or invalid token.

---

## Participants — Get play state (step 2.2)

**GET** `/api/participants/{participant}/play`  
**Auth:** Yes (Bearer)

Returns everything the app needs to render the current step for an existing participant: stage info, questions (if MCQ, without `is_correct`), status, next-step hints, and quit eligibility. Use after join or from "my participations" to drive the play/AR screen.

**URL**
- `{participant}`: Participant ID (from join response or from GET /api/quests/participating).

**Response** `200 OK`

Common fields (always present):
- `participant_id`, `quest_id`, `current_stage`, `status`, `can_quit`, `quit_guard_reason`, `total_stages`, `question_type`, `is_elimination`
- `stage_locked` (bool): when true, current stage is not yet open; use `next_stage_opens_at` and `next_stage_number` to show "Next stage opens at …".
- When stage is locked: `next_stage_opens_at`, `next_stage_number`
- `stage`: When not locked, object with `id`, `stage_number`, `location_hint`, `stage_deadline`, `stage_start`, `question_type`, `passing_score`, and `questions` (array; for MCQ only; choices do not include `is_correct`). Each question has `already_answered`.
- When active and stage unlocked: `next_stage_location_hint`, `next_stage_number`, `next_stage_starts_at` (for the next stage, when applicable)

When `status === 'awaiting_ranking'`:
- `awaiting_ranking: true`, `message`: e.g. "Waiting for results. You will advance or be eliminated based on your score."

When `status === 'completed'` or `'eliminated'`:
- `quest_completed` (true if completed), `eliminated` (true if eliminated), `outcome` (`"completed"` or `"eliminated"`), `rewards` (step 3.2: when completed, object with `points_earned`, `custom_prize`, `level_up`, `previous_level`, `new_level`, `achievements`; null when eliminated).

**Example (active, MCQ, stage unlocked)**
```json
{
  "participant_id": 42,
  "quest_id": 5,
  "current_stage": 1,
  "status": "active",
  "can_quit": true,
  "quit_guard_reason": null,
  "total_stages": 3,
  "question_type": "multiple_choice",
  "is_elimination": false,
  "stage_locked": false,
  "stage": {
    "id": 12,
    "stage_number": 1,
    "location_hint": "Near the library",
    "stage_deadline": "2026-03-15 20:00:00",
    "stage_start": null,
    "question_type": "multiple_choice",
    "passing_score": 2,
    "questions": [
      {
        "id": 1,
        "question_text": "What is the capital?",
        "question_type": "multiple_choice",
        "already_answered": false,
        "choices": [
          { "id": 10, "choice_text": "A" },
          { "id": 11, "choice_text": "B" }
        ]
      }
    ]
  },
  "next_stage_location_hint": "Main hall",
  "next_stage_number": 2,
  "next_stage_starts_at": null
}
```

**Errors**
- `401` — Missing or invalid token.
- `404` — Participation not found or not owned by the authenticated user. Body: `{ "message": "Participation not found." }`.

---

## Participants — Status (step 3.1: poll for elimination result)

**GET** `/api/participants/{participant}/status`  
**Auth:** Yes (Bearer)

Lightweight endpoint for **polling** after an elimination-stage submit. When the user's status becomes `awaiting_ranking`, the app shows "Waiting for results" and polls until ranking has run. Use this endpoint to keep payload small; when `awaiting_ranking` is false, the app can call **GET play** once to get full next-stage or eliminated details.

**Polling contract**
- **When to poll:** After submit on an elimination stage, when the submit response has `awaiting_ranking: true`.
- **Endpoint:** `GET /api/participants/{participant}/status` (or use `GET .../play` if you prefer the full payload each time).
- **Interval:** Poll every **5–10 seconds**. Avoid shorter intervals to reduce load.
- **When to stop:** When the response has `awaiting_ranking: false`. Then:
  - If `outcome === 'advanced'` or `status === 'active'`: call **GET .../play** to get `next_stage_location_hint`, `next_stage_starts_at`, and stage details.
  - If `outcome === 'eliminated'`: show eliminated message; optional call to play for full message.
  - If `outcome === 'completed'`: quest finished; play response includes rewards.

**Response** `200 OK`
```json
{
  "participant_id": 42,
  "status": "awaiting_ranking",
  "current_stage": 1,
  "awaiting_ranking": true,
  "outcome": null
}
```
When ranking has completed, for example:
```json
{
  "participant_id": 42,
  "status": "active",
  "current_stage": 2,
  "awaiting_ranking": false,
  "outcome": "advanced"
}
```
- `outcome`: `null` while awaiting; then `advanced` | `completed` | `eliminated` or the raw status.

**Errors**
- `401` — Missing or invalid token.
- `404` — Participation not found or not owned by the user.

---

## Participants — Submit answer or stage (step 2.3)

**POST** `/api/participants/{participant}/submit`  
**Auth:** Yes (Bearer)

Submit answers for the current stage (MCQ) or mark stage completed (QR). Backend records submissions, updates participant state (advance, fail, or awaiting_ranking), and applies passing score / elimination rules. Response has the same shape as play state plus immediate outcome so the app can show result, next location, "Waiting for results", or "Eliminated" without a second call.

**URL**
- `{participant}`: Participant ID.

**Request body** (JSON) — one of:

**MCQ:** `answers` array. Each item: `question_id` (required), and either `choice_id` or `answer` (choice id as string).
```json
{
  "answers": [
    { "question_id": 1, "choice_id": 10 },
    { "question_id": 2, "answer": "11" }
  ]
}
```
Or a single answer: `{ "question_id": 1, "choice_id": 10 }`.

**QR:** Stage completion (no questions to submit):
```json
{
  "stage_completed": true
}
```
Backend records the stage as done and runs the same outcome logic. Only valid when quest `question_type` is `qr_scan`.

**Idempotency (step 3.4)**  
- **Retry:** If the same answers (or stage_completed) are sent again and all questions are already submitted, the backend returns **200** with the same response shape and `idempotent_replay: true`, and does not double-count or log again.
- **Header (optional):** Send `Idempotency-Key: <uuid>` (e.g. client-generated). Within 24 hours, a duplicate request with the same key receives the **cached response** (same status and body as the first request). Use one key per logical submit attempt.

**Response** `200 OK`  
Same structure as GET play state, plus:
- `outcome`: `partial` | `advanced` | `completed` | `eliminated` | `awaiting_ranking` | `stage_not_ended` | `error`
- `message`: Human-readable result message.
- `passed`: `true` when outcome is `advanced` or `completed`.
- `failed`: `true` when outcome is `eliminated`.
- `awaiting_ranking`: `true` when elimination ranking is pending.
- `correct_count`, `total_count`: Present when outcome is from MCQ (score info).
- `rewards`: When quest/stage complete (step 3.2), object: `points_earned`, `custom_prize`, `level_up` (bool), `previous_level` (if level up), `new_level`, `achievements` (array of `{ id, name, description, image_url }` for quest-completion achievements earned). Null when not completed.
- `idempotent_replay`: Present and `true` when the request was a duplicate (all answers already submitted); no state change.

**Errors**
- `400` — No answers or stage_completed; invalid question_id; duplicate question_id in request; stage_completed on non–QR quest.
- `401` — Missing or invalid token.
- `403` — Not active in quest; stage not open yet (stage_start in future).
- `404` — Participation not found or not owned by the user.

---

## Participants — Quit quest (step 2.4)

**POST** `/api/participants/{participant}/quit`  
**Auth:** Yes (Bearer)

Leave the quest. Allowed only when participant status is **active** or **awaiting_ranking**. Blocked if quitting would leave the quest below the current stage's **minimum_participants** (so the quest can still run).

**URL**
- `{participant}`: Participant ID (must belong to the authenticated user).

**Response** `200 OK`
```json
{
  "ok": true,
  "message": "You have left the quest."
}
```

**Errors**
- `401` — Missing or invalid token.
- `403` — Not in progress (status is not active/awaiting_ranking), or quitting would leave the quest below minimum participants. Body: `{ "message": "..." }`.
- `404` — Participation not found or not owned by the user.
