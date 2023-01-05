package com.ivan.ra.service.cache;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccessControlSettingsConfig {

    @JsonProperty("enable")
    private boolean enable;

    @JsonProperty("name")
    private String name;

    @JsonProperty("client_cert_path")
    private String clientCertPath;
}

