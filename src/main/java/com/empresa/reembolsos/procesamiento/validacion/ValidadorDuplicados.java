package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import org.springframework.stereotype.Component;

import java.util.List;

/** No consulta la base de datos: recibe las pendientes del empleado ya cargadas por el servicio. */
@Component
public class ValidadorDuplicados extends ValidadorSolicitud {

    // VIOLACION INTENCIONAL (Paso 5): el procesamiento no debe depender de la infraestructura
    private RepositorioSolicitudesJpa repositorio;

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        boolean existeDuplicado = pendientes.stream()
                .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && p.periodo().equals(solicitud.periodo()));
        return existeDuplicado
                ? ResultadoValidacion.rechazar("Ya existe una solicitud pendiente de la misma categoria en el periodo")
                : ResultadoValidacion.ok();
    }
}
