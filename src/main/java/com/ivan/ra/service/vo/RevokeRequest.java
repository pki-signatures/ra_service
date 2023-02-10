package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivan.ra.service.validator.NoWhiteSpace;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevokeRequest {

    @NotBlank(message = "request id must be present")
    @NoWhiteSpace
    @JsonProperty("request_id")
    private String requestId;

}
