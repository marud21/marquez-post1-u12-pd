package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class ValidadorMontoMaximo extends ValidadorSolicitud {

    private static final Map<CategoriaGasto, BigDecimal> TOPES = Map.of(
            CategoriaGasto.VIAJE, new BigDecimal("150000"),          // tope DIARIO
            CategoriaGasto.REPRESENTACION, new BigDecimal("2000000"),
            CategoriaGasto.CAPACITACION, new BigDecimal("3000000"));

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        BigDecimal tope = topeDe(solicitud);
        return solicitud.getMontoSolicitado().compareTo(tope) > 0
                ? ResultadoValidacion.rechazar("Monto excede el tope de la categoria " + solicitud.getCategoria()
                        + " (" + tope.toPlainString() + ")")
                : ResultadoValidacion.ok();
    }

    // En viajes el tope es diario: un viaje de 3 dias admite hasta 3 x 150.000, coherente con CalculadoraViaje.
    private BigDecimal topeDe(SolicitudReembolso solicitud) {
        BigDecimal tope = TOPES.get(solicitud.getCategoria());
        return solicitud.getCategoria() == CategoriaGasto.VIAJE
                ? tope.multiply(BigDecimal.valueOf(Math.max(solicitud.getDiasViaje(), 1)))
                : tope;
    }
}
