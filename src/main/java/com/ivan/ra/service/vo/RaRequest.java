package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivan.ra.service.validator.NoWhiteSpace;
import com.ivan.ra.service.validator.OnUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaRequest {

    @NotBlank(message = "id must be present", groups = OnUpdate.class)
    @NoWhiteSpace
    @JsonProperty("id")
    private String id;

    @JsonProperty("email_address")
    private String emailAddress;

    @JsonProperty("mobile_no")
    private String mobileNo;

    @JsonProperty("ra_profile")
    private String raProfile;

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
