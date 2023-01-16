package com.ivan.ra.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchRaRequest {

    @JsonProperty("request_id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("mobile_no")
    private String mobileNo;

    @JsonProperty("email_address")
    private String emailAddress;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("from_date")
    private String fromDate;

    @JsonProperty("to_date")
    private String toDate;

    @JsonProperty("page_no")
    private int pageNo;

    @JsonProperty("page_size")
    private int pageSize;
}
