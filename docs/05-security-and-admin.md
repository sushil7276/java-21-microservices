# 05 · Security and the admin account

> The credentials are **development defaults** (login table in the [README](../README.md#where-things-are)). Change them before anyone else can reach this project (section 4).

## 1. Who can do what

Login returns a **JWT** (valid 1 hour) created by `user-service/.../JwtService.java`. The API gateway checks it on every request
([SecurityConfig](../api-gateway/src/main/java/com/ecommerce/gateway/SecurityConfig.java)); the website only hides what you may not use.

- **Public:** register, login, reading products.
- **Logged-in user:** own profile, cart, favourites, orders, payments, shipments.
- **Admin** (role `ADMIN`): everything above on **every page** of the website, plus the **Admin panel** (`/admin`):

| Action | Endpoint | In the Admin panel |
|---|---|---|
| Create / update / delete products | `POST` `PUT` `DELETE /api/products/**` | Products |
| List all users, delete a user | `GET /api/users`, `DELETE /api/users/{id}` | Users |
| Change another user's email or password | `PUT /api/users/{id}` | Users → **Edit** (with a *Generate* button for a random temporary password) |
| List all orders | `GET /api/orders/all` | Orders |
| Ship / deliver a shipment | `PUT /api/shipments/{id}/status` | Orders → Shipment |
| Read the audit log | `GET /api/audit/**` | Audit log |
| Import products, make sales reports, clean up | `/api/batch/**` | Batch jobs ([06-redis-and-batch.md](06-redis-and-batch.md#spring-batch-batch-service)) |

The default admin is created by `AdminSeeder` ([source](../user-service/src/main/java/com/ecommerce/user/config/AdminSeeder.java)) when
user-service starts and no user named `admin` exists. The credentials are hard-coded there.

## 2. The admin account is locked

Accounts with role `ADMIN` are read-only: nobody, including the admin, can change their email or password or delete them.

- **Backend** (`UserService`): `PUT` and `DELETE /api/users/{id}` on an admin answer **403** ("The admin account is protected …").
  A stolen admin session therefore cannot change the password and lock the owner out.
- **Website:** on the profile page the email and password fields are disabled and there is no Save button; the Users page shows "Protected" instead of Edit and delete.

## 3. Sessions

Every user has a **token version** in Redis ([how it works](06-redis-and-batch.md#3-login-revocation-user-service--api-gateway)).
When a password is changed (by the user or by an admin) or the user is deleted, **all existing logins of that user stop working immediately**
and they must sign in again. Logins also end after 1 hour, or on any `401`, which signs the website out.

## 4. Before using this project anywhere real

1. Replace the default admin password (section 6).
2. Change the JWT signing secret (`JWT_SECRET`) and the MySQL `root` password; both are defaults in the config files.
3. Prefer promoting individual users (section 5A) over sharing one account.

## 5. Making someone else an admin

Normal sign-up always creates role `USER`, and **no screen or API promotes a user**. Two ways:

**A (recommended): promote one specific user in the database.** The person signs up first, then:

```bash
docker exec ecommerce-mysql mysql -uroot -p123456 \
  -e "UPDATE user_db.users SET role='ADMIN' WHERE username='their_username';"
```
They must **sign out and in again** (the role is stored in the login token). To remove the rights set `role='USER'`; the old token works until it expires.
The lock of section 2 applies to them too, so **set their password before promoting them**.

**B: share the `admin` login.** It works, but you cannot tell who did what (the audit log shows one user), you cannot take access from one
person without changing the password for all, and the password is in this repository. Use it only for a throw-away demo.

## 6. Changing the admin password

The app cannot do it (section 2), so change it at the source, as a deliberate developer action:

1. Change the credentials in `AdminSeeder` (better: read them from environment variables such as `ADMIN_USERNAME` / `ADMIN_PASSWORD`).
2. Delete the old row: `docker exec ecommerce-mysql mysql -uroot -p123456 -e "DELETE FROM user_db.users WHERE username='admin';"`
3. Restart user-service ([02-running.md](02-running.md#2-one-service)); the seeder creates `admin` with the new values.

## 7. Copying this to another project

- Take `AdminSeeder` and the gateway `SecurityConfig` rules together. The role names (`ADMIN`, `USER`) must match in both and in the JWT `role` claim written by `JwtService`.
- The website decides what to show from `user.role === 'ADMIN'` (`frontend/src/features/auth/authSlice.js`), but the real protection is the gateway. Never rely on the UI alone.
