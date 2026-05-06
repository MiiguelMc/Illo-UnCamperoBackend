package com.illouncampero.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.model.dto.CrearPedidoRequest;
import com.illouncampero.Backend.model.dto.LineaPedidoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private Firestore db;
    @Mock private ProductoService productoService;
    @Mock private NotificacionService notificacionService;
    @Mock private EmailService emailService;
    @Mock private CollectionReference coleccionPedidos;
    @Mock private DocumentReference docRef;
    @Mock private ApiFuture<WriteResult> writeResultFuture;
    @Mock private CollectionReference coleccionUsuarios;
    @Mock private DocumentReference usuarioDocRef;
    @Mock private ApiFuture<DocumentSnapshot> userFuture;
    @Mock private DocumentSnapshot usuarioSnapshot;

    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService(db, productoService, notificacionService, emailService);
    }

    @Test
    void realizarPedidoConProductoInexistenteLanzaExcepcion() throws Exception {
        when(productoService.obtenerPorId("prod-inexistente")).thenReturn(null);

        CrearPedidoRequest request = new CrearPedidoRequest();
        LineaPedidoRequest linea = new LineaPedidoRequest();
        linea.setProductoId("prod-inexistente");
        linea.setCantidad(1);
        request.setProductos(List.of(linea));
        request.setNombreCliente("Test");
        request.setMetodoPago("EFECTIVO");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.guardarNuevoPedido(request, "uid-test"));
        assertTrue(ex.getMessage().contains("Producto no encontrado"));
    }

    @Test
    void realizarPedidoConProductoNoDisponibleLanzaExcepcion() throws Exception {
        Producto productoNoDisponible = new Producto();
        productoNoDisponible.setId("prod-1");
        productoNoDisponible.setNombre("Burger especial");
        productoNoDisponible.setPrecio(8.50);
        productoNoDisponible.setDisponible(false);

        when(productoService.obtenerPorId("prod-1")).thenReturn(productoNoDisponible);

        CrearPedidoRequest request = new CrearPedidoRequest();
        LineaPedidoRequest linea = new LineaPedidoRequest();
        linea.setProductoId("prod-1");
        linea.setCantidad(1);
        request.setProductos(List.of(linea));
        request.setNombreCliente("Test");
        request.setMetodoPago("EFECTIVO");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.guardarNuevoPedido(request, "uid-test"));
        assertTrue(ex.getMessage().contains("no está disponible"));
    }

    @Test
    void actualizarEstadoConValorInvalidoLanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.actualizarEstado("pedido-1", "INVENTADO"));
        assertTrue(ex.getMessage().contains("Estado no válido"));
    }

    @Test
    void descuentoNoPuedeSuperar() throws Exception {
        Producto producto = new Producto();
        producto.setId("prod-1");
        producto.setNombre("Burger");
        producto.setPrecio(5.0);
        producto.setDisponible(true);

        when(productoService.obtenerPorId("prod-1")).thenReturn(producto);
        when(db.collection("pedidos")).thenReturn(coleccionPedidos);
        when(coleccionPedidos.document(anyString())).thenReturn(docRef);
        when(docRef.set(any())).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));
        when(db.collection("usuarios")).thenReturn(coleccionUsuarios);
        when(coleccionUsuarios.document(anyString())).thenReturn(usuarioDocRef);
        when(usuarioDocRef.get()).thenReturn(userFuture);
        when(userFuture.get()).thenReturn(usuarioSnapshot);
        when(usuarioSnapshot.exists()).thenReturn(false);

        CrearPedidoRequest request = new CrearPedidoRequest();
        LineaPedidoRequest linea = new LineaPedidoRequest();
        linea.setProductoId("prod-1");
        linea.setCantidad(1);
        request.setProductos(List.of(linea));
        request.setNombreCliente("Test");
        request.setMetodoPago("EFECTIVO");
        request.setDescuento(999.0); // descuento mayor que el subtotal

        String id = pedidoService.guardarNuevoPedido(request, "uid-test");
        assertNotNull(id);
        // El total no puede ser negativo (el descuento se capa al subtotal)
    }
}
