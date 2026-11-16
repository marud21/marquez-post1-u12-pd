package com.empresa.reembolsos.aplicacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import com.empresa.reembolsos.procesamiento.calculo.SelectorCalculadora;
import com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado;
import com.empresa.reembolsos.procesamiento.validacion.CadenaValidadores;
import com.empresa.reembolsos.procesamiento.validacion.ResultadoValidacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Unico punto que coordina los tres patrones y el unico que conoce el repositorio. */
@Service
@Transactional
public class SolicitudReembolsoService {

    private final CadenaValidadores validadores;
    private final SelectorCalculadora calculadoras;
    private final List<ObservadorCambioEstado> observadores;
    private final RepositorioSolicitudesJpa repositorio;

    public SolicitudReembolsoService(CadenaValidadores validadores, SelectorCalculadora calculadoras,
                                     List<ObservadorCambioEstado> observadores, RepositorioSolicitudesJpa repositorio) {
        this.validadores = validadores;
        this.calculadoras = calculadoras;
        this.observadores = observadores;
        this.repositorio = repositorio;
    }

    public SolicitudReembolso crear(SolicitudReembolso solicitud) {
        List<SolicitudReembolso> pendientes =
                repositorio.findByEmpleadoIdAndEstado(solicitud.getEmpleadoId(), EstadoSolicitud.PENDIENTE);
        ResultadoValidacion resultado = validadores.validar(solicitud, pendientes);
        if (!resultado.valida()) {
            throw new SolicitudInvalidaException(resultado.motivoRechazo());
        }
        solicitud.setMontoAprobado(calculadoras.obtener(solicitud.getCategoria()).calcular(solicitud));
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        return repositorio.save(solicitud);
    }

    public SolicitudReembolso cambiarEstado(Long id, EstadoSolicitud nuevoEstado) {
        SolicitudReembolso solicitud = repositorio.findById(id)
                .orElseThrow(() -> new SolicitudNoEncontradaException(id));
        EstadoSolicitud estadoAnterior = solicitud.getEstado();
        solicitud.setEstado(nuevoEstado);
        SolicitudReembolso guardada = repositorio.save(solicitud);
        observadores.forEach(o -> o.onCambioEstado(guardada, estadoAnterior));
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<SolicitudReembolso> listar() {
        return repositorio.findAll();
    }
}
