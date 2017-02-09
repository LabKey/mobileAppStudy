package org.labkey.mobileappstudy.providers;

import org.labkey.mobileappstudy.data.SurveyInfo;

import java.util.List;

/**
 * Created by iansigmon on 2/2/17.
 */
public class SurveyDesign
{
    private SurveyInfo _surveyInfo;
    private List<SurveyStep> steps;

    public List<SurveyStep> getSteps()
    {
        return steps;
    }
    public void setSteps(List<SurveyStep> steps)
    {
        this.steps = steps;
    }

    public SurveyInfo getSurveyInfo()
    {
        return _surveyInfo;
    }
    public void setSurveyInfo(SurveyInfo surveyInfo)
    {
        _surveyInfo = surveyInfo;
    }

    public String getSurveyName()
    {
        return _surveyInfo.getSurveyId();
    }

}
