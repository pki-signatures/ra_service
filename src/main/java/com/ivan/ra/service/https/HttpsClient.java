package com.ivan.ra.service.https;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class HttpsClient {

    private HttpClientBuilder builder;
    private Registry<ConnectionSocketFactory> registry;

    private int httpStatus;

    private String responseBody;

    public HttpsClient(String clientPfxPath, String password, String trustStorePath, String trustStorePassword)
            throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream(clientPfxPath),password.toCharArray());

        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(ks, password.toCharArray())
                .loadTrustMaterial(new File(trustStorePath), trustStorePassword.toCharArray())
                .build();

        builder = HttpClientBuilder.create();
        SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(
                sslContext, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        builder.setSSLSocketFactory(sslConnectionFactory);
        registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslConnectionFactory)
                .build();
    }

    public void sendHttpPostRequest(String url, byte[] requestBody) throws Exception {
        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
        builder.setConnectionManager(ccm);

        HttpPost request = new HttpPost(url);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(-1)
                .setConnectTimeout(-1)
                .setSocketTimeout(-1)
                .build();
        request.setConfig(requestConfig);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");

        request.setEntity(new ByteArrayEntity(requestBody));

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(request)) {

            httpStatus = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                responseBody = EntityUtils.toString(entity);
            }
        }
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
