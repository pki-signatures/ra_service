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
public class RaOperatorController {
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

    private static final Logger logger = LogManager.getLogger(RaOperatorController.class);

    @PostMapping(value = "/ra/v1/registration/authority/operator", produces = "application/json", consumes = "application/json")
    public ResponseEntity registerRaOperator(@Valid @RequestBody RaOperator request, HttpServletRequest httpRequest) {
        try {
            logger.info("register new RA operator request received");
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
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.CREATE_OPERATORS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to create further RA operators");
            }
            List<RegistrationAuthorityOperator> raOperator = raOperatorRepository.findOperatorByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), request.getName());
            if (raOperator.size() != 0) {
                logger.info("RA operator with this name already present: " + request.getName());
                return errorResponse.generateErrorResponse("RA operator with this name already present: " + request.getName());
            }
            String certDigest = util.getCertDigest(request.getClientAuthCert());
            ResponseEntity error = authService.isClientCertAlreadyConfigured(certDigest);
            if (error != null) {
                return error;
            }
            String[] rolesReq = null;
            if (request.getRoles() != null && request.getRoles().length != 0) {
                Set<String> temp = new LinkedHashSet<>(Arrays.asList(request.getRoles()));
                rolesReq = temp.toArray(new String[temp.size()]);
            } else {
                return errorResponse.generateErrorResponse("roles must be present");
            }

            RegistrationAuthorityPK registrationAuthorityPK = new RegistrationAuthorityPK();
            registrationAuthorityPK.setName(request.getName());
            registrationAuthorityPK.setRegistrationAuthority(regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority());

            RegistrationAuthorityOperator registrationAuthorityOperator = new RegistrationAuthorityOperator();
            registrationAuthorityOperator.setRegistrationAuthorityPK(registrationAuthorityPK);
            registrationAuthorityOperator.setRole(String.join(":", rolesReq));
            registrationAuthorityOperator.setStatus(request.getStatus());
            registrationAuthorityOperator.setClientAuthCert(request.getClientAuthCert());
            registrationAuthorityOperator.setClientAuthCertHash(certDigest);
            registrationAuthorityOperator.setEmailAddress(request.getEmailAddress());
            raOperatorRepository.save(registrationAuthorityOperator);
            logger.info("new RA operator registered successfully");

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(value = "/ra/v1/registration/authority/operator", produces = "application/json", consumes = "application/json")
    public ResponseEntity updateRaOperator(@Valid @RequestBody RaOperator request, HttpServletRequest httpRequest) {
        try {
            logger.info("update RA operator information request received");
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
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.UPDATE_OPERATORS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to update RA operators");
            }
            List<RegistrationAuthorityOperator> raOperator = raOperatorRepository.findOperatorByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), request.getName());
            if (raOperator.size() == 0) {
                logger.info("no RA operator with this name already present: " + request.getName());
                return errorResponse.generateErrorResponse("no RA operator with this name already present: " + request.getName());
            }
            RegistrationAuthorityOperator regAuthOp = raOperator.get(0);

            if (!StringUtils.isBlank(request.getClientAuthCert())) {
                String certDigest = util.getCertDigest(request.getClientAuthCert());
                ResponseEntity error = authService.isClientCertAlreadyConfigured(certDigest);
                if (error != null) {
                    return error;
                }
                regAuthOp.setClientAuthCert(request.getClientAuthCert());
                regAuthOp.setClientAuthCertHash(certDigest);
            }
            if (!StringUtils.isBlank(request.getStatus())) {
                regAuthOp.setStatus(request.getStatus());
            }
            if (!StringUtils.isBlank(request.getEmailAddress())) {
                regAuthOp.setEmailAddress(request.getEmailAddress());
            }
            if (request.getRoles() != null && request.getRoles().length != 0) {
                List<String> rolesList = new ArrayList<>(Arrays.asList(regAuthOp.getRole().split(":")));
                rolesList.addAll(Arrays.asList(request.getRoles()));

                Set<String> temp = new LinkedHashSet<>(rolesList);
                String[] roles = temp.toArray(new String[temp.size()]);
                regAuthOp.setRole(String.join(":", roles));
            }
            raOperatorRepository.save(regAuthOp);
            logger.info("RA operator updated successfully");

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/operator", produces = "application/json")
    public ResponseEntity getAllRaOperators(HttpServletRequest httpRequest) {
        try {
            logger.info("get all RA operators request received");
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
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.READ_OPERATORS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to get further RA admins information");
            }
            List<RaOperator> raOperatorsList = new ArrayList<>();
            for (RegistrationAuthorityOperator regAuthorityOp: raOperatorRepository.findAllOperatorsByRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName())) {
                RaOperator raOperator = new RaOperator();
                raOperator.setName(regAuthorityOp.getRegistrationAuthorityPK().getName());
                raOperator.setEmailAddress(regAuthorityOp.getEmailAddress());
                raOperator.setClientAuthCert(regAuthorityOp.getClientAuthCert());
                raOperator.setStatus(regAuthorityOp.getStatus());
                raOperator.setRoles(regAuthorityOp.getRole().split(":"));
                raOperatorsList.add(raOperator);
            }
            logger.info("all RA operators information sent successfully");
            return ResponseEntity.ok().body(raOperatorsList);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/operator/{name}", produces = "application/json")
    public ResponseEntity getRaOperator(@RequestParam("name") @NotBlank String name, HttpServletRequest httpRequest) {
        try {
            logger.info("get RA operator by name request received");
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
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.READ_OPERATORS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to get further RA operators information");
            }
            List<RegistrationAuthorityOperator> regAuthOpsList = raOperatorRepository.findOperatorByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), name);
            if (regAuthOpsList.size() == 0) {
                logger.info("no RA operator with this name already present: " + name);
                return errorResponse.generateErrorResponse("no RA admin with this name already present: " + name);
            }
            RegistrationAuthorityOperator regAuthorityOp = regAuthOpsList.get(0);

            RaOperator raOperator = new RaOperator();
            raOperator.setName(regAuthorityOp.getRegistrationAuthorityPK().getName());
            raOperator.setEmailAddress(regAuthorityOp.getEmailAddress());
            raOperator.setClientAuthCert(regAuthorityOp.getClientAuthCert());
            raOperator.setStatus(regAuthorityOp.getStatus());
            raOperator.setRoles(regAuthorityOp.getRole().split(":"));

            logger.info("RA operator by name information sent successfully");
            return ResponseEntity.ok().body(raOperator);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
