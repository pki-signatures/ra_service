package com.ivan.ra.service.controller;

import com.ivan.ra.service.cache.RaServiceCache;
import com.ivan.ra.service.clients.CaClient;
import com.ivan.ra.service.clients.SsaClient;
import com.ivan.ra.service.config.ra.profile.SubjectInfoVO;
import com.ivan.ra.service.constants.RaServiceConstants;
import com.ivan.ra.service.model.QRegistrationAuthorityRequest;
import com.ivan.ra.service.model.RegistrationAuthorityOperator;
import com.ivan.ra.service.model.RegistrationAuthorityRequest;
import com.ivan.ra.service.model.RegistrationAuthorityRequestPK;
import com.ivan.ra.service.model.RequestSubjectInfo;
import com.ivan.ra.service.repository.RaRequestRepository;
import com.ivan.ra.service.service.ClientCertAuthService;
import com.ivan.ra.service.service.EmailService;
import com.ivan.ra.service.service.RaProfileCompatibilityChecker;
import com.ivan.ra.service.vo.ApproveRaRequest;
import com.ivan.ra.service.vo.ApproveRevokeRequest;
import com.ivan.ra.service.vo.ImportCertToSsaRequest;
import com.ivan.ra.service.vo.RaRequest;
import com.ivan.ra.service.vo.RegisterRaResponse;
import com.ivan.ra.service.vo.RejectRaRequest;
import com.ivan.ra.service.vo.RejectRevokeRequest;
import com.ivan.ra.service.vo.RevokeRequest;
import com.ivan.ra.service.vo.SearchRaRequest;
import com.ivan.ra.service.vo.SendCsrToCaRequest;
import com.ivan.ra.service.vo.SendRegSuccessEmailRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
public class RaRequestController {
    @Autowired
    ErrorResponse errorResponse;
    @Autowired
    ClientCertAuthService authService;
    @Autowired
    RaProfileCompatibilityChecker profileCompatibilityChecker;
    @Autowired
    RaRequestRepository raRequestRepository;
    @Autowired
    SsaClient ssaClient;

    @Autowired
    CaClient caClient;

    @Autowired
    EmailService emailService;
    @Value("${validation.docs.path}")
    private String validationDocDirPath;

    @Value("${ssa.client.auth.p12.path}")
    private String ssaClientAuthP12Path;

    @Value("${ssa.client.auth.p12.password}")
    private String ssaClientAuthP12Password;

    @Value("${ca.client.auth.p12.path}")
    private String caClientAuthP12Path;

    @Value("${ca.client.auth.p12.password}")
    private String caClientAuthP12Password;

    @Value("${email.templates.dir.path}")
    private String emailTemplatesDirPath;

    @Value("${web.pages.dir.path}")
    private String webPagesDirPath;

    @Value("${ra.service.url}")
    private String raServiceUrl;

    private static final Logger logger = LogManager.getLogger(RaRequestController.class);

    @PostMapping(value = "/ra/v1/registration/authority/issue/request", produces = "application/json", consumes = "application/json")
    public ResponseEntity createRequest(@Valid @RequestBody RaRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("create certificate issuance request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.CREATE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to create requests");
            }

            boolean raProfileValid = false;
            String[] raProfiles = regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
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
            regAuthReqPk.setRegistrationAuthority(regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority());
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

    @PutMapping(value = "/ra/v1/registration/authority/issue/request", produces = "application/json", consumes = "application/json")
    public ResponseEntity updateRequest(@Valid @RequestBody RaRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("update certificate issuance request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.UPDATE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to update requests");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getId(),
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!StringUtils.isBlank(request.getEmailAddress())) {
                regAuthReq.setEmailAddress(request.getEmailAddress());
            }
            if (!StringUtils.isBlank(request.getMobileNo())) {
                regAuthReq.setMobileNo(request.getMobileNo());
            }
            if (!StringUtils.isBlank(request.getStatus()) && !request.getStatus().equals(
                    RaServiceConstants.CERTIFICATE_REQUEST_PROCESSING)) {
                return errorResponse.generateErrorResponse("only allowed status: " + RaServiceConstants.CERTIFICATE_REQUEST_PROCESSING);
            } else {
                regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_PROCESSING);
            }

            RequestSubjectInfo subjectInfo = regAuthReq.getRequestSubjectInfo();
            if (subjectInfo != null) {
                if (!StringUtils.isBlank(subjectInfo.getCommonName()) && !StringUtils.isBlank(request.getCommonName())) {
                    subjectInfo.setCommonName(request.getCommonName());
                } else if (StringUtils.isBlank(subjectInfo.getCommonName()) && !StringUtils.isBlank(request.getCommonName())) {
                    return errorResponse.generateErrorResponse("not allowed to update common name");
                }

                if (!StringUtils.isBlank(subjectInfo.getGivenName()) && !StringUtils.isBlank(request.getGivenName())) {
                    subjectInfo.setCommonName(request.getGivenName());
                } else if (StringUtils.isBlank(subjectInfo.getGivenName()) && !StringUtils.isBlank(request.getGivenName())) {
                    return errorResponse.generateErrorResponse("not allowed to update given name");
                }

                if (!StringUtils.isBlank(subjectInfo.getSurname()) && !StringUtils.isBlank(request.getSurname())) {
                    subjectInfo.setCommonName(request.getSurname());
                } else if (StringUtils.isBlank(subjectInfo.getSurname()) && !StringUtils.isBlank(request.getSurname())) {
                    return errorResponse.generateErrorResponse("not allowed to update surname");
                }

                if (!StringUtils.isBlank(subjectInfo.getOrganization()) && !StringUtils.isBlank(request.getOrganization())) {
                    subjectInfo.setCommonName(request.getOrganization());
                } else if (StringUtils.isBlank(subjectInfo.getOrganization()) && !StringUtils.isBlank(request.getOrganization())) {
                    return errorResponse.generateErrorResponse("not allowed to update organization");
                }

                if (!StringUtils.isBlank(subjectInfo.getOrganizationUnit()) && !StringUtils.isBlank(request.getOrganizationUnit())) {
                    subjectInfo.setCommonName(request.getOrganizationUnit());
                } else if (StringUtils.isBlank(subjectInfo.getOrganizationUnit()) && !StringUtils.isBlank(request.getOrganizationUnit())) {
                    return errorResponse.generateErrorResponse("not allowed to update organization unit");
                }

                if (!StringUtils.isBlank(subjectInfo.getOrganizationIdentifier()) && !StringUtils.isBlank(request.getOrganizationIdentifier())) {
                    subjectInfo.setCommonName(request.getOrganizationIdentifier());
                } else if (StringUtils.isBlank(subjectInfo.getOrganizationIdentifier()) && !StringUtils.isBlank(request.getOrganizationIdentifier())) {
                    return errorResponse.generateErrorResponse("not allowed to update organization identifier");
                }

                if (!StringUtils.isBlank(subjectInfo.getCountry()) && !StringUtils.isBlank(request.getCountry())) {
                    subjectInfo.setCommonName(request.getCountry());
                } else if (!StringUtils.isBlank(subjectInfo.getCountry()) && !StringUtils.isBlank(request.getCountry())) {
                    return errorResponse.generateErrorResponse("not allowed to update country");
                }

                if (!StringUtils.isBlank(subjectInfo.getSerialNumber()) && !StringUtils.isBlank(request.getSerialNumber())) {
                    subjectInfo.setCommonName(request.getSerialNumber());
                } else if (!StringUtils.isBlank(subjectInfo.getSerialNumber()) && !StringUtils.isBlank(request.getSerialNumber())) {
                    return errorResponse.generateErrorResponse("not allowed to update serial number");
                }
            }
            raRequestRepository.save(regAuthReq);
            logger.info("certificate issuance request having ID updated successfully: " + request.getId());
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/validation/info")
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
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.CREATE_REQUESTS) &&
                    !regAuthOperatorAuth.getRole().contains(RaServiceConstants.UPDATE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to upload validation docs");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(requestId,
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + requestId);
                return errorResponse.generateErrorResponse("no request with this ID already present: " + requestId);
            }
            byte[] validationDocsZipBytes = IOUtils.toByteArray(httpRequest.getInputStream());
            FileOutputStream fout = new FileOutputStream(validationDocDirPath +
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName() + requestId + ".zip");
            fout.write(validationDocsZipBytes);
            fout.close();

            logger.info("validation docs uploaded successfully");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/validation/info/{id}", produces = "application/json")
    public void getValidationDocs(@RequestParam("id") @NotBlank String requestId, HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        try {
            logger.info("get validation docs request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                errorResponse.sendErrorResponse("TLS client certificate authentication failed", httpResponse);
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                errorResponse.sendErrorResponse("RA operator status is disabled", httpResponse);
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                errorResponse.sendErrorResponse("RA status is disabled", httpResponse);
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.READ_REQUESTS)) {
                errorResponse.sendErrorResponse("RA operator not authorized to get validation docs", httpResponse);
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(requestId,
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + requestId);
                errorResponse.sendErrorResponse("no request with this ID already present: " + requestId, httpResponse);
            }
            Path path = Paths.get(validationDocDirPath +
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName() + requestId + ".zip");
            byte[] validationDocsBytes = Files.readAllBytes(path);

            httpResponse.setContentType("application/zip");
            httpResponse.setStatus(HttpServletResponse.SC_OK);

            ServletOutputStream servletOutputStream = httpResponse.getOutputStream();
            servletOutputStream.write(validationDocsBytes);
            servletOutputStream.flush();
            servletOutputStream.close();

            logger.info("validation docs returned successfully");
        } catch (Exception ex) {
            logger.error("", ex);
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/request/data", produces = "application/json", consumes = "application/json")
    public ResponseEntity getRequests(@Valid @RequestBody SearchRaRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("get issuance requests request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.READ_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to get requests");
            }
            if (request.getPageSize() <= 0) {
                return errorResponse.generateErrorResponse("page size must be positive number");
            }
            PageRequest pageRequest = PageRequest.of(request.getPageNo(), request.getPageSize());

            BooleanBuilder booleanBuilder = new BooleanBuilder();
            booleanBuilder.and(QRegistrationAuthorityRequest.registrationAuthorityRequest.
                    registrationAuthorityRequestPK.registrationAuthority.name.eq(
                            regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName()));

            if (!StringUtils.isBlank(request.getId())) {
                booleanBuilder.and(QRegistrationAuthorityRequest.registrationAuthorityRequest.
                        registrationAuthorityRequestPK.id.eq(request.getId()));
            }
            if (!StringUtils.isBlank(request.getEmailAddress())) {
                booleanBuilder.and(QRegistrationAuthorityRequest.registrationAuthorityRequest.emailAddress.eq(request.getEmailAddress()));
            }
            if (!StringUtils.isBlank(request.getStatus())) {
                booleanBuilder.and(QRegistrationAuthorityRequest.registrationAuthorityRequest.status.eq(request.getStatus()));
            }
            if (!StringUtils.isBlank(request.getMobileNo())) {
                booleanBuilder.and(QRegistrationAuthorityRequest.registrationAuthorityRequest.mobileNo.eq(request.getMobileNo()));
            }
            if (!StringUtils.isBlank(request.getGivenName())) {
                booleanBuilder.and(QRegistrationAuthorityRequest.registrationAuthorityRequest.
                        requestSubjectInfo.givenName.eq(request.getGivenName()));
            }
            if (!StringUtils.isBlank(request.getSurname())) {
                booleanBuilder.and(QRegistrationAuthorityRequest.registrationAuthorityRequest.
                        requestSubjectInfo.surname.eq(request.getSurname()));
            }
            if (!StringUtils.isBlank(request.getFromDate()) && !StringUtils.isBlank(request.getToDate())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LocalDate ldFromDate = LocalDate.parse(request.getFromDate(), formatter);
                LocalDateTime fromDateTime = ldFromDate.atStartOfDay();
                LocalDate ldToDate = LocalDate.parse(request.getToDate(), formatter);
                LocalDateTime toDateTime = ldToDate.atStartOfDay();
                booleanBuilder.and(QRegistrationAuthorityRequest.registrationAuthorityRequest.createDateTime.between(fromDateTime, toDateTime));
            }
            Predicate predicate = booleanBuilder.getValue();
            if (predicate != null) {
                return ResponseEntity.status(HttpStatus.OK).body(raRequestRepository.findAll(booleanBuilder.getValue(), pageRequest));
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(raRequestRepository.findAll(pageRequest));
            }
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/issue/request/approve", consumes = "application/json")
    public ResponseEntity approveRequest(@Valid @RequestBody ApproveRaRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("approve certificate issuance request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.APPROVE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to approve requests");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.CERTIFICATE_REQUEST_PROCESSING)) {
                return errorResponse.generateErrorResponse("request status must "+
                    RaServiceConstants.CERTIFICATE_REQUEST_PROCESSING + " to approve this request");
            }

            String raName = regAuthReq.getRegistrationAuthorityRequestPK().getRegistrationAuthority().getName();
            String requestId = regAuthReq.getRegistrationAuthorityRequestPK().getId();
            String ssaId = null;
            try {
                ssaId = ssaClient.sendInitializeRequest(regAuthReq.getMobileNo(), raName, requestId);
                regAuthReq.setSsaId(ssaId);
                raRequestRepository.save(regAuthReq);
                logger.info("SSA request initialized with id: " + ssaId);
            } catch (Exception ex) {
                logger.error("", ex);
                return errorResponse.generateErrorResponse("error approving request, unable to initialize the SSA against request: "
                    + regAuthReq.getRegistrationAuthorityRequestPK().getId());
            }
            // Send certificate register email
            Path path = Paths.get(emailTemplatesDirPath + raName + "/cert_register_email.html");
            String emailTemplate = new String(Files.readAllBytes(path));
            String certRegisterLink = raServiceUrl + "ra/v1/register/" +raName+"/"+requestId;
            emailTemplate = emailTemplate.replace("<CERT_REGISTER_LINK>", certRegisterLink);
            emailService.sendCertRegisterEmail(regAuthReq.getEmailAddress(), emailTemplate);
            logger.info("email sent for request id: " + requestId + " and RA: " + raName);

            // Send OTP
            ssaClient.sendOtp(ssaId);
            logger.info("OTP sent for certificate registration");

            regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_APPROVED);
            raRequestRepository.save(regAuthReq);
            logger.info("approve certificate issuance response sent");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/issue/request/reject", consumes = "application/json")
    public ResponseEntity rejectRequest(@Valid @RequestBody RejectRaRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("reject certificate issuance request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.REJECT_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to reject requests");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.CERTIFICATE_REQUEST_PROCESSING)) {
                return errorResponse.generateErrorResponse("request status must "+
                    RaServiceConstants.CERTIFICATE_REQUEST_PROCESSING + " to reject this request");
            }
            // Send rejection email
            String raName = regAuthReq.getRegistrationAuthorityRequestPK().getRegistrationAuthority().getName();
            String requestId = regAuthReq.getRegistrationAuthorityRequestPK().getId();

            Path path = Paths.get(emailTemplatesDirPath + raName + "/cert_request_rejection_email.html");
            String emailTemplate = new String(Files.readAllBytes(path));
            emailTemplate = emailTemplate.replace("<REQUEST_ID>", requestId);
            emailTemplate = emailTemplate.replace("<REJECTION_REASON>", request.getRejectionReason());
            emailService.sendRequestRejectionEmail(regAuthReq.getEmailAddress(), emailTemplate);
            logger.info("rejection email sent for request id: " + requestId + " and RA: " + raName);

            regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_REJECTED);
            regAuthReq.setRejectionReason(request.getRejectionReason());
            raRequestRepository.save(regAuthReq);
            logger.info("reject certificate issuance response sent");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/send/csr/ca", consumes = "application/json")
    public ResponseEntity sendCsrToCA(@Valid @RequestBody SendCsrToCaRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("send CSR to CA request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.APPROVE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to send CSR to CA");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.CERTIFICATE_REQUEST_KEYPAIR_GENERATED_SSA)) {
                return errorResponse.generateErrorResponse("request status must "+
                        RaServiceConstants.CERTIFICATE_REQUEST_KEYPAIR_GENERATED_SSA + " to send CSR to CA");
            }
            String[] certChain = caClient.sendCsrToCa(regAuthReq.getCsr(), regAuthReq.getRequestSubjectInfo(),
                    RaServiceCache.getRaProfile(regAuthReq.getRaProfile()).getCertProfile());
            regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_CERT_ISSUED_CA);
            regAuthReq.setEndEntityCertificate(certChain[0]);
            regAuthReq.setIssuerCertificate(certChain[1]);
            raRequestRepository.save(regAuthReq);
            logger.info("send CSR to CA response sent");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/import/cert/ssa", consumes = "application/json")
    public ResponseEntity importCertToSsa(@Valid @RequestBody ImportCertToSsaRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("send CSR to CA request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.APPROVE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to import certificate to SSA");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.CERTIFICATE_REQUEST_CERT_ISSUED_CA)) {
                return errorResponse.generateErrorResponse("request status must "+
                        RaServiceConstants.CERTIFICATE_REQUEST_CERT_ISSUED_CA + " to import certificate to SSA");
            }
            SecureRandom secureRandom = new SecureRandom();
            int randomNumber = secureRandom.nextInt(999999);
            String revocationCode = String.format("%06d", randomNumber);

            ssaClient.importCertificate(regAuthReq.getSsaId(), regAuthReq.getEndEntityCertificate(), regAuthReq.getIssuerCertificate());
            regAuthReq.setStatus(RaServiceConstants.CERTIFICATE_REQUEST_CERT_IMPORTED_SSA);
            regAuthReq.setRevocationCode(revocationCode);
            raRequestRepository.save(regAuthReq);
            logger.info("certificate imported to SSA");

            String raName = regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName();
            String requestId = request.getRequestId();
            Path path = Paths.get(emailTemplatesDirPath + raName + "/certificate_registration_success.html");
            String emailTemplate = new String(Files.readAllBytes(path));
            String certRevocationLink = raServiceUrl + "ra/v1/revoke/" +raName+"/"+requestId+"/"+revocationCode;
            emailTemplate = emailTemplate.replace("<CERTIFICATE_REVOCATION_LINK>", certRevocationLink);
            emailService.sendCertRegisterSuccessEmail(regAuthReq.getEmailAddress(), emailTemplate);
            logger.info("email sent for request id: " + requestId + " and RA: " + raName);

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/cert/reg/success/email", consumes = "application/json")
    public ResponseEntity sendRegSuccessEmail(@Valid @RequestBody SendRegSuccessEmailRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("send registration success email request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.APPROVE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to send registration success email");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.CERTIFICATE_REQUEST_CERT_IMPORTED_SSA)) {
                return errorResponse.generateErrorResponse("request status must "+
                        RaServiceConstants.CERTIFICATE_REQUEST_CERT_IMPORTED_SSA + " to send registration success email");
            }
            String raName = regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName();
            String requestId = request.getRequestId();
            Path path = Paths.get(emailTemplatesDirPath + raName + "/certificate_registration_success.html");
            String emailTemplate = new String(Files.readAllBytes(path));
            String certRevocationLink = raServiceUrl + "ra/v1/revoke/" +raName+"/"+requestId+"/"+regAuthReq.getRevocationCode();
            emailTemplate = emailTemplate.replace("<CERTIFICATE_REVOCATION_LINK>", certRevocationLink);
            emailService.sendCertRegisterSuccessEmail(regAuthReq.getEmailAddress(), emailTemplate);
            logger.info("email sent for request id: " + requestId + " and RA: " + raName);

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/revoke/request", produces = "application/json", consumes = "application/json")
    public ResponseEntity revokeRequest(@Valid @RequestBody RevokeRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("revoke request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.CREATE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to send revoke request");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.CERTIFICATE_REQUEST_CERT_IMPORTED_SSA) &&
                    !regAuthReq.getStatus().equals(RaServiceConstants.CERTIFICATE_REQUEST_CERT_ISSUED_CA)) {
                return errorResponse.generateErrorResponse("cannot revoke as certificate is not issued yet");
            }
            regAuthReq.setStatus(RaServiceConstants.REVOKE_REQUEST_PENDING);
            raRequestRepository.save(regAuthReq);
            logger.info("revoke revoke successfully created");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(value = "/ra/v1/registration/authority/revoke/request", produces = "application/json", consumes = "application/json")
    public ResponseEntity assignRevokeRequest(@Valid @RequestBody RevokeRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("revoke request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.UPDATE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to update revoke request");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.REVOKE_REQUEST_PENDING)) {
                return errorResponse.generateErrorResponse("cannot revoke as certificate is not issued yet");
            }
            regAuthReq.setStatus(RaServiceConstants.REVOKE_REQUEST_PROCESSING);
            raRequestRepository.save(regAuthReq);
            logger.info("revoke request successfully assigned");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/revoke/request/approve", produces = "application/json", consumes = "application/json")
    public ResponseEntity approveRevokeRequest(@Valid @RequestBody ApproveRevokeRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("revoke request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.APPROVE_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to approve revoke request");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.REVOKE_REQUEST_PROCESSING)) {
                return errorResponse.generateErrorResponse("cannot approve as request status is not "+
                        RaServiceConstants.REVOKE_REQUEST_PROCESSING);
            }
            caClient.revokeCert(regAuthReq.getEndEntityCertificate(), regAuthReq.getIssuerCertificate(), request.getRevocationReason());
            ssaClient.revokeCertificate(regAuthReq.getSsaId());

            regAuthReq.setStatus(RaServiceConstants.REVOKE_REQUEST_APPROVED);
            raRequestRepository.save(regAuthReq);

            X509CertificateHolder subject = new X509CertificateHolder(Base64.getDecoder().decode(regAuthReq.getEndEntityCertificate()));
            String raName = regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName();
            Path path = Paths.get(emailTemplatesDirPath + raName + "/certificate_revocation_success.html");
            String emailTemplate = new String(Files.readAllBytes(path));
            emailTemplate = emailTemplate.replace("<RA_NAme>", raName);
            emailTemplate = emailTemplate.replace("<REQUEST_ID>", request.getRequestId());
            emailTemplate = emailTemplate.replace("<SUBJECT_DN>", subject.getSubject().toString());
            emailService.sendRequestRejectionEmail(regAuthReq.getEmailAddress(), emailTemplate);
            logger.info("email sent for request id: " + request.getRequestId() + " and RA: " + raName);

            logger.info("revoke request approved successfully");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/ra/v1/registration/authority/revoke/request/reject", produces = "application/json", consumes = "application/json")
    public ResponseEntity rejectRevokeRequest(@Valid @RequestBody RejectRevokeRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("reject revocation request received");
            RegistrationAuthorityOperator regAuthOperatorAuth = authService.authenticateRaOperator(httpRequest);
            if (regAuthOperatorAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthOperatorAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA operator status is disabled");
            }
            if (regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthOperatorAuth.getRole().contains(RaServiceConstants.REJECT_REQUESTS)) {
                return errorResponse.generateErrorResponse("RA operator not authorized to reject revoke request");
            }
            List<RegistrationAuthorityRequest> reqList = raRequestRepository.findRequestByIdAndRa(request.getRequestId(),
                    regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName());
            if (reqList.size() == 0) {
                logger.info("no request with this ID already present: " + request.getRequestId());
                return errorResponse.generateErrorResponse("no request with this ID already present: " + request.getRequestId());
            }
            RegistrationAuthorityRequest regAuthReq = reqList.get(0);
            if (!regAuthReq.getStatus().equals(RaServiceConstants.REVOKE_REQUEST_PROCESSING)) {
                return errorResponse.generateErrorResponse("cannot reject request as request status is not "+
                        RaServiceConstants.REVOKE_REQUEST_PROCESSING);
            }
            regAuthReq.setStatus(RaServiceConstants.REVOKE_REQUEST_REJECTED);
            regAuthReq.setRejectionReason(request.getRejectionReason());
            raRequestRepository.save(regAuthReq);

            String raName = regAuthOperatorAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName();
            Path path = Paths.get(emailTemplatesDirPath + raName + "/revoke_request_rejection_email.html");
            String emailTemplate = new String(Files.readAllBytes(path));
            emailTemplate = emailTemplate.replace("<RA_NAme>", raName);
            emailTemplate = emailTemplate.replace("<REQUEST_ID>", request.getRequestId());
            emailTemplate = emailTemplate.replace("<REJECTION_REASON>", request.getRejectionReason());
            emailService.sendRequestRejectionEmail(regAuthReq.getEmailAddress(), emailTemplate);
            logger.info("rejection email sent for request id: " + request.getRequestId() + " and RA: " + raName);

            logger.info("revoke request rejected response sent");
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
