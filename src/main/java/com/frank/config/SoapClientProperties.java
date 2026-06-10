package com.frank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "soap.country-info")
public class SoapClientProperties {

    private String wsdlUrl;
    private String namespace;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;

    public String getEndpointUrl() {
        if (wsdlUrl == null || wsdlUrl.isBlank()) {
            throw new IllegalStateException("soap.country-info.wsdl-url is not configured");
        }
        return wsdlUrl.endsWith("?WSDL") || wsdlUrl.endsWith("?wsdl")
                ? wsdlUrl.substring(0, wsdlUrl.indexOf('?'))
                : wsdlUrl;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
