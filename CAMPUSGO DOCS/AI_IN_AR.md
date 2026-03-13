# AI in AR — Proposal & Documentation

This document describes how AI can enhance the **Augmented Reality (AR)** experience in CampusGo: the camera/overlay flow used during quests (QR scan, location hints, questions, level-up, and stage transitions).

**Related:** [quests API](api-docs/quests.md), [MOBILE_API_PLAN.md](MOBILE_API_PLAN.md)

---

## 1. Overview

AR in CampusGo refers to the in-quest experience where users:

- Scan QR codes at physical locations
- See location hints and stage information
- Answer multiple-choice or QR-only stages
- See “Quest taken”, “Next stage opens at…”, “Waiting for results”, and completion/level-up feedback

AI can augment this flow with **visual recognition**, **contextual hints**, **in-AR guidance**, **open/voice answers**, **accessibility**, and **optional verification** — without replacing existing APIs.

---

## 2. AI Capabilities (Detailed)

### 2.1 Location verification (beyond QR)

| Item | Description |
|------|-------------|
| **What** | Use the device camera and an AI vision model to confirm the user is at the correct physical location (e.g. “Point your camera at the library entrance”). |
| **Why** | Adds “find the spot” gameplay, reduces reliance on QR-only verification, and can serve as a fallback or extra validation. |
| **In AR** | User sees live camera view; when the model recognizes the target (building, statue, sign), the app overlays “Location verified” and can unlock the stage or reveal the next hint. |
| **API fit** | Complements `GET /api/quests/resolve` and `GET /api/participants/{id}/play`; verification result can be sent as part of join or submit logic if backend supports it. |

---

### 2.2 Object / landmark quests

| Item | Description |
|------|-------------|
| **What** | Quests like “Find the [statue / mural / landmark]” where the user must point the camera at the correct object. AI confirms the object is in view. |
| **Why** | Reuses existing stage flow and `location_hint`; adds a visual check instead of (or in addition to) scanning a QR. |
| **In AR** | Camera feed with overlay; when AI confirms the object, the app can mark the stage as completed or “correct landmark” and call existing play/submit flows. |
| **API fit** | Same participant/play/submit APIs; optionally a new quest type or `question_type` (e.g. `landmark`) if backend is extended. |

---

### 2.3 Contextual AR hints

| Item | Description |
|------|-------------|
| **What** | After a QR scan (or when play state returns the current stage), an AI uses quest + stage data (e.g. `location_hint`, `stage_number`) and optionally a single camera frame to generate short, natural-language hints (e.g. “Head toward the red brick building on your left”). |
| **Why** | Makes hints feel dynamic and easier to follow than a static string. |
| **In AR** | Hint shown as text or voice overlay on the camera view. |
| **API fit** | Uses `location_hint`, `stage_number`, and play state from `GET /api/participants/{id}/play` and `GET /api/quests/resolve`. Can be client-side AI or a dedicated backend hint endpoint. |

---

### 2.4 In-AR assistant / guide

| Item | Description |
|------|-------------|
| **What** | A small “guide” in the AR UI that answers: “Where do I go next?”, “What’s this stage about?”, “I’m stuck.” |
| **Why** | Reduces confusion; uses existing participant and play state. |
| **In AR** | Chat bubble or voice in the same screen as the camera and quest overlay. |
| **API fit** | Uses `participant_id`, play state, `location_hint`, and stage info from existing APIs; AI composes answers from that context. |

---

### 2.5 Short / voice answers in AR (beyond MCQ)

| Item | Description |
|------|-------------|
| **What** | For stages that support open answers, the user speaks or types a short response; AI scores it (e.g. correct concept, acceptable synonym). |
| **Why** | Extends beyond `question_type: multiple_choice`; allows “answer in your own words” while keeping backend as source of truth. |
| **In AR** | After scanning the stage QR (or verifying location), show the question in the overlay and a mic/text input; submit the AI result (e.g. score/flag) to backend; backend can treat it like an MCQ result for pass/fail. |
| **API fit** | Extends existing submit flow; backend may need to accept an “open answer” type and optional AI score/confidence. |

---

### 2.6 Accessibility in AR

| Item | Description |
|------|-------------|
| **What** | AI describes the scene (“You’re facing a building with glass doors”) or reads out quest text, hints, and choices (text-to-speech). |
| **Why** | Makes AR quests usable for users with low vision or preference for audio. |
| **In AR** | Same camera + overlay screen, with optional “Describe scene” and “Read hint / question” actions that call AI and play TTS or show descriptions. |
| **API fit** | No backend change required; uses existing quest/stage text and play state. |

---

### 2.7 Smarter “Next stage” and “Waiting” overlays

| Item | Description |
|------|-------------|
| **What** | When play state is `stage_locked` or `awaiting_ranking`, AI generates a short, friendly message (e.g. “Next stage opens at 3 PM at the fountain”) from `next_stage_opens_at`, `next_stage_location_hint`, etc. |
| **Why** | Docs already mention showing “Next stage opens at…” and “Waiting for results” in AR; AI can make those messages clearer and less generic. |
| **In AR** | Same place where “Waiting for results” or “Next stage opens at…” is shown; replace static text with AI-generated copy. |
| **API fit** | Uses `stage_locked`, `next_stage_opens_at`, `next_stage_location_hint`, and `awaiting_ranking` from `GET /api/participants/{id}/play`. |

---

### 2.8 Cheat / fairness checks (optional)

| Item | Description |
|------|-------------|
| **What** | Optionally verify that the environment (e.g. indoor/outdoor, or specific landmark) is plausible for the stage before allowing submit. |
| **Why** | Reduces “scan QR at home and answer elsewhere”; keeps quests tied to being on campus. |
| **In AR** | Before or after QR scan, send a single frame (or short clip) to a lightweight model; backend receives a flag (e.g. `environment_ok`) and can block or flag suspicious submissions. |
| **API fit** | Could be a new optional parameter on join or submit (e.g. `environment_check: ok`), or a dedicated verification endpoint. |

---

## 3. Suggested implementation order

| Priority | Feature | Rationale |
|----------|---------|----------|
| 1 | **Contextual AR hints (2.3)** | Uses existing `location_hint` and play state; high impact, no new quest types. |
| 2 | **In-AR guide (2.4)** | Uses same play state and participant; improves UX with minimal API change. |
| 3 | **Smarter “Next stage” messages (2.7)** | Uses existing `stage_locked` / `awaiting_ranking`; improves clarity of current AR overlays. |
| 4 | **Object / landmark quests (2.2)** | New quest type or stage type; reuses join/play/submit flow. |
| 5 | **Short / voice answers (2.5)** | Extends `question_type` and submit flow; requires backend support. |
| 6 | **Location verification (2.1)** | Enhances trust and gameplay; can be optional per quest. |
| 7 | **Accessibility (2.6)** | No API change; improves inclusivity. |
| 8 | **Cheat checks (2.8)** | Optional; depends on policy and privacy considerations. |

---

## 4. API touchpoints

- **Resolve & join:** `GET /api/quests/resolve`, `POST /api/quests/join` — QR/location context for hints and verification.
- **Play state:** `GET /api/participants/{id}/play` — current stage, `location_hint`, `stage_locked`, `next_stage_opens_at`, `awaiting_ranking`, questions.
- **Quest detail:** `GET /api/quests/{quest}` with `include_questions=1` — stage and questions for AR overlay.
- **Submit:** Existing submit endpoints — may be extended for open-answer or verification payloads.

---

## 5. Out of scope (this doc)

- General chatbot outside AR (e.g. help desk).
- AI for admin panel (quest creation, moderation).
- Backend implementation details of AI models (this doc focuses on product/UX and API fit).

---

## 6. Changelog

| Date | Change |
|------|--------|
| 2026-03-13 | Initial document: AI-in-AR capabilities, priorities, and API touchpoints. |
