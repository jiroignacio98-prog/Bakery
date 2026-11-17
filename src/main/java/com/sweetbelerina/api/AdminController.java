package com.sweetbelerina.api;

import com.sweetbelerina.service.OrderService;
import com.sweetbelerina.service.StoreService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final StoreService store;
    private final OrderService orders;

    public AdminController(StoreService store, OrderService orders) {
        this.store = store;
        this.orders = orders;
    }

    @GetMapping("/settings")
    public Map<String, String> settings(Authentication auth) { requireOwner(auth); return store.settings(); }

    @PutMapping("/settings")
    public Map<String, String> saveSettings(Authentication auth, @RequestBody Map<String, Object> body) {
        requireOwner(auth);
        store.saveSettings(body);
        return Map.of("status", "saved");
    }

    @GetMapping("/products")
    public Map<String, Object> products(Authentication auth) { requireOwner(auth); return Map.of("products", store.products(false)); }

    @PostMapping("/products")
    public Map<String, String> saveProduct(Authentication auth, @RequestBody Map<String, Object> body) {
        requireOwner(auth);
        return Map.of("id", store.saveProduct(body));
    }

    @GetMapping("/orders")
    public Map<String, Object> orders(Authentication auth) { requireOwner(auth); return Map.of("orders", orders.allOrders()); }

    @PutMapping("/orders/{code}")
    public Map<String, String> updateOrder(Authentication auth, @PathVariable String code, @RequestBody Map<String, Object> body) {
        requireOwner(auth);
        orders.updateOrder(code, body);
        return Map.of("status", "updated");
    }

    private void requireOwner(Authentication authentication) {
        if (!store.isOwner(authentication)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Owner access is not configured for this signed-in email.");
    }
}
