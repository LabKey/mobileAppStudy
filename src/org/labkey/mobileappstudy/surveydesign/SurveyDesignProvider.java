package org.labkey.mobileappstudy.surveydesign;

import org.labkey.api.data.Container;

/**
 * Created by iansigmon on 2/2/17.
 */
public interface SurveyDesignProvider
{
    SurveyDesign getSurveyDesign(Container c, String shortName, String activityId, String version) throws InvalidDesignException, Exception;
}
