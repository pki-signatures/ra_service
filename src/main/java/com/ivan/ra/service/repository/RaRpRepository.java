package com.ivan.ra.service.repository;

import com.ivan.ra.service.model.RegistrationAuthorityOperator;
import com.ivan.ra.service.model.RegistrationAuthorityPK;
import com.ivan.ra.service.model.RegistrationAuthorityRelyingParty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaRpRepository extends CrudRepository<RegistrationAuthorityRelyingParty, RegistrationAuthorityPK> {
    @Query("from RegistrationAuthorityRelyingParty raRp where raRp.clientAuthCertHash =:clientAuthCertHash")
    List<RegistrationAuthorityRelyingParty> findRaRpByClientCert(@Param("clientAuthCertHash") String clientAuthCertHash);

    @Query("from RegistrationAuthorityRelyingParty raRp where raRp.registrationAuthorityPK.registrationAuthority.name =:raName")
    List<RegistrationAuthorityRelyingParty> findRpsByRa(@Param("raName") String raName);

    @Query("from RegistrationAuthorityRelyingParty raRp where raRp.registrationAuthorityPK.registrationAuthority.name =:raName and" +
            " raRp.registrationAuthorityPK.name =:rpName")
    List<RegistrationAuthorityRelyingParty> findRpByNameAndRa(@Param("raName") String raName, @Param("rpName") String rpName);

    @Query("from RegistrationAuthorityRelyingParty raRp where raRp.registrationAuthorityPK.registrationAuthority.name =:raName")
    List<RegistrationAuthorityRelyingParty> findAllRpsByRa(@Param("raName") String raName);
}
