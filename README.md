# SWEET BEL-ERINA

A Java 21 / Spring Boot bakery storefront with a responsive product catalog, saved customer profiles, timed reservations, COD and manual GCash checkout, Google Maps distance pricing, and an owner settings screen. The browser UI is plain HTML/CSS/JavaScript; the server and database access use Java and JDBC.

## Open and run in VS Code

1. Install JDK 21, Apache Maven 3.9+, and Microsoft's **Extension Pack for Java**. Open this project folder in VS Code.
2. Copy `.env.example` to `.env`. Blank `DATABASE_URL` uses a temporary H2 in-memory database for the local demo. `SESSION_COOKIE_SECURE=false` is only for plain `http://localhost`.
3. Choose **Run > Start Debugging** and select **Sweet Bel-Erina (local)**, or use the VS Code Maven task **Maven: package**. The run configuration loads `.env`.
4. Open `http://localhost:3000`.

To run in a terminal with `.env`, use `set -a; . ./.env; set +a; mvn spring-boot:run`. To persist local data across restarts, set `DATABASE_URL` to a local MySQL JDBC URL, such as `jdbc:mysql://localhost:3306/sweetbel?sslMode=DISABLED`, and provide its local username/password. The managed public deployment supplies a separate project database DSN with verified TLS. Never commit `.env` or real secrets.

## Set up the shop before taking orders

Open `/admin.html` after signing in with the Google or Facebook account whose email is configured in `STORE_OWNER_EMAIL`. Set your bakery/pickup address and map coordinates, contact number, GCash receiving details, delivery fee bands, and product descriptions/prices/photos. The starter products, prices, and fee bands are examples. Customer Google/Facebook login only becomes active after configuring the corresponding OAuth app credentials and callback URLs (see [PUBLIC-LAUNCH.md](PUBLIC-LAUNCH.md)).

GCash checkout is manual: the buyer transfers to the configured GCash account and submits a reference number; the owner verifies payment before changing the order status. This code does not claim automatic payment verification.

## Project structure

- `src/main/java/com/sweetbelerina/` — Spring Boot APIs, OAuth, Maps proxy, delivery quote, order persistence, and store settings.
- `src/main/resources/schema.sql` — repeatable, additive database tables.
- `src/main/resources/static/index.html`, `app.css`, `app.js` — responsive storefront, cart, and checkout.
- `src/main/resources/static/admin.html`, `admin.css`, `admin.js` — owner-only shop settings, products, and order review.
- `.vscode/` — recommended Java extension, debug launch, and Maven build task.
- `Dockerfile` — clean Java container build for public deployment.
- `PUBLIC-LAUNCH.md` — provider setup and go-live checklist.
