package com.ivan.ra.service.service;

import com.ivan.ra.service.cache.RaServiceCache;
import com.ivan.ra.service.controller.ErrorResponse;
import com.ivan.ra.service.model.RegistrationAuthorityAdmin;
import com.ivan.ra.service.model.RegistrationAuthorityOperator;
import com.ivan.ra.service.model.RegistrationAuthorityRelyingParty;
import com.ivan.ra.service.repository.RaAdminRepository;
import com.ivan.ra.service.repository.RaOperatorRepository;
import com.ivan.ra.service.repository.RaRpRepository;
import com.ivan.ra.service.vo.Errors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

@Component
public class ClientCertAuthService {
    @Autowired
    RaAdminRepository raAdminRepository;
    @Autowired
    RaOperatorRepository raOperatorRepository;
    @Autowired
    RaRpRepository raRpRepository;
    @Autowired
    ErrorResponse errorResponse;

    private static final Logger logger = LogManager.getLogger(ClientCertAuthService.class);

    public String authenticateSuperAdmins(HttpServletRequest httpRequest) throws Exception {
        X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            logger.info("TLS client certificate must be present in request");
            return null;
        }
        logger.info("TLS client certificate received in request with subject :" + certs[0].getSubjectDN());

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(certs[0].getEncoded());
        String digest = Base64.getEncoder().encodeToString(md.digest());

        if (RaServiceCache.getAccessControlSettingsConfig(digest) == null) {
            logger.info("TLS client authentication failed");
            return null;
        } else {
            logger.info("TLS client authentication passed");
            return digest;
        }
    }

    public RegistrationAuthorityAdmin authenticateRaAdmin(HttpServletRequest httpRequest) throws Exception {
        X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            logger.info("TLS client certificate must be present in request");
            return null;
        }
        logger.info("TLS client certificate received in request with subject :" + certs[0].getSubjectDN());

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(certs[0].getEncoded());
        String digest = Base64.getEncoder().encodeToString(md.digest());

        List<RegistrationAuthorityAdmin> raAdminsList = raAdminRepository.findRaAdminByClientCert(digest);
        if (raAdminsList.size() == 0) {
            logger.info("TLS client authentication failed");
            return null;
        } else {
            return raAdminsList.get(0);
        }
    }

    public RegistrationAuthorityOperator authenticateRaOperator(HttpServletRequest httpRequest) throws Exception {
        X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            logger.info("TLS client certificate must be present in request");
            return null;
        }
        logger.info("TLS client certificate received in request with subject :" + certs[0].getSubjectDN());

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(certs[0].getEncoded());
        String digest = Base64.getEncoder().encodeToString(md.digest());

        List<RegistrationAuthorityOperator> raOperatorsList = raOperatorRepository.findRaOperatorByClientCert(digest);
        if (raOperatorsList.size() == 0) {
            logger.info("TLS client authentication failed");
            return null;
        } else {
            return raOperatorsList.get(0);
        }
    }


    public ResponseEntity isClientCertAlreadyConfigured(String certDigest) throws Exception {
        if (RaServiceCache.getAccessControlSettingsConfig(certDigest) != null) {
            logger.info("certificate already configured in access control config file");
            return errorResponse.generateErrorResponse("certificate already configured. Use other certificate");
        }
        List<RegistrationAuthorityAdmin> raAdmins = raAdminRepository.findRaAdminByClientCert(certDigest);
        if (raAdmins.size() != 0) {
            logger.info("certificate already configured under RA admin configurations");
            return errorResponse.generateErrorResponse("certificate already configured. Use other certificate");
        }
        List<RegistrationAuthorityOperator> raOps = raOperatorRepository.findRaOperatorByClientCert(certDigest);
        if (raOps.size() != 0) {
            logger.info("certificate already configured under RA operator configurations");
            return errorResponse.generateErrorResponse("certificate already configured. Use other certificate");
        }
        List<RegistrationAuthorityRelyingParty> raRps = raRpRepository.findRaRpByClientCert(certDigest);
        if (raRps.size() != 0) {
            logger.info("certificate already configured under RA relying party configurations");
            return errorResponse.generateErrorResponse("certificate already configured. Use other certificate");
        }
        return null;
    }
}
