---
theme: default
title: "Claude & The Mystery of Integrations"
info: |
  How Claude Code helps us build and test integrations we can't control
class: text-center
drawings:
  persist: false
transition: slide-left
---

# Claude & The Mystery of Integrations

How we used AI to help us build and test systems we can't control

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
- Simple and self-contained

</v-clicks>

---

# The Architecture

```
  ┌─────────────┐
  │   Frontend   │  Angular web app
  └──────┬──────┘
         │ HTTP
  ┌──────▼──────┐
  │ Log Manager  │  Spring Boot + MySQL
  └─────────────┘
```

That's it. A frontend and a backend. Users manage their log groups here.

---
layout: section
---

# Act 2: The New Requirement

---

# "We Need to Archive This Data"

As a SaaS platform, our customers need their closed log groups archived -- but **each customer uses their own archive provider**

<v-clicks>

- Customer A uses **Noark A**, Customer B uses **Noark B**
- Each provider is a SaaS solution on **their own infrastructure**
- They follow **their own rules** (the NOARK standard)
- Sometimes they are **slow**, **broken**, or **unavailable**

</v-clicks>

---

# Keeping It Separate

We don't want archiving to break our core application

**Solution:** A separate **Edge service** connected via a message queue

```
  ┌─────────────┐
  │   Frontend   │
  └──────┬──────┘
  ┌──────▼──────┐
  │ Log Manager  │ ──── events ────► RabbitMQ
  └─────────────┘                      │
                                ┌──────▼──────┐
                                │    Edge      │
                                └──────┬──────┘
                                  ┌────▼────┐
                                  │ Noark A │
                                  │ Noark B │
                                  └─────────┘
```

<v-clicks>

- Log Manager publishes events: *"group created"*, *"entry added"*, *"group closed"*
- Edge picks them up and handles archiving **independently**
- If archiving fails, the core app keeps working fine

</v-clicks>

---

# That's a Lot of Moving Parts

Two adapters, a mock server, a message queue, two databases, six microservices

<v-clicks>

- How do we **test** all of this?
- How do we test what happens when a provider is **slow**, **broken**, or **wrong**?
- How do we make sure we don't break Noark A when we fix something for Noark B?

</v-clicks>

---

# The Testing Pyramid

```
        /\
       /  \       ▲ More useful information
      / E2E\      │ "Does the whole system actually work?"
     /──────\     │
    /  Inte-  \   │
   / gration   \  │
  /─────────────\ │
 /    Unit        \│ ▼ More speed
/___________________\ "Is this piece correct?"
```

<v-clicks>

- **Unit tests** -- fast, test small pieces in isolation
- **Integration tests** -- test how pieces work together
- **E2E (End-to-End) tests** -- slow, but tell us the most about our system

</v-clicks>

---

# The Problem with Real Providers

Can't we just test against the real Noark systems?

<v-clicks>

- Most providers **don't offer a test environment** at all
- When they do, it's one that **works** -- returns success every time
- But the most important thing to test is **what happens when things go wrong**
- Timeouts, server errors, bad responses, downtime -- none of that on demand

</v-clicks>

---
layout: section
---

# Act 3: The Mock Server

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
layout: section
---

# Act 4: This Is a Lot of Work

---

# Building a Mock Server Is Not Free

Each archive provider needs its own set of mock endpoints

<v-clicks>

- A new **RestController** for every provider
- Endpoints for each **failure scenario** -- 400, 500, 503, timeouts, ...
- Very similar work each time, but **not identical** across providers
- Repetitive, detailed, error-prone -- and it has to be maintained

</v-clicks>

---

# Perfect Work for a Coding Agent

These tasks are:

<v-clicks>

- **Repetitive** -- follow the same patterns every time
- **Well-defined** -- given an API spec, the structure is predictable
- **Similar but not equal** -- each provider has its own quirks
- Exactly the kind of work where an LLM with the right context excels

</v-clicks>

---

# Enter: Claude Code Skills

A **skill** is a focused instruction file that teaches Claude how to do a specific task

```
  .claude/skills/mock-provider/SKILL.md
```

<v-clicks>

- Written in plain language, lives in the project
- Describes patterns, helpers, and conventions
- Claude reads it **when the task matches** -- like a specialist joining the team
- Developers invoke it with a command: `/mock-provider`

</v-clicks>

---

# Skill 1: The Mock Provider Skill

This skill understands the mock server's structure

Given a new provider's **OpenAPI YAML file**, Claude can:

<v-clicks>

- Generate the RestController following existing patterns
- Add all the necessary failure scenario endpoints
- Wire it into the mock server's control API
- One command, one new provider -- fully mocked

</v-clicks>

---

# But Mocking Is Only Half the Problem

We also need to **test through** the mock -- and control it from our tests

---

# Controlling Mocks from Tests

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

# Skill 2: The Testing Skill

Our `testing-guide` skill teaches Claude how to write tests at **three levels**:

<v-clicks>

- **Unit tests** -- per-service, using Mockito mocks
- **Java integration test** -- full pipeline with TestContainers
- **Playwright E2E** -- browser-based tests against the live stack

</v-clicks>

---

# The Skill Knows the Details

Things a developer would need to look up -- the skill already knows:

<v-clicks>

- Which helper methods exist (`createGroup`, `configureMock`, `triggerEdgeRetry`)
- That the retry scheduler is **disabled** in Java tests but **enabled** in Playwright
- How to import from `base-test.ts` instead of raw Playwright
- The mock API endpoints and their parameters
- The exact assertion patterns that work for async pipelines

</v-clicks>

---

# A Set of Skills, Working Together

```
  mock-provider skill   →  Knows how to add mock support for a new provider
  testing-guide skill   →  Knows how to write tests at all three levels
  CLAUDE.md             →  Knows the architecture, services, and data flow
```

<v-clicks>

Together, they give Claude the full picture: **providers**, **mocks**, and **tests**

</v-clicks>

---
layout: section
---

# Act 5: Demo

---

# Demo

Live: adding a new provider and its test coverage

<v-clicks>

- Give Claude the new provider's OpenAPI spec
- The mock-provider skill generates the mock endpoints
- The testing skill generates tests at all three levels
- Each skill follows the project's established patterns
- Nothing is forgotten -- mocks, unit tests, integration, *and* E2E

</v-clicks>

---
layout: section
---

# Act 6: Why This Matters

---

# Skills as Team Knowledge

Skills capture what a **senior developer would explain** to a new team member

<v-clicks>

- "Don't forget to test at all three levels"
- "The retry scheduler behaves differently in each test environment"
- "Use these specific helpers, not raw HTTP calls"
- "Assert on final state, not intermediate mock history"

</v-clicks>

---

# What We Gain

<v-clicks>

- **Consistency** -- every new feature gets the same thorough test coverage
- **Speed** -- Claude generates the boilerplate, we focus on the interesting parts
- **Knowledge sharing** -- the skill file is versioned, reviewed, and improved over time
- **Onboarding** -- new developers (and AI) can be productive immediately

</v-clicks>

---
layout: center
class: text-center
---

# Summary

We teach Claude about our system through **CLAUDE.md** and **skills**

It uses that knowledge to generate adapters, mocks, and tests that follow our patterns

The result: consistent, thorough test coverage -- even for systems we can't control

---
layout: end
---

# Thank you!
