package org.example.htwifiadmin.config;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.example.htwifiadmin.soap.WifiPlatformPortType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Builds the SOAP client for the external WiFi platform.
 * The generated {@link WifiPlatformPortType} carries the contract (namespace,
 * SOAPAction, message shapes); this config only supplies the runtime concerns
 * the contract does not: which URL to call and how long to wait.
 */
@Configuration
public class SoapClientConfig {

    @Bean
    public WifiPlatformPortType wifiPlatformPort(
            @Value("${platform.soap.endpoint}") String endpoint,
            @Value("${platform.soap.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${platform.soap.receive-timeout-ms:5000}") long receiveTimeoutMs) {

        // 1-4: manufacture the client proxy for the generated port interface,
        // pointed at the configured platform URL.
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(WifiPlatformPortType.class);
        factory.setAddress(endpoint);

        // Force the platform namespace to serialize with the literal prefix "tns".
        // By default CXF emits it as a default namespace (<GetCpeIdRequest xmlns="...">),
        // but the Mockoon template reads the request by a fixed string path
        // (soap:Body.tns:GetCpeIdRequest.tns:cpeId), so the elements must carry "tns:".
        JAXBDataBinding dataBinding = new JAXBDataBinding();
        dataBinding.setNamespaceMap(Map.of("http://wifi-admin.local/platform/v1", "tns"));
        factory.setDataBinding(dataBinding);

        WifiPlatformPortType port = (WifiPlatformPortType) factory.create();

        // Timeouts: reach under the proxy to the HTTP transport so a slow/unreachable
        // platform fails fast (-> mappable to HTTP 502) instead of hanging the thread.
        Client client = ClientProxy.getClient(port);
        HTTPConduit conduit = (HTTPConduit) client.getConduit();
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(connectTimeoutMs);
        policy.setReceiveTimeout(receiveTimeoutMs);
        // CXF 4 uses the JDK HttpClient, which tries an HTTP/2 (h2c) upgrade by default.
        // The Mockoon mock only speaks HTTP/1.1 and answers 404 to the upgrade, so pin 1.1.
        policy.setVersion("1.1");
        conduit.setClient(policy);

        // Tolerate the platform's malformed response prolog (see bug-fixes.md BUG-003).
        client.getInInterceptors().add(leadingWhitespaceStrippingInterceptor());

        return port;
    }

    /**
     * The mock's updateCpeId response has a leading newline before {@code <?xml?>}, which is invalid
     * XML and makes CXF's parser fail. This RECEIVE-phase interceptor strips any leading whitespace
     * from the response before parsing, so the client tolerates the platform's quirk.
     */
    private AbstractPhaseInterceptor<Message> leadingWhitespaceStrippingInterceptor() {
        return new AbstractPhaseInterceptor<>(Phase.RECEIVE) {
            @Override
            public void handleMessage(Message message) throws Fault {
                InputStream in = message.getContent(InputStream.class);
                if (in == null) {
                    return;
                }
                try {
                    byte[] bytes = in.readAllBytes();
                    int start = 0;
                    while (start < bytes.length && Character.isWhitespace(bytes[start])) {
                        start++;
                    }
                    message.setContent(InputStream.class,
                            new ByteArrayInputStream(bytes, start, bytes.length - start));
                } catch (IOException e) {
                    throw new Fault(e);
                }
            }
        };
    }
}
