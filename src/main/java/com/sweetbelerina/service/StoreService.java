package com.sweetbelerina.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {
    private final JdbcTemplate jdbc;
    private final String ownerEmail;
    private final String googleId;
    private final String googleSecret;
    private final String facebookId;
    private final String facebookSecret;
    private final String mapsApiUrl;
    private final String mapsBrowserKey;

    public StoreService(JdbcTemplate jdbc,
                        @Value("${app.owner.email:}") String ownerEmail,
                        @Value("${app.oauth.google-id:}") String googleId,
                        @Value("${app.oauth.google-secret:}") String googleSecret,
                        @Value("${app.oauth.facebook-id:}") String facebookId,
                        @Value("${app.oauth.facebook-secret:}") String facebookSecret,
                        @Value("${app.maps.api-url:}") String mapsApiUrl,
                        @Value("${app.maps.browser-key:}") String mapsBrowserKey) {
        this.jdbc = jdbc;
        this.ownerEmail = ownerEmail == null ? "" : ownerEmail.trim();
        this.googleId = googleId == null ? "" : googleId.trim();
        this.googleSecret = googleSecret == null ? "" : googleSecret.trim();
        this.facebookId = facebookId == null ? "" : facebookId.trim();
        this.facebookSecret = facebookSecret == null ? "" : facebookSecret.trim();
        this.mapsApiUrl = mapsApiUrl == null ? "" : mapsApiUrl.trim();
        this.mapsBrowserKey = mapsBrowserKey == null ? "" : mapsBrowserKey.trim();
    }

    public Map<String, String> settings() {
        Map<String, String> values = new LinkedHashMap<>();
        jdbc.query("SELECT setting_key, setting_value FROM store_settings", rs -> {
            values.put(rs.getString(1), rs.getString(2));
        });
        return values;
    }

    public Map<String, Object> publicConfig() {
        Map<String, String> settings = settings();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("storeName", settings.getOrDefault("store_name", "SWEET BEL-ERINA"));
        config.put("storePhone", settings.getOrDefault("store_phone", ""));
        config.put("gcashName", settings.getOrDefault("gcash_name", ""));
        config.put("gcashNumber", settings.getOrDefault("gcash_number", ""));
        config.put("shippingDiscountPct", decimal(settings.get("gcash_shipping_discount_pct"), 6));
        config.put("providers", Map.of(
                "google", !googleId.isBlank() && !googleSecret.isBlank(),
                "facebook", !facebookId.isBlank() && !facebookSecret.isBlank()));
        config.put("mapsEnabled", !mapsApiUrl.isBlank() && !mapsBrowserKey.isBlank());
        config.put("mapsApiUrl", mapsApiUrl);
        config.put("mapsBrowserKey", mapsBrowserKey);
        return config;
    }

    public List<Map<String, Object>> products(boolean availableOnly) {
        String sql = "SELECT product_id, name, description, category, price, image_url, is_available FROM products"
                + (availableOnly ? " WHERE is_available = TRUE" : "") + " ORDER BY category, name";
        return jdbc.query(sql, (rs, row) -> {
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("id", rs.getString("product_id"));
            product.put("name", rs.getString("name"));
            product.put("description", rs.getString("description"));
            product.put("category", rs.getString("category"));
            product.put("price", rs.getBigDecimal("price"));
            product.put("imageUrl", rs.getString("image_url"));
            product.put("available", rs.getBoolean("is_available"));
            return product;
        });
    }

    public Map<String, Object> product(String id) {
        return jdbc.queryForObject("SELECT product_id, name, description, category, price, image_url, is_available FROM products WHERE product_id = ?",
                (rs, row) -> {
                    Map<String, Object> product = new LinkedHashMap<>();
                    product.put("id", rs.getString("product_id"));
                    product.put("name", rs.getString("name"));
                    product.put("description", rs.getString("description"));
                    product.put("category", rs.getString("category"));
                    product.put("price", rs.getBigDecimal("price"));
                    product.put("imageUrl", rs.getString("image_url"));
                    product.put("available", rs.getBoolean("is_available"));
                    return product;
                }, id);
    }

    @Transactional
    public String saveProduct(Map<String, Object> input) {
        String id = text(input.get("id"));
        if (id.isBlank()) id = UUID.randomUUID().toString().substring(0, 12);
        String name = required(input.get("name"), "Product name is required.", 120);
        String description = required(input.get("description"), "Product description is required.", 500);
        String category = required(input.get("category"), "Product category is required.", 60);
        BigDecimal price = money(input.get("price"));
        String image = text(input.get("imageUrl"));
        boolean available = !Boolean.FALSE.equals(input.get("available"));
        if (price.signum() < 0) throw new IllegalArgumentException("Product price cannot be negative.");
        try {
            jdbc.update("INSERT INTO products (product_id, name, description, category, price, image_url, is_available) VALUES (?, ?, ?, ?, ?, ?, ?)", id, name, description, category, price, emptyToNull(image), available);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            jdbc.update("UPDATE products SET name = ?, description = ?, category = ?, price = ?, image_url = ?, is_available = ? WHERE product_id = ?",
                    name, description, category, price, emptyToNull(image), available, id);
        }
        return id;
    }

    @Transactional
    public void saveSettings(Map<String, Object> input) {
        putSetting("store_name", required(input.get("storeName"), "Store name is required.", 100));
        putSetting("store_phone", clipped(input.get("storePhone"), 40));
        putSetting("store_address", clipped(input.get("storeAddress"), 500));
        putSetting("store_lat", clipped(input.get("storeLat"), 40));
        putSetting("store_lng", clipped(input.get("storeLng"), 40));
        putSetting("gcash_name", clipped(input.get("gcashName"), 120));
        putSetting("gcash_number", clipped(input.get("gcashNumber"), 40));
        putSetting("free_delivery_km", numberSetting(input.get("freeDeliveryKm"), 5, 0, 200));
        putSetting("far_distance_km", numberSetting(input.get("farDistanceKm"), 12, 0, 500));
        putSetting("standard_delivery_fee", numberSetting(input.get("standardDeliveryFee"), 60, 0, 100000));
        putSetting("far_delivery_fee", numberSetting(input.get("farDeliveryFee"), 120, 0, 100000));
        double discount = parseNumber(input.get("gcashShippingDiscountPct"), 6);
        if (discount < 5 || discount > 7) throw new IllegalArgumentException("GCash shipping discount must be between 5% and 7%.");
        putSetting("gcash_shipping_discount_pct", BigDecimal.valueOf(discount).stripTrailingZeros().toPlainString());
    }

    public boolean isOwner(Authentication authentication) {
        if (ownerEmail.isBlank() || authentication == null || !(authentication.getPrincipal() instanceof OAuth2User user)) return false;
        Object email = user.getAttributes().get("email");
        return email != null && ownerEmail.equalsIgnoreCase(email.toString().trim());
    }

    private void putSetting(String key, String value) {
        try {
            jdbc.update("INSERT INTO store_settings (setting_key, setting_value) VALUES (?, ?)", key, value == null ? "" : value);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            jdbc.update("UPDATE store_settings SET setting_value = ? WHERE setting_key = ?", value == null ? "" : value, key);
        }
    }

    private static String numberSetting(Object value, double fallback, double min, double max) {
        double parsed = parseNumber(value, fallback);
        if (parsed < min || parsed > max) throw new IllegalArgumentException("A delivery setting is outside the allowed range.");
        return BigDecimal.valueOf(parsed).stripTrailingZeros().toPlainString();
    }

    private static double parseNumber(Object value, double fallback) {
        try { return value == null ? fallback : Double.parseDouble(value.toString()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Please enter a valid number."); }
    }

    private static BigDecimal decimal(String value, double fallback) {
        try { return new BigDecimal(value == null ? String.valueOf(fallback) : value); }
        catch (NumberFormatException ex) { return BigDecimal.valueOf(fallback); }
    }

    private static BigDecimal money(Object value) {
        try { return new BigDecimal(value == null ? "0" : value.toString()).setScale(2, java.math.RoundingMode.HALF_UP); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Please enter a valid product price."); }
    }

    private static String required(Object value, String message, int max) {
        String result = clipped(value, max);
        if (result.isBlank()) throw new IllegalArgumentException(message);
        return result;
    }

    private static String clipped(Object value, int max) {
        String result = text(value);
        return result.length() > max ? result.substring(0, max) : result;
    }

    private static String text(Object value) { return value == null ? "" : value.toString().trim(); }
    private static String emptyToNull(String value) { return value.isBlank() ? null : value; }
}
