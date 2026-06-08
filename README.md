# Bhavya Printers — Java Spring Boot Backend

Drop-in replacement for the original Node.js/Express backend.
All API endpoints, response shapes, and auth logic are identical.

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

## Quick Start

```bash
# 1. Set your database URL
export DATABASE_URL=jdbc:postgresql://localhost:5432/bhavya_printers

# 2. Build
mvn clean package -DskipTests

# 3. Run
java -jar target/bhavya-printers-backend-1.0.0.jar
```

The server starts on port **8080** with context path `/api`.
All routes are accessible at `http://localhost:8080/api/...`

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/bhavya_printers` | PostgreSQL JDBC URL |
| `PORT` | `8080` | HTTP server port |
| `app.settings.path` | `data/settings.json` | Path to the admin settings file |

## Database Setup

Hibernate will auto-create tables on first run (`spring.jpa.hibernate.ddl-auto=update`).
Tables created: `products`, `banks`, `orders`

## Default Admin Credentials

- **Username:** `admin`
- **Password:** `bhavya1996`

Stored hashed in `data/settings.json` (SHA-256 + salt, same as original).

## API Endpoints

All routes are prefixed with `/api`.

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/admin/login` | Admin login |
| POST | `/api/auth/bank/register` | Bank self-registration |
| POST | `/api/auth/bank/login` | Bank login |

### Products
| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List all products |
| POST | `/api/products` | Create product (admin) |
| GET | `/api/products/:id` | Get product |
| PUT | `/api/products/:id` | Update product (admin) |
| DELETE | `/api/products/:id` | Delete product (admin) |

### Banks
| Method | Path | Description |
|---|---|---|
| GET | `/api/banks` | List all banks (admin) |
| POST | `/api/banks` | Create bank (admin) |
| GET | `/api/banks/:id` | Get bank |
| DELETE | `/api/banks/:id` | Delete bank (admin) |

### Orders
| Method | Path | Description |
|---|---|---|
| GET | `/api/orders` | List all orders (admin) |
| POST | `/api/orders` | Place order |
| GET | `/api/orders/:id` | Get order |
| PUT | `/api/orders/:id/status` | Update status (admin) |
| GET | `/api/orders/bank/:bankId` | Get orders for a bank |

### Analytics
| Method | Path | Description |
|---|---|---|
| GET | `/api/analytics/revenue` | Monthly revenue breakdown |
| GET | `/api/analytics/top-banks` | Top banks by spend |
| GET | `/api/analytics/monthly-gst` | Monthly GST breakdown |
| GET | `/api/analytics/dashboard` | Admin dashboard stats |

### Settings
| Method | Path | Description |
|---|---|---|
| GET | `/api/settings` | Get UPI/admin settings |
| PUT | `/api/settings/credentials` | Change admin credentials |
| PUT | `/api/settings/upi` | Update UPI details |

### Health
| Method | Path | Description |
|---|---|---|
| GET | `/api/healthz` | Health check |

## Connecting the Frontend

The React frontend already talks to `/api/*` paths through the reverse proxy.
No changes needed — just make sure both are behind the same reverse proxy
(e.g. nginx, Caddy, or the Replit shared proxy) that routes `/api` → port 8080.

## Project Structure

```
src/main/java/com/bhavyaprinters/
├── BhavyaPrintersApplication.java   — Entry point
├── config/
│   └── CorsConfig.java              — CORS (allows all origins)
├── controller/
│   ├── HealthController.java
│   ├── ProductController.java
│   ├── AuthController.java
│   ├── BankController.java
│   ├── OrderController.java
│   ├── AnalyticsController.java
│   ├── SettingsController.java
│   └── GlobalExceptionHandler.java
├── dto/                             — Request/response shapes
├── entity/                          — JPA entities (Product, Bank, Order)
├── repository/                      — Spring Data JPA repos
└── service/                         — Business logic
    ├── ProductService.java
    ├── BankService.java
    ├── OrderService.java
    ├── AnalyticsService.java
    ├── SettingsService.java
    └── TokenService.java
```


## Google OAuth Setup (Optional)

To enable Google Sign-In for both admin and banks:

### 1. Create Google Cloud OAuth credentials
1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create a new project → APIs & Services → Credentials
3. Create OAuth 2.0 Client ID → Web application
4. Add Authorised JavaScript origins: `http://localhost:5173`
5. Add Authorised redirect URIs: `http://localhost:5173`
6. Copy the **Client ID**

### 2. Configure frontend (bhavya-printers-frontend/)
Create a `.env` file in the frontend directory:
```
VITE_GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID_HERE.apps.googleusercontent.com
```

### 3. Configure backend (application.properties)
```properties
google.admin.email=your-admin@gmail.com
```
This is the Google account that will be granted admin access.

### 4. Bank Google login
Banks log in with their Google account. Their Google email must match the email they registered with.
If they haven't registered yet, Google sign-in pre-fills their email in the registration form.
