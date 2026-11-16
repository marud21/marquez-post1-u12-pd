package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadoresTest {

    static SolicitudReembolso solicitud(CategoriaGasto cat, String monto, LocalDate fecha, int dias, boolean docs) {
        return new SolicitudReembolso("E-1", cat, new BigDecimal(monto), fecha, dias, docs);
    }

    @Nested
    class DocumentosSoporte {
        private final ValidadorDocumentosSoporte validador = new ValidadorDocumentosSoporte();

        @Test
        void sinDocumentos_rechazaConMotivoClaro() {
            ResultadoValidacion r = validador.validar(
                    solicitud(CategoriaGasto.REPRESENTACION, "100000", LocalDate.now(), 0, false), List.of());
            assertFalse(r.valida());
            assertEquals("Faltan documentos soporte", r.motivoRechazo());
        }

        @Test
        void conDocumentos_aprueba() {
            assertTrue(validador.validar(
                    solicitud(CategoriaGasto.REPRESENTACION, "100000", LocalDate.now(), 0, true), List.of()).valida());
        }
    }

    @Nested
    class MontoMaximo {
        private final ValidadorMontoMaximo validador = new ValidadorMontoMaximo();

        @Test
        void representacionSobreElTope_rechaza() {
            assertFalse(validador.validar(
                    solicitud(CategoriaGasto.REPRESENTACION, "2000001", LocalDate.now(), 0, true), List.of()).valida());
        }

        @Test
        void viajeUsaTopeDiarioPorDias() {
            assertTrue(validador.validar(
                    solicitud(CategoriaGasto.VIAJE, "450000", LocalDate.now(), 3, true), List.of()).valida());
            assertFalse(validador.validar(
                    solicitud(CategoriaGasto.VIAJE, "450001", LocalDate.now(), 3, true), List.of()).valida());
        }
    }

    @Nested
    class Duplicados {
        private final ValidadorDuplicados validador = new ValidadorDuplicados();

        @Test
        void mismaCategoriaYMismoPeriodo_rechaza() {
            SolicitudReembolso pendiente = solicitud(CategoriaGasto.CAPACITACION, "500000", LocalDate.of(2026, 11, 2), 0, true);
            assertFalse(validador.validar(
                    solicitud(CategoriaGasto.CAPACITACION, "300000", LocalDate.of(2026, 11, 20), 0, true),
                    List.of(pendiente)).valida());
        }

        @Test
        void mismoMesDeOtroAnioOOtraCategoria_noEsDuplicado() {
            SolicitudReembolso pendiente = solicitud(CategoriaGasto.CAPACITACION, "500000", LocalDate.of(2025, 11, 2), 0, true);
            assertTrue(validador.validar(
                    solicitud(CategoriaGasto.CAPACITACION, "300000", LocalDate.of(2026, 11, 20), 0, true),
                    List.of(pendiente)).valida());
            assertTrue(validador.validar(
                    solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2025, 11, 5), 1, true),
                    List.of(pendiente)).valida());
        }
    }

    @Nested
    class Cadena {
        @Test
        void seDetieneEnElPrimerRechazoSinEjecutarLosEslabonesSiguientes() {
            AtomicInteger llamadasAlUltimo = new AtomicInteger();
            ValidadorSolicitud espia = new ValidadorSolicitud() {
                @Override
                protected ResultadoValidacion validarPropio(SolicitudReembolso s, List<SolicitudReembolso> p) {
                    llamadasAlUltimo.incrementAndGet();
                    return ResultadoValidacion.ok();
                }
            };
            ValidadorDocumentosSoporte documentos = new ValidadorDocumentosSoporte();
            documentos.siguiente(new ValidadorMontoMaximo()).siguiente(espia);

            ResultadoValidacion r = documentos.validar(
                    solicitud(CategoriaGasto.VIAJE, "1", LocalDate.now(), 1, false), List.of());

            assertFalse(r.valida());
            assertEquals(0, llamadasAlUltimo.get());
        }

        @Test
        void cadenaCompleta_apruebaCuandoTodosLosEslabonesAprueban() {
            CadenaValidadores cadena = new CadenaValidadores(new ValidadorDocumentosSoporte(),
                    new ValidadorMontoMaximo(), new ValidadorDuplicados());
            assertTrue(cadena.validar(
                    solicitud(CategoriaGasto.REPRESENTACION, "800000", LocalDate.now(), 0, true), List.of()).valida());
        }
    }
}
