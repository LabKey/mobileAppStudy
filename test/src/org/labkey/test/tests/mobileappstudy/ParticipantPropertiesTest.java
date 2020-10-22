package org.labkey.test.tests.mobileappstudy;

import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.Locator;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.mobileappstudy.EnrollmentTokenValidationCommand;
import org.labkey.test.commands.mobileappstudy.SubmitResponseCommand;
import org.labkey.test.commands.mobileappstudy.WithdrawParticipantCommand;
import org.labkey.test.components.mobileappstudy.TokenBatchPopup;
import org.labkey.test.data.mobileappstudy.ParticipantProperty;
import org.labkey.test.pages.list.BeginPage;
import org.labkey.test.pages.mobileappstudy.SetupPage;
import org.labkey.test.pages.mobileappstudy.TokenListPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestLogger;
import org.mockserver.integration.ClientAndServer;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({Git.class})
public class ParticipantPropertiesTest extends BaseMobileAppStudyTest
{
    public final static String STUDY_NAME01 = "PARTICIPANTPROPERTIESTEST";  // Study names are case insensitive
    public final static String PROJECT_NAME01 = "CreateParticipantPropertiesList" + STUDY_NAME01;
    public final static String STUDY_NAME02 = "WITHDRAWPARTICIPANTPROPERTIES";  // Study names are case insensitive
    public final static String PROJECT_NAME02 = "withdrawParticipantProperties" + STUDY_NAME02;
    public final static String STUDY_NAME03 = "NOTEUSEDFORQUERYS";  // Study names are case insensitive
    public final static String PROJECT_NAME03 = "NOTEUSEDFORQUERYS" + STUDY_NAME03;
    public final static String STUDY_NAME04 = "UPDATEPARTPROPS";  // Study names are case insensitive
    public final static String PROJECT_NAME04 = "UPDATE" + STUDY_NAME04;

    protected final static int PORT = 8083; //Setting to different port than ForwardResponseTest

    private final static String BASE_URL = "http://localhost:" + PORT;
    private final static String MODULE_NAME = "MobileAppStudy";
    private final static String METADATASERVICE_PROPERTY_NAME = "MetadataServiceBaseUrl";
    private final static String METADATASERVICE_ACCESSTOKEN_PROPERTY_NAME = "MetadataServiceAccessToken";
    private final static String MOCKSERVER_CALL_MATCHER_CLASS = "org.labkey.test.mockserver.mobileappstudy.ParticipantPropertiesSeverGetCallback";
    private final static String PARTICIPANT_PROPERTIES_LIST_NAME = "ParticipantProperties";
    private final static String ENROLLMENTTOKEN_FIELD_KEY = "EnrollmentToken";
    private final static List<String> POSTENROLLMENT_FIELD_NAMES = Arrays.asList("EnrollmentToken", "stringPostEnroll", "integerPostEnroll",
            "decimalPostEnroll", "datePostEnroll", "timePostEnroll", "booleanPostEnroll");
    private final static List<String> PREENROLLMENT_FIELD_NAMES = Arrays.asList("stringPreEnroll", "integerPreEnroll",
            "decimalPreEnroll", "datePreEnroll", "timePreEnroll", "booleanPreEnroll");
    private final static List<String> STANDARD_FIELD_NAMES = Arrays.asList("Modified", "lastIndexed",
            "ModifiedBy", "Created", "CreatedBy", "container", "EntityId");
    private final static List<String> ALL_FIELDS = Stream.of(PREENROLLMENT_FIELD_NAMES, POSTENROLLMENT_FIELD_NAMES, STANDARD_FIELD_NAMES).flatMap(Collection::stream).collect(Collectors.toList());
    private final static String TOKEN_BATCH_SIZE = "10";

    private final static int EXECUTE_SQL_INDEX = 0;
    private final static int XSTUDY_TOKEN_INDEX = 1;
    private static final int INSERT_ROW_TOKEN_INDEX = 2;

    public final static String WCP_SURVEY_METHOD = "activity";
    public final static String WCP_API_METHOD = "participantProperties";
    public final static String ADD_PATH = "AddPropertyPath";
    public final static String UPDATE_PATH = "UpdatePropertyPath";
    public final static String DELETE_PATH = "DeletePropertyPath";
    public final static String SURVEY_UPDATE_PATH = "SurveyUpdatePath";


    @Override
    protected @Nullable String getProjectName()
    {
        return PROJECT_NAME01;
    }
    protected static ClientAndServer mockServer = null;

    @Override
    void setupProjects()
    {
        initMockserver();
        setupProject(STUDY_NAME01, PROJECT_NAME01, null, true);
        goToProjectHome(PROJECT_NAME01);
        setupMockserverModuleProperties(PROJECT_NAME01, STUDY_NAME01);

        goToProjectHome(PROJECT_NAME01);
        createTokenBatch(PROJECT_NAME01);

        setSurveyMetadataDropDir();
        goToProjectHome(PROJECT_NAME01);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");

        setupProject(STUDY_NAME03, PROJECT_NAME03, null, true);
        setupMockserverModuleProperties(PROJECT_NAME03, STUDY_NAME03);
        goToProjectHome(PROJECT_NAME03);
        SetupPage setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().clickUpdateMetadata();
        createTokenBatch(PROJECT_NAME03);

        //Do this here as other tests will use this basic project, so if it is broken we may as well stop here
        testBaseParticipantProperties();
    }

    private void setupMockserverModuleProperties(String project, String token)
    {
        setupMockserverModuleProperties(project, token, BASE_URL);
    }

    private void setupMockserverModuleProperties(String project, String token, String url)
    {
        goToProjectHome(project);
        //Setup a study
        ModulePropertyValue baseUrlMP = new ModulePropertyValue(MODULE_NAME, project, METADATASERVICE_PROPERTY_NAME, url);
        ModulePropertyValue accessTokenMP = new ModulePropertyValue(MODULE_NAME, project, METADATASERVICE_ACCESSTOKEN_PROPERTY_NAME, token);
        setModuleProperties(Arrays.asList(baseUrlMP, accessTokenMP));
        goToProjectHome(project);
    }

    @Override
    protected void setupProject(String studyName, String projectName, String surveyName, boolean enableResponseCollection)
    {
        _containerHelper.deleteProject(projectName, false);
        super.setupProject(studyName, projectName, surveyName, enableResponseCollection);
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
            addRequestMatcher(mockServer, WCP_API_METHOD, this::log,"GET", MOCKSERVER_CALL_MATCHER_CLASS);
            addRequestMatcher(mockServer, String.join("/", ADD_PATH, WCP_API_METHOD), this::log,"GET", MOCKSERVER_CALL_MATCHER_CLASS);
            addRequestMatcher(mockServer, String.join("/", UPDATE_PATH, WCP_API_METHOD), this::log,"GET", MOCKSERVER_CALL_MATCHER_CLASS);
            addRequestMatcher(mockServer, String.join("/", DELETE_PATH, WCP_API_METHOD), this::log,"GET", MOCKSERVER_CALL_MATCHER_CLASS);
            addRequestMatcher(mockServer, String.join("/", SURVEY_UPDATE_PATH, WCP_API_METHOD), this::log,"GET", MOCKSERVER_CALL_MATCHER_CLASS);
            addRequestMatcher(mockServer, String.join("/", SURVEY_UPDATE_PATH, WCP_SURVEY_METHOD), this::log,"GET", MOCKSERVER_CALL_MATCHER_CLASS);
        }
        else {
            log("Mockserver is not running, could not add RequestMatcher.");
        }
    }


    @Before
    public void prep()
    {
        goToProjectHome(PROJECT_NAME01);
    }

    /**
     * Verify that ParticipantProperties list is created as expected
     */
    private void testBaseParticipantProperties()
    {
        goToProjectHome(PROJECT_NAME01);
        BeginPage beginPage = goToManageLists();
        assertTextPresent("No data to show");
        goToProjectHome(PROJECT_NAME01);

        //Create ParticipantProperties list
        SetupPage setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().clickUpdateMetadata();

        beginPage = goToManageLists();
        List<String> lists = beginPage.getGrid().getColumnDataAsText("Name");

        assertTrue("ParticipantProperties list not created", lists.contains(PARTICIPANT_PROPERTIES_LIST_NAME));
        clickAndWait(Locator.linkWithText(PARTICIPANT_PROPERTIES_LIST_NAME));
        List<String> propNames = _listHelper.goToEditDesign(PARTICIPANT_PROPERTIES_LIST_NAME).getFieldsPanel().fieldNames();
        assertTrue("Not all properties added to the ParticipantProperties list: " + String.join(", ", propNames), ALL_FIELDS.containsAll(propNames));
    }

    @Test
    public void testInsertRow() throws IOException, CommandException
    {
        goToProjectHome(PROJECT_NAME01);
        String token = TokenListPage.beginAt(this, PROJECT_NAME01).getToken(INSERT_ROW_TOKEN_INDEX);
        String testStringValue = "Some Test Value";
        String dateValue = "2019-02-22 00:00:00.000";
        String timeValue = "12:34:56.789";
        Number intValue = 1;
        Number doubleValue = 1.1;
        boolean booleanValue = true;

        Map<String, Object> values = new HashMap<>();
        values.put(ENROLLMENTTOKEN_FIELD_KEY, token);
        values.put(POSTENROLLMENT_FIELD_NAMES.get(1), testStringValue);
        values.put(POSTENROLLMENT_FIELD_NAMES.get(2), intValue);
        values.put(POSTENROLLMENT_FIELD_NAMES.get(3), doubleValue);
        values.put(POSTENROLLMENT_FIELD_NAMES.get(4), dateValue);
        values.put(POSTENROLLMENT_FIELD_NAMES.get(5), timeValue);
        values.put(POSTENROLLMENT_FIELD_NAMES.get(6), booleanValue);

        values.put(PREENROLLMENT_FIELD_NAMES.get(0), testStringValue);
        values.put(PREENROLLMENT_FIELD_NAMES.get(1), intValue);
        values.put(PREENROLLMENT_FIELD_NAMES.get(2), doubleValue);
        values.put(PREENROLLMENT_FIELD_NAMES.get(3), dateValue);
        values.put(PREENROLLMENT_FIELD_NAMES.get(4), timeValue);
        values.put(PREENROLLMENT_FIELD_NAMES.get(5), booleanValue);

        log("Insert row into participant properties list");
        UpdateRowsCommand cmd = new UpdateRowsCommand(LIST_SCHEMA, PARTICIPANT_PROPERTIES_LIST_NAME);
        cmd.addRow(values);
        cmd.execute(WebTestHelper.getRemoteApiConnection(), PROJECT_NAME01);

        String participantAppToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, token);
        Map<String, Object> params = new HashMap<>();
        params.put("participantId", participantAppToken);
        params.put("queryName", PARTICIPANT_PROPERTIES_LIST_NAME);
        params.put("schemaName", "lists");

        log(String.format("Query ParticipantProperties list, via select rows using participantId [%1$s] and enrollmentToken [%2$s]", participantAppToken, token));
        CommandResponse selectRowsResponse = callSelectRows(params);
        Map<String, Object> results = selectRowsResponse.getParsedData();
        assertEquals("Unexpected rowcount returned participant id", 1L, results.get("rowCount"));
        JSONArray rows = selectRowsResponse.getProperty("rows");
        JSONObject row = (JSONObject)rows.get(0);
        checkJsonObjectAgainstExpectedValues(values, row);
    }


    /**
     * Verify no rows return with invalid token
     */
    @Test
    public void testCrossStudyExecuteSQL() throws IOException, CommandException
    {
        Map<String, Object> params = new HashMap<>();
        TokenListPage tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME03);
        String token = tokenListPage.getToken(XSTUDY_TOKEN_INDEX);
        String participantAppToken = getNewAppToken(PROJECT_NAME03, STUDY_NAME03, token);
        params.put("participantId", participantAppToken);

        goToProjectHome(PROJECT_NAME01);
        String sql = String.format("SELECT * FROM participantproperties WHERE container = '%1$s';", getContainerId());
        params.put("sql", sql);

        CommandResponse executeSqlResponse = callExecuteSql(params);
        Map<String, Object> results = executeSqlResponse.getParsedData();
        assertEquals("Unexpected row returned across container boundary", 0L, results.get("rowCount"));
    }

    /**
     * Verify ExecuteSQL success
     */
    @Test
    public void testExecuteSQL() throws IOException, CommandException
    {
        Map<String, Object> params = new HashMap<>();
        TokenListPage tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME01);
        String token = tokenListPage.getToken(EXECUTE_SQL_INDEX);
        String participantAppToken = getNewAppToken(PROJECT_NAME01, STUDY_NAME01, token);
        params.put("participantId", participantAppToken);
        goToProjectHome(PROJECT_NAME01);
        String sql = String.format("SELECT * FROM participantproperties WHERE container = '%1$s';", getContainerId());
        params.put("sql", sql);

        CommandResponse executeSqlResponse = callExecuteSql(params);
        Map<String, Object> results = executeSqlResponse.getParsedData();
        assertEquals("Unexpected row returned w/o participant id", 1L, results.get("rowCount"));
        JSONArray rows = executeSqlResponse.getProperty("rows");
        JSONObject jsonArrayEntry = (JSONObject)rows.get(0);
        JSONObject rowData = (JSONObject)jsonArrayEntry.get("data");
        assertNotNull("No ParticipantProperties row data returned by execute sql", rowData);
        for (String propertyId : (Set<String>)rowData.keySet())
        {
            assertTrue(String.format("Expected column not found: " + propertyId), POSTENROLLMENT_FIELD_NAMES.contains(propertyId)
                    || PREENROLLMENT_FIELD_NAMES.contains(propertyId) || STANDARD_FIELD_NAMES.contains(propertyId));
        }
    }

    /**
     * Verify new batches of Enrollment Tokens are preloaded to the ParticipantProperties list
     */
    @Test
    public void testAddEnrollmentTokens()
    {
        SelectRowsResponse selectRowsResponse = getMobileAppData(PARTICIPANT_PROPERTIES_LIST_NAME, LIST_SCHEMA);
        assertTrue("EnrollmentToken is not the list's key field", ENROLLMENTTOKEN_FIELD_KEY.equalsIgnoreCase(selectRowsResponse.getIdColumn()));
        Integer rowCount = selectRowsResponse.getRowCount().intValue();

        createTokenBatch(PROJECT_NAME01);

        selectRowsResponse = getMobileAppData(PARTICIPANT_PROPERTIES_LIST_NAME, LIST_SCHEMA);
        assertEquals("New enrollment tokens not preloaded in ParticipantProperties list.", rowCount + Integer.valueOf(TOKEN_BATCH_SIZE), selectRowsResponse.getRowCount().intValue());
    }

    private void createTokenBatch(String project)
    {
        //Check if newly created enrollment tokens are loaded into the ParticipantProperties list
        goToProjectHome(project);
        SetupPage setupPage = new SetupPage(this);
        log(String.format("Creating %1$s tokens for %2$s", TOKEN_BATCH_SIZE, project));
        TokenBatchPopup tokenBatchPopup = setupPage.getTokenBatchesWebPart().openNewBatchPopup();
        tokenBatchPopup.selectOtherBatchSize();
        tokenBatchPopup.setOtherBatchSize(TOKEN_BATCH_SIZE);
        tokenBatchPopup.createNewBatch();
    }

    @Test
    public void testParticipantWithdrawl()
    {
        setupProject(STUDY_NAME02, PROJECT_NAME02, null, true);
        goToProjectHome(PROJECT_NAME02);
        setupMockserverModuleProperties(PROJECT_NAME02, STUDY_NAME02);

        //Create participant properties before EnrollmentTokens
        goToProjectHome(PROJECT_NAME02);
        SetupPage setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().clickUpdateMetadata();

        goToProjectHome(PROJECT_NAME02);
        createTokenBatch(PROJECT_NAME02);
        TokenListPage tokenListPage = TokenListPage.beginAt(this, PROJECT_NAME02);
        String token = tokenListPage.getToken(0);
        String token2 = tokenListPage.getToken(1);

        log("Token validation action: successful token request");
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(PROJECT_NAME02, STUDY_NAME02, token, this::log);
        cmd.execute(200);
        assertTrue("Enrollment token validation failed when it shouldn't have", cmd.getSuccess());
        Collection<ParticipantProperty> preenrollmentProperties = cmd.getPreEnrollmentParticipantProperties();
        assertEquals("Unexpected number of preenrollment properties", 6, preenrollmentProperties.size());
        for (ParticipantProperty prop : preenrollmentProperties)
            assertTrue("Unexpected preenrollment property: " + prop.getPropertyId(), PREENROLLMENT_FIELD_NAMES.contains(prop.getPropertyId()));

        String appToken = getNewAppToken(PROJECT_NAME02, STUDY_NAME02, token);
        String appToken2 = getNewAppToken(PROJECT_NAME02, STUDY_NAME02, token2);
        checkParticipantWithdrawal(token, appToken, true, "not deleted");
        checkParticipantWithdrawal(token2, appToken2, false, "deleted");
    }

    @Test
    public void testAddColumn()
    {
        String study = ADD_PATH;
        String project = PROJECT_NAME04 + study;
        String changeUrl = String.join("/", BASE_URL, study);
        String token = setupProjectWithParticipantProperties(study, project);

        verifyBaseParticipantPropertiesSetup(project, study, token);

        // Easiest way to change response body of mockserver call is to adjust the path,
        // so update the url used to request participant properties metadata
        setupMockserverModuleProperties(project, study, changeUrl);
        SetupPage setupPage = SetupPage.beginAt(this, project);
        setupPage.getStudySetupWebPart().clickUpdateMetadata();  //Should load OriginalParticipantProperty.json

        verifyAddColumn(project, study, token);
    }

    @Test
    public void testDeleteColumn()
    {
        String study = DELETE_PATH;
        String project = PROJECT_NAME04 + study;
        String changeUrl = String.join("/", BASE_URL, study);
        String token = setupProjectWithParticipantProperties(study, project);

        verifyBaseParticipantPropertiesSetup(project, study, token);

        // Easiest way to change response body of mockserver call is to adjust the path,
        // so update the url used to request participant properties metadata
        setupMockserverModuleProperties(project, study, changeUrl);
        SetupPage setupPage = SetupPage.beginAt(this, project);
        setupPage.getStudySetupWebPart().clickUpdateMetadata();  //Should load OriginalParticipantProperty.json

        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(project, study, token, this::log);
        cmd.execute(200);
        assertTrue("Enrollment token validation failed when it shouldn't have", cmd.getSuccess());
        Collection<ParticipantProperty> preenrollmentProperties = cmd.getPreEnrollmentParticipantProperties();
        assertEquals("Unexpected number of preenrollment properties", 2, preenrollmentProperties.size()); //Should have both original property, SingleProperty, and DeleteProperty
        for (ParticipantProperty prop : preenrollmentProperties)
            assertTrue("Unexpected preenrollment property: " + prop.getPropertyId(), Arrays.asList("SingleProperty", "DeleteProperty").contains(prop.getPropertyId()));
    }

    @Test
    public void testUpdateColumn() throws IOException, CommandException
    {
        String study = UPDATE_PATH;
        String project = PROJECT_NAME04 + study;
        String changeUrl = String.join("/", BASE_URL, study);
        String token = setupProjectWithParticipantProperties(study, project);

        verifyBaseParticipantPropertiesSetup(project, study, token);

        // Easiest way to change response body of mockserver call is to adjust the path,
        // so update the url used to request participant properties metadata
        setupMockserverModuleProperties(project, study, changeUrl);
        SetupPage setupPage = SetupPage.beginAt(this, project);
        setupPage.getStudySetupWebPart().clickUpdateMetadata();  //Should load OriginalParticipantProperty.json

        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(project, study, token, this::log);
        cmd.execute(200);
        assertTrue("Enrollment token validation failed when it shouldn't have", cmd.getSuccess());
        Collection<ParticipantProperty> preenrollmentProperties = cmd.getPreEnrollmentParticipantProperties();
        assertEquals("Unexpected number of preenrollment properties", 0, preenrollmentProperties.size()); //Should have no properties, SingleProperty has been made a postenrollment property

        Map<String, Object> params = new HashMap<>();
        String participantAppToken = getNewAppToken(project, study, token);
        params.put("participantId", participantAppToken);
        params.put("queryName", PARTICIPANT_PROPERTIES_LIST_NAME);
        params.put("schemaName", "lists");

        CommandResponse selectRowsResponse = callSelectRows(params);
        Map<String, Object> results = selectRowsResponse.getParsedData();
        assertEquals("Unexpected row returned w/o participant id", 1L, results.get("rowCount"));
        JSONArray rows = selectRowsResponse.getProperty("rows");
        Map<String, Object> row = (Map<String, Object>)rows.get(0);
        for (String propertyId : row.keySet())
        {
            assertTrue(String.format("Expected column not found: " + propertyId), "SingleProperty".equalsIgnoreCase(propertyId) || ENROLLMENTTOKEN_FIELD_KEY.equalsIgnoreCase(propertyId));
        }
    }

    @Test
    public void testUpdateAfterSurveyResponse()
    {
        String study = SURVEY_UPDATE_PATH;
        String project = PROJECT_NAME04 + study;
        String changeUrl = String.join("/", BASE_URL, study);
        String token = setupProjectWithParticipantProperties(study, project);

        verifyBaseParticipantPropertiesSetup(project, study, token);

        // Easiest way to change response body of mockserver call is to adjust the path,
        // so update the url used to request participant properties metadata
        setupMockserverModuleProperties(project, study, changeUrl);

        String appToken = getNewAppToken(project, study, token);
        String responseString = getResponseFromFile("ParticipantPropertiesMetadata", "Survey_Response.json");
        SubmitResponseCommand cmd = new SubmitResponseCommand(this::log, SURVEY_UPDATE_PATH, "1", appToken, responseString);
        cmd.execute(200); //Should load AddParticipantProperty.json

        TokenListPage tokenListPage = TokenListPage.beginAt(this, project);
        String token2 = tokenListPage.getToken(1);  //get next token (0 was used by setup method above)
        verifyAddColumn(project, study, token2);

        //Sanity check that survey was actually created too
        BeginPage beginPage = goToManageLists();
        List<String> lists = beginPage.getGrid().getColumnDataAsText("Name");
        assertTrue("Survey list not created", lists.contains(study.toUpperCase()));
    }

    private void verifyAddColumn(String project, String study, String token)
    {
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(project, study, token, this::log);
        cmd.execute(200);
        assertTrue("Enrollment token validation failed when it shouldn't have", cmd.getSuccess());
        Collection<ParticipantProperty> preenrollmentProperties = cmd.getPreEnrollmentParticipantProperties();
        assertEquals("Unexpected number of preenrollment properties", 2, preenrollmentProperties.size());
        for (ParticipantProperty prop : preenrollmentProperties)
            assertTrue("Unexpected preenrollment property: " + prop.getPropertyId(), Arrays.asList("SingleProperty", "AddedProperty").contains(prop.getPropertyId()));

    }

    /**
     * Sets up the StudyId and creates a batch of 10 tokens
     * @param study StudyId to use
     * @param project container name
     * @return first (0th) token in the batch generated
     */
    private String setupProjectWithParticipantProperties(String study, String project)
    {
        setupProject(study, project, null, true);
        goToProjectHome(project);
        setupMockserverModuleProperties(project, study);

        SetupPage setupPage = SetupPage.beginAt(this, project);
        setupPage.getStudySetupWebPart().clickUpdateMetadata();  //Should load OriginalParticipantProperty.json
        createTokenBatch(project);

        TokenListPage tokenListPage = TokenListPage.beginAt(this, project);
        return tokenListPage.getToken(0);
    }

    private void verifyBaseParticipantPropertiesSetup(String project, String study, String enrollmentToken)
    {
        //Verify base setup
        log("Token validation action: successful token request");
        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand(project, study, enrollmentToken, this::log);
        cmd.execute(200);
        assertTrue("Enrollment token validation failed when it shouldn't have", cmd.getSuccess());
        Collection<ParticipantProperty> preenrollmentProperties = cmd.getPreEnrollmentParticipantProperties();
        assertEquals("Unexpected number of preenrollment properties", 1, preenrollmentProperties.size());
        for (ParticipantProperty prop : preenrollmentProperties)
            assertTrue("Unexpected preenrollment property: " + prop.getPropertyId(), Arrays.asList("SingleProperty").contains(prop.getPropertyId()));
    }

    private void checkParticipantWithdrawal(String token, String appToken, boolean delete, String deleteMessage)
    {
        WithdrawParticipantCommand withdrawalCmd = new WithdrawParticipantCommand(appToken, delete, this::log);
        withdrawalCmd.execute(200);

        goToProjectHome(PROJECT_NAME02);
        SelectRowsResponse selectRowsResponse = getMobileAppData(PARTICIPANT_PROPERTIES_LIST_NAME, LIST_SCHEMA);
        boolean dataDeleted = true;
        for(Row row : selectRowsResponse.getRowset())
        {
            if (token.equalsIgnoreCase(row.getValue(ENROLLMENTTOKEN_FIELD_KEY).toString()))
                dataDeleted = false;
        }
        assertTrue(String.format("Withdrawn Participant's data %1$s from ParticipantProperties list", deleteMessage), delete == dataDeleted);
    }

    @AfterClass
    @LogMethod
    public static void afterClassCleanup() throws IOException
    {
        if(null != mockServer)
        {
            TestLogger.log("Stopping the mockserver.");
            mockServer.stop();

            TestLogger.log("Waiting for the mockserver to stop.");
            new FluentWait<>(mockServer).withMessage("waiting for the mockserver to stop.").until(mockServer -> !mockServer.isRunning());
            TestLogger.log("The mockserver is stopped.");
        }
    }

}
