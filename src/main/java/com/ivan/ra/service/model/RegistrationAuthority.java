package com.ivan.ra.service.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "registration_authorities")
public class RegistrationAuthority {

    @Id
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "organization_name", nullable = false, length = 100)
    private String organizationName;

    @Column(name = "organization_address", nullable = true, length = 200)
    private String organizationAddress;

    @Column(name = "organization_city", nullable = true, length = 50)
    private String organizationCity;

    @Column(name = "organization_province", nullable = true, length = 50)
    private String organizationProvince;

    @Column(name = "organization_country", nullable = false, length = 50)
    private String organizationCountry;

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

    @Column(name = "ra_profiles", nullable = true, columnDefinition = "TEXT")
    private String raProfiles;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updateDateTime;
}
