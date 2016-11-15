package org.labkey.test.tests.mobileappstudy;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.mobileappstudy.EnrollParticipantCommand;
import org.labkey.test.commands.mobileappstudy.SubmitResponseCommand;
import org.labkey.test.pages.mobileappstudy.SetupPage;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@Category({Git.class})
public class ResponseSubmissionTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    protected final PortalHelper _portalHelper = new PortalHelper(this);

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
        return "Response Submission Test Project";
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
        EnrollParticipantCommand cmd = new EnrollParticipantCommand(project, studyShortName, batchToken, this::log);

        cmd.execute(200);
        return cmd.getAppToken();
    }

    @Test
    public void testAppStudyResponseRequests()
    {
        //Create study
        final String STUDY_NAME01 = "Study01";  // Study names are case insensitive, so test it once.
        final String STUDY_NAME02 = "Study02";
        final String PROJECT_NAME01 = getProjectName() + " " + STUDY_NAME01;
        final String PROJECT_NAME02 = getProjectName() + " " + STUDY_NAME02;
        final String SURVEY_NAME = "Fake_Survey 1";

        SetupPage setupPage;

        _containerHelper.deleteProject(PROJECT_NAME01, false);
        _containerHelper.createProject(PROJECT_NAME01, "Mobile App Study");
        goToProjectHome(PROJECT_NAME01);

        setupPage = new SetupPage(this);

        //Validate collection checkbox behavior
        log("Collection is initially disabled");
        assertFalse("Response collection is enabled at study creation", setupPage.studySetupWebPart.isResponseCollectionChecked());

        log("Enabling response collection doesn't allow submit prior to a valid study name");
        setupPage.studySetupWebPart.checkResponseCollection();
        setupPage.validateSubmitButtonDisabled();

        log("Set a study name.");
        setupPage.studySetupWebPart.setShortName(STUDY_NAME01);
        setupPage.validateSubmitButtonEnabled();

        log("Disabling response collection allows study config submission");
        setupPage.studySetupWebPart.uncheckResponseCollection();
        setupPage.validateSubmitButtonEnabled();
        setupPage.studySetupWebPart.clickSubmit();

        //Capture a participant appToken
        log("Requesting app token");
        String appToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, null);
        assertNotNull("Apptoken was null", appToken);
        log("AppToken received: " + appToken);

        //refresh the page
        goToProjectHome(PROJECT_NAME01);

        //Scenarios
        //        1. request body not present
        log("Testing bad request body");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log);
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYINFO_MISSING_MESSAGE, cmd.getExceptionMessage());

        //        2. AppToken not present
        log("Testing AppToken not present");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", "", "{}" );
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.PARTICIPANTID_MISSING_MESSAGE, cmd.getExceptionMessage());

        //        3. Invalid AppToken Participant
        log("Testing invalid apptoken");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", "INVALIDPARTICIPANTID", "{}" );
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.NO_PARTICIPANT_MESSAGE, cmd.getExceptionMessage());

        //        4. Invalid SurveyInfo
        //            A. SurveyInfo element missing
        log("Testing SurveyInfo element not present");
        cmd = new SubmitResponseCommand(this::log, null, null, appToken, "{}" );
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYINFO_MISSING_MESSAGE, cmd.getExceptionMessage());

        //            B. Survey Version
        log("Testing SurveyVersion not present");
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, null, appToken, "{}" );
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYVERSION_MISSING_MESSAGE, cmd.getExceptionMessage());

        //            C. Survey SurveyId
        log("Testing SurveyId not present");
        cmd = new SubmitResponseCommand(this::log, null, "1", appToken, "{}" );
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.SURVEYID_MISSING_MESSAGE, cmd.getExceptionMessage());

        //TODO: We don't currently lookup survey so this never fails
        //             D. Survey not found
//        cmd = new SubmitResponseCommand(this::log, "INvAlID SUrVEY NaM3", "1", appToken, "{}" );
//        cmd.execute(400);
//        assertEquals("Unexpected error message", cmd.SURVEY_NOT_FOUND_MESSAGE, cmd.getExceptionMessage());

        //        5. Response not present
        log("Testing Response element not present");
        cmd = new SubmitResponseCommand(this::log);
        cmd.setBody(String.format(SubmitResponseCommand.MISSING_RESPONSE_JSON_FORMAT, SURVEY_NAME, "1", appToken));
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.RESPONSE_MISSING_MESSAGE, cmd.getExceptionMessage());

        //        6. Study not collecting
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}" );
        cmd.execute(400);
        assertTrue("Unexpected error message", String.format(SubmitResponseCommand.COLLECTION_DISABLED_MESSAGE_FORMAT, STUDY_NAME01)
                .equalsIgnoreCase(cmd.getExceptionMessage()));

        //        7. Success
        //Enable study collection
        goToProjectHome(PROJECT_NAME01);
        setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.checkResponseCollection();
        setupPage.studySetupWebPart.clickSubmit();
        goToProjectHome(PROJECT_NAME01);

        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}" );
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());
        String originalUrl = cmd.getTargetURL();

        //        8. Verify submission is container agnostic
        //Setup a secondary study
        _containerHelper.deleteProject(PROJECT_NAME02, false);
        _containerHelper.createProject(PROJECT_NAME02, "Mobile App Study");
        goToProjectHome(PROJECT_NAME02);

        setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.checkResponseCollection();
        setupPage.studySetupWebPart.setShortName(STUDY_NAME02);
        setupPage.validateSubmitButtonEnabled();
        setupPage.studySetupWebPart.clickSubmit();

        //Submit API call using apptoken associated to original study
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}" );
        cmd.changeProjectTarget(PROJECT_NAME02);
        assertNotEquals("Attempting to test container agnostic submission and URL targets are the same", originalUrl, cmd.getTargetURL());
        cmd.execute(200);
        assertTrue("Submission failed, expected success", cmd.getSuccess());

        _containerHelper.deleteProject(PROJECT_NAME01, false);

        //        9. Check submission to deleted project
        //Submit API call using appToken associated to original study
        cmd = new SubmitResponseCommand(this::log, SURVEY_NAME, "1", appToken, "{}" );
        cmd.execute(400);
        assertEquals("Unexpected error message", SubmitResponseCommand.NO_PARTICIPANT_MESSAGE, cmd.getExceptionMessage());
    }
}
