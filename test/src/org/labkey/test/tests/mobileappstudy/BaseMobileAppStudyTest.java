/*
 * Copyright (c) 2016-2018 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.tests.mobileappstudy;

import org.junit.BeforeClass;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.commands.mobileappstudy.EnrollParticipantCommand;
import org.labkey.test.commands.mobileappstudy.SubmitResponseCommand;
import org.labkey.test.data.mobileappstudy.InitialSurvey;
import org.labkey.test.data.mobileappstudy.QuestionResponse;
import org.labkey.test.data.mobileappstudy.Survey;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.SimpleHttpResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by iansigmon on 12/9/16.
 */
public abstract class BaseMobileAppStudyTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    protected static final String MOBILEAPP_SCHEMA = "mobileappstudy";
    protected static final String LIST_SCHEMA = "lists";

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
        EnrollParticipantCommand cmd = new EnrollParticipantCommand(project, studyShortName, batchToken, this::log);

        cmd.execute(200);
        String appToken = cmd.getAppToken();
        assertNotNull("AppToken was null", appToken);
        log("AppToken received: " + appToken);

        return appToken;
    }

    protected boolean mobileAppTableExists(String table, String schema)
    {
        Connection cn = createDefaultConnection(true);
        SelectRowsCommand selectCmd = new SelectRowsCommand(schema, table);
        selectCmd.setColumns(Arrays.asList("*"));

        try
        {
            selectCmd.execute(cn, getCurrentContainerPath());
        }
        catch (CommandException | IOException e)
        {
            return false;
        }

        return true;
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

    protected void assignTokens(List<String> tokensToAssign, String projectName, String studyName)
    {
        Connection connection = createDefaultConnection(false);
        for(String token : tokensToAssign)
        {
            Command command = new Command("mobileappstudy", "enroll");
            HashMap<String, Object> params = new HashMap<>(Maps.of("shortName", studyName, "token", token));
            command.setParameters(params);
            log("Assigning token: " + token);
            try
            {
                CommandResponse response = command.execute(connection, projectName);
                assertEquals(true, response.getProperty("success"));
                log("Token assigned.");
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException("Failed to assign token");
            }
        }
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        BaseMobileAppStudyTest initTest = (BaseMobileAppStudyTest) getCurrentTest();
        initTest.setupProjects();
    }

    void setupProjects()
    {
        //Do nothing as default, Tests can override if needed
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        for (String project : _containerHelper.getCreatedProjects())
        {
            _containerHelper.deleteProject(project, false);
        }
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
        ModulePropertyValue val = new ModulePropertyValue("MobileAppStudy", "/", "SurveyMetadataDirectory", TestFileUtils.getLabKeyRoot() + "/server/optionalModules/mobileAppStudy/test/sampledata/SurveyMetadata/");
        setModuleProperties(Arrays.asList(val));
    }
}
