package com.illouncampero.Backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    public void enviarNotificacion(String fcmToken, String titulo, String cuerpo) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(
                            Notification.builder()
                                    .setTitle(titulo)
                                    .setBody(cuerpo)
                                    .build()
                    )
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.error("Error al enviar notificación FCM: {}", e.getMessage(), e);
        }
    }
}
