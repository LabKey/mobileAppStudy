package org.labkey.mobileappstudy.surveydesign;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by iansigmon on 2/2/17.
 */
public abstract class AbstractSurveyDesignProviderImpl implements SurveyDesignProvider
{
    protected Container container;
    protected final Logger logger;

    public AbstractSurveyDesignProviderImpl(Container container, Logger logger)
    {
        this.container = container;
        this.logger = logger;
    }

    protected SurveyDesign getSurveyDesign(InputStream input) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, SurveyDesign.class);
    }

    protected SurveyDesign getSurveyDesign(String contents) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(contents, SurveyDesign.class);
    }
}
