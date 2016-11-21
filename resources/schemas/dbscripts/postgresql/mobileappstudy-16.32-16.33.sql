DROP TABLE mobileappstudy."ResponseMetadata";

CREATE TABLE mobileappstudy."ResponseMetadata"
(
  RowId SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  ListName VARCHAR,
  SurveyId INTEGER NOT NULL,
  QuestionId CHARACTER varying,
  ResponseId INTEGER,
  Start TIMESTAMP,
  End TIMESTAMP,
  Skipped BOOLEAN,
  Created TIMESTAMP,
  CreatedBy USERID NOT NULL,
  Modified TIMESTAMP,
  ModifiedBy USERID,
  CONSTRAINT PK_QuestionMetadata PRIMARY KEY (RowId),
  CONSTRAINT FK_QuestionMetadata_Response FOREIGN KEY (ResponseId)
    REFERENCES mobileappstudy.response (rowid)
)