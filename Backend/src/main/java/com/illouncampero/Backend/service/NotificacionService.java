package com.illouncampero.Backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    /**
     * Envía una notificación push a un dispositivo concreto mediante su fcmToken.
     *
     * @param fcmToken Token FCM del dispositivo destino (guardado en Firestore al hacer login)
     * @param titulo   Título que aparece en la notificación
     * @param cuerpo   Texto del cuerpo de la notificación
     */
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

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("LOG: Notificación enviada correctamente. ID: " + response);

        } catch (Exception e) {
            // No lanzamos la excepción para que un fallo en la notificación
            // no revierta la actualización de estado del pedido
            System.err.println("ERROR: Fallo al enviar notificación FCM: " + e.getMessage());
        }
    }
}