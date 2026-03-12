# Leaderboard

## Leaderboard (step 1.10)

**GET** `/api/leaderboard`  
**Auth:** Yes (Bearer)

Returns leaderboard rankings by period. When authenticated, response includes the current user's rank and value for that period.

**Query parameters**
| Parameter | Type   | Required | Default | Description |
|-----------|--------|----------|---------|-------------|
| period    | string | No       | `week`  | One of: `today`, `week`, `month`, `semester`, `overall` |

**Response** `200 OK`
```json
{
  "entries": [
    {
      "rank": 1,
      "user_id": 5,
      "user_name": "Jane Doe",
      "value": 120
    }
  ],
  "period": "week",
  "periods": ["today", "week", "month", "semester", "overall"],
  "value_label": "Points gained",
  "my_rank": 3,
  "my_value": 85
}
```
- For `period=overall`, `value_label` is `"Total XP earned"` and `value` is total XP.
- For other periods, `value` is points gained in that period.
- `my_rank`, `my_value`: Present when authenticated; the current user's rank (1-based) and value in this period, or `null` if not on the list.

**Errors**
- `401` — Missing or invalid token.
