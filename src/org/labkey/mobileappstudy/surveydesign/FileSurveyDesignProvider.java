package org.labkey.mobileappstudy.surveydesign;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.mobileappstudy.MobileAppStudyModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Get MobileAppStudy SurveySchema from a resource file
 */
public class FileSurveyDesignProvider extends AbstractSurveyDesignProviderImpl
{
    public FileSurveyDesignProvider(Container container, Logger logger)
    {
        super(container, logger);
    }

    @Override
    public SurveyDesign getSurveyDesign(Container c, String studyId, String activityId, String version) throws InvalidDesignException
    {
        try
        {
            //TODO: make this more flexible
            StringBuilder sb = new StringBuilder();
            Path filePath = Paths.get(getBasePath(c), String.join("_", studyId, activityId, version) + ".json");
            Files.readAllLines(filePath).forEach(sb::append);

            return getSurveyDesign(sb.toString());
        }
        catch (IOException x)
        {
            throw new InvalidDesignException("Unable to read from SurveyDesign file", x);
        }
    }

    public static String getBasePath(Container c)
    {
        Module module = ModuleLoader.getInstance().getModule(MobileAppStudyModule.NAME);
        return module.getModuleProperties().get(MobileAppStudyModule.SURVEY_METADATA_DIRECTORY).getEffectiveValue(c);
    }
}
