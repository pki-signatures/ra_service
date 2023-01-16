package com.ivan.ra.service.repository;

import com.ivan.ra.service.model.RegistrationAuthorityAdmin;
import com.ivan.ra.service.model.RegistrationAuthorityOperator;
import com.ivan.ra.service.model.RegistrationAuthorityPK;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaOperatorRepository extends CrudRepository<RegistrationAuthorityOperator, RegistrationAuthorityPK> {
    @Query("from RegistrationAuthorityOperator raOp where raOp.clientAuthCertHash =:clientAuthCertHash")
    List<RegistrationAuthorityOperator> findRaOperatorByClientCert(@Param("clientAuthCertHash") String clientAuthCertHash);

    @Query("from RegistrationAuthorityOperator raOp where raOp.registrationAuthorityPK.registrationAuthority.name =:raName")
    List<RegistrationAuthorityOperator> findOperatorsByRa(@Param("raName") String raName);

    @Query("from RegistrationAuthorityOperator raOp where raOp.registrationAuthorityPK.registrationAuthority.name =:raName and" +
            " raOp.registrationAuthorityPK.name =:opName")
    List<RegistrationAuthorityOperator> findOperatorByNameAndRa(@Param("raName") String raName, @Param("opName") String opName);

    @Query("from RegistrationAuthorityOperator raOp where raOp.registrationAuthorityPK.registrationAuthority.name =:raName")
    List<RegistrationAuthorityOperator> findAllOperatorsByRa(@Param("raName") String raName);
}
