package com.example.tcu_car.client;

import android.os.Build;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CustomRestClient implements Closeable {
    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    private static final long AVG_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    protected static final String ACTIVATE_TOKEN_REGEX = "/api/noauth/activate?activateToken=";
    private static final Logger log = LoggerFactory.getLogger(CustomRestClient.class);

    private final ExecutorService service = Executors.newWorkStealingPool(10);
    protected final RestTemplate restTemplate;
    protected final RestTemplate loginRestTemplate;
    protected final String baseURL;

    private String username;
    private String password;
    private String mainToken;
    private String refreshToken;
    private long mainTokenExpTs;
    private long refreshTokenExpTs;
    private long clientServerTimeDiff;

    public CustomRestClient(String baseURL, SSLContext sslContext) throws NoSuchAlgorithmException {
        log.debug("Initializing CustomRestClient with SSL context");
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        this.loginRestTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        this.baseURL = baseURL;
        initialize();
    }

    public CustomRestClient(RestTemplate restTemplate, String baseURL) {
        log.debug("Initializing CustomRestClient with existing RestTemplate");
        this.restTemplate = restTemplate;
        this.loginRestTemplate = new RestTemplate(restTemplate.getRequestFactory());
        this.baseURL = baseURL;
        initialize();
    }

    private void initialize() {
        log.debug("Adding interceptors to RestTemplate");
        this.restTemplate.getInterceptors().add((request, bytes, execution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            long calculatedTs = System.currentTimeMillis() + clientServerTimeDiff + AVG_REQUEST_TIMEOUT;
            if (calculatedTs > mainTokenExpTs) {
                synchronized (CustomRestClient.this) {
                    if (calculatedTs > mainTokenExpTs) {
                        if (calculatedTs < refreshTokenExpTs) {
                            log.debug("Token expired, refreshing token");
                            refreshToken();
                        } else {
                            log.debug("Refresh token expired, logging in again");
                            doLogin();
                        }
                    }
                }
            }
            wrapper.getHeaders().set(JWT_TOKEN_HEADER_PARAM, "Bearer " + mainToken);
            return execution.execute(wrapper, bytes);
        });
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public String getToken() {
        return mainToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void refreshToken() {
        log.debug("Refreshing token");
        Map<String, String> refreshTokenRequest = new HashMap<>();
        refreshTokenRequest.put("refreshToken", refreshToken);
        long ts = System.currentTimeMillis();
        try {
            ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/token", refreshTokenRequest, JsonNode.class);
            setTokenInfo(ts, tokenInfo.getBody());
            log.info("Token refreshed successfully");
        } catch (RestClientException e) {
            log.error("Error refreshing token", e);
        }
    }

    public void login(String username, String password) {
        log.debug("Logging in with username: {}", username);
        this.username = username;
        this.password = password;
        doLogin();
    }

    private void doLogin() {
        long ts = System.currentTimeMillis();
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        try {
            ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/login", loginRequest, JsonNode.class);
            setTokenInfo(ts, tokenInfo.getBody());
            log.info("Logged in successfully with username: {}", username);
        } catch (RestClientException e) {
            log.error("Error logging in", e);
        }
    }

    private synchronized void setTokenInfo(long ts, JsonNode tokenInfo) {
        log.debug("Setting token information");
        this.mainToken = tokenInfo.get("token").asText();
        this.refreshToken = tokenInfo.get("refreshToken").asText();

        DecodedJWT decodedMainToken = JWT.decode(this.mainToken);
        DecodedJWT decodedRefreshToken = JWT.decode(this.refreshToken);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mainTokenExpTs = decodedMainToken.getExpiresAt().toInstant().toEpochMilli();
            this.refreshTokenExpTs = decodedRefreshToken.getExpiresAt().toInstant().toEpochMilli();
            this.clientServerTimeDiff = decodedMainToken.getIssuedAt().toInstant().toEpochMilli() - ts;
            log.debug("Token expiry timestamps set: mainTokenExpTs={}, refreshTokenExpTs={}", mainTokenExpTs, refreshTokenExpTs);
        }
    }

    @Override
    public void close() {
        log.debug("Shutting down ExecutorService");
        service.shutdown();
    }

    // Add additional methods to replicate the functionality you need from RestClient
}
