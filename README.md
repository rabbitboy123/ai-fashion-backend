# AI Fashion Shop Backend

REST API for the AI Fashion Shop, built with Java 21 and Spring Boot 3.3.4. The application uses PostgreSQL, Spring Data JPA, stateless JWT authentication, Jakarta Bean Validation, and a feature-oriented package structure under `src/main/java/com/fashion/backend`.

## Features

- Authentication, registration, profile, and one-time-code password reset by email.
- Product and category catalog, cart, order creation, and inventory management.
- Bank transfer checkout with VietQR details, SePay webhook verification, payment expiry, and admin reconciliation.
- AI recommendations and history.
- Standardized validation and API error responses.

## Run Locally

Install JDK 21 and configure PostgreSQL, then set the environment variables below. Start the API from this directory:

```powershell
./mvnw.cmd spring-boot:run
./mvnw.cmd test
./mvnw.cmd package
```

The server listens on port `8080` by default. `PORT` can override it in container hosting.

## Configuration

Set secrets and service details through the runtime environment. Do not commit credential values.

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Database credentials |
| `JWT_SECRET` | JWT signing key (at least 32 UTF-8 bytes) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP server for password reset codes |
| `MAIL_FROM` | Sender address for reset emails |
| `SEPAY_WEBHOOK_SECRET` | Secret used to verify SePay webhook signatures |
| `VIETQR_BANK_ID`, `VIETQR_ACCOUNT_NUMBER`, `VIETQR_ACCOUNT_NAME` | Bank account details for checkout |
| `GEMINI_API_KEY` | Optional AI recommendation provider key |

Password reset delivery requires a working SMTP service. Bank transfer confirmation requires a correctly configured SePay webhook and VietQR account. Keep webhook and signing secrets private, and use separate values for production.

## API and Operations

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- Liveness/readiness health: `/actuator/health/liveness` and `/actuator/health/readiness`
- Public catalog: `GET /api/products`, `GET /api/categories`
- Auth: `/api/auth`; password reset: `/api/auth/forgot-password` and `/api/auth/reset-password`
- Cart and orders: `/api/cart`, `/api/orders`
- SePay webhook: `/api/payments/webhooks/sepay`

Protected API calls use `Authorization: Bearer <JWT>`. Swagger UI includes a bearer authorization control.

## Layout

- `src/main/java/.../modules`: business features and their controllers, DTOs, services, and repositories.
- `src/main/java/.../common`, `config`, `security`: shared API errors/types and cross-cutting configuration.
- `src/main/resources/application.yaml`: environment-based runtime configuration.
- `src/test/java`: unit and integration tests in matching packages.
- `Dockerfile`: multi-stage Java 21 container build for deployment.
