/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests.mobileappstudy;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GuestCredentialsProvider;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.commands.mobileappstudy.EnrollParticipantCommand;
import org.labkey.test.commands.mobileappstudy.SubmitResponseCommand;
import org.labkey.test.data.mobileappstudy.InitialSurvey;
import org.labkey.test.data.mobileappstudy.QuestionResponse;
import org.labkey.test.data.mobileappstudy.Survey;
import org.labkey.test.pages.mobileappstudy.SetupPage;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PostgresOnlyTest;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpClassCallback;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockserver.model.HttpRequest.request;

/**
 * Created by iansigmon on 12/9/16.
 */
public abstract class BaseMobileAppStudyTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    protected static final String MOBILEAPP_SCHEMA = "mobileappstudy";
    protected static final String LIST_SCHEMA = "lists";
    protected final static String BASE_RESULTS = "{\n" +
            "\t\t\"start\": \"2016-09-06T15:48:13.000+0000\",\n" +
            "\t\t\"end\": \"2016-09-06T15:48:45.000+0000\",\n" +
            "\t\t\"results\": []\n" +
            "}";

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("MobileAppStudy");
    }

    /**
     * Get apptoken associated to a participant and study via the API
     * @param project study container folder
     * @param studyShortName study parameter
     * @param batchToken get
     * @return the appToken string
     */
    String getNewAppToken(String project, String studyShortName, String batchToken)
    {
        log("Requesting app token for project [" + project +"] and study [" + studyShortName + "]");
        EnrollParticipantCommand cmd = new EnrollParticipantCommand(project, studyShortName, batchToken, "true", this::log);

        cmd.execute(200);
        String appToken = cmd.getAppToken();
        assertNotNull("AppToken was null", appToken);
        log("AppToken received: " + appToken);

        return appToken;
    }

    protected boolean mobileAppTableExists(String table, String schema) throws CommandException, IOException
    {
        Connection cn = createDefaultConnection(true);
        SelectRowsCommand selectCmd = new SelectRowsCommand(schema, table);
        selectCmd.setColumns(Arrays.asList("*"));

        try
        {
            selectCmd.execute(cn, getCurrentContainerPath());
            return true;
        }
        catch (CommandException e)
        {
            if (e.getStatusCode() == 404)
                return false;
            else
                throw e;
        }
    }

    protected SelectRowsResponse getMobileAppData(String table, String schema)
    {
        Connection cn = createDefaultConnection(true);
        SelectRowsCommand selectCmd = new SelectRowsCommand(schema, table);
        selectCmd.setColumns(Arrays.asList("*"));

        SelectRowsResponse selectResp;
        try
        {
            selectResp = selectCmd.execute(cn, getCurrentContainerPath());
        }
        catch (CommandException | IOException e)
        {
            log(e.getMessage());
            throw new RuntimeException(e);
        }

        return selectResp;
    }

    protected SelectRowsResponse getMobileAppDataWithRetry(String table, String schema)
    {
        int waitTime = 1000;
        while (waitTime < 45000)
        {
            try
            {
                return getMobileAppData(table, schema);
            }
            catch (RuntimeException e)
            {
                if (waitTime > 30000)
                    throw e;

                log("Waiting " + waitTime + " before retrying");
                sleep(waitTime);
                waitTime *= 2;
            }
        }

        return null;
    }

    @LogMethod
    protected void assignTokens(List<String> tokensToAssign, String projectName, String studyName)
    {
        Connection connection = createGuestConnection();  // No credentials, just the token -- mimic the mobile app
        for(String token : tokensToAssign)
        {
            try
            {
                CommandResponse response = assignToken(connection, token, projectName, studyName);
                assertEquals(true, response.getProperty("success"));
                log("Token assigned.");
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException("Failed to assign token", e);
            }
        }
    }

    @LogMethod
    protected CommandResponse assignToken(Connection connection, @LoggedParam String token, @LoggedParam String projectName, @LoggedParam String studyName) throws IOException, CommandException
    {
        Command command = new PostCommand("mobileappstudy", "enroll");
        HashMap<String, Object> params = new HashMap<>(Maps.of("shortName", studyName, "token", token, "allowDataSharing", "true"));
        command.setParameters(params);
        log("Assigning token: " + token);
        return command.execute(connection, projectName);
    }

    @BeforeClass
    public static void doSetup()
    {
        BaseMobileAppStudyTest initTest = (BaseMobileAppStudyTest) getCurrentTest();
        initTest.setupProjects();
    }

    void setupProjects()
    {
        //Do nothing as default, Tests can override if needed
    }

    /**
     * Wrap question response and submit to server via the API
     *
     * @param qr to send to server
     * @param appToken to use in request
     * @return error message of request if there is one.
     */
    protected String submitQuestion(QuestionResponse qr, String appToken, String surveyName, String surveyVersion, int expectedStatusCode)
    {
        Survey survey = new InitialSurvey(appToken, surveyName, surveyVersion, new Date(), new Date());
        survey.addResponse(qr);

        return submitSurvey(survey, expectedStatusCode);
    }

    /**
     * Submit the survey to server via API
     *
     * @param survey to submit
     * @param expectedStatusCode status code to expect from server
     * @return error message from response (if it exists)
     */
    protected String submitSurvey(Survey survey, int expectedStatusCode)
    {
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, survey);
        cmd.execute(expectedStatusCode);

        return cmd.getExceptionMessage();
    }

    protected void setSurveyMetadataDropDir()
    {
        ModulePropertyValue val = new ModulePropertyValue("MobileAppStudy", "/", "SurveyMetadataDirectory", TestFileUtils.getSampleData("SurveyMetadata").getAbsolutePath());
        setModuleProperties(Arrays.asList(val));
    }

    protected void setupProject(String studyName, String projectName, String surveyName, boolean enableResponseCollection)
    {
        _containerHelper.createProject(projectName, "Mobile App Study");
        log("Set a study name.");
        goToProjectHome(projectName);
        SetupPage setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().setShortName(studyName);
        if (enableResponseCollection)
            setupPage.getStudySetupWebPart().checkResponseCollection();
        setupPage.validateSubmitButtonEnabled();
        setupPage.getStudySetupWebPart().clickSubmit();
        if (StringUtils.isNotBlank(surveyName))
            _listHelper.createList(projectName, surveyName, ListHelper.ListColumnType.AutoInteger, "Key");
    }

    /**
     * Adds a Request matcher to the mockserver
     * @param mockServer to add matcher to
     * @param requestPath to add matcher for
     * @param log logging method
     * @param method HTTP request type, e.g., GET, POST, etc.
     * @param matcher Fully qualified class name String to request handler that implements ExpectationResponseCallback
     */
    protected static void addRequestMatcher(ClientAndServer mockServer, String requestPath, Consumer<String> log, String method, String matcher )
    {
        log.accept(String.format("Adding a response for %1$s requests.", requestPath));
        mockServer.when(
                request()
                        .withMethod(method)
                        .withPath("/" + requestPath)
        ).respond(HttpClassCallback.callback(matcher));
    }

    protected CommandResponse callSelectRows(Map<String, Object> params) throws IOException, CommandException
    {
        return callCommand("selectRows", params);
    }

    protected CommandResponse callExecuteSql(Map<String, Object> params) throws IOException, CommandException
    {
        return callCommand("executeSql", params);
    }

    protected CommandResponse callCommand(String action, Map<String, Object> params)  throws IOException, CommandException
    {
        Command selectCmd = new Command("mobileAppStudy", action);
        selectCmd.setParameters(params);

        return selectCmd.execute(createGuestConnection(), getProjectName());
    }

    /**
     * Returns a remoteapi Connection that uses no credentials and no cookies -- to mimic the mobile app that uses
     * participantId or enrollment token to authenticate
     */
    protected Connection createGuestConnection()
    {
        return new Connection(WebTestHelper.getBaseURL(), new GuestCredentialsProvider());
    }

    protected void checkJsonObjectAgainstExpectedValues(Map<String, Object> expectedValues, JSONObject jsonObject)
    {
        Set<String> columns = expectedValues.keySet();

        for(String column : columns)
        {
            Assert.assertTrue("Expected column " + column + " was not in the jsonObject.", jsonObject.containsKey(column));

            Object  jsonObjectValue;
            if(jsonObject.get(column).getClass().getSimpleName().equals("JSONObject"))
            {
                // Need to do this if the object that is being compared came from an executeSql call.
                JSONObject jObject = (JSONObject)jsonObject.get(column);
                jsonObjectValue = jObject.get("value");
            }
            else
                jsonObjectValue = jsonObject.get(column);

            log("Validating column '" + column + "' which is a '" + jsonObjectValue.getClass().getName() + "' data type.");
//            log("Type of value returned by json: " + jsonObjectValue.getClass().getName());
//            log("Type of value expected: " + expectedValues.get(column).getClass().getName());

            switch(expectedValues.get(column).getClass().getSimpleName())
            {
                case "Integer":

                    // There is this odd case where the field is an integer but the json returns a long.
                    // Not worth worrying about, but will need to account for.
                    Assert.assertEquals(column + " not as expected.", expectedValues.get(column), ((Number)jsonObjectValue).intValue());
                    break;
                case "Double":
                    Assert.assertEquals(column + " not as expected.", Double.parseDouble(expectedValues.get(column).toString()), (double)jsonObjectValue, 0.0);
                    break;
                case "Number":
                    Assert.assertEquals(column + " not as expected.", expectedValues.get(column), jsonObjectValue);
                case "Boolean":
                    if ((boolean)expectedValues.get(column))
                        Assert.assertTrue(column + " was not true (as expected).",(boolean)jsonObjectValue);
                    else
                        Assert.assertFalse(column + " was not false (as expected).",(boolean)jsonObjectValue);
                    break;
                default:
                    // Long and String are the only types that don't need some kind of special casting.
                    Assert.assertEquals(column + " not as expected.", expectedValues.get(column), jsonObjectValue);
                    break;
            }
        }

        // If we've gotten to this point then we know that all of the expected columns and values were there.
        // Now we need to check that the jsonObject did not return any unexpected columns.
        StringBuilder unexpectedJsonColumn = new StringBuilder();
        boolean pass = true;
        for(Object jsonColumn : jsonObject.keySet())
        {
            String column = (String)jsonColumn;
            // If the query returned all columns there are a few columns to ignore.
            // Ignore the 'Created', 'Key', 'EntityId', 'lastIndexed' and 'Modified' fields. These fields can be tricky to get an accurate expected value especially the timestamp fields.
            if ((!expectedValues.containsKey(column)) &&
                    (!column.equals("Key") &&
                            !column.equals("Created") &&
                            !column.equals("Modified") &&
                            !column.equals("lastIndexed") &&
                            !column.equals("EntityId")))
            {
                unexpectedJsonColumn.append("Found unexpected column '").append(column).append("' in jsonObject.\r\n");
                pass = false;
            }
        }

        Assert.assertTrue(unexpectedJsonColumn.toString(), pass);
    }

    protected String getResponseFromFile(String dir, String filename)
    {
        return TestFileUtils.getFileContents(TestFileUtils.getSampleData(String.join("/", dir, filename)));
    }
}
