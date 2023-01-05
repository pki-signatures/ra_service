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
@Table(name = "registration_authorities_relying_parties")
public class RegistrationAuthorityRelyingParty {

    @EmbeddedId
    private RegistrationAuthorityPK registrationAuthorityPK;

    @Column(name = "client_auth_cert_hash", nullable = false, length = 200)
    private String clientAuthCertHash;

    @Column(name = "client_auth_cert", nullable = false, columnDefinition = "TEXT")
    private String clientAuthCert;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "primary_contact_name", nullable = false, length = 100)
    private String primaryContactName;

    @Column(name = "primary_contact_no", nullable = false, length = 50)
    private String primaryContactNo;

    @Column(name = "primary_contact_email_address", nullable = false, length = 50)
    private String primaryContactEmailAddress;

    @Column(name = "secondary_contact_name", nullable = true, length = 100)
    private String secondaryContactName;

    @Column(name = "secondary_contact_no", nullable = true, length = 50)
    private String secondaryContactNo;

    @Column(name = "secondary_contact_email_address", nullable = true, length = 50)
    private String secondaryContactEmailAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updateDateTime;
}
