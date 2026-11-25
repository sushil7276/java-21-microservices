# Novashop website (frontend)

React 18 + Redux Toolkit (RTK Query) + Tailwind CSS: the shop and the admin panel. It talks only to the API gateway (`:8080`).
How to run it: [02-running.md](../docs/02-running.md#3-website). Logins and what an admin can do: [05-security-and-admin.md](../docs/05-security-and-admin.md).

## Configuration

In development Vite proxies `/api` to the gateway (`vite.config.js`), so no CORS setup is needed.

| Variable | Use |
|---|---|
| `VITE_GATEWAY_URL` | where the dev proxy points (default `http://localhost:8080`) |
| `VITE_API_URL` | for a production build served from another origin, e.g. `https://your-gateway/api`, set before `npm run build` |

Copy `.env.example` to `.env.local` to set them.

## Code structure

```
src/
  app/store.js            Redux store
  services/api.js         RTK Query: every gateway endpoint, caching and tag invalidation
  features/auth           token + user (kept in localStorage), role helpers
  features/ui             theme (dark / light)
  components/             Layout, ProductCard, ProductArt, PasswordInput, modals, route guard, shared UI
  pages/                  Home, Shop, ProductDetail, Cart, Checkout, Orders, Favourites, Profile, Auth
  pages/admin/            Dashboard, Products, Orders + shipment, Users, Audit log
  utils/                  formatting (₹), error mapping, hooks
```

## How the website uses the backend

- **Data, not samples:** categories, the menu, dashboard numbers and charts are computed from API responses. Every list shows a "No data" state when the backend returns nothing.
- **Product pictures** are a link only (`imageUrl`, `http(s)://`, max 2000 characters; the backend rejects other schemes). Admin → Products has an *Image URL* field with a live preview: paste a direct
  image address (right-click a picture → "Copy image address"). A product without a link, or with a link that fails to load, shows a category gradient tile. No image file is ever uploaded or stored.
- **Cart:** the API can only add (increment) or remove a line, so "set quantity" is remove + add.
- **Checkout:** a declined payment returns `402` with the order as the body; the website shows that order with a red banner. The payment method **"Card that gets declined"** triggers it.
- **Sessions:** a `401` on any authenticated call signs the user out ([rules](../docs/05-security-and-admin.md#3-sessions)); a `429` shows a "too many requests" message.
- **Access:** admin-only routes are guarded in the website and enforced by the gateway.
- **Feedback:** there are no pop-up notifications. Messages appear where the action happens (inline banners such as "password changed" or errors), and the screen updates itself (cart badge, totals, lists).
  Password fields have an eye button to show or hide what you type.
