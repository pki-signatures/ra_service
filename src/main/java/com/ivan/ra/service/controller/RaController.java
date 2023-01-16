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
import com.ivan.ra.service.repository.RaRepository;
import com.ivan.ra.service.repository.RaRpRepository;
import com.ivan.ra.service.service.ClientCertAuthService;
import com.ivan.ra.service.util.Util;
import com.ivan.ra.service.vo.Ra;
import com.ivan.ra.service.vo.RaAdmin;
import com.ivan.ra.service.vo.RaOperator;
import com.ivan.ra.service.vo.RaRp;
import com.ivan.ra.service.vo.RegisterRaRequest;
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
import java.util.Optional;
import java.util.Set;


@RestController
@Validated
public class RaController {
    @Autowired
    RaRepository raRepository;
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
    private static final Logger logger = LogManager.getLogger(RaController.class);

    @PostMapping(value = "/ra/v1/registration/authority", produces = "application/json", consumes = "application/json")
    public ResponseEntity registerRa(@Valid @RequestBody RegisterRaRequest registerRaRequest, HttpServletRequest httpRequest) {
        try {
            logger.info("register RA request received");
            String clientCertDigest = authService.authenticateSuperAdmins(httpRequest);
            if (clientCertDigest == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            String certDigest = util.getCertDigest(registerRaRequest.getRaAdminClientAuthCertificate());
            ResponseEntity error = authService.isClientCertAlreadyConfigured(certDigest);
            if (error != null) {
                return error;
            }
            Optional<RegistrationAuthority> ra = raRepository.findById(registerRaRequest.getName());
            if (ra.isPresent()) {
                logger.info("RA with this name already present: " + registerRaRequest.getName());
                return errorResponse.generateErrorResponse("RA with this name already present: " + registerRaRequest.getName());
            }

            RegistrationAuthority registrationAuthority = new RegistrationAuthority();
            registrationAuthority.setName(registerRaRequest.getName());

            RegistrationAuthorityPK registrationAuthorityPK = new RegistrationAuthorityPK();
            registrationAuthorityPK.setRegistrationAuthority(registrationAuthority);
            registrationAuthorityPK.setName(registerRaRequest.getRaAdminName());

            Optional<RegistrationAuthorityAdmin> raAdmin = raAdminRepository.findById(registrationAuthorityPK);
            if (raAdmin.isPresent()) {
                return errorResponse.generateErrorResponse("RA admin with this name already present: " + registerRaRequest.getName());
            }

            RegistrationAuthority regAuthority = new RegistrationAuthority();
            regAuthority.setName(registerRaRequest.getName());
            regAuthority.setOrganizationName(registerRaRequest.getOrganizationName());
            regAuthority.setOrganizationAddress(registerRaRequest.getOrganizationAddress());
            regAuthority.setOrganizationCity(registerRaRequest.getOrganizationCity());
            regAuthority.setOrganizationProvince(registerRaRequest.getOrganizationProvince());
            regAuthority.setOrganizationCountry(registerRaRequest.getOrganizationCountry());
            regAuthority.setPrimaryContactName(registerRaRequest.getPrimaryContactName());
            regAuthority.setPrimaryContactNo(registerRaRequest.getPrimaryContactNo());
            regAuthority.setPrimaryContactEmailAddress(registerRaRequest.getPrimaryContactEmailAddress());
            regAuthority.setSecondaryContactName(registerRaRequest.getSecondaryContactName());
            regAuthority.setSecondaryContactNo(registerRaRequest.getSecondaryContactNo());
            regAuthority.setSecondaryContactEmailAddress(registerRaRequest.getSecondaryContactEmailAddress());

            if (registerRaRequest.getRaProfiles() != null && registerRaRequest.getRaProfiles().length != 0) {
                Set<String> temp = new LinkedHashSet<>(Arrays.asList(registerRaRequest.getRaProfiles()));
                String[] raProfiles = temp.toArray(new String[temp.size()]);
                regAuthority.setRaProfiles(String.join(":", raProfiles));
            }
            RegistrationAuthority regAuthorityPersistent = raRepository.save(regAuthority);

            RegistrationAuthorityPK regAuthorityPK = new RegistrationAuthorityPK();
            regAuthorityPK.setRegistrationAuthority(regAuthorityPersistent);
            regAuthorityPK.setName(registerRaRequest.getRaAdminName());

            RegistrationAuthorityAdmin regAdmin = new RegistrationAuthorityAdmin();
            regAdmin.setRegistrationAuthorityPK(regAuthorityPK);
            regAdmin.setEmailAddress(registerRaRequest.getRaAdminEmailAddress());
            regAdmin.setClientAuthCertHash(certDigest);
            regAdmin.setClientAuthCert(registerRaRequest.getRaAdminClientAuthCertificate());
            regAdmin.setStatus(RaServiceConstants.STATUS_ENABLED);

            List<String> rolesList = new ArrayList<>();
            rolesList.add(RaServiceConstants.CREATE_ADMINS);
            rolesList.add(RaServiceConstants.READ_ADMINS);
            rolesList.add(RaServiceConstants.UPDATE_ADMINS);
            rolesList.add(RaServiceConstants.CREATE_OPERATORS);
            rolesList.add(RaServiceConstants.READ_OPERATORS);
            rolesList.add(RaServiceConstants.UPDATE_OPERATORS);
            rolesList.add(RaServiceConstants.CREATE_RELYING_PARTIES);
            rolesList.add(RaServiceConstants.READ_RELYING_PARTIES);
            rolesList.add(RaServiceConstants.UPDATE_RELYING_PARTIES);

            Set<String> temp = new LinkedHashSet<>(rolesList);
            String[] raAdminRoles = temp.toArray(new String[temp.size()]);
            regAdmin.setRole(String.join(":", raAdminRoles));
            raAdminRepository.save(regAdmin);
            logger.info("new RA registered successfully");

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(value = "/ra/v1/registration/authority", produces = "application/json", consumes = "application/json")
    public ResponseEntity updateRa(@Valid @RequestBody Ra request, HttpServletRequest httpRequest) {
        try {
            logger.info("update RA information request received");
            String clientCertDigest = authService.authenticateSuperAdmins(httpRequest);
            if (clientCertDigest == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            Optional<RegistrationAuthority> ra = raRepository.findById(request.getName());
            if (ra.isEmpty()) {
                logger.info("RA with this name not present: " + request.getName());
                return errorResponse.generateErrorResponse("RA with this name not present: " + request.getName());
            }
            RegistrationAuthority regAuthority = ra.get();
            if (!StringUtils.isBlank(request.getStatus())) {
                regAuthority.setStatus(request.getStatus());
            }
            if (!StringUtils.isBlank(request.getOrganizationName())) {
                regAuthority.setOrganizationName(request.getOrganizationName());
            }
            if (!StringUtils.isBlank(request.getOrganizationAddress())) {
                regAuthority.setOrganizationAddress(request.getOrganizationAddress());
            }
            if (!StringUtils.isBlank(request.getOrganizationCity())) {
                regAuthority.setOrganizationCity(request.getOrganizationCity());
            }
            if (!StringUtils.isBlank(request.getOrganizationProvince())) {
                regAuthority.setOrganizationProvince(request.getOrganizationProvince());
            }
            if (!StringUtils.isBlank(request.getOrganizationCountry())) {
                regAuthority.setOrganizationCountry(request.getOrganizationCountry());
            }
            if (!StringUtils.isBlank(request.getPrimaryContactName())) {
                regAuthority.setPrimaryContactName(request.getPrimaryContactName());
            }
            if (!StringUtils.isBlank(request.getPrimaryContactNo())) {
                regAuthority.setPrimaryContactNo(request.getPrimaryContactNo());
            }
            if (!StringUtils.isBlank(request.getPrimaryContactEmailAddress())) {
                regAuthority.setPrimaryContactEmailAddress(request.getPrimaryContactEmailAddress());
            }
            if (!StringUtils.isBlank(request.getSecondaryContactName())) {
                regAuthority.setSecondaryContactName(request.getSecondaryContactName());
            }
            if (!StringUtils.isBlank(request.getSecondaryContactNo())) {
                regAuthority.setSecondaryContactNo(request.getSecondaryContactNo());
            }
            if (!StringUtils.isBlank(request.getSecondaryContactEmailAddress())) {
                regAuthority.setSecondaryContactEmailAddress(request.getSecondaryContactEmailAddress());
            }
            if (request.getRaProfiles() != null && request.getRaProfiles().length != 0) {
                List<String> raProfilesList = new ArrayList<>(Arrays.asList(regAuthority.getRaProfiles().split(":")));
                raProfilesList.addAll(Arrays.asList(request.getRaProfiles()));

                Set<String> temp = new LinkedHashSet<>(raProfilesList);
                String[] raProfiles = temp.toArray(new String[temp.size()]);
                regAuthority.setRaProfiles(String.join(":", raProfiles));
            }
            raRepository.save(regAuthority);
            logger.info("RA updated successfully");

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority", produces = "application/json")
    public ResponseEntity getAllRas(HttpServletRequest httpRequest) {
        try {
            logger.info("get all RA information request received");
            String clientCertDigest = authService.authenticateSuperAdmins(httpRequest);
            if (clientCertDigest == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            List<Ra> ras = new ArrayList<>();
            for (RegistrationAuthority regAuth : raRepository.findAll()) {
                Ra ra = new Ra();
                ra.setName(regAuth.getName());
                ra.setStatus(regAuth.getStatus());
                ra.setOrganizationName(regAuth.getOrganizationName());
                ra.setOrganizationAddress(regAuth.getOrganizationAddress());
                ra.setOrganizationCity(regAuth.getOrganizationCity());
                ra.setOrganizationProvince(regAuth.getOrganizationProvince());
                ra.setOrganizationCountry(regAuth.getOrganizationCountry());
                ra.setPrimaryContactName(regAuth.getPrimaryContactName());
                ra.setPrimaryContactNo(regAuth.getPrimaryContactNo());
                ra.setPrimaryContactEmailAddress(regAuth.getPrimaryContactEmailAddress());
                ra.setSecondaryContactName(regAuth.getSecondaryContactName());
                ra.setSecondaryContactNo(regAuth.getSecondaryContactNo());
                ra.setSecondaryContactEmailAddress(regAuth.getSecondaryContactEmailAddress());
                ra.setRaProfiles(regAuth.getRaProfiles().split(":"));
                ras.add(ra);
            }
            logger.info("all RA's information sent successfully");
            return ResponseEntity.ok().body(ras);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/{name}", produces = "application/json")
    public ResponseEntity getRa(@RequestParam("name") @NotBlank String name, HttpServletRequest httpRequest) {
        try {
            logger.info("get RA by name request received");
            String clientCertDigest = authService.authenticateSuperAdmins(httpRequest);
            if (clientCertDigest == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            Optional<RegistrationAuthority> raPresent = raRepository.findById(name);
            if (raPresent.isEmpty()) {
                logger.info("RA with this name not present: " + name);
                return errorResponse.generateErrorResponse("RA with this name not present: " + name);
            }
            RegistrationAuthority regAuth = raPresent.get();

            Ra ra = new Ra();
            ra.setName(regAuth.getName());
            ra.setStatus(regAuth.getStatus());
            ra.setOrganizationName(regAuth.getOrganizationName());
            ra.setOrganizationAddress(regAuth.getOrganizationAddress());
            ra.setOrganizationCity(regAuth.getOrganizationCity());
            ra.setOrganizationProvince(regAuth.getOrganizationProvince());
            ra.setOrganizationCountry(regAuth.getOrganizationCountry());
            ra.setPrimaryContactName(regAuth.getPrimaryContactName());
            ra.setPrimaryContactNo(regAuth.getPrimaryContactNo());
            ra.setPrimaryContactEmailAddress(regAuth.getPrimaryContactEmailAddress());
            ra.setSecondaryContactName(regAuth.getSecondaryContactName());
            ra.setSecondaryContactNo(regAuth.getSecondaryContactNo());
            ra.setSecondaryContactEmailAddress(regAuth.getSecondaryContactEmailAddress());
            ra.setRaProfiles(regAuth.getRaProfiles().split(":"));

            logger.info("RA information sent successfully");
            return ResponseEntity.ok().body(ra);
        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/admins/{name}", produces = "application/json")
    public ResponseEntity getAllAdminsByRa(@RequestParam("name") @NotBlank String name, HttpServletRequest httpRequest) {
        try {
            logger.info("get all RA admins by RA request received");
            String clientCertDigest = authService.authenticateSuperAdmins(httpRequest);
            if (clientCertDigest == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            List<RaAdmin> raAdminsList = new ArrayList<>();
            for (RegistrationAuthorityAdmin regAuthAdmin : raAdminRepository.findAdminsByRa(name)) {
                RaAdmin raAdmin = new RaAdmin();
                raAdmin.setName(regAuthAdmin.getRegistrationAuthorityPK().getName());
                raAdmin.setEmailAddress(regAuthAdmin.getEmailAddress());
                raAdmin.setStatus(regAuthAdmin.getStatus());
                raAdmin.setRoles(regAuthAdmin.getRole().split(":"));
                raAdmin.setClientAuthCert(regAuthAdmin.getClientAuthCert());
                raAdminsList.add(raAdmin);
            }
            logger.info("get all RA admins by RA information sent successfully");
            return ResponseEntity.ok().body(raAdminsList);

        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/operators/{name}", produces = "application/json")
    public ResponseEntity getAllOperatorsByRa(@RequestParam("name") @NotBlank String name, HttpServletRequest httpRequest) {
        try {
            logger.info("get all RA operators by RA request received");
            String clientCertDigest = authService.authenticateSuperAdmins(httpRequest);
            if (clientCertDigest == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            List<RaOperator> raAdminsList = new ArrayList<>();
            for (RegistrationAuthorityOperator regAuthOp : raOperatorRepository.findOperatorsByRa(name)) {
                RaOperator raOperator = new RaOperator();
                raOperator.setName(regAuthOp.getRegistrationAuthorityPK().getName());
                raOperator.setEmailAddress(regAuthOp.getEmailAddress());
                raOperator.setStatus(regAuthOp.getStatus());
                raOperator.setRoles(regAuthOp.getRole().split(":"));
                raOperator.setClientAuthCert(regAuthOp.getClientAuthCert());
                raAdminsList.add(raOperator);
            }
            logger.info("get all RA operators by RA information sent successfully");
            return ResponseEntity.ok().body(raAdminsList);

        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/ra/v1/registration/authority/rps/{name}", produces = "application/json")
    public ResponseEntity getAllRpsByRa(@RequestParam("name") @NotBlank String name, HttpServletRequest httpRequest) {
        try {
            logger.info("get all RA relying parties by RA request received");
            String clientCertDigest = authService.authenticateSuperAdmins(httpRequest);
            if (clientCertDigest == null) {
                return errorResponse.generateErrorResponse("TLS client certificate authentication failed");
            }
            List<RaRp> raRpList = new ArrayList<>();
            for (RegistrationAuthorityRelyingParty regAuthRp : raRpRepository.findRpsByRa(name)) {
                RaRp raRp = new RaRp();
                raRp.setName(regAuthRp.getRegistrationAuthorityPK().getName());
                raRp.setStatus(regAuthRp.getStatus());
                raRp.setClientAuthCert(regAuthRp.getClientAuthCert());
                raRp.setPrimaryContactName(regAuthRp.getPrimaryContactName());
                raRp.setPrimaryContactNo(regAuthRp.getPrimaryContactNo());
                raRp.setPrimaryContactEmailAddress(regAuthRp.getPrimaryContactEmailAddress());
                raRp.setSecondaryContactName(regAuthRp.getSecondaryContactName());
                raRp.setSecondaryContactNo(regAuthRp.getSecondaryContactNo());
                raRp.setSecondaryContactEmailAddress(regAuthRp.getSecondaryContactEmailAddress());
                raRpList.add(raRp);
            }
            logger.info("get all RA relying parties by RA information sent successfully");
            return ResponseEntity.ok().body(raRpList);

        } catch (Exception ex) {
            logger.error("", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

}
