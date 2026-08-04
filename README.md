# MyBakery

MyBakery is a Spring Boot web application for a bakery storefront and product administration.

## Features

- Public storefront showing only products marked as available
- Protected admin panel for product creation, editing, deletion, and availability changes
- Product image uploads
- Light and dark theme toggle for the storefront
- Admin sign-in with password management and optional TOTP MFA
- REST API for products
- H2 for local development and PostgreSQL/MySQL/SQL Server support

## Requirements

- Java 17 or later
- Git
- No local database is required for local development: the `local` profile uses H2

## Run locally

From the project root in PowerShell:

```powershell
$env:APP_ADMIN_USERNAME = "admin"
$env:APP_ADMIN_PASSWORD = "choose-a-strong-password"
$env:SPRING_DATASOURCE_URL = "jdbc:h2:mem:mybakery;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
$env:SPRING_DATASOURCE_USERNAME = "sa"
$env:SPRING_DATASOURCE_PASSWORD = ""
$env:SPRING_DATASOURCE_DRIVER = "org.h2.Driver"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Open:

- Storefront: `http://localhost:8080/`
- Admin: `http://localhost:8080/admin` (redirects to `/admin/products`)

The first start with an empty database creates the admin user from `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`.

## Test

```powershell
.\mvnw.cmd test
```

## Deploy to Render

This repository includes a multi-stage `Dockerfile` and `render.yaml` for deployment.

### 1. Push the latest changes

The repository is already on GitHub. Commit and push the deployment-ready changes:

```powershell
git add .
git commit -m "Prepare production deployment"
git push origin main
```

Never commit real database credentials, production passwords, `.env` files, the `uploads/` folder, or log files.

### 2. Create a Render PostgreSQL database

In the [Render Dashboard](https://dashboard.render.com/):

1. Select **New** → **PostgreSQL**.
2. Name it, for example, `mybakery-db`.
3. Select the same region as the future web service.
4. Create the database.
5. From the database's **Connect** or **Info** page, collect the host, port, database name, username, and password.

### 3. Create the Render web service

1. Select **New** → **Web Service**.
2. Select this GitHub repository.
3. Use these settings:

| Setting | Value |
| --- | --- |
| Branch | `main` |
| Runtime | Docker |
| Dockerfile path | `Dockerfile` |
| Root directory | Leave blank |
| Auto Deploy | Enabled |

### 4. Configure production environment variables

In the web service's **Environment** page, add the following values:

| Key | Value |
| --- | --- |
| `APP_ADMIN_USERNAME` | Your production admin username |
| `APP_ADMIN_PASSWORD` | A unique 12+ character production password |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DATABASE_NAME` |
| `SPRING_DATASOURCE_USERNAME` | Render PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | Render PostgreSQL password |
| `SPRING_DATASOURCE_DRIVER` | `org.postgresql.Driver` |
| `SPRING_JPA_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |

If Render provides a URL such as:

```text
postgresql://bakery_user:password@host.example.com/mybakery
```

use the host and database portions to create the JDBC URL:

```text
jdbc:postgresql://host.example.com:5432/mybakery
```

Store credentials only in Render's environment-variable settings.

### 5. Deploy and verify

Create the service or select **Deploy latest commit**. Wait for the logs to show that `BakeryApplication` has started, then open the assigned `https://<service>.onrender.com` URL.

Verify the following:

1. The storefront loads.
2. The theme toggle works.
3. `/admin` redirects to `/admin/products`.
4. You can sign in using the configured admin account.
5. Adding a product and changing its availability updates the storefront.

Each push to `main` triggers a new deployment while auto-deploy is enabled.

### Product image persistence

Images are written to `uploads/` at runtime. Container filesystems are temporary by default, so uploads will be lost after a restart or deployment unless persistent storage is configured.

For a Render persistent disk, attach a disk to the web service with mount path:

```text
/app/uploads
```

The Dockerfile uses `/app` as its working directory, so this path preserves the current upload location. Alternatively, move images to cloud object storage such as Cloudinary or Amazon S3.

### Custom domain

After the `onrender.com` URL is working:

1. Open the web service's **Settings** → **Custom Domains**.
2. Add your domain, for example `mybakery.in`.
3. Add the DNS record Render provides at your domain registrar.
4. Return to Render and verify the domain.

Render creates and renews HTTPS certificates after verification.

## REST API

Public read endpoints:

- `GET /api/products`
- `GET /api/products/{id}`

Authenticated admin endpoints:

- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`

## Security notes

- Admin and product-changing routes require sign-in.
- Available products appear publicly; unavailable products remain visible only in the admin panel.
- Do not expose admin credentials or database passwords in source control.
- Use a production PostgreSQL database instead of the local in-memory H2 fallback.
