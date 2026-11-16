package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.math.BigDecimal;

/** Strategy: una formula de monto aprobado por categoria de gasto. */
public interface CalculadoraReembolso {

    CategoriaGasto getCategoria();

    BigDecimal calcular(SolicitudReembolso solicitud);
}
