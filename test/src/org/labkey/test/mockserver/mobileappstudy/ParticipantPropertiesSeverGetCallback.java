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
    public static final int PUT_SUCCESS_RESPONSE_CODE = 202;
    public static final int UNAUTHORIZED = 401;
    public static final int FAILURE = 500;

    private static final String PARTICIPANT_PROPERTIES_API_PATH = "/ParticipantProperties"; //TODO: confirm with BTC
    private static final String STUDY_ID_PARAMETER_NAME = "studyId";

    private static final String ALL_FIELDS_FILENAME = "AllFieldTypes.json";
    private static final String UPDATE_PROPERTY_FILENAME = "UpdateParticipantProperty.json";
    private static final String ORIGINAL_PROPERTY_FILENAME = "OriginalParticipantProperty.json";
    private static final String ADD_PROPERTY_FILENAME = "AddParticipantProperty.json";
    private static final String DELETE_PROPERTY_FILENAME = "DeleteParticipantProperty.json";

    @Override
    public HttpResponse handle(HttpRequest httpRequest)
    {
        if (httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.STUDY_NAME01) || httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.STUDY_NAME02)
        )
            return participantPropertiesResponse(ALL_FIELDS_FILENAME);
        else if (httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.STUDY_NAME03))
            return participantPropertiesResponse(ORIGINAL_PROPERTY_FILENAME);
        else if (httpRequest.getPath().getValue().contains(ParticipantPropertiesTest.UPDATE_PATH) &&
                httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.UPDATE_PATH))
            return participantPropertiesResponse(UPDATE_PROPERTY_FILENAME);
        else if (httpRequest.getPath().getValue().contains(ParticipantPropertiesTest.ADD_PATH) &&
                httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.ADD_PATH))
            return participantPropertiesResponse(ADD_PROPERTY_FILENAME);
        else if (httpRequest.getPath().getValue().contains(ParticipantPropertiesTest.DELETE_PATH) &&
                httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.DELETE_PATH))
            return participantPropertiesResponse(DELETE_PROPERTY_FILENAME);
        else if (httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.ADD_PATH)
                || httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.UPDATE_PATH)
                || httpRequest.getFirstQueryStringParameter(STUDY_ID_PARAMETER_NAME).equalsIgnoreCase(ParticipantPropertiesTest.DELETE_PATH)
        )
            return participantPropertiesResponse(ORIGINAL_PROPERTY_FILENAME);
        else
        {
            TestLogger.log("Response not available from Mockserver");
            return notFoundResponse();
        }
    }

    private HttpResponse participantPropertiesResponse(String filename)
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

        return response(sb.toString()).withStatusCode(SUCCESS);
    }

}