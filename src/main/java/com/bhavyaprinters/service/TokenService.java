package com.bhavyaprinters.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Generates simple Base64-encoded tokens that match the original
 * Node.js implementation: Base64( JSON({ id, role, ts }) )
 */
@Service
public class TokenService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateToken(long id, String role) {
        String payload = String.format("{\"id\":%d,\"role\":\"%s\",\"ts\":%d}", id, role, System.currentTimeMillis());
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public record DecodedToken(long id, String role) {}

    /**
     * Decodes a token issued by generateToken(). Throws IllegalArgumentException
     * if the token is malformed. See class-level note on the limits of this scheme.
     */
    @SuppressWarnings("unchecked")
    public DecodedToken decodeToken(String token) {
        try {
            String json = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            long id = ((Number) map.get("id")).longValue();
            String role = (String) map.get("role");
            return new DecodedToken(id, role);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }
}
