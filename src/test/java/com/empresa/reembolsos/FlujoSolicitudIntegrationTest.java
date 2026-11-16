package com.empresa.reembolsos;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorAuditoria;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorContabilidad;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorEmpleado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujo completo: REST -> servicio -> cadena -> estrategia -> JPA (H2) -> observadores. */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoSolicitudIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @SpyBean
    NotificadorEmpleado empleado;
    @SpyBean
    NotificadorContabilidad contabilidad;
    @SpyBean
    NotificadorAuditoria auditoria;

    private long crear(String cuerpo) throws Exception {
        String res = mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    @Test
    void creaLasTresCategoriasCalculaElMontoYNotificaALosTresObservadores() throws Exception {
        long viaje = crear("{\"empleadoId\":\"E-1001\",\"categoria\":\"VIAJE\",\"montoSolicitado\":400000,"
                + "\"fechaGasto\":\"2026-11-03\",\"diasViaje\":3,\"documentosSoporte\":true}");
        crear("{\"empleadoId\":\"E-1001\",\"categoria\":\"REPRESENTACION\",\"montoSolicitado\":850000,"
                + "\"fechaGasto\":\"2026-11-05\",\"documentosSoporte\":true}");
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoId\":\"E-1002\",\"categoria\":\"CAPACITACION\",\"montoSolicitado\":1000000,"
                                + "\"fechaGasto\":\"2026-11-10\",\"documentosSoporte\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoAprobado").value(800000.0))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        mvc.perform(put("/api/solicitudes/" + viaje + "/estado").contentType(MediaType.APPLICATION_JSON).content("\"APROBADA\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.montoAprobado").value(400000.0));

        verify(empleado, times(1)).onCambioEstado(any(SolicitudReembolso.class), eq(EstadoSolicitud.PENDIENTE));
        verify(contabilidad, times(1)).onCambioEstado(argThat(s -> s.getEstado() == EstadoSolicitud.APROBADA), any());
        verify(auditoria, times(1)).onCambioEstado(any(SolicitudReembolso.class), eq(EstadoSolicitud.PENDIENTE));
    }

    @Test
    void rechazaSinDocumentosYDuplicadosConMensajeClaro() throws Exception {
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoId\":\"E-2001\",\"categoria\":\"REPRESENTACION\",\"montoSolicitado\":100000,"
                                + "\"fechaGasto\":\"2026-10-01\",\"documentosSoporte\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Faltan documentos soporte"));

        String valida = "{\"empleadoId\":\"E-2001\",\"categoria\":\"REPRESENTACION\",\"montoSolicitado\":100000,"
                + "\"fechaGasto\":\"2026-10-01\",\"documentosSoporte\":true}";
        crear(valida);
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(valida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ya existe una solicitud pendiente de la misma categoria en el periodo"));

        mvc.perform(put("/api/solicitudes/9999/estado").contentType(MediaType.APPLICATION_JSON).content("\"APROBADA\""))
                .andExpect(status().isNotFound());
    }
}
