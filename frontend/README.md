# Novashop frontend

React 18 + Redux Toolkit (RTK Query) + Tailwind CSS storefront and admin panel for the
Spring Boot microservices backend. It talks only to the API gateway (`:8080`).

## Run

```bash
# 1. backend: infra + services (from the repo root)
./scripts/start-infra.sh && ./scripts/start-services.sh

# 2. frontend
cd frontend
npm install
npm run dev        # http://localhost:5173
```

In development Vite proxies `/api` to the gateway (`vite.config.js`), so there is no CORS setup.
Point it elsewhere with `VITE_GATEWAY_URL`. For a production build served from a different origin set
`VITE_API_URL=https://your-gateway/api` before `npm run build`.

> If `npm install` hangs, your npm registry may be unreachable. Use
> `npm install --registry https://registry.npmjs.org`.

## Demo accounts

| Role  | Login                | Notes                                   |
|-------|----------------------|-----------------------------------------|
| Admin | `admin` / `admin123` | Admin panel at `/admin`                 |
| User  | register in the app  | Sign-up logs you in automatically       |

Checkout payment method **"Card that gets declined"** exercises the failed-payment path
(order is kept as `PAYMENT_FAILED`, stock is released).

## Structure

```
src/
  app/store.js            Redux store
  services/api.js         RTK Query: every gateway endpoint, caching and tag invalidation
  features/auth           token + user (persisted in localStorage), role helpers
  features/ui             theme (dark/light)
  components/             Layout, ProductCard, ProductArt, modals, route guard, shared UI
  pages/                  Home, Shop, ProductDetail, Cart, Checkout, Orders, Favourites, Profile, Auth
  pages/admin/            Dashboard, Products CRUD, Orders + shipment, Users, Audit log
  utils/                  formatting, error mapping, hooks
```

## Notes on the backend contract

* Product pictures are stored as a **link only** (`imageUrl`, max 2000 chars, `http(s)://` only; the backend rejects other schemes).
  Admin → Products → New/Edit has an *Image URL* field with a live preview. Paste a direct image address
  (right-click a picture on any site → "Copy image address"). Products with no link, or a link that fails to load,
  show a category gradient tile instead. The image itself is never uploaded or stored by this project.
* The cart API can only add (increment) or remove a line; "set quantity" is remove + add.
* Checkout returns `402` with the order as body when payment is declined; the UI routes to that order.
* A 401 on any authenticated call (tokens last 1 hour) logs the user out.
* Admin-only routes are guarded in the UI **and** enforced by the gateway.
* An admin has full access: every storefront page plus the admin panel (extra "Admin panel" link in the header).
  The admin account itself is read-only (the backend answers 403 to edits/deletes), but the admin can edit other
  users' email and password under Admin panel → Users → Edit.
* Nothing on screen is sample data: categories, navigation, dashboard numbers and charts are all computed from API
  responses, and every list shows a "No data" state when the backend returns nothing.
* There are no pop-up notifications. Feedback is shown where it happens: inline messages on forms (e.g. the green
  "password changed" banner on the profile page, red error banners), and the screen itself updates (cart badge,
  totals, lists). Password fields have an eye button to show / hide what you type (`components/PasswordInput.jsx`).
