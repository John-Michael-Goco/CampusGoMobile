# CampusGo Mobile App — Screens & API Reference

Copy this document into your Android Studio codebase (e.g. `docs/` or `app/src/main/assets/docs/`) for implementation reference.

**Base URL:** `https://your-domain.com/api`  
**Auth:** Send `Authorization: Bearer <token>` for all endpoints except signin, signup, health.

---

## 1. Auth & Onboarding

| Screen | Description | Endpoint | Method |
|--------|--------------|----------|--------|
| **Splash** | Optional: check API connectivity on app launch | `/health` | GET |
| **Sign In** | Email + password; returns `token`, `user` | `/auth/signin` | POST |
| **Sign Up** | Student registration; same response shape as signin | `/auth/signup` | POST |

### Sign In — POST `/api/auth/signin`
- **Body:** `{ "email": string, "password": string, "device_name": string? }`
- **Response:** `{ "token", "token_type": "Bearer", "user": { id, name, email, role, points_balance, level, total_xp_earned, total_completed_quests, profile_image } }`
- **Errors:** 422 (validation / invalid credentials)

### Sign Up — POST `/api/auth/signup`
- **Body:** `student_number`, `first_name`, `last_name`, `course`, `year_level` (1–10), `email`, `password` (min 8), `device_name`?
- **Response:** Same as signin.
- **Errors:** 422 (e.g. student not found, already registered, email taken)

### Health — GET `/api/health`
- **Response:** `{ "ok": true }`

---

## 2. Profile & Account

| Screen | Description | Endpoint | Method |
|--------|--------------|----------|--------|
| **Profile** | View current user (points, level, XP, profile image) | `/user` | GET |
| **Edit Profile / Settings** | Change password; update or remove profile picture | `/user/password`, `/user/profile` | PUT, POST |
| **Transaction History** | Paginated points transactions; filters: type, date_from, date_to | `/user/transactions` | GET |
| **Activity Log** | Paginated activity (quest_joined, store_redeem, etc.); optional filters | `/user/activity` | GET |

### Get profile — GET `/api/user`
- **Response:** `{ id, name, email, role, points_balance, level, total_xp_earned, total_completed_quests, profile_image }`  
  `profile_image` = full URL or null.

### Change password — PUT `/api/user/password`
- **Body:** `current_password`, `password`, `password_confirmation`
- **Response:** `{ "message": "Password updated successfully." }`

### Update profile picture — POST `/api/user/profile`
- **Content-Type:** `multipart/form-data`
- **Body:** `profile_image` (file, optional) and/or `remove_profile_image` (boolean, e.g. 1 to remove)
- **Response:** `{ "message", "user": { ... same as GET /user } }`

### Transactions — GET `/api/user/transactions`
- **Query:** `page`, `per_page` (1–50), `type`, `date_from`, `date_to` (Y-m-d)
- **Response:** `{ "transactions": [ { id, amount, transaction_type, type_label, reference_id, created_at } ], "points_balance", "pagination" }`  
  amount: positive = credit, negative = debit.

### Activity — GET `/api/user/activity`
- **Query:** `page`, `per_page`, `action` (prefix, e.g. `quest_`), `date_from`, `date_to`
- **Response:** `{ "activity": [ { id, action_key, detail, display_label, timestamp } ], "pagination" }`

### Sign out — POST `/api/auth/signout`
- **Response:** `{ "message": "Signed out successfully." }`

---

## 3. Quests

| Screen | Description | Endpoint | Method |
|--------|--------------|----------|--------|
| **Available Quests** | List quests user can join | `/quests` | GET |
| **Quest Detail** | Preview quest + stage (optional questions for AR) | `/quests/{quest}` | GET |
| **My Quests / Participating** | Active participations + next location or stage opens at | `/quests/participating` | GET |
| **QR Scanner** | Scan QR → resolve → join or play | `/quests/resolve` | GET |
| **Join Quest** | After scanning first-stage QR | `/quests/join` | POST |
| **Play** | Current stage, questions, location, quit, submit | `/participants/{id}/play` | GET |
| **Submit** | MCQ answers or QR stage completed | `/participants/{id}/submit` | POST |
| **Awaiting Ranking** | Poll until ranking done (elimination) | `/participants/{id}/status` | GET |
| **Quest Result** | Completed / eliminated; rewards | Use play response after status | GET |
| **Quit Quest** | Leave quest | `/participants/{id}/quit` | POST |

### List available quests — GET `/api/quests`
- **Response:** `{ "quests": [ { id, title, description, quest_type, question_type, is_elimination, reward_points, reward_custom_prize, buy_in_points, max_participants, current_participants, stages_count, status, start_date, end_date, first_stage_id, first_stage_location_hint } ] }`

### Resolve from QR — GET `/api/quests/resolve`
- **Query:** `qr=<full_url>` (e.g. `https://domain.com/quests/5/stages/12`) **or** `quest_id` + `stage_id`
- **Response:** `{ quest_id, quest_title, stage_id, stage_number, location_hint, can_join, can_play, reason?, question_type, is_elimination, stage_deadline?, stage_start? }`  
  Use `can_join` for stage 1 to show “Join”; `can_play` to show “Play” or open play screen.

### Quest detail — GET `/api/quests/{quest}`
- **Query:** `stage` (stage number, optional), `include_questions` (1 for AR)
- **Response:** `{ "quest": { ... }, "stage": { id, stage_number, location_hint, stage_deadline, stage_start, passing_score, questions? } }`

### List participating — GET `/api/quests/participating`
- **Response:** `{ "participations": [ { participant_id, quest_id, quest_title, current_stage, status, total_stages, preview?: { next_location_hint, next_stage_number } | { next_stage_opens_at, next_stage_number } } ] }`

### Join — POST `/api/quests/join`
- **Body:** `{ "quest_id": int, "stage_id": int? }` (stage_id must be first stage if provided)
- **Response:** `{ participant_id, quest_id, current_stage, status, quest: { ... }, stage: { ... } }`
- **Errors:** 400, 403, 404, 409 (e.g. already joined)

### Play state — GET `/api/participants/{participant}/play`
- **Response:** participant_id, quest_id, current_stage, status, can_quit, quit_guard_reason, total_stages, question_type, is_elimination, stage_locked, next_stage_opens_at?, next_stage_number?, stage? (id, stage_number, location_hint, questions for MCQ, etc.), next_stage_location_hint?, awaiting_ranking?, message?, outcome?, rewards? (when completed: points_earned, custom_prize, level_up, previous_level, new_level, achievements[]).

### Status (poll) — GET `/api/participants/{participant}/status`
- **When:** After submit on elimination stage when response has `awaiting_ranking: true`. Poll every 5–10 s.
- **Response:** `{ participant_id, status, current_stage, awaiting_ranking, outcome }`  
  When `awaiting_ranking` false: call play for full state; outcome can be `advanced` | `completed` | `eliminated`.

### Submit — POST `/api/participants/{participant}/submit`
- **MCQ body:** `{ "answers": [ { "question_id", "choice_id" or "answer" } ] }`
- **QR body:** `{ "stage_completed": true }`
- **Optional header:** `Idempotency-Key: <uuid>` (24h cache for duplicate requests)
- **Response:** Same shape as play + outcome, message, passed, failed, awaiting_ranking, correct_count?, total_count?, rewards?, idempotent_replay?

### Quit — POST `/api/participants/{participant}/quit`
- **Response:** `{ "ok": true, "message": "You have left the quest." }`
- **Errors:** 403 if not in progress or below minimum_participants

---

## 4. Store

| Screen | Description | Endpoint | Method |
|--------|--------------|----------|--------|
| **Store** | List items + points_balance, can_afford, is_available | `/store` | GET |
| **Redeem** | Confirm and redeem item (quantity) | `/store/redeem` | POST |

### List store — GET `/api/store`
- **Response:** `{ "points_balance", "items": [ { id, name, description, cost_points, stock, start_date, end_date, is_available, can_afford, image_url } ] }`

### Redeem — POST `/api/store/redeem`
- **Body:** `{ "store_item_id": int, "quantity": int? }` (default quantity 1)
- **Response:** `{ "message", "points_balance", "redeemed": { store_item_id, name, quantity, cost_points } }`
- **Errors:** 422 (e.g. insufficient points, out of stock)

---

## 5. Inventory

| Screen | Description | Endpoint | Method |
|--------|--------------|----------|--------|
| **My Inventory** | List items (store + quest custom prizes) | `/user/inventory` | GET |
| **Use Item** | Use one unit (by store_item_id or inventory_id) | `/user/inventory/use` or `/user/inventory/{id}/use` | POST |
| **Inventory History** | Paginated list of items used | `/user/inventory/history` | GET |

### List inventory — GET `/api/user/inventory`
- **Response:** `{ "inventory": [ { id, item_id, quantity, acquired_at, source_quest_id, store_item?, custom_prize_description? } ] }`

### Use item — POST `/api/user/inventory/use`
- **Body:** `store_item_id` **or** `inventory_id` (one required)
- **Alternative:** POST `/api/user/inventory/{inventory}/use` (no body)
- **Response:** `{ "message", "item_name", "remaining_quantity" }`

### History — GET `/api/user/inventory/history`
- **Query:** `page`, `per_page`
- **Response:** `{ "history": [ { item_name, used_at } ], "pagination" }`

---

## 6. Achievements

| Screen | Description | Endpoint | Method |
|--------|--------------|----------|--------|
| **All Achievements** | List all with earned / earned_at | `/achievements` | GET |
| **My Achievements** | List earned only (can be tab on same screen) | `/user/achievements` | GET |

### All — GET `/api/achievements`
- **Response:** `{ "achievements": [ { id, name, description, requirement_type, requirement_value, image_url, earned, earned_at } ] }`

### User earned — GET `/api/user/achievements`
- **Response:** `{ "achievements": [ { user_achievement_id, achievement_id, name, description, requirement_type, requirement_value, earned_at } ] }`

---

## 7. Leaderboard

| Screen | Description | Endpoint | Method |
|--------|--------------|----------|--------|
| **Leaderboard** | Rankings by period; show my_rank, my_value | `/leaderboard` | GET |

### Leaderboard — GET `/api/leaderboard`
- **Query:** `period` = `today` | `week` | `month` | `semester` | `overall` (default: week)
- **Response:** `{ "entries": [ { rank, user_id, user_name, value } ], "period", "periods", "value_label", "my_rank", "my_value" }`  
  For `overall`, value_label = "Total XP earned".

---

## 8. App Shell & Navigation (Recommended)

**Goal:** Feel like a proper app with a simple nav: Home, Quests (with prominent scanner), Store, Profile. Leaderboard lives inside Profile.

### Recommended: 4 tabs — Home | Quests (scanner) | Store | Profile

**Bottom navigation:** 4 tabs only.

| Tab | Content | Notes |
|-----|---------|--------|
| **Home** | Points, level, welcome; **active quests**; **store highlights**; quick links. | Scrollable hub. Uses `/user`, `/quests/participating`, `/store` (e.g. featured). No leaderboard here — it’s in Profile. |
| **Quests** | Available quests list → detail → Join → Play → Submit → Result. **Center action:** a **circle with the scanner icon** (e.g. FAB or raised center button) that opens the **QR Scanner** (resolve → join/play). | Main game flow; scanner is the primary action (circle + scanner icon). |
| **Store** | Store list + redeem. Link to **Inventory** (“My items”). | |
| **Profile** | Avatar, name, points, level, XP; **Leaderboard** (rankings by period); **Achievements** (all + earned); **Inventory** (list, use, history); Edit profile, Transactions, Activity, Sign out. | **Leaderboard** is inside Profile, not its own tab. Achievements and Inventory also inside Profile. |

- **Scanner:** On the Quests tab, use a **circle with the scanner/QR icon** (e.g. floating action button or elevated center control) so “scan to play” is obvious and one tap away.
- **Leaderboard:** Full leaderboard (today / week / month / semester / overall) is a section or screen **inside Profile**.
- **Achievements:** Inside Profile (e.g. “Achievements” section or screen, “All” / “Earned” tabs).
- **Inventory:** Inside Profile and/or linked from Store (“My items”).
- **Result:** **Home | Quests (with scanner circle) | Store | Profile** — 4 tabs, leaderboard on Profile.

### Alternative: 5 tabs (Leaderboard as its own tab)

If you prefer Leaderboard as a separate tab: Home | Quests (scanner) | Store | Leaderboard | Profile. Same as above but Leaderboard is a tab instead of inside Profile.

---

## 9. App Shell (screen reference)

| Screen | Description | APIs to use |
|--------|--------------|-------------|
| **Home / Dashboard** | Summary: points, level, active quests, store highlights, quick links | `/user`, `/quests/participating`, `/store` (optional); optional recent activity |

---

## Screen Count Summary

| Area | Screens |
|------|--------|
| Auth & onboarding | 2–3 (Splash, Sign In, Sign Up) |
| Profile & account | 4–5 (Profile, Edit/Settings, Transactions, Activity, Sign out) |
| Quests | 8–10 (list, detail, my quests, QR, join, play, submit, awaiting ranking, result, quit) |
| Store | 2 (list, redeem) |
| Inventory | 3 (list, use, history) |
| Achievements | 1–2 (all + my) |
| Leaderboard | 1 |
| Home | 1 |

**Total:** ~22–27 screens (some can be combined, e.g. Achievements as one screen with tabs).

---

## Suggested Navigation (Android)

See **§8 App Shell & Navigation** for the recommended structure. In short:

- **Bottom nav (4 tabs):** Home | Quests | Store | Profile.
- **Home:** Hub with points, level, active quests, store highlights, quick actions.
- **Quests:** Available quests list → Detail → Join → Play → Submit → Result. **Scanner:** use a **circle with the scanner/QR icon** (e.g. FAB or center button) on this tab to open the QR Scanner (resolve → join/play).
- **Store:** Store list → Redeem; link to Inventory (“My items”).
- **Profile:** Full profile + **Leaderboard** (rankings by period) + **Achievements** (All / Earned) + **Inventory** (list, use, history) + Edit profile, Transactions, Activity, Sign out.

---

*Generated from CampusGo API. Update base URL and this doc when the API changes.*
