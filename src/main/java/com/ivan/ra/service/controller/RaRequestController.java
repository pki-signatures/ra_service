package com.ivan.ra.service.controller;

import com.ivan.ra.service.config.ra.profile.SubjectInfoVO;
import com.ivan.ra.service.constants.RaServiceConstants;
import com.ivan.ra.service.model.QRegistrationAuthorityRequest;
import com.ivan.ra.service.model.RegistrationAuthorityOperator;
import com.ivan.ra.service.model.RegistrationAuthorityRequest;
import com.ivan.ra.service.model.RegistrationAuthorityRequestPK;
import com.ivan.ra.service.model.RequestSubjectInfo;
import com.ivan.ra.service.repository.RaRequestRepository;
import com.ivan.ra.service.service.ClientCertAuthService;
import com.ivan.ra.service.service.RaProfileCompatibilityChecker;
import com.ivan.ra.service.vo.ApproveRaRequest;
import com.ivan.ra.service.vo.RaRequest;
import com.ivan.ra.service.vo.RegisterRaResponse;
import com.ivan.ra.service.vo.RejectRaRequest;
import com.ivan.ra.service.vo.SearchRaRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class RaRequestController {
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

    private static final Logger logger = LogManager.getLogger(RaRequestController.class);

    @PostMapping(value = "/ra/v1/registration/authority/request", produces = "application/json", consumes = "application/json")
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
            regAuthReq.setStatus(RaServiceConstants.REQUEST_STATUS_PENDING);
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

    @PutMapping(value = "/ra/v1/registration/authority/request", produces = "application/json", consumes = "application/json")
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
                    RaServiceConstants.REQUEST_STATUS_PROCESSING)) {
                return errorResponse.generateErrorResponse("only allowed status: " + RaServiceConstants.REQUEST_STATUS_PROCESSING);
            } else {
                regAuthReq.setStatus(RaServiceConstants.REQUEST_STATUS_PROCESSING);
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

    @PostMapping(value = "/ra/v1/registration/authority/request/approve", consumes = "application/json")
    public ResponseEntity approveRequest(@Valid @RequestBody ApproveRaRequest request, HttpServletRequest httpRequest) {
        return null;
    }

    @PostMapping(value = "/ra/v1/registration/authority/request/reject", consumes = "application/json")
    public ResponseEntity rejectRequest(@Valid @RequestBody RejectRaRequest request, HttpServletRequest httpRequest) {
        return null;
    }

    // Handle revocation request
}
