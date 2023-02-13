package com.ivan.ra.service.clients;

import com.ivan.ra.service.controller.RaRequestController;
import com.ivan.ra.service.https.HttpsClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SsaClient {

    @Value("${ssa.client.auth.p12.path}")
    private String clientAuthP12Path;

    @Value("${ssa.client.auth.p12.password}")
    private String clientAuthP12Password;

    @Value("${ssa.service.url}")
    private String ssaServiceUrl;

    @Value("${trust.store.path}")
    private String trustStorePath;

    @Value("${trust.store.password}")
    private String trustStorePassword;

    private static final Logger logger = LogManager.getLogger(SsaClient.class);

    public String sendInitializeRequest(String mobileNo, String raName, String requestId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("request_id", requestId);
        body.put("ra_name", raName);
        body.put("mobile_no", mobileNo);

        HttpsClient httpsClient = new HttpsClient(clientAuthP12Path, clientAuthP12Password, trustStorePath, trustStorePassword);
        httpsClient.sendHttpPostRequest(ssaServiceUrl+"/ssa/v1/initialize", body.toJSONString().getBytes());
        int httpStatus = httpsClient.getHttpStatus();
        logger.info("HTTP status code: " + httpStatus);
        String responseBody = httpsClient.getResponseBody();

        if (httpStatus == 400) {
            JSONParser responseParser = new JSONParser();
            JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
            JSONArray errors = (JSONArray) responseJson.get("errors");
            throw new SsaException((String)errors.get(0));
        } else if (httpStatus == 500) {
            throw new Exception("internal error occurred during processing of request");
        }
        JSONParser responseParser = new JSONParser();
        JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
        return (String)responseJson.get("ssa_id");
    }

    public String keyPairGenRequest(String ssaId, String pin, String otp) throws Exception {
        JSONObject body = new JSONObject();
        body.put("ssa_id", ssaId);
        body.put("pin", pin);
        body.put("otp", otp);

        HttpsClient httpsClient = new HttpsClient(clientAuthP12Path, clientAuthP12Password, trustStorePath, trustStorePassword);
        httpsClient.sendHttpPostRequest(ssaServiceUrl+"/ssa/v1/csr", body.toJSONString().getBytes());
        int httpStatus = httpsClient.getHttpStatus();
        logger.info("HTTP status code: " + httpStatus);
        String responseBody = httpsClient.getResponseBody();

        if (httpStatus == 400) {
            JSONParser responseParser = new JSONParser();
            JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
            JSONArray errors = (JSONArray) responseJson.get("errors");
            throw new SsaException((String)errors.get(0));
        } else if (httpStatus == 500) {
            throw new Exception("internal error occurred during processing of request");
        }
        JSONParser responseParser = new JSONParser();
        JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
        return (String)responseJson.get("csr");
    }

    public void importCertificate(String ssaId, String endEntityCert, String issuerCert) throws Exception {
        JSONObject body = new JSONObject();
        body.put("ssa_id", ssaId);
        body.put("end_entity_certificate", endEntityCert);
        body.put("issuer_certificate", issuerCert);

        HttpsClient httpsClient = new HttpsClient(clientAuthP12Path, clientAuthP12Password, trustStorePath, trustStorePassword);
        httpsClient.sendHttpPostRequest(ssaServiceUrl+"/ssa/v1/certificate", body.toJSONString().getBytes());
        int httpStatus = httpsClient.getHttpStatus();
        logger.info("HTTP status code: " + httpStatus);
        if (httpStatus == 400) {
            String responseBody = httpsClient.getResponseBody();

            JSONParser responseParser = new JSONParser();
            JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
            JSONArray errors = (JSONArray) responseJson.get("errors");
            throw new SsaException((String)errors.get(0));
        } else if (httpStatus == 500) {
            throw new Exception("internal error occurred during processing of request");
        }
    }

    public void sendOtp(String ssaId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("ssa_id", ssaId);

        HttpsClient httpsClient = new HttpsClient(clientAuthP12Path, clientAuthP12Password, trustStorePath, trustStorePassword);
        httpsClient.sendHttpPostRequest(ssaServiceUrl+"/ssa/v1/send/otp", body.toJSONString().getBytes());
        int httpStatus = httpsClient.getHttpStatus();
        logger.info("HTTP status code: " + httpStatus);
        if (httpStatus == 400) {
            String responseBody = httpsClient.getResponseBody();

            JSONParser responseParser = new JSONParser();
            JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
            JSONArray errors = (JSONArray) responseJson.get("errors");
            throw new SsaException((String)errors.get(0));
        } else if (httpStatus == 500) {
            throw new Exception("internal error occurred during processing of request");
        }
    }

    public void revokeCertificate(String ssaId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("ssa_id", ssaId);

        HttpsClient httpsClient = new HttpsClient(clientAuthP12Path, clientAuthP12Password, trustStorePath, trustStorePassword);
        httpsClient.sendHttpPostRequest(ssaServiceUrl+"/ssa/v1/certificate/revoke", body.toJSONString().getBytes());
        int httpStatus = httpsClient.getHttpStatus();
        logger.info("HTTP status code: " + httpStatus);
        if (httpStatus == 400) {
            String responseBody = httpsClient.getResponseBody();

            JSONParser responseParser = new JSONParser();
            JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
            JSONArray errors = (JSONArray) responseJson.get("errors");
            throw new SsaException((String)errors.get(0));
        } else if (httpStatus == 500) {
            throw new Exception("internal error occurred during processing of request");
        }
    }
}
