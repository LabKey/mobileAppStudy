ALTER TABLE mobileappstudy.response
  RENAME COLUMN response TO data;

ALTER TABLE mobileappstudy.response
  RENAME COLUMN surveyid TO activityid;

ALTER TABLE mobileappstudy.responsemetadata
  RENAME COLUMN surveyid TO activityid;
