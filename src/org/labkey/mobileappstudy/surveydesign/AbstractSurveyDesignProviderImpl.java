package org.labkey.mobileappstudy.surveydesign;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by iansigmon on 2/2/17.
 */
public abstract class AbstractSurveyDesignProviderImpl implements SurveyDesignProvider
{
    protected final Logger logger;

    public AbstractSurveyDesignProviderImpl(Logger logger)
    {
        this.logger = logger;
    }

    protected SurveyDesign getSurveyDesign(String contents) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(contents, SurveyDesign.class);
    }
}
