package com.example.tcu_car.client;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

public class ThingsBoardClient {

    private static final Logger log = LoggerFactory.getLogger(ThingsBoardClient.class);
    private final CustomRestClient customRestClient;

    public ThingsBoardClient(Context context) {
        log.debug("Initializing ThingsBoardClient");

        // Get SSLContext without certificate verification
        SSLUtils.SSLContextAndTrustManager sslContextAndTrustManager = SSLUtils.getSSLContext();
        if (sslContextAndTrustManager == null) {
            log.error("Failed to initialize SSL context");
            throw new RuntimeException("Failed to initialize SSL context");
        }
        SSLContext sslContext = sslContextAndTrustManager.getSslContext();

        String baseURL = "https://revecom.vedecom.fr:8080";
        try {
            this.customRestClient = new CustomRestClient(baseURL, sslContext);
            log.debug("CustomRestClient initialized with baseURL: {}", baseURL);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to initialize SSL context", e);
            throw new RuntimeException("Failed to initialize SSL context", e);
        }

        String username = "tenant@thingsboard.org";
        String password = "tenant";
        try {
            this.customRestClient.login(username, password);
            log.info("Logged into ThingsBoardClient successfully with username: {}", username);
        } catch (RestClientException e) {
            log.error("Error logging into ThingsBoardClient", e);
        }
    }

    public String getRawResponseForDevices() {
        log.debug("Fetching raw response for devices");
        try {
            String response = customRestClient.getRestTemplate().getForObject(customRestClient.baseURL + "/api/tenant/devices?pageSize=10&page=0", String.class);
            log.debug("Raw response for devices: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error getting raw response for devices", e);
            return "Error: " + e.getMessage();
        }
    }

    // Additional methods to interact with the ThingsBoard API using customRestClient
}
