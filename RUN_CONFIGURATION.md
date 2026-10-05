# Inkwell Run Configuration

## Database Name

Use one MySQL database:

`inkwell_platform`

## 1) Initialize MySQL once

Run:

`mysql -u root -p < mysql-init.sql`

This creates:

- database: `inkwell_platform`
- user: `root`
- password: `admin`

## 2) Set backend env vars in PowerShell

You can copy `backend.env.example` to `backend.env` and edit values.
`start-backend.ps1` will load `backend.env` automatically.

`$env:INKWELL_DB_URL="jdbc:mysql://localhost:3306/inkwell_platform?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"`

`$env:INKWELL_DB_USERNAME="root"`

`$env:INKWELL_DB_PASSWORD="admin"`

`$env:INKWELL_DB_DRIVER="com.mysql.cj.jdbc.Driver"`

`$env:INKWELL_JPA_DIALECT="org.hibernate.dialect.MySQLDialect"`

`$env:GATEWAY_ALLOWED_ORIGIN="http://localhost:5173"`

All services use this same database via shared env vars.

## 3) Start backend

`.\start-backend.ps1`

Backend logs for all services are written to:

`log/`

- Spring logger files: `<service>.app.log`
- Launcher process streams: `<service>.out.log` and `<service>.err.log`

`start-backend.ps1` sets `INKWELL_LOG_DIR` automatically to this folder.
If you start a service manually from IDE/terminal, set it explicitly to keep logs centralized:

`$env:INKWELL_LOG_DIR="C:\Users\mishr\Desktop\Inkwell\log"`

Ports:

1. `service-registry` -> `8761`
2. `api-gateway` -> `8080`
3. `auth-service` -> `8081`
4. `post-service` -> `8082`
5. `comment-service` -> `8083`
6. `category-service` -> `8084`
7. `media-service` -> `8085`
8. `newsletter-service` -> `8086`
9. `notification-service` -> `8087`

## 4) Frontend

Create `inkwell-frontend/.env`:

`VITE_API_BASE_URL=http://localhost:8080`

Run:

`cd inkwell-frontend`

`npm install`

`npm run dev`

## 5) Auth and panels

- `/author` is protected for `AUTHOR` and `ADMIN`
- `/admin` is protected for `ADMIN`
- `/profile`, `/notifications`, `/newsletter` require login

## 6) OAuth note

Current implementation supports local login/register fully.

Google/GitHub button flow is not full redirect OAuth yet, so client id/client secret are not required right now.
