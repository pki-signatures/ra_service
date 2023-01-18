package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivan.ra.service.validator.NoWhiteSpace;
import com.ivan.ra.service.validator.OnCreate;
import com.ivan.ra.service.validator.OnUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaRequest {

    @NotBlank(message = "id must be present", groups = OnUpdate.class)
    @NoWhiteSpace
    @JsonProperty("id")
    private String id;

    @NotBlank(message = "email address must be present", groups = OnCreate.class)
    @Email(message = "email address must be of valid format")
    @JsonProperty("email_address")
    private String emailAddress;

    @NotBlank(message = "mobile number must be present", groups = OnCreate.class)
    @JsonProperty("mobile_no")
    private String mobileNo;

    @NotBlank(message = "ra profile must be provided", groups = OnCreate.class)
    @Null(message = "ra profile cannot be provided", groups = OnUpdate.class)
    @JsonProperty("ra_profile")
    private String raProfile;

    @Null(message = "status cannot be provided", groups = OnCreate.class)
    @JsonProperty("status")
    private String status;

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
