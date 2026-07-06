package org.example.htwifiadmin;

import org.example.htwifiadmin.persistence.WifiConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full end-to-end: REST request -> controller -> service -> SOAP client -> Mockoon -> back.
 * MockMvc is built from the context via spring-test, with the Spring Security filter chain
 * applied so requests carry HTTP Basic credentials. Requires the mock running (docker compose up -d).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:controllerit;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "sync.enabled=false"
})
class WifiControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private WifiConfigurationRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getWifiParameter_returnsSeededConfigAsJson() throws Exception {
        mockMvc.perform(get("/wifi-parameter/CPE_001").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpeId").value("CPE_001"))
                .andExpect(jsonPath("$.ssid").value("Office-2G"))
                .andExpect(jsonPath("$.wifiBand").value("BAND_2_4_GHZ"))
                .andExpect(jsonPath("$.encryptionType").value("WPA2_PSK"));
    }

    @Test
    void getWifiParameter_unknownCpe_returns404() throws Exception {
        mockMvc.perform(get("/wifi-parameter/DOES_NOT_EXIST").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CPE_NOT_FOUND"));
    }

    @Test
    void putWifiParameter_missingPasswordForWpa2_returns400() throws Exception {
        String body = """
                {"cpeId":"CPE_001","wifiBand":"BAND_2_4_GHZ","ssid":"Net","encryptionType":"WPA2_PSK"}
                """;
        mockMvc.perform(put("/wifi-parameter").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_REQUIRED"));
    }

    @Test
    void getWifiParameter_withoutCredentials_returns401() throws Exception {
        mockMvc.perform(get("/wifi-parameter/CPE_001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putWifiParameter_valid_updatesAndReturns200() throws Exception {
        String body = """
                {"cpeId":"CPE_001","wifiBand":"BAND_2_4_GHZ","ssid":"Office-2G",
                 "encryptionType":"WPA2_PSK","password":"new-pass"}
                """;
        mockMvc.perform(put("/wifi-parameter").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpeId").value("CPE_001"))
                .andExpect(jsonPath("$.password").value("new-pass"));
    }

    @Test
    void getWifiParameter_cacheMiss_persistsToDatabase() throws Exception {
        assertThat(repository.findById("CPE_002")).isEmpty(); // fresh in-memory DB

        mockMvc.perform(get("/wifi-parameter/CPE_002").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());

        assertThat(repository.findById("CPE_002")).isPresent(); // read-through cached it
    }
}
