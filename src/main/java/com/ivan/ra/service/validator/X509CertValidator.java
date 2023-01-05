package com.ivan.ra.service.validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Base64;

public class X509CertValidator implements ConstraintValidator<X509Cert, String> {

    private static final Logger logger = LogManager.getLogger(X509CertValidator.class);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        try {
            if (StringUtils.isBlank(value)) {
                return true;
            }
            byte[] cert = Base64.getDecoder().decode(value);
            X509CertificateHolder certHolder = new X509CertificateHolder(cert);
            logger.info("valid format certificate: "+certHolder.getSubject());
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }
}
