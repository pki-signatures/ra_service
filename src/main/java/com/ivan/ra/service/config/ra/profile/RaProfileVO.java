package com.ivan.ra.service.config.ra.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RaProfileVO {

    @JsonProperty("name")
    private String name;

    @JsonProperty("subject_info")
    private SubjectInfoVO subjectInfo;

    @JsonProperty("cert_profile")
    private String certProfile;
}
