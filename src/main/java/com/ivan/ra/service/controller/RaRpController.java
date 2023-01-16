package com.ivan.ra.service.controller;

import com.ivan.ra.service.cache.RaServiceCache;
import com.ivan.ra.service.constants.RaServiceConstants;
import com.ivan.ra.service.model.RegistrationAuthorityAdmin;
import com.ivan.ra.service.model.RegistrationAuthorityOperator;
import com.ivan.ra.service.model.RegistrationAuthorityPK;
import com.ivan.ra.service.model.RegistrationAuthorityRelyingParty;
import com.ivan.ra.service.repository.RaAdminRepository;
import com.ivan.ra.service.repository.RaOperatorRepository;
import com.ivan.ra.service.repository.RaRpRepository;
import com.ivan.ra.service.service.ClientCertAuthService;
import com.ivan.ra.service.util.Util;
import com.ivan.ra.service.vo.RaOperator;
import com.ivan.ra.service.vo.RaRp;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@Validated
public class RaRpController {

    @Autowired
    RaAdminRepository raAdminRepository;

    @Autowired
    RaOperatorRepository raOperatorRepository;

    @Autowired
    RaRpRepository raRpRepository;

    @Autowired
    ErrorResponse errorResponse;

    @Autowired
    Util util;

    @Autowired
    ClientCertAuthService authService;

    private static final Logger logger = LogManager.getLogger(RaRpController.class);

    @PostMapping(value = "/ra/v1/registration/authority/rp", produces = "application/json", consumes = "application/json")
    public ResponseEntity registerRaRp(@Valid @RequestBody RaRp request, HttpServletRequest httpRequest) {
        try {
            logger.info("register new RP request received");
            RegistrationAuthorityAdmin regAuthAdminAuth = authService.authenticateRaAdmin(httpRequest);
            if (regAuthAdminAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthAdminAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA admin status is disabled");
            }
            if (regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.CREATE_RELYING_PARTIES)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to create further RPs");
            }
            List<RegistrationAuthorityRelyingParty> raRp = raRpRepository.findRpByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), request.getName());
            if (raRp.size() != 0) {
                logger.info("RA RP with this name already present: " + request.getName());
                return errorResponse.generateErrorResponse("RA RP with this name already present: " + request.getName());
            }
            String certDigest = util.getCertDigest(request.getClientAuthCert());
            ResponseEntity error = authService.isClientCertAlreadyConfigured(certDigest);
            if (error != null) {
                return error;
            }

            RegistrationAuthorityPK registrationAuthorityPK = new RegistrationAuthorityPK();
            registrationAuthorityPK.setName(request.getName());
            registrationAuthorityPK.setRegistrationAuthority(regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority());

            RegistrationAuthorityRelyingParty registrationAuthorityRp = new RegistrationAuthorityRelyingParty();
            registrationAuthorityRp.setRegistrationAuthorityPK(registrationAuthorityPK);
            registrationAuthorityRp.setStatus(request.getStatus());
            registrationAuthorityRp.setClientAuthCert(request.getClientAuthCert());
            registrationAuthorityRp.setClientAuthCertHash(certDigest);
            registrationAuthorityRp.setPrimaryContactName(request.getPrimaryContactName());
            registrationAuthorityRp.setPrimaryContactNo(request.getPrimaryContactNo());
            registrationAuthorityRp.setPrimaryContactEmailAddress(request.getPrimaryContactEmailAddress());
            registrationAuthorityRp.setSecondaryContactName(request.getPrimaryContactName());
            registrationAuthorityRp.setSecondaryContactNo(request.getPrimaryContactNo());
            registrationAuthorityRp.setSecondaryContactEmailAddress(request.getPrimaryContactEmailAddress());
            raRpRepository.save(registrationAuthorityRp);
            logger.info("new RA RP registered successfully");

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(value = "/ra/v1/registration/authority/rp", produces = "application/json", consumes = "application/json")
    public ResponseEntity updateRaRp(@Valid @RequestBody RaRp request, HttpServletRequest httpRequest) {
        try {
            logger.info("update RA RP information request received");
            RegistrationAuthorityAdmin regAuthAdminAuth = authService.authenticateRaAdmin(httpRequest);
            if (regAuthAdminAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthAdminAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA admin status is disabled");
            }
            if (regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.UPDATE_RELYING_PARTIES)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to update RA RPs");
            }
            List<RegistrationAuthorityRelyingParty> raRp = raRpRepository.findRpByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), request.getName());
            if (raRp.size() == 0) {
                logger.info("no RA RP with this name already present: " + request.getName());
                return errorResponse.generateErrorResponse("no RA RP with this name already present: " + request.getName());
            }
            RegistrationAuthorityRelyingParty regAuthRp = raRp.get(0);

            if (!StringUtils.isBlank(request.getClientAuthCert())) {
                String certDigest = util.getCertDigest(request.getClientAuthCert());
                ResponseEntity error = authService.isClientCertAlreadyConfigured(certDigest);
                if (error != null) {
                    return error;
                }
                regAuthRp.setClientAuthCert(request.getClientAuthCert());
                regAuthRp.setClientAuthCertHash(certDigest);
            }
            if (!StringUtils.isBlank(request.getStatus())) {
                regAuthRp.setStatus(request.getStatus());
            }
            if (!StringUtils.isBlank(request.getPrimaryContactName())) {
                regAuthRp.setPrimaryContactName(request.getPrimaryContactName());
            }
            if (!StringUtils.isBlank(request.getPrimaryContactNo())) {
                regAuthRp.setPrimaryContactNo(request.getPrimaryContactNo());
            }
            if (!StringUtils.isBlank(request.getPrimaryContactEmailAddress())) {
                regAuthRp.setPrimaryContactEmailAddress(request.getPrimaryContactEmailAddress());
            }
            if (!StringUtils.isBlank(request.getSecondaryContactName())) {
                regAuthRp.setSecondaryContactName(request.getSecondaryContactName());
            }
            if (!StringUtils.isBlank(request.getSecondaryContactNo())) {
                regAuthRp.setSecondaryContactNo(request.getSecondaryContactNo());
            }
            if (!StringUtils.isBlank(request.getSecondaryContactEmailAddress())) {
                regAuthRp.setSecondaryContactEmailAddress(request.getSecondaryContactEmailAddress());
            }
            raRpRepository.save(regAuthRp);
            logger.info("RA RP updated successfully");

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/rp", produces = "application/json")
    public ResponseEntity getAllRaRps(HttpServletRequest httpRequest) {
        try {
            logger.info("get all RA RPs request received");
            RegistrationAuthorityAdmin regAuthAdminAuth = authService.authenticateRaAdmin(httpRequest);
            if (regAuthAdminAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthAdminAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA admin status is disabled");
            }
            if (regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.READ_RELYING_PARTIES)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to get further RA RPs information");
            }
            List<RaRp> raOperatorsList = new ArrayList<>();
            for (RegistrationAuthorityRelyingParty regAuthorityRp: raRpRepository.findAllRpsByRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName())) {
                RaRp raRp = new RaRp();
                raRp.setName(regAuthorityRp.getRegistrationAuthorityPK().getName());
                raRp.setClientAuthCert(regAuthorityRp.getClientAuthCert());
                raRp.setStatus(regAuthorityRp.getStatus());
                raRp.setPrimaryContactName(regAuthorityRp.getPrimaryContactName());
                raRp.setPrimaryContactNo(regAuthorityRp.getPrimaryContactNo());
                raRp.setPrimaryContactEmailAddress(regAuthorityRp.getPrimaryContactEmailAddress());
                raRp.setSecondaryContactName(regAuthorityRp.getSecondaryContactName());
                raRp.setSecondaryContactNo(regAuthorityRp.getSecondaryContactNo());
                raRp.setSecondaryContactEmailAddress(regAuthorityRp.getSecondaryContactEmailAddress());
                raOperatorsList.add(raRp);
            }
            logger.info("all RA RPs information sent successfully");
            return ResponseEntity.ok().body(raOperatorsList);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/rp/{name}", produces = "application/json")
    public ResponseEntity getRaRp(@RequestParam("name") @NotBlank String name, HttpServletRequest httpRequest) {
        try {
            logger.info("get RA RP by name request received");
            RegistrationAuthorityAdmin regAuthAdminAuth = authService.authenticateRaAdmin(httpRequest);
            if (regAuthAdminAuth == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthAdminAuth.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA admin status is disabled");
            }
            if (regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.READ_RELYING_PARTIES)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to get further RA RPs information");
            }
            List<RegistrationAuthorityRelyingParty> regAuthRpsList = raRpRepository.findRpByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), name);
            if (regAuthRpsList.size() == 0) {
                logger.info("no RA RP with this name already present: " + name);
                return errorResponse.generateErrorResponse("no RA RP with this name already present: " + name);
            }
            RegistrationAuthorityRelyingParty regAuthorityRp = regAuthRpsList.get(0);

            RaRp raRp = new RaRp();
            raRp.setName(regAuthorityRp.getRegistrationAuthorityPK().getName());
            raRp.setClientAuthCert(regAuthorityRp.getClientAuthCert());
            raRp.setStatus(regAuthorityRp.getStatus());
            raRp.setPrimaryContactName(regAuthorityRp.getPrimaryContactName());
            raRp.setPrimaryContactNo(regAuthorityRp.getPrimaryContactNo());
            raRp.setPrimaryContactEmailAddress(regAuthorityRp.getPrimaryContactEmailAddress());
            raRp.setSecondaryContactName(regAuthorityRp.getSecondaryContactName());
            raRp.setSecondaryContactNo(regAuthorityRp.getSecondaryContactNo());
            raRp.setSecondaryContactEmailAddress(regAuthorityRp.getSecondaryContactEmailAddress());

            logger.info("RA RP by name information sent successfully");
            return ResponseEntity.ok().body(raRp);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
