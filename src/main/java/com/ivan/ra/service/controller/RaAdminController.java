package com.ivan.ra.service.controller;

import com.ivan.ra.service.constants.RaServiceConstants;
import com.ivan.ra.service.model.RegistrationAuthorityAdmin;
import com.ivan.ra.service.model.RegistrationAuthorityPK;
import com.ivan.ra.service.repository.RaAdminRepository;
import com.ivan.ra.service.repository.RaOperatorRepository;
import com.ivan.ra.service.repository.RaRpRepository;
import com.ivan.ra.service.service.ClientCertAuthService;
import com.ivan.ra.service.util.Util;
import com.ivan.ra.service.vo.RaAdmin;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@Validated
public class RaAdminController {
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

    private static final Logger logger = LogManager.getLogger(RaAdminController.class);

    @PostMapping(value = "/ra/v1/registration/authority/admin", produces = "application/json", consumes = "application/json")
    public ResponseEntity registerRaAdmin(@Valid @RequestBody RaAdmin request, HttpServletRequest httpRequest) {
        try {
            logger.info("register new RA admin request received");
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
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.CREATE_ADMINS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to create further RA admins");
            }
            List<RegistrationAuthorityAdmin> raAdmin = raAdminRepository.findAdminByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), request.getName());
            if (raAdmin.size() != 0) {
                logger.info("RA admin with this name already present: " + request.getName());
                return errorResponse.generateErrorResponse("RA admin with this name already present: " + request.getName());
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

                List<String> rolesAssigned = Arrays.asList(regAuthAdminAuth.getRole().split(":"));
                for (String roleReq : rolesReq) {
                    if (!rolesAssigned.contains(roleReq)) {
                        return errorResponse.generateErrorResponse("not authorized to assign this role to admin: " + roleReq);
                    }
                }
            } else {
                return errorResponse.generateErrorResponse("roles must be present");
            }

            RegistrationAuthorityPK registrationAuthorityPK = new RegistrationAuthorityPK();
            registrationAuthorityPK.setName(request.getName());
            registrationAuthorityPK.setRegistrationAuthority(regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority());

            RegistrationAuthorityAdmin registrationAuthorityAdmin = new RegistrationAuthorityAdmin();
            registrationAuthorityAdmin.setRegistrationAuthorityPK(registrationAuthorityPK);
            registrationAuthorityAdmin.setRole(String.join(":", rolesReq));
            registrationAuthorityAdmin.setStatus(request.getStatus());
            registrationAuthorityAdmin.setClientAuthCert(request.getClientAuthCert());
            registrationAuthorityAdmin.setClientAuthCertHash(certDigest);
            registrationAuthorityAdmin.setEmailAddress(request.getEmailAddress());
            raAdminRepository.save(registrationAuthorityAdmin);
            logger.info("new RA admin registered successfully");

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(value = "/ra/v1/registration/authority/admin", produces = "application/json", consumes = "application/json")
    public ResponseEntity updateRaAdmin(@Valid @RequestBody RaAdmin request, HttpServletRequest httpRequest) {
        try {
            logger.info("update RA admin information request received");
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
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.UPDATE_ADMINS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to update further RA admins");
            }
            List<RegistrationAuthorityAdmin> raAdmin = raAdminRepository.findAdminByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), request.getName());
            if (raAdmin.size() == 0) {
                logger.info("no RA admin with this name already present: " + request.getName());
                return errorResponse.generateErrorResponse("no RA admin with this name already present: " + request.getName());
            }
            RegistrationAuthorityAdmin regAuthAdmin = raAdmin.get(0);

            if (!StringUtils.isBlank(request.getClientAuthCert())) {
                String certDigest = util.getCertDigest(request.getClientAuthCert());
                ResponseEntity error = authService.isClientCertAlreadyConfigured(certDigest);
                if (error != null) {
                    return error;
                }
                regAuthAdmin.setClientAuthCert(request.getClientAuthCert());
                regAuthAdmin.setClientAuthCertHash(certDigest);
            }
            if (!StringUtils.isBlank(request.getStatus())) {
                regAuthAdmin.setStatus(request.getStatus());
            }
            if (!StringUtils.isBlank(request.getEmailAddress())) {
                regAuthAdmin.setEmailAddress(request.getEmailAddress());
            }
            if (request.getRoles() != null && request.getRoles().length != 0) {
                Set<String> tempReq = new LinkedHashSet<>(Arrays.asList(request.getRoles()));
                String[] rolesReq = tempReq.toArray(new String[tempReq.size()]);
                List<String> rolesAssigned = Arrays.asList(regAuthAdminAuth.getRole().split(":"));
                for (String roleReq : rolesReq) {
                    if (!rolesAssigned.contains(roleReq)) {
                        return errorResponse.generateErrorResponse("not authorized to assign this role to admin: " + roleReq);
                    }
                }

                List<String> rolesList = new ArrayList<>(Arrays.asList(regAuthAdmin.getRole().split(":")));
                rolesList.addAll(Arrays.asList(request.getRoles()));
                Set<String> temp = new LinkedHashSet<>(rolesList);
                String[] roles = temp.toArray(new String[temp.size()]);
                regAuthAdmin.setRole(String.join(":", roles));
            }
            raAdminRepository.save(regAuthAdmin);
            logger.info("RA admin updated successfully");

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/admin", produces = "application/json")
    public ResponseEntity getAllRaAdmins(HttpServletRequest httpRequest) {
        try {
            logger.info("get all RA admins request received");
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
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.READ_ADMINS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to get further RA admins information");
            }
            List<RaAdmin> raAdminList = new ArrayList<>();
            for (RegistrationAuthorityAdmin regAuthorityAdmin : raAdminRepository.findAllAdminsByRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName())) {
                RaAdmin raAdmin = new RaAdmin();
                raAdmin.setName(regAuthorityAdmin.getRegistrationAuthorityPK().getName());
                raAdmin.setEmailAddress(regAuthorityAdmin.getEmailAddress());
                raAdmin.setClientAuthCert(regAuthorityAdmin.getClientAuthCert());
                raAdmin.setStatus(regAuthorityAdmin.getStatus());
                raAdmin.setRoles(regAuthorityAdmin.getRole().split(":"));
                raAdminList.add(raAdmin);
            }
            logger.info("all RA admins information sent successfully");
            return ResponseEntity.ok().body(raAdminList);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/admin/{name}", produces = "application/json")
    public ResponseEntity getRaAdmin(@RequestParam("name") @NotBlank String name, HttpServletRequest httpRequest) {
        try {
            logger.info("get RA admin by name request received");
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
            if (!regAuthAdminAuth.getRole().contains(RaServiceConstants.READ_ADMINS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to get further RA admins information");
            }
            List<RegistrationAuthorityAdmin> raAdminList = raAdminRepository.findAdminByNameAndRa(
                    regAuthAdminAuth.getRegistrationAuthorityPK().getRegistrationAuthority().getName(), name);
            if (raAdminList.size() == 0) {
                logger.info("no RA admin with this name already present: " + name);
                return errorResponse.generateErrorResponse("no RA admin with this name already present: " + name);
            }
            RegistrationAuthorityAdmin regAuthorityAdmin = raAdminList.get(0);

            RaAdmin raAdmin = new RaAdmin();
            raAdmin.setName(regAuthorityAdmin.getRegistrationAuthorityPK().getName());
            raAdmin.setEmailAddress(regAuthorityAdmin.getEmailAddress());
            raAdmin.setClientAuthCert(regAuthorityAdmin.getClientAuthCert());
            raAdmin.setStatus(regAuthorityAdmin.getStatus());
            raAdmin.setRoles(regAuthorityAdmin.getRole().split(":"));

            logger.info("RA admin by name information sent successfully");
            return ResponseEntity.ok().body(raAdmin);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}