# Before opening Sweet Bel-Erina to customers

1. In the owner screen, change the sample catalog/prices, enter the bakery's origin address, contact number, GCash receiving name/number, and delivery fee bands. The owner address is needed for distance pricing. Choose the GCash shipping discount from 5% to 7%.
2. Set the owner's email in `STORE_OWNER_EMAIL`, then sign in using that exact Google or Facebook identity. Add Google OAuth and Facebook Login app credentials as protected server secrets. Register these HTTPS callbacks on the providers after choosing the final public domain: `/login/oauth2/code/google` and `/login/oauth2/code/facebook`.
3. Check Google Maps Places and Directions are available to the site. Managed hosting uses the project Maps proxy; independent hosting needs a Google Maps key with the JavaScript, Places, and Directions APIs enabled and restricted to your domain.
4. The current GCash flow is manual: customers transfer, submit their reference, and the owner checks it against the GCash transaction before marking it paid. Automatic verification needs a separately chosen GCash-capable gateway and signed webhook credentials.
5. Publish a saved checkpoint from the project Dashboard, set project visibility to Public, and optionally attach a domain you own in domain settings. Keep HTTPS enabled; update Google/Facebook origins and redirect URLs to the final hostname, then set production OAuth secrets.
6. Test one full customer reservation and one owner verification before announcing the shop.

For VS Code, use JDK 21, Apache Maven 3.9+, and the Extension Pack for Java. Local run: `SESSION_COOKIE_SECURE=false mvn spring-boot:run`. Do not use the local cookie override on HTTPS Preview or production.
