package org.labkey.test.tests.mobileappstudy;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.mobileappstudy.SubmitResponseCommand;
import org.labkey.test.pages.mobileappstudy.SetupPage;

import java.util.Map;

/**
 * Created by RyanS on 2/16/2017.
 */
@Category({Git.class})
public class DynamicSchemaTest extends BaseMobileAppStudyTest
{
    private static final String PROJECT_NAME = "DynamicSchemaTestProject";
    private static final String STUDY_NAME = "DynamicSchemaStudy";
    private static final String SURVEY_NAME = "NewSurvey";
    private static final String SURVEY_VERSION_V1 = "1";
    private static final String NEW_SURVEY_VERSION = "1.1";
    private static String RESPONSE_V1;

    @Override
    protected
    @Nullable String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    void setupProjects()
    {
        //Setup a study
        _containerHelper.deleteProject(PROJECT_NAME, false);
        _containerHelper.createProject(PROJECT_NAME, "Mobile App Study");
        goToProjectHome(PROJECT_NAME);
        SetupPage setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.checkResponseCollection();
        setupPage.studySetupWebPart.setShortName(STUDY_NAME);
        setupPage.validateSubmitButtonEnabled();
        setupPage.studySetupWebPart.clickSubmit();

        //setupLists();
        setSurveyMetadataDropDir();
        goToProjectHome(PROJECT_NAME);
        //populateLists();
    }

    @Test
    public void testAddSingleQuestions()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testDropSingleQuestions()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testAddSingleQuestionToSub()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testDropSingleQuestionFromSub()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testAddSingleToSubSub()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testDropSingleQuestionToSubSub()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testAddGroup()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testRemoveGroup()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testAddSubGroup()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    @Test
    public void testRemoveSubGroup()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
    }

    private void submitResponse(String appToken, String surveyName, String version, String responseString)
    {
        log("Testing AppToken not present");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, surveyName, version, appToken, responseString);
        cmd.execute(200);
    }

    private Map<String,Object> getTableMetaData(String table, String schema)
    {
        return getMobileAppData(table,schema).getMetaData();
    }
}
