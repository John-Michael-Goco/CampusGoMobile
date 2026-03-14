# CampusGo API Documentation

Base URL for all endpoints: **`/api`** (e.g. `https://your-domain.com/api`).

**Authentication:** Most endpoints require a Bearer token. Send it in the header:
```http
Authorization: Bearer <token>
```
Obtain the token via `POST /api/auth/signin` (or signup). Use `POST /api/auth/signout` to invalidate the current token.

---

## Convention: Document Every New API

**When you add a new API endpoint, add it to this documentation.** For each endpoint document:

- Method and path
- Whether authentication is required
- Request: body (JSON) and/or query parameters
- Response: success body (and shape) and error responses
- HTTP status codes

Keep the format consistent across the api-docs files below.

---

## Table of Contents

| Section | Description |
|---------|-------------|
| [Health](api-docs/health.md) | Health check (no auth). |
| [Auth & User](api-docs/auth.md) | Sign in, sign up, sign out, user profile, change password, update profile picture, points transactions, activity log. |
| [Store](api-docs/store.md) | List store items, redeem item. |
| [Achievements](api-docs/achievements.md) | List all achievements, list user's earned achievements. |
| [Inventory](api-docs/inventory.md) | List inventory, use item, history of items used. |
| [Quests](api-docs/quests.md) | List available quests, **resolve from QR** (QR payload = full URL path `/quests/{id}/stages/{id}` — see quests.md), list participating, quest history, get quest + stage detail, join, play state, submit, quit. |
| [Leaderboard](api-docs/leaderboard.md) | Leaderboard by period (with current user rank). |
| [Points transfer](api-docs/points-transfer.md) | Search students by school_id, transfer points (students only; min 10, max 100). |


---

## Changelog

| Date       | Change |
|------------|--------|
| (initial)  | Documented: health, auth (signin, signup, signout), user, leaderboard. |
| Step 1.2   | Added GET /api/quests — list available quests for authenticated user. |
| Step 1.3   | Added GET /api/quests/resolve — resolve quest + stage from QR URL or ids; returns can_join, can_play, reason. |
| Step 1.4   | Added GET /api/quests/{quest} — quest + stage detail; default = no questions (name, description, location); optional include_questions=1 for AR; stage = participant current or 1. |
| Step 1.5   | Confirmed GET /api/user is the extended profile (id, name, email, role, points_balance, level, total_xp_earned, total_completed_quests, profile_image as full URL). |
| (profile)  | Added PUT /api/user/password (change password) and POST /api/user/profile (update or remove profile picture). |
| Step 1.6   | Added GET /api/store (list items with points_balance, can_afford, is_available) and POST /api/store/redeem (redeem + activity log). |
| Step 1.7   | Added GET /api/achievements — list all with earned/earned_at per achievement. |
| Step 1.8   | Added GET /api/user/achievements — list user's earned achievements; achievement unlock is logged (achievement_earned). |
| Step 1.9   | Added GET /api/user/inventory (list), POST /api/user/inventory/use (use + log), GET /api/user/inventory/history (items used, paginated). |
| Step 1.10  | Leaderboard: GET /api/leaderboard now includes my_rank and my_value for authenticated user. |
| (taken)    | Added GET /api/quests/participating — list taken quests with preview: next_location_hint or next_stage_opens_at. |
| (split)    | Split API docs into api-docs/ by domain (health, auth, store, achievements, inventory, quests, leaderboard); this file is the index. |
| Step 1.11  | Added GET /api/user/transactions — paginated points transaction history (type, amount, type_label, reference_id, created_at); optional filters: type, date_from, date_to; includes points_balance. |
| Step 1.12  | Added GET /api/user/activity — paginated activity log (action_key, detail, display_label, timestamp); optional filters: action prefix, date_from, date_to. |
| Step 2.1   | Added POST /api/quests/join — join quest (body: quest_id, optional stage_id); returns participant_id, quest/stage summary; 403/409 for eligibility/full/already joined. |
| Step 2.2   | Added GET /api/participants/{participant}/play — play state: stage, questions (MCQ, no is_correct), status, can_quit, stage_locked, awaiting_ranking, completed/eliminated outcome. |
| Step 2.3   | Added POST /api/participants/{participant}/submit — MCQ: answers (question_id + choice_id/answer); QR: stage_completed. Returns play-state shape + outcome, message, passed, failed, awaiting_ranking, rewards. |
| Step 2.4   | Added POST /api/participants/{participant}/quit — leave quest; 403 if not in progress or below minimum_participants. |
| Step 2.5   | Store redeem: already implemented as 1.6 (POST /api/store/redeem). Marked Done in plan. |
| Step 2.6   | Inventory use: already 1.9; added POST /api/user/inventory/{inventory}/use (use by entry id in URL). |
| Step 3.1   | Poll for elimination result: added GET /api/participants/{participant}/status (lightweight status/outcome); documented polling (5–10 s) and contract in quests.md. |
| Step 3.2   | Rich rewards in play/submit: when completed, `rewards` includes points_earned, custom_prize, level_up, previous_level, new_level, achievements[]. |
| Step 3.4   | Idempotency: duplicate submit returns 200 with idempotent_replay; optional Idempotency-Key header caches response 24h. |
| Step 3.3   | QR payload: option A adopted — URL path `/quests/{quest_id}/stages/{stage_id}`; documented in quests.md and MOBILE_API_PLAN. |

When you add or change an endpoint, add a line above and update the relevant api-docs file.
