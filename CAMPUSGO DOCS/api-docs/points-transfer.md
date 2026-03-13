# Points transfer (sharing)

Only **students** can search for other students and transfer points. Transferred points update both users’ balances and are logged as `transfer_out` / `transfer_in` in point transactions and activity log. Transferred points do **not** count toward XP or leaderboard.

**Limits:** Min **10** points, max **100** points per transfer.

---

## Search students

**GET** `/api/students/search`  
**Auth:** Yes (Bearer)

Search for a student by their **school_id** (student number from the masterlist). Use the result to show the recipient in the app and then call **POST /api/points/transfer** with `to_user_id` and `amount`.

**Query parameters**
| Parameter   | Type   | Required | Description |
|------------|--------|----------|-------------|
| student_id | string | Yes      | School ID / student number of the recipient (e.g. `2024-001`). |

**Response** `200 OK`
```json
{
  "student": {
    "id": 5,
    "name": "Doe, John",
    "email": "john@example.com",
    "school_id": "2024-001",
    "first_name": "John",
    "last_name": "Doe",
    "course": "BSCS",
    "year_level": 2,
    "section": "2A",
    "points_balance": 80
  }
}
```

**Errors**
- `401` — Missing or invalid token.
- `403` — Authenticated user is not a student. Body: `{ "message": "Only students can transfer points." }`.
- `404` — No student found with that school_id, or not a student. Body: `{ "message": "Student not found." }`.
- `422` — Validation failed (e.g. missing `student_id`). Body: `{ "message": "...", "errors": { ... } }`.

---

## Transfer points

**POST** `/api/points/transfer`  
**Auth:** Yes (Bearer)

Transfer points from the authenticated user (sender) to another student. Sender and receiver must both be students. Amount must be between 10 and 100, and sender must have enough balance.

**Request body** (JSON)
| Field       | Type    | Required | Description |
|------------|---------|----------|-------------|
| to_user_id | integer | Yes      | User ID of the recipient (from search). |
| amount     | integer | Yes      | Points to transfer (10–100). |

**Response** `200 OK`
```json
{
  "message": "Points transferred successfully.",
  "points_balance": 50,
  "transferred": {
    "to_user_id": 5,
    "to_name": "Doe, John",
    "amount": 50
  }
}
```
- `points_balance`: Sender’s new balance after the transfer.

**Errors**
- `401` — Missing or invalid token.
- `422` — Validation or business rule failed. Possible `errors`: `amount` (e.g. "Only students can transfer points.", "Not enough points."), `to_user_id` (e.g. "You can only transfer to another student.", "You cannot transfer to yourself."). Body: `{ "message": "...", "errors": { ... } }`.
