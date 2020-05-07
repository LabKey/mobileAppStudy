-- Add new column and populate existing rows with INVALID
ALTER TABLE mobileappstudy.Participant ADD COLUMN AllowDataSharing VARCHAR(20) NOT NULL DEFAULT 'INVALID';
-- No longer need that default constraint; insert must supply this value
ALTER TABLE mobileappstudy.Participant ALTER COLUMN AllowDataSharing DROP DEFAULT;
