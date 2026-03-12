# Mobile App API — Step-by-Step Plan (Basic to Hard)

This document is **separate** from the main [Implementation Plan](IMPLEMENTATION_PLAN.md). It outlines how to build or expose APIs for the **mobile game app** in order of difficulty: basic first, then medium, then hard.

**Existing API surface (under `/api`):**  
Auth (signin, signup, user, signout), leaderboard, health. Quest flows currently live in web/simulation routes; the plan below can be implemented either by adding JSON API routes (e.g. in `routes/api.php`) that mirror or replace simulation behavior, or by making the simulation endpoints return JSON for mobile clients.

**Documentation:** When you add or change an API endpoint, document it in [API_DOCUMENTATION.md](API_DOCUMENTATION.md) (method, path, auth, request, response, errors).

---

## Table of Contents

1. [Tier 1: Basic](#tier-1-basic)
2. [Tier 2: Medium](#tier-2-medium)
3. [Tier 3: Hard](#tier-3-hard)
4. [Summary Checklist](#summary-checklist)

---

## Tier 1: Basic

*Read-only and auth. No quest state changes.*

### Step 1.1 — Auth (mobile-friendly)

- **Goal:** Mobile app can sign in and get a token; token is used for all subsequent requests.
- **Status:** **Done.** `POST /api/auth/signin`, `GET /api/user`, `POST /api/auth/signout` (Sanctum). Signup: `POST /api/auth/signup`. Client sends credentials, receives token + user; attach `Authorization: Bearer <token>` to requests.
- **Response shape:** Token + user (id, name, email, role, points_balance, level, total_xp_earned, total_completed_quests, profile_image as full URL) for profile and AR (e.g. level-up).

### Step 1.2 — List available quests (for this user)

- **Goal:** Return quests the user can join (approved, upcoming/ongoing, not already joined, target-group and enrollment rules applied).
- **Status:** **Done.** `GET /api/quests` — returns `quests[]` with `id`, `title`, `description`, `quest_type`, `question_type`, `is_elimination`, `reward_points`, `reward_custom_prize`, `buy_in_points`, `max_participants`, `current_participants`, `stages_count`, `first_stage_id`, `first_stage_location_hint`, `status`, `start_date`, `end_date`. No stage details.
- **Input:** Authenticated user (Bearer token).
- **Source of truth:** Same logic as simulation available quests list (target-group, enrollment, not already joined).
- **Difficulty:** Low — wrap existing query in an API controller and return JSON.

### Step 1.3 — Resolve quest + stage from QR (lookup)

- **Goal:** Mobile scans a QR; backend tells which quest and stage (and whether user can join/play).
- **Status:** **Done.** `GET /api/quests/resolve` — query `qr` (full URL, e.g. `https://.../quests/5/stages/12`) or `quest_id` + `stage_id`. Returns `quest_id`, `quest_title`, `stage_id`, `stage_number`, `location_hint`, `can_join`, `can_play`, `question_type`, `is_elimination`, `stage_deadline`, `stage_start`; optional `reason` when user cannot join/play. 404 if quest/stage not found; 400 if payload invalid.
- **Input:** Query: `qr` (URL with path `/quests/{id}/stages/{id}`) or `quest_id` and `stage_id`.
- **Output:** Quest + stage identity and `can_join` / `can_play` flags (and `reason` when blocked).
- **Difficulty:** Low — single lookup + permission checks, return small JSON.

### Step 1.4 — Get quest + stage detail (read-only)

- **Goal:** Given `quest_id` and optionally `stage_number`, return quest metadata and one stage’s detail (no submit, no join).
- **Status:** **Done.** `GET /api/quests/{quest}` — default returns quest + one stage (name, description, location, etc.) **without questions**; stage = participant’s current stage if in quest, else 1. Use `?include_questions=1` for AR after QR scan (questions + choices, no correct-answer flag).
- **Input:** Path `{quest}`; query `stage` (optional), `include_questions` (optional, for questions in AR).
- **Output:** Quest fields + stage `location_hint`, `stage_deadline`, `stage_start`, `passing_score`; optionally `stage.questions` when `include_questions=1`.
- **Difficulty:** Low — read from DB, no business logic.

### Step 1.5 — User profile (extended)

- **Goal:** Return the authenticated user’s profile for the app: id, name, email, role, points_balance, level, total_xp_earned, total_completed_quests, and optionally profile_image.
- **Status:** **Done.** `GET /api/user` returns the full profile: id, name, email, role, points_balance, level, total_xp_earned, total_completed_quests, profile_image (full URL for display, or null). Use for profile screen and AR (e.g. level-up comparison).
- **Endpoint:** `GET /api/user`.
- **Difficulty:** Low — extend existing user response or add dedicated profile endpoint.

### Step 1.6 — Store (list items + redeem)

- **Goal:** List redeemable store items for the user: id, name, description, points_cost, stock, image_url (if any), and whether the user can afford / item is available. Also allow redeeming an item (with activity log).
- **Status:** **Done.** `GET /api/store` returns items with `points_balance`, `is_available`, `can_afford` per item. `POST /api/store/redeem` with `store_item_id` and optional `quantity` redeems the item (deducts points, adds to inventory, decrements stock) and **logs the redemption** (`store_redeem` in activity log).
- **Source of truth:** Same as simulation store (store_items, is_visible, isAvailableNow()); redeem logic mirrors `StoreRedeemController`.
- **Difficulty:** Low — list read-only; redeem reuses existing logic + log.

### Step 1.7 — Achievements (list all)

- **Goal:** List all achievement definitions: id, name, description, criteria (e.g. quest count, level), image_url or badge icon, so the app can show “all achievements” and which are locked vs unlocked.
- **Status:** **Done.** `GET /api/achievements` — returns all achievements with `id`, `name`, `description`, `requirement_type`, `requirement_value`, `image_url` (null); when authenticated, each includes `earned` (true/false) and `earned_at`.
- **Source of truth:** `achievements` table; join with `user_achievements` when user is authenticated.
- **Difficulty:** Low — read-only.

### Step 1.8 — User achievements (list earned)

- **Goal:** List achievements the user has earned: achievement id, name, description, earned_at. Used for profile “My achievements” or AR after unlock. **User achieving an achievement is logged** (activity log `achievement_earned`) when they earn one (e.g. after quest complete or level-up).
- **Status:** **Done.** `GET /api/user/achievements` — returns array of earned achievements with achievement details and `earned_at`. Earning is logged in existing flows (quest completion, simulation level-up, etc.) via `ActivityLog::ACTION_ACHIEVEMENT_EARNED`.
- **Endpoint:** `GET /api/user/achievements`.
- **Source of truth:** `user_achievements` joined with `achievements`.
- **Difficulty:** Low — read-only.

### Step 1.9 — Inventory (user’s items) + use + history

- **Goal:** List the user’s inventory; allow using an item; provide history of items used.
- **Status:** **Done.** `GET /api/user/inventory` — list inventory (store items + quest custom prizes) with id, item_id, quantity, acquired_at, store_item (id, name, description), custom_prize_description. `POST /api/user/inventory/use` — use one unit (body: `store_item_id` or `inventory_id`); decrements/removes and **logs** (`item_used`) for history. `GET /api/user/inventory/history` — paginated list of items used (item_name, used_at) from activity log.
- **Source of truth:** `user_inventory`; use mirrors simulation `InventoryUseController`; history from `activity_logs` where action like `item_used:%`.
- **Difficulty:** Low — list read-only; use + log; history read from logs.

### Step 1.10 — Leaderboard

- **Goal:** Return leaderboard rankings (e.g. by period: today, week, month, semester) so the app can show top users and the current user’s rank.
- **Status:** **Done.** `GET /api/leaderboard` — query `period` (default `week`): one of `today`, `week`, `month`, `semester`, `overall`. Response: `entries` (rank, user_id, user_name, value), `period`, `periods`, `value_label`; when authenticated, `my_rank` and `my_value` for the current user in that period (or null if not on list).
- **Difficulty:** Low — already implemented; response extended with current user rank/value.

### Step 1.11 — User points transactions (history)

- **Status:** **Done.** `GET /api/user/transactions` — paginated list of point_transactions for the authenticated user: id, amount, transaction_type, type_label, reference_id, created_at; response includes points_balance and pagination. Optional query: page, per_page (1–50), type, date_from, date_to.
- **Goal:** Return the user’s points transaction history (paginated): type (credit/debit), amount, balance_after, description or reference (e.g. “Quest completed”, “Store redeem”), created_at. Used for “Transaction history” or “Points history” in the app.
- **Input:** Authenticated user. Optional: pagination (page, per_page), filters (type, date range).
- **Output:** List of point_transactions for the user, ordered by created_at desc. Include current points_balance in response if helpful.
- **Source of truth:** `point_transactions` where user_id = auth id. Expose as `GET /api/user/transactions` or `GET /api/points/transactions`.
- **Difficulty:** Low — read-only, single table query with pagination.

### Step 1.12 — User activity logs

- **Status:** **Done.** `GET /api/user/activity` — paginated list of activity_logs for the authenticated user: id, action_key, detail, display_label, timestamp. Optional query: page, per_page (1–50), action (prefix filter), date_from, date_to.
- **Goal:** Return the authenticated user’s activity log: actions they performed (e.g. quest_joined, quest_stage_submitted, store_redeem, achievement_earned, auth_signin) with a human-readable description and timestamp. Used for a “My activity” or “History” screen in the app.
- **Input:** Authenticated user. Optional: pagination (page, per_page), date range, or action filter.
- **Output:** List of activity log entries: `id`, `action` (or parsed `action_key` and `detail`), `timestamp`. Optionally include a display label per action type (e.g. “Joined quest”, “Redeemed item”) for the app to show without parsing.
- **Source of truth:** `activity_logs` where user_id = auth id, ordered by timestamp desc. Same table as admin LogController; filter to current user only. Expose as `GET /api/user/logs` or `GET /api/user/activity`.
- **Difficulty:** Low — read-only, single table query with optional pagination and filters.

---

## Tier 2: Medium

*State-changing actions: join, play state, submit. Single request → single response.*

### Step 2.1 — Join quest (scan first-stage QR)

- **Status:** **Done.** `POST /api/quests/join` — body: `quest_id` (required), `stage_id` (optional, validated as first stage). Returns participant_id, quest_id, current_stage (1), status (active), minimal quest/stage. Errors: 403 (target, approved, status, stage, points, full), 409 (already joined).
- **Goal:** User scans stage-1 QR; backend creates `QuestParticipant`, increments `current_participants`, applies target-group and max-participants checks.
- **Input:** `quest_id` (and optionally `stage_id` or stage number to validate it’s stage 1). User from auth.
- **Output:** `participant_id`, `quest_id`, `current_stage` (1), `status` (e.g. `active`), and minimal quest/stage info so the app can show “Quest taken” AR and then call play.
- **Errors:** 400/403 if user not in target group, quest full, or not stage 1. 409 if already joined.
- **Source of truth:** Same as `QuestParticipationController@join` (atomic increment, target check). Expose as `POST /api/quests/{quest}/join` or `POST /api/quests/join` with body `{ "quest_id": 1 }`.
- **Difficulty:** Medium — reuse existing join logic; ensure response is JSON and includes `participant_id` for later play/submit.

### Step 2.2 — Get play state (current stage, questions, status)

- **Status:** **Done.** `GET /api/participants/{participant}/play` — returns participant_id, quest_id, current_stage, status, can_quit, quit_guard_reason; stage (with questions for MCQ, no is_correct); stage_locked, next_stage_opens_at; awaiting_ranking/message; completed/eliminated outcome and rewards placeholder.
- **Goal:** For an existing participant, return everything the app needs to render the current step: stage info, questions (if MCQ), status (active, awaiting_ranking, completed, eliminated), and next-step hints (next location, stage_start if locked).
- **Input:** `participant_id` (from join or from “my participations”).
- **Output (JSON):**
  - `participant_id`, `quest_id`, `current_stage`, `status`, `can_quit`, `quit_guard_reason`
  - Current stage: `stage_number`, `location_hint`, `stage_deadline`, `stage_start`, `question_type`
  - If **stage locked** (stage_start in future): `stage_locked: true`, `next_stage_opens_at`, `next_stage_number` (so app can show “Next stage opens at …” in AR).
  - If **MCQ:** list of questions with `id`, `question_text`, `choices` (id, text; do not send `is_correct` until after submit).
  - If **QR-only:** no questions; app just needs to know “submit this stage” (e.g. scan or button).
  - If **awaiting_ranking:** `awaiting_ranking: true` and message so app shows “Waiting for results” AR.
  - If **completed / eliminated:** outcome and, when relevant, `next_stage_location_hint`, `next_stage_starts_at`, or `quest_completed`, plus any rewards (see Step 3.2).
- **Source of truth:** Same as `QuestParticipationController@play`. Expose as `GET /api/participants/{participant}/play` or `GET /api/quests/play/{participant}` returning JSON.
- **Difficulty:** Medium — one endpoint that branches on status and stage type; keep response shape consistent so the app can drive AR from a single contract.

### Step 2.3 — Submit answer (MCQ) or submit stage (QR)

- **Status:** **Done.** `POST /api/participants/{participant}/submit` — body: `answers` (array of question_id + choice_id or answer) or `stage_completed: true` for QR. Returns play-state shape plus outcome, message, passed, failed, awaiting_ranking, correct_count/total_count, rewards when completed. Reuses Simulation processSubmit; 400 for invalid/duplicate/already submitted, 403 for not active or stage locked.
- **Goal:** For MCQ: submit one answer (or batch per stage). For QR: submit stage completion. Backend records submission, updates participant state (advance, fail, or awaiting_ranking), applies passing score / elimination rules.
- **Input:** `participant_id`, and either:
  - **MCQ:** `question_id`, `choice_id` (or list of answers per question for the stage).
  - **QR:** “stage completed” (no choice; backend marks stage done).
- **Output:** Same shape as “play state” after action: updated `status`, `current_stage`, and **immediate outcome** when available: `passed`, `failed`, `awaiting_ranking`, `next_stage_location_hint`, `next_stage_starts_at`, `stage_locked`, etc. Include **rewards** in this response when the stage or quest is complete (see Step 3.2).
- **Errors:** 400 if already submitted for that question/stage, or invalid choice. 403 if stage locked or not current.
- **Source of truth:** Same as `QuestParticipationController@submit`. Expose as `POST /api/participants/{participant}/submit` with body e.g. `{ "question_id": 1, "choice_id": 2 }` or `{ "stage_completed": true }` for QR.
- **Difficulty:** Medium — single submit handler; response must be rich enough so the app can show result, then “next location” or “next stage opens at …” or “Waiting for results” or “Eliminated” without a second call (unless polling).

### Step 2.4 — Quit quest

- **Status:** **Done.** `POST /api/participants/{participant}/quit` — sets status to quit, decrements quest current_participants, logs; 403 if not active/awaiting_ranking or below minimum_participants. Returns `{ "ok": true, "message": "..." }`.
- **Goal:** User leaves the quest; backend decrements `current_participants`, updates participant status, enforces minimum-participants guard.
- **Input:** `participant_id`. User from auth.
- **Output:** 200 and optional updated participations list or simple `{ "ok": true }`.
- **Source of truth:** Same as `QuestParticipationController@quit`. Expose as `POST /api/participants/{participant}/quit`.
- **Difficulty:** Low — thin wrapper over existing quit logic.

### Step 2.5 — Store redeem

- **Status:** **Done.** Implemented as Step 1.6: `POST /api/store/redeem` — body `store_item_id`, optional `quantity`; atomic points deduction, inventory insert, stock decrement, activity log; returns points_balance and redeemed item; 422 for insufficient points or stock.
- **Goal:** User redeems a store item for points. Backend deducts points, decrements stock atomically, creates user_inventory entry and point_transaction.
- **Input:** Authenticated user, `store_item_id` (and optionally quantity if supported).
- **Output:** Success: updated points_balance, inventory entry (id, item, earned_at). Errors: 400 if insufficient points or out of stock; 409 if stock changed since list was fetched.
- **Source of truth:** Same as simulation `StoreRedeemController@store` (atomic stock check, points deduction, inventory insert). Expose as `POST /api/store/redeem` with body `{ "store_item_id": 1 }`.
- **Difficulty:** Medium — reuse existing redeem logic; ensure atomicity and JSON response.

### Step 2.6 — Inventory use (optional)

- **Status:** **Done.** Step 1.9: `POST /api/user/inventory/use` (body: inventory_id or store_item_id) and `POST /api/user/inventory/{inventory}/use`; decrements quantity, logs item_used; returns message, item_name, remaining_quantity.
- **Goal:** If the app supports “using” an inventory item (e.g. consume a voucher), record that use and optionally update quantity or mark as used.
- **Input:** Authenticated user, `inventory_id` or `user_inventory_id`.
- **Output:** 200 and updated inventory list or simple success; 400 if already used or invalid.
- **Source of truth:** Same as simulation `InventoryUseController@store` if applicable. Expose as `POST /api/inventory/use` or `POST /api/user/inventory/{id}/use`.
- **Difficulty:** Low–Medium — depends on existing use logic.

---

## Tier 3: Hard

*Multi-step flows, polling, and rich reward payloads.*

### Step 3.1 — Poll for elimination result (waiting for results)

- **Status:** **Done.** App polls until result is in. Added GET /api/participants/{participant}/status (lightweight: status, current_stage, awaiting_ranking, outcome). Documented polling interval 5–10 s and contract in api-docs/quests.md.
- **Goal:** After submit on an elimination stage, status is `awaiting_ranking`. App shows “Waiting for results” AR and periodically refetches until status becomes `advanced` or `eliminated`.
- **Approach:** Reuse **play state** (Step 2.2). App polls `GET /api/participants/{participant}/play` every N seconds until `awaiting_ranking` is false. Response then includes outcome (advanced → next stage info; eliminated → message). No new endpoint required; document polling interval and response contract.
- **Optional:** Add a lightweight `GET /api/participants/{participant}/status` that returns only `status`, `current_stage`, `outcome` to reduce payload while polling.
- **Difficulty:** Medium — mostly client discipline; backend already computes outcome when ranking runs (scheduled or on-demand).

### Step 3.2 — Return rewards in play/submit response (level, achievement, custom prize, points)

- **Status:** **Done.** Submit and play responses now include rich `rewards` when participant is winner/completed: `points_earned`, `custom_prize`, `level_up`, `previous_level`, `new_level`, `achievements` (array of id, name, description, image_url for quest-completion achievements). No change to award logic; payload built from quest + user state in API.
- **Goal:** When a stage or quest completes, the app must show AR for: level up, new achievement(s), custom reward, and points earned. All of this should be derivable from one play or submit response so the app doesn’t need multiple round-trips.
- **Approach:** Extend the **play state** and **submit** responses with a `rewards` (or `progression`) object, e.g.:
  - `level_up: boolean` and `new_level: number` (and maybe `previous_level`)
  - `achievements: [{ id, name, description, image_url? }]` (newly earned this session or this submit)
  - `custom_prize: string | null` (from quest `reward_custom_prize` or inventory entry)
  - `points_earned: number` (from this stage or quest completion)
- **Implementation:** After submit (or when returning play state for a completed participant), run the same logic that awards points, level, achievements, and custom prize; then attach a summary to the JSON response. May require refactoring so “award rewards” is a shared function called from both web and API flows.
- **Difficulty:** Hard — requires unifying reward application and exposing a clear, stable payload for the app to drive “Level up!”, “Achievement unlocked”, “Custom reward”, “Points earned” AR.

### Step 3.3 — QR payload design and stability

- **Decision:** **Option A** — QR encodes quest + stage via a full URL with path `/quests/{quest_id}/stages/{stage_id}`. Resolve (Step 1.3) accepts that URL in `qr` and returns the same response contract; no short codes or signed tokens for v1. Documented in api-docs/quests.md.
- **Goal:** QR codes are printed or displayed; they must uniquely identify quest + stage and (optionally) be short and durable.
- **Options:** (a) Embed `quest_id` and `stage_id` in URL or JSON in QR. (b) Short code (e.g. 6–8 chars) that maps to `quest_id` + `stage_id` in DB. (c) Signed token so backend can validate without storing one-off codes.
- **Endpoint:** Resolve endpoint (Step 1.3) must accept whatever format you choose and return consistent `quest_id`, `stage_id`, `can_join`/`can_play`.
- **Difficulty:** Medium — design decision plus a small lookup or validation layer.

### Step 3.4 — Idempotency and duplicate submits

- **Status:** **Done.** Duplicate submit (all answers already submitted) returns 200 with current play state and `idempotent_replay: true`; no double-count or extra log. Optional header `Idempotency-Key: <uuid>` caches response 24h and returns cached on same key.
- **Goal:** Mobile may retry submit on poor network; backend should not double-count answers or double-award points.
- **Approach:** Ensure one submission per (participant, question) or per (participant, stage) for QR. Use DB unique constraint or “already submitted” check; return same outcome as first submit (e.g. current play state) on retry. Optionally add idempotency key header (e.g. `Idempotency-Key: <uuid>`) and cache response for that key.
- **Difficulty:** Medium — existing logic may already prevent double submit; document and add key support if needed.

### Step 3.5 — Offline / sync later (optional, hardest)

- **Goal:** Allow scanning and answering offline; sync when back online.
- **Approach:** Mobile stores pending actions (join, submit) in local queue; when online, replay in order. Backend must accept submits with timestamps or “offline_id” and resolve conflicts (e.g. stage already closed). Likely requires new endpoints or flags (e.g. “accept offline submit”) and conflict resolution rules.
- **Difficulty:** Hard — leave for a later phase unless required for v1.

---

## Summary Checklist

| Step | Description | Tier |
|------|-------------|------|
| 1.1 | Auth (signin, token, user) | Basic |
| 1.2 | List available quests | Basic |
| 1.3 | Resolve quest + stage from QR | Basic |
| 1.4 | Get quest + stage detail (read-only) | Basic |
| 1.5 | User profile (extended: points, level, etc.) | Basic |
| 1.6 | Store (list items) | Basic |
| 1.7 | Achievements (list all; optional user earned state) | Basic |
| 1.8 | User achievements (list earned) | Basic |
| 1.9 | Inventory (user’s items) | Basic |
| 1.10 | Leaderboard | Basic |
| 1.11 | User points transactions (history) | Basic |
| 1.12 | User activity logs | Basic |
| 2.1 | Join quest (stage 1) | Medium |
| 2.2 | Get play state (stage, questions, status, next location, locked) | Medium |
| 2.3 | Submit answer (MCQ) or stage (QR) | Medium |
| 2.4 | Quit quest | Medium |
| 2.5 | Store redeem | Medium |
| 2.6 | Inventory use (optional) | Medium |
| 3.1 | Poll for elimination result (reuse play) | Hard |
| 3.2 | Rewards in response (level, achievement, custom, points) | Hard |
| 3.3 | QR payload design + resolve | Hard |
| 3.4 | Idempotency / duplicate submit handling | Hard |
| 3.5 | Offline sync (optional) | Hard |

Implement in order: Tier 1 → Tier 2 → Tier 3. Steps 1.2–2.3 are the minimum for the mobile AR flow described in the [Implementation Plan](IMPLEMENTATION_PLAN.md#mobile-game-app-ar--quest-flow); 3.1 and 3.2 are required for “Waiting for results” and all reward AR. Steps 1.5–1.12 and 2.5–2.6 cover profile, store, achievements, user achievements, inventory, leaderboard, points transactions, activity logs, and redeem for the full mobile app experience.
