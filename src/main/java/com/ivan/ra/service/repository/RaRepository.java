package com.ivan.ra.service.repository;

import com.ivan.ra.service.model.RegistrationAuthority;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RaRepository extends CrudRepository<RegistrationAuthority, String>  {
}
