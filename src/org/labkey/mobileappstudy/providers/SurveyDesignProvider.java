package org.labkey.mobileappstudy.providers;

import org.labkey.api.data.Container;

/**
 * Created by iansigmon on 2/2/17.
 */
public interface SurveyDesignProvider
{
    SurveyDesign getSurveyDesign(Container c, String shortName, String surveyId, String version) throws InvalidDesignException;
}
