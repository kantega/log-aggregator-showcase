---
name: add-mock-provider
description: Onboard a new Noark-compliant archive provider end-to-end. Given an OpenAPI spec, adds the provider's mock controller to external-apis-mock, scaffolds a new adapter service via adapter-archetype, customizes the adapter's payload/transform/client from the spec, and wires Edge + root pom.xml + start-all.sh. Does NOT run any tests or builds — the user runs them afterwards.
---

# Add Mock Provider (full onboarding)

Onboard a new archive provider (e.g. "Noark C") across the whole stack:

1. **Scaffold `adapter-<slug>/` via `adapter-archetype`** (Maven)
2. **Customize the adapter** from the OpenAPI spec: payload fields, transform mapping, client endpoint path
3. **Add the provider's mock controller + unit test** to `external-apis-mock/`
4. **Wire the new adapter into Edge, root `pom.xml`, and `start-all.sh`**

**Do not run `mvn test`, `mvn install` on the generated adapter, `./start-all.sh`, or any verification.** Tell the user to run tests themselves when done.

## Input

A single OpenAPI spec path (YAML or JSON). Read it **once** at the start. Extract:

| From the spec | Used for |
|---------------|----------|
| `info.title` | Provider display name → derive `providerName`, `providerSlug`, `providerKey` |
| The primary archive endpoint (POST that creates/submits records) | `${providerName}Client.postArchive()`'s `.uri(...)` path |
| The request schema referenced by that endpoint | Fields of `${providerName}Payload` |
| Nested schemas (e.g. document, attachment) | Inner static classes in `${providerName}Payload` |

Do **not** model every endpoint in the spec. Only the one primary "submit archive" path matters.

## Name derivation

From `info.title` (e.g. `"Noark C"`):

| Var | Convention | Example |
|-----|-----------|---------|
| `{providerName}` | PascalCase, no spaces | `NoarkC` |
| `{providerSlug}` | lowercase, spaces → hyphens | `noark-c` |
| `{providerKey}` | lowercase, no separators | `noarkc` |
| `{providerDisplayName}` | `info.title` verbatim, spaces preserved | `Noark C` |
| `{port}` | next free ≥ 8085 — check root `pom.xml` and `start-all.sh` for conflicts | `8085` |

## Prerequisite (assumed already done by the user before the demo)

The archetype must be installed locally:

```bash
cd adapter-archetype && mvn install
```

Do not run this in the skill. If `mvn archetype:generate` in Step 1 fails with "archetype not found," abort and tell the user to install the archetype.

## Execution

### Step 1 — Scaffold the adapter via archetype (Bash, ~10–15s)

**Run archetype:generate in a temp directory, then move the result into the project root.** Running it directly in the project root causes Maven to auto-edit and reformat the parent `pom.xml` (4-space → 2-space, XML decl collapsed) — `<modules>` gets touched as a side effect of generation. Scaffolding in a temp dir avoids that; Step 4b adds the `<module>` line surgically.

```bash
PROJECT_ROOT="$(pwd)"
TMPDIR="$(mktemp -d)"
cd "$TMPDIR" && mvn -q -B archetype:generate \
  -DarchetypeCatalog=local \
  -DarchetypeGroupId=no.kantega \
  -DarchetypeArtifactId=adapter-archetype \
  -DarchetypeVersion=0.0.1-SNAPSHOT \
  -DgroupId=no.kantega \
  -DartifactId=adapter-{providerSlug} \
  -Dpackage=no.kantega.adapter{providerKey} \
  -Dversion=0.0.1-SNAPSHOT \
  -Dport={port} \
  -DproviderName={providerName} \
  -DproviderSlug={providerSlug} \
  -DproviderKey={providerKey} \
  && mv "adapter-{providerSlug}" "$PROJECT_ROOT/" \
  && cd "$PROJECT_ROOT" && rm -rf "$TMPDIR"
```

This creates `adapter-{providerSlug}/` with:
- `pom.xml`, `application.properties` — fully wired
- `ArchiveController`, `ArchiveService`, `ArchiveRequest`, `ArchiveResult` — generic, no changes needed
- Tests for application context, controller, service, and OpenAPI backwards-compat — generic, no changes needed
- **Stubs to customize in Step 2:** `{providerName}Payload.java`, `TransformService.java`, `{providerName}Client.java`, `TransformServiceTest.java`

### Step 2 — Customize the adapter stubs from the spec

**Runs after Step 1 completes.** All four Writes are independent — issue them as parallel tool calls.

Base directory: `adapter-{providerSlug}/src/main/java/no/kantega/adapter{providerKey}/`

**2a. `model/{providerName}Payload.java`** — overwrite the stub with real fields.

Map the primary request schema's properties into the payload. Types: JSON string → Java `String`, JSON array → `List<Inner>`, JSON object → inner static class. Use Lombok `@Data @NoArgsConstructor @AllArgsConstructor`. Pattern:

```java
package no.kantega.adapter{providerKey}.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class {providerName}Payload {
    // Fields from the spec's primary request schema, e.g. for Noark C's ArchiveRecord:
    //   private String recordId;
    //   private String title;
    //   private String description;
    //   private String createdAt;
    //   private List<Document> documents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        // Fields from the nested document/attachment schema
    }
}
```

**2b. `service/TransformService.java`** — overwrite with real mapping.

Map `ArchiveRequest` → `{providerName}Payload`. One payload per request; one inner item per `ArchiveRequest.LogEntry`. Use `Instant.now().toString()` for any "createdAt" / "archivedAt" field. Use `request.getGroupId()`, `request.getGroupName()`, `request.getEntries()`.

```java
package no.kantega.adapter{providerKey}.service;

import no.kantega.adapter{providerKey}.model.ArchiveRequest;
import no.kantega.adapter{providerKey}.model.{providerName}Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransformService {

    public {providerName}Payload transform(ArchiveRequest request) {
        // Map fields from request into the payload structure from the spec.
        // Collections: stream request.getEntries() into inner items.
    }
}
```

**2c. `service/{providerName}Client.java`** — change the endpoint path.

The generated stub uses `.uri("/archive")` which is wrong for most providers. Replace it with the primary submit path from the spec (e.g. `/records` for Noark C). Keep the rest of the file identical.

Minimal surgical edit via `Edit` tool — only change the `.uri(...)` line. Do not rewrite the whole file.

**2d. `src/test/java/no/kantega/adapter{providerKey}/service/TransformServiceTest.java`** — overwrite the stub with real assertions.

Add field-level `assertThat(payload.getX()).isEqualTo(...)` assertions for each payload field that comes from `ArchiveRequest`. One `@Test` for the happy path is enough.

### Step 3 — Add the provider mock controller (parallel with Step 1)

Issue these in the same turn as Step 1 — they don't depend on the archetype.

**3a. Create `external-apis-mock/src/main/java/no/kantega/externalapismock/controller/{providerName}Controller.java`:**

```java
package no.kantega.externalapismock.controller;

import jakarta.servlet.http.HttpServletRequest;
import no.kantega.externalapismock.model.MockConfig;
import no.kantega.externalapismock.service.MockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{providerKey}")
public class {providerName}Controller {

    private final MockService mockService;

    public {providerName}Controller(MockService mockService) {
        this.mockService = mockService;
    }

    @PostMapping("/**")
    public ResponseEntity<String> handlePost(@RequestBody(required = false) String body,
                                             HttpServletRequest request) {
        return handle("POST", body, request);
    }

    @GetMapping("/**")
    public ResponseEntity<String> handleGet(HttpServletRequest request) {
        return handle("GET", null, request);
    }

    @PutMapping("/**")
    public ResponseEntity<String> handlePut(@RequestBody(required = false) String body,
                                            HttpServletRequest request) {
        return handle("PUT", body, request);
    }

    private ResponseEntity<String> handle(String method, String body, HttpServletRequest request) {
        MockConfig config = mockService.getConfig("{providerKey}");
        mockService.recordRequest("{providerKey}", method, request.getRequestURI(), body);

        if (config.getDelayMs() > 0) {
            try {
                Thread.sleep(config.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return ResponseEntity.status(config.getStatusCode()).body(config.getBody());
    }
}
```

**3b. Create `external-apis-mock/src/test/java/no/kantega/externalapismock/controller/{providerName}ControllerTest.java`:**

```java
package no.kantega.externalapismock.controller;

import no.kantega.externalapismock.service.MockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({providerName}Controller.class)
class {providerName}ControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MockService mockService;

    @TestConfiguration
    static class Config {
        @Bean public MockService mockService() { return new MockService(); }
    }

    @Test
    void post_returnsDefaultOk() throws Exception {
        mockMvc.perform(post("/api/{providerKey}/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void post_recordsRequest() throws Exception {
        mockService.reset();

        mockMvc.perform(post("/api/{providerKey}/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":true}"))
                .andExpect(status().isOk());

        var history = mockService.getHistory();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getEndpoint()).isEqualTo("{providerKey}");
    }
}
```

**Do not touch `external-apis-mock/src/test/resources/openapi-baseline.json`.** Adding a new controller is an additive (non-breaking) OpenAPI change — `OpenApiBackwardsCompatibilityTest` will pass against the existing baseline. If the user wants a refreshed baseline they can delete it themselves and re-run the test.

### Step 4 — Wire the adapter into the stack (parallel with Step 1)

Issue these Edits in the same turn as Step 1. They touch different files.

**4a. Edge config** — Edit `edge/src/main/resources/application.properties`, append (use next free index ≥ 2):

```properties
adapters[2].name=adapter-{providerSlug}
adapters[2].url=http://localhost:{port}
```

**4b. Root `pom.xml`** — inside `<modules>`, add:

```xml
        <module>adapter-{providerSlug}</module>
```

**4c. `start-all.sh`** — two surgical edits:

1. After the adapter-noark-b launcher line, add:
   ```bash
   cd "$SCRIPT_DIR/adapter-{providerSlug}"   && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port={port}" &
   ```
2. After the "Adapter Noark B" URL echo, add (use `{providerDisplayName}` so spacing matches the existing "Noark A" / "Noark B" lines, not the PascalCase `{providerName}`):
   ```bash
   echo "  Adapter {providerDisplayName}:    http://localhost:{port}"
   ```

### Step 5 — Stop

Report a concise summary:

- Scaffolded: `adapter-{providerSlug}/` via archetype (in temp dir, then moved into project root)
- Customized: `{providerName}Payload.java`, `TransformService.java`, `{providerName}Client.java` (endpoint → `<path-from-spec>`), `TransformServiceTest.java`
- Added to mock: `{providerName}Controller.java` + test
- Wired: edge config, root pom.xml, start-all.sh

End with:
> Run `mvn -pl external-apis-mock,adapter-{providerSlug} install -DskipTests && (cd external-apis-mock && mvn test) && (cd adapter-{providerSlug} && mvn test)` to verify. Or start the stack with `./start-all.sh`.

**Do not run `mvn test`, `mvn install` on new modules, or `./start-all.sh`.**

## Parallelization plan

Group tool calls across **three turns**:

**Turn A (single tool call):** Read the OpenAPI spec.

**Turn B (parallel tool calls — one message, many tools):**
- Bash: archetype:generate in temp dir + `mv` into project root (Step 1)
- Write: mock `{providerName}Controller.java` (Step 3a)
- Write: mock `{providerName}ControllerTest.java` (Step 3b)
- Edit: `edge/src/main/resources/application.properties` (Step 4a)
- Edit: root `pom.xml` — surgical add of `<module>` line (Step 4b). Safe to run alongside Step 1 because archetype:generate runs in a temp dir and does not touch the project's parent pom.
- Edit: `start-all.sh` — launcher line (Step 4c.1)
- Edit: `start-all.sh` — URL echo line (Step 4c.2)

**Turn C (after B completes — parallel tool calls):**
- Write: `adapter-{providerSlug}/.../model/{providerName}Payload.java` (Step 2a)
- Write: `adapter-{providerSlug}/.../service/TransformService.java` (Step 2b)
- Edit: `adapter-{providerSlug}/.../service/{providerName}Client.java` — only the `.uri(...)` line (Step 2c)
- Write: `adapter-{providerSlug}/.../service/TransformServiceTest.java` (Step 2d)

Total: ~12 tool calls in 3 batches. Bound by the archetype (~10–15s) in Turn B.

## Constraints

- Do not read `adapter-noark-a/` or `adapter-noark-b/`. The archetype is the source of truth for structure.
- Do not read `MockService.java` or other mock internals. Trust the template in Step 3.
- Do not adjust frontend code.
- Do not run tests or builds.
