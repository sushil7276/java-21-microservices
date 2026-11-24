# Admin access – credentials and how to give admin rights

> These are **development defaults** for local use. Change them before this project is reachable by anyone else
> (see section 5).

## 1. Default admin account

| | |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Email | `admin@ecommerce.local` |
| Role | `ADMIN` |

* Created automatically by `AdminSeeder` ([user-service](user-service/src/main/java/com/ecommerce/user/config/AdminSeeder.java))
  the first time user-service starts **and no user named `admin` exists**. The credentials are hard-coded in that class.
* Login: frontend <http://localhost:5173/login>, or `POST http://localhost:8080/api/auth/login`
  with `{"username":"admin","password":"admin123"}`.
* Tokens expire after 1 hour.

Other local credentials (for reference): MySQL `root` / `123456` (see [SETUP.md](SETUP.md)).

## 2. What an admin can do

Enforced by the API gateway ([SecurityConfig](api-gateway/src/main/java/com/ecommerce/gateway/SecurityConfig.java)), not only by the UI:

| Action | Endpoint |
|---|---|
| Create / update / delete products | `POST` `PUT` `DELETE /api/products/**` |
| List all users, delete a user | `GET /api/users`, `DELETE /api/users/{id}` |
| List all orders | `GET /api/orders/all` |
| Ship / deliver a shipment | `PUT /api/shipments/{id}/status` |
| Read the audit log | `GET /api/audit/**` |

In the frontend these are under **Admin panel** (`/admin`), shown only to admin users.

## 3. Important: normal sign-up never creates an admin

Every account created through **Sign up** / `POST /api/auth/register` gets role `USER`.
There is **no API or screen that promotes a user to admin**. So there are only two ways to make someone an admin:

### Option A (recommended): promote one specific user in the database

The person registers normally, then you run (MySQL on localhost:3306):

```bash
docker exec ecommerce-mysql mysql -uroot -p123456 \
  -e "UPDATE user_db.users SET role='ADMIN' WHERE username='their_username';"
```

(or the same `UPDATE` in any MySQL client). They must **sign out and sign in again**: the role is stored in the
login token, so the old token still says `USER`.

To remove admin rights: set `role='USER'` again (the old token keeps working until it expires, max 1 hour).

### Option B: share the `admin` login

Anyone who knows `admin` / `admin123` is an admin. It works, but:

* you cannot tell who did what (the audit log shows the same user for everyone);
* you cannot take access from one person without changing the password for all;
* the password is in this repository, so anyone with the code knows it.

Use Option B only for a throw-away demo.

## 4. The admin account is locked (cannot be edited or deleted)

Accounts with role `ADMIN` are **read-only** in the app:

* **Backend** (`UserService`): `PUT /api/users/{id}` and `DELETE /api/users/{id}` on an admin answer **403**
  ("The admin account is protected..."). This applies to everyone, including the admin themselves, so a stolen admin
  session cannot change the email/password and lock the owner out.
* **Frontend**: on *Account* (`/profile`) the email and password fields are disabled and there is no Save button; the
  Users page shows no delete button for admins.
* **Full access**: an admin can open every page (home, shop, cart, orders, favourites, profile) as well as the
  admin panel; the header shows an extra **Admin panel** link.
* **Managing other users**: Admin panel → Users → **Edit** lets the admin change another user's email and reset their
  password (with a *Generate* button for a random temporary password). Admin accounts show "Protected" instead.
  Sessions the user already has stay valid until they expire (1 hour).

### How to change the admin password anyway

Because the API refuses, change it at the source (a deliberate, developer-only action):

1. Change the credentials in `AdminSeeder` (ideally read them from environment variables).
2. Delete the old admin row: `docker exec ecommerce-mysql mysql -uroot -p123456 -e "DELETE FROM user_db.users WHERE username='admin';"`
3. Restart `user-service`; the seeder recreates `admin` with the new values.

Note: the same lock applies to any user you promote with the SQL in section 3. **Set their password before promoting
them**, because they cannot change it afterwards.

## 5. Before using this project anywhere real

1. Replace the default admin password (section 4 explains how, since the app itself cannot change it), ideally by moving
   the seed credentials out of the code (read them from environment variables, e.g. `ADMIN_USERNAME` / `ADMIN_PASSWORD`,
   in `AdminSeeder`).
2. Change the JWT signing secret and the MySQL `root` password (both are defaults in the config files).
3. Prefer Option A over sharing one admin account.

## 6. When you copy this project into another project

* Bring the `AdminSeeder` class and the gateway `SecurityConfig` rules together; the role names (`ADMIN`, `USER`)
  must match in both places and in the JWT `role` claim written by `JwtService`.
* The frontend decides what to show from `user.role === 'ADMIN'` (`frontend/src/features/auth/authSlice.js`),
  but real protection is the gateway; never rely on the UI alone.
