package com.sweetbelerina;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class BakeryApplication {
    public static void main(String[] args) {
        SpringApplication.run(BakeryApplication.class, args);
    }

    @Bean
    DataSource dataSource(Environment environment,
                          @Value("${spring.datasource.url}") String configuredUrl,
                          @Value("${spring.datasource.username:}") String configuredUsername,
                          @Value("${spring.datasource.password:}") String configuredPassword,
                          @Value("${app.db.ssl-mode:VERIFY_IDENTITY}") String sslMode) {
        String rawUrl = environment.getProperty("DATABASE_URL", "").trim();
        String url = normalizeJdbcUrl(rawUrl.isBlank() ? configuredUrl : rawUrl, sslMode);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setPoolName("sweet-bel-erina-db");
        config.setConnectionTimeout(10000);
        config.setInitializationFailTimeout(10000);
        if (url.startsWith("jdbc:mysql:")) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            String username = environment.getProperty("DATABASE_USERNAME", "").trim();
            String password = environment.getProperty("DATABASE_PASSWORD", "");
            if ((username.isBlank() || password.isBlank()) && rawUrl.startsWith("mysql://")) {
                String[] embedded = embeddedCredentials(rawUrl);
                if (username.isBlank()) username = embedded[0];
                if (password.isBlank()) password = embedded[1];
            }
            if (username.isBlank()) username = configuredUsername == null ? "" : configuredUsername.trim();
            if (password.isBlank()) password = configuredPassword == null ? "" : configuredPassword;
            if (!username.isBlank()) config.setUsername(username);
            if (!password.isBlank()) config.setPassword(password);
        } else if (url.startsWith("jdbc:h2:")) {
            config.setDriverClassName("org.h2.Driver");
            String username = environment.getProperty("DATABASE_USERNAME", "sa");
            config.setUsername(username == null || username.isBlank() ? "sa" : username);
            config.setPassword(environment.getProperty("DATABASE_PASSWORD", ""));
        }
        return new HikariDataSource(config);
    }

    static String normalizeJdbcUrl(String source, String sslMode) {
        if (source == null || source.isBlank()) {
            return "jdbc:h2:mem:sweetbel;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        }
        String raw = source.trim();
        if (raw.startsWith("jdbc:mysql:") || raw.startsWith("jdbc:h2:")) return raw;
        int schemeEnd = raw.indexOf("://");
        if (schemeEnd < 0 || !(raw.startsWith("mysql://") || raw.startsWith("mariadb://"))) {
            throw new IllegalArgumentException("DATABASE_URL must be a MySQL URL or JDBC URL.");
        }

        String remainder = raw.substring(schemeEnd + 3);
        int queryStart = remainder.indexOf('?');
        String location = queryStart < 0 ? remainder : remainder.substring(0, queryStart);
        String rawQuery = queryStart < 0 ? "" : remainder.substring(queryStart + 1);
        int slash = location.indexOf('/');
        String authority = slash < 0 ? location : location.substring(0, slash);
        String path = slash < 0 ? "" : location.substring(slash);
        int userInfo = authority.lastIndexOf('@');
        String hostPort = userInfo < 0 ? authority : authority.substring(userInfo + 1);
        if (hostPort.isBlank()) throw new IllegalArgumentException("DATABASE_URL must include a database host.");

        String host;
        String port = "";
        if (hostPort.startsWith("[")) {
            int closingBracket = hostPort.indexOf(']');
            if (closingBracket < 0) throw new IllegalArgumentException("DATABASE_URL contains an invalid database host.");
            host = hostPort.substring(0, closingBracket + 1);
            String remainderPort = hostPort.substring(closingBracket + 1);
            if (!remainderPort.isBlank()) {
                if (!remainderPort.matches(":\\d{1,5}")) throw new IllegalArgumentException("DATABASE_URL contains an invalid database port.");
                port = remainderPort;
            }
        } else {
            int colon = hostPort.lastIndexOf(':');
            if (colon > 0 && hostPort.substring(colon + 1).matches("\\d{1,5}")) {
                host = hostPort.substring(0, colon);
                port = hostPort.substring(colon);
            } else {
                host = hostPort;
            }
        }
        if (host.isBlank()) throw new IllegalArgumentException("DATABASE_URL must include a database host.");

        List<String> query = new ArrayList<>();
        if (!rawQuery.isBlank()) {
            for (String parameter : rawQuery.split("&")) {
                if (parameter.isBlank()) continue;
                int equals = parameter.indexOf('=');
                String name = (equals < 0 ? parameter : parameter.substring(0, equals)).trim().toLowerCase();
                if (name.equals("ssl") || name.equals("sslmode") || name.equals("ssl-mode")) continue;
                query.add(parameter);
            }
        }
        String normalizedSsl = sslMode == null || sslMode.isBlank() ? "REQUIRED" : sslMode.trim().toUpperCase();
        if (!List.of("DISABLED", "PREFERRED", "REQUIRED", "VERIFY_CA", "VERIFY_IDENTITY").contains(normalizedSsl)) {
            throw new IllegalArgumentException("DB_SSL_MODE must be a supported MySQL SSL mode.");
        }
        query.add("sslMode=" + normalizedSsl);
        return "jdbc:mysql://" + host + port + path + "?" + String.join("&", query);
    }

    private static String[] embeddedCredentials(String rawUrl) {
        int schemeEnd = rawUrl.indexOf("://");
        if (schemeEnd < 0) return new String[]{"", ""};
        String remainder = rawUrl.substring(schemeEnd + 3);
        int locationEnd = remainder.length();
        int slash = remainder.indexOf('/');
        int query = remainder.indexOf('?');
        if (slash >= 0) locationEnd = Math.min(locationEnd, slash);
        if (query >= 0) locationEnd = Math.min(locationEnd, query);
        String authority = remainder.substring(0, locationEnd);
        int at = authority.lastIndexOf('@');
        if (at < 0) return new String[]{"", ""};
        String userInfo = authority.substring(0, at);
        int divider = userInfo.indexOf(':');
        String user = divider < 0 ? userInfo : userInfo.substring(0, divider);
        String password = divider < 0 ? "" : userInfo.substring(divider + 1);
        return new String[]{percentDecode(user), percentDecode(password)};
    }

    private static String percentDecode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
