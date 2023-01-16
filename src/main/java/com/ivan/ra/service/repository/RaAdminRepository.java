package com.ivan.ra.service.repository;

import com.ivan.ra.service.model.RegistrationAuthorityAdmin;
import com.ivan.ra.service.model.RegistrationAuthorityPK;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaAdminRepository extends CrudRepository<RegistrationAuthorityAdmin, RegistrationAuthorityPK> {
    @Query("from RegistrationAuthorityAdmin raAdmin where raAdmin.clientAuthCertHash =:clientAuthCertHash")
    List<RegistrationAuthorityAdmin> findRaAdminByClientCert(@Param("clientAuthCertHash") String clientAuthCertHash);

    @Query("from RegistrationAuthorityAdmin raAdmin where raAdmin.registrationAuthorityPK.registrationAuthority.name =:raName")
    List<RegistrationAuthorityAdmin> findAdminsByRa(@Param("raName") String raName);

    @Query("from RegistrationAuthorityAdmin raAdmin where raAdmin.registrationAuthorityPK.registrationAuthority.name =:raName and" +
            " raAdmin.registrationAuthorityPK.name =:adminName")
    List<RegistrationAuthorityAdmin> findAdminByNameAndRa(@Param("raName") String raName, @Param("adminName") String adminName);

    @Query("from RegistrationAuthorityAdmin raAdmin where raAdmin.registrationAuthorityPK.registrationAuthority.name =:raName")
    List<RegistrationAuthorityAdmin> findAllAdminsByRa(@Param("raName") String raName);
}
