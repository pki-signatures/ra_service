package com.ivan.ra.service.controller;

import com.ivan.ra.service.config.ra.profile.SubjectInfoVO;
import com.ivan.ra.service.constants.RaServiceConstants;
import com.ivan.ra.service.model.RegistrationAuthorityOperator;
import com.ivan.ra.service.model.RegistrationAuthorityRelyingParty;
import com.ivan.ra.service.model.RegistrationAuthorityRequest;
import com.ivan.ra.service.model.RegistrationAuthorityRequestPK;
import com.ivan.ra.service.model.RequestSubjectInfo;
import com.ivan.ra.service.repository.RaRequestRepository;
import com.ivan.ra.service.service.ClientCertAuthService;
import com.ivan.ra.service.service.RaProfileCompatibilityChecker;
import com.ivan.ra.service.vo.RaRequest;
import com.ivan.ra.service.vo.RegisterRaResponse;
import com.ivan.ra.service.vo.RevokeRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

@RestController
public class RaRpRequestController {

    @Autowired
    ErrorResponse errorResponse;

    @Autowired
    ClientCertAuthService authService;

    @Autowired
    RaProfileCompatibilityChecker profileCompatibilityChecker;

    @Autowired
    RaRequestRepository raRequestRepository;

    @Value("${validation.docs.path}")
    private String validationDocDirPath;

    private static final Logger logger = LogManager.getLogger(RaRpRequestController.class);

    @PostMapping(value = "/ra/v1/registration/authority/rp/issue/request", produces = "application/json", consumes = "application/json")
    public ResponseEntity createRequest(@Valid @RequestBody RaRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("create issue certificate request received using RP");
            RegistrationAuthorityRelyingParty regAuthRpAuth = authService.authenticateRaRp(httpRequest);
            if (regAuthRpAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthRpAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA RP status is disabled");
            }
            if (regAuthRpAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            boolean raProfileValid = false;
            String[] raProfiles = regAuthRpAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getRaProfiles().split(":");
            for (String raProfile : raProfiles) {
                if (request.getRaProfile().equals(raProfile)) {
                    raProfileValid = true;
                }
            }
            if (!raProfileValid) {
                return errorResponse.generateErrorResponse("invalid RA profile");
            }

            SubjectInfoVO subjectInfo = null;
            try {
                subjectInfo = profileCompatibilityChecker.getFinalSubjectInfo(request.getRaProfile(), request);
            } catch (Exception ex) {
                logger.info(ex.getMessage());
                return errorResponse.generateErrorResponse(ex.getMessage());
            }

            String requestId = UUID.randomUUID().toString();

            RegistrationAuthorityRequestPK regAuthReqPk = new RegistrationAuthorityRequestPK();
            regAuthReqPk.setRegistrationAuthority(regAuthRpAuth.getRegistrationAuthorityPK().getRegistrationAuthority());
            regAuthReqPk.setId(requestId);

            RequestSubjectInfo requestSubjectInfo = new RequestSubjectInfo();
            requestSubjectInfo.setId(UUID.randomUUID().toString());
            requestSubjectInfo.setCommonName(subjectInfo.getCommonName());
            requestSubjectInfo.setGivenName(subjectInfo.getGivenName());
            requestSubjectInfo.setSurname(subjectInfo.getSurname());
            requestSubjectInfo.setCountry(subjectInfo.getCountry());
            requestSubjectInfo.setOrganization(subjectInfo.getOrganization());
            requestSubjectInfo.setOrganizationIdentifier(subjectInfo.getOrganizationIdentifier());
            requestSubjectInfo.setOrganizationUnit(subjectInfo.getOrganizationUnit());
            requestSubjectInfo.setSerialNumber(subjectInfo.getSerialNumber());

            RegistrationAuthorityRequest regAuthReq = new RegistrationAuthorityRequest();
            regAuthReq.setEmailAddress(request.getEmailAddress());
            regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_PENDING);
            regAuthReq.setMobileNo(request.getMobileNo());
            regAuthReq.setRaProfile(request.getRaProfile());
            regAuthReq.setRegistrationAuthorityRequestPK(regAuthReqPk);
            regAuthReq.setRequestSubjectInfo(requestSubjectInfo);
            raRequestRepository.save(regAuthReq);
            logger.info("new certificate issuance request having ID created successfully: " + requestId);

            RegisterRaResponse response = new RegisterRaResponse();
            response.setRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/rp/validation/info")
    public ResponseEntity uploadValidationDocs(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            logger.info("upload validation docs request received");
            if (!httpRequest.getContentType().equals("application/zip")) {
                return errorResponse.generateErrorResponse("request content type must be application/zip");
            }
            String requestId = httpRequest.getHeader("request_id");
            if (StringUtils.isBlank(requestId)) {
                return errorResponse.generateErrorResponse("invalid request id");
            }
            RegistrationAuthorityRelyingParty regAuthRpAuth = authService.authenticateRaRp(httpRequest);
            if (regAuthRpAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthRpAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA RP status is disabled");
            }
            if (regAuthRpAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(requestId,
                    regAuthRpAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + requestId);
                return errorResponse.generateErrorResponse("no request with this ID already present: " + requestId);
            }
            byte[] validationDocsZipBytes = IOUtils.toByteArray(httpRequest.getInputStream());
            FileOutputStream fout = new FileOutputStream(validationDocDirPath +
                    regAuthRpAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName() + requestId + ".zip");
            fout.write(validationDocsZipBytes);
            fout.close();

            logger.info("validation docs uploaded successfully");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/rp/revoke/request", produces = "application/json", consumes = "application/json")
    public ResponseEntity revokeRequest(@Valid @RequestBody RevokeRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("revoke request received");
            RegistrationAuthorityRelyingParty regAuthRpAuth = authService.authenticateRaRp(httpRequest);
            if (regAuthRpAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthRpAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA RP status is disabled");
            }
            if (regAuthRpAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                    regAuthRpAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            regAuthReq.setStatus(RaServiceConstants.REVOKE_REQUEST_PENDING);
            raRequestRepository.save(regAuthReq);
            logger.info("revoke revoke successfully created");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
