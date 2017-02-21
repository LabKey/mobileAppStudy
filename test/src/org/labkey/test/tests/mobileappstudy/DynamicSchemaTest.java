package org.labkey.test.tests.mobileappstudy;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Git;
import org.labkey.test.data.mobileappstudy.AbstractQuestionResponse;
import org.labkey.test.data.mobileappstudy.InitialSurvey;
import org.labkey.test.data.mobileappstudy.NewSurvey;
import org.labkey.test.data.mobileappstudy.QuestionResponse;
import org.labkey.test.data.mobileappstudy.Survey;
import org.labkey.test.pages.mobileappstudy.SetupPage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private static final String SURVEY_VERSION = "1";
    private static final String NEW_SURVEY_VERSION = "1.1";

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
        populateLists();
    }

    @Test
    public void testAddSingleQuestions()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testDropSingleQuestions()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testAddSingleQuestionToSub()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testDropSingleQuestionFromSub()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testAddSingleToSubSub()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testDropSingleQuestionToSubSub()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testAddGroup()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testRemoveGroup()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testAddSubGroup()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    @Test
    public void testRemoveSubGroup()
    {
        String appToken = getNewAppToken(PROJECT_NAME,STUDY_NAME,null);
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
    }

    private void setupLists()
    {
        //Import static survey lists to populate
        _listHelper.importListArchive(TestFileUtils.getSampleData("TestLists.lists.zip"));
    }

    private void populateLists()
    {
        List<String> appTokens = new ArrayList<>();
        //need some data in the NewSurvey list so we can ensure it is retained when a question is removed in a later version of a survey
        for(int i=0; i<10; i++)
        {
            appTokens.add(getNewAppToken(PROJECT_NAME, STUDY_NAME, null));
        }
        for(String appToken : appTokens)
        {
            submitResponses(appToken);
        }
    }

    private void submitResponses(String appToken)
    {
        String fieldName = NewSurvey.BOOLEAN_FIELD;
        AbstractQuestionResponse.SupportedResultType type = AbstractQuestionResponse.SupportedResultType.BOOL;

        QuestionResponse qr = new QuestionResponse(type, fieldName, new Date(), new Date(), false, false);
        submitQuestion(qr, appToken,SURVEY_NAME,SURVEY_VERSION, 200);

        type = AbstractQuestionResponse.SupportedResultType.DATE;
        fieldName = NewSurvey.DATE_FIELD;

        Date dateVal = new Date();
        qr = new QuestionResponse(type, fieldName, new Date(), new Date(), false, dateVal);
        submitQuestion(qr, appToken,SURVEY_NAME,SURVEY_VERSION, 200);

        type = AbstractQuestionResponse.SupportedResultType.TEXT;
        fieldName = NewSurvey.TEXT_SCALE_FIELD;
        String val = "I hate waffles";

        qr = new QuestionResponse(type, fieldName, new Date(), new Date(), false, val);
        submitQuestion(qr, appToken,SURVEY_NAME,SURVEY_VERSION, 200);

        fieldName = NewSurvey.CONTINUOUS_SCALE_FIELD;
        val = "Pancakes";
        submitQuestion(qr, appToken,SURVEY_NAME,SURVEY_VERSION, 200);
        
//        qr = new ChoiceQuestionResponse("medName",
//                new Date(), new Date(), false, "Acetaminophen");
//        QuestionResponse groupedQuestionResponse = new GroupedQuestionResponse(rx,
//                new Date(), new Date(), false, qr);
//        submitQuestion(qr, appToken,SURVEY_NAME,SURVEY_VERSION, 200);
//
//        QuestionResponse groupedQuestionResponse1 = new GroupedQuestionResponse("rx",
//                new Date(), new Date(), false, new GroupedQuestionResponse("Group", new Date(), new Date(), false,
//                new QuestionResponse(AbstractQuestionResponse.SupportedResultType.BOOL, "Bool", new Date(), new Date(), false, true),
//                new QuestionResponse(AbstractQuestionResponse.SupportedResultType.NUMERIC, "Decimal", new Date(), new Date(), false, 3.14),
//                new QuestionResponse(AbstractQuestionResponse.SupportedResultType.TEXT, "Text", new Date(), new Date(), false, "I'm part of a grouped group"),
//                new QuestionResponse(AbstractQuestionResponse.SupportedResultType.DATE, "Date", new Date(), new Date(), false, new Date())
//        ));
//        submitQuestion(qr, appToken,SURVEY_NAME,SURVEY_VERSION, 200);
    }

    private Map<String,Object> getTableMetaData(String table, String schema)
    {
        return getMobileAppData(table,schema).getMetaData();
    }
}
