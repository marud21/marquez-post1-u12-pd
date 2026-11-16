package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/** Observer: cada interesado reacciona a su manera a un cambio de estado. */
public interface ObservadorCambioEstado {

    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}
