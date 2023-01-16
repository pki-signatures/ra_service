package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivan.ra.service.validator.NoWhiteSpace;
import com.ivan.ra.service.validator.OnCreate;
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
public class RaRp {

    @NotBlank(message = "name must be present")
    @NoWhiteSpace
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "client certificate must be present", groups = OnCreate.class)
    @X509Cert
    @JsonProperty("client_auth_certificate")
    private String clientAuthCert;

    @NotBlank(message = "status must be present", groups = OnCreate.class)
    @StatusValueCheck
    @JsonProperty("status")
    private String status;

    @NotBlank(message = "primary contact name must be present", groups = OnCreate.class)
    @JsonProperty("primary_contact_name")
    private String primaryContactName;

    @NotBlank(message = "primary contact number must be present", groups = OnCreate.class)
    @JsonProperty("primary_contact_no")
    private String primaryContactNo;

    @NotBlank(message = "primary contact email address must be present", groups = OnCreate.class)
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
}
