package org.labkey.test.mockserver.mobileappstudy;

import org.labkey.test.TestFileUtils;
import org.labkey.test.tests.mobileappstudy.ParticipantPropertiesTest;
import org.labkey.test.util.TestLogger;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;

public class ParticipantPropertiesSeverGetCallback implements ExpectationResponseCallback
{
    public static final int SUCCESS = 200;
    public static final int FAILURE = 500;

    private static final String STUDY_ID_PARAMETER_NAME = "studyId";

    private static final String ALL_FIELDS_FILENAME = "AllFieldTypes.json";
    private static final String UPDATE_PROPERTY_FILENAME = "UpdateParticipantProperty.json";
    private static final String ORIGINAL_PROPERTY_FILENAME = "OriginalParticipantProperty.json";
    private static final String ADD_PROPERTY_FILENAME = "AddParticipantProperty.json";
    private static final String DELETE_PROPERTY_FILENAME = "DeleteParticipantProperty.json";
    private static final String SURVEY_RESPONSE_FILENAME = "Survey_Metadata.json";

    @Override
    public HttpResponse handle(HttpRequest httpRequest)
    {
        String studyId = httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME);

        if (ParticipantPropertiesTest.STUDY_NAME01.equalsIgnoreCase(studyId) || ParticipantPropertiesTest.STUDY_NAME02.equalsIgnoreCase(studyId))
        {
            return readFileResponse(ALL_FIELDS_FILENAME, studyId);
        }
        else if (ParticipantPropertiesTest.STUDY_NAME03.equalsIgnoreCase(studyId))
        {
            return readFileResponse(ORIGINAL_PROPERTY_FILENAME, studyId);
        }
        else if (ParticipantPropertiesTest.UPDATE_PATH.equalsIgnoreCase(studyId))
        {
            return getAlterationResponse(httpRequest, ParticipantPropertiesTest.UPDATE_PATH, studyId, UPDATE_PROPERTY_FILENAME);
        }
        else if (ParticipantPropertiesTest.ADD_PATH.equalsIgnoreCase(studyId))
        {
            return getAlterationResponse(httpRequest, ParticipantPropertiesTest.ADD_PATH, studyId, ADD_PROPERTY_FILENAME);
        }
        else if (ParticipantPropertiesTest.DELETE_PATH.equalsIgnoreCase(studyId))
        {
            return getAlterationResponse(httpRequest, ParticipantPropertiesTest.DELETE_PATH, studyId, DELETE_PROPERTY_FILENAME);
        }
        else if (ParticipantPropertiesTest.SURVEY_UPDATE_PATH.equalsIgnoreCase(studyId))
        {
            return getSurveyUpdateResponse(httpRequest, studyId);
        }
        TestLogger.log("Response not available from Mockserver");
        return notFoundResponse();
    }

    private HttpResponse getSurveyUpdateResponse(HttpRequest httpRequest, String studyId)
    {
        if (httpRequest.getPath().getValue().endsWith(ParticipantPropertiesTest.WCP_SURVEY_METHOD))
            return readFileResponse(SURVEY_RESPONSE_FILENAME, studyId);
        else if (httpRequest.getPath().getValue().contains(ParticipantPropertiesTest.SURVEY_UPDATE_PATH))
            return readFileResponse(ADD_PROPERTY_FILENAME, studyId);
        else if (httpRequest.getPath().getValue().endsWith(ParticipantPropertiesTest.WCP_API_METHOD))
            return readFileResponse(ORIGINAL_PROPERTY_FILENAME, studyId);
        else
        {
            TestLogger.log("Response not available from Mockserver");
            return notFoundResponse();
        }
    }

    private HttpResponse getAlterationResponse(HttpRequest httpRequest, String expectedPath, String studyId, String filename)
    {
        if (httpRequest.getPath().getValue().contains(expectedPath))
            return readFileResponse(filename, studyId);
        else if (httpRequest.getPath().getValue().endsWith(ParticipantPropertiesTest.WCP_API_METHOD))
            return readFileResponse(ORIGINAL_PROPERTY_FILENAME, studyId);
        else
        {
            TestLogger.log("Response not available from Mockserver");
            return notFoundResponse();
        }
    }

    private HttpResponse readFileResponse(String filename, String studyId)
    {
        StringBuilder sb = new StringBuilder();
        Path filePath = TestFileUtils.getSampleData("/ParticipantPropertiesMetadata/" + filename).toPath();
        try
        {
            Files.readAllLines(filePath).forEach(sb::append);
        }
        catch (IOException e)
        {
            return response("Can't find response file").withStatusCode(FAILURE);
        }

        return response(String.format(sb.toString(), studyId)).withStatusCode(SUCCESS);
    }
}