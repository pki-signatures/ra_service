package com.ivan.ra.service.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRequestSubjectInfo is a Querydsl query type for RequestSubjectInfo
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRequestSubjectInfo extends EntityPathBase<RequestSubjectInfo> {

    private static final long serialVersionUID = -1584094751L;

    public static final QRequestSubjectInfo requestSubjectInfo = new QRequestSubjectInfo("requestSubjectInfo");

    public final StringPath commonName = createString("commonName");

    public final StringPath country = createString("country");

    public final DateTimePath<java.time.LocalDateTime> createDateTime = createDateTime("createDateTime", java.time.LocalDateTime.class);

    public final StringPath givenName = createString("givenName");

    public final StringPath id = createString("id");

    public final StringPath organization = createString("organization");

    public final StringPath organizationIdentifier = createString("organizationIdentifier");

    public final StringPath organizationUnit = createString("organizationUnit");

    public final StringPath serialNumber = createString("serialNumber");

    public final StringPath surname = createString("surname");

    public final DateTimePath<java.time.LocalDateTime> updateDateTime = createDateTime("updateDateTime", java.time.LocalDateTime.class);

    public QRequestSubjectInfo(String variable) {
        super(RequestSubjectInfo.class, forVariable(variable));
    }

    public QRequestSubjectInfo(Path<? extends RequestSubjectInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRequestSubjectInfo(PathMetadata metadata) {
        super(RequestSubjectInfo.class, metadata);
    }

}

