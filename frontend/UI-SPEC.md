# UI Spec — Right-Side Panel Upgrade (Issue #2)

## Layout Overview

```
┌─────────────────────────────────────┬──────────────────────────────┐
│  Log Manager                [RESET] │  RabbitMQ Live Feed  (~20%)  │
│─────────────────────────────────────│  (STOMP WebSocket messages)  │
│  [New group name...] [Create]       │──────────────────────────────│
│─────────────────────────────────────│                              │
│ Groups    │ Group Detail            │  External APIs Mock  (~80%)  │
│ (1/3)     │ (2/3)                   │                              │
│           │ name, status, close btn │  ┌─ Setup Controls ────────┐ │
│ > Group 1 │                         │  │ Noark A: [200▾] [0ms]   │ │
│   Group 2 │ [Add entry...] [Add]    │  │ Noark B: [200▾] [0ms]   │ │
│   Group 3 │                         │  └─────────────────────────┘ │
│           │ entry 1                 │                              │
│           │ entry 2                 │  ┌─ Noark A History ───────┐ │
│           │ entry 3                 │  │ POST /api/noarka  12:01  │ │
│           │                         │  │ POST /api/noarka  12:02  │ │
│─────────────────────────────────────│  └─────────────────────────┘ │
│  Edge / MongoDB                     │                              │
│  ┌──────┐ ┌──────┐ ┌──────┐        │  ┌─ Noark B History ───────┐ │
│  │Grp 1 │ │Grp 2 │ │Grp 3 │        │  │ POST /api/noarkb  12:01  │ │
│  │ARCHVD│ │FAILD │ │PNDNG │        │  │ POST /api/noarkb  12:02  │ │
│  └──────┘ └──────┘ └──────┘        │  └─────────────────────────┘ │
│  (expandable cards)                 │                              │
└─────────────────────────────────────┴──────────────────────────────┘
```

## Panel Header Colors (subtle accents)

Each panel has a thin left border or header background tint:

| Panel | Header accent | Badge/text color |
|-------|--------------|-----------------|
| Log Manager | `blue-600` border-left or `blue-50` bg | `text-blue-800` |
| RabbitMQ | `purple-600` border-left or `purple-50` bg | `text-purple-800` |
| Edge (MongoDB) | `amber-600` border-left or `amber-50` bg | `text-amber-800` |
| External APIs Mock | `emerald-600` border-left or `emerald-50` bg | `text-emerald-800` |

## Component Extraction

Extract 3 new standalone components from the current inline panels:

```
frontend/src/app/
├── layout/
│   └── layout.ts              # Orchestrator: left panel + hosts child components
├── panels/
│   ├── rabbitmq-panel.ts      # RabbitMQ live STOMP feed
│   ├── edge-panel.ts          # Edge/MongoDB archive group cards
│   └── mock-panel.ts          # Mock history + setup controls
├── services/
│   ├── rabbitmq-panel.service.ts  # STOMP WebSocket (rewritten)
│   ├── edge-panel.service.ts      # Polls GET /api/groups
│   ├── mock-panel.service.ts      # Polls history + config
│   └── log-manager-api.service.ts # Existing
└── models/
    └── ...
```

## Left Side (55%)

### Header
- "Log Manager" title on the left
- Red RESET button top-right: `bg-red-600 text-white text-xs px-3 py-1.5 rounded-lg`
- Clicking RESET shows a native `confirm()` dialog, then fires parallel reset calls

### Groups + Detail (existing, no major changes)
- Stays as-is, takes ~65% of the left side height

### Edge Panel (new location — bottom of left side, ~35% height)
- Horizontal scrollable row of group cards, or a compact grid
- Each card shows:
  - Group name (bold)
  - Status badge (color-coded, see below)
  - Retry count (small text)
- Clicking a card expands it inline to show entries + errors JSON
- FAILED cards show a "Retry" button

**Status badge colors:**
| Status | Badge classes |
|--------|-------------|
| ARCHIVED | `bg-green-100 text-green-800` |
| PENDING | `bg-yellow-100 text-yellow-800` |
| IN_PROGRESS | `bg-orange-100 text-orange-800` |
| FAILED | `bg-red-100 text-red-800` |

## Right Side (45%)

### RabbitMQ Panel (~20% height)
- Compact live message feed
- Header: "RabbitMQ Live Feed" + connection status dot + message count badge
- Each message is one line: `[EVENT_TYPE] Group #id — timestamp`
- Color-code by event type:
  - GROUP_CREATED: `text-blue-600`
  - ENTRY_ADDED: `text-gray-600`
  - GROUP_CLOSED: `text-purple-600`
- Scrollable, newest at top
- "No messages yet" empty state

### Mock Panel (~80% height)
Split into two sections:

#### Setup Controls (top, compact)
- Two rows (Noark A, Noark B), each with:
  - Label: "Noark A" / "Noark B"
  - Status indicator dot (green for 200, red for 4xx/5xx)
  - Status code `<select>`: 200, 400, 500, 503
  - Delay `<input type="number">` in ms (default 0)
  - "Apply" button (small, inline)
- Current state shown via the status dot + text ("200 OK" / "500 Error")

#### Request History (bottom, scrollable)
- Two sections: "Noark A" and "Noark B" (stacked, not side-by-side — more room)
- Each request: `POST /path — timestamp`
- Clickable to expand and show request body (JSON formatted)
- "No requests yet" empty state per section

## RESET Button Behavior

1. User clicks RESET → `confirm('Reset all services? This clears all data.')`
2. On confirm, fire in parallel:
   - `DELETE /api/groups` → Log Manager (clear MySQL)
   - `DELETE /edge-api/api/groups` → Edge (clear MongoDB)
   - `POST /mock-api/api/test/reset` → Mock (clear history + config)
   - `DELETE /rabbitmq-api/api/queues/%2F/log-events-queue/contents` → RabbitMQ (purge queue)
3. Clear frontend state: groups list, selected group, entries, WebSocket messages
4. Brief green toast "Reset complete" (auto-dismiss after 3s) or red toast on error

## Responsive Notes
- This is a presentation demo — designed for 1080p+ landscape screens
- No mobile breakpoints needed
- Minimum viable viewport: 1280x720
