package com.ivan.ra.service.repository;

import com.ivan.ra.service.model.RegistrationAuthorityRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaRequestRepository  extends PagingAndSortingRepository<RegistrationAuthorityRequest, String>,
        QuerydslPredicateExecutor<RegistrationAuthorityRequest> {

    @Query("from RegistrationAuthorityRequest raReq where raReq.registrationAuthorityRequestPK.registrationAuthority.name =:raName and" +
            " raReq.registrationAuthorityRequestPK.id =:requestId")
    List<RegistrationAuthorityRequest> findRequestByIdAndRa(@Param("raName") String raName, @Param("requestId") String requestId);
}
