package com.ivan.ra.service.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRegistrationAuthorityRelyingParty is a Querydsl query type for RegistrationAuthorityRelyingParty
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRegistrationAuthorityRelyingParty extends EntityPathBase<RegistrationAuthorityRelyingParty> {

    private static final long serialVersionUID = 153427992L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRegistrationAuthorityRelyingParty registrationAuthorityRelyingParty = new QRegistrationAuthorityRelyingParty("registrationAuthorityRelyingParty");

    public final StringPath clientAuthCert = createString("clientAuthCert");

    public final StringPath clientAuthCertHash = createString("clientAuthCertHash");

    public final DateTimePath<java.time.LocalDateTime> createDateTime = createDateTime("createDateTime", java.time.LocalDateTime.class);

    public final StringPath primaryContactEmailAddress = createString("primaryContactEmailAddress");

    public final StringPath primaryContactName = createString("primaryContactName");

    public final StringPath primaryContactNo = createString("primaryContactNo");

    public final QRegistrationAuthorityPK registrationAuthorityPK;

    public final StringPath secondaryContactEmailAddress = createString("secondaryContactEmailAddress");

    public final StringPath secondaryContactName = createString("secondaryContactName");

    public final StringPath secondaryContactNo = createString("secondaryContactNo");

    public final StringPath status = createString("status");

    public final DateTimePath<java.time.LocalDateTime> updateDateTime = createDateTime("updateDateTime", java.time.LocalDateTime.class);

    public QRegistrationAuthorityRelyingParty(String variable) {
        this(RegistrationAuthorityRelyingParty.class, forVariable(variable), INITS);
    }

    public QRegistrationAuthorityRelyingParty(Path<? extends RegistrationAuthorityRelyingParty> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRegistrationAuthorityRelyingParty(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRegistrationAuthorityRelyingParty(PathMetadata metadata, PathInits inits) {
        this(RegistrationAuthorityRelyingParty.class, metadata, inits);
    }

    public QRegistrationAuthorityRelyingParty(Class<? extends RegistrationAuthorityRelyingParty> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.registrationAuthorityPK = inits.isInitialized("registrationAuthorityPK") ? new QRegistrationAuthorityPK(forProperty("registrationAuthorityPK"), inits.get("registrationAuthorityPK")) : null;
    }

}

