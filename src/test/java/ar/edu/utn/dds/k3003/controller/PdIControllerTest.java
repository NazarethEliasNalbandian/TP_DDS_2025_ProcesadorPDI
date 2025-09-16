//package ar.edu.utn.dds.k3003.controller;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//public class PdIControllerTest {
//
//    @Autowired private MockMvc mockMvc;
//
//    @Autowired private ObjectMapper objectMapper;
//
//    private final String hechoId = "hecho123";
//
//    @Test
//    public void listarPdis_DeberiaRetornarListaVacia() throws Exception {
//        mockMvc.perform(get("/pdis?hecho=" + hechoId))
//                .andExpect(status().isOk())
//                .andExpect(content().json("[]"));
//    }
//
//    @Test
//    public void crearPdI_DeberiaRetornarPdICreado() throws Exception {
//        PdIDTO nuevoPdI =
//                new PdIDTO(
//                        "", // id se ignora
//                        hechoId,
//                        "Incendio en bosque",
//                        "Patagonia",
//                        LocalDateTime.now(),
//                        "fuego y humo visible",
//                        List.of());
//
//        mockMvc.perform(
//                        post("/pdis")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(nuevoPdI)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.hecho_id").value(hechoId))
//                .andExpect(jsonPath("$.descripcion").value("Incendio en bosque"));
//
//        // Verificar que se guardó
//        mockMvc.perform(get("/pdis?hecho=" + hechoId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].hecho_id").value(hechoId));
//    }
//}
