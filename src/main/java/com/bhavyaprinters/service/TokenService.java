package com.bhavyaprinters.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Generates simple Base64-encoded tokens that match the original
 * Node.js implementation: Base64( JSON({ id, role, ts }) )
 */
@Service
public class TokenService {

    public String generateToken(long id, String role) {
        String payload = String.format("{\"id\":%d,\"role\":\"%s\",\"ts\":%d}", id, role, System.currentTimeMillis());
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
