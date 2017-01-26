package org.labkey.test.tests.mobileappstudy;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.mobileappstudy.WithdrawParticipantCommand;
import org.labkey.test.components.mobileappstudy.TokenBatchPopup;
import org.labkey.test.data.mobileappstudy.AbstractQuestionResponse;
import org.labkey.test.data.mobileappstudy.ChoiceQuestionResponse;
import org.labkey.test.data.mobileappstudy.GroupedQuestionResponse;
import org.labkey.test.data.mobileappstudy.InitialSurvey;
import org.labkey.test.data.mobileappstudy.MedForm;
import org.labkey.test.data.mobileappstudy.QuestionResponse;
import org.labkey.test.data.mobileappstudy.Survey;
import org.labkey.test.pages.mobileappstudy.SetupPage;
import org.labkey.test.pages.mobileappstudy.TokenListPage;
import org.labkey.test.util.ListHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Test to verify participant withdrawal from a MobileAppStudy
 */
@Category({Git.class})
public class StudyWithdrawTest extends BaseMobileAppStudyTest
{
    private static final String PROJECT_NAME = "StudyWithdrawTestProject";
    private static final String STUDY_NAME = "StudyWithdrawTestStudy";
    private static final String MOBILEAPP_SCHEMA = "mobileappstudy";
    private static final String LIST_SCHEMA = "lists";
    private final static String BASE_RESULTS = "{\n" +
            "\t\t\"start\": \"2016-09-06 15:48:13 +0000\",\n" +
            "\t\t\"end\": \"2016-09-06 15:48:45 +0000\",\n" +
            "\t\t\"results\": []\n" +
            "}";
    private static final List<String> TABLES = Arrays.asList("Response","ResponseMetadata","EnrollmentToken");
    //Lists populated by test data
    private static final List<String> LISTS = Arrays.asList("InitialSurvey", "InitialSurveyRx", "InitialSurveyRxGroup",
            "InitialSurveyRxMedName", "InitialSurveySupplements");
    private final static String SURVEY_NAME = "InitialSurvey";
    private final static String SURVEY_VERSION = "123.9";

    private final static String ENROLLED = "0";

    private static String HAS_RESPONSES_DELETE;
    private static String HAS_RESPONSES_NO_DELETE;
    private static String NO_RESPONSES_DELETE;
    private static String WITHDRAWS_TWICE;
    private static String STAYS_IN;
    private static String HRD_KEY;
    private static String HRND_KEY;
    private static String NRD_KEY;
    private static String WT_KEY;
    private static String SI_KEY;

    @Override
    void setupProjects()
    {
        _containerHelper.deleteProject(getProjectName(),false);
        _containerHelper.createProject(getProjectName(), "Mobile App Study");
        _containerHelper.enableModule("MobileAppStudy");
        goToProjectHome();
        SetupPage setupPage = new SetupPage(this);
        setupPage.studySetupWebPart.setShortName(STUDY_NAME);
        setupPage.studySetupWebPart.checkResponseCollection();
        setupPage.studySetupWebPart.clickSubmit();

        //Use enrollment tokens so EnrollmentToken Table is populated
        TokenBatchPopup tokenBatchPopup = setupPage.tokenBatchesWebPart.openNewBatchPopup();
        TokenListPage tokenListPage = tokenBatchPopup.createNewBatch("100");
        int token = 0;
        HAS_RESPONSES_DELETE = getNewAppToken(PROJECT_NAME, STUDY_NAME, tokenListPage.getToken(token++));
        HAS_RESPONSES_NO_DELETE = getNewAppToken(PROJECT_NAME, STUDY_NAME, tokenListPage.getToken(token++));
        NO_RESPONSES_DELETE = getNewAppToken(PROJECT_NAME, STUDY_NAME, tokenListPage.getToken(token++));
        WITHDRAWS_TWICE = getNewAppToken(PROJECT_NAME, STUDY_NAME, tokenListPage.getToken(token++));
        STAYS_IN = getNewAppToken(PROJECT_NAME, STUDY_NAME, tokenListPage.getToken(token++));

        Map<String,String> appTokenParticipantIds = getColumnMap("Participant", "AppToken", "RowId");
        HRD_KEY = appTokenParticipantIds.get(HAS_RESPONSES_DELETE);
        HRND_KEY = appTokenParticipantIds.get(HAS_RESPONSES_NO_DELETE);
        NRD_KEY = appTokenParticipantIds.get(NO_RESPONSES_DELETE);
        WT_KEY = appTokenParticipantIds.get(WITHDRAWS_TWICE);
        SI_KEY = appTokenParticipantIds.get(STAYS_IN);

        _listHelper.createList(getProjectName(), SURVEY_NAME, ListHelper.ListColumnType.AutoInteger, "Key" );
        setupLists();
        submitResponses(Arrays.asList(HAS_RESPONSES_DELETE,HAS_RESPONSES_NO_DELETE,WITHDRAWS_TWICE,STAYS_IN));

    }

    private void setupLists()
    {
        //Import static survey lists to populate
        goToProjectHome();
        _listHelper.importListArchive(TestFileUtils.getSampleData("TestLists.lists.zip"));
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testParticipantWithdrawal()
    {
        //foreach, validate status update, data deletion, appToken null
        //invalid token
        WithdrawParticipantCommand command = new WithdrawParticipantCommand("BadToken",false, this::log);
        HttpResponse httpResponse = command.execute(400);
        SelectRowsResponse selectResponse = getMobileAppData("Participant");
        //valid token, no delete
        log("withdraw participant " + HRD_KEY + " delete responses");
        command = new WithdrawParticipantCommand(HAS_RESPONSES_DELETE, true, this::log);
        command.execute(200);
        //valid token, delete
        log("withdraw participant " + HRND_KEY + " do not delete responses");
        command = new WithdrawParticipantCommand(HAS_RESPONSES_NO_DELETE, false, this::log);
        command.execute(200);
        //no data
        log("withdraw participant " + NRD_KEY + " delete responses");
        command = new WithdrawParticipantCommand(NO_RESPONSES_DELETE, true, this::log);
        command.execute(200);
        //valid token, already withdrawn
        log("withdraw participant " + WT_KEY + " do not delete responses");
        command = new WithdrawParticipantCommand(WITHDRAWS_TWICE, false, this::log);
        command.execute(200);
        log("attempt to withdraw participant " + WT_KEY + " delete responses");
        command = new WithdrawParticipantCommand(WITHDRAWS_TWICE, true, this::log);
        command.execute(400);

        //*** Verify Participant Table ***
        //all users except STAYS_IN should no longer be enrolled
        log("verify participant status");
        Map<String, String> colMap = getColumnMap("Participant", "RowId", "Status");
        Assert.assertFalse("User " + HRD_KEY + " still shown as enrolled in Participant after withdrawing", ENROLLED.equals(colMap.get(HRD_KEY)));
        Assert.assertFalse("User " + HRND_KEY + " still shown as enrolled in Participant after withdrawing", ENROLLED.equals(colMap.get(HRND_KEY)));
        Assert.assertFalse("User " + NRD_KEY + " still shown as enrolled in Participant after withdrawing", ENROLLED.equals(colMap.get(NRD_KEY)));
        Assert.assertFalse("User " + WT_KEY + " still shown as enrolled in Participant after withdrawing", ENROLLED.equals(colMap.get(WT_KEY)));
        //User who did not withdraw should be enrolled
        Assert.assertTrue("User " + SI_KEY + " still shown as enrolled in Participant after withdrawing", ENROLLED.equals(colMap.get(SI_KEY)));

        //check tokens are null for withdrawn participants
        colMap = getColumnMap("Participant", "RowId", "AppToken");
        Assert.assertTrue("User " + HRD_KEY + " did not have appToken null after withdrawal", StringUtils.isBlank(colMap.get(HRD_KEY)));
        Assert.assertTrue("User " + HRND_KEY + " did not have appToken null after withdrawal", StringUtils.isBlank(colMap.get(HRND_KEY)));
        Assert.assertTrue("User " + NRD_KEY + " did not have appToken null after withdrawal", StringUtils.isBlank(colMap.get(NRD_KEY)));
        Assert.assertTrue("User " + WT_KEY + " did not have appToken null after withdrawal", StringUtils.isBlank(colMap.get(WT_KEY)));
        //check tokens retained for remaining users
        Assert.assertFalse("User " + SI_KEY + " did not withdraw but did not retain appToken ", StringUtils.isBlank(colMap.get(SI_KEY)));


        //*** Verify Other Tables ***
        String deletedFormat = "Data found for user %1$s in table %2$s that should have been deleted";
        String retainedFormat = "Data should have been retained in table %2$s for user %1$s but was not";
        log("verify participant data deletion from tables");
        assertParticipantIds(MOBILEAPP_SCHEMA, TABLES, deletedFormat, retainedFormat);

        //*** Verify Lists ***
        deletedFormat = "Data found for user %1$s in list %2$s that should have been deleted";
        retainedFormat = "Data should have been retained in list %2$s for user %1$s but was not";
        log("verify participant data deletion from lists");
        assertParticipantIds(LIST_SCHEMA, LISTS, deletedFormat, retainedFormat);
    }

    private void assertParticipantIds(String schema, List<String> tables, String deletedFormat, String retainedFormat)
    {
        for (String table : tables)
        {
            Set<String> participantIds = getParticipantIds(schema, table);

            //Users who have elected to have data deleted should have responses deleted
            Assert.assertFalse(String.format(deletedFormat, HRD_KEY, table), participantIds.contains(HRD_KEY));
            Assert.assertFalse(String.format(deletedFormat, NRD_KEY, table), participantIds.contains(NRD_KEY));
            //Users who have elected not to have data deleted should retain data
            Assert.assertTrue(String.format(retainedFormat, HRND_KEY, table), participantIds.contains(HRND_KEY));
            Assert.assertTrue(String.format(retainedFormat, SI_KEY, table), participantIds.contains(SI_KEY));
            Assert.assertTrue(String.format(retainedFormat, WT_KEY, table), participantIds.contains(WT_KEY));
        }
    }

    private SelectRowsResponse getMobileAppData(String table)
    {
        return getMobileAppData(table, MOBILEAPP_SCHEMA);
    }

    private SelectRowsResponse getMobileAppData(String table, String schema)
    {
        Connection cn = createDefaultConnection(true);
        SelectRowsCommand selectCmd = new SelectRowsCommand(schema, table);
        selectCmd.setColumns(Arrays.asList("*"));

        SelectRowsResponse selectResp = null;
        try
        {
            selectResp = selectCmd.execute(cn,getCurrentContainerPath());
        }
        catch (CommandException | IOException e)
        {
            log(e.getMessage());
            throw new RuntimeException(e);
        }

        return selectResp;
    }

    private void submitResponses(List<String> appTokens)
    {
        for (String appToken : appTokens)
        {
            String fieldName = InitialSurvey.PLANNED_PREGNANCY;
            AbstractQuestionResponse.SupportedResultType type = AbstractQuestionResponse.SupportedResultType.BOOL;

            QuestionResponse qr = new QuestionResponse(type, fieldName, new Date(), new Date(), false, false);
            submitQuestion(qr, appToken, 200);

            type = AbstractQuestionResponse.SupportedResultType.DATE;
            fieldName = InitialSurvey.DUE_DATE;

            Date dateVal = new Date();
            qr = new QuestionResponse(type, fieldName, new Date(), new Date(), false, dateVal);
            submitQuestion(qr, appToken, 200);

            type = AbstractQuestionResponse.SupportedResultType.TEXT;
            fieldName = InitialSurvey.ILLNESS_WEEK;
            String val = "I \u9829 waffles";

            qr = new QuestionResponse(type, fieldName, new Date(), new Date(), true, val);
            submitQuestion(qr, appToken, 200);

            String field = InitialSurvey.SUPPLEMENTS;
            String value1 = "Pancakes";
            String value2 = "French Toast";

            qr = new ChoiceQuestionResponse( field, new Date(), new Date(), false,
                    value1, "Waffles", value2, "Crepes");
            submitQuestion(qr, appToken, 200);

            String rx = MedForm.MedType.Prescription.getDisplayName();
            qr = new ChoiceQuestionResponse("medName",
                    new Date(), new Date(), false, "Acetaminophen");
            QuestionResponse groupedQuestionResponse = new GroupedQuestionResponse(rx,
                    new Date(), new Date(), false, qr);
            submitQuestion(groupedQuestionResponse, appToken, 200);

            QuestionResponse groupedQuestionResponse1 = new GroupedQuestionResponse("rx",
                    new Date(), new Date(), false, new GroupedQuestionResponse("Group", new Date(), new Date(), false,
                    new QuestionResponse(AbstractQuestionResponse.SupportedResultType.BOOL, "Bool", new Date(), new Date(), false, true),
                    new QuestionResponse(AbstractQuestionResponse.SupportedResultType.NUMBER, "Decimal", new Date(), new Date(), false, 3.14),
                    new QuestionResponse(AbstractQuestionResponse.SupportedResultType.SCALE, "Integer", new Date(), new Date(), false, 400),
                    new QuestionResponse(AbstractQuestionResponse.SupportedResultType.TEXT, "Text", new Date(), new Date(), false, "I'm part of a grouped group"),
                    new QuestionResponse(AbstractQuestionResponse.SupportedResultType.DATE, "Date", new Date(), new Date(), false, new Date())
            ));
            submitQuestion(groupedQuestionResponse1, appToken, 200);
        }
    }

    private String submitQuestion(QuestionResponse qr, String appToken, int expectedStatusCode)
    {
        Survey survey = new InitialSurvey(appToken, SURVEY_NAME, SURVEY_VERSION, new Date(), new Date());
        survey.addResponse(qr);

        return submitSurvey(survey, expectedStatusCode);
    }

    private Map<String, String> getColumnMap(String table, String keyCol, String valCol)
    {
        Map<String, String> map = new HashMap<>();
        getMobileAppData(table).getRows().forEach(row -> map.put(String.valueOf(row.get(keyCol)), Objects.toString(row.get(valCol), "")));
        return map;
    }

    private Set<String> getParticipantIds(String schema, String tableName)
    {
        HashSet<String> set = new HashSet<>();
        getMobileAppData(tableName, schema).getRows().forEach(row -> set.add(Objects.toString(row.get("ParticipantId"), "")));
        return set;
    }

    private List<String> getCol(String table, String col, String schema)
    {
        List<String> column = new ArrayList<>();
        SelectRowsResponse selectResponse = getMobileAppData(table,schema);
        List<Map<String,Object>> rows = selectResponse.getRows();
        for(Map<String,Object> row : rows)
        {
            column.add(String.valueOf(row.get(col)));
        }
        return column;
    }

    private boolean isParticipantIdPresent(String table, String participantId, String schema)
    {
        List<String> col = getCol(table, "ParticipantId",schema);
        return col.contains(participantId);
    }
}
