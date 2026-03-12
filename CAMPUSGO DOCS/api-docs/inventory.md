# Inventory

## Inventory — List (step 1.9)

**GET** `/api/user/inventory`  
**Auth:** Yes (Bearer)

Returns the authenticated user's inventory: items from store redeems and quest custom prizes.

**Response** `200 OK`
```json
{
  "inventory": [
    {
      "id": 5,
      "item_id": 1,
      "quantity": 2,
      "acquired_at": "2026-03-10 14:00:00",
      "source_quest_id": null,
      "store_item": {
        "id": 1,
        "name": "Coffee Voucher",
        "description": "One free coffee at the canteen."
      },
      "custom_prize_description": null
    },
    {
      "id": 6,
      "item_id": null,
      "quantity": 1,
      "acquired_at": "2026-03-11 10:00:00",
      "source_quest_id": 3,
      "store_item": null,
      "custom_prize_description": "Quest reward"
    }
  ]
}
```
- `store_item`: Present when the entry is from a store redeem (`item_id` set). Null for quest custom prizes.
- `custom_prize_description`: Set for quest rewards when `item_id` is null.

**Errors**
- `401` — Missing or invalid token.

---

## Inventory — Use item (step 1.9, 2.6)

**POST** `/api/user/inventory/use`  
**POST** `/api/user/inventory/{inventory}/use`  
**Auth:** Yes (Bearer)

Use one unit of an item. Decrements quantity (or removes the entry if quantity was 1). **Logs the use** in the activity log (`item_used`) so it appears in the items-used history.

- **Body form:** Send `store_item_id` or `inventory_id` in the request body to `POST /api/user/inventory/use`.
- **URL form (step 2.6):** Send no body to `POST /api/user/inventory/{inventory}/use` to use the inventory entry with that id (must belong to the user).

**Request body** (JSON) for `/api/user/inventory/use` — provide one of:
| Field           | Type | Required | Description |
|-----------------|------|----------|-------------|
| store_item_id   | int  | No*      | Store item ID (uses one from the oldest-acquired stack). |
| inventory_id    | int  | No*      | Specific inventory entry ID. |

\* Provide either `store_item_id` or `inventory_id`.

**Response** `200 OK`
```json
{
  "message": "Item used.",
  "item_name": "Coffee Voucher",
  "remaining_quantity": 1
}
```

**Errors**
- `401` — Missing or invalid token.
- `422` — Validation failed or user has none of this item. Body: `{ "message": "...", "errors": { ... } }`

---

## Inventory — History of items used

**GET** `/api/user/inventory/history`  
**Auth:** Yes (Bearer)

Returns a paginated history of items the user has used (from the activity log). Each use is logged when calling `POST /api/user/inventory/use`.

**Query parameters**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| per_page  | int  | No       | 20      | Items per page (1–50). |
| page      | int  | No       | 1       | Page number. |

**Response** `200 OK`
```json
{
  "history": [
    {
      "item_name": "Coffee Voucher",
      "used_at": "2026-03-12 09:30:00"
    }
  ],
  "pagination": {
    "current_page": 1,
    "per_page": 20,
    "total": 5,
    "last_page": 1
  }
}
```

**Errors**
- `401` — Missing or invalid token.
