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
 * Builds the SOAP client for the external WiFi platform: sets the URL,
 * the timeouts, and a few workarounds for the mock server's quirks.
 */
@Configuration
public class SoapClientConfig {

    /** Creates the SOAP client used everywhere else in the app. */
    @Bean
    public WifiPlatformPortType wifiPlatformPort(
            @Value("${platform.soap.endpoint}") String endpoint,
            @Value("${platform.soap.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${platform.soap.receive-timeout-ms:5000}") long receiveTimeoutMs) {

        // Build a client for the generated SOAP interface, pointed at the platform URL.
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(WifiPlatformPortType.class);
        factory.setAddress(endpoint);

        // Make the XML use the "tns:" prefix explicitly — the mock server looks up
        // request fields by that exact prefix and returns nothing without it.
        JAXBDataBinding dataBinding = new JAXBDataBinding();
        dataBinding.setNamespaceMap(Map.of("http://wifi-admin.local/platform/v1", "tns"));
        factory.setDataBinding(dataBinding);

        WifiPlatformPortType port = (WifiPlatformPortType) factory.create();

        // Set timeouts so a slow or dead platform fails fast instead of hanging.
        Client client = ClientProxy.getClient(port);
        HTTPConduit conduit = (HTTPConduit) client.getConduit();
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(connectTimeoutMs);
        policy.setReceiveTimeout(receiveTimeoutMs);
        // Stick to HTTP/1.1 — the mock server rejects CXF's default HTTP/2 upgrade attempt.
        policy.setVersion("1.1");
        conduit.setClient(policy);

        // Work around the mock's invalid leading newline in responses (see bug-fixes.md BUG-003).
        client.getInInterceptors().add(leadingWhitespaceStrippingInterceptor());

        return port;
    }

    /**
     * Strips whitespace from the start of every response before XML parsing.
     * Needed because the mock's updateCpeId response starts with a newline,
     * which is invalid XML and would otherwise crash the parser.
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
