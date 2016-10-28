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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.RuntimeValidationException;
import org.labkey.api.security.User;
import org.labkey.api.util.ChecksumUtil;
import org.labkey.api.util.ContainerUtil;
import org.labkey.api.util.GUID;
import org.labkey.mobileappstudy.data.EnrollmentToken;
import org.labkey.mobileappstudy.data.EnrollmentTokenBatch;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.data.Participant;

import java.util.Collections;
import java.util.Date;

public class MobileAppStudyManager
{
    private static final Integer TOKEN_SIZE = 8;
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final MobileAppStudyManager _instance = new MobileAppStudyManager();
    private static final ChecksumUtil _checksumUtil = new ChecksumUtil(TOKEN_CHARS);

    private MobileAppStudyManager()
    {
        // prevent external construction with a private default constructor
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
    public static void purgeContainer(@NotNull Container c)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        try (DbScope.Transaction transaction = schema.getSchema().getScope().ensureTransaction())
        {
            ContainerUtil.purgeTable(schema.getTableInfoEnrollmentToken(), c, null);
            ContainerUtil.purgeTable(schema.getTableInfoEnrollmentTokenBatch(), c, null);
            ContainerUtil.purgeTable(schema.getTableInfoParticipant(), c, null);
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
    private MobileAppStudy updateShortName(@NotNull MobileAppStudy study, @NotNull String newShortName, @NotNull User user)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();

        TableInfo studyTable = schema.getTableInfoStudy();

        study.setShortName(newShortName.toUpperCase());

        return Table.update(user, studyTable, study, study.getRowId());
    }

    /**
     * Insert a new study object into the database
     * @param shortName the short name identifier for the study
     * @param container the container to be associated with this short name
     * @param user the user making the insert request
     * @return the object representing the newly inserted study.
     */
    private MobileAppStudy insertStudy(@NotNull String shortName, @NotNull Container container, @NotNull User user)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();

        TableInfo studyTable = schema.getTableInfoStudy();

        MobileAppStudy study = new MobileAppStudy();
        study.setShortName(shortName.toUpperCase());
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
    public MobileAppStudy insertOrUpdateStudy(@NotNull String shortName, @NotNull Container container, @NotNull User user)
    {
        MobileAppStudy study = getStudy(container);
        if (study == null)
            return insertStudy(shortName, container, user);
        else
            return updateShortName(study, shortName, user);
    }

}