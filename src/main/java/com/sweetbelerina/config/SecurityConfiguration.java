package com.sweetbelerina.config;

import com.sweetbelerina.auth.OAuthLoginSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain bakerySecurity(HttpSecurity http,
                                      ObjectProvider<ClientRegistrationRepository> registrations,
                                      OAuthLoginSuccessHandler successHandler) throws Exception {
        HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
        csrfRepository.setHeaderName("X-CSRF-TOKEN");

        http
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/app.css", "/app.js", "/admin.html", "/admin.css", "/admin.js", "/manus-routes.json", "/images/**", "/api/config", "/api/store", "/api/products", "/api/session", "/api/csrf", "/api/health", "/oauth2/**", "/login/**", "/error").permitAll()
                .requestMatchers("/api/orders/**", "/api/profile/**", "/api/admin/**", "/api/delivery/quote").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                (request, response, exception) -> response.sendError(401, "Sign in with Google or Facebook before ordering."),
                new AntPathRequestMatcher("/api/**")))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("SWEET_BELERINA_SESSION"));

        if (registrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth.successHandler(successHandler));
        }
        return http.build();
    }
}
