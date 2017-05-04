package org.labkey.mobileappstudy.data;

import org.labkey.mobileappstudy.surveydesign.SurveyDesign;

/**
 * Created by susanh on 5/3/17.
 */
public class ActivityMetadataResponse
{
    private String message;
    private SurveyDesign activity;

    public SurveyDesign getActivity()
    {
        return activity;
    }

    public void setActivity(SurveyDesign activity)
    {
        this.activity = activity;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
