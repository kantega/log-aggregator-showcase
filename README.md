# Log Aggregator Showcase: Claude & The Mystery of Integrations

This repository contains the companion code for the presentation **"Claude and the Mystery of Integrations.
"** It features a sample log aggregator application used to demonstrate how to effectively test third-party integrations, 
moving beyond standard "happy paths" to handle unpredictable real-world failures.

**Key demonstrations in this repo include:**
* **AI-Generated Mock Servers:** Using Claude to spin up a test server that accurately mimics an external provider.
* **Failure Scenario Testing:** Triggering custom responses to test how the application handles unreliability.
* **Testing Exponential Backoff:** Verifying that the system correctly succeeds on a second retry after an initial failure.
* **Claude Skills Integration:** Utilizing custom Skills to feed architectural context and test strategies into Claude, 
minimizing manual prompt engineering.