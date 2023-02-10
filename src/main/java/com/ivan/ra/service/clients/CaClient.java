package com.ivan.ra.service.clients;

import com.ivan.ra.service.https.HttpsClient;
import com.ivan.ra.service.model.RequestSubjectInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CaClient {

    @Value("${ca.client.auth.p12.path}")
    private String clientAuthP12Path;

    @Value("${ca.client.auth.p12.password}")
    private String clientAuthP12Password;

    @Value("${ca.service.url}")
    private String caServiceUrl;

    @Value("${trust.store.path}")
    private String trustStorePath;

    @Value("${trust.store.password}")
    private String trustStorePassword;

    private static final Logger logger = LogManager.getLogger(SsaClient.class);

    public String[] sendCsrToCa(String csr, RequestSubjectInfo subject, String certProfile) throws Exception {
        JSONObject subjectInfo = new JSONObject();
        if (!StringUtils.isBlank(subject.getCommonName())) {
            subjectInfo.put("common_name", subject.getCommonName());
        }
        if (!StringUtils.isBlank(subject.getGivenName())) {
            subjectInfo.put("given_name", subject.getGivenName());
        }
        if (!StringUtils.isBlank(subject.getSurname())) {
            subjectInfo.put("surname", subject.getSurname());
        }
        if (!StringUtils.isBlank(subject.getOrganization())) {
            subjectInfo.put("organization", subject.getOrganization());
        }
        if (!StringUtils.isBlank(subject.getOrganizationUnit())) {
            subjectInfo.put("organization_unit", subject.getOrganizationUnit());
        }
        if (!StringUtils.isBlank(subject.getOrganizationIdentifier())) {
            subjectInfo.put("organization_identifier", subject.getOrganizationIdentifier());
        }
        if (!StringUtils.isBlank(subject.getCountry())) {
            subjectInfo.put("country", subject.getCountry());
        }
        if (!StringUtils.isBlank(subject.getSerialNumber())) {
            subjectInfo.put("serial_number", subject.getSerialNumber());
        }
        JSONObject body = new JSONObject();
        body.put("certificate_profile", certProfile);
        body.put("subject_info", subjectInfo);
        body.put("csr", csr);

        HttpsClient httpsClient = new HttpsClient(clientAuthP12Path, clientAuthP12Password, trustStorePath, trustStorePassword);
        httpsClient.sendHttpPostRequest(caServiceUrl+"/ca/v1/issue/cert", body.toJSONString().getBytes());
        int httpStatus = httpsClient.getHttpStatus();
        logger.info("HTTP status code: " + httpStatus);
        String responseBody = httpsClient.getResponseBody();

        if (httpStatus == 400) {
            JSONParser responseParser = new JSONParser();
            JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
            String error = (String) responseJson.get("error_description");
            throw new Exception(error);
        } else if (httpStatus == 500) {
            throw new Exception("internal error occurred during processing of request");
        }
        JSONParser responseParser = new JSONParser();
        JSONObject responseJson = (JSONObject) responseParser.parse(responseBody);
        String endEntityCert = (String)responseJson.get("end_entity_certificate");
        String issuerCert = (String)responseJson.get("ca_certificate");
        return new String[]{endEntityCert, issuerCert};
    }
}
