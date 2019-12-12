

CREATE TABLE mobileappstudy.ParticipantPropertyMetadata
(
    RowId SERIAL,
    ListId INTEGER NOT NULL,
    PropertyURI VARCHAR NOT NULL,
    PropertyType INTEGER NOT NULL,

    Created TIMESTAMP NOT NULL,
    Modified TIMESTAMP NULL,
    Container ENTITYID NOT NULL,

    CONSTRAINT PK_ParticipantPropertiesMetadata PRIMARY KEY (RowId)
);
