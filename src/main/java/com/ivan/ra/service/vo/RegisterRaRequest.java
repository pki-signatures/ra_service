package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivan.ra.service.validator.NoWhiteSpace;
import com.ivan.ra.service.validator.StatusValueCheck;
import com.ivan.ra.service.validator.X509Cert;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterRaRequest {

    @NotBlank(message = "name must be present")
    @NoWhiteSpace
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "status must be present")
    @StatusValueCheck
    @JsonProperty("status")
    private String status;

    @NotBlank(message = "organization name must be present")
    @JsonProperty("organization_name")
    private String organizationName;

    @JsonProperty("organization_address")
    private String organizationAddress;

    @JsonProperty("organization_city")
    private String organizationCity;

    @JsonProperty("organization_province")
    private String organizationProvince;

    @NotBlank(message = "organization country must be present")
    @JsonProperty("organization_country")
    private String organizationCountry;

    @NotBlank(message = "primary contact name must be present")
    @JsonProperty("primary_contact_name")
    private String primaryContactName;

    @NotBlank(message = "primary contact number must be present")
    @JsonProperty("primary_contact_no")
    private String primaryContactNo;

    @NotBlank(message = "primary contact email address must be present")
    @Email(message = "primary contact email address must be of valid format")
    @JsonProperty("primary_contact_email_address")
    private String primaryContactEmailAddress;

    @JsonProperty("secondary_contact_name")
    private String secondaryContactName;

    @JsonProperty("secondary_contact_no")
    private String secondaryContactNo;

    @Email(message = "secondary contact email address must be of valid format")
    @JsonProperty("secondary_contact_email_address")
    private String secondaryContactEmailAddress;

    @JsonProperty("ra_profiles")
    private String[] raProfiles;

    @NotBlank (message = "ra admin name must be present")
    @JsonProperty("ra_admin_name")
    private String raAdminName;

    @NotBlank (message = "ra admin email address must be present")
    @Email(message = "ra admin email address must be of valid format")
    @JsonProperty("ra_admin_email_address")
    private String raAdminEmailAddress;

    @NotBlank(message = "ra admin client certificate must be present")
    @X509Cert
    @JsonProperty("ra_admin_client_auth_certificate")
    private String raAdminClientAuthCertificate;
}
