/*
 * Copyright (c) 2018-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.labkey.test.commands.mobileappstudy.EnrollParticipantCommand;
import org.labkey.test.commands.mobileappstudy.EnrollmentTokenValidationCommand;
import org.labkey.test.components.mobileappstudy.TokenBatchPopup;
import org.labkey.test.pages.mobileappstudy.SetupPage;
import org.labkey.test.pages.mobileappstudy.TokenListPage;

import java.io.IOException;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Category({Git.class})
public class SharedStudyIdTest extends BaseMobileAppStudyTest
{
    private static final String CLIENT_1_PROJECT_NAME = "Shared Study Client 1";
    private static final String CLIENT_2_PROJECT_NAME = "Shared Study Client 2";
    private static final String PROJECT_NAME01 = "SharedStudyIdTest DataPartner1";
    private static final String PROJECT_NAME02 = "SharedStudyIdTest DataPartner2";
    private static final String STUDY_ID = "SharedStudy01";
    private static final String STUDY_SUBFOLDER = STUDY_ID + " Subfolder";
    private static final String CLIENT_1_TOKEN_STUDY = CLIENT_1_PROJECT_NAME + "/" + STUDY_SUBFOLDER;
    private static final String CLIENT_2_TOKEN_STUDY = CLIENT_2_PROJECT_NAME + "/" + STUDY_SUBFOLDER;

    private static final String NO_TOKEN_STUDY_ID = "NoTokenSharedStudy01";
    private static final String NO_TOKEN_STUDY_SUBFOLDER = NO_TOKEN_STUDY_ID + " Subfolder";

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(CLIENT_1_PROJECT_NAME, false);
        _containerHelper.deleteProject(CLIENT_2_PROJECT_NAME, false);
        _containerHelper.deleteProject(PROJECT_NAME01, false);
        _containerHelper.deleteProject(PROJECT_NAME02, false);
    }

    void setUpProject(String project, String subfolder, String studyId, boolean addTokens)
    {
        if (!_containerHelper.getCreatedProjects().contains(project))
            _containerHelper.createProject(project, null);
        _containerHelper.createSubfolder(project, subfolder, "Mobile App Study");

        String projectPath = project + "/" + subfolder;

        SetupPage setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().setShortName(studyId);
        setupPage.getStudySetupWebPart().clickSubmit();

        if (addTokens)
        {
            log("Creating 10 tokens for " + projectPath);
            TokenBatchPopup tokenBatchPopup = setupPage.getTokenBatchesWebPart().openNewBatchPopup();
            tokenBatchPopup.selectOtherBatchSize();
            tokenBatchPopup.setOtherBatchSize("10");
            tokenBatchPopup.createNewBatch();
        }
    }

    @Override
    void setupProjects()
    {
        _containerHelper.deleteProject(CLIENT_1_PROJECT_NAME, false);
        _containerHelper.deleteProject(CLIENT_2_PROJECT_NAME, false);
        setUpProject(CLIENT_1_PROJECT_NAME, STUDY_SUBFOLDER,  STUDY_ID,true);
        setUpProject(CLIENT_1_PROJECT_NAME, NO_TOKEN_STUDY_SUBFOLDER, NO_TOKEN_STUDY_ID, false);
        setUpProject(CLIENT_2_PROJECT_NAME, STUDY_SUBFOLDER, STUDY_ID, true);
        setUpProject(CLIENT_2_PROJECT_NAME, NO_TOKEN_STUDY_SUBFOLDER, NO_TOKEN_STUDY_ID, false);
    }

    @Test
    public void testStudyNameShared()
    {
        final String STUDY_FOLDER_NAME = "StudyFolder";
        final String SHORT_NAME = "Shared";

        _containerHelper.createProject(PROJECT_NAME01, "Collaboration");
        _containerHelper.createSubfolder(PROJECT_NAME01, STUDY_FOLDER_NAME,"Mobile App Study");

        SetupPage setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().setShortName(SHORT_NAME);
        setupPage.getStudySetupWebPart().clickSubmit();

        _containerHelper.createProject(PROJECT_NAME02, "Collaboration");
        _containerHelper.createSubfolder(PROJECT_NAME02, STUDY_FOLDER_NAME, "Mobile App Study");

        setupPage = new SetupPage(this);
        setupPage.getStudySetupWebPart().setShortName(SHORT_NAME);
        setupPage.getStudySetupWebPart().clickSubmit();
        goToProjectHome(PROJECT_NAME02);
        clickFolder(STUDY_FOLDER_NAME);
        assertEquals("Study name not saved for second project", SHORT_NAME.toUpperCase(), setupPage.getStudySetupWebPart().getShortName());

        log("Testing enrollment, which should fail without any tokens.");
        EnrollParticipantCommand enrollCmd = new EnrollParticipantCommand("home", SHORT_NAME, null, "NA", this::log);
        enrollCmd.execute(400);
        assertFalse("Enrollment should fail when two projects share a study id but have no enrollment tokens", enrollCmd.getSuccess());
    }


    @Test
    public void testValidateWithTokens()
    {
        // test validation for a token from each study
        // ensure that you get the appropriate errors when trying to enroll when a study id is shared but there are no tokens
        TokenListPage tokenListPage = TokenListPage.beginAt(this, CLIENT_1_TOKEN_STUDY);
        String token = tokenListPage.getToken(0);

        EnrollmentTokenValidationCommand cmd = new EnrollmentTokenValidationCommand("home", STUDY_ID, token, this::log);
        cmd.execute(200);
        assertTrue("Enrollment token validation for " + CLIENT_1_TOKEN_STUDY + " failed when it shouldn't have", cmd.getSuccess());

        tokenListPage = TokenListPage.beginAt(this, CLIENT_2_TOKEN_STUDY);
        token = tokenListPage.getToken(0);
        cmd = new EnrollmentTokenValidationCommand("home", STUDY_ID, token, this::log);
        cmd.execute(200);
        assertTrue("Enrollment token validation for " + CLIENT_2_TOKEN_STUDY + " failed when it shouldn't have", cmd.getSuccess());
    }

    @Test
    public void testEnrollWithTokens()
    {
        // test enrollment for a token from each study
        // then check validation using the same tokens (should see an error about token already in use, but no exception)
        TokenListPage tokenListPage = TokenListPage.beginAt(this, CLIENT_1_TOKEN_STUDY);
        String token = tokenListPage.getToken(0);

        EnrollParticipantCommand enrollCmd = new EnrollParticipantCommand("home", STUDY_ID, token, "true", this::log);
        enrollCmd.execute(200);
        assertTrue("Enrollment with token '" + token + "' for " + CLIENT_1_TOKEN_STUDY + " failed when it shouldn't have", enrollCmd.getSuccess());
        EnrollmentTokenValidationCommand validateCmd = new EnrollmentTokenValidationCommand("home", STUDY_ID, token, this::log);
        validateCmd.execute(400);
        assertFalse("Enrollment token validation for " + CLIENT_1_TOKEN_STUDY + " with token '" + token + "' should fail after enrollment succeeds", validateCmd.getSuccess());

        tokenListPage = TokenListPage.beginAt(this, CLIENT_2_TOKEN_STUDY);
        token = tokenListPage.getToken(0);
        enrollCmd = new EnrollParticipantCommand("home", STUDY_ID, token, "false", this::log);
        enrollCmd.execute(200);
        assertTrue("Enrollment with token '" + token + "' for  " + CLIENT_2_TOKEN_STUDY + " failed when it shouldn't have", enrollCmd.getSuccess());
        validateCmd = new EnrollmentTokenValidationCommand("home", STUDY_ID, token, this::log);
        validateCmd.execute(400);
        assertFalse("Enrollment token validation for " + CLIENT_2_TOKEN_STUDY + " with token '" + token + "' should fail after enrollment succeeds", validateCmd.getSuccess());
    }

    @Test
    public void testAllowDataSharingValidation() throws IOException, CommandException
    {
        // test validation of the "allowDataSharing" parameter at enrollment time
        TokenListPage tokenListPage = TokenListPage.beginAt(this, CLIENT_1_TOKEN_STUDY);
        String token1 = tokenListPage.getToken(1);
        String token2 = tokenListPage.getToken(2);
        String token3 = tokenListPage.getToken(3);

        // test null, blank, and invalid values - all should fail
        EnrollParticipantCommand enrollCmd = new EnrollParticipantCommand("home", STUDY_ID, token1, null, this::log);
        testRequired(enrollCmd, null);
        testRequired(enrollCmd, "");
        testRequired(enrollCmd, "%20%20%20");
        testInvalid(enrollCmd, "na");
        testInvalid(enrollCmd, "n/a");
        testInvalid(enrollCmd, "N/A");
        testInvalid(enrollCmd, "TRUE");
        testInvalid(enrollCmd, "True");
        testInvalid(enrollCmd, "T");
        testInvalid(enrollCmd, "t");
        testInvalid(enrollCmd, "yes");
        testInvalid(enrollCmd, "YES");
        testInvalid(enrollCmd, "1");
        testInvalid(enrollCmd, "FALSE");
        testInvalid(enrollCmd, "False");
        testInvalid(enrollCmd, "F");
        testInvalid(enrollCmd, "f");
        testInvalid(enrollCmd, "no");
        testInvalid(enrollCmd, "NO");
        testInvalid(enrollCmd, "0");
        testInvalid(enrollCmd, "Wombat");
        testInvalid(enrollCmd, "Mazipan");

        // test the three valid values - all should succeed
        testValid(enrollCmd, token1, "true");
        testValid(enrollCmd, token2, "false");
        testValid(enrollCmd, token3, "NA");
    }

    private void testValid(EnrollParticipantCommand enrollCmd, String token, String allowDataSharing) throws IOException, CommandException
    {
        enrollCmd.setBatchToken(token);
        enrollCmd.setAllowDataSharing(allowDataSharing);
        enrollCmd.execute(200);
        assertTrue("Enrollment with token '" + token + "' for " + CLIENT_1_TOKEN_STUDY + " failed when it shouldn't have", enrollCmd.getSuccess());

        log("AppToken: " + enrollCmd.getAppToken());

        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("mobileappstudy", "Participant");
        cmd.setColumns(List.of("allowDataSharing"));
        cmd.addFilter("AppToken", enrollCmd.getAppToken(), Filter.Operator.EQUAL);
        SelectRowsResponse resp = cmd.execute(cn, CLIENT_1_TOKEN_STUDY);

        log(resp.getRows().toString());
    }

    private void testRequired(EnrollParticipantCommand enrollCmd, String allowDataSharing)
    {
        test(enrollCmd, allowDataSharing, "allowDataSharing is required");
    }

    private void testInvalid(EnrollParticipantCommand enrollCmd, String allowDataSharing)
    {
        test(enrollCmd, allowDataSharing, "Invalid allowDataSharing value: '" + allowDataSharing + "'");
    }

    private void test(EnrollParticipantCommand enrollCmd, String allowDataSharing, String expectedMessage)
    {
        enrollCmd.setAllowDataSharing(allowDataSharing);
        enrollCmd.execute(400);
        assertEquals(expectedMessage, enrollCmd.getExceptionMessage());
    }
}
