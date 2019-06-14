/*
 * Copyright (c) 2019 LabKey Corporation
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

import com.google.common.net.MediaType;
import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.Test;
import org.labkey.test.commands.mobileappstudy.SubmitResponseCommand;
import org.labkey.test.components.mobileappstudy.TokenBatchPopup;
import org.labkey.test.pages.mobileappstudy.SetupPage;
import org.labkey.test.pages.mobileappstudy.TokenListPage;
import org.labkey.test.util.PipelineStatusTable;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpClassCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.StringBody;
import org.mockserver.verify.VerificationTimes;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;

public class ForwardResponseTest extends BaseMobileAppStudyTest
{
    protected static final int PORT = 8082;
    private static final String FORWARDING_URL = "http://localhost:" + PORT;
    private static final String FORWARDING_USER = "forwarding_test_user";
    private static final String FORWARDING_PASSWORD = "password";
    protected static ClientAndServer mockServer = null;
    private static final String FORWARD_BODY_FORMAT = "{\"token\": \"%1$s\", \"response\": %2$s}";  //Needs to match SurveyResponsePipelineJob.FORWARD_JSON_FORMAT

    //Create study
    public final static String STUDY_NAME01 = "ForwardingSuccess";  // Study names are case insensitive
    public final static String STUDY_NAME02 = "ForwardingFailed";
    public final static String STUDY_NAME03 = "ForwardingIfAtFirst";
    private final static String BASE_PROJECT_NAME = "Response Forwarding Test Project";
    private final static String PROJECT_NAME01 = BASE_PROJECT_NAME + " " + STUDY_NAME01;
    private final static String PROJECT_NAME02 = BASE_PROJECT_NAME + " " + STUDY_NAME02;
    private final static String PROJECT_NAME03 = BASE_PROJECT_NAME + " " + STUDY_NAME03;
    private final static String SURVEY_NAME = "FakeForwardingSurvey";
    private final static String FORWARDING_PIPELINE_JOB_FORMAT = "Survey Response forwarding for %1$s";


    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    private void initMockserver()
    {
        if((null == mockServer) || (!mockServer.isRunning()))
            mockServer = ClientAndServer.startClientAndServer(PORT);

        int count = 1;

        while(!mockServer.isRunning() & count < 10)
        {
            log("Waiting for mockServer to start.");
            count++;
            sleep(1000);
        }

        mockServer.reset();

        if(mockServer.isRunning()) {
            addRequestMatcher(mockServer, STUDY_NAME01, this::log);
            addRequestMatcher(mockServer, STUDY_NAME02, this::log);
            addRequestMatcher(mockServer, STUDY_NAME03, this::log);
        }
        else {
            log("Mockserver is not running, could not add RequestMatcher.");
        }
    }

    private static void addRequestMatcher(ClientAndServer mockServer, String requestPath, Consumer<String> log )
    {
        log.accept(String.format("Adding a response for %1$s requests.", requestPath));
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/" + requestPath)
        ).respond(HttpClassCallback.callback("org.labkey.test.mockserver.mobileappstudy.MockServerPostCallback"));
    }

    @Override
    void setupProjects()
    {
        setupProject(STUDY_NAME01, PROJECT_NAME01, SURVEY_NAME, true);
        setupProject(STUDY_NAME02, PROJECT_NAME02, SURVEY_NAME, true);
        setupProject(STUDY_NAME03, PROJECT_NAME03, SURVEY_NAME, true);
        setSurveyMetadataDropDir();
        initMockserver();
    }

    private void enableForwarding(String projectName, String forwardingPath)
    {
        log(String.format("Enabling forwarding for %1$s", projectName));
        goToProjectHome(projectName);
        SetupPage setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().checkEnableForwarding();
        setupPage.getStudySetupWebPart().setUrlField(FORWARDING_URL + "/" + forwardingPath);
        setupPage.getStudySetupWebPart().setUserField(FORWARDING_USER);
        setupPage.getStudySetupWebPart().setPasswordField(FORWARDING_PASSWORD);
        setupPage.validateSubmitButtonEnabled();
        setupPage.getStudySetupWebPart().clickSubmit();
    }

    private TokenListPage createTokenBatch(SetupPage setupPage)
    {
        String tokenBatchId, tokenCount = "100";

        log("Create " + tokenCount + " tokens.");
        TokenBatchPopup tokenBatchPopup = setupPage.getTokenBatchesWebPart().openNewBatchPopup();
        TokenListPage tokenListPage = tokenBatchPopup.createNewBatch(tokenCount);

        tokenBatchId = tokenListPage.getBatchId();
        log("First batch id: " + tokenBatchId);

        return tokenListPage;
    }

    private HttpRequest getMockRequest(String urlPath, String enrollmentToken)
    {
        return request()
                .withMethod("POST")
                .withPath("/" + urlPath)
                .withBody(new StringBody(
                    String.format(FORWARD_BODY_FORMAT, enrollmentToken, BASE_RESULTS.replaceAll("\\s", "")),
//                    true,
                    MediaType.PLAIN_TEXT_UTF_8)
                );
    }

    @Test
    public void testForwardResponse()
    {
        goToProjectHome(PROJECT_NAME01);
        enableForwarding(PROJECT_NAME01, STUDY_NAME01);

        goToProjectHome(PROJECT_NAME01);
        SetupPage setupPage = new SetupPage(this);
        TokenListPage tokenListPage = createTokenBatch(setupPage);
        String myToken = tokenListPage.getToken(0);

        checkErrors();
        PipelineStatusTable pst = goToDataPipeline();

        log("Testing successfully forwarding response");
        submitResponse(PROJECT_NAME01, STUDY_NAME01, myToken);

        String forwardingJobDescription = String.format(FORWARDING_PIPELINE_JOB_FORMAT, PROJECT_NAME01);
        //TODO: this may be flaky as Timer job may create one in the interim...
        waitForPipelineJobsToComplete(1, forwardingJobDescription, false);
        assertTrue("Forwarding job failed unexpectedly.", "Complete".equalsIgnoreCase(pst.getJobStatus(forwardingJobDescription)));

        HttpRequest req = getMockRequest(STUDY_NAME01, myToken);
        mockServer.verify(req, VerificationTimes.once()); //Will throw an AssertionError if not found correct number of times.

        log("Checking pipeline job log");
        //TODO: this may be flaky as Timer job may create one in the interim...
        pst.clickStatusLink(forwardingJobDescription);
        assertTextPresent("Forwarding completed. 1 response(s) sent to");
    }

    @Test
    public void testFailedForwardResponse()
    {
        goToProjectHome(PROJECT_NAME02);
        enableForwarding(PROJECT_NAME02, STUDY_NAME02);

        checkErrors();
        PipelineStatusTable pst = goToDataPipeline();

        log("Testing failed forwarding of survey response");
        submitResponse(PROJECT_NAME02, STUDY_NAME02, null);

        String forwardingJobDescription = String.format(FORWARDING_PIPELINE_JOB_FORMAT, PROJECT_NAME02);
        waitForPipelineJobsToComplete(1, forwardingJobDescription, true);
        assertTrue("Forwarding job Passed unexpectedly.", "Error".equalsIgnoreCase(pst.getJobStatus(forwardingJobDescription)));

        log("Checking pipeline job log");
        pst.clickStatusLink(forwardingJobDescription);
        assertTextPresent("ERROR: Stopping forwarding job.");
        checkExpectedErrors(1);
    }

    @Test
    public void testEnableAndDisableForwarding()
    {
        goToProjectHome(PROJECT_NAME02);
        HttpRequest req = request().withMethod("POST").withPath("/" + STUDY_NAME03);
        log("Testing forwarding of prior survey responses");

        checkErrors();
        PipelineStatusTable pst = goToDataPipeline();
        int oldCount = pst.getDataRowCount();

        log("Submitting responses prior to enabling forwarding");
        submitResponse(PROJECT_NAME03, STUDY_NAME03, null);
        submitResponse(PROJECT_NAME03, STUDY_NAME03, null);
        submitResponse(PROJECT_NAME03, STUDY_NAME03, null);
        int responseCount = 3;

        sleep(2000);  //Give pipeline job a chance to start processing
        pst = goToDataPipeline();  //refresh page
        int newCount = pst.getDataRowCount();
        assertEquals("Unexpected new pipeline job", oldCount, newCount);
        mockServer.verify(req, VerificationTimes.exactly(0));  //Will throw AssertionError if count doesn't match

        assertEquals("Response forwarding pipeline job count not as expected", 0, pst.getDataRowCount()); //Allow delta of 1 in the event scheduled job runs
        enableForwarding(PROJECT_NAME03, STUDY_NAME03);
        pst = goToDataPipeline();

        log("Submitting response to trigger forwarding now that it is enabled");
        submitResponse(PROJECT_NAME03, STUDY_NAME03, null);
        responseCount++;

        String forwardingJobDescription = String.format(FORWARDING_PIPELINE_JOB_FORMAT, PROJECT_NAME03);
        waitForPipelineJobsToComplete(1, forwardingJobDescription, false);
        assertTrue("Forwarding job failed unexpectedly.", "Complete".equalsIgnoreCase(pst.getJobStatus(forwardingJobDescription)));

        mockServer.verify(req, VerificationTimes.exactly(responseCount)); //Will throw an AssertionError if not found correct number of times.

        log("Checking pipeline job log");
        //TODO: this may be flaky as Timer job may create one in the interim...
        pst.clickStatusLink(forwardingJobDescription);
        assertTextPresent(String.format("Forwarding completed. %1$s response(s) sent to", responseCount));

        log("Clearing mockserver request logs");
        mockServer.clear(req);

        goToProjectHome(PROJECT_NAME03);
        SetupPage setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().uncheckEnableForwarding();
        setupPage.validateSubmitButtonEnabled();
        setupPage.getStudySetupWebPart().clickSubmit();

        pst = goToDataPipeline();
        oldCount = pst.getDataRowCount();
        submitResponse(PROJECT_NAME03, STUDY_NAME03, null);
        sleep(2000);  //Give pipeline job a chance to start processing
        pst = goToDataPipeline();
        newCount = pst.getDataRowCount();

        assertEquals("Unexpected new pipeline job", oldCount, newCount);
        mockServer.verify(req, VerificationTimes.exactly(0));  //Will throw AssertionError if count doesn't match
    }

    private String submitResponse(String projectName, String studyName, String batchtoken)
    {
        String appToken = getNewAppToken(projectName, studyName, batchtoken);
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, BASE_RESULTS);
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        return appToken;
    }

    @AfterClass
    public static void afterClassCleanup()
    {
        ForwardResponseTest afterClass = (ForwardResponseTest) getCurrentTest();

        if(null != mockServer)
        {
            afterClass.log("Stopping the mockserver.");
            mockServer.stop();

            try
            {
                while (mockServer.isRunning())
                {
                    afterClass.log("Waiting for the mockserver to stop.");
                    Thread.sleep(1000);
                }

                afterClass.log("The mockserver is stopped.");
            }
            catch (InterruptedException ie)
            {
                afterClass.log("Got an Interrupt Exception when trying to stop the mockserver: " + ie);
            }
        }
    }
}
