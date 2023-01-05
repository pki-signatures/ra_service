package com.ivan.ra.service.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
@EqualsAndHashCode
public class RegistrationAuthorityPK implements Serializable {

    @Column(name = "name", length = 100)
    private String name;

    @JoinColumns({
            @JoinColumn(name = "ra_name", referencedColumnName = "name")
    })
    @ManyToOne
    private RegistrationAuthority registrationAuthority;
}
