package com.illouncampero.Backend.service;

import com.illouncampero.Backend.model.Producto;
import com.illouncampero.Backend.model.dto.CrearPedidoRequest;
import com.illouncampero.Backend.model.dto.LineaPedidoRequest;
import com.illouncampero.Backend.repository.PedidoRepository;
import com.illouncampero.Backend.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private ProductoService productoService;
    @Mock private NotificacionService notificacionService;
    @Mock private PushSubscriptionRepository pushSubscriptionRepository;

    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService(pedidoRepository, productoService,
                notificacionService, pushSubscriptionRepository);
    }

    private CrearPedidoRequest requestCon(String productoId, int cantidad) {
        LineaPedidoRequest linea = new LineaPedidoRequest();
        linea.setProductoId(productoId);
        linea.setCantidad(cantidad);

        CrearPedidoRequest request = new CrearPedidoRequest();
        request.setProductos(List.of(linea));
        request.setNombreCliente("Test");
        request.setMetodoPago("EFECTIVO");
        return request;
    }

    @Test
    void realizarPedidoConProductoInexistenteLanzaExcepcion() {
        when(productoService.obtenerPorId("prod-inexistente")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.guardarNuevoPedido(requestCon("prod-inexistente", 1), "uid-test"));
        assertTrue(ex.getMessage().contains("Producto no encontrado"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void realizarPedidoConProductoNoDisponibleLanzaExcepcion() {
        Producto producto = new Producto();
        producto.setId("prod-1");
        producto.setNombre("Burger especial");
        producto.setPrecio(8.50);
        producto.setDisponible(false);
        when(productoService.obtenerPorId("prod-1")).thenReturn(producto);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.guardarNuevoPedido(requestCon("prod-1", 1), "uid-test"));
        assertTrue(ex.getMessage().contains("no está disponible"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void actualizarEstadoConValorInvalidoLanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.actualizarEstado("pedido-1", "INVENTADO"));
        assertTrue(ex.getMessage().contains("Estado no válido"));
    }

    @Test
    void descuentoNoPuedeSuperarElSubtotal() {
        Producto producto = new Producto();
        producto.setId("prod-1");
        producto.setNombre("Burger");
        producto.setPrecio(5.0);
        producto.setDisponible(true);
        when(productoService.obtenerPorId("prod-1")).thenReturn(producto);

        CrearPedidoRequest request = requestCon("prod-1", 1); // subtotal = 5.0
        request.setDescuento(100.0);                          // descuento desproporcionado

        pedidoService.guardarNuevoPedido(request, "uid-test");

        org.mockito.ArgumentCaptor<com.illouncampero.Backend.model.Pedido> captor =
                org.mockito.ArgumentCaptor.forClass(com.illouncampero.Backend.model.Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        // El total nunca puede ser negativo: el descuento se limita al subtotal.
        assertEquals(0.0, captor.getValue().getTotal(), 0.001);
    }
}
