# Auth & User

## Auth — Sign in

**POST** `/api/auth/signin`  
**Auth:** No

**Request body** (JSON)
| Field     | Type   | Required | Description                    |
|----------|--------|----------|--------------------------------|
| email    | string | Yes      | User email                     |
| password | string | Yes      | User password                  |
| device_name | string | No    | Token name (default: `"api"`)  |

**Response** `200 OK`
```json
{
  "token": "<plain_text_token>",
  "token_type": "Bearer",
  "user": {
    "id": 1,
    "name": "Doe, Jane",
    "email": "jane@example.com",
    "role": "student",
    "points_balance": 100,
    "level": 2,
    "total_xp_earned": 250,
    "total_completed_quests": 3,
    "profile_image": null,
    "student": {
      "student_number": "2020-001",
      "first_name": "Jane",
      "last_name": "Doe",
      "course": "BSCS",
      "year_level": 4,
      "section": "4A"
    }
  }
}
```
- When `role` is `"student"` and the user is linked to a master record, the `user` object includes an optional **`student`** object (see **User profile — extended** for the full `student` shape). For other roles, `student` is omitted.

**Errors**
- `422` — Validation failed (e.g. missing email/password, invalid email). Body: `{ "message": "...", "errors": { "email": ["..."] } }`
- Invalid credentials: `errors.email` = `["The provided credentials are incorrect."]`

---

## Auth — Sign up

**POST** `/api/auth/signup`  
**Auth:** No

Register a new student. Student must exist in the masterlist and match the given details.

**Request body** (JSON)
| Field          | Type    | Required | Description                    |
|----------------|---------|----------|--------------------------------|
| student_number | string  | Yes      | School ID from masterlist      |
| first_name     | string  | Yes      | Must match masterlist           |
| last_name      | string  | Yes      | Must match masterlist           |
| course         | string  | Yes      | Must match masterlist           |
| year_level     | integer | Yes      | 1–10, must match masterlist    |
| email          | string  | Yes      | Unique; used for login          |
| password       | string  | Yes      | Min 8 characters                |
| device_name    | string  | No       | Token name (default: `"api"`)   |

**Response** `200 OK`  
Same shape as signin: `token`, `token_type`, `user` (minimal user object).

**Errors**
- `422` — Validation or business rule failed. Possible `errors`: `student_number` (not found, already registered, or details don't match), `email` (e.g. already taken).

---

## Auth — Sign out

**POST** `/api/auth/signout`  
**Auth:** Yes (Bearer)

Invalidates the current access token. After this, the token cannot be used for authenticated requests.

**Response** `200 OK`
```json
{
  "message": "Signed out successfully."
}
```

**Errors**
- `401` — Missing or invalid token. Body: `{ "message": "Unauthenticated." }`

---

## User profile — extended (step 1.5)

**GET** `/api/user`  
**Auth:** Yes (Bearer)

Returns the authenticated user's profile for the app: profile screen and AR (e.g. level-up comparison). Same shape is returned after signin/signup and in the update-profile response.

**Response** `200 OK`
```json
{
  "id": 1,
  "name": "Doe, Jane",
  "email": "jane@example.com",
  "role": "student",
  "points_balance": 100,
  "level": 2,
  "total_xp_earned": 250,
  "total_completed_quests": 3,
  "profile_image": null,
  "student": {
    "student_number": "2020-001",
    "first_name": "Jane",
    "last_name": "Doe",
    "course": "BSCS",
    "year_level": 4,
    "section": "4A"
  }
}
```
- `profile_image`: Full URL for display (e.g. `https://example.com/storage/profile-images/abc.jpg`) or null. Use for profile/AR UI.
- **User object — student data:** When `role` is `"student"` and the user has a linked master record, the response includes a **`student`** object. For admin/professor or students without a master link, `student` is omitted.

| Field (inside `student`) | Type    | Description |
|--------------------------|---------|-------------|
| student_number           | string  | School ID from masterlist. |
| first_name               | string  | First name from masterlist. |
| last_name               | string  | Last name from masterlist. |
| course                   | string  | Course (e.g. BSCS). |
| year_level               | integer | Year level (1–10). |
| section                  | string  | Section (e.g. 4A); may be null. |

**Errors**
- `401` — Missing or invalid token. Body: `{ "message": "Unauthenticated" }`

---

## User — Change password

**PUT** `/api/user/password`  
**Auth:** Yes (Bearer)

**Request body** (JSON)
| Field             | Type   | Required | Description                          |
|-------------------|--------|----------|--------------------------------------|
| current_password  | string | Yes      | Current password.                    |
| password          | string | Yes      | New password (must meet app rules).  |
| password_confirmation | string | Yes | Must match `password`.               |

**Response** `200 OK`
```json
{
  "message": "Password updated successfully."
}
```

**Errors**
- `401` — Missing or invalid token.
- `422` — Validation failed (e.g. wrong current password, password too weak, confirmation mismatch). Body: `{ "message": "...", "errors": { "current_password": ["..."], ... } }`

---

## User — Update profile picture

**POST** `/api/user/profile`  
**Auth:** Yes (Bearer)  
**Content-Type:** `multipart/form-data`

**Request body** (form data)
| Field                 | Type    | Required | Description                                      |
|-----------------------|---------|----------|--------------------------------------------------|
| profile_image         | file    | No       | New image (jpeg, png, jpg, gif, webp; max 2MB).  |
| remove_profile_image  | boolean | No       | Set to `1` or `true` to remove current image.   |

Send either a new `profile_image` file or `remove_profile_image=1`, or both (remove then upload = replace).

**Response** `200 OK`
```json
{
  "message": "Profile updated successfully.",
  "user": {
    "id": 1,
    "name": "Doe, Jane",
    "email": "jane@example.com",
    "role": "student",
    "points_balance": 100,
    "level": 2,
    "total_xp_earned": 250,
    "total_completed_quests": 3,
    "profile_image": "https://example.com/storage/profile-images/abc123.jpg",
    "student": {
      "student_number": "2020-001",
      "first_name": "Jane",
      "last_name": "Doe",
      "course": "BSCS",
      "year_level": 4,
      "section": "4A"
    }
  }
}
```
- If no changes were sent, `message` is `"No changes made."` and `user` reflects current profile. The `user` object uses the same shape as GET `/api/user` (including optional `student` when role is student).

**Errors**
- `401` — Missing or invalid token.
- `422` — Validation failed (e.g. file too large, invalid mime). Body: `{ "message": "...", "errors": { ... } }`

---

## User — Points transaction history (step 1.11)

**GET** `/api/user/transactions`  
**Auth:** Yes (Bearer)

Returns the authenticated user's points transaction history (paginated). Used for "Transaction history" or "Points history" in the app.

**Query parameters**
| Parameter   | Type   | Required | Description                                              |
|------------|--------|----------|----------------------------------------------------------|
| page       | int    | No       | Page number (default: 1).                                |
| per_page   | int    | No       | Items per page, 1–50 (default: 20).                      |
| type       | string | No       | Filter by `transaction_type` (e.g. `quest_reward`, `store_redeem`). |
| date_from  | string | No       | Filter: transactions on or after this date (Y-m-d).      |
| date_to    | string | No       | Filter: transactions on or before this date (Y-m-d).     |

**Response** `200 OK`
```json
{
  "transactions": [
    {
      "id": 1,
      "amount": 50,
      "transaction_type": "quest_reward",
      "type_label": "Quest reward",
      "reference_id": null,
      "created_at": "2026-03-10 14:30:00"
    }
  ],
  "points_balance": 150,
  "pagination": {
    "current_page": 1,
    "per_page": 20,
    "total": 42,
    "last_page": 3
  }
}
```
- `amount`: positive = credit, negative = debit.
- `type_label`: human-readable label for the transaction type.

**Errors**
- `401` — Missing or invalid token.

---

## User — Activity log (step 1.12)

**GET** `/api/user/activity`  
**Auth:** Yes (Bearer)

Returns the authenticated user's activity log: actions they performed (e.g. quest_joined, store_redeem, achievement_earned) with a human-readable label and optional detail. Used for "My activity" or "History" screen in the app.

**Query parameters**
| Parameter   | Type   | Required | Description                                              |
|------------|--------|----------|----------------------------------------------------------|
| page       | int    | No       | Page number (default: 1).                                |
| per_page   | int    | No       | Items per page, 1–50 (default: 20).                      |
| action     | string | No       | Filter by action key prefix (e.g. `quest_` for quest-related). |
| date_from  | string | No       | Filter: entries on or after this date (Y-m-d).           |
| date_to    | string | No       | Filter: entries on or before this date (Y-m-d).         |

**Response** `200 OK`
```json
{
  "activity": [
    {
      "id": 1,
      "action_key": "quest_joined",
      "detail": "Campus Tour Quest",
      "display_label": "Joined quest",
      "timestamp": "2026-03-10 14:30:00"
    }
  ],
  "pagination": {
    "current_page": 1,
    "per_page": 20,
    "total": 15,
    "last_page": 1
  }
}
```
- `action_key`: parsed key (e.g. `quest_joined`, `store_redeem`).
- `detail`: optional human-readable detail stored with the action (e.g. quest title).
- `display_label`: human-readable label for the app to show without parsing.

**Errors**
- `401` — Missing or invalid token.
