package com.sweetbelerina.api;

import com.sweetbelerina.service.CustomerService;
import com.sweetbelerina.service.StoreService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoreController {
    private final StoreService store;
    private final CustomerService customers;

    public StoreController(StoreService store, CustomerService customers) {
        this.store = store;
        this.customers = customers;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/api/config")
    public Map<String, Object> config() { return store.publicConfig(); }

    @GetMapping("/api/store")
    public Map<String, Object> publicStore() { return store.publicConfig(); }

    @GetMapping("/api/products")
    public Map<String, Object> products() { return Map.of("products", store.products(true)); }

    @GetMapping("/api/session")
    public Map<String, Object> session(Authentication authentication) { return customers.currentProfile(authentication); }

    @GetMapping("/api/csrf")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }
}
