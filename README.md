# FINT FLYT GATEWAY TEMPLATE

## Overview
This template is a skeleton gateway with example code for creating an instance gateway in FINT Flyt. It receives incoming HTTP requests, maps payloads to `InstanceObject`, and publishes data internally in the FINT Flyt flow.

The project includes two example flows:
- **Simple**: a single instance without collections and without file upload.
- **Advanced**: collections and file upload using the `persistFile` callback.

## Getting Started
Customize this template for your source system before deployment.

### Rename and adjust template identifiers
Update template names and identifiers so they match your source application:
- Rename project/artifact names such as `fint-flyt-example-gateway`.
- Rename the `no.novari.flyt.example.gateway` package and the `/api/example` routes.
- Update `fint.application-id` in `src/main/resources/application.yaml`.
- Update metadata and image values in `kustomize/base/flais.yaml`.

### Configure source application access
In `src/main/resources/application-flyt-security.yaml`, set the gateway's routes and its `SourceApplication` from
[flyt-security-starter](https://github.com/FINTLabs/fint-flyt-security-starter); a new source application needs an
entry there first. Until both are set, the template's routes are denied.

```yaml
novari:
    flyt:
        security:
            external:
                paths: /api/acos/**
                source-application: ACOS
```

Only tokens from the source application's NAM client are accepted. Load its client ID under `spec.env` in
`kustomize/base/flais.yaml`:

```yaml
-   name: fint.flyt.acos.sso.client-id
    valueFrom:
        secretKeyRef:
            name: fint-flyt-acos-oauth2-client
            key: fint.flyt.acos.sso.client-id
```

### Local application configuration
Adjust local settings in `src/main/resources/application-local-staging.yaml`:
- `server.port`: use `81xx`, where `xx` reflects the source application ID (for example, ID `9` -> port `8109`).

## Processing Instances
Create one `InstanceProcessor` per incoming model type using `InstanceProcessorFactoryService`.

In this template, examples are:
- `src/main/kotlin/no/novari/flyt/example/gateway/instance/simple/SimpleProcessorConfiguration.kt`
- `src/main/kotlin/no/novari/flyt/example/gateway/instance/advanced/AdvancedProcessorConfiguration.kt`

Each processor uses an `InstanceMapper` implementation:
- `src/main/kotlin/no/novari/flyt/example/gateway/instance/simple/SimpleMappingService.kt`
- `src/main/kotlin/no/novari/flyt/example/gateway/instance/advanced/AdvancedMappingService.kt`

Use the advanced mapping example if you need to persist attachments through the file service. The advanced example supports both:
- JSON requests with base64 encoded file content through `InstanceProcessor` and `InstanceMapper`.
- Multipart requests with `MultipartFileReference` through `MultipartInstanceProcessor` and `MultipartInstanceMapper`.

Multipart requests to `/api/example/instances/advanced` must include an `instance` part containing the JSON payload, plus one or more file parts. The JSON payload should reference file parts with `multipartFileReference`:

```json
{
  "sysId": "sys-adv-1",
  "string1": "hello",
  "int1": 7,
  "documents": [
    {
      "title": "Attachment",
      "multipartFileReference": {
        "partName": "documents",
        "fileName": "attachment.pdf",
        "originalFilename": "attachment.pdf"
      }
    }
  ],
  "caseWorkers": [
    {
      "email": "caseworker@example.com",
      "name": "Case Worker"
    }
  ]
}
```

## Testing Locally
Start Kafka on `localhost:9092` and Postgres on `localhost:5432` (database `flais`, user and password `postgres`) with `docker compose --profile tools up -d`. Services in other repos can use this Postgres when they run locally. Kafka topics and the database are empty on every start. The `tools` profile also starts Kafdrop, where Kafka messages can be inspected:
- http://localhost:19000/

Calls need a NAM token from the source application's client, and `fint.flyt.<app>.sso.client-id` set to that client ID.
The gateway calls intgr-conf-service and history-service with `fint-flyt-authorization-oauth2-client`, so set
`FINT_FLYT_AUTHORIZATION_SSO_CLIENT_ID` and `FINT_FLYT_AUTHORIZATION_SSO_CLIENT_SECRET` from a beta namespace.

Run this gateway with local profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=local-staging'
```

## GitHub Workflows
The template uses shared workflows from `FINTLabs/fint-flyt-github-workflows`:
- `CI.yaml` builds and optionally publishes an image.
- `CD.yaml` deploys completed main-branch builds.
- `MD.yaml` builds, publishes, and deploys manually to the selected cluster and organisation.

## Technology
- Spring Boot 4.2, Jackson 3, Java 25 and Kotlin
- `no.novari:flyt-gateway-starter` (versions in `gradle/libs.versions.toml`). See its README for the OAuth clients and
  registrations a gateway needs.
- Blocking architecture (non-reactive)

## Additional Documentation
See [docs/architecture.md](docs/architecture.md) for C4 diagrams. For more details on FINT Flyt architecture and setup, see:
- https://fintlabs.atlassian.net/wiki/spaces/FINTKB/pages/379355169/FINT+Flyt+Arkitektur+event+driven

Document your source application details (data models, request contracts, and endpoints) alongside your implementation.

## Dependency Locking

The build uses [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html), so every build resolves exactly the same direct and transitive versions. The locked versions live in the committed `gradle.lockfile` files. A pull request therefore shows every transitive version change it causes. Gradle plugins are not locked, because Dependabot only updates `gradle.lockfile`; their versions are pinned in `gradle/libs.versions.toml` instead.

**Automatic (Dependabot):** Dependabot updates the lockfiles in its own pull requests, so no manual step is needed. If two Dependabot pull requests conflict in a lockfile, Dependabot rebases the remaining one after the first is merged. If it does not, comment `@dependabot recreate` on the pull request.

**Manual:** Any change you make by hand that affects dependency resolution requires regenerating the lockfiles. That covers versions, constraints, and adding or removing a dependency:

```bash
./gradlew dependencies --write-locks
```

Commit the updated lockfiles together with the change. If you forget, the build fails both locally and in CI with an error saying the resolved version "is not part of the dependency lock state".
