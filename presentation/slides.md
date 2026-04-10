---
theme: default
title: "Claude & The Mystery of Integrations"
info: |
  How do you test systems that talk to services you don't control?
class: text-center
drawings:
  persist: false
transition: slide-left
---

# Claude & The Mystery of Integrations

Testing third-party integrations you can't control

---
layout: section
---

# Act 1: The Application

---

# What Does Our App Do?

A **log aggregation system** that collects and organizes log entries

<v-clicks>

- Users create **log groups** and add text entries to them
- When a group is complete, the user **closes** it
- The system then needs to **archive** the data to external systems

</v-clicks>

---

# The Architecture (Simplified)

```
  ┌─────────────┐
  │   Frontend   │  Users interact here
  └──────┬──────┘
         │
  ┌──────▼──────┐
  │ Log Manager  │  Stores the log data
  └──────┬──────┘
         │
  ┌──────▼──────┐
  │    Edge      │  Sends data to external archives
  └──────┬──────┘
         │
    ┌────▼────┐
    │ Noark A │   External archive systems
    │ Noark B │   (we don't control these!)
    └─────────┘
```

---
layout: section
---

# Act 2: The Problem

---

# We Need to Archive Data

When a log group is closed, we must send the data to **external archive systems**

These systems are:

<v-clicks>

- Run by **someone else** (the customer)
- On **their infrastructure**
- Following **their rules** (the NOARK standard)
- Sometimes they are **slow**, **broken**, or **unavailable**

</v-clicks>

---

# Keeping Things Separate

We don't want archiving to break our core application

**Solution:** Use a **message queue** (RabbitMQ)

```
  Log Manager ──── message ────► Edge ────► External Systems
     (core)       (queue)      (bridge)
```

<v-clicks>

- Log Manager publishes events: *"group created"*, *"entry added"*, *"group closed"*
- The Edge service picks up events and handles archiving **independently**
- If archiving fails, the core app keeps working fine

</v-clicks>

---

# Why a Message Queue?

Think of it like a **mailbox**

<v-clicks>

- The sender drops off a letter and walks away
- The receiver picks it up when ready
- If the receiver is busy, the letter waits safely in the box
- The sender never needs to wait or worry

</v-clicks>

---
layout: section
---

# Act 3: How Do We Test This?

---

# The Testing Pyramid

```
        /\
       /  \       Few, slow, expensive
      / E2E\
     /──────\
    /  Inte-  \
   / gration   \
  /─────────────\
 /    Unit        \   Many, fast, cheap
/___________________\
```

<v-clicks>

- **Unit tests** -- test small pieces in isolation
- **Integration tests** -- test how pieces work together
- **E2E (End-to-End) tests** -- test the whole system like a real user would

</v-clicks>

---

# The Challenge

How do we test our connection to external systems we don't control?

<v-clicks>

- We can't use the **real systems** in our tests
- We can't make them return **errors on demand**
- We can't test what happens when they are **slow**
- We need something that **behaves like** the real thing

</v-clicks>

---
layout: section
---

# Act 4: The Mock Server

---

# Enter the Mock Server

A **mock server** is a fake version of an external system

It pretends to be the real thing, but **we control it**

<v-clicks>

- Looks like the real Noark systems to our app
- Accepts the same requests
- But we decide what it responds with
- We can make it return **success**, **errors**, or **nothing at all**

</v-clicks>

---

# Our Mock in Action

```
  Real production:         Our test setup:

  App ──► Real Noark       App ──► Mock Server
          (no control)             (full control!)
```

The mock server exposes a **control panel** where we can configure its behavior at any time

---
layout: section
---

# Act 5: Testing with Playwright

---

# What is Playwright?

A tool that **drives a web browser automatically**

<v-clicks>

- Opens the browser just like a real user would
- Clicks buttons, fills in forms, reads text on screen
- Can verify that the right things appear
- Runs the same steps every time -- no human error

</v-clicks>

---

# Playwright in Our Project

Our Playwright tests walk through the full user journey:

<v-clicks>

1. Open the app in a browser
2. Create a new log group
3. Add entries to the group
4. Close the group
5. Verify the data was archived successfully

</v-clicks>

---
layout: section
---

# Act 6: E2E Testing in Java

---

# Why Also Test in Java?

Our backend is written in Java -- so we can test the **full pipeline** without a browser

<v-clicks>

- Starts all the services automatically
- Spins up real databases and a real message queue (using containers)
- Sends API requests directly
- Checks that data flows all the way through to the archive

</v-clicks>

---

# What the Java Test Covers

```
  API Request ──► Log Manager ──► RabbitMQ ──► Edge ──► Adapters ──► Mock
```

<v-clicks>

- Happy path: everything works
- One adapter fails: the other still succeeds
- All adapters fail: system handles it gracefully
- Retry: fails once, succeeds on retry

</v-clicks>

---
layout: section
---

# Act 7: Controlling the Mock from Tests

---

# The Secret Ingredient

We can **change the mock server's behavior from inside our tests**

<v-clicks>

- Before a test: tell the mock to return errors
- Run the test: see how our app handles the failure
- Reset the mock: back to normal
- Next test: try a different scenario

</v-clicks>

---

# How It Works

```
  Test says:  "Mock, return error 500 for Noark A"
                          │
  Mock says:  "OK, I will fail all Noark A requests"
                          │
  Test runs:  Create group ──► Close group ──► Edge tries to archive
                          │
  Result:     Archive fails ──► Edge records the failure
                          │
  Test says:  "Mock, go back to normal"
```

This lets us test **every scenario** -- not just the happy path

---

# What Can We Simulate?

<v-clicks>

- **Success** -- everything works perfectly
- **Server errors** -- the external system crashes (500)
- **Bad requests** -- our data format is wrong (400)
- **Timeouts** -- the external system is too slow
- **Recovery** -- fails first, then succeeds on retry

</v-clicks>

---
layout: center
class: text-center
---

# Summary

We test integrations we can't control by **replacing them with something we can**

Mock servers + automated tests = confidence that our system handles the real world

---
layout: end
---

# Thank you!
