# AR / Quest Play — UI Requirements (from API docs)

**Copy this into your mobile/AR codebase** as a checklist for what to show at each step. All data comes from **GET /api/quests/resolve**, **POST /api/quests/join**, **GET /api/participants/{id}/play**, **POST .../submit** (response), and **GET .../status** (polling).

**Joining is done in AR by scanning the first-stage QR — there is no separate “Join” button.** The app uses AR for both join and play.

---

## 0. Joining via AR — scan first-stage QR

There is **no join button**; the user **joins by scanning the first-stage QR** in AR.

| Step | Action | API | What to show in AR |
|------|--------|-----|--------------------|
| 1 | User opens AR and scans a QR | — | Camera / scanner overlay. |
| 2 | App sends scanned URL to backend | **GET /api/quests/resolve?qr={scanned_url}** | — |
| 3 | Resolve returns quest + stage + flags | Response: `quest_id`, `quest_title`, `stage_id`, `stage_number`, `can_join`, `can_play`, `reason?`, `location_hint`, etc. | — |
| 4a | **First stage (stage_number === 1) and `can_join === true`** | **POST /api/quests/join** with `quest_id` (and optional `stage_id`) | On success: AR feedback “Quest taken” / “You joined!” and quest title (from resolve or join response). Then load play (step 5) or show first location hint + “Scan to complete” / questions. |
| 4b | First stage but `can_join === false` | — | Show `reason` in AR (e.g. “Quest full”, “Already joined”, “You must be enrolled first”). Do not call join. |
| 4c | **Not first stage, or `can_play === true`** (already in quest) | Use **GET /api/participants/{id}/play** (participant from participating list or context) | Show play UI for current stage (§2). |

**Resolve response fields (for join flow):** `quest_id`, `quest_title`, `stage_id`, `stage_number`, `location_hint`, `can_join`, `can_play`, `reason` (when join/play not allowed), `question_type`, `is_elimination`, `stage_deadline`, `stage_start`.

**Join response:** `participant_id` (use for all later play/submit/status calls), `quest.title`, `stage.location_hint`. After join, show AR “Quest taken” and then either show the first stage’s location hint + scan/question UI, or call **GET /api/participants/{participant_id}/play** to get full play state.

---

## 1. Quest title

- **Source:** Not in the play response. Use **quest title** from:
  - **GET /api/quests/resolve** (`quest_title`) when scanning (join or play), or
  - **POST /api/quests/join** response (`quest.title`) right after joining via first QR, or
  - **GET /api/quests/participating** (field `quest_title`) when resuming an existing participation.
- **Show:** In the header/title of the Play screen and on result screens.

---

## 2. Play screen (current stage — before submit)

**Data from:** `GET /api/participants/{participant}/play`

| What to show | API field(s) | Notes |
|--------------|--------------|--------|
| **Quest title** | *(see §1)* | From participating/resolve/join. |
| **Current stage** | `current_stage`, `total_stages` | e.g. "Stage 1 of 3". |
| **Location hint** | `stage.location_hint` | Only when `stage_locked === false`. |
| **Questions (MCQ)** | `stage.questions[]` | Each: `question_text`, `choices[]` (`id`, `choice_text`). Use `already_answered` to show progress. No `is_correct` in choices. |
| **QR instruction** | — | When `question_type === 'qr_scan'`: show "Scan QR to complete" (no question list). |
| **Passing score** | `stage.passing_score` | For non-elimination MCQ; e.g. "Need X correct to pass." |
| **Quit** | `can_quit`, `quit_guard_reason` | Show Quit button when `can_quit === true`; if false, show `quit_guard_reason` (e.g. "Quitting would leave the quest below the minimum participants."). |

**When `stage_locked === true`** (stage not yet open):

| What to show | API field(s) |
|--------------|--------------|
| Message | "Next stage opens at {date}" |
| Date | `next_stage_opens_at` |
| Stage number | `next_stage_number` |

Do **not** show questions or location until the stage is unlocked.

---

## 3. After submit — outcome and score

**Data from:** `POST /api/participants/{participant}/submit` response (same shape as play + outcome fields).

| What to show | API field(s) | When |
|--------------|--------------|------|
| **Score (MCQ)** | `correct_count`, `total_count` | When present (e.g. "You got 3/5 correct."). |
| **Result message** | `message` | Always; human-readable (e.g. "Stage passed! 3/5 correct. Moving to stage 2."). |
| **Eliminated** | `failed === true` or `outcome === 'eliminated'` | Show "You were eliminated." (and optional `message`). |
| **Advanced** | `passed === true` and `outcome === 'advanced'` | Proceed to next stage (see §4). |
| **Winner (last stage)** | `outcome === 'completed'` or status `winner` | Show quest result screen (see §5). |
| **Waiting for ranking** | `awaiting_ranking === true` | Show "Waiting for results…" and poll status (§6). |

---

## 4. Proceed to next stage — next location or open time

**Data from:** Same submit response or **GET .../play** after advancing.

| What to show | API field(s) | Condition |
|--------------|--------------|-----------|
| **Next location hint** | `next_stage_location_hint` | When the **next** stage is already unlocked (no `stage_start` or it’s in the past). Show e.g. "Go to: {next_stage_location_hint}". |
| **Stage opens at** | `next_stage_starts_at` | When the next stage is **locked** by `stage_start`. Show e.g. "Stage 2 will open at {next_stage_starts_at}." |
| **Next stage number** | `next_stage_number` | For context (e.g. "Stage 2 will open at …"). |

- If `next_stage_location_hint` is present and `next_stage_starts_at` is null/empty → stage is unlocked, show location.
- If `next_stage_starts_at` is present → stage is locked, show "Stage will be open at {next_stage_starts_at}".

---

## 5. Quest result screen (winner / completed)

**When:** `status === 'completed'` or `'winner'`, or submit response `outcome === 'completed'`.

**Data from:** Play response or submit response: **`rewards`** object (null when eliminated).

| What to show | API field | Notes |
|--------------|-----------|--------|
| **Points earned** | `rewards.points_earned` | e.g. "+50 points". |
| **Custom prize** | `rewards.custom_prize` | Show only if present (non-null, non-empty). |
| **Level up** | `rewards.level_up` | If true, show e.g. "Level up! {previous_level} → {new_level}" using `rewards.previous_level`, `rewards.new_level`. |
| **Achievements unlocked** | `rewards.achievements[]` | Array of `{ id, name, description }` for achievements earned by completing this quest. Show each (name, description). |

If `rewards` is null (e.g. eliminated), do not show this block.

---

## 6. Eliminated

**When:** `failed === true` or `outcome === 'eliminated'` or `status === 'eliminated'`.

| What to show |
|--------------|
| Message: **"You were eliminated."** |
| Optional: `message` from API (e.g. "Eliminated! You got 2/5 correct."). |
| Do not show rewards (points, prize, achievements). |

---

## 7. Waiting for ranking (elimination stage)

**When:** Submit returns `awaiting_ranking === true` (elimination quest, stage submitted).

| What to show | API / action |
|--------------|----------------|
| Message | "Waiting for results…" (or use `message` from play, e.g. "Waiting for results. You will advance or be eliminated based on your score."). |
| **Poll** | **GET /api/participants/{participant}/status** every 5–10 seconds. |
| **Stop polling** when | `awaiting_ranking === false`. Then: |
| If `outcome === 'advanced'` or `status === 'active'` | Call **GET .../play** and show next stage (location hint or "stage opens at …"). |
| If `outcome === 'eliminated'` | Show eliminated screen (§6). |
| If `outcome === 'completed'` | Show quest result screen (§5); play response has `rewards`. |

---

## 8. Summary checklist

| Area | Needed on AR |
|------|-------------------------------|
| **Join (AR only)** | No join button. User scans first-stage QR → resolve → if `can_join` call join → show "Quest taken" + quest title, then play. If `can_join === false` show `reason`. |
| **Header** | Quest title (from resolve/join or participating). |
| **Play** | Location hint, questions (MCQ) or "Scan QR to complete", passing score (MCQ), Quit (if `can_quit`). |
| **Stage locked** | "Next stage opens at {next_stage_opens_at}" (and `next_stage_number`). |
| **After submit** | Score (`correct_count`/`total_count`), result message, and one of: eliminated / advanced / completed / awaiting ranking. |
| **Advanced** | Next location hint if unlocked, or "Stage will open at {next_stage_starts_at}" if locked. |
| **Winner** | Points earned, custom prize (if any), level up (if any), achievements unlocked (if any). |
| **Eliminated** | "You were eliminated." (and optional API message). |
| **Awaiting ranking** | "Waiting for results…" + poll status until done, then show advanced / eliminated / completed. |

---

## 9. API quick reference

| Endpoint | Use |
|----------|-----|
| **GET /api/quests/resolve?qr={url}** | After scanning any QR: get `quest_id`, `quest_title`, `stage_id`, `stage_number`, `can_join`, `can_play`, `reason`, `location_hint`. Used for both join (first QR) and play (later stages). |
| **POST /api/quests/join** | Join quest (body: `quest_id`, optional `stage_id`). Call **only** when user scanned first-stage QR and resolve returned `can_join === true`. Returns `participant_id`, quest title, stage. |
| **GET /api/participants/{id}/play** | Full play state: stage, questions, location, locked state, next location/open time, rewards when completed. |
| **POST /api/participants/{id}/submit** | Submit answers (MCQ) or `stage_completed: true` (QR). Response = play shape + `outcome`, `message`, `passed`, `failed`, `awaiting_ranking`, `correct_count`, `total_count`, `rewards`. |
| **GET /api/participants/{id}/status** | Lightweight poll: `status`, `awaiting_ranking`, `outcome` (`advanced` \| `completed` \| `eliminated`). |
| **GET /api/quests/participating** | List of participations with `quest_title`, `participant_id`, preview (next location or opens at). |

All endpoints require **Authorization: Bearer {token}**.
