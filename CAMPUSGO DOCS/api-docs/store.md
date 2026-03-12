# Store

## Store — List items (step 1.6)

**GET** `/api/store`  
**Auth:** Yes (Bearer)

Returns redeemable store items (visible only) with the user's current points and per-item availability and affordability.

**Response** `200 OK`
```json
{
  "points_balance": 150,
  "items": [
    {
      "id": 1,
      "name": "Coffee Voucher",
      "description": "One free coffee at the canteen.",
      "cost_points": 50,
      "stock": 20,
      "start_date": "2026-03-01 00:00:00",
      "end_date": "2026-06-30 23:59:59",
      "is_available": true,
      "can_afford": true,
      "image_url": null
    }
  ]
}
```
- `is_available`: Item is within its start/end window and can be redeemed.
- `can_afford`: User's `points_balance` ≥ item's `cost_points`.
- `image_url`: Reserved for future use; currently always `null`.

**Errors**
- `401` — Missing or invalid token.

---

## Store — Redeem item (step 1.6)

**POST** `/api/store/redeem`  
**Auth:** Yes (Bearer)

Redeem a store item: deducts points, creates point transaction, adds to user inventory, decrements stock. **Logs the redemption** in activity log (`store_redeem`).

**Request body** (JSON)
| Field          | Type   | Required | Default | Description        |
|----------------|--------|----------|---------|--------------------|
| store_item_id  | int    | Yes      | —       | Store item ID.     |
| quantity       | int    | No       | `1`     | Quantity to redeem.|

**Response** `200 OK`
```json
{
  "message": "Redeemed successfully.",
  "points_balance": 100,
  "redeemed": {
    "store_item_id": 1,
    "name": "Coffee Voucher",
    "quantity": 1,
    "cost_points": 50
  }
}
```

**Errors**
- `401` — Missing or invalid token.
- `422` — Validation or business rule failed: item not available, not visible, insufficient stock, insufficient points. Body: `{ "message": "...", "errors": { "store_item_id": ["..."], ... } }`
