# Public launch checklist

The project has a Java 21 Spring Boot container and managed database declared. Saving a website checkpoint and publishing it are separate actions; the site is not public until you publish it and set its project visibility to public.

## 1. Prepare the shop

Before taking real orders, sign in as the owner and visit `/admin.html`. Set the store name, pickup/origin address (and map location), delivery fees and distance bands, GCash receiving name/number, and product descriptions/prices/photos. The initial prices and fee bands are sample settings. Set `STORE_OWNER_EMAIL` to the email on the Google/Facebook account you will use as the store owner. Owner controls stay closed if this is blank.

## 2. Configure customer sign-in

Create a Google OAuth web application and a Facebook Login app. Add your site's exact HTTPS callback URLs in each provider console:

- Google: `https://YOUR-PUBLIC-DOMAIN/login/oauth2/code/google`
- Facebook: `https://YOUR-PUBLIC-DOMAIN/login/oauth2/code/facebook`

Add the site's HTTPS origin to Google's authorized JavaScript origins and the domain plus callback URL to Facebook's app settings. Set these server environment values through the project's protected Secrets/configuration UI (do not put real values in source control): `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `FACEBOOK_APP_ID`, and `FACEBOOK_APP_SECRET`. Only configured providers appear as usable sign-in options; ordering remains blocked until the customer is signed in.

## 3. Google Maps

On the managed Manus deployment, the app uses the platform Maps proxy when its browser-safe and server credentials are available. The customer address field can fall back to typed text if Places is not enabled. If you host this Java project outside Manus, create a Google Maps Platform key with Maps JavaScript API, Places API, and Directions API enabled, restrict it to the website's HTTPS domain, and adapt the browser/server proxy configuration to your own Google project. Do not expose a server-only key in browser code.

## 4. GCash

Set the shop's receiving name and number/QR details in owner settings. Buyers transfer manually and submit the GCash reference; the order remains pending until the owner verifies it. For automatic in-site confirmation, you must separately choose and configure a GCash-capable payment provider/merchant account and implement its checkout plus signed webhook. No card or wallet credentials are included in this code.

## 5. Publish and connect a domain

1. Save the completed source as a project checkpoint on the canonical `main` branch.
2. In the website Dashboard, choose **Publish** for that checkpoint and wait for the container health check to succeed. The deployment uses Java 21, the project Dockerfile, port `3000` (or the platform `PORT`), and `/api/health`.
3. Set website visibility to **Public** in the project/Dashboard access settings. Application customer login is separate: users still need Google or Facebook.
4. In project domain settings, attach a domain you own (buy one if needed), follow its DNS instructions, and wait for HTTPS certificate provisioning. Do not put OAuth callbacks into provider consoles until the final public HTTPS hostname is known.
5. Add the final HTTPS domain and callback URLs to Google/Facebook, then set the OAuth secrets for the **production** environment. Republish if the platform requires the new runtime settings to load.
6. Run one small end-to-end test: customer social sign-in, saved phone/address, map lookup, far-distance quote, COD order, GCash transfer/reference, owner verification, and no-refund acknowledgment.

For a local VS Code preview, use JDK 21 and Maven, set `SESSION_COOKIE_SECURE=false` only for plain local HTTP, then run `mvn spring-boot:run`. Use secure session cookies and HTTPS for any public launch. The managed development and public application share the same managed database, so test orders are real stored records; remove only test orders through the owner workflow if cleanup is needed.
