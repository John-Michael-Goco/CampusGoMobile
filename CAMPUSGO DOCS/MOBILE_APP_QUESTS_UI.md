# CampusGo Mobile App — Quests Tab UI

Design and implementation reference for the Quests tab. Use with `MOBILE_APP_SCREENS_AND_API.md` and `MOBILE_APP_COLOR_PALETTE.md`. API details are in `api-docs/quests.md`.

---

## 1. Overview

The Quests tab is the main entry for:

- **My Quests** — active participations and “what to do next” (location or when the next stage opens).
- **Discover** — available quests the user can join.
- **Scan** — the center FAB in the bottom nav opens the QR Scanner (resolve → join/play). No duplicate scanner button on the Quests screen.

**Flow:** List → Quest detail → (after scanning first-stage QR) Join → Play → Submit → (if elimination) Awaiting ranking → Result. Quit is available from Play when allowed.

---

## 2. Screen structure

| Area | Content |
|------|---------|
| **Header** | Title (e.g. “Quests”) |
| **Segment** | Two options: **My Quests** \| **Discover** (segmented control or tab row) |
| **Body** | Scrollable list — either “My active quests” or “Available quests” depending on segment |
| **Scanner** | Center FAB in app bottom bar (existing) — opens QR Scanner; no extra FAB on this screen |

---

## 3. My Quests segment

**API:** `GET /api/quests/participating`

**Response:** `participations[]` with `participant_id`, `quest_id`, `quest_title`, `current_stage`, `status`, `total_stages`, and optional `preview`.

### 3.1 Card (per participation)

| Element | Source | Notes |
|--------|--------|------|
| Title | `quest_title` | Primary text |
| Progress | `current_stage`, `total_stages` | e.g. “Stage 2 of 3”; show as **amber** progress bar or step indicator |
| Next step | `preview` | See below |
| Status chip | `status` | `active` → amber “Active”; `awaiting_ranking` → blue “Results pending” |

**Next step from `preview`:**

- If `next_location_hint` is present: **“Next: {next_location_hint}”** (e.g. “Next: Near the library”). Optionally show `next_stage_number`.
- If `next_stage_opens_at` is present: **“Stage {next_stage_number} opens at {formatted date/time}”** (e.g. “Stage 2 opens Mar 14, 9:00 AM”). Use **info/calendar** styling (blue).

**Tap:** Navigate to **Play** (or Quest detail → Play) using `participant_id` (e.g. `GET /api/participants/{participant}/play`).

### 3.2 Empty state

- **Copy:** “No active quests. Scan a QR to join one or browse Discover.”
- Optional: short CTA to switch to Discover or to open scanner.

---

## 4. Discover segment

**API:** `GET /api/quests`

**Response:** `quests[]` with full quest metadata (see Screens & API doc).

### 4.1 Card (per available quest)

| Element | Source | Notes |
|--------|--------|------|
| Title | `title` | Primary text |
| Description | `description` | 1–2 lines, truncated |
| Meta row | `stages_count`, `reward_points`, `buy_in_points` | e.g. “3 stages · 50 pts reward”; if `buy_in_points > 0` add “Entry: 10 pts” |
| Status chip | `status` | `ongoing` → emerald “Ongoing”; `upcoming` → muted “Upcoming”; optional “Starts {start_date}” |
| Participants (optional) | `current_participants`, `max_participants` | Only when `max_participants > 0`, e.g. “12 / 50” |

**Tap:** Navigate to **Quest detail** (`GET /api/quests/{quest}`) — full description, stages, rewards, join instructions.

### 4.2 Empty state

- **Copy:** “No quests available right now. Check back later or scan a QR at an event.”

---

## 5. Quest detail screen

**API:** `GET /api/quests/{quest}` (optional query: `stage`, `include_questions` for AR).

**Layout:**

- **Header:** Quest title, status, date range (`start_date`–`end_date`).
- **Body:** Full description, “Stages: N”, “Reward: X pts” (and `reward_custom_prize` if present), “Entry: X pts” if `buy_in_points > 0`.
- **Bottom CTA:** “Scan first stage to join” and/or “Join” (if join is allowed without scanning). Primary button — use **emerald**.

Optional: list of stages (number + `location_hint` from detail API) to set expectations.

---

## 6. QR Scanner and resolve result

**Flow:** User scans QR → app calls `GET /api/quests/resolve?qr=<url>` (or `quest_id` + `stage_id`).

**Response:** `quest_id`, `quest_title`, `stage_id`, `stage_number`, `location_hint`, `can_join`, `can_play`, `reason?`, etc.

**UI:**

- **`can_join === true` (stage 1):** Show “Join [Quest name]” — emerald primary button. On confirm → `POST /api/quests/join` with `quest_id` and optional `stage_id` → then navigate to Play.
- **`can_play === true` (already in quest, correct stage):** Show “Continue to stage” → navigate to Play with `participant_id` (from `GET /api/quests/participating` or from join response).
- **Otherwise:** Show `reason` in a snackbar or inline message (error/warning style). Do not show Join/Play.

---

## 7. Colors and tokens (Quests)

Use `MOBILE_APP_COLOR_PALETTE.md` for full definitions. Quests-specific:

| Use | Color | Hex (light) | Hex (dark) |
|-----|-------|-------------|------------|
| Quest accent, progress bar, stage indicator | Amber | `#f59e0b` | `#d97706` |
| Primary CTA (Join, Play, Start) | Emerald | `#059669` | `#047857` |
| “Opens at” / info / calendar | Blue | `#2563eb` | `#60a5fa` |
| Screen background | Zinc | `#f4f4f5` | `#18181b` |
| Card surface | White / Zinc 800 | `#ffffff` | `#27272a` |
| Borders, dividers | Zinc 200 / 700 | `#e4e4e7` | `#3f3f46` |
| Secondary / meta text | Zinc 500 / 400 | `#71717a` | `#a1a1aa` |
| Elimination badge (optional) | Rose | `#e11d48` | `#fb7185` |

---

## 8. API ↔ UI mapping (summary)

| UI | API | When |
|----|-----|------|
| My Quests list | `GET /api/quests/participating` | Tab/screen load, pull-to-refresh |
| Discover list | `GET /api/quests` | Tab/screen load, pull-to-refresh |
| Quest detail | `GET /api/quests/{quest}` | Tap quest in Discover |
| Resolve QR | `GET /api/quests/resolve?qr=...` | After scan in Scanner screen |
| Join | `POST /api/quests/join` | After resolve when `can_join`; then go to Play |
| Play state | `GET /api/participants/{id}/play` | Enter Play screen, after join/submit |
| Submit | `POST /api/participants/{id}/submit` | User submits MCQ or QR stage |
| Status (poll) | `GET /api/participants/{id}/status` | After submit when `awaiting_ranking`; every 5–10 s until false |
| Quit | `POST /api/participants/{id}/quit` | User taps Quit from Play (when `can_quit`) |

---

## 9. Optional enhancements

- **Pull-to-refresh** on both My Quests and Discover.
- **Filter chips** on Discover: “All” | “Ongoing” | “Upcoming” (client-side filter on `status`).
- **Quest type label:** Small chip for `quest_type` (e.g. “Event”, “Daily”, “Enrollment”) if useful.
- **Elimination badge:** Small “Elimination” pill on cards when `is_elimination === true` (e.g. rose accent).

---

## 10. Screen list (Quests area)

| Screen | Purpose |
|--------|---------|
| Quests tab | My Quests / Discover segments + list; FAB in nav for scanner |
| Quest detail | Full quest info; “Scan to join” / Join CTA |
| Scanner | Camera → resolve → Join or Play or show reason |
| Play | Current stage, questions/location, submit, quit |
| Awaiting ranking | Shown when `awaiting_ranking`; poll status |
| Quest result | Completed / eliminated; rewards from play response |

---

*Aligns with CampusGo API and MOBILE_APP_SCREENS_AND_API.md. Update when API or navigation changes.*
