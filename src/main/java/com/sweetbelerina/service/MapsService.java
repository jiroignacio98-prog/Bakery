package com.sweetbelerina.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MapsService {
    public record Distance(double kilometers, String source) {}

    private final String apiUrl;
    private final String serverKey;
    private final RestClient client = RestClient.builder().build();

    public MapsService(@Value("${app.maps.api-url:}") String apiUrl,
                       @Value("${app.maps.server-key:}") String serverKey) {
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
        this.serverKey = serverKey == null ? "" : serverKey.trim();
    }

    public Distance drivingDistance(double originLat, double originLng, double destinationLat, double destinationLng) {
        if (!apiUrl.isBlank() && !serverKey.isBlank()) {
            try {
                URI uri = UriComponentsBuilder.fromUriString(apiUrl.replaceAll("/$", "") + "/v1/maps/proxy/maps/api/directions/json")
                        .queryParam("key", serverKey)
                        .queryParam("origin", originLat + "," + originLng)
                        .queryParam("destination", destinationLat + "," + destinationLng)
                        .queryParam("mode", "driving")
                        .queryParam("units", "metric")
                        .build().encode().toUri();
                @SuppressWarnings("unchecked") Map<String, Object> response = client.get().uri(uri).retrieve().body(Map.class);
                if (response != null && "OK".equals(response.get("status"))) {
                    List<?> routes = (List<?>) response.get("routes");
                    if (routes != null && !routes.isEmpty()) {
                        Map<?, ?> route = (Map<?, ?>) routes.get(0);
                        List<?> legs = (List<?>) route.get("legs");
                        if (legs != null && !legs.isEmpty()) {
                            Map<?, ?> leg = (Map<?, ?>) legs.get(0);
                            Map<?, ?> distance = (Map<?, ?>) leg.get("distance");
                            if (distance != null && distance.get("value") instanceof Number meters) {
                                return new Distance(meters.doubleValue() / 1000.0, "google_maps_driving");
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Do not disclose provider response details to customers; use a conservative estimate below.
            }
        }
        return new Distance(haversineKm(originLat, originLng, destinationLat, destinationLng), "straight_line_estimate");
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
