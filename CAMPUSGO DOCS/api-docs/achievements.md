# Achievements

## Achievements — List all (step 1.7)

**GET** `/api/achievements`  
**Auth:** Yes (Bearer)

Returns all achievement definitions. When authenticated, each achievement includes `earned` (true/false) and `earned_at` (when the user earned it, or null) so the app can show "all achievements" and which are locked vs unlocked.

**Response** `200 OK`
```json
{
  "achievements": [
    {
      "id": 1,
      "name": "First Quest",
      "description": "Complete your first quest.",
      "requirement_type": "quest_count",
      "requirement_value": 1,
      "image_url": null,
      "earned": true,
      "earned_at": "2026-03-10 14:30:00"
    },
    {
      "id": 2,
      "name": "Level 5",
      "description": "Reach level 5.",
      "requirement_type": "level",
      "requirement_value": 5,
      "image_url": null,
      "earned": false,
      "earned_at": null
    }
  ]
}
```
- `requirement_type`: e.g. `quest_count`, `level`, `quest_win`, `complete_quest`. `requirement_value` is the threshold (e.g. number of quests, level number, or quest id for `complete_quest`).
- `image_url`: Reserved; currently always `null`.

**Errors**
- `401` — Missing or invalid token.

---

## User achievements — List earned (step 1.8)

**GET** `/api/user/achievements`  
**Auth:** Yes (Bearer)

Returns achievements the authenticated user has earned (for profile "My achievements" or AR after unlock). When a user earns an achievement (e.g. after completing a quest or leveling up), it is **logged** in the activity log (`achievement_earned`).

**Response** `200 OK`
```json
{
  "achievements": [
    {
      "user_achievement_id": 10,
      "achievement_id": 1,
      "name": "First Quest",
      "description": "Complete your first quest.",
      "requirement_type": "quest_count",
      "requirement_value": 1,
      "earned_at": "2026-03-10 14:30:00"
    }
  ]
}
```

**Errors**
- `401` — Missing or invalid token.
