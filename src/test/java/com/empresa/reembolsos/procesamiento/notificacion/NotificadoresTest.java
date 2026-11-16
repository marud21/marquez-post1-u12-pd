package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class NotificadoresTest {

    private SolicitudReembolso solicitudEn(EstadoSolicitud estado) {
        SolicitudReembolso s = new SolicitudReembolso("E-7", CategoriaGasto.VIAJE, new BigDecimal("100000"),
                LocalDate.of(2026, 11, 3), 1, true);
        s.setId(7L);
        s.setMontoAprobado(new BigDecimal("100000"));
        s.setEstado(estado);
        return s;
    }

    @Test
    void contabilidadSoloReaccionaALasAprobaciones(CapturedOutput salida) {
        NotificadorContabilidad contabilidad = new NotificadorContabilidad();
        contabilidad.onCambioEstado(solicitudEn(EstadoSolicitud.RECHAZADA), EstadoSolicitud.PENDIENTE);
        assertFalse(salida.getOut().contains("programar pago"));
        contabilidad.onCambioEstado(solicitudEn(EstadoSolicitud.APROBADA), EstadoSolicitud.PENDIENTE);
        assertTrue(salida.getOut().contains("Contabilidad: programar pago de 100000 para E-7"));
    }

    @Test
    void auditoriaYEmpleadoReaccionanATodoCambio(CapturedOutput salida) {
        new NotificadorAuditoria().onCambioEstado(solicitudEn(EstadoSolicitud.REQUIERE_CORRECCION), EstadoSolicitud.PENDIENTE);
        new NotificadorEmpleado().onCambioEstado(solicitudEn(EstadoSolicitud.REQUIERE_CORRECCION), EstadoSolicitud.PENDIENTE);
        assertTrue(salida.getOut().contains("Auditoria: solicitud 7 cambio de PENDIENTE a REQUIERE_CORRECCION"));
        assertTrue(salida.getOut().contains("Notificando a E-7: su solicitud 7 paso de PENDIENTE a REQUIERE_CORRECCION"));
    }
}
