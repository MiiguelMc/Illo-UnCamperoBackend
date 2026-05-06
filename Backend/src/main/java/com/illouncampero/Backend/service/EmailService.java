package com.illouncampero.Backend.service;

import com.illouncampero.Backend.model.LineaPedido;
import com.illouncampero.Backend.model.Pedido;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarBienvenida(String destinatario, String nombre) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(destinatario);
            helper.setSubject("Bienvenido a Illo, Un Campero");
            helper.setText(plantillaBienvenida(nombre), true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Error al enviar email de bienvenida: " + e.getMessage());
        }
    }

    @Async
    public void enviarConfirmacionPedido(String destinatario, String nombre, Pedido pedido) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(destinatario);
            helper.setSubject("Confirmación de pedido #" + pedido.getId().substring(0, 8).toUpperCase());
            helper.setText(plantillaConfirmacionPedido(nombre, pedido), true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Error al enviar email de confirmacion: " + e.getMessage());
        }
    }

    private String plantillaBienvenida(String nombre) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;color:#222">
              <h2 style="color:#c0392b">Bienvenido a Illo, Un Campero</h2>
              <p>Hola <strong>%s</strong>,</p>
              <p>Tu cuenta ha sido creada correctamente. Ya puedes hacer tus pedidos desde nuestra web.</p>
              <p style="margin-top:32px;color:#888;font-size:12px">
                Illo, Un Campero &middot; Si no creaste esta cuenta, ignora este mensaje.
              </p>
            </div>
            """.formatted(nombre);
    }

    private String plantillaConfirmacionPedido(String nombre, Pedido pedido) {
        String idCorto = pedido.getId().substring(0, 8).toUpperCase();
        StringBuilder lineas = new StringBuilder();
        for (LineaPedido lp : pedido.getProductos()) {
            lineas.append("<tr>")
                  .append("<td style='padding:6px 0'>").append(lp.getCantidad()).append("x ").append(lp.getNombre()).append("</td>")
                  .append("<td style='padding:6px 0;text-align:right'>")
                  .append(String.format("%.2f", lp.getPrecioUnidad() * lp.getCantidad())).append("&euro;")
                  .append("</td>")
                  .append("</tr>");
        }

        String descuentoFila = "";
        if (pedido.getDescuento() != null && pedido.getDescuento() > 0) {
            descuentoFila = "<tr><td style='color:#27ae60'>Descuento</td><td style='color:#27ae60;text-align:right'>-"
                + String.format("%.2f", pedido.getDescuento()) + "&euro;</td></tr>";
        }

        return """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;color:#222">
              <h2 style="color:#c0392b">Pedido confirmado #%s</h2>
              <p>Hola <strong>%s</strong>, hemos recibido tu pedido.</p>
              <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                %s
                %s
                <tr style="border-top:2px solid #eee">
                  <td style="padding-top:8px"><strong>Total</strong></td>
                  <td style="padding-top:8px;text-align:right"><strong>%.2f&euro;</strong></td>
                </tr>
              </table>
              <p><strong>Método de pago:</strong> %s</p>
              %s
              <p style="margin-top:32px;color:#888;font-size:12px">Illo, Un Campero &middot; Gracias por tu pedido.</p>
            </div>
            """.formatted(
                idCorto,
                nombre,
                lineas.toString(),
                descuentoFila,
                pedido.getTotal(),
                pedido.getMetodoPago(),
                pedido.getDireccion() != null && !pedido.getDireccion().isEmpty()
                    ? "<p><strong>Dirección de entrega:</strong> " + pedido.getDireccion() + "</p>"
                    : ""
            );
    }
}
