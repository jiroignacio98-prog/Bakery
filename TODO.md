# TODO — Sweet Bel-Erina Bakery

- [ ] **Editable bakery identity.** Provide an owner-controlled setting for changing the store name from SWEET BEL-ERINA without editing source code.
  - Status: The owner settings screen and API are implemented. Configure the owner's account email and Google/Facebook OAuth credentials before using it.
- [ ] **Restricted customer authentication and saved details.** Require customers to sign up or log in before ordering, using Google or Facebook as the only sign-in methods. Collect each customer’s name, phone number, and delivery address and persist the details in a database.
  - Status: Only Google/Facebook OAuth is implemented and customer profiles are database-backed. Provider app credentials have not been configured, so sign-in/order placement is not active yet.
- [ ] **Google Maps address support.** Accept, search, and validate entered delivery addresses through Google Maps location lookup.
  - Status: Places and driving-route integrations are wired to the managed Maps proxy. Set the bakery origin address and map coordinates in owner settings before delivery quotes can be used.
- [x] **Bakery catalog and basket.** Display Banana Loaf, Crinkles, Dream Cake, and additional configurable products with pink-and-brown themed cards and hover animations. Let customers add bakery products to a basket, adjust or remove selections, view the cart, and choose a reservation or fulfillment time.
  - Status: Implemented; the database/catalog endpoint returned six starter products and the storefront/basket routes were verified.
- [ ] **Checkout, delivery pricing, and payment choice.** Provide Cash on Delivery or GCash online payment and present the complete order summary before submission. Calculate a higher delivery fee for far-away addresses and apply a configurable 5–7% shipping discount when GCash is selected.
  - Status: The flow and distance-band pricing are implemented. Configure OAuth, bakery origin/delivery settings, and GCash receiving details before accepting orders. GCash currently uses manual transfer and owner reference review.
- [x] **No-refund confirmation.** Clearly disclose that completed orders are non-refundable and require customer acknowledgment before final order placement.
  - Status: The checkout displays the required acknowledgment and the server rejects an order without it.
- [x] **Responsive ordering and public-launch guidance.** Support laptop and phone layouts, including a mobile three-dot menu for Add to Cart, View Cart, and Checkout. Include practical instructions for deploying the Java application, database, domain, HTTPS, OAuth credentials, Google Maps key, and GCash configuration so the site is publicly accessible.
  - Status: Responsive layout and mobile actions are implemented; the owner-facing public-launch guide and VS Code quickstart are included. Publishing and setting visibility to Public remain owner actions.
