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

How we used AI to help us build and test integrations with systems we can't control

<!--
We built a system that integrates with external archive providers we don't control. This talk is about how we used Claude Code to make that testable.
-->

---

<div style="display: flex; justify-content: center; align-items: center; gap: 4rem;">
  <div style="text-align: center;">
    <img src="/pawel.wesolowski.jpeg" style="width: 200px; height: 200px; border-radius: 50%; object-fit: cover;" />
    <h3>Pawel Wesolowski</h3>
  </div>
  <div style="text-align: center;">
    <img src="/martin.larsson.jpeg" style="width: 200px; height: 200px; border-radius: 50%; object-fit: cover;" />
    <h3>Martin Larsson</h3>
  </div>
</div>

---
layout: section
---

# Act 1: The Application

---

# Log Aggregation System

<v-clicks>

- Create log groups
- Close when done
- Simple self-contained

</v-clicks>

<!--
A log aggregation system that collects and organizes log entries. Users create log groups and add text entries to them. When a group is complete, the user closes it. Simple and self-contained — nothing fancy yet.
-->

---

# Frontend Plus Backend

```
  ┌─────────────┐
  │   Frontend   │  Angular web app
  └──────┬──────┘
         │ HTTP
  ┌──────▼──────┐
  │ Log Manager  │  Spring Boot + MySQL
  └─────────────┘
```

<!--
That's it. A frontend and a backend. Users manage their log groups here. Angular web app talking to a Spring Boot backend with MySQL.
-->

---
layout: section
---

# Act 2: The New Requirement

---

# Customers Need Archiving

<v-clicks>

- Customers use different providers
- Core Team should not be bothered
- Architectural choices

</v-clicks>

<!--
As a SaaS platform, our customers need their closed log groups archived — but each customer uses their own archive provider. Customer A uses Noark A, Customer B uses Noark B. Each provider is a SaaS solution on their own infrastructure. They follow their own rules (the NOARK standard). Sometimes they are slow, broken, or unavailable. The core team shouldn't have to worry about archiving breaking the main app.
-->

---

# Decouple via Edge

```
  ┌─────────────┐
  │   Frontend  │
  └──────┬──────┘
  ┌──────▼──────┐
  │ Log Manager │ ──── events ────► RabbitMQ
  └─────────────┘                      │
                                ┌──────▼──────┐
                                │    Edge     │
                                └──────┬──────┘
                                  ┌────▼────┐
                                  │ Noark A │
                                  │ Noark B │
                                  └─────────┘
```

<v-clicks>

- Publishes events
- Archives independently
- Core keeps working

</v-clicks>

<!--
We don't want archiving to break our core application. Solution: a separate Edge service connected via a message queue. Log Manager publishes events: "group created", "entry added", "group closed". Edge picks them up and handles archiving independently. If archiving fails, the core app keeps working fine.
-->

---

# Many Moving Parts

<v-clicks>

- How do we test this?
- How do we test error handling?
- How can we do regressions?

</v-clicks>

<!--
Two adapters, a mock server, a message queue, two databases, six microservices. How do we test all of this? How do we test what happens when a provider is slow, broken, or wrong? How do we make sure we don't break Noark A when we fix something for Noark B?
-->

---

# The Testing Diamond

```
        /\
       /  \           ▲ More useful information
      / E2E\          │ "Does the whole system actually work?"
     /──────\         │
    /  Inte- \        │
   /  gration \       │ ◆ Sweet spot?
   \          /       |
    \ ────── /        │
     \ Unit /         │
      \    /          │ "Is this piece correct?"
       \  /           ▼ More speed
        \/
```

<!--
Traditional testing pyramid says lots of unit tests, fewer integration tests, even fewer E2E. But with this kind of system, integration tests are where we get the most value — they tell us the pieces actually work together. Unit tests are fast but miss the interactions. E2E tests catch everything but are slow. The diamond shape reflects where we invest most.
-->

---

# Real World Providers

<v-clicks>

- Often no test env
- Always succeeds
- Errors matter

</v-clicks>

<!--
Can't we just test against the real Noark systems? Most providers don't offer a test environment at all. When they do, it's one that works — returns success every time. But the most important thing to test is what happens when things go wrong. Timeouts, server errors, bad responses, downtime — none of that on demand.
-->

---
layout: section
---

# Act 3: The Mock Server

---

# Fake Controllable Server

<v-clicks>

- Looks real
- Same requests
- We control the response
- Any response
- Including timeouts and invalid certs

</v-clicks>

<!--
A mock server is a fake version of an external system. It pretends to be the real thing, but we control it. Looks like the real Noark systems to our app. Accepts the same requests. But we decide what it responds with. We can make it return success, errors, or nothing at all — including timeouts and invalid certificates.
-->

---
layout: section
---

# Extended UI of our system

<v-clicks>

- System much larger than our app
- UI larger than our app
- Testing requirements larger than our app

</v-clicks>

<!--
Once you add mock control, the system's UI surface grows well beyond the core app. The mock server needs configuration controls. The Edge service needs monitoring of archive state. Testing requirements scale with system complexity, not just app complexity.
-->

---
layout: section
---

# Act 4: This Is a Lot of Work

---

# Mocking Costs Effort

<v-clicks>

- One controller per provider
- Different endpoints for each failure scenario
- Providers are similar, not identical
- Failure scenarios are similar, but not identical
- Maintenance

</v-clicks>

<!--
Each archive provider needs its own set of mock endpoints. A new RestController for every provider. Endpoints for each failure scenario — 400, 500, 503, timeouts. Very similar work each time, but not identical across providers. Repetitive, detailed, error-prone — and it has to be maintained.
-->

---

# Agents

<v-clicks>

- Similar, but different patterns
- Well defined APIs
- Sound like a job for an LLM

</v-clicks>

<!--
These tasks are repetitive — follow the same patterns every time. Well-defined — given an API spec, the structure is predictable. Similar but not equal — each provider has its own quirks. Exactly the kind of work where an LLM with the right context excels.
-->

---
layout: section
---

# Act 5: Claude Code Skills

---

# Claude Code Skills

<v-clicks>

- Plain language
- Project patterns
- Read when the task matches
- Slash command - /mock-provider

</v-clicks>

<!--
A skill is a focused instruction file that teaches Claude how to do a specific task. Written in plain language, lives in the project. Describes patterns, helpers, and conventions. Claude reads it when the task matches — like a specialist joining the team. Developers invoke it with a slash command: /mock-provider.
-->

---

# Mock Provider Skill

```
  .claude/skills/mock-provider/SKILL.md
```

<v-clicks>

- Has understanding of mock server
- Generates controller
- Adds failures
- Wires API
- One command

</v-clicks>

<!--
This skill understands the mock server's structure. Given a new provider's OpenAPI YAML file, Claude can generate the RestController following existing patterns, add all the necessary failure scenario endpoints, wire it into the mock server's control API. One command, one new provider — fully mocked.
-->

---

# Mocking Half Solved

We also need to **test through** the mock -- and control it from our tests

<!--
We also need to test through the mock — and control it from our tests. Generating the mock is only half the battle.
-->

---

# Controlling the Mock Server

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

<!--
The sequence: test tells the mock to fail, test runs the pipeline, test checks the result, test resets the mock. This lets us test every scenario — not just the happy path.
-->

---

# Simulate Any Scenario

<v-clicks>

- Happy path
- Server crashes
- Bad requests
- Timeouts
- Retry recovery

</v-clicks>

<!--
Success — everything works perfectly. Server errors — the external system crashes (500). Bad requests — our data format is wrong (400). Timeouts — the external system is too slow. Recovery — fails first, then succeeds on retry.
-->

---

# Testing Guide Skill

```
  .claude/skills/testing-guide/SKILL.md
```

<v-clicks>

- Java Unit tests with Mockito
- Java Integration tests with TestContainers
- E2E Playwright

</v-clicks>

<!--
Our testing-guide skill teaches Claude how to write tests at three levels: unit tests per-service using Mockito mocks, Java integration tests with the full pipeline via TestContainers, and Playwright E2E browser-based tests against the live stack.
-->

---

# Skill Knows Details

<v-clicks>

- Helper methods
- Scheduler differences
- Import conventions fir PlayWright
- Mock API endpoints
- Assertion patterns

</v-clicks>

<!--
Things a developer would need to look up — the skill already knows. Which helper methods exist (createGroup, configureMock, triggerEdgeRetry). That the retry scheduler is disabled in Java tests but enabled in Playwright. How to import from base-test.ts instead of raw Playwright. The mock API endpoints and their parameters. The exact assertion patterns that work for async pipelines.
-->

---

# Skills Working Together

```
  mock-provider skill   →  Knows how to add mock support for a new provider
  testing-guide skill   →  Knows how to write tests at all three levels
  CLAUDE.md             →  Knows the architecture, services, and data flow
```

<v-clicks>

Together, they give Claude the full picture: **providers**, **mocks**, and **tests**

</v-clicks>

<!--
mock-provider skill knows how to add mock support for a new provider. testing-guide skill knows how to write tests at all three levels. CLAUDE.md knows the architecture, services, and data flow. Together, they give Claude the full picture: providers, mocks, and tests.
-->

---
layout: section
---

# Act 6: Demo

---

# Adding a new Provider Demo

<!--
Live demo: give Claude the new provider's OpenAPI spec. The mock-provider skill generates the mock endpoints. The testing skill generates tests at all three levels. Each skill follows the project's established patterns. Nothing is forgotten — mocks, unit tests, integration, and E2E.
-->

---
layout: section
---

# Act 7: Why This Matters

---

# Skills Capture Knowledge

<v-clicks>

- Three levels
- Environment differences
- Right helpers
- Final state

</v-clicks>

<!--
Skills capture what a senior developer would explain to a new team member. "Don't forget to test at all three levels." "The retry scheduler behaves differently in each test environment." "Use these specific helpers, not raw HTTP calls." "Assert on final state, not intermediate mock history."
-->

---

# What We Gain

<v-clicks>

- Consistent coverage
- Developer speed
- Knowledge versioned
- Instant onboarding

</v-clicks>

<!--
Consistency — every new feature gets the same thorough test coverage. Speed — Claude generates the boilerplate, we focus on the interesting parts. Knowledge sharing — the skill file is versioned, reviewed, and improved over time. Onboarding — new developers (and AI) can be productive immediately.
-->

---
layout: center
class: text-center
---

# Summary

We teach Claude about our system through **CLAUDE.md** and **skills**

It uses that knowledge to generate adapters, mocks, and tests that follow our patterns

The result: consistent, thorough test coverage -- even for systems we can't control

<!--
We teach Claude about our system through CLAUDE.md and skills. It uses that knowledge to generate adapters, mocks, and tests that follow our patterns. The result: consistent, thorough test coverage — even for systems we can't control.
-->

---

# Skills at Different Levels

<v-clicks>

- **Project level** -- testing patterns, mock setup, coding conventions
- **Team level** -- review checklists, deployment procedures, incident response
- **Org level** -- security policies, compliance requirements, API standards

</v-clicks>

<!--
Skills aren't just for one project. At the project level, they capture testing patterns and mock setup like we showed. At the team level, they can encode review checklists or deployment procedures. At the org level, they can enforce security policies, compliance requirements, or API standards. The higher the level, the more people benefit from the same encoded knowledge.
-->

---

# Skills vs Agents

<v-clicks>

- **Skill** -- instructions that tell the agent *how* to do something
- **Agent** -- the LLM that *does* the work
- Skills are passive -- they don't run, they inform
- Agents are active -- they read skills and act on them
- Same agent, different skills, different expertise

</v-clicks>

<!--
A skill is a document — it doesn't do anything on its own. It's like a runbook or a checklist. An agent is the LLM that reads the skill and acts on it. The skill tells the agent how to do the task, but the agent decides when and how to apply it. You can give the same agent different skills for different tasks — mock generation, test writing, code review — and it becomes a specialist each time. Skills are the knowledge, agents are the action.
-->

---
layout: end
---

# Thank you!
