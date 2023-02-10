package com.ivan.ra.service.controller;

import com.ivan.ra.service.cache.RaServiceCache;
import com.ivan.ra.service.clients.CaClient;
import com.ivan.ra.service.clients.SsaClient;
import com.ivan.ra.service.clients.SsaException;
import com.ivan.ra.service.constants.RaServiceConstants;
import com.ivan.ra.service.model.RegistrationAuthorityRequest;
import com.ivan.ra.service.repository.RaRequestRepository;
import com.ivan.ra.service.service.EmailService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;

@RestController
@Validated
public class CertRegistrationController {

    @Autowired
    RaRequestRepository raRequestRepository;

    @Autowired
    SsaClient ssaClient;

    @Autowired
    CaClient caClient;

    @Autowired
    EmailService emailService;

    @Value("${web.pages.dir.path}")
    private String webPagesDirPath;

    @Value("${email.templates.dir.path}")
    private String emailTemplatesDirPath;

    @Value("${ra.service.url}")
    private String raServiceUrl;

    private static final Logger logger = LogManager.getLogger(CertRegistrationController.class);

    @PostMapping(value = "/ra/v1/register/{ra_name}/{request_id}")
    public void register(@RequestParam("ra_name") @NotBlank String raName, @RequestParam("request_id") @NotBlank String requestId,
                         HttpServletResponse httpResponse) {
        try {
            logger.info("certificate register request received");
            logger.info("RA name: " + raName);
            logger.info("Request Id: " + requestId);
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(requestId, raName);
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + requestId);
                sendToErrorPage(httpResponse, raName, "Invalid RA name or request ID");
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (regAuthReq.getRegistrationAuthorityRequestPK().getRegistrationAuthority().getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                logger.info("RA status is disabled");
                sendToErrorPage(httpResponse, raName, "Invalid RA");
            }

            Path path = Paths.get(webPagesDirPath + raName + "/input_signing_credential.html");
            String pageTemplate = new String(Files.readAllBytes(path));
            pageTemplate = pageTemplate.replace("<RA_NAME>", raName);
            pageTemplate = pageTemplate.replace("<REQUEST_ID>", requestId);
            pageTemplate = pageTemplate.replace("<INVALID_PIN>", "");
            pageTemplate = pageTemplate.replace("<INVALID_OTP>", "");

            httpResponse.setContentType("text/html");
            httpResponse.setStatus(HttpServletResponse.SC_OK);

            PrintWriter printWriter = httpResponse.getWriter();
            printWriter.write(pageTemplate);
            printWriter.flush();
            printWriter.close();
            logger.info("certificate register response sent");
        } catch (Exception ex) {
            logger.error("", ex);
            try {
                sendToErrorPage(httpResponse, raName, "Internal error occurred during processing of request");
            } catch (Exception ex1) {
                logger.error("", ex1);
            }
        }
    }

    @PostMapping(value = "/ra/v1/cert/register")
    public void registerCert(@RequestParam("pin") String pin, @RequestParam("otp") String otp,
                             @RequestParam("ra_name") String raName, @RequestParam("request_id") String requestId,
                             HttpServletResponse httpResponse) {
        try {
            logger.info("certificate register request received");
            logger.info("RA name: " + raName);
            logger.info("Request Id: " + requestId);
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(requestId, raName);
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + requestId);
                sendToErrorPage(httpResponse, raName, "Invalid RA name or request ID");
                return;
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (regAuthReq.getRegistrationAuthorityRequestPK().getRegistrationAuthority().getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                logger.info("RA status is disabled");
                sendToErrorPage(httpResponse, raName, "Invalid RA");
                return;
            }

            String csr = null;
            try {
                csr = ssaClient.keyPairGenRequest(regAuthReq.getSsaId(), pin, otp);
                regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_KEYPAIR_GENERATED_SSA);
                regAuthReq.setCsr(csr);
                raRequestRepository.save(regAuthReq);

                Path path = Paths.get(emailTemplatesDirPath + raName + "/user_pin.html");
                String emailTemplate = new String(Files.readAllBytes(path));
                emailTemplate = emailTemplate.replace("<PIN>", pin);
                emailService.sendCertRegisterPinEmail(regAuthReq.getEmailAddress(), emailTemplate);
                logger.info("pin email sent for request id: " + requestId + " and RA: " + raName);
            } catch (SsaException ex) {
                if (ex.getMessage().contains("use random digits and non sequential digits pin")) {
                    sendInputValidationError(httpResponse, raName, requestId, ex.getMessage(), null);
                } else if (ex.getMessage().contains("invalid OTP value")) {
                    sendInputValidationError(httpResponse, raName, requestId, null, ex.getMessage());
                } else {
                    sendToErrorPage(httpResponse, raName, "error on generation of natural person signing credentials");
                }
                return;
            } catch (Exception ex) {
                sendToErrorPage(httpResponse, raName, "error generation of natural person signing credentials");
                return;
            }

            String[] certChain = null;
            try {
                certChain = caClient.sendCsrToCa(csr, regAuthReq.getRequestSubjectInfo(),
                        RaServiceCache.getRaProfile(regAuthReq.getRaProfile()).getCertProfile());
                regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_CERT_ISSUED_CA);
                regAuthReq.setEndEntityCertificate(certChain[0]);
                regAuthReq.setIssuerCertificate(certChain[1]);
                raRequestRepository.save(regAuthReq);
            } catch (Exception ex) {
                logger.error("", ex);
                sendToErrorPage(httpResponse, raName, "error generation of natural person signing credentials");
                return;
            }

            SecureRandom secureRandom = new SecureRandom();
            int randomNumber = secureRandom.nextInt(999999);
            String revocationCode = String.format("%06d", randomNumber);
            try {
                ssaClient.importCertificate(regAuthReq.getSsaId(), certChain[0], certChain[1]);
                regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_CERT_IMPORTED_SSA);
                regAuthReq.setRevocationCode(revocationCode);
                raRequestRepository.save(regAuthReq);
            } catch (Exception ex) {
                logger.error("", ex);
                sendToErrorPage(httpResponse, raName, "error generation of natural person signing credentials");
                return;
            }

            // Send registration success email
            Path path = Paths.get(emailTemplatesDirPath + raName + "/certificate_registration_success.html");
            String emailTemplate = new String(Files.readAllBytes(path));
            String certRevocationLink = raServiceUrl + "ra/v1/revoke/" +raName+"/"+requestId+"/"+revocationCode;
            emailTemplate = emailTemplate.replace("<CERTIFICATE_REVOCATION_LINK>", certRevocationLink);
            emailService.sendCertRegisterSuccessEmail(regAuthReq.getEmailAddress(), emailTemplate);
            logger.info("email sent for request id: " + requestId + " and RA: " + raName);
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    @PostMapping(value = "/ra/v1/otp")
    public void sendOtp(@RequestParam("ra_name") String raName, @RequestParam("request_id") String requestId,
                        HttpServletResponse httpResponse) {
        try {
            logger.info("send otp request received");
            logger.info("RA name: " + raName);
            logger.info("Request Id: " + requestId);
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(requestId, raName);
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + requestId);
                sendToErrorPage(httpResponse, raName, "Invalid RA name or request ID");
                return;
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (regAuthReq.getRegistrationAuthorityRequestPK().getRegistrationAuthority().getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                logger.info("RA status is disabled");
                sendToErrorPage(httpResponse, raName, "Invalid RA");
                return;
            }
            ssaClient.sendOtp(regAuthReq.getSsaId());
            logger.info("send otp response sent");
        } catch (Exception ex) {
            logger.error("", ex);
            try {
                sendToErrorPage(httpResponse, raName, "error sending the otp");
            } catch (Exception ex1) {}
        }
    }

    @PostMapping(value = "/ra/v1/revoke/{ra_name}/{request_id}/{revocation_code}")
    public void register(@RequestParam("ra_name") @NotBlank String raName, @RequestParam("request_id") @NotBlank String requestId,
                         @RequestParam("revocation_code") @NotBlank String revocationCode, HttpServletResponse httpResponse) {
        try {
            logger.info("certificate revoke request received");
            logger.info("RA name: " + raName);
            logger.info("Request Id: " + requestId);
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(requestId, raName);
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + requestId);
                sendToErrorPage(httpResponse, raName, "Invalid RA name or request ID");
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (regAuthReq.getRegistrationAuthorityRequestPK().getRegistrationAuthority().getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                logger.info("RA status is disabled");
                sendToErrorPage(httpResponse, raName, "Invalid RA");
            }
            if (!revocationCode.equals(regAuthReq.getRevocationCode())) {
                logger.info("Invalid revocation code");
                sendToErrorPage(httpResponse, raName, "Invalid revocation code");
            }
            regAuthReq.setStatus(RaServiceConstants.REVOKE_REQUEST_PENDING);
            raRequestRepository.save(regAuthReq);

            Path path = Paths.get(webPagesDirPath + raName + "/revoke_request_pending.html");
            String pageTemplate = new String(Files.readAllBytes(path));
            pageTemplate = pageTemplate.replace("<REQUEST_ID>", requestId);

            httpResponse.setContentType("text/html");
            httpResponse.setStatus(HttpServletResponse.SC_OK);

            PrintWriter printWriter = httpResponse.getWriter();
            printWriter.write(pageTemplate);
            printWriter.flush();
            printWriter.close();
            logger.info("certificate register response sent");
        } catch (Exception ex) {
            logger.error("", ex);
            try {
                sendToErrorPage(httpResponse, raName, "Internal error occurred during processing of request");
            } catch (Exception ex1) {
                logger.error("", ex1);
            }
        }
    }

    private void sendToErrorPage(HttpServletResponse httpResponse, String raName, String errorMessage) throws Exception {
        Path path = Paths.get(webPagesDirPath + raName + "/error_page.html");
        String pageTemplate = new String(Files.readAllBytes(path));
        pageTemplate = pageTemplate.replace("<ERROR_MESSAGE>", errorMessage);

        httpResponse.setContentType("text/html");
        httpResponse.setStatus(HttpServletResponse.SC_OK);

        PrintWriter printWriter = httpResponse.getWriter();
        printWriter.write(pageTemplate);
        printWriter.flush();
        printWriter.close();
    }

    private void sendInputValidationError(HttpServletResponse httpResponse, String raName, String requestId,
                                          String invalidPinErrorMessage, String invalidOtpErrorMessage) throws Exception {
        Path path = Paths.get(webPagesDirPath + raName + "/input_signing_credential.html");
        String pageTemplate = new String(Files.readAllBytes(path));
        pageTemplate = pageTemplate.replace("<RA_NAME>", raName);
        pageTemplate = pageTemplate.replace("<REQUEST_ID>", requestId);

        if (!StringUtils.isBlank(invalidPinErrorMessage)) {
            pageTemplate = pageTemplate.replace("<INVALID_PIN>", "");
        }
        if (!StringUtils.isBlank(invalidOtpErrorMessage)) {
            pageTemplate = pageTemplate.replace("<INVALID_OTP>", "");
        }
        httpResponse.setContentType("text/html");
        httpResponse.setStatus(HttpServletResponse.SC_OK);

        PrintWriter printWriter = httpResponse.getWriter();
        printWriter.write(pageTemplate);
        printWriter.flush();
        printWriter.close();
    }
}
