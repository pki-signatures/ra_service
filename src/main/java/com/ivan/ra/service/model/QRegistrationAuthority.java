package com.ivan.ra.service.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRegistrationAuthority is a Querydsl query type for RegistrationAuthority
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRegistrationAuthority extends EntityPathBase<RegistrationAuthority> {

    private static final long serialVersionUID = 1466556116L;

    public static final QRegistrationAuthority registrationAuthority = new QRegistrationAuthority("registrationAuthority");

    public final DateTimePath<java.time.LocalDateTime> createDateTime = createDateTime("createDateTime", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final StringPath organizationAddress = createString("organizationAddress");

    public final StringPath organizationCity = createString("organizationCity");

    public final StringPath organizationCountry = createString("organizationCountry");

    public final StringPath organizationName = createString("organizationName");

    public final StringPath organizationProvince = createString("organizationProvince");

    public final StringPath primaryContactEmailAddress = createString("primaryContactEmailAddress");

    public final StringPath primaryContactName = createString("primaryContactName");

    public final StringPath primaryContactNo = createString("primaryContactNo");

    public final StringPath raProfiles = createString("raProfiles");

    public final StringPath secondaryContactEmailAddress = createString("secondaryContactEmailAddress");

    public final StringPath secondaryContactName = createString("secondaryContactName");

    public final StringPath secondaryContactNo = createString("secondaryContactNo");

    public final StringPath status = createString("status");

    public final DateTimePath<java.time.LocalDateTime> updateDateTime = createDateTime("updateDateTime", java.time.LocalDateTime.class);

    public QRegistrationAuthority(String variable) {
        super(RegistrationAuthority.class, forVariable(variable));
    }

    public QRegistrationAuthority(Path<? extends RegistrationAuthority> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRegistrationAuthority(PathMetadata metadata) {
        super(RegistrationAuthority.class, metadata);
    }

}

