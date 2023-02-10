package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivan.ra.service.validator.NoWhiteSpace;

import javax.validation.constraints.NotBlank;

public class RejectRevokeRequest {

    @NotBlank(message = "request id must be present")
    @NoWhiteSpace
    @JsonProperty("request_id")
    private String requestId;

    @NotBlank(message = "rejection reason must be present")
    @NoWhiteSpace
    @JsonProperty("rejection_reason")
    private String rejectionReason;
}
