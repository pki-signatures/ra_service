package com.ivan.ra.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import iaik.pkcs.pkcs11.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;

public class RaServiceCache {

    private static final Logger logger = LogManager.getLogger(RaServiceCache.class);

    private static final HashMap<String, AccessControlSettingsConfig> accessControlSettingsMap = new HashMap<>();

    public static void loadAccessControlSettings(String accessControlSettingsPath) throws Exception {
        logger.info("loading access control settings from path: " + accessControlSettingsPath);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        AccessControlSettingsConfig[]  accessControlSettingsConfigs = mapper.readValue(new File(accessControlSettingsPath),
                AccessControlSettingsConfig[].class);
        if (accessControlSettingsConfigs.length == 0) {
            throw new Exception("no client configured in access control settings config file");
        }

        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        for (AccessControlSettingsConfig config: accessControlSettingsConfigs) {
            byte[] clientCertBytes = Files.readAllBytes(Paths.get(config.getClientCertPath()));
            X509Certificate clientCert = (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(clientCertBytes));

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(clientCert.getEncoded());
            String digest = Base64.getEncoder().encodeToString(md.digest());

            if (accessControlSettingsMap.containsKey(digest)) {
                throw new Exception("every client must have a unique TLS client authentication certificate configured");
            }
            accessControlSettingsMap.put(digest, config);
        }
        logger.info("access control settings loaded from path: " + accessControlSettingsPath);
    }

    public static AccessControlSettingsConfig getAccessControlSettingsConfig(String certDigest) {
        return accessControlSettingsMap.get(certDigest);
    }
}
