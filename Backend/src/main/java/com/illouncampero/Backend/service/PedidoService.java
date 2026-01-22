package com.illouncampero.Backend.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.illouncampero.Backend.model.Pedido;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PedidoService {

    public String crearPedido(Pedido pedido) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        pedido.setId(UUID.randomUUID().toString());
        pedido.setEstado("PENDIENTE");
        pedido.setFecha(System.currentTimeMillis());

        db.collection("pedidos").document(pedido.getId()).set(pedido);
        return pedido.getId();
    }
}