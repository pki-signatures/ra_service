package com.ivan.ra.service.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRegistrationAuthorityRequest is a Querydsl query type for RegistrationAuthorityRequest
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRegistrationAuthorityRequest extends EntityPathBase<RegistrationAuthorityRequest> {

    private static final long serialVersionUID = 543233307L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRegistrationAuthorityRequest registrationAuthorityRequest = new QRegistrationAuthorityRequest("registrationAuthorityRequest");

    public final StringPath certificate = createString("certificate");

    public final DateTimePath<java.time.LocalDateTime> createDateTime = createDateTime("createDateTime", java.time.LocalDateTime.class);

    public final StringPath emailAddress = createString("emailAddress");

    public final StringPath mobileNo = createString("mobileNo");

    public final StringPath raProfile = createString("raProfile");

    public final QRegistrationAuthorityRequestPK registrationAuthorityRequestPK;

    public final StringPath rejectionReason = createString("rejectionReason");

    public final QRequestSubjectInfo requestSubjectInfo;

    public final StringPath ssaId = createString("ssaId");

    public final StringPath status = createString("status");

    public final DateTimePath<java.time.LocalDateTime> updateDateTime = createDateTime("updateDateTime", java.time.LocalDateTime.class);

    public QRegistrationAuthorityRequest(String variable) {
        this(RegistrationAuthorityRequest.class, forVariable(variable), INITS);
    }

    public QRegistrationAuthorityRequest(Path<? extends RegistrationAuthorityRequest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRegistrationAuthorityRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRegistrationAuthorityRequest(PathMetadata metadata, PathInits inits) {
        this(RegistrationAuthorityRequest.class, metadata, inits);
    }

    public QRegistrationAuthorityRequest(Class<? extends RegistrationAuthorityRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.registrationAuthorityRequestPK = inits.isInitialized("registrationAuthorityRequestPK") ? new QRegistrationAuthorityRequestPK(forProperty("registrationAuthorityRequestPK"), inits.get("registrationAuthorityRequestPK")) : null;
        this.requestSubjectInfo = inits.isInitialized("requestSubjectInfo") ? new QRequestSubjectInfo(forProperty("requestSubjectInfo")) : null;
    }

}

