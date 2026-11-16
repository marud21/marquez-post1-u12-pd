package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CalculadoraCapacitacion implements CalculadoraReembolso {

    private static final BigDecimal PORCENTAJE = new BigDecimal("0.8");
    private static final BigDecimal TOPE_ANUAL = new BigDecimal("3000000");

    @Override
    public CategoriaGasto getCategoria() {
        return CategoriaGasto.CAPACITACION;
    }

    @Override
    public BigDecimal calcular(SolicitudReembolso solicitud) {
        BigDecimal reembolsoParcial = solicitud.getMontoSolicitado().multiply(PORCENTAJE)
                .setScale(2, RoundingMode.HALF_UP);
        return reembolsoParcial.min(TOPE_ANUAL);
    }
}
