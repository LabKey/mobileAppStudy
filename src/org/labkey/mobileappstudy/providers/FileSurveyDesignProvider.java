package org.labkey.mobileappstudy.providers;

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
    public FileSurveyDesignProvider(Logger logger)
    {
        super(logger);
    }

    @Override
    public SurveyDesign getSurveyDesign(Container c, String studyId, String surveyId, String version) throws InvalidDesignException
    {
        try
        {
            //TODO: make this more flexible
            StringBuilder sb = new StringBuilder();
            Path filePath = Paths.get(getBasePath(c), String.join("_", studyId, surveyId, version) + ".txt");
            Files.readAllLines(filePath).forEach(sb::append);

            return getSurveyDesign(sb.toString());

//            String contents =
//                    "{\n" +
//                    "  \"surveyInfo\":{\n" +
//                    "    \"surveyId\": \"InitialSurvey\"\n" +
//                    "    , \"version\": \"1\"\n" +
//                    "  }\n" +
//                    "  , \"steps\":[{\n" +
//                    "    \"type\": \"question-scale\"\n" +
//                    "    , \"resultType\": \"integer\"\n" +
//                    "    , \"key\": \"Scalar\"\n" +
//                    "  }, {\n" +
//                    "    \"type\": \"question-text\"\n" +
//                    "    , \"resultType\": \"text\"\n " +
//                    "    , \"key\": \"TextField\"\n " +
//                    "    , \"format\": {\n" +
//                    "       \"maxLength\": 0\n" +
//                    "    }\n " +
//                    "  }]\n" +
//                    "}";
//            return getSurveyDesign(contents);
        }
        catch (IOException x)
        {
            throw new InvalidDesignException("Unable to read from SurveyDesign file", x);
        }
    }

    private String getBasePath(Container c)
    {
        Module module = ModuleLoader.getInstance().getModule(MobileAppStudyModule.NAME);
        return module.getModuleProperties().get(MobileAppStudyModule.DROP_DIR_PROP_NAME).getEffectiveValue(c);
    }
}
