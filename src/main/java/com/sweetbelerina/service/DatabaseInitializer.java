package com.sweetbelerina.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

@Component
@Order(0)
public class DatabaseInitializer implements ApplicationRunner {
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public DatabaseInitializer(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        seedProducts();
        migrateSeedImages();
        seedSettings();
    }

    private void seedProducts() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        if (count != null && count > 0) return;
        addProduct("banana-loaf", "Banana Loaf", "Golden, tender banana loaf baked in small batches.", "Loaves", "220.00", "/manus-storage/async-images/KX5Evw7mhNqziPjoqd4swG/image-2.webp");
        addProduct("crinkles", "Chocolate Crinkles", "Fudgy cocoa cookies with a snowy sugar finish.", "Cookies", "180.00", "/manus-storage/async-images/KX5Evw7mhNqziPjoqd4swG/image-3.webp");
        addProduct("dream-cake", "Dream Cake", "Silky chocolate layers made for a little celebration.", "Cakes", "650.00", "/manus-storage/async-images/KX5Evw7mhNqziPjoqd4swG/image-4.webp");
        addProduct("ube-cheesecake", "Ube Cheesecake", "A creamy ube swirl over a buttery crumb base.", "Cakes", "520.00", "");
        addProduct("brownies", "Fudge Brownies", "A rich, crackly-top cocoa square for sharing.", "Bars", "280.00", "");
        addProduct("cinnamon-rolls", "Cinnamon Rolls", "Soft rolls with cinnamon sugar and a light glaze.", "Pastries", "300.00", "");
    }

    private void migrateSeedImages() {
        replaceSeedImage("banana-loaf", "/manus-storage/async-images/ez6F61WWuJ2bAbifBv4lR1/image-2.webp", "/manus-storage/async-images/KX5Evw7mhNqziPjoqd4swG/image-2.webp");
        replaceSeedImage("crinkles", "/manus-storage/async-images/ez6F61WWuJ2bAbifBv4lR1/image-3.webp", "/manus-storage/async-images/KX5Evw7mhNqziPjoqd4swG/image-3.webp");
        replaceSeedImage("dream-cake", "/manus-storage/async-images/ez6F61WWuJ2bAbifBv4lR1/image-4.webp", "/manus-storage/async-images/KX5Evw7mhNqziPjoqd4swG/image-4.webp");
    }

    private void replaceSeedImage(String productId, String oldImage, String newImage) {
        jdbc.update("UPDATE products SET image_url = ? WHERE product_id = ? AND image_url = ?", newImage, productId, oldImage);
    }

    private void addProduct(String id, String name, String description, String category, String price, String image) {
        jdbc.update("INSERT INTO products (product_id, name, description, category, price, image_url, is_available) VALUES (?, ?, ?, ?, ?, ?, TRUE)",
                id, name, description, category, new BigDecimal(price), image.isBlank() ? null : image);
    }

    private void seedSettings() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM store_settings", Integer.class);
        if (count != null && count > 0) return;
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("store_name", "SWEET BEL-ERINA");
        defaults.put("store_phone", "");
        defaults.put("store_address", "");
        defaults.put("store_lat", "");
        defaults.put("store_lng", "");
        defaults.put("gcash_name", "");
        defaults.put("gcash_number", "");
        defaults.put("free_delivery_km", "5");
        defaults.put("far_distance_km", "12");
        defaults.put("standard_delivery_fee", "60");
        defaults.put("far_delivery_fee", "120");
        defaults.put("gcash_shipping_discount_pct", "6");
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            jdbc.update("INSERT INTO store_settings (setting_key, setting_value) VALUES (?, ?)", entry.getKey(), entry.getValue());
        }
    }
}
