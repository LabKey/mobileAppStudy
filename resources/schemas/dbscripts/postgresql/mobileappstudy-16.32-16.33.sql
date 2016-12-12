CREATE TABLE mobileappstudy.ResponseMetadata
(
  RowId SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  ListName VARCHAR(64) NOT NULL,
  SurveyId INTEGER NOT NULL,
  QuestionId VARCHAR(64),
  StartTime TIMESTAMP,
  EndTime TIMESTAMP,
  Skipped BOOLEAN,
  Created TIMESTAMP NOT NULL,
  CONSTRAINT PK_ResponseMetadata PRIMARY KEY (RowId)
);