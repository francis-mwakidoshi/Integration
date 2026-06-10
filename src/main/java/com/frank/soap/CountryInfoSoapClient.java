package com.frank.soap;

import com.frank.config.SoapClientProperties;
import com.frank.exception.SoapServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CountryInfoSoapClient {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoSoapClient.class);

    private final RestTemplate restTemplate;
    private final SoapClientProperties properties;

    public CountryInfoSoapClient(RestTemplate soapRestTemplate, SoapClientProperties properties) {
        this.restTemplate = soapRestTemplate;
        this.properties = properties;
    }

    @CircuitBreaker(name = "countrySoapService", fallbackMethod = "fetchIsoCodeFallback")
    @Retry(name = "countrySoapService")
    public String fetchCountryIsoCode(String countryName) {
        log.info("Calling SOAP CountryISOCode for country={}", countryName);
        String envelope = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <CountryISOCode xmlns="%s">
                      <sCountryName>%s</sCountryName>
                    </CountryISOCode>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(properties.getNamespace(), escapeXml(countryName));

        String response = postSoap(envelope, "CountryISOCode");
        String isoCode = extractTextContent(response, "CountryISOCodeResult");

        if (isoCode == null || isoCode.isBlank()) {
            throw new SoapServiceException("ISO code not found for country: " + countryName);
        }

        log.info("SOAP CountryISOCode succeeded country={} isoCode={}", countryName, isoCode);
        return isoCode.trim();
    }

    @CircuitBreaker(name = "countrySoapService", fallbackMethod = "fetchFullInfoFallback")
    @Retry(name = "countrySoapService")
    public FullCountryInfoData fetchFullCountryInfo(String isoCode) {
        log.info("Calling SOAP FullCountryInfo for isoCode={}", isoCode);
        String envelope = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <FullCountryInfo xmlns="%s">
                      <sCountryISOCode>%s</sCountryISOCode>
                    </FullCountryInfo>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(properties.getNamespace(), escapeXml(isoCode));

        String response = postSoap(envelope, "FullCountryInfo");
        return parseFullCountryInfo(response, isoCode);
    }

    private String postSoap(String envelope, String operation) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", properties.getNamespace() + "/" + operation);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    properties.getEndpointUrl(),
                    new HttpEntity<>(envelope, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new SoapServiceException("SOAP call failed for operation: " + operation);
            }

            String body = response.getBody();
            if (body.contains(":Fault") || body.contains("Fault>")) {
                String fault = extractTextContent(body, "faultstring");
                throw new SoapServiceException("SOAP fault during " + operation + ": "
                        + (fault != null ? fault : "unknown fault"));
            }

            return body;
        } catch (RestClientException ex) {
            throw new SoapServiceException("Unable to reach SOAP service for " + operation, ex);
        }
    }

    private FullCountryInfoData parseFullCountryInfo(String xml, String requestedIsoCode) {
        try {
            Document document = parseXml(xml);
            String isoCode = firstNonBlank(
                    textByLocalName(document, "sISOCode"),
                    requestedIsoCode
            );
            String name = textByLocalName(document, "sName");
            String capitalCity = textByLocalName(document, "sCapitalCity");
            String phoneCode = textByLocalName(document, "sPhoneCode");
            String continentCode = textByLocalName(document, "sContinentCode");
            String currencyCode = textByLocalName(document, "sCurrencyISOCode");
            String flagUrl = textByLocalName(document, "sCountryFlag");
            List<String> languages = extractLanguages(document);

            if (name == null || name.isBlank()) {
                throw new SoapServiceException("Full country info missing name for ISO code: " + requestedIsoCode);
            }

            log.info("SOAP FullCountryInfo succeeded isoCode={} name={}", isoCode, name);
            return new FullCountryInfoData(
                    isoCode,
                    name,
                    capitalCity,
                    phoneCode,
                    continentCode,
                    currencyCode,
                    flagUrl,
                    languages
            );
        } catch (SoapServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SoapServiceException("Failed to parse FullCountryInfo response", ex);
        }
    }

    private List<String> extractLanguages(Document document) {
        List<String> languages = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ("sName".equals(node.getLocalName()) && isLanguageNode(node)) {
                String value = node.getTextContent();
                if (value != null && !value.isBlank()) {
                    languages.add(value.trim());
                }
            }
        }
        return languages;
    }

    private boolean isLanguageNode(Node node) {
        Node parent = node.getParentNode();
        while (parent != null) {
            String localName = parent.getLocalName();
            if ("tLanguage".equals(localName) || "Language".equals(localName)) {
                return true;
            }
            if ("tCountryInfo".equals(localName) || "CountryInfo".equals(localName)) {
                return false;
            }
            parent = parent.getParentNode();
        }
        return false;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private String extractTextContent(String xml, String localName) {
        try {
            return textByLocalName(parseXml(xml), localName);
        } catch (Exception ex) {
            throw new SoapServiceException("Failed to parse SOAP response element: " + localName, ex);
        }
    }

    private String textByLocalName(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            nodes = document.getElementsByTagName(localName);
        }
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback;
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    @SuppressWarnings("unused")
    private String fetchIsoCodeFallback(String countryName, Throwable throwable) {
        log.error("SOAP CountryISOCode fallback triggered country={} reason={}", countryName, throwable.getMessage());
        throw new SoapServiceException(
                "Country information service is temporarily unavailable. Please try again later.",
                throwable
        );
    }

    @SuppressWarnings("unused")
    private FullCountryInfoData fetchFullInfoFallback(String isoCode, Throwable throwable) {
        log.error("SOAP FullCountryInfo fallback triggered isoCode={} reason={}", isoCode, throwable.getMessage());
        throw new SoapServiceException(
                "Country information service is temporarily unavailable. Please try again later.",
                throwable
        );
    }
}
