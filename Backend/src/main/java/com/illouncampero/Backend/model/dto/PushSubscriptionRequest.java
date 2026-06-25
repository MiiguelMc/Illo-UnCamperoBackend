package com.illouncampero.Backend.model.dto;

import lombok.Data;

/**
 * Estructura que envia el navegador al suscribirse a Web Push:
 * { endpoint, expirationTime, keys: { p256dh, auth } }
 */
@Data
public class PushSubscriptionRequest {
    private String endpoint;
    private Keys keys;

    @Data
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
