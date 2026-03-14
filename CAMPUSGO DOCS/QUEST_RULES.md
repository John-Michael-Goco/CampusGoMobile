# Quest rules (mobile & web)

Rules for the four quest types: **elimination vs non-elimination** and **QR-only vs MCQ (multiple choice)**. Same rules apply on mobile and web.

---

## 1. Elimination + QR only

- **Join:** Scan the first-stage QR to join. One QR per stage.
- **Play:** Go to each stage location and scan that stage’s QR. No questions, no choices — scanning the correct QR completes the stage.
- **Elimination:** You are **eliminated** if the stage deadline has passed or max participants has already been hit. Your status becomes eliminated; you cannot continue to the next stage.
- **Winning:** First participant(s) to complete all stages (scan all QRs in order) within the rules win. Eliminated players are out.

---

## 2. Elimination + MCQ (multiple choice)

- **Join:** Scan the first-stage QR to join. You may need to scan the stage QR to reveal the questions.
- **Play:** At each stage, answer all the multiple-choice questions. Wait for every participant to submit or for the stage to end to know if you are eliminated or proceed/win.
- **Elimination:** Highest score wins; time submitted is the tie breaker.
- **Winning:** First participant(s) to complete all stages with passing scores win. Eliminated players are out.

---

## 3. Non-elimination + QR only

- **Join:** Scan the first-stage QR to join.
- **Play:** Go to each stage and scan that stage’s QR. No questions — scanning the correct QR completes the stage.
- **Winning / completion:** Participants who complete all stages (scan all QRs in order) receive rewards. Leaderboard may still rank by completion time.

---

## 4. Non-elimination + MCQ

- **Join:** Scan the first-stage QR to join.
- **Play:** At each stage, answer all multiple-choice questions. You need to meet the **passing score** to advance.
- Not meeting the passing score will get you **removed** from the quest. You cannot retry unless it is a daily quest.
- **Winning / completion:** Participants who pass all stages get completion rewards. Leaderboard may rank by score and/or time.

---

*These rules align with the API: `question_type` (`qr_scan` / `multiple_choice`) and `is_elimination` on quest and play responses.*
