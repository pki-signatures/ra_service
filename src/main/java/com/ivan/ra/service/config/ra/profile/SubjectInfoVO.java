package com.ivan.ra.service.config.ra.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubjectInfoVO {

    @JsonProperty("common_name")
    private String commonName;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("organization_unit")
    private String organizationUnit;

    @JsonProperty("organization_identifier")
    private String organizationIdentifier;

    @JsonProperty("country")
    private String country;

    @JsonProperty("serial_number")
    private String serialNumber;
}
