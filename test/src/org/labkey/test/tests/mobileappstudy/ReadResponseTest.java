package org.labkey.test.tests.mobileappstudy;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.collections.ArrayListMap;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.categories.Git;
import org.labkey.test.components.mobileappstudy.TokenBatchPopup;
import org.labkey.test.pages.mobileappstudy.SetupPage;
import org.labkey.test.pages.mobileappstudy.TokenListPage;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Category({Git.class})
public class ReadResponseTest extends BaseMobileAppStudyTest
{

    final String PROJECT_NAME = getProjectName();
    final String PROJECT_STUDY_NAME = "TEST_READRESPONSE_STUDY";
    final String LIST_DIFF_DATATYPES = "TestListDiffDataTypes";

    final String FIRST_STRING_FIELD_VALUE = "This is the string value for participant ";
    final String SECOND_STRING_FIELD_VALUE = "This is the second string value for participant ";
    final String THIRD_STRING_FIELD_VALUE = "This is the third string value for participant ";

    final String FIRST_MULTILINE_STRING_FIELD = "This is the \r\nfirst\r\nmulti-line for participant $\r\nand here is a new line.";
    final String SECOND_MULTILINE_STRING_FIELD = "This is the \r\nsecond\r\nmulti-line for participant $\r\nand here is a new line.";
    final String THIRD_MULTILINE_STRING_FIELD = "This is the \r\nthird\r\nmulti-line for participant $\r\nand here is a new line.";

    final String FIRST_FLAG_FIELD = "First flag ";
    final String SECOND_FLAG_FIELD = "Second flag ";
    final String THIRD_FLAG_FIELD = "Third flag ";

    final String FIRST_MANTISSA = ".0123456789";
    final String SECOND_MANTISSA = ".22222";
    final String THIRD_MANTISSA = ".3333";

    final String FIRST_DATE = "2017/03/17 11:11:11";
    final String SECOND_DATE = "2017/01/15 08:02:00";
    final String THIRD_DATE = "2016/11/20 14:25:00";

    final int FIRST_INT_OFFSET = 5;
    final int SECOND_INT_OFFSET = 7;
    final int THIRD_INT_OFFSET = 11;

    static ParticipantInfo participantToSkip, participantWithMultipleRow, participantWithOneRow;

    protected final PortalHelper _portalHelper = new PortalHelper(this);

    @Override
    protected String getProjectName()
    {
        return "Read Response Test Project";
    }

    @Override
    void setupProjects()
    {
        _containerHelper.deleteProject(PROJECT_NAME, false);
        _containerHelper.createProject(PROJECT_NAME, "Mobile App Study");

        createBatchAndAssignTokens();

        setupList();

    }

    private void createBatchAndAssignTokens()
    {
        String batchId, tokenCount = "100";

        log("Creating the batch.");
        goToProjectHome(PROJECT_NAME);

        SetupPage setupPage = new SetupPage(this);

        log("Set a study name.");
        setupPage.studySetupWebPart.setShortName(PROJECT_STUDY_NAME)
                .clickSubmit();

        log("Create " + tokenCount + " tokens.");
        TokenBatchPopup tokenBatchPopup = setupPage.tokenBatchesWebPart.openNewBatchPopup();
        TokenListPage tokenListPage = tokenBatchPopup.createNewBatch(tokenCount);

        batchId = tokenListPage.getBatchId();
        log("Batch Id: " + batchId);

        List<String> tokensToAssign = new ArrayList<>();
        tokensToAssign.add(tokenListPage.getToken(0));
        tokensToAssign.add(tokenListPage.getToken(1));
        tokensToAssign.add(tokenListPage.getToken(2));
        tokensToAssign.add(tokenListPage.getToken(3));
        tokensToAssign.add(tokenListPage.getToken(4));
        tokensToAssign.add(tokenListPage.getToken(5));
        tokensToAssign.add(tokenListPage.getToken(6));

        log("Validate that the correct info for the tokens is shown in the grid.");
        clickTab("Setup");
        confirmBatchInfoCreated(setupPage, batchId, tokenCount, "0");

        log("Now assign some of the tokens from the batch.");

        assignTokens(tokensToAssign, PROJECT_NAME, PROJECT_STUDY_NAME);

    }

    private void setupList()
    {
        List<ParticipantInfo> participantsInfo = getTokens();

        goToProjectHome();

        log("Create a list with columns for each of the basic data types.");
        ListHelper.ListColumn participantIdColumn = new ListHelper.ListColumn("participantId", "participantId", ListHelper.ListColumnType.Integer, "");
        ListHelper.ListColumn stringTypeColumn = new ListHelper.ListColumn("stringField", "stringField", ListHelper.ListColumnType.String, "");
        ListHelper.ListColumn multiLineTypeColumn = new ListHelper.ListColumn("multiLineField", "multiLineField", ListHelper.ListColumnType.MultiLine, "");
        ListHelper.ListColumn booleanTypeColumn = new ListHelper.ListColumn("booleanField", "booleanField", ListHelper.ListColumnType.Boolean, "");
        ListHelper.ListColumn integerTypeColumn = new ListHelper.ListColumn("integerField", "integerField", ListHelper.ListColumnType.Integer, "");
        ListHelper.ListColumn doubleTypeColumn = new ListHelper.ListColumn("doubleField", "doubleField", ListHelper.ListColumnType.Double, "");
        ListHelper.ListColumn dateTimeTypeColumn = new ListHelper.ListColumn("dateTimeField", "dateTimeField", ListHelper.ListColumnType.DateTime, "");
        ListHelper.ListColumn flagTypeColumn = new ListHelper.ListColumn("flagField", "flagField", ListHelper.ListColumnType.Flag, "");

        _listHelper.createList(getProjectName(), LIST_DIFF_DATATYPES, ListHelper.ListColumnType.AutoInteger, "Key", participantIdColumn, stringTypeColumn, multiLineTypeColumn, booleanTypeColumn, integerTypeColumn, doubleTypeColumn, dateTimeTypeColumn, flagTypeColumn);
        clickButton("Done");

        clickAndWait(Locator.linkWithText(LIST_DIFF_DATATYPES));

        int index = participantsInfo.size()/2;
        ReadResponseTest.participantToSkip = new ParticipantInfo(participantsInfo.get(index).getId(), participantsInfo.get(index).getAppToken());
        log("Not going to put participant: " + ReadResponseTest.participantToSkip.getId() + " (" + ReadResponseTest.participantToSkip.getAppToken() + ") into the list.");

        int idAsInt;
        Map<String, String> rowData;

        for(ParticipantInfo participantInfo : participantsInfo)
        {
            if(!participantInfo.getId().equals(ReadResponseTest.participantToSkip.getId()))
            {

                // Convert the id to an int because it will be used in some of the numeric fields below.
                idAsInt = Integer.parseInt(participantInfo.getId());

                rowData = new HashMap<>();

                rowData.put("participantId", participantInfo.getId());
                rowData.put("stringField", FIRST_STRING_FIELD_VALUE + participantInfo.getId());
                rowData.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", participantInfo.getId()));

                if ((idAsInt & 1) == 0)
                    rowData.put("booleanField", "true");
                else
                    rowData.put("booleanField", "false");

                rowData.put("integerField", Integer.toString(idAsInt + FIRST_INT_OFFSET));

                rowData.put("doubleField", participantInfo.getId() + FIRST_MANTISSA);

                rowData.put("dateTimeField", FIRST_DATE);

                rowData.put("flagField", FIRST_FLAG_FIELD + participantInfo.getId());

                _listHelper.insertNewRow(rowData);
            }
        }

        ReadResponseTest.participantWithMultipleRow = new ParticipantInfo(participantsInfo.get(0).getId(), participantsInfo.get(0).getAppToken());
        log("Now add a few more rows in the list for participant: " + ReadResponseTest.participantWithMultipleRow.getId() + " (" + ReadResponseTest.participantWithMultipleRow.getAppToken() + ")");

        idAsInt = Integer.parseInt(ReadResponseTest.participantWithMultipleRow.getId());

        rowData = new HashMap<>();
        rowData.put("participantId", ReadResponseTest.participantWithMultipleRow.getId());
        rowData.put("stringField", SECOND_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId());
        rowData.put("multiLineField", SECOND_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithMultipleRow.getId()));
        rowData.put("booleanField", "true");
        rowData.put("integerField", Integer.toString(idAsInt + SECOND_INT_OFFSET));
        rowData.put("doubleField", ReadResponseTest.participantWithMultipleRow.getId() + SECOND_MANTISSA);
        rowData.put("dateTimeField", SECOND_DATE);
        rowData.put("flagField", SECOND_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId());
        _listHelper.insertNewRow(rowData);

        rowData = new HashMap<>();
        rowData.put("participantId", ReadResponseTest.participantWithMultipleRow.getId());
        rowData.put("stringField", THIRD_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId());
        rowData.put("multiLineField", THIRD_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithMultipleRow.getId()));
        rowData.put("booleanField", "false");
        rowData.put("integerField", Integer.toString(idAsInt + THIRD_INT_OFFSET));
        rowData.put("doubleField", ReadResponseTest.participantWithMultipleRow.getId() + THIRD_MANTISSA);
        rowData.put("dateTimeField", THIRD_DATE);
        rowData.put("flagField", THIRD_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId());
        _listHelper.insertNewRow(rowData);

        ReadResponseTest.participantWithOneRow = new ParticipantInfo(participantsInfo.get(1).getId(), participantsInfo.get(1).getAppToken());
    }

    private List<ParticipantInfo> getTokens()
    {
        List<ParticipantInfo> _participantInfos = new ArrayList<>();

        try
        {

            Connection cn = createDefaultConnection(false);
            SelectRowsCommand selectCmd = new SelectRowsCommand("mobileappstudy", "Participant");
            SelectRowsResponse rowsResponse = selectCmd.execute(cn, getProjectName());
            log("Row count: " + rowsResponse.getRows().size());

            for(Map<String, Object> row: rowsResponse.getRows())
            {
                _participantInfos.add(new ParticipantInfo(row.get("RowId").toString(), row.get("AppToken").toString()));
            }
        }
        catch(CommandException ce)
        {
            fail("Command exception when running query: " + ce);
        }
        catch(IOException ioe)
        {
            fail("IO exception when running query: " + ioe);
        }

        log("_participantInfos.size(): " + _participantInfos.size());

        return _participantInfos;
    }

    @Test
    public void validateQueryViewWithMultipleRows() throws CommandException, IOException, ParseException
    {

        log("Call the API with participant " + ReadResponseTest.participantWithMultipleRow.getId() + " (" + ReadResponseTest.participantWithMultipleRow.getAppToken() + "). This participant should return multiple rows.");
        goToProjectHome();
        String participantAppToken = ReadResponseTest.participantWithMultipleRow.getAppToken();

        Map<String, Object> params = new HashMap<>();
        params.put("schemaName", "MobileAppResponse");
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "participantId, stringField, multiLineField, booleanField, integerField, doubleField, dateTimeField, flagField");
        params.put("participantId", participantAppToken);

        CommandResponse rowsResponse = callSelectRows(params);

        JSONArray jsonArray = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithMultipleRow.getId() + " (" + ReadResponseTest.participantWithMultipleRow.getAppToken() + ") not as expected.", 3, jsonArray.size());

        log("Validate the first item returned in the json.");
        Map<String, Object> expectedValues = new ArrayListMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithMultipleRow.getId()));

        int idAsInt = Integer.parseInt(ReadResponseTest.participantWithMultipleRow.getId());
        if ((idAsInt & 1) == 0)
            expectedValues.put("booleanField", true);
        else
            expectedValues.put("booleanField", false);

        expectedValues.put("integerField", String.valueOf(idAsInt + FIRST_INT_OFFSET));
        expectedValues.put("doubleField", ReadResponseTest.participantWithMultipleRow.getId() + FIRST_MANTISSA);
        expectedValues.put("dateTimeField", FIRST_DATE);
        expectedValues.put("flagField", FIRST_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId());

        JSONObject jsonObject = (JSONObject)jsonArray.get(0);
        checkJsonObjectAgainstExpectedValues(expectedValues, jsonObject);

        log("Validate the second item returned in the json.");
        expectedValues = new ArrayListMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("stringField", SECOND_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("multiLineField", SECOND_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithMultipleRow.getId()));
        expectedValues.put("booleanField", true);
        expectedValues.put("integerField", String.valueOf(idAsInt + SECOND_INT_OFFSET));
        expectedValues.put("doubleField", ReadResponseTest.participantWithMultipleRow.getId() + SECOND_MANTISSA);
        expectedValues.put("dateTimeField", SECOND_DATE);
        expectedValues.put("flagField", SECOND_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId());

        jsonObject = (JSONObject)jsonArray.get(1);
        checkJsonObjectAgainstExpectedValues(expectedValues, jsonObject);

        log("Validate the third item returned in the json.");
        expectedValues = new ArrayListMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("stringField", THIRD_STRING_FIELD_VALUE + ReadResponseTest.participantWithMultipleRow.getId());
        expectedValues.put("multiLineField", THIRD_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithMultipleRow.getId()));
        expectedValues.put("booleanField", false);
        expectedValues.put("integerField", String.valueOf(idAsInt + THIRD_INT_OFFSET));
        expectedValues.put("doubleField", ReadResponseTest.participantWithMultipleRow.getId() + THIRD_MANTISSA);
        expectedValues.put("dateTimeField", THIRD_DATE);
        expectedValues.put("flagField", THIRD_FLAG_FIELD + ReadResponseTest.participantWithMultipleRow.getId());

        jsonObject = (JSONObject)jsonArray.get(2);
        checkJsonObjectAgainstExpectedValues(expectedValues, jsonObject);

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateQueryViewWithOneRow() throws CommandException, IOException, ParseException
    {

        log("Call the API with participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + "). This participant should return one row.");
        goToProjectHome();
        String participantAppToken = ReadResponseTest.participantWithOneRow.getAppToken();

        Map<String, Object> params = new HashMap<>();
        params.put("schemaName", "MobileAppResponse");
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "participantId, stringField, multiLineField, booleanField, integerField, doubleField, dateTimeField, flagField");
        params.put("participantId", participantAppToken);

        CommandResponse rowsResponse = callSelectRows(params);

        JSONArray jsonArray = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, jsonArray.size());

        log("Validate row returned.");
        Map<String, Object> expectedValues = new ArrayListMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithOneRow.getId()));

        int idAsInt = Integer.parseInt(ReadResponseTest.participantWithOneRow.getId());
        if ((idAsInt & 1) == 0)
            expectedValues.put("booleanField", true);
        else
            expectedValues.put("booleanField", false);

        expectedValues.put("integerField", String.valueOf(idAsInt + FIRST_INT_OFFSET));
        expectedValues.put("doubleField", ReadResponseTest.participantWithOneRow.getId() + FIRST_MANTISSA);
        expectedValues.put("dateTimeField", FIRST_DATE);
        expectedValues.put("flagField", FIRST_FLAG_FIELD + ReadResponseTest.participantWithOneRow.getId());

        JSONObject jsonObject = (JSONObject)jsonArray.get(0);
        checkJsonObjectAgainstExpectedValues(expectedValues, jsonObject);

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateQueryViewWithNoRows() throws CommandException, IOException, ParseException
    {

        log("Call the API with participant " + ReadResponseTest.participantToSkip.getId() + " (" + ReadResponseTest.participantToSkip.getAppToken() + "). This participant has no rows in the list.");
        goToProjectHome();
        String participantAppToken = ReadResponseTest.participantToSkip.getAppToken();

        Map<String, Object> params = new HashMap<>();
        params.put("schemaName", "MobileAppResponse");
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "participantId, stringField, multiLineField, booleanField, integerField, doubleField, dateTimeField, flagField");
        params.put("participantId", participantAppToken);

        CommandResponse rowsResponse = callSelectRows(params);

        JSONArray jsonArray = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantToSkip.getId() + " (" + ReadResponseTest.participantToSkip.getAppToken() + ") not as expected.", 0, jsonArray.size());

        log("Looks good. Go home.");
        goToHome();
    }

    @Test
    public void validateQueryViewColumnParameter() throws CommandException, IOException, ParseException
    {

        String columnsToReturn = "integerField, participantId, stringField, dateTimeField, multiLineField";

        log("Call the API with participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + "). This participant should return one row.");
        log("Only these columns '" + columnsToReturn + "' should be returned.");

        goToProjectHome();
        String participantAppToken = ReadResponseTest.participantWithOneRow.getAppToken();

        Map<String, Object> params = new HashMap<>();
        params.put("schemaName", "MobileAppResponse");
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", columnsToReturn);
        params.put("participantId", participantAppToken);

        CommandResponse rowsResponse = callSelectRows(params);

        JSONArray jsonArray = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, jsonArray.size());

        log("Validate row returned.");
        Map<String, Object> expectedValues = new ArrayListMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithOneRow.getId()));

        int idAsInt = Integer.parseInt(ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("integerField", String.valueOf(idAsInt + FIRST_INT_OFFSET));

        expectedValues.put("dateTimeField", FIRST_DATE);

        JSONObject jsonObject = (JSONObject)jsonArray.get(0);
        checkJsonObjectAgainstExpectedValues(expectedValues, jsonObject);

        log("Call the API with no columns parameter, this should return all columns.");

        params = new HashMap<>();
        params.put("schemaName", "MobileAppResponse");
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("participantId", participantAppToken);

        rowsResponse = callSelectRows(params);

        jsonArray = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, jsonArray.size());

        log("Validate row returned. All columns should be there.");
        expectedValues = new ArrayListMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithOneRow.getId()));

        if ((idAsInt & 1) == 0)
            expectedValues.put("booleanField", true);
        else
            expectedValues.put("booleanField", false);

        expectedValues.put("integerField", String.valueOf(idAsInt + FIRST_INT_OFFSET));
        expectedValues.put("doubleField", ReadResponseTest.participantWithOneRow.getId() + FIRST_MANTISSA);
        expectedValues.put("dateTimeField", FIRST_DATE);
        expectedValues.put("flagField", FIRST_FLAG_FIELD + ReadResponseTest.participantWithOneRow.getId());

        jsonObject = (JSONObject)jsonArray.get(0);
        checkJsonObjectAgainstExpectedValues(expectedValues, jsonObject);

        log("Now call API with only bad column names.");
        params = new HashMap<>();
        params.put("schemaName", "MobileAppResponse");
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "foo, bar");
        params.put("participantId", participantAppToken);

        rowsResponse = callSelectRows(params);

        jsonArray = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, jsonArray.size());

        // If only invalid columns were provided no columns should be returned.
        expectedValues = new ArrayListMap<>();

        jsonObject = (JSONObject)jsonArray.get(0);
        checkJsonObjectAgainstExpectedValues(expectedValues, jsonObject);

        columnsToReturn = "integerField, participantId, foo, stringField, dateTimeField, bar, multiLineField";
        log("Now validate with a mix of valid and invalid columns. Column parameter: '" + columnsToReturn + "'.");

        params.put("schemaName", "MobileAppResponse");
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", columnsToReturn);
        params.put("participantId", participantAppToken);

        rowsResponse = callSelectRows(params);

        jsonArray = rowsResponse.getProperty("rows");
        Assert.assertEquals("Number of rows returned for participant " + ReadResponseTest.participantWithOneRow.getId() + " (" + ReadResponseTest.participantWithOneRow.getAppToken() + ") not as expected.", 1, jsonArray.size());

        log("Validate row returned.");
        expectedValues = new ArrayListMap<>();
        expectedValues.put("participantId", ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("stringField", FIRST_STRING_FIELD_VALUE + ReadResponseTest.participantWithOneRow.getId());
        expectedValues.put("multiLineField", FIRST_MULTILINE_STRING_FIELD.replace("$", ReadResponseTest.participantWithOneRow.getId()));

        expectedValues.put("integerField", String.valueOf(idAsInt + FIRST_INT_OFFSET));

        expectedValues.put("dateTimeField", FIRST_DATE);

        jsonObject = (JSONObject)jsonArray.get(0);
        checkJsonObjectAgainstExpectedValues(expectedValues, jsonObject);

        log("Looks good. Go home.");
        goToHome();

    }

    @Test
    public void validateQueryViewErrorConditions() throws CommandException, IOException, ParseException
    {

        final String ERROR_NO_PARTICIPANTID = "ParticipantId not included in request";
        goToProjectHome();

        log("Call the API without a participantId.");
        Map<String, Object> params = new HashMap<>();
        params.put("schemaName", "MobileAppResponse");
        params.put("query.queryName", LIST_DIFF_DATATYPES);
        params.put("query.columns", "participantId, stringField, multiLineField, booleanField, integerField, doubleField, dateTimeField, flagField");

        try
        {
            callSelectRows(params);
        }
        catch(CommandException ce)
        {
            Assert.assertTrue("Command exception did not include expected message: ", ce.getMessage().equals(ERROR_NO_PARTICIPANTID));
        }

        log("Looks good. Go home.");
        goToHome();

    }

    private void confirmBatchInfoCreated(SetupPage setupPage, String batchId, String expectedTokenCount, String expectedUsedCount)
    {
        Map<String, String> batchData = setupPage.tokenBatchesWebPart.getBatchData(batchId);

        assertEquals("BatchId not as expected.", batchId, batchData.get("RowId"));
        assertEquals("Expected number of tokens not created.", expectedTokenCount, batchData.get("Count"));
        assertEquals("Number of tokens in use not as expected.", expectedUsedCount, batchData.get("TokensInUse"));
    }

    private CommandResponse callSelectRows(Map<String, Object> params) throws IOException, CommandException
    {

        Connection cn = createDefaultConnection(false);
        Command selectCmd = new Command("mobileAppStudy", "selectRows");
        selectCmd.setParameters(params);

        return selectCmd.execute(cn, getProjectName());

    }

    private void checkJsonObjectAgainstExpectedValues(Map<String, Object> expectedValues, JSONObject jsonObject)
    {
        Set<String> columns = expectedValues.keySet();

        for(String column : columns)
        {
            Assert.assertTrue("Exected column " + column + " was not in the jsonObject.", jsonObject.keySet().contains(column));

            switch(column)
            {
                case "stringField":
                case "multiLineField":
                case "dateTimeField":
                case "flagField":
                    Assert.assertEquals(column + " not as expected.", expectedValues.get(column), jsonObject.get(column));
                    break;
                case "participantId":
                case "integerField":
                    Assert.assertEquals(column + " not as expected.", expectedValues.get(column), Long.toString((Long)jsonObject.get(column)));
                    break;
                case "doubleField":
                    Assert.assertEquals(column + " not as expected.", expectedValues.get(column), Double.toString((Double)jsonObject.get(column)));
                    break;
                case "booleanField":
                    if ((boolean)expectedValues.get("booleanField"))
                        Assert.assertTrue("booleanField was not true (as expected).",(Boolean)jsonObject.get("booleanField"));
                    else
                        Assert.assertFalse("booleanField was not false (as expected).",(Boolean)jsonObject.get("booleanField"));
                    break;
            }
        }

        // If we've gotten to this point then we know that all of the expected columns and values were there.
        // Now we need to check that the jsonObject did not return any unexpected columns.
        String unexpectedJsonColum = "";
        boolean pass = true;
        for(Object jsonColumn : jsonObject.keySet())
        {
            if((!expectedValues.keySet().contains(jsonColumn)) && (!jsonColumn.equals("Key")))
            {
                unexpectedJsonColum = unexpectedJsonColum + "Found unexpected column " + jsonColumn + " in jsonObject.\r\n";
                pass = false;
            }
        }

        Assert.assertTrue(unexpectedJsonColum, pass);

    }

    private SelectRowsResponse getListInfo(String schema, String query) throws IOException, CommandException
    {
        Connection cn;
        SelectRowsCommand selectCmd  = new SelectRowsCommand(schema, query);
        cn = createDefaultConnection(false);
        return  selectCmd.execute(cn, getProjectName());
    }

    private class ParticipantInfo
    {
        protected String _participantId;
        protected String _appToken;

        public ParticipantInfo()
        {
            // Do nothing constructor.
        }

        public ParticipantInfo(String participantId, String appToken)
        {
            _participantId = participantId;
            _appToken = appToken;
        }

        public void setId(String id)
        {
            _participantId = id;
        }

        public String getId()
        {
            return _participantId;
        }

        public void setAppToken(String appToken)
        {
            _appToken = appToken;
        }

        public String getAppToken()
        {
            return _appToken;
        }

    }

}
