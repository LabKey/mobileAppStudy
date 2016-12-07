package org.labkey.test.tests.mobileappstudy;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.mobileappstudy.EnrollParticipantCommand;
import org.labkey.test.commands.mobileappstudy.SubmitResponseCommand;
import org.labkey.test.pages.mobileappstudy.SetupPage;
import org.labkey.test.util.PostgresOnlyTest;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@Category({Git.class})
public class ResponseSubmissionTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    //Create study
    private final static String STUDY_NAME01 = "Study01";  // Study names are case insensitive
    private final static String STUDY_NAME02 = "Study02";
    private final static String STUDY_NAME03 = "Study03";
    private final static String BASE_PROJECT_NAME = "Response Submission Test Project";
    private final static String PROJECT_NAME01 = BASE_PROJECT_NAME + " " + STUDY_NAME01;
    private final static String PROJECT_NAME02 = BASE_PROJECT_NAME + " " + STUDY_NAME02;
    private final static String PROJECT_NAME03 = BASE_PROJECT_NAME + " " + STUDY_NAME03;
    private final static String SURVEY_NAME = "Fake_Survey 1";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        for (String project : _containerHelper.getCreatedProjects())
        {
            _containerHelper.deleteProject(project, false);
        }
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return BASE_PROJECT_NAME;
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
    private String getNewAppToken(String project, String studyShortName, String batchToken)
    {
        log("Requesting app token for project [" + PROJECT_NAME01 +"] and study [" + STUDY_NAME01 + "]");
        EnrollParticipantCommand cmd = new EnrollParticipantCommand(project, studyShortName, batchToken, this::log);

        cmd.execute(200);
        String appToken = cmd.getAppToken();
        assertNotNull("AppToken was null", appToken);
        log("AppToken received: " + appToken);

        return appToken;
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        ResponseSubmissionTest initTest = (ResponseSubmissionTest)getCurrentTest();
        initTest.setupProjects();

    }

    public void setupProjects()
    {
        _containerHelper.deleteProject(PROJECT_NAME01, false);
        _containerHelper.createProject(PROJECT_NAME01, "Mobile App Study");
        log("Set a study name.");
        goToProjectHome(PROJECT_NAME01);
        SetupPage setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.setShortName(STUDY_NAME01);
        setupPage.validateSubmitButtonEnabled();
        setupPage.studySetupWebPart.clickSubmit();

        //Setup a secondary study
        _containerHelper.deleteProject(PROJECT_NAME02, false);
        _containerHelper.createProject(PROJECT_NAME02, "Mobile App Study");
        goToProjectHome(PROJECT_NAME02);
        setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.checkResponseCollection();
        setupPage.studySetupWebPart.setShortName(STUDY_NAME02);
        setupPage.validateSubmitButtonEnabled();
        setupPage.studySetupWebPart.clickSubmit();
    }



    @Test
    public void testRequestBodyNotPresent()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);

        checkErrors();
        //Scenarios
        //        1. request body not present
        log("Testing bad request body");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log);
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYINFO_MISSING_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);
    }

    @Test
    public void testAppToken()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);

        checkErrors();
        //        2. AppToken not present
        log("Testing AppToken not present");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", "", "{}");
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.PARTICIPANTID_MISSING_MESSAGE, cmd.getExceptionMessage());

        //        3. Invalid AppToken Participant
        log("Testing invalid apptoken");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", "INVALIDPARTICIPANTID", "{}");
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.NO_PARTICIPANT_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);

    }

    @Test
    public void testSurveyInfo()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);
        //Capture a participant appToken
        String appToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, null);

        checkErrors();
        //        4. Invalid SurveyInfo
        //            A. SurveyInfo element missing
        log("Testing SurveyInfo element not present");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, null, null, appToken, "{}");
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYINFO_MISSING_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);

        //            B. Survey Version
        log("Testing SurveyVersion not present");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, null, appToken, "{}");
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYVERSION_MISSING_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);

        //            C. Survey SurveyId
        log("Testing SurveyId not present");
        cmd = new SubmitResponseCommand(this::log, null, "1", appToken, "{}");
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYID_MISSING_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);

        //TODO: We don't currently lookup survey so this never fails
        //             D. Survey not found
//        cmd = new SubmitResponseCommand(this::log, "INvAlID SUrVEY NaM3", "1", appToken, "{}" );
//        cmd.execute(400);
//        assertEquals("Unexpected error message", cmd.SURVEY_NOT_FOUND_MESSAGE, cmd.getExceptionMessage());
    }

    @Test
    public void testResponseNotPresent()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);
        String appToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, null);
        checkErrors();
        //        5. Response not present
        log("Testing Response element not present");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log);
        cmd.setBody(String.format(SubmitResponseCommand.MISSING_RESPONSE_JSON_FORMAT, SURVEY_NAME, "1", appToken));
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.RESPONSE_MISSING_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);
    }

    @Test
    public void testResponseCollection()
    {
        //refresh the page
        goToProjectHome(PROJECT_NAME01);
        String appToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, null);
        SetupPage setupPage = new SetupPage(this);
        if (setupPage.studySetupWebPart.isResponseCollectionChecked())
        {
            setupPage.studySetupWebPart.uncheckResponseCollection();
            log("Disabling response collection for " + STUDY_NAME01);
            setupPage.studySetupWebPart.clickSubmit();
        }

        checkErrors();
        //        6. Study not collecting
        log("Testing Response Submission with Study collection turned off");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}");
        cmd.execute(400);
        assertTrue("Unexpected error message", String.format(SubmitResponseCommand.COLLECTION_DISABLED_MESSAGE_FORMAT, STUDY_NAME01)
                .equalsIgnoreCase(cmd.getExceptionMessage()));
        checkExpectedErrors(1);

        //        7. Success
        //Enable study collection
        log("Enabling response collection for " + STUDY_NAME01);
        goToProjectHome(PROJECT_NAME01);
        setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.checkResponseCollection();
        setupPage.studySetupWebPart.clickSubmit();
        goToProjectHome(PROJECT_NAME01);

        log("Testing Response Submission with Study collection turned on");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}");
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        goToProjectHome(PROJECT_NAME01);
        log("Disabling response collection for " + STUDY_NAME01);
        setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.uncheckResponseCollection();
        setupPage.studySetupWebPart.clickSubmit();
        goToProjectHome(PROJECT_NAME01);

        //        8. Test submitting to a Study previously collecting, but not currently accepting results
        log("Testing Response Submission with Study collection turned off after previously being on");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}");
        cmd.execute(400);
        assertTrue("Unexpected error message", String.format(SubmitResponseCommand.COLLECTION_DISABLED_MESSAGE_FORMAT, STUDY_NAME01)
                .equalsIgnoreCase(cmd.getExceptionMessage()));
        checkExpectedErrors(1);
    }

    @Test
    public void testContainerSubmission()
    {
        //        8. Verify submission is container agnostic
        log("Testing Response Submission is container agnostic");
        goToProjectHome(PROJECT_NAME02);  //Setup Previously
        //Capture a participant appToken
        String appToken = getNewAppToken(PROJECT_NAME02, STUDY_NAME02, null);

        log("Verifying " + STUDY_NAME02 + " collecting responses.");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}");
        log("Testing submission to root container");
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());
        String originalUrl = cmd.getTargetURL();

        //Submit API call using apptoken associated to original study
        log("Submitting from " + PROJECT_NAME02 + " container");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}");
        cmd.changeProjectTarget(PROJECT_NAME02);
        assertNotEquals("Attempting to test container agnostic submission and URL targets are the same", originalUrl, cmd.getTargetURL());
        log("Testing submission to matching container");
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        //Submit API call using apptoken associated to original study
        log("Submitting from " + PROJECT_NAME01 + " container");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}");
        cmd.changeProjectTarget(PROJECT_NAME01);
        assertNotEquals("Attempting to test container agnostic submission and URL targets are the same", originalUrl, cmd.getTargetURL());
        log("Testing submission to appToken/container mismatch");
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());
    }

    @Test
    public void testSubmissionToDeletedProject()
    {
        //Setup a third study that we can delete
        _containerHelper.deleteProject(PROJECT_NAME03, false);
        _containerHelper.createProject(PROJECT_NAME03, "Mobile App Study");
        goToProjectHome(PROJECT_NAME03);
        SetupPage setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.checkResponseCollection();
        setupPage.studySetupWebPart.setShortName(STUDY_NAME03);
        setupPage.validateSubmitButtonEnabled();
        setupPage.studySetupWebPart.clickSubmit();
        longWait();
        goToProjectHome(PROJECT_NAME03);

        //Capture a participant appToken
        String appToken = getNewAppToken(PROJECT_NAME03, STUDY_NAME03, null);

        log("Verifying Response Submission prior to study deletion");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}");
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());
        log("successful submission to " + STUDY_NAME03);

        _containerHelper.deleteProject(PROJECT_NAME03, false);

        //        9. Check submission to deleted project
        //Submit API call using appToken associated to deleted study
        checkErrors();
        log("Verifying Response Submission after study deletion");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}" );
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.NO_PARTICIPANT_MESSAGE, cmd.getExceptionMessage());
        checkExpectedErrors(1);
    }
}
