package org.labkey.mobileappstudy.data;

/**
 * Created by iansigmon on 11/3/16.
 */
public class SurveyInfo
{
    private String _studyId;
    private String _surveyId;
    private String _version;

    public String getVersion()
    {
        return _version;
    }
    public void setVersion(String version)
    {
        _version = version;
    }

    public String getSurveyId()
    {
        return _surveyId;
    }
    public void setSurveyId(String surveyId)
    {
        _surveyId = surveyId;
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
