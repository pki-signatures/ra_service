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
@Table(name = "request_subject_info")
public class RequestSubjectInfo {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "common_name", nullable = false, length = 100)
    private String commonName;

    @Column(name = "given_name", nullable = true, length = 100)
    private String givenName;

    @Column(name = "surname", nullable = true, length = 100)
    private String surname;

    @Column(name = "organization", nullable = true, length = 100)
    private String organization;

    @Column(name = "organization_unit", nullable = true, length = 100)
    private String organizationUnit;

    @Column(name = "organization_identifier", nullable = true, length = 100)
    private String organizationIdentifier;

    @Column(name = "country", nullable = true, length = 20)
    private String country;

    @Column(name = "serial_number", nullable = true, length = 100)
    private String serialNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updateDateTime;
}
