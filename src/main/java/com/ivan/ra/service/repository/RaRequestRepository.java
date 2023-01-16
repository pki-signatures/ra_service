package com.ivan.ra.service.repository;

import com.ivan.ra.service.model.RegistrationAuthorityRequest;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface RaRequestRepository  extends PagingAndSortingRepository<RegistrationAuthorityRequest, String>,
        QuerydslPredicateExecutor<RegistrationAuthorityRequest> {
}
