package org.labkey.mobileappstudy.data;

/**
 * Created by iansigmon on 11/3/16.
 */
public class SurveyMetadata
{
    private String _studyId;
    private String _activityId;
    private String _version;

    public String getVersion()
    {
        return _version;
    }
    public void setVersion(String version)
    {
        _version = version;
    }

    public String getActivityId()
    {
        return _activityId;
    }
    public void setActivityId(String activityId)
    {
        _activityId = activityId;
    }

    public String getStudyId()
    {
        return _studyId;
    }
    public void setStudyId(String studyId)
    {
        _studyId = studyId;
    }
}
