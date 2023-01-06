package com.ivan.ra.service.controller;

import com.ivan.ra.service.cache.RaServiceCache;
import com.ivan.ra.service.constants.RaServiceConstants;
import com.ivan.ra.service.model.RegistrationAuthority;
import com.ivan.ra.service.model.RegistrationAuthorityAdmin;
import com.ivan.ra.service.model.RegistrationAuthorityOperator;
import com.ivan.ra.service.model.RegistrationAuthorityPK;
import com.ivan.ra.service.model.RegistrationAuthorityRelyingParty;
import com.ivan.ra.service.repository.RaAdminRepository;
import com.ivan.ra.service.repository.RaOperatorRepository;
import com.ivan.ra.service.repository.RaRpRepository;
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
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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

    private static final Logger logger = LogManager.getLogger(RaAdminController.class);

    @PostMapping(value = "/ra/v1/registration/authority/admin", produces = "application/json", consumes = "application/json")
    public ResponseEntity registerRaAdmin(@Valid @RequestBody RaAdmin request, HttpServletRequest httpRequest) {
        try {
            logger.info("register new RA admin request received");
            RegistrationAuthorityAdmin regAuthAdmin = authenticate(httpRequest);
            if (regAuthAdmin == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthAdmin.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA admin status is disabled");
            }
            if (regAuthAdmin.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthAdmin.getRole().contains(RaServiceConstants.CREATE_ADMINS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to create further RA admins");
            }
            List<RegistrationAuthorityAdmin> raAdmin = raAdminRepository.findAdminByName(request.getName());
            if (raAdmin.size() != 0) {
                logger.info("RA admin with this name already present: " + request.getName());
                return errorResponse.generateErrorResponse("RA admin with this name already present: " + request.getName());
            }
            String certDigest = util.getCertDigest(request.getClientAuthCert());
            if (RaServiceCache.getAccessControlSettingsConfig(certDigest) != null) {
                logger.info("new RA admin certificate already configured in access control config file");
                return errorResponse.generateErrorResponse("new RA admin client certificate already configured. " +
                        "Use other certificate");
            }
            List<RegistrationAuthorityAdmin> raAdmins = raAdminRepository.findRaAdminByClientCert(certDigest);
            if (raAdmins.size() != 0) {
                logger.info("new RA admin certificate is already configured under RA admin configurations");
                return errorResponse.generateErrorResponse("new RA admin client certificate already configured. " +
                        "Use other certificate");
            }
            List<RegistrationAuthorityOperator> raOps = raOperatorRepository.findRaOperatorByClientCert(certDigest);
            if (raOps.size() != 0) {
                logger.info("new RA admin certificate is already configured under RA operator configurations");
                return errorResponse.generateErrorResponse("new RA admin client certificate already configured. " +
                        "Use other certificate");
            }
            List<RegistrationAuthorityRelyingParty> raRps = raRpRepository.findRaRpByClientCert(certDigest);
            if (raRps.size() != 0) {
                logger.info("new RA admin certificate is already configured under RA relying party configurations");
                return errorResponse.generateErrorResponse("new RA admin client certificate already configured. " +
                        "Use other certificate");
            }
            String[] rolesReq = null;
            if (request.getRoles() != null && request.getRoles().length != 0) {
                Set<String> temp = new LinkedHashSet<>(Arrays.asList(request.getRoles()));
                rolesReq = temp.toArray(new String[temp.size()]);

                List<String> rolesAssigned = Arrays.asList(regAuthAdmin.getRole().split(":"));
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
            registrationAuthorityPK.setRegistrationAuthority(regAuthAdmin.getRegistrationAuthorityPK().getRegistrationAuthority());

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
            RegistrationAuthorityAdmin regAuthAdmin = authenticate(httpRequest);
            if (regAuthAdmin == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthAdmin.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA admin status is disabled");
            }
            if (regAuthAdmin.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthAdmin.getRole().contains(RaServiceConstants.UPDATE_ADMINS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to update further RA admins");
            }
            List<RegistrationAuthorityAdmin> raAdmin = raAdminRepository.findAdminByName(request.getName());
            if (raAdmin.size() == 0) {
                logger.info("no RA admin with this name already present: " + request.getName());
                return errorResponse.generateErrorResponse("no RA admin with this name already present: " + request.getName());
            }
            if (!StringUtils.isBlank(request.getClientAuthCert())) {
                String certDigest = util.getCertDigest(request.getClientAuthCert());
                if (RaServiceCache.getAccessControlSettingsConfig(certDigest) != null) {
                    logger.info("new RA admin certificate already configured in access control config file");
                    return errorResponse.generateErrorResponse("new RA admin client certificate already configured. " +
                            "Use other certificate");
                }
                List<RegistrationAuthorityAdmin> raAdmins = raAdminRepository.findRaAdminByClientCert(certDigest);
                if (raAdmins.size() != 0) {
                    logger.info("new RA admin certificate is already configured under RA admin configurations");
                    return errorResponse.generateErrorResponse("new RA admin client certificate already configured. " +
                            "Use other certificate");
                }
                List<RegistrationAuthorityOperator> raOps = raOperatorRepository.findRaOperatorByClientCert(certDigest);
                if (raOps.size() != 0) {
                    logger.info("new RA admin certificate is already configured under RA operator configurations");
                    return errorResponse.generateErrorResponse("new RA admin client certificate already configured. " +
                            "Use other certificate");
                }
                List<RegistrationAuthorityRelyingParty> raRps = raRpRepository.findRaRpByClientCert(certDigest);
                if (raRps.size() != 0) {
                    logger.info("new RA admin certificate is already configured under RA relying party configurations");
                    return errorResponse.generateErrorResponse("new RA admin client certificate already configured. " +
                            "Use other certificate");
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
                // TODO - check roles must be subset/equal to logged in admin roles

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
            RegistrationAuthorityAdmin regAuthAdmin = authenticate(httpRequest);
            if (regAuthAdmin == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthAdmin.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA admin status is disabled");
            }
            if (regAuthAdmin.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthAdmin.getRole().contains(RaServiceConstants.READ_ADMINS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to get further RA admins information");
            }
            List<RaAdmin> raAdminList = new ArrayList<>();
            for (RegistrationAuthorityAdmin regAuthorityAdmin: raAdminRepository.findAll()) {
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
            RegistrationAuthorityAdmin regAuthAdmin = authenticate(httpRequest);
            if (regAuthAdmin == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            if (regAuthAdmin.getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA admin status is disabled");
            }
            if (regAuthAdmin.getRegistrationAuthorityPK().getRegistrationAuthority().
                    getStatus().equals(RaServiceConstants.STATUS_DISABLED)) {
                return errorResponse.generateErrorResponse("RA status is disabled");
            }
            if (!regAuthAdmin.getRole().contains(RaServiceConstants.READ_ADMINS)) {
                return errorResponse.generateErrorResponse("RA admin not authorized to get further RA admins information");
            }
            List<RegistrationAuthorityAdmin> regAuthAdminList = raAdminRepository.findAdminByName(name);
            if (regAuthAdminList.size() == 0) {
                logger.info("no RA admin with this name already present: " + name);
                return errorResponse.generateErrorResponse("no RA admin with this name already present: " + name);
            }
            RegistrationAuthorityAdmin regAuthorityAdmin = regAuthAdminList.get(0);

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

    private RegistrationAuthorityAdmin authenticate(HttpServletRequest httpRequest) throws Exception {
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
}