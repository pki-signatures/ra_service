package com.ivan.ra.service.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "registration_authorities_operators")
public class RegistrationAuthorityOperator {

    @EmbeddedId
    private RegistrationAuthorityPK registrationAuthorityPK;

    @Column(name = "email_address", nullable = false, length = 50)
    private String emailAddress;

    @Column(name = "client_auth_cert_hash", nullable = false, length = 200)
    private String clientAuthCertHash;

    @Column(name = "client_auth_cert", nullable = false, columnDefinition = "TEXT")
    private String clientAuthCert;

    @Column(name = "role", nullable = false, length = 200)
    private String role;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updateDateTime;
}
