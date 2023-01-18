package com.ivan.ra.service.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRegistrationAuthorityOperator is a Querydsl query type for RegistrationAuthorityOperator
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRegistrationAuthorityOperator extends EntityPathBase<RegistrationAuthorityOperator> {

    private static final long serialVersionUID = -446933096L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRegistrationAuthorityOperator registrationAuthorityOperator = new QRegistrationAuthorityOperator("registrationAuthorityOperator");

    public final StringPath clientAuthCert = createString("clientAuthCert");

    public final StringPath clientAuthCertHash = createString("clientAuthCertHash");

    public final DateTimePath<java.time.LocalDateTime> createDateTime = createDateTime("createDateTime", java.time.LocalDateTime.class);

    public final StringPath emailAddress = createString("emailAddress");

    public final QRegistrationAuthorityPK registrationAuthorityPK;

    public final StringPath role = createString("role");

    public final StringPath status = createString("status");

    public final DateTimePath<java.time.LocalDateTime> updateDateTime = createDateTime("updateDateTime", java.time.LocalDateTime.class);

    public QRegistrationAuthorityOperator(String variable) {
        this(RegistrationAuthorityOperator.class, forVariable(variable), INITS);
    }

    public QRegistrationAuthorityOperator(Path<? extends RegistrationAuthorityOperator> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRegistrationAuthorityOperator(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRegistrationAuthorityOperator(PathMetadata metadata, PathInits inits) {
        this(RegistrationAuthorityOperator.class, metadata, inits);
    }

    public QRegistrationAuthorityOperator(Class<? extends RegistrationAuthorityOperator> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.registrationAuthorityPK = inits.isInitialized("registrationAuthorityPK") ? new QRegistrationAuthorityPK(forProperty("registrationAuthorityPK"), inits.get("registrationAuthorityPK")) : null;
    }

}

