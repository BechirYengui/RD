package com.example.tcu_car.client;

import android.util.Log;

import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLUtils {

    private static final String TAG = "SSLUtils";

    public static SSLContextAndTrustManager getSSLContext() {
        Log.d(TAG, "Initializing SSL context");
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            Log.d(TAG, "checkClientTrusted: authType=" + authType);
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            Log.d(TAG, "checkServerTrusted: authType=" + authType);
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            Log.d(TAG, "getAcceptedIssuers called");
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            Log.d(TAG, "SSL context initialized successfully");

            return new SSLContextAndTrustManager(sslContext, (X509TrustManager) trustAllCerts[0]);
        } catch (Exception e) {
            Log.e(TAG, "Error creating SSL context", e);
            return null;
        }
    }

    public static class SSLContextAndTrustManager {
        private SSLContext sslContext;
        private X509TrustManager trustManager;

        public SSLContextAndTrustManager(SSLContext sslContext, X509TrustManager trustManager) {
            Log.d(TAG, "SSLContextAndTrustManager created");
            this.sslContext = sslContext;
            this.trustManager = trustManager;
        }

        public SSLContext getSslContext() {
            Log.d(TAG, "getSslContext called");
            return sslContext;
        }

        public X509TrustManager getTrustManager() {
            Log.d(TAG, "getTrustManager called");
            return trustManager;
        }
    }
}
