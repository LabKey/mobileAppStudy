ALTER TABLE mobileappstudy.participant
  ADD COLUMN "status" integer;

UPDATE mobileappstudy.participant set "status" = 0;

ALTER TABLE mobileappstudy.responsemetadata
  ADD COLUMN "participantid" integer;

CREATE INDEX IX_responsemetadata_participantid ON mobileappstudy.responsemetadata ("participantid");