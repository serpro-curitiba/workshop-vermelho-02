package br.gov.client.sifap.integration;

import br.gov.client.sifap.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setupData() throws Exception {
        mockMvc.perform(post("/api/v1/programas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"PAY1",
                                  "nome":"Pagamento ativo",
                                  "tipo":"A",
                                  "valorBase":200.00,
                                  "fatorReajuste":0.10,
                                  "exigeBiometria":true,
                                  "fatoresRegionais":[{"uf":"SP","fatorRegional":1.1000}]
                                }
                                """));

        mockMvc.perform(post("/api/v1/beneficiarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf":"98765432100",
                                  "nome":"Jose Pereira",
                                  "dataNascimento":"1959-03-15",
                                  "status":"A",
                                  "uf":"SP",
                                  "codRegiao":"03",
                                  "rendaFamiliar":250.00,
                                  "codigoPrograma":"PAY1",
                                  "dependentes":[
                                    {
                                      "cpf":"11111111111",
                                      "nome":"Filho 1",
                                      "dataNascimento":"2012-01-10",
                                      "parentesco":"FI",
                                      "status":"A",
                                      "indicadorDeficiencia":"N"
                                    },
                                    {
                                      "cpf":"22222222222",
                                      "nome":"Filho 2",
                                      "dataNascimento":"2014-07-10",
                                      "parentesco":"FI",
                                      "status":"A",
                                      "indicadorDeficiencia":"N"
                                    }
                                  ]
                                }
                                """));
    }

    @Test
    void shouldRejectCalculationWhenBiometricsArePending() throws Exception {
        mockMvc.perform(post("/api/v1/pagamentos:calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf":"98765432100",
                                  "competencia":"202605",
                                  "descontos":[]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("BIOMETRIA PENDENTE"));
    }

    @Test
    void shouldCalculateDecemberPaymentAndExposeAuditTrail() throws Exception {
        mockMvc.perform(post("/api/v1/beneficiarios/98765432100/biometria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hashTemplate":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                                  "codigoPosto":"PST002"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pagamentos:calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf":"98765432100",
                                  "competencia":"202612",
                                  "descontos":[
                                    {"tipo":"JD","valor":50.00},
                                    {"tipo":"IR","valor":80.00,"dtInicio":"2020-01-01","dtFim":"2099-01-01"},
                                    {"tipo":"EX","valor":15.00,"dtInicio":"2099-01-01"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPagamento").value("D"))
                .andExpect(jsonPath("$.valorDecimoTerceiro").exists())
                .andExpect(jsonPath("$.valorAbono").exists())
                .andExpect(jsonPath("$.valorLiquido").isNumber());

        mockMvc.perform(post("/api/v1/pagamentos:calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf":"98765432100",
                                  "competencia":"202612",
                                  "descontos":[]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(get("/api/v1/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acao").exists());
    }
}