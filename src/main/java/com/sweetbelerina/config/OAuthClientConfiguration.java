package com.sweetbelerina.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class OAuthClientConfiguration {
    @Bean
    @ConditionalOnExpression("('${app.oauth.google-id:}' != '' and '${app.oauth.google-secret:}' != '') or ('${app.oauth.facebook-id:}' != '' and '${app.oauth.facebook-secret:}' != '')")
    ClientRegistrationRepository clientRegistrationRepository(Environment env) {
        List<ClientRegistration> registrations = new ArrayList<>();
        String googleId = env.getProperty("app.oauth.google-id", "").trim();
        String googleSecret = env.getProperty("app.oauth.google-secret", "").trim();
        if (!googleId.isBlank() && !googleSecret.isBlank()) {
            registrations.add(ClientRegistration.withRegistrationId("google")
                    .clientId(googleId)
                    .clientSecret(googleSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://oauth2.googleapis.com/token")
                    .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                    .userNameAttributeName("sub")
                    .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                    .issuerUri("https://accounts.google.com")
                    .clientName("Google")
                    .build());
        }

        String facebookId = env.getProperty("app.oauth.facebook-id", "").trim();
        String facebookSecret = env.getProperty("app.oauth.facebook-secret", "").trim();
        if (!facebookId.isBlank() && !facebookSecret.isBlank()) {
            registrations.add(ClientRegistration.withRegistrationId("facebook")
                    .clientId(facebookId)
                    .clientSecret(facebookSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("email", "public_profile")
                    .authorizationUri("https://www.facebook.com/dialog/oauth")
                    .tokenUri("https://graph.facebook.com/oauth/access_token")
                    .userInfoUri("https://graph.facebook.com/me?fields=id,name,email")
                    .userNameAttributeName("id")
                    .clientName("Facebook")
                    .build());
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }
}
