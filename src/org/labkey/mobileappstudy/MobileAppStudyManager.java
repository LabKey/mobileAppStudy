/*
 * Copyright (c) 2016 LabKey Corporation
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

package org.labkey.mobileappstudy;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.ArrayListMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.RuntimeValidationException;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SubmitterRole;
import org.labkey.api.util.ChecksumUtil;
import org.labkey.api.util.ContainerUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.Pair;
import org.labkey.api.view.NotFoundException;
import org.labkey.mobileappstudy.data.EnrollmentToken;
import org.labkey.mobileappstudy.data.EnrollmentTokenBatch;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.data.Participant;
import org.labkey.mobileappstudy.data.Response;
import org.labkey.mobileappstudy.data.SurveyResponse;
import org.labkey.mobileappstudy.data.SurveyResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MobileAppStudyManager
{
    private static final String TRUNCATED_MESSAGE_SUFFIX =  "... (message truncated)";
    private static final Integer ERROR_MESSAGE_MAX_SIZE = 1000 - TRUNCATED_MESSAGE_SUFFIX.length();
    private static final Integer TOKEN_SIZE = 8;
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final MobileAppStudyManager _instance = new MobileAppStudyManager();
    private static final ChecksumUtil _checksumUtil = new ChecksumUtil(TOKEN_CHARS);

    private static final int THREAD_COUNT = 10; //TODO: Verify this is an appropriate number
    private static JobRunner _shredder;
    private static final Logger logger = Logger.getLogger(MobileAppStudy.class);

    private MobileAppStudyManager()
    {
        if (_shredder == null)
            _shredder = new JobRunner("MobileAppResponseShredder", THREAD_COUNT);
        //Pick up any pending shredder jobs that might have been lost at shutdown/crash/etc
        Collection<SurveyResponse> pendingResponses = getResponsesByStatus(SurveyResponse.ResponseStatus.PENDING);
        if (pendingResponses != null)
        {
            pendingResponses.forEach(response ->
            {
                final Integer rowId = response.getRowId();
                enqueueSurveyResponse(() -> shredSurveyResponse(rowId, null));
            });
        }
    }

    public static MobileAppStudyManager get()
    {
        return _instance;
    }

    /**
     * Create an enrollment token with TOKEN_SIZE characters plus a checksum character
     * @return the generated token.
     */
    public String generateEnrollmentToken()
    {
        String prefix = RandomStringUtils.random(TOKEN_SIZE, TOKEN_CHARS);
        return (prefix + _checksumUtil.getValue(prefix));
    }

    /**
     * Determines if the checksum value included in the token string validates or not
     * @param token string including the checksum character
     * @return true or false to indicate if the checksum validates
     */
    public boolean isChecksumValid(@NotNull String token)
    {
        return _checksumUtil.isValid(token.toUpperCase());
    }

    /**
     * Determines if the token provided is one that is associated with the study short name
     * by the short name.  This does not check the validity of the token itself.
     * @param token the token string
     * @param shortName unique identifier for the study
     * @return true or false to indicate if the given string is associated with the study.
     */
    public boolean isValidStudyToken(@NotNull String token, @NotNull String shortName)
    {
        Container container = getStudyContainer(shortName);
        if (container == null)
            return false;
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        filter.addCondition(FieldKey.fromString("Token"), token.toUpperCase());
        TableSelector selector = new TableSelector(schema.getTableInfoEnrollmentToken(), filter, null);
        return selector.exists();
    }

    /**
     * Determines if the given shortName corresponds to a study that requires an enrollment token to be provided.
     * This will be true if there have been token batches generated for the container that the shortName is associated
     * with.
     * @param shortName unique string identifier for the study
     * @return true if a token is required and false otherwise
     */
    public boolean enrollmentTokenRequired(@NotNull String shortName)
    {
        Container container = getStudyContainer(shortName);
        if (container == null)
            return false;
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        TableSelector selector = new TableSelector(schema.getTableInfoEnrollmentTokenBatch(), filter, null);
        return selector.exists();
    }

    /**
     * Determine if the study identified by the given short name has a participant with the given token
     * @param shortName identifier for the study
     * @param tokenValue identifier for the participant
     * @return true or false depending on existence of participant in the given study
     */
    public boolean hasParticipant(@NotNull String shortName, @NotNull String tokenValue)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        TableInfo participantTable = schema.getTableInfoParticipant();
        TableInfo tokenTable = schema.getTableInfoEnrollmentToken();
        TableInfo studyTable = schema.getTableInfoStudy();

        SQLFragment sql = new SQLFragment("SELECT p.* from ").append(participantTable, "p");
        sql.append(" JOIN ").append(tokenTable, "t").append(" ON t.participantId = p.rowId");
        sql.append(" JOIN ").append(studyTable, "s").append(" ON p.studyId = s.rowId");
        sql.append(" WHERE t.token = ? AND s.shortName = ?");

        SqlSelector selector = new SqlSelector(schema.getSchema(), sql.getSQL(), tokenValue.toUpperCase(), shortName.toUpperCase());
        return selector.exists();
    }

    /**
     * Given a tokenValue that is associated with a study whose short name is provided, adds a new
     * study participant, generates the application token for that participant, and updates the
     * enrollment tokens table to link the token to the participant.
     * @param shortName identifier for the study
     * @param tokenValue the token string being used for enrollment
     * @return an object representing the participant that was created
     */
    public Participant enrollParticipant(@NotNull String shortName, @Nullable String tokenValue)
    {
        DbScope scope = MobileAppStudySchema.getInstance().getSchema().getScope();

        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
            Participant participant = new Participant();
            MobileAppStudy study = getStudy(shortName);
            participant.setStudyId(study.getRowId());
            participant.setAppToken(GUID.makeHash());
            participant.setContainer(study.getContainer());
            participant = Table.insert(null, schema.getTableInfoParticipant(), participant);
            if (tokenValue != null)
            {
                EnrollmentToken eToken = getEnrollmentToken(study.getContainer(), tokenValue);
                if (eToken == null)
                    throw new RuntimeValidationException("Invalid token '" + tokenValue + "' in this container. Participant cannot be enrolled.");

                eToken.setParticipantId(participant.getRowId());
                Table.update(null, schema.getTableInfoEnrollmentToken(), eToken, eToken.getRowId());
            }

            transaction.commit();

            return participant;
        }
    }

    /**
     * Given a container and a token value, return the corresponding enrollment token object.
     * @param container the container in which the token is valid
     * @param tokenValue the value of the token
     * @return object representation of the database record for this token.
     */
    private EnrollmentToken getEnrollmentToken(@NotNull Container container, @NotNull String tokenValue)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        filter.addCondition(FieldKey.fromString("Token"), tokenValue.toUpperCase());
        TableSelector selector = new TableSelector(schema.getTableInfoEnrollmentToken(), filter, null);
        return selector.getObject(EnrollmentToken.class);
    }

    /**
     * Creates a new token batch with a given number of unique tokens
     * @param count number of unique tokens to generate
     * @param user the user making the request for the batch
     * @param container container in which the tokens are being generated
     * @return an enrollment token batch object representing the newly created batch.
     */
    public EnrollmentTokenBatch createTokenBatch(@NotNull Integer count, @NotNull User user, @NotNull Container container)
    {
        DbScope scope = MobileAppStudySchema.getInstance().getSchema().getScope();

        Date createdDate = new Date();
        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            EnrollmentTokenBatch batch = new EnrollmentTokenBatch();
            batch.setCount(count);
            batch.setContainer(container);

            // create the batch entry
            TableInfo batchTable = MobileAppStudySchema.getInstance().getTableInfoEnrollmentTokenBatch();
            batch = Table.insert(user, batchTable, batch);

            // Generate the individual tokens, checking for duplicates
            TableInfo tokenTable = MobileAppStudySchema.getInstance().getTableInfoEnrollmentToken();
            MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
            SqlExecutor executor = new SqlExecutor(schema.getSchema());

            int numTokens = 0;
            SQLFragment sql = new SQLFragment("INSERT INTO " + tokenTable);
            sql.append(" (BatchId, Token, ParticipantId, Created, CreatedBy, Container) ");
            sql.append(" SELECT ?, ?, NULL, ?, ?, ? ");
            sql.append(" WHERE NOT EXISTS (SELECT * FROM ").append(tokenTable, "dup").append(" WHERE dup.Token = ?)");
            while (numTokens < count)
            {
                String token = generateEnrollmentToken();
                numTokens += executor.execute(sql.getSQL(), batch.getRowId(), token, createdDate, user.getUserId(), container, token);
            }

            transaction.commit();
            return batch;
        }
    }

    /**
     * Remove data associated with the given container from the tables for this module
     * @param c container that is being removed
     */
    public void purgeContainer(@NotNull Container c)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        try (DbScope.Transaction transaction = schema.getSchema().getScope().ensureTransaction())
        {
            ContainerUtil.purgeTable(schema.getTableInfoEnrollmentToken(), c, null); //Has a FKs to TokenBatch and Participant tables
            ContainerUtil.purgeTable(schema.getTableInfoResponse(), c, null);   //Has a FK to participant table
            ContainerUtil.purgeTable(schema.getTableInfoParticipant(), c, null); //Has a FK to study table
            ContainerUtil.purgeTable(schema.getTableInfoEnrollmentTokenBatch(), c, null);
            ContainerUtil.purgeTable(schema.getTableInfoStudy(), c, null);

            transaction.commit();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    /**
     * Get the study associated with a given identifier
     * @param shortName identifier for the study
     * @return the associated study, or null if these is no such study
     */
    public MobileAppStudy getStudy(@NotNull String shortName)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("ShortName"), shortName.toUpperCase());
        TableSelector selector = new TableSelector(schema.getTableInfoStudy(), filter, null);
        return selector.getObject(MobileAppStudy.class);
    }


    /**
     * Get the study associated with a given container
     * @param c the container in question
     * @return the associated study, or null if there is no such study
     */
    public MobileAppStudy getStudy(@NotNull Container c)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        SimpleFilter filter = SimpleFilter.createContainerFilter(c);
        TableSelector selector = new TableSelector(schema.getTableInfoStudy(), filter, null);
        return selector.getObject(MobileAppStudy.class);
    }


    /**
     * Get the container associated with a particular study short name
     * @param shortName identifier for the study
     * @return the associated container, or null if there is no such container.
     */
    public Container getStudyContainer(@NotNull String shortName)
    {
        MobileAppStudy study = getStudy(shortName);

        if (study == null)
            return null;

        return study.getContainer();
    }


    /**
     * Given a container, find the associated study short name
     * @param c container in question
     * @return the short name identifier for the study associated with the container or null if there is no such study.
     */
    public String getStudyShortName(@NotNull Container c)
    {
        MobileAppStudy study = getStudy(c);
        return study == null ? null : study.getShortName();
    }

    /**
     * Determines if the study in the given container has any data about participants or not
     * @param c the container in question
     * @return true if there are any participants associated with the given container, false otherwise.
     */
    public boolean hasStudyParticipants(@NotNull Container c)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        SimpleFilter filter = SimpleFilter.createContainerFilter(c);
        TableSelector selector = new TableSelector(schema.getTableInfoParticipant(), Collections.singleton("RowId"), filter, null);
        return selector.exists();
    }

    /**
     * Determines if there is a study with the given short name identifier
     * @param shortName identifier for the study
     * @return true or false, depending on whether the study short name already exists in the database.
     */
    public boolean studyExists(@NotNull String shortName)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();

        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("ShortName"), shortName.toUpperCase());
        TableSelector selector = new TableSelector(schema.getTableInfoStudy(), Collections.singleton("ShortName"), filter, null);
        return selector.exists();
    }

    /**
     * Determines if the given study short name that identifies the study is associated with
     * a container other than the one provided
     * @param shortName identifier for the study
     * @param container current container (the opposite of 'elsewhere')
     * @return true if there is another container that has this short name associated with it; false otherwise
     */
    public boolean studyExistsElsewhere(@NotNull String shortName, @NotNull Container container)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();

        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("ShortName"), shortName.toUpperCase());
        filter.addCondition(FieldKey.fromString("Container"), container, CompareType.NEQ);
        TableSelector selector = new TableSelector(schema.getTableInfoStudy(), Collections.singleton("ShortName"), filter, null);
        return selector.exists();
    }

    /**
     * Updates the study short name for a given study
     * @param study the existing study object
     * @param newShortName the new short name for the study
     * @param user the user making the update request
     * @return the updated study object
     */
    private MobileAppStudy updateStudy(@NotNull MobileAppStudy study, @NotNull String newShortName, boolean collectionEnabled, @NotNull User user)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        TableInfo studyTable = schema.getTableInfoStudy();
        study.setShortName(newShortName.toUpperCase());
        study.setCollectionEnabled(collectionEnabled);

        return Table.update(user, studyTable, study, study.getRowId());
    }

    /**
     * Insert a new study object into the database
     * @param shortName the short name identifier for the study
     * @param container the container to be associated with this short name
     * @param user the user making the insert request
     * @return the object representing the newly inserted study.
     */
    private MobileAppStudy insertStudy(@NotNull String shortName, boolean collectionEnabled, @NotNull Container container, @NotNull User user)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        TableInfo studyTable = schema.getTableInfoStudy();

        MobileAppStudy study = new MobileAppStudy();
        study.setShortName(shortName.toUpperCase());
        study.setCollectionEnabled(collectionEnabled);
        study.setContainer(container);
        return Table.insert(user, studyTable, study);
    }

    /**
     * Method to update or insert a study, depending on whether the study already exists or not.
     * @param shortName the short name identifier for the study
     * @param container the container associated with the study
     * @param user the user making the request
     * @return the object representing the newly updated or inserted study.
     */
    public MobileAppStudy insertOrUpdateStudy(@NotNull String shortName, boolean collectionEnabled, @NotNull Container container, @NotNull User user)
    {
        MobileAppStudy study = getStudy(container);
        if (study == null)
            return insertStudy(shortName, collectionEnabled, container, user);
        else
            return updateStudy(study, shortName, collectionEnabled, user);
    }

    /**
     * Add response processing job to queue
     * @param run Runnable to add to processing queue
     */
    void enqueueSurveyResponse(@NotNull Runnable run)
    {
        _shredder.execute(run);
    }

    /**
     * Method to process Survey Responses
     * @param rowId mobileappstudy.Response.RowId to process
     * @param user the user initiating the shredding request
     */
    void shredSurveyResponse(@NotNull Integer rowId, @Nullable User user)
    {
        SurveyResponse surveyResponse = getResponse(rowId);

        if (surveyResponse != null)
        {
            logger.info(String.format("Processing response " + rowId + " in container " + surveyResponse.getContainer().getName()));
            MobileAppStudyManager manager = MobileAppStudyManager.get();
            try
            {
                manager.store(surveyResponse, rowId, user);
                manager.updateProcessingStatus(user, rowId, SurveyResponse.ResponseStatus.PROCESSED);
                logger.info(String.format("Processed response " + rowId + " in container " + surveyResponse.getContainer().getName()));
            }
            catch (Exception e)
            {
                logger.error("Error processing response " + rowId + " in container " + surveyResponse.getContainer().getName(), e);
                manager.updateProcessingStatus(user, rowId, SurveyResponse.ResponseStatus.ERROR, e instanceof NullPointerException ? "NullPointerException" : e.getMessage());
            }
        }
        else
        {
            logger.error("No response found for id " + rowId);
        }
    }

    /**
     * Get Response from DB
     * @param rowId mobileappstudy.Response.RowId to retrieve
     * @return SurveyResponse object representing row, or null if not found
     */
    @Nullable
    SurveyResponse getResponse(@NotNull Integer rowId)
    {
        FieldKey fkey = FieldKey.fromParts("rowId");
        SimpleFilter filter = new SimpleFilter(fkey, rowId);

        return new TableSelector(MobileAppStudySchema.getInstance().getTableInfoResponse(), filter, null)
                .getObject(SurveyResponse.class);
    }

    Collection<SurveyResponse> getResponsesByStatus(SurveyResponse.ResponseStatus status)
    {
        FieldKey fkey = FieldKey.fromParts("Status");
        SimpleFilter filter = new SimpleFilter(fkey, status.getPkId());

        return new TableSelector(MobileAppStudySchema.getInstance().getTableInfoResponse(), filter, null)
                .getCollection(SurveyResponse.class);
    }



    /**
     * Verify if survey exists
     * @param surveyId to verify
     * @param container holding study/survey
     * @param user executing query
     * @return true if survey found
     */
    boolean surveyExists(String surveyId, Container container, User user)
    {
        try
        {
            getResultTable(surveyId, container, user);
            return true;
        }
        catch (NotFoundException e)
        {
            return false;
        }
    }

    /**
     * Verify if appToken is assigned to participant
     * @param appToken to look up
     * @return true if apptoken found in mobilestudy.Participant.appToken
     */
    boolean participantExists(String appToken)
    {
        return getParticipantFromAppToken(appToken) != null;
    }

    /**
     * Retrieve the participant record associated with the supplied appToken
     * @param appToken to lookup
     * @return Participant if found, null if not
     */
    @Nullable
    Participant getParticipantFromAppToken(String appToken)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        FieldKey pKey = FieldKey.fromParts("apptoken");

        SimpleFilter filter = new SimpleFilter(pKey, appToken);
        return new TableSelector(schema.getTableInfoParticipant(), filter, null).getObject(Participant.class);
    }

    /**
     * Insert new row into the mobileappstudy.Response table
     * @param resp to insert
     * @return updated object representing row
     */
    @NotNull
    SurveyResponse insertResponse(@NotNull SurveyResponse resp)
    {
        Participant participant = getParticipantFromAppToken(resp.getAppToken());
        if (participant == null)
            throw new IllegalStateException("Could not insert Response, Participant associated with appToken not found");

        resp.setContainer(participant.getContainer());
        resp.setParticipantId(participant.getRowId());

        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        TableInfo responseTable = schema.getTableInfoResponse();
        return Table.insert(null, responseTable, resp);
    }

    /**
     * Retrieve the study associated to an appToken via the participant
     * @param appToken to lookup
     * @return MobileAppStudy object, will return null if participant or study not found
     */
    @Nullable
    MobileAppStudy getStudyFromAppToken(String appToken)
    {
        Participant participant = getParticipantFromAppToken(appToken);
        if (participant == null)
            return null;
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("rowId"), participant.getStudyId());
        return new TableSelector(MobileAppStudySchema.getInstance().getTableInfoStudy(),  filter, null).getObject(MobileAppStudy.class);
    }

    public int reprocessResponses(User user, @NotNull Set<Integer> listIds)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("rowId"), listIds, CompareType.IN);
        Collection<SurveyResponse> responses = new TableSelector(
                MobileAppStudySchema.getInstance().getTableInfoResponse(), filter, null).getCollection(SurveyResponse.class);

        responses.forEach(response ->
        {
            updateProcessingStatus(user, response.getRowId(), SurveyResponse.ResponseStatus.PENDING);
            enqueueSurveyResponse(() -> shredSurveyResponse(response.getRowId(), user));
        });

        return responses.size();
    }

    public void updateProcessingStatus(@Nullable User user, @NotNull Integer rowId, @NotNull SurveyResponse.ResponseStatus newStatus)
    {
        updateProcessingStatus(user, rowId, newStatus, null);
    }

    public void updateProcessingStatus(@Nullable User user, @NotNull Integer rowId, @NotNull SurveyResponse.ResponseStatus newStatus, @Nullable String errorMessage)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        TableInfo responseTable = schema.getTableInfoResponse();
        SurveyResponse response = new TableSelector(responseTable).getObject(rowId, SurveyResponse.class);

        response.setStatus(newStatus);
        if (errorMessage != null)
            response.setErrorMessage(errorMessage.length() > ERROR_MESSAGE_MAX_SIZE ? errorMessage.substring(0, ERROR_MESSAGE_MAX_SIZE) + TRUNCATED_MESSAGE_SUFFIX : errorMessage);
        // we currently have only start and end statuses, so we can safely set the processed and processedBy
        // fields at this point.
        response.setProcessed(new Date());
        if (user != null && !user.isGuest())
            response.setProcessedBy(user);

        Table.update(user, responseTable, response, response.getRowId());
    }

    public String getNonErrorResponses(Set<Integer> listIds)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("rowId"), listIds, CompareType.IN);
        filter.addCondition(FieldKey.fromParts("status"), SurveyResponse.ResponseStatus.ERROR.getPkId(), CompareType.NEQ);
        Collection<SurveyResponse> responses = new TableSelector(
                MobileAppStudySchema.getInstance().getTableInfoResponse(), filter, null).getCollection(SurveyResponse.class);

        return String.join(", ", responses.stream().map((response) -> response.getRowId().toString()).collect(Collectors.toList()));
    }

    /**
     * Stores the survey results of this object into their respective lists
     * @param surveyResponse the response to be stored
     * @param responseBlobId rowId of the response table
     *@param user the user initiating the store request  @throws Exception if there was a problem storing the results in one or more of its lists, in which case none of the lists will be updated
     */
    public void store(@NotNull SurveyResponse surveyResponse, Integer responseBlobId, @Nullable User user) throws Exception
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        DbScope scope = schema.getSchema().getScope();

        List<String> errors = new ArrayList<>();

        // if a user isn't provided, need to create a LimitedUser to use for checking permissions, wrapping the Guest user
        User insertUser = new LimitedUser((user == null)? UserManager.getGuestUser() : user,
                new int[0], Collections.singleton(RoleManager.getRole(SubmitterRole.class)), false);

        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            Response response = Response.getResponseObject(surveyResponse.getResponse());
            storeSurveyResult(response, surveyResponse.getSurveyId(), surveyResponse.getParticipantId(), responseBlobId, response.getResults(), errors, surveyResponse.getContainer(), insertUser);
            if (!errors.isEmpty())
                throw new Exception("Problem storing data in list '" + surveyResponse.getSurveyId() + "' in container '" + surveyResponse.getContainer().getName() + "'.\n" + StringUtils.join(errors, "\n"));
            else
                transaction.commit();
        }
    }

    /**
     * Store a SurveyResult in a given list and then recursively store its multi-valued results in their respective lists
     *
     * @param response the response whose results are being stored
     * @param listName name of the list to store data in
     * @param participantId identifier of the participant corresponding to this survey
     * @param responseBlobId rowId to the mobileappstudy.response table
     * @param results the collection of results for this survey result
     * @param errors the collection of errors encountered while storing this survey result
     * @param container the container in which the list lives
     * @param user the user who will store the data     @throws Exception if there are problems storing the data.
     */
    private void storeSurveyResult(@NotNull Response response, @NotNull String listName, @NotNull Integer participantId, @NotNull Integer responseBlobId, @Nullable List<SurveyResult> results, @NotNull List<String> errors, @NotNull Container container, @NotNull User user) throws Exception
    {
        if (results == null)
        {
            errors.add("No results provided in response.");
            return;
        }
        // initialize the data map with the survey result values
        Map<String, Object> data = new ArrayListMap<>();
        data.put("startTime", response.getStartTime());
        data.put("endTime", response.getEndTime());
        data.put("participantId", participantId);
        data.put("responseId", responseBlobId);

        Map<String, Object> row = storeListResults(null, listName, results, data, errors, container, user);
        if (!row.isEmpty())
        {
            Integer surveyId = (Integer) row.get("Key");
            Pair<String, Integer> rowKey = new Pair<>("SurveyId", surveyId);
            List<SurveyResult> multiValuedResults = getMultiValuedResults(listName, results);
            storeMultiValuedResults(multiValuedResults, surveyId, rowKey, errors, container, user);
        }
    }

    /**
     * Extract the subset of results that are single-valued and validate that these fields exist and are of the proper type in the given list
     * @param list the table where the single-valued results are to be stored; used for validating column names and types
     * @param results the results to be filtered
     * @param errors a collection of validation errors
     * @return the filtered set of results
     */
    private List<SurveyResult> getSingleValuedResults(@NotNull TableInfo list, @NotNull List<SurveyResult> results, @NotNull List<String> errors)
    {
        List<SurveyResult> singleValuedResults = new ArrayList<>();
        results.stream().filter(result -> result.getValueType().isSingleValued()).forEach(result ->
        {
            result.setListName(list.getName());
            if (validateListColumn(list, result.getIdentifier(), result.getValueType(), errors))
            {
                singleValuedResults.add(result);
            }
        });

        return singleValuedResults;
    }

    /**
     * Find the set of results in a list that are multi-valued responses
     * @param baseListName name of the containing response element
     * @param results the set of responses that have more than a single value
     * @return the subset of results that are multi-valued
     */
    private List<SurveyResult> getMultiValuedResults(@NotNull String baseListName, @NotNull List<SurveyResult> results)
    {
        List<SurveyResult> multiValuedResults = new ArrayList<>();
        for (SurveyResult result : results)
        {
            if (!result.getValueType().isSingleValued())
            {
                result.setListName(baseListName + StringUtils.capitalize(result.getIdentifier()));
                multiValuedResults.add(result);
            }
        }

        return multiValuedResults;
    }

    /**
     * Gets a table for a given list name in a given container
     * @param listName name of the list whose table is to be returned
     * @param container container for the list
     * @param user user for retrieving the table from the list
     * @return the table
     * @throws NotFoundException if the list or table info cannot be found
     */
    private TableInfo getResultTable(@NotNull String listName, @NotNull Container container, @Nullable User user) throws NotFoundException
    {
        ListDefinition listDef = ListService.get().getList(container, listName);
        if (listDef == null)
            throw new NotFoundException("Invalid list '" + listName + "' for container '" + container.getName() + "'");

        TableInfo resultTable = listDef.getTable(user, container);
        if (resultTable == null)
            throw new NotFoundException("Unable to find table for list '" + listDef.getName() + "' in container '" + container.getName() + "'");

        return resultTable;
    }

    /**
     * Determine if a column of the appropriate type is available in the given table
     * @param table the table in question
     * @param columnName name of the column
     * @param resultValueType the type of the column we expect
     * @param errors collection of validation errors accumulated thus far
     * @return true if the expected column with the expected type is found; false otherwise
     */
    private boolean validateListColumn(@NotNull TableInfo table, @NotNull String columnName, @NotNull SurveyResult.ValueType resultValueType, @NotNull List<String> errors)
    {
        ColumnInfo column = table.getColumn(columnName);
        if (column == null)
        {
            errors.add("Unable to find column '" + columnName + "' in list '" + table.getName() + "'");
        }
        else if (column.getJdbcType() != resultValueType.getJdbcType())
        {
            errors.add("Type '" + resultValueType.getTypeName() + "' (" + resultValueType.getJdbcType() + ") of result '" + columnName + "' does not match expected type (" + column.getJdbcType() + ")");
        }

        return errors.isEmpty();
    }

    /**
     * Inserts a row for the given list table to add the data provided
     * @param table the list table in which data is to be stored
     * @param data the collection of data elements for the new row
     * @param container the container in which the list (table) lives
     * @param user the user inserting data into the list
     * @return the newly created row
     * @throws Exception if the table has no update service or there is any other problem inserting the new row
     */
    private Map<String, Object> storeListData(@NotNull TableInfo table, @NotNull Map<String, Object> data, @NotNull Container container, @Nullable User user) throws Exception
    {
        // Add an entry to the survey list and get the id.
        BatchValidationException exception = new BatchValidationException();

        if (table.getUpdateService() == null)
            throw new NotFoundException("Unable to get update service for table " + table.getName());
        List<Map<String, Object>> rows = table.getUpdateService().insertRows(user, container, Collections.singletonList(data), exception, null, null);
        if (exception.hasErrors())
            throw exception;
        else
            return rows.get(0);
    }

    /**
     * Stores a set of SurveyResult objects in a given list. For each given SurveyResult that is single-valued, store it
     * in the column with the corresponding name.

     * @param surveyId identifier of the survey
     * @param listName name of the list in which the responses should be stored
     * @param results the superset of results to be stored.  This may contain multi-valued results as well, but these
     *                will not be handled in this method
     * @param data contains the initial set of data to be stored in the row (primarily the parent key field).
     * @param errors the set of errors accumulated thus far, which will be appended with errors encountered for storing these results
     * @param container the container in which the list lives
     * @param user the user to do the insert
     * @return the newly created list row
     * @throws Exception if there is a problem finding or updating the appropriate lists
     */
    private Map<String, Object> storeListResults(@Nullable Integer surveyId, @NotNull String listName, @NotNull List<SurveyResult> results, @NotNull Map<String, Object> data, @NotNull List<String> errors, @NotNull Container container, @NotNull User user) throws Exception
    {
        TableInfo surveyTable = getResultTable(listName, container, user);
        if (surveyTable.getUpdateService() == null)
        {
            errors.add("No update service available for the given survey table: " + listName);
            return Collections.emptyMap();
        }

        // find all the single-value results, check if they are in the list, check the type, and add them to the data map if everything is good
        List<SurveyResult> singleValuedResults = getSingleValuedResults(surveyTable, results, errors);
        if (!errors.isEmpty())
            return Collections.emptyMap();

        for (SurveyResult result: singleValuedResults)
            data.put(result.getIdentifier(), result.getValue());
        Map<String, Object> row = storeListData(surveyTable, data, container, user);
        if (surveyId == null)
            surveyId = (Integer) row.get("Key");

        // Add a resultMetadata row for each of the individual rows using the given surveyId
        storeResponseMetadata(singleValuedResults, surveyId, container, user);

        return row;
    }

    /**
     * Recursively stores a set of multi-valued results
     * @param results the set of multi-valued results to be stored
     * @param surveyId identifier for the survey being processed
     * @param parentKey the key for the list that these multi-valued results are associated with
     * @param errors the collection of validation errors encountered thus far
     * @param container container for the lists
     * @param user user to do the inserts
     * @throws Exception if there is a problem finding or updating the appropriate lists
     */
    private void storeMultiValuedResults(@NotNull List<SurveyResult> results, @NotNull Integer surveyId, @NotNull Pair<String, Integer> parentKey, @NotNull List<String> errors, @NotNull Container container, @NotNull User user) throws Exception
    {
        for (SurveyResult result : results)
        {
            if (result.getValueType() == SurveyResult.ValueType.CHOICE)
            {
                storeResultChoices(result, surveyId, parentKey, errors, container, user);
            }
            else // result is of type GROUPED_RESULT
            {
                if (result.getSkipped())
                    storeResponseMetadata(Collections.singletonList(result), surveyId, container, user);
                else
                {
                    // two scenarios, groupedResult is an array of SurveyResult objects or is an array of an array of SurveyResult objects
                    List<List<SurveyResult>> groupedResultList = new ArrayList<>();
                    for (Object gr : (ArrayList) result.getValue())
                    {
                        if (gr instanceof SurveyResult) // this means we have a single set of grouped results to process.
                        {
                            groupedResultList.add((ArrayList) result.getValue());
                            break;
                        }
                        else
                        {
                            groupedResultList.add((ArrayList) gr);
                        }
                    }

                    // store the data for each of the group result sets
                    for (List<SurveyResult> groupResults : groupedResultList)
                    {
                        String listName = result.getListName();
                        Map<String, Object> data = new ArrayListMap<>();
                        data.put(parentKey.getKey(), parentKey.getValue());
                        Map<String, Object> row = storeListResults(surveyId, listName, groupResults, data, errors, container, user);
                        if (!row.isEmpty())
                        {
                            Pair<String, Integer> rowKey = new Pair<>(result.getIdentifier() + "Id", (Integer) row.get("Key"));
                            List<SurveyResult> multiValuedResults = getMultiValuedResults(listName, groupResults);
                            storeMultiValuedResults(multiValuedResults, surveyId, rowKey, errors, container, user);
                        }
                    }
                }
            }
        }
    }

    /**
     * Stores a set of values for a choice response
     * @param result the result whose values are being stored
     * @param surveyId identifier for the survey being processed
     * @param parentKey the key for the list to which the choice will be associated
     * @param errors the set of validation errors encountered thus far
     * @param container the container for the lists
     * @param user user to do the inserts
     * @throws Exception if there is a problem finding or updating the appropriate lists
     */
    private void storeResultChoices(@NotNull SurveyResult result, @NotNull Integer surveyId, @NotNull Pair<String, Integer> parentKey, @NotNull List<String> errors, @NotNull Container container, @NotNull User user) throws Exception
    {
        if (result.getSkipped()) // store only metadata if the response was skipped
            storeResponseMetadata(Collections.singletonList(result), surveyId, container, user);
        else
        {
            TableInfo table = getResultTable(result.getListName(), container, user);
            validateListColumn(table, result.getIdentifier(), SurveyResult.ValueType.STRING, errors);

            if (errors.isEmpty())
            {
                if (result.getValue() != null)
                {
                    for (Object value : (ArrayList) result.getValue())
                    {
                        Map<String, Object> data = new ArrayListMap<>();
                        data.put(parentKey.getKey(), parentKey.getValue());
                        data.put(result.getIdentifier(), value);
                        storeListData(table, data, container, user);
                    }
                }

                storeResponseMetadata(Collections.singletonList(result), surveyId, container, user);
            }
        }
    }

    /**
     * Stores the response metadata for the given results
     * @param results the results whose metadata is to be stored
     * @param surveyId the identifier of the survey whose responses are being stored
     * @param container the container in which the lists live
     * @param user the user to be used for inserting the data
     */
    private void storeResponseMetadata(@NotNull List<SurveyResult> results, @NotNull Integer surveyId, @NotNull Container container, @NotNull User user)
    {
        for (SurveyResult result : results)
        {

            MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
            TableInfo responseMetadataTable = schema.getTableInfoResponseMetadata();
            result.setSurveyId(surveyId);
            result.setContainer(container);
            result.setFieldName(result.getIdentifier());

            Table.insert(user, responseMetadataTable, result);
        }
    }
}