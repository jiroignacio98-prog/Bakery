package com.sweetbelerina.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    public record ItemInput(String productId, Integer quantity) {}
    public record QuoteInput(List<ItemInput> items, String paymentMethod, Double destinationLat, Double destinationLng) {}
    public record PlaceOrderInput(List<ItemInput> items, String paymentMethod, String name, String phone,
                                  String address, Double destinationLat, Double destinationLng,
                                  String fulfillmentAt, Boolean noRefundAck) {}

    private final JdbcTemplate jdbc;
    private final StoreService store;
    private final MapsService maps;

    public OrderService(JdbcTemplate jdbc, StoreService store, MapsService maps) {
        this.jdbc = jdbc;
        this.store = store;
        this.maps = maps;
    }

    public Map<String, Object> quote(QuoteInput input) {
        if (input == null || input.items() == null || input.items().isEmpty()) throw new IllegalArgumentException("Add at least one product to your basket.");
        if (input.destinationLat() == null || input.destinationLng() == null) throw new IllegalArgumentException("Choose your delivery address from Google Maps so we can estimate delivery distance.");
        if (Math.abs(input.destinationLat()) > 90 || Math.abs(input.destinationLng()) > 180) throw new IllegalArgumentException("Choose a valid address from Google Maps.");
        String method = normalizePayment(input.paymentMethod());
        Map<String, String> settings = store.settings();
        double originLat = settingCoordinate(settings, "store_lat");
        double originLng = settingCoordinate(settings, "store_lng");
        MapsService.Distance route = maps.drivingDistance(originLat, originLng, input.destinationLat(), input.destinationLng());
        BigDecimal subtotal = subtotal(input.items());
        double distanceKm = route.kilometers();
        double freeKm = settingNumber(settings, "free_delivery_km", 5);
        double farKm = settingNumber(settings, "far_distance_km", 12);
        BigDecimal standardFee = settingMoney(settings, "standard_delivery_fee", 60);
        BigDecimal farFee = settingMoney(settings, "far_delivery_fee", 120);
        BigDecimal fee = distanceKm >= farKm ? farFee : (distanceKm > freeKm ? standardFee : BigDecimal.ZERO);
        double discountPercent = settingNumber(settings, "gcash_shipping_discount_pct", 6);
        BigDecimal discount = method.equals("GCASH") ? fee.multiply(BigDecimal.valueOf(discountPercent / 100.0)).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(fee).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subtotal", subtotal);
        result.put("distanceKm", BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP));
        result.put("distanceSource", route.source());
        result.put("deliveryFee", fee);
        result.put("shippingDiscount", discount);
        result.put("discountPercent", method.equals("GCASH") ? discountPercent : 0);
        result.put("paymentMethod", method);
        result.put("total", total);
        result.put("currency", "PHP");
        return result;
    }

    @Transactional
    public Map<String, Object> placeOrder(String customerId, PlaceOrderInput input) {
        if (input == null || !Boolean.TRUE.equals(input.noRefundAck())) {
            throw new IllegalArgumentException("Please acknowledge the no-refund policy before placing your order.");
        }
        String name = required(input.name(), "Enter your name.", 160);
        String phone = required(input.phone(), "Enter your phone number.", 40);
        String address = required(input.address(), "Enter your delivery address.", 1000);
        LocalDateTime fulfillment;
        try { fulfillment = LocalDateTime.parse(input.fulfillmentAt()); }
        catch (DateTimeParseException | NullPointerException ex) { throw new IllegalArgumentException("Choose a valid reservation date and time."); }
        if (fulfillment.isBefore(LocalDateTime.now().plusMinutes(15))) throw new IllegalArgumentException("Choose a reservation time at least 15 minutes from now.");

        QuoteInput quoteInput = new QuoteInput(input.items(), input.paymentMethod(), input.destinationLat(), input.destinationLng());
        Map<String, Object> quote = quote(quoteInput);
        String paymentMethod = quote.get("paymentMethod").toString();
        String orderCode = "SB-" + java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmm").format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String paymentStatus = paymentMethod.equals("GCASH") ? "PENDING_REFERENCE" : "DUE_ON_DELIVERY";
        String orderStatus = "RESERVED";
        jdbc.update("INSERT INTO bakery_orders (order_code, customer_id, customer_name, customer_phone, delivery_address, delivery_lat, delivery_lng, fulfillment_at, distance_km, subtotal, delivery_fee, shipping_discount, total, payment_method, payment_status, order_status, no_refund_ack) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                orderCode, customerId, name, phone, address, input.destinationLat(), input.destinationLng(), Timestamp.valueOf(fulfillment),
                quote.get("distanceKm"), quote.get("subtotal"), quote.get("deliveryFee"), quote.get("shippingDiscount"), quote.get("total"),
                paymentMethod, paymentStatus, orderStatus, true);
        Long orderId = jdbc.queryForObject("SELECT order_id FROM bakery_orders WHERE order_code = ?", Long.class, orderCode);
        if (orderId == null) throw new IllegalStateException("The order could not be saved.");
        for (ItemInput item : input.items()) {
            Map<String, Object> product = store.product(item.productId());
            if (!Boolean.TRUE.equals(product.get("available"))) throw new IllegalArgumentException("One of the selected products is no longer available.");
            int quantity = safeQuantity(item.quantity());
            BigDecimal unit = (BigDecimal) product.get("price");
            jdbc.update("INSERT INTO bakery_order_items (order_id, product_id, product_name, quantity, unit_price, line_total) VALUES (?, ?, ?, ?, ?, ?)",
                    orderId, item.productId(), product.get("name"), quantity, unit, unit.multiply(BigDecimal.valueOf(quantity)));
        }
        Map<String, Object> result = new LinkedHashMap<>(quote);
        result.put("orderCode", orderCode);
        result.put("paymentStatus", paymentStatus);
        result.put("orderStatus", orderStatus);
        result.put("fulfillmentAt", fulfillment.toString());
        return result;
    }

    @Transactional
    public void submitGcashReference(String customerId, String orderCode, String reference) {
        String cleaned = required(reference, "Enter your GCash reference number after sending payment.", 100);
        if (cleaned.length() < 4) throw new IllegalArgumentException("Enter a valid GCash reference number.");
        int changed = jdbc.update("UPDATE bakery_orders SET gcash_reference = ?, payment_status = 'PENDING_REVIEW' WHERE order_code = ? AND customer_id = ? AND payment_method = 'GCASH'",
                cleaned, orderCode, customerId);
        if (changed == 0) throw new IllegalArgumentException("That GCash order was not found.");
    }

    public List<Map<String, Object>> customerOrders(String customerId) {
        return orderList("SELECT order_id, order_code, customer_name, customer_phone, delivery_address, fulfillment_at, distance_km, subtotal, delivery_fee, shipping_discount, total, payment_method, payment_status, order_status, gcash_reference, created_at FROM bakery_orders WHERE customer_id = ? ORDER BY created_at DESC", customerId);
    }

    public List<Map<String, Object>> allOrders() {
        return orderList("SELECT order_id, order_code, customer_name, customer_phone, delivery_address, fulfillment_at, distance_km, subtotal, delivery_fee, shipping_discount, total, payment_method, payment_status, order_status, gcash_reference, created_at FROM bakery_orders ORDER BY created_at DESC");
    }

    @Transactional
    public void updateOrder(String orderCode, Map<String, Object> input) {
        String status = String.valueOf(input.getOrDefault("orderStatus", "")).toUpperCase();
        String payment = String.valueOf(input.getOrDefault("paymentStatus", "")).toUpperCase();
        List<String> allowedOrder = List.of("RESERVED", "PREPARING", "READY", "OUT_FOR_DELIVERY", "COMPLETED", "CANCELLED");
        List<String> allowedPayment = List.of("PENDING_REFERENCE", "PENDING_REVIEW", "PAID", "DUE_ON_DELIVERY", "FAILED");
        if (!allowedOrder.contains(status) || !allowedPayment.contains(payment)) throw new IllegalArgumentException("Choose a valid order and payment status.");
        int changed = jdbc.update("UPDATE bakery_orders SET order_status = ?, payment_status = ? WHERE order_code = ?", status, payment, orderCode);
        if (changed == 0) throw new IllegalArgumentException("Order not found.");
    }

    private BigDecimal subtotal(List<ItemInput> items) {
        BigDecimal result = BigDecimal.ZERO;
        for (ItemInput item : items) {
            int quantity = safeQuantity(item.quantity());
            Map<String, Object> product;
            try { product = store.product(item.productId()); }
            catch (org.springframework.dao.EmptyResultDataAccessException ex) { throw new IllegalArgumentException("One of the selected products could not be found."); }
            if (!Boolean.TRUE.equals(product.get("available"))) throw new IllegalArgumentException("One of the selected products is no longer available.");
            result = result.add(((BigDecimal) product.get("price")).multiply(BigDecimal.valueOf(quantity)));
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> orderList(String sql, Object... args) {
        return jdbc.query(sql, rs -> {
            List<Map<String, Object>> orders = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("orderId", rs.getLong("order_id"));
                row.put("orderCode", rs.getString("order_code"));
                row.put("customerName", rs.getString("customer_name"));
                row.put("phone", rs.getString("customer_phone"));
                row.put("address", rs.getString("delivery_address"));
                row.put("fulfillmentAt", rs.getTimestamp("fulfillment_at").toLocalDateTime().toString());
                row.put("distanceKm", rs.getBigDecimal("distance_km"));
                row.put("subtotal", rs.getBigDecimal("subtotal"));
                row.put("deliveryFee", rs.getBigDecimal("delivery_fee"));
                row.put("shippingDiscount", rs.getBigDecimal("shipping_discount"));
                row.put("total", rs.getBigDecimal("total"));
                row.put("paymentMethod", rs.getString("payment_method"));
                row.put("paymentStatus", rs.getString("payment_status"));
                row.put("orderStatus", rs.getString("order_status"));
                row.put("gcashReference", rs.getString("gcash_reference"));
                row.put("createdAt", rs.getTimestamp("created_at").toLocalDateTime().toString());
                row.put("items", jdbc.query("SELECT product_name, quantity, unit_price, line_total FROM bakery_order_items WHERE order_id = ? ORDER BY item_id",
                        (itemRs, index) -> Map.of("name", itemRs.getString("product_name"), "quantity", itemRs.getInt("quantity"), "unitPrice", itemRs.getBigDecimal("unit_price"), "lineTotal", itemRs.getBigDecimal("line_total")), rs.getLong("order_id")));
                orders.add(row);
            }
            return orders;
        }, args);
    }

    private static String normalizePayment(String method) {
        String value = method == null ? "" : method.trim().toUpperCase();
        if (!value.equals("COD") && !value.equals("GCASH")) throw new IllegalArgumentException("Choose Cash on Delivery or GCash.");
        return value;
    }

    private static int safeQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 50) throw new IllegalArgumentException("Each item quantity must be between 1 and 50.");
        return quantity;
    }

    private static double settingCoordinate(Map<String, String> settings, String key) {
        try { return Double.parseDouble(settings.getOrDefault(key, "")); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("The shop owner needs to set the bakery's map location before delivery fees can be calculated."); }
    }

    private static double settingNumber(Map<String, String> settings, String key, double fallback) {
        try { return Double.parseDouble(settings.getOrDefault(key, String.valueOf(fallback))); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private static BigDecimal settingMoney(Map<String, String> settings, String key, double fallback) {
        try { return new BigDecimal(settings.getOrDefault(key, String.valueOf(fallback))).setScale(2, RoundingMode.HALF_UP); }
        catch (NumberFormatException ex) { return BigDecimal.valueOf(fallback).setScale(2, RoundingMode.HALF_UP); }
    }

    private static String required(String value, String message, int max) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isBlank() || cleaned.length() > max) throw new IllegalArgumentException(message);
        return cleaned;
    }
}
