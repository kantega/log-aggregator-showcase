# Log Aggregator Showcase — Full Test Plan

## Application Overview

A microservice-based log management system that archives log data to external NOARK-compliant archiving systems. The Angular frontend has a split layout: left side (55%) contains the Log Manager (group list, entries, group detail) and the Edge/MongoDB panel below it; right side (45%) contains the RabbitMQ Live Feed panel (top 30%) and the External APIs Mock panel (bottom 70%). Data flows: user creates groups and entries via the UI → Log Manager API publishes events to RabbitMQ → Edge consumes events and routes to Adapter A and Adapter B → adapters POST to external-apis-mock (Noark A and Noark B). The mock can be configured at runtime to return specific HTTP status codes and simulated delays. All panels show live state via WebSocket/STOMP connections.

## Test Scenarios

### 1. Log Group CRUD

**Seed:** `tests/seed.spec.ts`

#### 1.1. Create a new log group with a valid name

**File:** `tests/log-group-crud/create-group.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200
    - expect: The page loads with the heading 'Log Manager' visible
    - expect: The group name input field is empty
    - expect: The 'Create' button is disabled
  2. Type 'My First Group' into the group name input (data-testid='group-name-input')
    - expect: The input shows 'My First Group'
    - expect: The 'Create' button becomes enabled
  3. Click the 'Create' button (data-testid='create-group-button')
    - expect: A new group item 'My First Group' appears in the Groups list
    - expect: The group item status badge shows 'OPEN'
    - expect: The group name input is cleared
    - expect: The 'Create' button becomes disabled again
    - expect: A toast notification confirms successful creation
  4. Click on the newly created 'My First Group' item in the list
    - expect: The detail panel on the right shows 'My First Group' as the group name (data-testid='group-detail-name')
    - expect: The detail panel status shows 'OPEN' (data-testid='group-detail-status')
    - expect: The entry content input (data-testid='entry-content-input') is visible
    - expect: The 'Close Group' button (data-testid='close-group-button') is visible
    - expect: No entries are listed yet

#### 1.2. Create button is disabled when group name input is empty

**File:** `tests/log-group-crud/create-group-validation.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200
    - expect: The page loads successfully
  2. Observe the 'Create' button without typing anything in the group name input
    - expect: The 'Create' button is disabled
  3. Type a single space into the group name input
    - expect: The 'Create' button remains disabled or if enabled, clicking it shows a validation error
  4. Clear the input and leave it blank
    - expect: The 'Create' button is disabled

#### 1.3. Add log entries to an open group

**File:** `tests/log-group-crud/add-entries.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and create a new group named 'Entry Test Group' using the group name input and Create button
    - expect: The group 'Entry Test Group' appears in the list with status OPEN
  2. Click on 'Entry Test Group' to select it
    - expect: The detail panel shows the group with OPEN status and an empty entries list
  3. Type 'Alpha entry' into the entry content input (data-testid='entry-content-input') and click 'Add Entry' (data-testid='add-entry-button')
    - expect: 'Alpha entry' appears in the entries list (data-testid='entry-content')
    - expect: The entry content input is cleared
    - expect: There is exactly 1 entry in the list
  4. Type 'Beta entry' into the entry content input and click 'Add Entry'
    - expect: 'Beta entry' appears in the entries list
    - expect: There are now 2 entries total
  5. Type 'Gamma entry' into the entry content input and click 'Add Entry'
    - expect: 'Gamma entry' appears in the entries list
    - expect: There are now 3 entries total (data-testid='entry-item' count is 3)

#### 1.4. Add entry button is disabled when entry content is empty

**File:** `tests/log-group-crud/add-entry-validation.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200, create a group named 'Validation Group', and click on it to select it
    - expect: The group is selected and shows OPEN status
  2. Leave the entry content input (data-testid='entry-content-input') empty and observe the 'Add Entry' button
    - expect: The 'Add Entry' button (data-testid='add-entry-button') is disabled
  3. Type a single space into the entry content input
    - expect: The 'Add Entry' button remains disabled or shows validation error when clicked

#### 1.5. Close a log group and verify it becomes read-only

**File:** `tests/log-group-crud/close-group.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200, create a group named 'Close Test Group', click on it, and add one entry 'Test entry'
    - expect: The group shows OPEN and has one entry
  2. Click the 'Close Group' button (data-testid='close-group-button')
    - expect: The group detail status (data-testid='group-detail-status') changes to 'CLOSED'
    - expect: The group list item status badge (data-testid='group-item-status') changes to 'CLOSED'
    - expect: The entry content input (data-testid='entry-content-input') is no longer visible
    - expect: The 'Add Entry' button (data-testid='add-entry-button') is no longer visible
    - expect: The 'Close Group' button (data-testid='close-group-button') is no longer visible
  3. Verify the existing entry 'Test entry' is still visible in the detail panel
    - expect: 'Test entry' is visible in the entries list
    - expect: The entry is read-only (no edit controls)

#### 1.6. Browse all groups and switch between them

**File:** `tests/log-group-crud/browse-groups.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200, then reset all state using the RESET button (data-testid='reset-button') and confirm if prompted
    - expect: The groups list is empty after reset
  2. Create two groups: 'Group One' and 'Group Two'
    - expect: Both groups appear in the list in reverse chronological order (most recent first)
  3. Click on 'Group One' in the list
    - expect: The detail panel shows 'Group One' with OPEN status
    - expect: 'Group One' appears highlighted/active in the list
  4. Click on 'Group Two' in the list
    - expect: The detail panel updates to show 'Group Two' with OPEN status
    - expect: 'Group Two' appears highlighted/active in the list
    - expect: 'Group One' is no longer highlighted

### 2. Happy Path E2E

**Seed:** `tests/seed.spec.ts`

#### 2.1. Full archive flow: create group, add entries, close group, verify archiving in Noark A and B

**File:** `tests/happy-path/full-archive-flow.spec.ts`

**Steps:**
  1. Reset all state via the RESET button, then configure the mock via POST /api/test/reset (or use the Mock Panel) to ensure both Noark A and Noark B return 200 OK
    - expect: Noark A status shows '200 OK' in the Mock Setup panel
    - expect: Noark B status shows '200 OK' in the Mock Setup panel
  2. Wait for the RabbitMQ status indicator (data-testid='rabbitmq-status') to show 'Connected'
    - expect: The status badge has aria-label 'Connected'
  3. Create a new group named 'Happy Path Group'
    - expect: The group appears in the list with status OPEN
    - expect: A new message appears in the RabbitMQ Live Feed (GROUP_CREATED event)
    - expect: The message count badge increments by 1
  4. Click on 'Happy Path Group' and add two entries: 'First log line' and 'Second log line'
    - expect: Both entries appear in the detail panel
    - expect: Each entry addition generates a message in the RabbitMQ Live Feed (ENTRY_ADDED events)
    - expect: The message count badge increments by 2 more
    - expect: After each entry, a new archive request to /api/noarka/archive appears in the Noark A section of the Mock panel
  5. Click the 'Close Group' button
    - expect: The group status changes to CLOSED in both the list and detail panel
    - expect: A GROUP_CLOSED event appears in the RabbitMQ Live Feed
    - expect: The message count badge increments by 1 more
    - expect: A new archive request to /api/noarka/archive appears in the Noark A section
    - expect: A new archive request to /api/noarkb/archive appears in the Noark B section
  6. Click on 'Happy Path Group' in the Edge panel (data-testid starts with 'edge-group-')
    - expect: The Edge panel detail shows archive status ARCHIVED (data-testid='edge-group-status')
    - expect: The archive events list shows 4 events: ENTRY_ADDED for adapter-a, ENTRY_ADDED for adapter-b (skipped by B but tracked), GROUP_CLOSED for adapter-a, GROUP_CLOSED for adapter-b

#### 2.2. Verify RabbitMQ message count increments correctly for all mutation operations

**File:** `tests/happy-path/rabbitmq-message-count.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and record the initial message count from data-testid='rabbitmq-message-count'
    - expect: A numeric value is visible in the message count badge
  2. Create a new group named 'MQ Count Group'
    - expect: The message count increments by 1 (GROUP_CREATED event)
  3. Select the group and add one entry 'Entry One'
    - expect: The message count increments by 1 more (ENTRY_ADDED event)
  4. Add a second entry 'Entry Two'
    - expect: The message count increments by 1 more
  5. Click 'Close Group'
    - expect: The message count increments by 1 more (GROUP_CLOSED event)
    - expect: Total increment from initial count is 4 (1 create + 2 entries + 1 close)

### 3. RabbitMQ Integration

**Seed:** `tests/seed.spec.ts`

#### 3.1. RabbitMQ panel shows Connected status on page load

**File:** `tests/rabbitmq/connection-status.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200
    - expect: The RabbitMQ Live Feed panel is visible on the right side of the screen
  2. Wait up to 15 seconds for the RabbitMQ status indicator (data-testid='rabbitmq-status') to change
    - expect: The status indicator has aria-label 'Connected' within 15 seconds

#### 3.2. RabbitMQ live feed shows message details on mutation

**File:** `tests/rabbitmq/live-feed-messages.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200, wait for the RabbitMQ status to show 'Connected', and note the initial feed state
    - expect: The feed shows 'No messages yet' if no messages exist, or existing messages
  2. Create a new group named 'Feed Test Group'
    - expect: A new message entry appears in the RabbitMQ Live Feed (data-testid='rabbitmq-feed')
    - expect: The message references the group name or a GROUP_CREATED event type
  3. Select the group and add an entry 'Feed entry one'
    - expect: Another message appears in the feed for ENTRY_ADDED
    - expect: The message count (data-testid='rabbitmq-message-count') shows the updated total
  4. Close the group
    - expect: Another message appears in the feed for GROUP_CLOSED

### 4. Edge Archive Tracking

**Seed:** `tests/seed.spec.ts`

#### 4.1. Edge panel shows archive group card after group creation

**File:** `tests/edge/archive-group-appears.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and wait for the Edge panel WebSocket to connect (data-testid='edge-status' shows 'Connected')
    - expect: The Edge / MongoDB panel is visible with a 'Connected' status indicator
  2. Create a new group named 'Edge Track Group'
    - expect: Within a few seconds, a new card appears in the Edge / MongoDB panel for 'Edge Track Group'
    - expect: The card shows an archive status badge (data-testid='edge-group-status')
  3. Click on the Edge group card for 'Edge Track Group'
    - expect: The detail view expands or a detail panel appears
    - expect: Archive events are listed including the ENTRY_ADDED and GROUP_CLOSED events with their adapters and timestamps

#### 4.2. Edge panel shows ARCHIVED status after successful archiving

**File:** `tests/edge/archive-status-archived.spec.ts`

**Steps:**
  1. Ensure the mock is configured with both Noark A and Noark B returning 200 OK using the Mock Setup controls
    - expect: Noark A status badge shows 200 OK
    - expect: Noark B status badge shows 200 OK
  2. Create a group named 'Status Verify Group', add one entry 'Status entry', and close the group
    - expect: The group is CLOSED in the Log Manager panel
  3. Wait up to 10 seconds for the Edge panel card for 'Status Verify Group' to update
    - expect: The archive status badge (data-testid='edge-group-status') on the Edge card shows 'ARCHIVED'

#### 4.3. Edge panel shows PENDING status briefly before archiving completes

**File:** `tests/edge/archive-status-pending.spec.ts`

**Steps:**
  1. Configure Noark A to return 200 with a 3-second delay using the Mock Setup controls, then click Apply
    - expect: The Noark A status indicator updates to show 200 with delay applied
  2. Create a group named 'Pending Status Group' and add an entry 'Pending entry'
    - expect: A card for 'Pending Status Group' appears in the Edge panel
  3. Observe the Edge group card status immediately after the entry is added
    - expect: The archive status badge shows 'PENDING' or 'IN_PROGRESS' before the delayed archiving completes
  4. Wait for the delay to pass and the archiving to complete
    - expect: The archive status badge transitions to 'ARCHIVED'

### 5. Error Handling — Noark A Failures

**Seed:** `tests/seed.spec.ts`

#### 5.1. Configure Noark A to return 500 and verify failure is tracked in Edge

**File:** `tests/error-handling/noarka-500-failure.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and click the RESET button to clear all state
    - expect: All panels are cleared
  2. In the Mock Setup panel, select '500' from the Noark A status code dropdown (data-testid='mock-setup-noarka-status') and click Apply (data-testid='mock-setup-noarka-apply')
    - expect: The Noark A status indicator shows '500 Error'
  3. Create a group named 'Failure Test A Group', click on it, and add one entry 'Failure entry'
    - expect: The group is created and the entry is added in the Log Manager panel
  4. Wait up to 15 seconds for the Edge panel to update
    - expect: The Edge card for 'Failure Test A Group' shows a status of 'FAILED' (data-testid='edge-group-status')
  5. Click on the Edge card for 'Failure Test A Group'
    - expect: The archive events detail shows a failed event for adapter-a
    - expect: Error context is recorded (event type, adapter name, timestamp)

#### 5.2. Configure Noark A to return 400 and verify failure tracking

**File:** `tests/error-handling/noarka-400-failure.spec.ts`

**Steps:**
  1. Click the RESET button, then configure Noark A to return 400 via the Mock Setup panel and click Apply
    - expect: The Noark A status indicator shows '400' or 'Bad Request'
  2. Create a group named 'Bad Request A Group' and add one entry 'Bad request entry'
    - expect: The group and entry are created in the Log Manager
  3. Wait for the Edge panel to reflect the result
    - expect: The Edge card for 'Bad Request A Group' shows 'FAILED' status

#### 5.3. Configure Noark A to return 503 and verify failure tracking

**File:** `tests/error-handling/noarka-503-failure.spec.ts`

**Steps:**
  1. Click the RESET button, then configure Noark A to return 503 via the Mock Setup panel and click Apply
    - expect: The Noark A status indicator shows '503' or 'Service Unavailable'
  2. Create a group named 'Unavailable A Group' and add one entry 'Service unavailable entry'
    - expect: The group and entry are created
  3. Wait for the Edge panel to reflect the result
    - expect: The Edge card for 'Unavailable A Group' shows 'FAILED' status

### 6. Error Handling — Noark B Failures

**Seed:** `tests/seed.spec.ts`

#### 6.1. Configure Noark B to return 500 and verify failure is tracked in Edge on GROUP_CLOSED

**File:** `tests/error-handling/noarkb-500-failure.spec.ts`

**Steps:**
  1. Click the RESET button, then in the Mock Setup panel configure Noark A to return 200 (data-testid='mock-setup-noarka-status') and click Apply; then configure Noark B to return 500 (data-testid='mock-setup-noarkb-status') and click Apply
    - expect: Noark A status shows 200 OK
    - expect: Noark B status shows 500 Error
  2. Create a group named 'Failure Test B Group', add one entry 'Entry for B', and then close the group
    - expect: The group is CLOSED in the Log Manager panel
  3. Wait up to 15 seconds for the Edge panel to update
    - expect: The Edge card for 'Failure Test B Group' shows 'FAILED' status (data-testid='edge-group-status')
  4. Click on the Edge card for 'Failure Test B Group' to see details
    - expect: The archive events list shows GROUP_CLOSED for adapter-b with a failed/error status
    - expect: The adapter-a event should show success (200 OK)

#### 6.2. Noark B skips ENTRY_ADDED events and only archives on GROUP_CLOSED

**File:** `tests/error-handling/noarkb-entry-added-skipped.spec.ts`

**Steps:**
  1. Click the RESET button, then configure both Noark A and Noark B to return 200 OK via the Mock Setup panel
    - expect: Both adapters return 200 OK
  2. Create a group named 'B Skip Test Group' and add two entries: 'First skip entry' and 'Second skip entry'
    - expect: Both entries appear in the Log Manager detail panel
  3. Observe the External APIs Mock panel — count requests to /api/noarka/archive and /api/noarkb/archive
    - expect: Noark A (/api/noarka/archive) receives 2 POST requests (one per ENTRY_ADDED)
    - expect: Noark B (/api/noarkb/archive) receives 0 POST requests at this point (Adapter B skips ENTRY_ADDED)
  4. Close the group and observe the mock panel again
    - expect: Noark A receives 1 more POST request (GROUP_CLOSED) — total 3
    - expect: Noark B receives exactly 1 POST request (GROUP_CLOSED only)

### 7. Retry Mechanism

**Seed:** `tests/seed.spec.ts`

#### 7.1. Edge retries a failed archive up to max retries then marks as FAILED

**File:** `tests/retry/max-retries-exceeded.spec.ts`

**Steps:**
  1. Click the RESET button to clear all state
    - expect: All panels are empty
  2. Configure Noark A to return 500 via the Mock Setup panel and click Apply
    - expect: Noark A status shows 500 Error
  3. Create a group named 'Retry Limit Group' and add one entry 'Retry entry'
    - expect: The group and entry are created
  4. Wait for the Edge to process the event and retry (up to 3 retries with delays between them — allow up to 60 seconds total)
    - expect: The Edge card for 'Retry Limit Group' eventually shows 'FAILED' status after exhausting retries
    - expect: Multiple archive request entries for /api/noarka/archive appear in the Noark A mock panel (initial attempt + up to 3 retries = up to 4 total attempts)

#### 7.2. Archive recovers after mock is restored to 200 OK and retry button is clicked

**File:** `tests/retry/retry-recovery.spec.ts`

**Steps:**
  1. Click the RESET button, configure Noark A to return 500 and click Apply
    - expect: Noark A status shows 500 Error
  2. Create a group named 'Recovery Group' and add one entry 'Recovery entry'
    - expect: A card for 'Recovery Group' appears in the Edge panel with FAILED status after a short wait
  3. In the Mock Setup panel, change Noark A back to 200 and click Apply
    - expect: Noark A status shows 200 OK
  4. Click the retry button (data-testid='edge-retry-button') on the 'Recovery Group' Edge card
    - expect: The Edge card status changes from FAILED to ARCHIVED after the retry succeeds
    - expect: A new POST request to /api/noarka/archive appears in the mock panel

### 8. Mock Panel Controls

**Seed:** `tests/seed.spec.ts`

#### 8.1. Change Noark A status code and verify the status indicator updates

**File:** `tests/mock-panel/noarka-status-change.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and locate the Mock Setup section in the External APIs Mock panel
    - expect: The Mock Setup controls are visible (data-testid='mock-setup-controls')
    - expect: Noark A has a status code dropdown and a delay input
  2. Select '200' from the Noark A status code dropdown (data-testid='mock-setup-noarka-status') and click Apply
    - expect: The Noark A status indicator updates to show '200 OK'
  3. Select '400' from the Noark A status code dropdown and click Apply
    - expect: The Noark A status indicator updates to show '400' or 'Bad Request'
  4. Select '500' from the Noark A status code dropdown and click Apply
    - expect: The Noark A status indicator updates to show '500 Error'
  5. Select '503' from the Noark A status code dropdown and click Apply
    - expect: The Noark A status indicator updates to show '503' or 'Service Unavailable'

#### 8.2. Change Noark B status code and verify the status indicator updates

**File:** `tests/mock-panel/noarkb-status-change.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and locate the Noark B controls in the Mock Setup section
    - expect: The Noark B status code dropdown (data-testid='mock-setup-noarkb-status') and delay input are visible
  2. Select '500' from the Noark B status code dropdown and click the Noark B Apply button (data-testid='mock-setup-noarkb-apply')
    - expect: The Noark B status indicator updates to show '500 Error'
  3. Select '200' from the Noark B status code dropdown and click Apply
    - expect: The Noark B status indicator updates to show '200 OK'

#### 8.3. Configure Noark A delay and verify it takes effect

**File:** `tests/mock-panel/noarka-delay-config.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and set the Noark A delay input (data-testid='mock-setup-noarka-delay') to '3' seconds and click Apply
    - expect: The delay is applied to the mock configuration
  2. Create a group named 'Delay Test Group' and add an entry 'Delay entry'
    - expect: The group and entry are created in the Log Manager
  3. Observe that the Edge card for 'Delay Test Group' does not immediately show ARCHIVED — it should remain in PENDING or IN_PROGRESS for approximately 3 seconds
    - expect: The Edge group status is not ARCHIVED immediately after the event
    - expect: After approximately 3 seconds, the status transitions to ARCHIVED

#### 8.4. Mock panel shows request history for each adapter

**File:** `tests/mock-panel/request-history.spec.ts`

**Steps:**
  1. Click the RESET button to clear all state including mock history, then ensure both Noark A and B are set to 200 OK
    - expect: The Noark A and Noark B request sections in the mock panel show 0 requests (or empty list)
  2. Create a group named 'History Group', add an entry 'History entry', and close the group
    - expect: The Noark A section shows requests for each ENTRY_ADDED and GROUP_CLOSED event
    - expect: The Noark B section shows a request only for GROUP_CLOSED
    - expect: Each request entry shows the method (POST), path (/api/noarka/archive or /api/noarkb/archive), and timestamp
  3. Click on one of the Noark A request entries
    - expect: The request detail expands to show the JSON payload
    - expect: The payload includes title, description, archiveDate, and documents fields

### 9. Adapter Behavior Differences

**Seed:** `tests/seed.spec.ts`

#### 9.1. Adapter A archives on every ENTRY_ADDED event, Adapter B only on GROUP_CLOSED

**File:** `tests/adapter-behavior/adapter-event-routing.spec.ts`

**Steps:**
  1. Click the RESET button, configure both adapters to return 200 OK, then create a group named 'Routing Test Group'
    - expect: The group is created with OPEN status
  2. Add three entries: 'Route entry one', 'Route entry two', 'Route entry three' to 'Routing Test Group'
    - expect: All three entries appear in the Log Manager detail panel
  3. After all entries are added, observe the request counts in the mock panel
    - expect: The Noark A section shows 3 POST requests to /api/noarka/archive (one per ENTRY_ADDED)
    - expect: The Noark B section shows 0 POST requests to /api/noarkb/archive
  4. Close the group 'Routing Test Group'
    - expect: The Noark A section shows 4 total POST requests (3 ENTRY_ADDED + 1 GROUP_CLOSED)
    - expect: The Noark B section shows exactly 1 POST request to /api/noarkb/archive (GROUP_CLOSED only)

### 10. Reset Functionality

**Seed:** `tests/seed.spec.ts`

#### 10.1. RESET button clears all groups, entries, Edge data, and mock history

**File:** `tests/reset/global-reset.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200, create two groups ('Reset Group One' and 'Reset Group Two'), add entries to each, and close one of them
    - expect: Both groups are visible in the list
    - expect: Edge panel shows cards for both groups
    - expect: Mock panel shows several request entries
  2. Click the RESET button (data-testid='reset-button')
    - expect: A confirmation dialog appears (if applicable) — accept it
  3. Observe all panels after the reset completes
    - expect: The Groups list is empty
    - expect: The Edge / MongoDB panel shows no group cards
    - expect: The Noark A and Noark B request sections in the mock panel are empty or show 0 requests
    - expect: The RabbitMQ Live Feed shows no new messages (or is cleared)
    - expect: A toast notification may confirm the reset

#### 10.2. Application is functional after a reset — can create new groups immediately

**File:** `tests/reset/post-reset-functionality.spec.ts`

**Steps:**
  1. Click the RESET button and wait for the reset to complete
    - expect: The Groups list is empty
  2. Create a new group named 'Post Reset Group'
    - expect: The group appears in the list with status OPEN and a new ID
  3. Add an entry 'Post reset entry' and close the group
    - expect: The full workflow completes successfully
    - expect: The Edge panel shows the new group card with ARCHIVED status (with mock set to 200 OK)

### 11. Docs Page

**Seed:** `tests/seed.spec.ts`

#### 11.1. Navigate to Docs page via the Docs button and verify structure

**File:** `tests/docs/navigate-to-docs.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and click the 'Docs' link (data-testid='docs-button')
    - expect: The URL changes to http://localhost:4200/docs
    - expect: The page heading 'API Documentation' is visible
    - expect: A 'Back to App' link is visible
  2. Observe the tabs in the navigation
    - expect: Five service tabs are visible: 'Log Manager :8080', 'Edge :8081', 'Adapter A :8082', 'Adapter B :8083', 'Mock :8084'
    - expect: The 'Log Manager :8080' tab is selected by default
  3. Observe the iframe content for the Log Manager tab
    - expect: A Swagger UI iframe is visible showing the Log Manager API endpoints
    - expect: Endpoints including GET /api/groups, POST /api/groups, DELETE /api/groups, and group entry endpoints are listed

#### 11.2. Switch between service tabs on the Docs page

**File:** `tests/docs/service-tabs.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200/docs
    - expect: The docs page loads with the Log Manager tab active
  2. Click the 'Edge :8081' tab (data-testid='tab-Edge')
    - expect: The iframe updates to show the Edge service API documentation at port 8081
    - expect: The Edge tab becomes active/selected
  3. Click the 'Adapter A :8082' tab (data-testid='tab-Adapter A')
    - expect: The iframe updates to show the Adapter A API documentation at port 8082
  4. Click the 'Adapter B :8083' tab (data-testid='tab-Adapter B')
    - expect: The iframe updates to show the Adapter B API documentation at port 8083
  5. Click the 'Mock :8084' tab (data-testid='tab-Mock')
    - expect: The iframe updates to show the External APIs Mock API documentation at port 8084

#### 11.3. Back to App link navigates back from Docs page

**File:** `tests/docs/back-button.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200/docs
    - expect: The docs page loads
  2. Click the 'Back to App' link (data-testid='back-button')
    - expect: The URL changes back to http://localhost:4200/
    - expect: The Log Manager heading is visible
    - expect: The main app layout is displayed correctly

#### 11.4. Docs page is accessible directly via URL

**File:** `tests/docs/direct-url-access.spec.ts`

**Steps:**
  1. Navigate directly to http://localhost:4200/docs without going through the main page first
    - expect: The docs page loads with the 'API Documentation' heading
    - expect: All five service tabs are present and functional
    - expect: The Log Manager Swagger UI is loaded in the iframe

### 12. Form Validation

**Seed:** `tests/seed.spec.ts`

#### 12.1. Create button remains disabled until group name is non-empty

**File:** `tests/form-validation/group-name-required.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and observe the group name input (data-testid='group-name-input') and Create button (data-testid='create-group-button')
    - expect: The group name input is empty
    - expect: The Create button is disabled
  2. Type 'A' into the group name input
    - expect: The Create button becomes enabled
  3. Clear the input using backspace or the clear operation
    - expect: The Create button becomes disabled again
  4. Type a longer name 'Valid Group Name 123' and verify the button is enabled
    - expect: The Create button is enabled and clickable

#### 12.2. Add Entry button remains disabled until entry content is non-empty

**File:** `tests/form-validation/entry-content-required.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200, create a group named 'Validation Group', and click on it to select it
    - expect: The group is selected and shows OPEN status
    - expect: The entry content input (data-testid='entry-content-input') is visible
    - expect: The Add Entry button (data-testid='add-entry-button') is disabled
  2. Type 'Some content' into the entry content input
    - expect: The Add Entry button becomes enabled
  3. Clear the entry content input
    - expect: The Add Entry button becomes disabled again

#### 12.3. Group name persists in input if creation fails

**File:** `tests/form-validation/group-name-error-state.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200 and type 'Error Test Group' into the group name input
    - expect: The input shows 'Error Test Group'
    - expect: The Create button is enabled
  2. If a network error or service error occurs during group creation, observe the UI state
    - expect: The group name input retains its value or is restored so the user can retry
    - expect: An error toast or notification is displayed
    - expect: The Create button becomes available again

#### 12.4. Cannot add entries to a closed group

**File:** `tests/form-validation/closed-group-readonly.spec.ts`

**Steps:**
  1. Navigate to http://localhost:4200, create a group named 'Read Only Group', add one entry 'Only entry', and close the group
    - expect: The group shows CLOSED status
  2. Click on the closed group in the list to view its detail
    - expect: The group detail shows the existing entry 'Only entry'
    - expect: No entry content input (data-testid='entry-content-input') is visible
    - expect: No 'Add Entry' button (data-testid='add-entry-button') is visible
    - expect: No 'Close Group' button (data-testid='close-group-button') is visible
