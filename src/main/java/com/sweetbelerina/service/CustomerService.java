package com.sweetbelerina.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final JdbcTemplate jdbc;

    public CustomerService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public String customerId(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token) || !(token.getPrincipal() instanceof OAuth2User)) {
            throw new IllegalStateException("Please sign in with Google or Facebook first.");
        }
        return token.getAuthorizedClientRegistrationId().toLowerCase() + ":" + token.getName();
    }

    public Map<String, Object> currentProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) return Map.of("authenticated", false);
        String id = customerId(authentication);
        Map<String, Object> result = jdbc.query("SELECT customer_id, provider, email, full_name, phone, delivery_address, address_lat, address_lng FROM customers WHERE customer_id = ?",
                rs -> {
                    if (!rs.next()) return null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("customerId", rs.getString("customer_id"));
                    row.put("provider", rs.getString("provider"));
                    row.put("email", rs.getString("email"));
                    row.put("name", rs.getString("full_name"));
                    row.put("phone", rs.getString("phone"));
                    row.put("address", rs.getString("delivery_address"));
                    row.put("lat", (Double) rs.getObject("address_lat"));
                    row.put("lng", (Double) rs.getObject("address_lng"));
                    return row;
                }, id);
        if (result == null) return Map.of("authenticated", true, "customerId", id);
        result.put("authenticated", true);
        return result;
    }

    @Transactional
    public void saveProfile(Authentication authentication, Map<String, Object> profile) {
        String id = customerId(authentication);
        String name = text(profile.get("name"));
        String phone = text(profile.get("phone"));
        String address = text(profile.get("address"));
        if (name.isBlank() || name.length() > 160) throw new IllegalArgumentException("Enter your name.");
        if (phone.length() < 7 || phone.length() > 40) throw new IllegalArgumentException("Enter a valid phone number.");
        if (address.isBlank() || address.length() > 1000) throw new IllegalArgumentException("Enter your delivery address.");
        Double lat = numberOrNull(profile.get("lat"));
        Double lng = numberOrNull(profile.get("lng"));
        jdbc.update("UPDATE customers SET full_name = ?, phone = ?, delivery_address = ?, address_lat = ?, address_lng = ?, updated_at = CURRENT_TIMESTAMP WHERE customer_id = ?",
                name, phone, address, lat, lng, id);
    }

    private static String text(Object value) { return value == null ? "" : value.toString().trim(); }
    private static Double numberOrNull(Object value) {
        try { return value == null || value.toString().isBlank() ? null : Double.valueOf(value.toString()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Please select an address from the map or check the coordinates."); }
    }
}
