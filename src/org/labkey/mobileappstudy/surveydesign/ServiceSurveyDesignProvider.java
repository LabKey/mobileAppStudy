package org.labkey.mobileappstudy.surveydesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.mobileappstudy.MobileAppStudyModule;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by susanh on 3/10/17.
 */
public class ServiceSurveyDesignProvider extends AbstractSurveyDesignProviderImpl
{
    private static final String STUDY_ID_PARAM = "studyId";
    private static final String ACTIVITY_ID_PARAM = "activityId";
    private static final String VERSION_PARAM = "activityVersion";

    public ServiceSurveyDesignProvider(Container container, Logger logger)
    {
        super(container, logger);
    }

    @Override
    public SurveyDesign getSurveyDesign(Container c, String shortName, String activityId, String version) throws Exception
    {

        try
        {
            Map<String, String> parameters = new HashMap<>();
            parameters.put(STUDY_ID_PARAM, shortName);
            parameters.put(ACTIVITY_ID_PARAM, activityId);
            parameters.put(VERSION_PARAM, version);
            URL url = new URL(getServiceUrl(c) + "?" + PageFlowUtil.toQueryString(parameters.entrySet()));
            try
            {
                HttpRequest request = new BasicHttpRequest("GET", url.toString());
                request.setHeader("Authorization", "Basic bundleid:" + getServiceToken(c));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int status = connection.getResponseCode();

                if (status == HttpURLConnection.HTTP_OK)
                {
                    return getSurveyDesign(connection.getInputStream());
                }
                else
                {
                    throw new Exception(String.format("Received response status %d from %s",  status, url.toString()));
                }
            }
            catch (IOException e)
            {
                throw new Exception("IOException connecting to url: " + url, e);
            }
        }
        catch (MalformedURLException e)
        {
            throw new Exception(String.format("Malformed URL for shortName %s activityId %s and version %s", shortName, activityId, version));
        }
    }

    private static String getServiceToken(Container container)
    {
        Module module = ModuleLoader.getInstance().getModule(MobileAppStudyModule.NAME);
        return module.getModuleProperties().get(MobileAppStudyModule.METADATA_SERVICE_ACCESS_TOKEN).getEffectiveValue(container);
    }

    private static String getServiceUrl(Container container)
    {
        Module module = ModuleLoader.getInstance().getModule(MobileAppStudyModule.NAME);
        return module.getModuleProperties().get(MobileAppStudyModule.METADATA_SERVICE_BASE_URL).getEffectiveValue(container);
    }

    public static Boolean isConfigured(Container c)
    {
        return !StringUtils.isEmpty(getServiceToken(c)) && !StringUtils.isEmpty(getServiceUrl(c));
    }
}
