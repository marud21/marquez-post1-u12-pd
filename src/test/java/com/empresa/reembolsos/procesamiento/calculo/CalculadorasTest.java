package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculadorasTest {

    static SolicitudReembolso solicitud(CategoriaGasto cat, String monto, int dias) {
        return new SolicitudReembolso("E-1", cat, new BigDecimal(monto), LocalDate.of(2026, 11, 3), dias, true);
    }

    @ParameterizedTest(name = "viaje {0} en {1} dias -> {2}")
    @CsvSource({"200000, 2, 200000", "400000, 2, 300000", "150000, 1, 150000"})
    void viaje_aplicaTopeDiarioPorDias(String monto, int dias, String esperado) {
        assertEquals(0, new BigDecimal(esperado).compareTo(
                new CalculadoraViaje().calcular(solicitud(CategoriaGasto.VIAJE, monto, dias))));
    }

    @ParameterizedTest(name = "representacion {0} -> {1}")
    @CsvSource({"850000, 850000", "2500000, 2000000"})
    void representacion_reembolsaCompletoHastaTopeMensual(String monto, String esperado) {
        assertEquals(0, new BigDecimal(esperado).compareTo(
                new CalculadoraRepresentacion().calcular(solicitud(CategoriaGasto.REPRESENTACION, monto, 0))));
    }

    @ParameterizedTest(name = "capacitacion {0} -> {1}")
    @CsvSource({"1000000, 800000.00", "5000000, 3000000"})
    void capacitacion_reembolsa80PorCientoHastaTopeAnual(String monto, String esperado) {
        assertEquals(0, new BigDecimal(esperado).compareTo(
                new CalculadoraCapacitacion().calcular(solicitud(CategoriaGasto.CAPACITACION, monto, 0))));
    }

    @Test
    void selector_devuelveLaEstrategiaDeCadaCategoriaYRechazaLasNoRegistradas() {
        SelectorCalculadora selector = new SelectorCalculadora(List.of(new CalculadoraViaje(), new CalculadoraRepresentacion()));
        assertInstanceOf(CalculadoraViaje.class, selector.obtener(CategoriaGasto.VIAJE));
        assertInstanceOf(CalculadoraRepresentacion.class, selector.obtener(CategoriaGasto.REPRESENTACION));
        assertThrows(IllegalArgumentException.class, () -> selector.obtener(CategoriaGasto.CAPACITACION));
    }
}
