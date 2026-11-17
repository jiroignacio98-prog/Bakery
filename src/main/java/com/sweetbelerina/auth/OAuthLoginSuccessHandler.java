package com.sweetbelerina.auth;

import java.io.IOException;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JdbcTemplate jdbc;

    public OAuthLoginSuccessHandler(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken token
                && token.getPrincipal() instanceof OAuth2User user) {
            String provider = token.getAuthorizedClientRegistrationId().toLowerCase();
            Map<String, Object> attributes = user.getAttributes();
            String providerId = value(attributes, provider.equals("google") ? "sub" : "id");
            if (providerId.isBlank()) providerId = token.getName();
            String customerId = provider + ":" + providerId;
            String email = value(attributes, "email");
            String name = value(attributes, "name");
            if (name.isBlank()) name = email.isBlank() ? "Bakery customer" : email;
            try {
                jdbc.update("INSERT INTO customers (customer_id, provider, email, full_name) VALUES (?, ?, ?, ?)",
                        customerId, provider, emptyToNull(email), name);
            } catch (org.springframework.dao.DuplicateKeyException duplicate) {
                jdbc.update("UPDATE customers SET email = ?, full_name = ?, updated_at = CURRENT_TIMESTAMP WHERE customer_id = ?",
                        emptyToNull(email), name, customerId);
            }
        }
        response.sendRedirect("/");
    }

    private static String value(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
