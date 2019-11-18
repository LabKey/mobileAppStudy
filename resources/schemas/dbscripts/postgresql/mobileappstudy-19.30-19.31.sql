

CREATE TABLE mobileappstudy.ParticipantPropertiesMetadata
(
    RowId SERIAL,
    PropertyURI VARCHAR NOT NULL,
    PropertyType INTEGER NOT NULL,

    Created TIMESTAMP NOT NULL,
    Modified TIMESTAMP NULL,
    Container ENTITYID NOT NULL,

    CONSTRAINT PK_ParticipantPropertiesMetadata PRIMARY KEY (RowId)
);
