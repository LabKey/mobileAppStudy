/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- Create schema, tables, indexes, and constraints used for MobileAppSurvey module here
-- All SQL VIEW definitions should be created in mobileappsurvey-create.sql and dropped in mobileappsurvey-drop.sql
CREATE SCHEMA mobileappsurvey;
GO

CREATE TABLE mobileappsurvey.Study
(
  RowId INT IDENTITY(1,1),
  StudyId VARCHAR(32),

  Created DATETIME NOT NULL,
  CreatedBy USERID NOT NULL,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_Study PRIMARY KEY (RowId)
);

CREATE TABLE mobileappsurvey.Participant
(
  RowId INT IDENTITY(1,1),
  AppToken ENTITYID,
  StudyId INT NOT NULL,

  Created DATETIME NOT NULL,
  CreatedBy USERID NOT NULL,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_Participant PRIMARY KEY (RowId),
  CONSTRAINT FK_Participant_Study FOREIGN KEY (StudyId) REFERENCES mobileappsurvey.Study (RowId)
);

CREATE TABLE mobileappsurvey.EnrollmentTokenBatch
(
  RowId INT IDENTITY(1,1),
  Count INT NOT NULL,

  Created DATETIME NOT NULL,
  CreatedBy USERID NOT NULL,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_EnrollmentTokenBatch PRIMARY KEY (RowId)
);


CREATE TABLE mobileappsurvey.EnrollmentToken
(
  RowId INT IDENTITY(1,1),
  BatchId INT,
  Token VARCHAR(9) NOT NULL,
  ParticipantId INTEGER,

  Created DATETIME NOT NULL,
  CreatedBy USERID NOT NULL,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_EnrollmentToken PRIMARY KEY (RowId),
  CONSTRAINT FK_EnrollmentToken_EnrollmentTokenBatch FOREIGN KEY (BatchId) REFERENCES mobileappsurvey.EnrollmentTokenBatch (RowId),
  CONSTRAINT FK_EnrollmentToken_Participant FOREIGN KEY (ParticipantId) REFERENCES mobileappsurvey.Participant (RowId)

);