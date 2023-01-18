package com.ivan.ra.service.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRegistrationAuthorityPK is a Querydsl query type for RegistrationAuthorityPK
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QRegistrationAuthorityPK extends BeanPath<RegistrationAuthorityPK> {

    private static final long serialVersionUID = 611156943L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRegistrationAuthorityPK registrationAuthorityPK = new QRegistrationAuthorityPK("registrationAuthorityPK");

    public final StringPath name = createString("name");

    public final QRegistrationAuthority registrationAuthority;

    public QRegistrationAuthorityPK(String variable) {
        this(RegistrationAuthorityPK.class, forVariable(variable), INITS);
    }

    public QRegistrationAuthorityPK(Path<? extends RegistrationAuthorityPK> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRegistrationAuthorityPK(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRegistrationAuthorityPK(PathMetadata metadata, PathInits inits) {
        this(RegistrationAuthorityPK.class, metadata, inits);
    }

    public QRegistrationAuthorityPK(Class<? extends RegistrationAuthorityPK> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.registrationAuthority = inits.isInitialized("registrationAuthority") ? new QRegistrationAuthority(forProperty("registrationAuthority")) : null;
    }

}

