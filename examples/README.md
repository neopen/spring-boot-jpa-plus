# Examples and legacy code

The folders in this directory are intentionally excluded from the root Maven build and from the published starter jar.

- `demo/`: the original order-management sample (entities, repositories, controller and service). It is retained as a migration/reference sample, not as a production-ready application.
- `legacy-web-extension/`: the original HTTP/AOP helpers (request logging, idempotency token, date-string validation and rate limiting). These helpers require additional Web/AOP/JSON/cache dependencies and are not enabled by the JPA Plus auto-configuration.

If these capabilities are promoted to supported products, move them into a separately versioned Maven module with their own dependency list, tests, configuration properties and documentation. Do not copy the legacy files into the core artifact.
