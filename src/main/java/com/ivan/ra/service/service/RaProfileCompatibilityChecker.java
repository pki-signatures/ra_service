package com.ivan.ra.service.service;

import com.ivan.ra.service.cache.RaServiceCache;
import com.ivan.ra.service.config.ra.profile.RaProfileVO;
import com.ivan.ra.service.config.ra.profile.SubjectInfoVO;
import com.ivan.ra.service.constants.RaServiceConstants;
import com.ivan.ra.service.vo.RaRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class RaProfileCompatibilityChecker {
    private static final Logger logger = LogManager.getLogger(RaProfileCompatibilityChecker.class);

    public SubjectInfoVO getFinalSubjectInfo(String raProfileReq, RaRequest raRequest) throws Exception {
        SubjectInfoVO subjectInfoFinal = new SubjectInfoVO();

        RaProfileVO raProfile = RaServiceCache.getRaProfile(raProfileReq);
        if (raProfile == null) {
            throw new Exception("invalid RA profile in request: " + raProfileReq);
        }

        SubjectInfoVO subjectInfoProfile = raProfile.getSubjectInfo();
        subjectInfoFinal.setCommonName(checkRDN(raRequest.getCommonName(), subjectInfoProfile.getCommonName(),
                RaServiceConstants.COMMON_NAME));
        subjectInfoFinal.setGivenName(checkRDN(raRequest.getGivenName(), subjectInfoProfile.getGivenName(),
                RaServiceConstants.GIVEN_NAME));
        subjectInfoFinal.setSurname(checkRDN(raRequest.getSurname(), subjectInfoProfile.getSurname(),
                RaServiceConstants.SURNAME));
        subjectInfoFinal.setOrganization(checkRDN(raRequest.getOrganization(),
                subjectInfoProfile.getOrganization(), RaServiceConstants.ORGANIZATION));
        subjectInfoFinal.setOrganizationUnit(checkRDN(raRequest.getOrganizationUnit(),
                subjectInfoProfile.getOrganizationUnit(), RaServiceConstants.ORGANIZATION_UNIT));
        subjectInfoFinal.setOrganizationIdentifier(checkRDN(raRequest.getOrganizationIdentifier(),
                subjectInfoProfile.getOrganizationIdentifier(), RaServiceConstants.ORGANIZATION_IDENTIFIER));
        subjectInfoFinal.setCountry(checkRDN(raRequest.getCountry(), subjectInfoProfile.getCountry(),
                RaServiceConstants.COUNTRY));
        subjectInfoFinal.setSerialNumber(checkRDN(raRequest.getSerialNumber(),
                subjectInfoProfile.getSerialNumber(), RaServiceConstants.SERIAL_NUMBER));
        return subjectInfoFinal;
    }

    private String checkRDN(String RDNValueReq, String RDNValueProfile, String RDNName)
            throws Exception {
        if (RDNValueProfile != null) {
            if (!RDNValueProfile.equals("")) {
                logger.info("using default value of " + RDNName + "configured in profile: " + RDNValueProfile);
                return RDNValueProfile;
            } else {
                if (RDNValueReq == null || RDNValueReq.equals("")) {
                    throw new Exception(RDNName + " must be present in request as per certificate profile");
                } else {
                    logger.info("using "+RDNName+ " value from request: " + RDNValueReq);
                    return RDNValueReq;
                }
            }
        }
        return null;
    }
}
