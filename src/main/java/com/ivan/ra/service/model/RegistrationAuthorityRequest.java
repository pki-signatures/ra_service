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
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "registration_authorities_requests")
public class RegistrationAuthorityRequest {

    @EmbeddedId
    private RegistrationAuthorityRequestPK registrationAuthorityRequestPK;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "rejection_reason", nullable = true, length = 300)
    private String rejectionReason;

    @Column(name = "ra_profile", nullable = false, length = 100)
    private String raProfile;

    @Column(name = "mobile_no", nullable = false, length = 50)
    private String mobileNo;

    @Column(name = "email_address", nullable = false, length = 50)
    private String emailAddress;

    @Column(name = "ssa_id", nullable = true, length = 100)
    private String ssaId;

    @Column(name = "ccr", nullable = true, columnDefinition = "TEXT")
    private String csr;

    @Column(name = "end_entity_certificate", nullable = true, columnDefinition = "TEXT")
    private String endEntityCertificate;

    @Column(name = "issuer_certificate", nullable = true, columnDefinition = "TEXT")
    private String issuerCertificate;

    @Column(name = "revocation_code", nullable = true, length = 100)
    private String revocationCode;

    @JoinColumns({
            @JoinColumn(name = "request_subject_id", referencedColumnName = "id")
    })
    @OneToOne
    private RequestSubjectInfo requestSubjectInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updateDateTime;
}
