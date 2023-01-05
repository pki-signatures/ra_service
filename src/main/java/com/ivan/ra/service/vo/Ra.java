package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ra {

    @NotBlank(message = "name must be present")
    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("organization_name")
    private String organizationName;

    @JsonProperty("organization_address")
    private String organizationAddress;

    @JsonProperty("organization_city")
    private String organizationCity;

    @JsonProperty("organization_province")
    private String organizationProvince;

    @JsonProperty("organization_country")
    private String organizationCountry;

    @JsonProperty("primary_contact_name")
    private String primaryContactName;

    @JsonProperty("primary_contact_no")
    private String primaryContactNo;

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
}
