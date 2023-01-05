package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class RaAdmin {

    @NotBlank (message = "name must be present")
    @JsonProperty("name")
    private String name;

    @NotBlank (message = "email address must be present")
    @Email(message = "email address must be of valid format")
    @JsonProperty("email_address")
    private String emailAddress;

    @NotBlank(message = "client certificate must be present")
    @X509Cert
    @JsonProperty("client_auth_certificate")
    private String clientAuthCert;

    @JsonProperty("roles")
    private String[] roles;

    @JsonProperty("status")
    private String status;
}