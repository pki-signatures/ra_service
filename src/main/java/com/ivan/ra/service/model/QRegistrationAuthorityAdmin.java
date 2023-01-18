package com.ivan.ra.service.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRegistrationAuthorityAdmin is a Querydsl query type for RegistrationAuthorityAdmin
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRegistrationAuthorityAdmin extends EntityPathBase<RegistrationAuthorityAdmin> {

    private static final long serialVersionUID = 597121243L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRegistrationAuthorityAdmin registrationAuthorityAdmin = new QRegistrationAuthorityAdmin("registrationAuthorityAdmin");

    public final StringPath clientAuthCert = createString("clientAuthCert");

    public final StringPath clientAuthCertHash = createString("clientAuthCertHash");

    public final DateTimePath<java.time.LocalDateTime> createDateTime = createDateTime("createDateTime", java.time.LocalDateTime.class);

    public final StringPath emailAddress = createString("emailAddress");

    public final QRegistrationAuthorityPK registrationAuthorityPK;

    public final StringPath role = createString("role");

    public final StringPath status = createString("status");

    public final DateTimePath<java.time.LocalDateTime> updateDateTime = createDateTime("updateDateTime", java.time.LocalDateTime.class);

    public QRegistrationAuthorityAdmin(String variable) {
        this(RegistrationAuthorityAdmin.class, forVariable(variable), INITS);
    }

    public QRegistrationAuthorityAdmin(Path<? extends RegistrationAuthorityAdmin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRegistrationAuthorityAdmin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRegistrationAuthorityAdmin(PathMetadata metadata, PathInits inits) {
        this(RegistrationAuthorityAdmin.class, metadata, inits);
    }

    public QRegistrationAuthorityAdmin(Class<? extends RegistrationAuthorityAdmin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.registrationAuthorityPK = inits.isInitialized("registrationAuthorityPK") ? new QRegistrationAuthorityPK(forProperty("registrationAuthorityPK"), inits.get("registrationAuthorityPK")) : null;
    }

}

