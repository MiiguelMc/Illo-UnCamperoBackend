package com.illouncampero.Backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.illouncampero.Backend.model.PushSubscription;
import com.illouncampero.Backend.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.util.Map;

/**
 * Envio de notificaciones mediante Web Push (VAPID), sin Firebase/FCM.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final String vapidPublicKey;
    private final String vapidPrivateKey;
    private final String vapidSubject;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PushService pushService;

    public NotificacionService(
            @Value("${vapid.public-key}") String vapidPublicKey,
            @Value("${vapid.private-key}") String vapidPrivateKey,
            @Value("${vapid.subject}") String vapidSubject,
            PushSubscriptionRepository pushSubscriptionRepository) {
        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = vapidPrivateKey;
        this.vapidSubject = vapidSubject;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    @PostConstruct
    void init() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
    }

    @Transactional
    public void enviarNotificacion(PushSubscription sub, String titulo, String cuerpo) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "title", titulo,
                    "body", cuerpo
            ));

            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            var response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();

            // 404/410: la suscripcion ya no es valida -> se elimina.
            if (status == 404 || status == 410) {
                pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
            } else if (status >= 300) {
                log.warn("Web Push respondio {} para endpoint {}", status, sub.getEndpoint());
            }
        } catch (Exception e) {
            log.error("Error al enviar Web Push: {}", e.getMessage(), e);
        }
    }
}
