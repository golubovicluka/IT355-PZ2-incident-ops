# IncidentOps backend

## Local authentication demo

Start the local defense/demo profile from this directory:

```shell
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

The `local` profile enables the demo users, supplies an explicitly local-only
JWT signing fallback, and permits the Vite development origin at
`http://localhost:5173`.

Outside local development, configure a unique `JWT_SECRET` containing at least
32 UTF-8 bytes and a comma-separated `CORS_ALLOWED_ORIGINS` value. The
deterministic signing value in `src/test/resources/application-test.properties`
is only for automated tests and must never be reused by a running application.
