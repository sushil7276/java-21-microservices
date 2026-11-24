# Postman – import and run

Two files are in this folder. Both are standard Postman exports and can be imported directly.

| File | What it is |
|---|---|
| `Ecommerce-Microservices.postman_collection.json` | the collection: 52 requests in 10 folders, with automatic checks |
| `Ecommerce-Local.postman_environment.json` | the environment: `baseUrl` and variables for tokens and ids |

## Before you start
The system must be running ([../START.md](../START.md)), otherwise Postman shows "Could not get any response". `./scripts/doctor.sh` must be green.
Other ways to test: [../TESTING.md](../TESTING.md).

## Import (Postman app)
1. Open Postman and click **Import** (top left).
2. Drag **both** files from this folder into the window, then confirm.
3. Top right, open the environment drop-down and select **Ecommerce Local**.
   (If you skip this, variables like `{{baseUrl}}` stay empty and every request fails.)

If the import fails, update Postman to a recent version, or use **Import -> Raw text**
and paste the contents of the file.

## Run everything at once
Hover the collection **Ecommerce Microservices**, click the three dots, choose **Run collection**, then **Run**.
The folders run in order 0 to 9 and pass values to each other (token, product id, order id).
Expected result: **52 requests, 79 assertions, 0 failed**.

## Run requests one by one
Run the folders in order, because later requests use values saved by earlier ones:

| Folder | Saves for later requests |
|---|---|
| 0. Health checks | nothing |
| 1. Users and login | `username`, `token`, `adminToken`, `userId` |
| 2. Products | `productId` |
| 3. Favourites | nothing |
| 4. Cart | nothing |
| 5. Order + payment + delivery | `orderId` |
| 6. Delivery lifecycle | `shipmentId` |
| 7. Payment failure | `failedOrderId` |
| 8. Security | `bobToken` |
| 9. Audit trail | nothing |

## How login works
*Login as user* saves the JWT in `token`. The collection sends it automatically as
`Authorization: Bearer {{token}}`. Admin requests use `{{adminToken}}`. Public requests use "No Auth".
Tokens expire after 1 hour: run the two login requests again.

## See which services handled a request
1. Run **CHECKOUT** in folder 5.
2. Open the **Headers** tab of the response and copy the value of `X-Trace-Id`.
3. In a terminal, from the project root: `./scripts/trace.sh <that-id>`

## Run from the terminal (no app needed)
```bash
./scripts/postman-test.sh
```
This needs the Postman CLI (check with `postman --version`).

## Problems
| Message | Reason and fix |
|---|---|
| `Could not get any response` | The system is not running. Run `./scripts/doctor.sh`. |
| `401 Unauthorized` on many requests | The token is missing or expired. Run *Login as user* and *Login as ADMIN* again. |
| A URL contains `{{productId}}` or `{{orderId}}` literally, or returns 404 | An earlier request was skipped. Run the folders in order. |
| `{{baseUrl}}` shown literally | The **Ecommerce Local** environment is not selected (top right). |
| Stock checks fail | Run all folders in order. Each run creates its own product. |
