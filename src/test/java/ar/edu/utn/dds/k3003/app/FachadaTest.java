//package ar.edu.utn.dds.k3003.app;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//import ar.edu.utn.dds.k3003.facades.FachadaSolicitudes;
//import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
//import ar.edu.utn.dds.k3003.model.PdI;
//import ar.edu.utn.dds.k3003.repository.InMemoryPdIRepo;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//class FachadaTest {
//
//    @Test
//    @DisplayName("setFachadaSolicitudes permite usar la fachada mockeada correctamente")
//    void testSetFachadaSolicitudes() {
//        Fachada fachada = new Fachada();
//        FachadaSolicitudes mockSolicitudes = mock(FachadaSolicitudes.class);
//        when(mockSolicitudes.estaActivo("hecho-x")).thenReturn(true);
//
//        fachada.setFachadaSolicitudes(mockSolicitudes);
//
//        boolean resultado = mockSolicitudes.estaActivo("hecho-x");
//        assertTrue(resultado, "La fachada mockeada debe estar correctamente asignada");
//    }
//
//    @Test
//    @DisplayName(
//            "buscarPdIPorId devuelve correctamente un PdI previamente guardado sin usar procesar")
//    void testBuscarPdIPorId() {
//        InMemoryPdIRepo repositorio = new InMemoryPdIRepo();
//        Fachada fachada = new Fachada(repositorio);
//        FachadaSolicitudes mockSolicitudes = mock(FachadaSolicitudes.class);
//        fachada.setFachadaSolicitudes(mockSolicitudes);
//
//        PdI nuevo = new PdI("hecho-test", "desc", "lugar", LocalDateTime.now(), "contenido");
//        nuevo.setId(1L);
//        nuevo.setEtiquetas(List.of("prueba"));
//
//        repositorio.save(nuevo); // delegación directa al repo
//
//        PdIDTO recuperado = fachada.buscarPdIPorId("1");
//
//        assertEquals("hecho-test", recuperado.hechoId(), "El hechoId debe coincidir");
//        assertEquals("desc", recuperado.descripcion(), "La descripción debe coincidir");
//        assertEquals("lugar", recuperado.lugar(), "El lugar debe coincidir");
//    }
//
//    @Test
//    @DisplayName("buscarPorHecho devuelve correctamente los PdIs guardados sin usar procesar")
//    void testBuscarPorHecho() {
//        InMemoryPdIRepo repositorio = new InMemoryPdIRepo();
//        Fachada fachada = new Fachada(repositorio);
//        FachadaSolicitudes mockSolicitudes = mock(FachadaSolicitudes.class);
//        fachada.setFachadaSolicitudes(mockSolicitudes);
//
//        PdI pdi1 = new PdI("hecho-xyz", "desc1", "lugar1", LocalDateTime.now(), "contenido");
//        PdI pdi2 = new PdI("hecho-xyz", "desc2", "lugar2", LocalDateTime.now(), "contenido");
//
//        pdi1.setId(1L);
//        pdi2.setId(2L);
//        pdi1.setEtiquetas(List.of("etiqueta1"));
//        pdi2.setEtiquetas(List.of("etiqueta2"));
//
//        repositorio.save(pdi1);
//        repositorio.save(pdi2);
//
//        List<PdIDTO> resultados = fachada.buscarPorHecho("hecho-xyz");
//
//        assertEquals(2, resultados.size(), "Debe haber 2 PdIs asociadas al hecho");
//        assertEquals("desc1", resultados.get(0).descripcion());
//        assertEquals("desc2", resultados.get(1).descripcion());
//    }
//
//    @Test
//    @DisplayName("procesar retorna un PdIDTO con los datos esperados")
//    void testProcesar() {
//        InMemoryPdIRepo repositorio = new InMemoryPdIRepo();
//        Fachada fachada = new Fachada(repositorio);
//        FachadaSolicitudes mockSolicitudes = mock(FachadaSolicitudes.class);
//        fachada.setFachadaSolicitudes(mockSolicitudes);
//        when(mockSolicitudes.estaActivo("hecho-123")).thenReturn(true);
//
//        PdIDTO dtoEntrada =
//                new PdIDTO(
//                        null,
//                        "hecho-123",
//                        "descripcion prueba",
//                        "CABA",
//                        LocalDateTime.of(2025, 5, 17, 15, 30),
//                        "contenido con fuego",
//                        List.of());
//
//        PdIDTO resultado = fachada.procesar(dtoEntrada);
//
//        assertEquals("hecho-123", resultado.hechoId(), "El hechoId debe coincidir");
//        assertEquals(
//                "descripcion prueba", resultado.descripcion(), "La descripción debe coincidir");
//        assertEquals("CABA", resultado.lugar(), "El lugar debe coincidir");
//        assertEquals(
//                LocalDateTime.of(2025, 5, 17, 15, 30),
//                resultado.momento(),
//                "La fecha debe coincidir");
//        assertEquals("contenido con fuego", resultado.contenido(), "El contenido debe coincidir");
//        assertTrue(resultado.etiquetas().contains("incendio"), "Debe tener la etiqueta 'incendio'");
//    }
//}
