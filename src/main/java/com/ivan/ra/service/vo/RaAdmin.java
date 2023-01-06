package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivan.ra.service.validator.AdminRole;
import com.ivan.ra.service.validator.NoWhiteSpace;
import com.ivan.ra.service.validator.OnCreate;
import com.ivan.ra.service.validator.StatusValueCheck;
import com.ivan.ra.service.validator.X509Cert;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaAdmin {

    @NotBlank (message = "name must be present")
    @NoWhiteSpace
    @JsonProperty("name")
    private String name;

    @NotBlank (message = "email address must be present", groups = OnCreate.class)
    @Email(message = "email address must be of valid format")
    @JsonProperty("email_address")
    private String emailAddress;

    @NotBlank(message = "client certificate must be present", groups = OnCreate.class)
    @X509Cert
    @JsonProperty("client_auth_certificate")
    private String clientAuthCert;

    @NotNull(message = "roles must be present", groups = OnCreate.class)
    @AdminRole
    @JsonProperty("roles")
    private String[] roles;

    @NotBlank(message = "status must be present", groups = OnCreate.class)
    @StatusValueCheck
    @JsonProperty("status")
    private String status;
}