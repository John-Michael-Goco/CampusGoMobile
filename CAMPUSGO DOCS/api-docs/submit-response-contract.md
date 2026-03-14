# Submit response contract (AR / quest play)

To avoid client–backend guesswork and bugs, the mobile app expects the **POST /api/participants/{id}/submit** response to follow this contract. The app uses it to show Correct/Wrong, total score, and outcome (rewards vs next location).

---

## 1. Correct/Wrong for the **last answered question**

The app must show "Correct" or "Wrong" for the answer the user **just** submitted.

**Option A – Per-question (recommended for one-answer-at-a-time submits)**  
Each submit response describes only the question that was just submitted:

- `correct_count`: `0` or `1` (this question wrong/right)
- `total_count`: `1`

Then the app uses: **Correct** when `correct_count == 1`, **Wrong** when `correct_count == 0`.

**Option B – Cumulative**  
Each response describes the whole stage so far:

- `correct_count`: total number of correct answers so far (including this one)
- `total_count`: total number of questions in the stage (e.g. 3)

Then the app uses: **Correct** when `correct_count` increased compared to the previous submit (and we track "previous" on the client).

**Important:** The backend should pick **one** of these and stick to it for all submits in a stage. Mixing A and B causes wrong Correct/Wrong.

**Important – `passed`/`failed` are NOT per-answer flags:**  
`passed` means "the user passed the **stage**" (met the passing score). `failed` means "the user is eliminated." These are stage/quest-level outcomes. Do NOT use them to determine if a single answer was correct — the app ignores `passed: false` for individual answers because it just means "stage not completed yet."

---

## 2. Total score at the end of the stage

When the stage is finished (all questions submitted), the app shows "Score: X / Y".

- If the backend uses **Option A** above, the app accumulates `correct_count` and uses `stage.questions.size` as Y.
- If the backend uses **Option B**, the app uses the last response's `correct_count` and `total_count` as X and Y.

So the backend should either:

- Send **Option A** every time (and the app will sum correct and use question count as total), or  
- Send **Option B** every time (and the app will use the last response's counts).

---

## 3. Stage end and "rewards" vs "next location"

- **Last stage (e.g. 1-stage quest, or final stage of multi-stage):**  
  When the user has passed and there is **no** next stage, the backend should return:
  - `outcome`: **`"completed"`** (not `"advanced"`), and  
  - `rewards`: populated (points, level_up, achievements, etc.) so the app can show the reward screen.

- **Not last stage:**  
  When the user advances to the next stage, return:
  - `outcome`: **`"advanced"`**, and  
  - `next_stage_location_hint` or `next_stage_starts_at` so the app can show "Go to: …" or "Stage opens at …".

So: **use `outcome: "completed"` when the quest is finished (single-stage or last stage passed).** The app will then always show the rewards screen and never "next location" for a finished quest. The app can also fall back to "rewards" when `outcome == "advanced"` and `current_stage >= total_stages`, but relying on `"completed"` is clearer and avoids loops.

---

## 4. Backend MUST persist state changes on submit

**This is critical.** When the submit endpoint determines an outcome, the backend must **persist the change in the database**, not just return it in the response. Specifically:

### 4a. Advancing to the next stage (`outcome: "advanced"`)

When all stage questions have been answered and the user met the passing score, and there **are** more stages:

1. **Set `outcome` to `"advanced"`** in the response.
2. **Increment the participant's `current_stage`** in the database (e.g. 1 → 2).
3. **Populate `next_stage_location_hint`** (if the next stage is unlocked) or **`next_stage_starts_at`** (if locked by `stage_start`).
4. The participant status stays `"active"`.

If the backend does NOT increment `current_stage`, the user will be stuck on the same stage forever — even though the app shows "You advanced!".

### 4b. Completing the quest (`outcome: "completed"`)

When all stage questions have been answered and the user met the passing score, and this is the **last** stage:

1. **Set `outcome` to `"completed"`** in the response.
2. **Change the participant's `status`** to `"completed"` (or `"winner"`) in the database.
3. **Populate `rewards`** in the response: `points_earned`, `custom_prize`, `level_up`, `new_level`, `achievements`.
4. **Award the points** to the user's account (update user balance).
5. **Grant achievements** if any are configured for this quest.

If the backend does NOT update the status, `GET /api/quests/participating` will still show the user as `"active"` and they will never appear in quest history as completed.

### 4c. Elimination (`outcome: "eliminated"`)

1. **Set `outcome` to `"eliminated"`** and **`failed` to `true`**.
2. **Change participant `status`** to `"eliminated"` in the database.
3. **Do NOT populate `rewards`** (set to null).

---

## 5. One-at-a-time submissions

The mobile app submits answers **one question at a time** (for per-question Correct/Wrong feedback). The backend MUST handle this:

- **Track submitted answers cumulatively** per participant per stage (not just per request).
- After each individual submit, check: have **all** stage questions now been answered?
- If yes → run the passing/advancement/completion logic (§4a, §4b, §4c).
- If no → return `outcome: "partial"` or `"stage_not_ended"` and do NOT advance/complete yet.

**Example flow (3-question stage, one at a time):**

| Submit | Request body | Expected `outcome` | Backend action |
|--------|-------------|-------------------|----------------|
| 1st | `{ "answers": [{ "question_id": 1, "choice_id": 10 }] }` | `"partial"` | Record answer 1. 1/3 done → no action. |
| 2nd | `{ "answers": [{ "question_id": 2, "choice_id": 20 }] }` | `"partial"` | Record answer 2. 2/3 done → no action. |
| 3rd | `{ "answers": [{ "question_id": 3, "choice_id": 30 }] }` | `"advanced"` or `"completed"` | Record answer 3. 3/3 done → evaluate score, advance or complete. |

If the backend only triggers completion when **all answers arrive in a single request**, one-at-a-time submissions will never trigger advancement. The mobile app works around this by **batch-resubmitting all answers** after the last question (the API's idempotent replay guarantees this is safe), but the backend should handle one-at-a-time natively.

---

## 6. `GET /api/participants/{id}/play` after submit

After the submit, the app also calls `GET /api/participants/{id}/play` to get the definitive state. This response must reflect the **persisted** changes from §4:

- If advanced: `current_stage` should be the new stage number, `stage` should be the new stage's data, `stage_locked` / `next_stage_opens_at` if applicable.
- If completed: `status` should be `"completed"` or `"winner"`, `outcome` should be `"completed"`, `rewards` should be populated.
- If eliminated: `status` should be `"eliminated"`, `outcome` should be `"eliminated"`.

---

## 7. Summary for backend — Non-elimination quests

| Scenario | `outcome` | `correct_count` / `total_count` | **Backend must persist** |
|----------|-----------|-----------------------------------|-----------------------|
| One answer submitted (mid-stage) | `partial` or `stage_not_ended` | Either (0 or 1) and 1, or cumulative – **be consistent** | Record the answer. No status change. |
| All answers in, passed, **last stage** | **`completed`** | Full stage score | **Status → "completed"/"winner". Populate rewards. Award points.** |
| All answers in, passed, **more stages** | `advanced` | Full stage score | **Increment `current_stage`. Populate next-stage hints.** |
| All answers in, failed passing score | `eliminated` | Full stage score | **Status → "eliminated". No rewards.** |

---

## 8. Elimination quest rules

Elimination quests differ from non-elimination in several important ways. The backend **must** follow these rules.

### 8a. MCQ Elimination

**There is NO passing score.** Every participant answers all questions. After submitting the final answer, the backend does NOT immediately advance or eliminate — it returns `awaiting_ranking: true`.

**Per-answer submits (mid-stage):**
- Same as non-elimination: record the answer, return `correct_count`/`total_count`, `outcome: "partial"`.
- Do NOT evaluate elimination or advancement yet.

**Final answer submitted (all questions answered):**
1. Record the answer and the **submission timestamp** (used as tiebreaker).
2. Return `outcome: "awaiting_ranking"`, `awaiting_ranking: true`.
3. Set participant status to `"awaiting_ranking"` in the database.
4. Do NOT advance or eliminate yet.

**Ranking runs when ONE of these conditions is met:**
- **All active participants** on the stage have submitted their answers, OR
- The **stage deadline** is reached (participants who didn't submit by deadline → eliminated).

**When ranking runs:**
1. Rank all participants who submitted by **score (highest first)**, then **submission time (earliest first)** as tiebreaker.
2. Top N (up to `max_survivors`) → set status to `"active"`, increment `current_stage`, set `outcome: "advanced"`. If **last stage**: status → `"completed"`/`"winner"`, populate rewards, run `awardWinner()`.
3. Everyone else who submitted → status → `"eliminated"`, `outcome: "eliminated"`.
4. Participants who **didn't submit** before the deadline → status → `"eliminated"`.
5. If **minimum participants** is not met → stage fails, quest cancelled.
6. **Send push notifications** to all participants with their result (advanced/eliminated/winner).

**Response contract for MCQ elimination (after final answer):**

| Scenario | `outcome` | `awaiting_ranking` | **Backend action** |
|----------|-----------|--------------------|--------------------|
| Mid-stage answer | `partial` | `false` | Record answer. No status change. |
| Final answer submitted | `awaiting_ranking` | `true` | Record answer + timestamp. Status → `"awaiting_ranking"`. |
| After ranking → advanced (not last stage) | `advanced` | `false` | Status → `"active"`. Increment `current_stage`. |
| After ranking → winner (last stage) | `completed` | `false` | Status → `"completed"`/`"winner"`. Populate rewards. Award points. |
| After ranking → eliminated | `eliminated` | `false` | Status → `"eliminated"`. No rewards. |

### 8b. QR Scan Elimination

**Non-last stages:**
Same as MCQ elimination — submit `stage_completed: true`, backend returns `awaiting_ranking: true`. Ranking runs at stage deadline or when all participants submit. Rank by **submission time (earliest first)**.

**Last stage (immediate winner):**
The first participant to submit `stage_completed: true` wins immediately:
1. Set their status to `"completed"`/`"winner"`, run `awardWinner()`, return `outcome: "completed"` with rewards.
2. All other active participants → status → `"eliminated"`.
3. Subsequent submits → return `outcome: "eliminated"`, `message: "Too late! Another participant already completed this quest."`.
4. Resolve endpoint: if quest already has a winner → `can_play: false`, `reason: "This quest already has a winner."`.

### 8c. GET /api/participants/{id}/status (polling — optional)

Lightweight endpoint for checking ranking status. The mobile app currently shows "Waiting for results" and lets the user leave AR — results are delivered via push notification. But the status endpoint is available if needed:

```json
// While awaiting:
{ "participant_id": 42, "status": "awaiting_ranking", "current_stage": 1, "awaiting_ranking": true, "outcome": null }

// After ranking resolved:
{ "participant_id": 42, "status": "active", "current_stage": 2, "awaiting_ranking": false, "outcome": "advanced" }
```

---

Once this contract is implemented on the backend, the app logic can stay simple and we can stop guessing.
