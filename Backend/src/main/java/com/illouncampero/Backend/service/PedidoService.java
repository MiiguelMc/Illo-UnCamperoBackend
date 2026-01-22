package com.illouncampero.Backend.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.illouncampero.Backend.model.Pedido;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PedidoService {

    /**
     * Esta función recibe el pedido del móvil, le añade la información de seguridad
     * (ID, fecha y estado inicial) y lo guarda en Firebase.
     */
    public String guardarNuevoPedido(Pedido pedido) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        // 1. Generamos un ID único para el pedido (formato UUID)
        String idGenerado = UUID.randomUUID().toString();
        pedido.setId(idGenerado);

        // 2. IMPORTANTE: Forzamos el estado a PENDIENTE.
        // No dejamos que el móvil decida el estado por seguridad.
        pedido.setEstado("PENDIENTE");

        // 3. Registramos la fecha y hora exacta del servidor (en milisegundos)
        pedido.setFecha(System.currentTimeMillis());

        // 4. Guardamos el objeto en la colección "pedidos" usando el ID generado
        db.collection("pedidos").document(idGenerado).set(pedido);

        // Devolvemos el ID por si el móvil quiere guardarlo para hacer seguimiento
        return idGenerado;
    }
}