# CampusGo Admin Panel Guide

This guide explains how to use the admin panel from sign-in through academic setup, simulation, users, store, achievements, logs, and quests.

---

## 1. Sign In

- **URL:** `/login` (app root redirects here).
- **Default admin account** (created by the database seeder):
  - **Email:** `admin@email.com`
  - **Password:** `admin123` (change after first login if desired)
- Only users with the **admin** or **professor** role see the full admin navigation. **Students** see a limited menu (Dashboard, Leaderboards, Quests, Store & Achievements).

---

## 2. Academic Management

Academic Management is where you define **who can register** in the app: **students** (players) and **professors** (gamemasters). You also manage **semesters**.

**Sidebar:** **Academic Management** → **Students** | **Professors** | **Semester**

### 2.1 Students (masterlist)

- **URL:** `/masterlist/students`
- **Purpose:** List of students who are **allowed to register** as players. Registration (e.g. in simulation or mobile) checks this masterlist; if a student is not here or details don’t match, signup fails.
- **Actions (admin only):**
  - **Add student** — Create a masterlist entry.
  - **Edit** — Update an existing entry.
  - **Delete** — Remove from masterlist (does not delete an already registered user account).
- **Fields per student:**
  - **Student number** (e.g. 2024-001) — Must be unique; used at signup.
  - **First name**, **Last name**
  - **Course** — e.g. BSCS, BSIT, BSCPe, BSCE, ACT.
  - **Year level** — 1–10 (1–2 for ACT).
  - **Section** — Optional; depends on course/year.

### 2.2 Professors (masterlist)

- **URL:** `/masterlist/professors`
- **Purpose:** List of professors who can be turned into **gamemasters** (or admins) via the **Users** tab. Only people in this masterlist can be given a user account with role professor/admin.
- **Actions (admin only):**
  - **Add professor** — Create a masterlist entry.
  - **Edit** — Update an existing entry.
  - **Delete** — Remove from masterlist (does not delete an existing user account).
- **Fields per professor:**
  - **Employee ID** (e.g. EMP-001)
  - **First name**, **Last name**

### 2.3 Semester

- **URL:** `/semesters`
- **Purpose:** Define academic semesters. The **current semester** is the one whose date range includes today; it’s used for dashboard and for enrollment quests.
- **Actions (admin only):**
  - **Add semester** — Create a new semester.
  - **Edit** — Change name or dates.
  - **Delete** — Remove a semester.
- **Fields:**
  - **Name** — e.g. “1st Sem 2025-2026”
  - **Start date** / **End date** — Date range (no time). Today must fall within this range for the semester to be “current.”

**Order of setup:** Add at least one **semester**, then add **students** and **professors** so you can use simulation and create gamemasters.

---

## 3. Simulation: Register a Student and Log In

The simulation mimics the mobile app flow so you can test without the real app.

1. **Student registration (simulation)**  
   - **URL:** `/simulation/student-register`  
   - Fill in details that **exactly match** a student in the **masterlist**: student number, first name, last name, course, year level, email (unique), password (min 8 characters), password confirmation.  
   - On success, the page tells the user to go to log in.

2. **Student login (simulation)**  
   - **URL:** `/simulation/login`  
   - Use the **email** and **password** the student just registered with.  
   - After login, the student can use simulation pages: store, achievements, leaderboard, transactions, profile, quests (join, play, submit, quit).

Only people in the **students** masterlist can register. Professors/gamemasters are **not** created via this flow; they are created in the **Users** tab.

---

## 4. Users Tab — Creating a Gamemaster

- **URL:** `/users`
- **Purpose:** Manage app users. To create a **gamemaster**, you create a user and assign the **professor** (or **admin**) role. That user must exist in the **Professors** masterlist.
- **Create gamemaster (admin only):**
  - Click **Add Admin/Gamemaster** (or equivalent).
  - **Select a professor** from the list (only professors in the masterlist appear).
  - **Role:** **Admin** or **Professor** (gamemaster).
  - **Email** and **Password** for login.
  - Save.
- **Notes:**
  - The default admin (`admin@email.com`) cannot be deleted; its role cannot be changed.
  - Professors can create and manage their own quests (custom, event); admins can also approve quests and manage everything else.

---

## 4.1 Limits of a gamemaster (professor role)

A **gamemaster** is a user with the **professor** role. They have access to the same sidebar as an admin (Dashboard, Users, Leaderboards, Quests, Store & Achievements, Academic Management, Logs and Transactions) but many actions are **read-only** or **restricted** as follows.

### What a gamemaster CAN do

- **View** all of: Users, Leaderboards, Quests (Active, Created, History), Store, Achievements, Academic Management (Students, Professors, Semesters), Activity Logs, Points Transactions.
- **Create quests** — But only **Custom** and **Event** quest types (not Daily or Enrollment). They can set target, stages, MCQ or QR, rewards, dates, etc., and submit for approval.
- **Print QR codes** for their own quests (from Active or Created) so players can scan and join/play.
- **Delete** their own quests (e.g. cancel a pending or active quest they created).
- **See quest history** — Completed and cancelled quests, with an optional “Created by me” filter.

### What a gamemaster CANNOT do

| Area | Restriction |
|------|-------------|
| **Quests** | Cannot **approve** or **reject** quests. The **Approval** page (pending queue) is **admin only**. A professor’s new quest stays **Pending** until an admin approves it. |
| **Quests** | Cannot **edit** any quest (including their own). Edit quest and Edit stages are **admin only**. To change a quest, an admin must edit it. |
| **Academic Management** | Cannot add, edit, or delete **students**, **professors**, or **semesters**. View only. |
| **Users** | Cannot add, edit, or delete users (cannot create other gamemasters or change roles). View only. |
| **Store** | Cannot create, edit, or delete **store items**. View only. |
| **Achievements** | Cannot create, edit, or delete **achievements**. View only. |

### Summary

- **Gamemaster:** Can create (Custom/Event) and delete their own quests, print QR, and view everything. Cannot approve quests, edit quests, or change academic data, users, store, or achievements.
- **Admin:** Can do everything a gamemaster can, plus approve/edit quests, manage academic masterlist and semesters, manage users (create gamemasters), and manage store and achievements.

---

## 5. Store — Items for Players to Redeem

- **URL:** `/store`
- **Purpose:** Define **store items** that players can redeem with their **points** (earned from quests, etc.).
- **Create store item (admin only):**
  - **Name**, **Description** (optional)
  - **Cost (points)** — How many points one redemption costs.
  - **Stock** — How many units are available (decremented on each redeem).
  - **Start date** / **End date** (optional) — Item is only available (and visible) within this window.
  - **Visible in store** — If unchecked, the item is hidden from the store list (e.g. for seasonal or disabled items).
- **View:** Everyone (admin/professor/student) can see the store; only admins can create, edit, or delete items.

---

## 6. Achievements

- **URL:** `/achievements` (under **Store & Achievements** in the sidebar).
- **Purpose:** Define **achievements** that players unlock when they meet criteria. Used for gamification (e.g. level, quest count, quest wins, completing a specific quest).
- **Create achievement (admin only):**
  - **Name**, **Description** (optional)
  - **Requirement type** — What the player must do to earn it:
    - **Quest count** — Complete a number of quests (any).
    - **Level** — Reach a given level (e.g. 5).
    - **Quests win** — Win (e.g. rank 1) in quests a number of times.
    - **Complete specific quest** — Complete a chosen quest (then pick the quest from a dropdown).
  - **Requirement value** — Number for “quest count,” “level,” or “quests win”; or the selected quest for “complete specific quest.”

---

## 7. Logs and Transactions

**Sidebar:** **Logs and Transactions** → **Activity Logs** | **Points Transactions**

### 7.1 Activity Logs

- **URL:** `/logs`
- **Purpose:** View a log of user actions (e.g. quest_joined, store_redeem, achievement_earned, item_used).
- **Filters:** Search, date range, user, sort direction. Use this to audit what users did and when.

### 7.2 Points Transactions

- **URL:** `/transactions`
- **Purpose:** View all **points** transactions (credits and debits): quest rewards, store redemptions, transfers, etc.
- **Filters:** Search, user, date range, sort direction. Use this to see how points were earned or spent.

---

## 8. Quests

Quests are the main gameplay: players join, go through stages (e.g. scan QR or answer MCQ), and earn points and rewards. Admins and professors (gamemasters) manage quests; **only admins** approve quests created by professors.

**Sidebar:** **Quests** → **Active** | **Approval** (admin only) | **History**

### 8.1 Quest types and roles

- **Quest types:** Daily, Event, Custom, Enrollment.
  - **Professors** can create only **Custom** and **Event**.
  - **Admins** can create all types, including **Daily** and **Enrollment**.
- **Enrollment** quests are tied to a semester and target students enrolling in that semester; other types can target “everyone” or specific course/year/section.
- **Question types:** **Multiple choice** (MCQ) or **QR scan** (stages completed by scanning QR codes).

### 8.2 Active Quests

- **URL:** `/quests/active`
- **Purpose:** List quests that are **approved** and **upcoming or ongoing** (not yet completed/cancelled). From here you can open a quest to view details, edit (admin), print QR codes, or delete.
- **Filters:** Search, quest type, “Created by me,” sort.
- **Actions (per quest):**
  - **View** — See quest details, stages, and status.
  - **Print QR** (admin/professor) — Generate printable QR codes for each stage (used at physical locations for join/play).
  - **Edit** (admin only) — Change quest settings and stages.
  - **Delete** (admin/professor for own; admin for any) — Cancel/remove quest (subject to rules).

### 8.3 Approval (admin only)

- **URL:** `/quests/approval`
- **Purpose:** List quests by **approval status**: Pending, Approved, Rejected. Professors submit quests; **admins approve or reject** them. Until approved, a quest is not visible to players as “active.”
- **Actions:** Filter by status, open a quest, **Approve** or **Reject**. Approved quests become active (if within their start/end window).

### 8.4 Created Quests (professor view)

- **URL:** `/quests/created` (for professors this is under Quests; sidebar may label it “Approval” as in “my created”).
- **Purpose:** List quests **created by the current user** (professor or admin). Same approval status filters; professors can cancel their own pending quests.

### 8.5 Quest History

- **URL:** `/quests/history`
- **Purpose:** List quests that are **completed** or **cancelled**. Optional filter “Created by me” to see only your quests.

### 8.6 Creating a new quest (step-by-step)

1. **Open create**
   - From **Active** or **Created**, click **Create quest** (or go to `/quests/create`).

2. **Step 1 — Quest details (form)**
   - **Target:** Everyone, or specific **course** / **year level** / **section** (and for enrollment, **semester**).
   - **Title**, **Description**
   - **Quest type:** Daily | Event | Custom | Enrollment (professors only Custom/Event).
   - **Question type:** Multiple choice | QR scan.
   - **Number of stages** (e.g. 3).
   - **Elimination:** If enabled, lowest scorers per stage can be eliminated; set **max participants**, and per-stage **max survivors** and **minimum participants** in the stages step.
   - **Buy-in points** (optional) — Deducted when a player joins.
   - **Reward points** — Points given on completion (and optionally **custom prize** text).
   - **Max participants** (optional; required for elimination).
   - **Start date** / **End date** — When the quest is open for joining and playing.
   - **Creation cost points** (optional) — Cost for the creator to submit the quest.
   - Submit to go to **stages**.

3. **Step 2 — Stages** (`/quests/create/stages` or `/quests/{id}/edit/stages`)
   - One screen per stage (e.g. “Stage 1 of 3”). Navigate with **Back** / **Next**.
   - **Per stage:**
     - **Location hint** — Shown to players (e.g. “Near the library”); required.
     - **Stage deadline** — When this stage closes (for elimination, required for all but the last; last can use quest end date).
     - **Stage start** (optional) — When this stage opens (if after quest start).
     - For **elimination:** **Max survivors**, **Minimum participants** (so the quest can’t be stuck if too many quit).
     - For **MCQ:** **Passing score** (how many questions must be correct to advance); **Questions** with **choices** and one correct answer per question.
     - For **QR scan:** No questions; player completes the stage by scanning the stage QR.
   - On the **last stage**, click **Create quest** (or **Save** when editing). The quest is created in **pending** status (for professors) or can be approved by an admin.

4. **After creation**
   - **Professor:** Quest appears under **Created** with status Pending until an **admin** approves it in **Quests → Approval**.
   - **Admin:** Can approve their own quest from **Approval** or **Active** (depending on implementation).
   - Once **approved** and within the start/end window, the quest appears in **Active** and players can join (e.g. by scanning the first-stage QR or from the app).

### 8.7 Editing a quest (admin only)

- **URL:** `/quests/{id}/edit` then **Edit stages** → `/quests/{id}/edit/stages`
- Same fields as create. You can change target, dates, rewards, stages, questions, elimination settings, etc. Save updates the quest; approval status is unchanged unless you explicitly approve/reject.

### 8.8 Print QR codes

- From a quest’s detail or **Active** list, use **Print QR**.
- **URL:** `/quests/{id}/print-qr`
- Generates a page with **one QR per stage**. Each QR encodes the URL path `/quests/{quest_id}/stages/{stage_id}` so that when players scan it, the app can resolve the quest and stage (join on stage 1, play on current stage). Print and place QR codes at the physical locations indicated by each stage’s location hint.

### 8.9 Quest flow summary (admin perspective)

| Step | Who | Where | What |
|------|-----|--------|------|
| Create quest | Admin or Professor | Create quest → form → stages | Set target, type, dates, rewards, stages (MCQ or QR). |
| Submit | Professor | — | Quest saved as **Pending**. |
| Approve / Reject | Admin | Quests → Approval | Pending quests become Active or Rejected. |
| Print QR | Admin / Professor | Quest detail or Active → Print QR | Get QR codes for each stage. |
| Run quest | — | Players (app/simulation) | Players join via first QR, play stages (MCQ or QR), get points/rewards. |
| Review | Admin / Professor | Active, History, Logs, Transactions | Monitor participation, points, and activity. |

---

## Quick reference — Where to do what

| Task | Location |
|------|----------|
| Sign in as admin | `/login` — `admin@email.com` / `admin123` (then change password) |
| Add students (who can register as players) | Academic Management → Students |
| Add professors (who can be gamemasters) | Academic Management → Professors |
| Add semester | Academic Management → Semester |
| Register a test student | Simulation → `/simulation/student-register` |
| Log in as that student | Simulation → `/simulation/login` |
| Create a gamemaster (user with professor/admin role) | Users → Add Admin/Gamemaster (pick from professors masterlist) |
| Create store items (redeem with points) | Store & Achievements → Store |
| Create achievements (level, quest count, quest win, complete quest) | Store & Achievements → Achievements |
| View activity log | Logs and Transactions → Activity Logs |
| View points transactions | Logs and Transactions → Points Transactions |
| See active quests | Quests → Active |
| Approve/reject professor quests | Quests → Approval (admin only) |
| Create a new quest | Quests → Active → Create quest → form → stages |
| Edit a quest | Quests → Active → [Quest] → Edit (admin only) |
| Print QR codes for a quest | Quests → Active → [Quest] → Print QR |
| See completed/cancelled quests | Quests → History |

---

*This guide reflects the current CampusGo admin panel. If a menu or URL changes, update this document accordingly.*
