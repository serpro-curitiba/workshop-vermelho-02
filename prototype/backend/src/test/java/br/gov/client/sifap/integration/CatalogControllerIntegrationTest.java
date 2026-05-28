package br.gov.client.sifap.integration;

import br.gov.client.sifap.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateProgramWithAdjustedBaseAndFactorK() throws Exception {
        mockMvc.perform(post("/api/v1/programas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"PBF1",
                                  "nome":"Programa Bolsa Familia",
                                  "tipo":"A",
                                  "valorBase":100.00,
                                  "fatorReajuste":0.10,
                                  "exigeBiometria":true,
                                  "fatoresRegionais":[{"uf":"SP","fatorRegional":1.1000}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fatorK").value(1.0347215))
                .andExpect(jsonPath("$.valorBaseAjustado").value(103.47));

        mockMvc.perform(get("/api/v1/programas/PBF1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("PBF1"))
                .andExpect(jsonPath("$.fatoresRegionais[0].uf").value("SP"));
    }
}