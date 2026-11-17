package com.sweetbelerina.api;

import com.sweetbelerina.service.CustomerService;
import com.sweetbelerina.service.OrderService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final CustomerService customers;
    private final OrderService orders;

    public OrderController(CustomerService customers, OrderService orders) {
        this.customers = customers;
        this.orders = orders;
    }

    @PostMapping("/profile")
    public Map<String, String> saveProfile(Authentication authentication, @RequestBody Map<String, Object> profile) {
        customers.saveProfile(authentication, profile);
        return Map.of("status", "saved");
    }

    @PostMapping("/delivery/quote")
    public Map<String, Object> quote(@RequestBody OrderService.QuoteInput request) { return orders.quote(request); }

    @PostMapping("/orders")
    public Map<String, Object> place(Authentication authentication, @RequestBody OrderService.PlaceOrderInput request) {
        return orders.placeOrder(customers.customerId(authentication), request);
    }

    @GetMapping("/orders")
    public Map<String, Object> myOrders(Authentication authentication) {
        return Map.of("orders", orders.customerOrders(customers.customerId(authentication)));
    }

    @PostMapping("/orders/{code}/gcash-reference")
    public Map<String, String> gcashReference(Authentication authentication, @PathVariable String code,
                                              @RequestBody Map<String, String> request) {
        orders.submitGcashReference(customers.customerId(authentication), code, request.get("reference"));
        return Map.of("status", "pending_review");
    }
}
