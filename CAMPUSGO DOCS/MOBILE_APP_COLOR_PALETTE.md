# CampusGo Mobile App — Color Palette

Use these colors in your Android (or iOS) app to match the web admin and simulation UI. All hex values are for direct use in mobile (e.g. `#059669` in XML or Compose).

---

## 1. Neutrals (backgrounds, text, borders)

Use **zinc** for mobile screens (simulation uses zinc). **Slate** is used on the admin dashboard cards; you can use either for a consistent gray scale.

### Zinc (primary neutral in simulation)

| Name     | Hex       | Use |
|----------|-----------|-----|
| Zinc 50  | `#fafafa` | (optional) Lightest background |
| Zinc 100 | `#f4f4f5` | **Screen background (light)** |
| Zinc 200 | `#e4e4e7` | **Borders, dividers (light)** |
| Zinc 300 | `#d4d4d8` | Disabled / subtle borders |
| Zinc 400 | `#a1a1aa` | **Secondary text (light)** |
| Zinc 500 | `#71717a` | Placeholder, tertiary text |
| Zinc 600 | `#52525b` | **Secondary text (dark mode)** |
| Zinc 700 | `#3f3f46` | **Borders, cards (dark)** |
| Zinc 800 | `#27272a` | **Card/surface (dark)** |
| Zinc 900 | `#18181b` | **Screen background (dark)** |
| White    | `#ffffff`  | **Card background (light)** |

### Slate (optional, for headings)

| Name     | Hex       | Use |
|----------|-----------|-----|
| Slate 100 | `#f1f5f9` | Soft background |
| Slate 600 | `#475569` | Muted text |
| Slate 700 | `#334155` | Subheadings |
| Slate 800 | `#1e293b` | Headings (dark) |
| Slate 900 | `#0f172a` | Primary text (dark) |

**Text semantics (light mode):** Primary text `#18181b` (zinc-900) or `#0f172a` (slate-900). Secondary/muted `#71717a` (zinc-500) or `#52525b` (zinc-600).

**Text semantics (dark mode):** Primary `#f4f4f5` (zinc-100). Muted `#a1a1aa` (zinc-400) or `#71717a` (zinc-500).

---

## 2. Brand / primary actions (emerald)

Emerald is used for **sign-in header**, **success states**, **primary CTA** (e.g. “Register”, “Go to log in”), and **links** in the simulation.

| Name        | Hex       | Use |
|-------------|-----------|-----|
| Emerald 400 | `#34d399` | Success text, links (dark mode) |
| Emerald 500 | `#10b981` | — |
| Emerald 600 | `#059669` | **Primary CTA, header bar, success (light)** |
| Emerald 700 | `#047857` | **Primary CTA hover / dark header bar** |

**Primary button / header:** `#059669` (light), `#047857` (dark).

---

## 3. Points, quests, achievements (amber)

Amber is used for **points**, **leaderboard**, **achievements**, **quests**, and **simulation “status bar”** on several screens.

| Name        | Hex       | Use |
|-------------|-----------|-----|
| Amber 100   | `#fef3c7` | Achievement badge background (light) |
| Amber 400   | `#fbbf24` | **Accent text (dark mode)** |
| Amber 500   | `#f59e0b` | **Points, rank, status bar (light)** |
| Amber 600   | `#d97706` | **Points, rank, status bar (dark)** |

**Points / rank / “simulation” bar:** `#f59e0b` (light), `#d97706` (dark).

---

## 4. Leaderboard, rewards (violet)

Violet is used for **leaderboard** and **store** CTAs on the web.

| Name        | Hex       | Use |
|-------------|-----------|-----|
| Violet 600  | `#7c3aed` | **Leaderboard accent, primary actions** |
| Violet 700  | `#6d28d9` | Pressed/hover state |

---

## 5. Achievers / secondary accent (rose)

Rose is used for **“Top achievers”** and similar secondary accents.

| Name       | Hex       | Use |
|------------|-----------|-----|
| Rose 400   | `#fb7185` | Accent (dark mode) |
| Rose 600   | `#e11d48` | **Achievers, badges** |

---

## 6. Calendar / info (blue)

Blue is used for **semester/calendar** and **info** on the admin dashboard.

| Name       | Hex       | Use |
|------------|-----------|-----|
| Blue 400   | `#60a5fa` | Links (dark mode) |
| Blue 500   | `#3b82f6` | **Info, calendar** |
| Blue 600   | `#2563eb` | **Links, info (light)** |

---

## 7. Feedback / states

### Success

| Use        | Hex       |
|------------|-----------|
| Background | `#ecfdf5` (emerald-50) |
| Text/icon  | `#059669` (emerald-600) |
| Toast (app) | `#166534` (green-800, used in codebase) |

### Error / destructive

| Use        | Hex       |
|------------|-----------|
| Background | `#fef2f2` (red-50) or `#450a0a` (red-950) in dark |
| Text/icon  | `#b91c1c` (red-700) or `#fca5a5` (red-300) in dark |
| Toast (app) | `#991b1b` (red-800, used in codebase) |

### Warning

| Use   | Hex       |
|-------|-----------|
| Text  | `#d97706` (amber-600) |
| BG    | `#fffbeb` (amber-50) |

### Info

| Use  | Hex       |
|------|-----------|
| Text | `#2563eb` (blue-600) |

---

## 8. Theme tokens (from web CSS)

The web app uses **oklch** in `resources/css/app.css`. For mobile, use the hex equivalents below so the app feels consistent.

| Token        | Light (hex approx.) | Dark (hex approx.) | Use |
|--------------|----------------------|---------------------|-----|
| Background   | `#ffffff`            | `#18181b` (zinc-900) | Screen |
| Foreground   | `#18181b` (zinc-900) | `#fafafa` (zinc-50)  | Primary text |
| Card         | `#ffffff`            | `#18181b`            | Cards, sheets |
| Card text    | `#18181b`            | `#fafafa`            | Text on cards |
| Primary      | `#27272a` (zinc-800) | `#fafafa`            | Buttons (admin) |
| Primary text | `#fafafa`            | `#27272a`            | On primary button |
| Muted        | `#f4f4f5` (zinc-100) | `#3f3f46` (zinc-700) | Muted BG |
| Muted text   | `#71717a` (zinc-500) | `#a1a1aa` (zinc-400) | Secondary text |
| Border       | `#e4e4e7` (zinc-200) | `#3f3f46` (zinc-700) | Borders |
| Destructive  | `#dc2626` (red-600)  | `#b91c1c` (red-700)  | Delete, errors |

---

## 9. Quick reference by screen type

| Area              | Main color   | Hex (primary) |
|-------------------|-------------|----------------|
| Auth / sign-in    | Emerald     | `#059669`      |
| Points / rank     | Amber       | `#f59e0b` / `#d97706` |
| Leaderboard       | Violet      | `#7c3aed`      |
| Store / redeem    | Emerald + Violet | `#059669`, `#7c3aed` |
| Achievements      | Amber + Rose| `#f59e0b`, `#e11d48` |
| Quests            | Amber       | `#f59e0b`      |
| Success message   | Emerald     | `#059669`      |
| Error message     | Red         | `#b91c1c`      |
| Screen background | Zinc 100/900 | `#f4f4f5` / `#18181b` |
| Card surface      | White / Zinc 800 | `#ffffff` / `#27272a` |
| Borders / dividers| Zinc 200/700 | `#e4e4e7` / `#3f3f46` |

---

## 10. Dark mode summary

| Role           | Light      | Dark        |
|----------------|------------|------------|
| Screen BG      | `#f4f4f5`  | `#18181b`  |
| Card BG        | `#ffffff`  | `#27272a`  |
| Primary text    | `#18181b`  | `#f4f4f5`  |
| Secondary text | `#52525b`  | `#a1a1aa`  |
| Border         | `#e4e4e7`  | `#3f3f46`  |
| Primary CTA    | `#059669`  | `#047857`  |
| Points/amber   | `#f59e0b`  | `#d97706`  |

Use this palette in your mobile theme (e.g. Android `colors.xml` / Material Theme or Compose `Color`, or iOS asset catalog) so the app matches the web and simulation.
