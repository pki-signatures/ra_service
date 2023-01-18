package com.ivan.ra.service.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRegistrationAuthorityRequestPK is a Querydsl query type for RegistrationAuthorityRequestPK
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QRegistrationAuthorityRequestPK extends BeanPath<RegistrationAuthorityRequestPK> {

    private static final long serialVersionUID = -1938799530L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRegistrationAuthorityRequestPK registrationAuthorityRequestPK = new QRegistrationAuthorityRequestPK("registrationAuthorityRequestPK");

    public final StringPath id = createString("id");

    public final QRegistrationAuthority registrationAuthority;

    public QRegistrationAuthorityRequestPK(String variable) {
        this(RegistrationAuthorityRequestPK.class, forVariable(variable), INITS);
    }

    public QRegistrationAuthorityRequestPK(Path<? extends RegistrationAuthorityRequestPK> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRegistrationAuthorityRequestPK(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRegistrationAuthorityRequestPK(PathMetadata metadata, PathInits inits) {
        this(RegistrationAuthorityRequestPK.class, metadata, inits);
    }

    public QRegistrationAuthorityRequestPK(Class<? extends RegistrationAuthorityRequestPK> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.registrationAuthority = inits.isInitialized("registrationAuthority") ? new QRegistrationAuthority(forProperty("registrationAuthority")) : null;
    }

}

