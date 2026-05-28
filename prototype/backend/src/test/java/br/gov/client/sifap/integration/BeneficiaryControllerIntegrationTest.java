package br.gov.client.sifap.integration;

import br.gov.client.sifap.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BeneficiaryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setupProgram() throws Exception {
        mockMvc.perform(post("/api/v1/programas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"CAT1",
                                  "nome":"Catalogo teste",
                                  "tipo":"A",
                                  "valorBase":150.00,
                                  "fatorReajuste":0.05,
                                  "exigeBiometria":false,
                                  "fatoresRegionais":[{"uf":"SP","fatorRegional":1.0000}]
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldCreateBeneficiaryAndRegisterBiometrics() throws Exception {
        mockMvc.perform(post("/api/v1/beneficiarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf":"12345678901",
                                  "nome":"Maria da Silva",
                                  "dataNascimento":"1980-05-10",
                                  "status":"A",
                                  "uf":"SP",
                                  "codRegiao":"03",
                                  "rendaFamiliar":400.00,
                                  "codigoPrograma":"CAT1",
                                  "dependentes":[
                                    {
                                      "cpf":"00000000000",
                                      "nome":"Dependente 1",
                                      "dataNascimento":"2015-02-01",
                                      "parentesco":"FI",
                                      "status":"A",
                                      "indicadorDeficiencia":"N"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.totalDependentes").value(1));

        mockMvc.perform(post("/api/v1/beneficiarios/12345678901/biometria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hashTemplate":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                  "codigoPosto":"PST001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometria").value("S"));
    }
}