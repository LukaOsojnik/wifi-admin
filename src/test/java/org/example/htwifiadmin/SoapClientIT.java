package org.example.htwifiadmin;

import org.example.htwifiadmin.soap.GetCpeIdRequest;
import org.example.htwifiadmin.soap.GetCpeIdResponse;
import org.example.htwifiadmin.soap.WifiPlatformPortType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the SOAP wire end-to-end: config -> generated client -> CXF marshalling
 * -> HTTP -> Mockoon and back. Integration test: requires the mock running at
 * platform.soap.endpoint (docker compose up -d).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:soapclientit;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "sync.enabled=false"
})
class SoapClientIT {

    @Autowired
    private WifiPlatformPortType port;

    @Test
    void getCpeID_returnsSeededConfiguration() {
        GetCpeIdRequest request = new GetCpeIdRequest();
        request.setCpeId("CPE_001");

        GetCpeIdResponse response = port.getCpeID(request);

        assertThat(response.getConfiguration()).isNotNull();
        assertThat(response.getConfiguration().getCpeId()).isEqualTo("CPE_001");
        assertThat(response.getConfiguration().getSsid()).isEqualTo("Office-2G");
    }
}
