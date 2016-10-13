/*
 * Copyright (c) 2015 LabKey Corporation
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

package org.labkey.mobileappsurvey;

import org.apache.commons.lang3.RandomStringUtils;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.security.User;
import org.labkey.api.util.ChecksumUtil;
import org.labkey.api.util.ContainerUtil;
import org.labkey.mobileappsurvey.data.EnrollmentTokenBatch;

import java.util.Date;

public class MobileAppSurveyManager
{
    private static final Integer TOKEN_SIZE = 8;
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final MobileAppSurveyManager _instance = new MobileAppSurveyManager();
    private static final ChecksumUtil _checksumUtil = new ChecksumUtil(TOKEN_CHARS);

    private MobileAppSurveyManager()
    {
        // prevent external construction with a private default constructor
    }

    public static MobileAppSurveyManager get()
    {
        return _instance;
    }

    public String generateEnrollmentToken()
    {
        String prefix = RandomStringUtils.random(TOKEN_SIZE, TOKEN_CHARS);
        return (prefix + _checksumUtil.getValue(prefix));
    }

    public Integer insertNewTokenBatch(Integer count, User user, Container container)
    {
        DbScope scope = MobileAppSurveySchema.getInstance().getSchema().getScope();

        Date createdDate = new Date();
        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            EnrollmentTokenBatch batch = new EnrollmentTokenBatch();
            batch.setCount(count);
            batch.setContainer(container);
            batch.setCreatedBy(user);
            batch.setCreated(createdDate);

            // create the batch entry
            TableInfo batchTable = MobileAppSurveySchema.getInstance().getTableInfoEnrollmentTokenBatch();
            batch = Table.insert(user, batchTable, batch);

            // Generate the individual tokens, checking for duplicates
            TableInfo tokenTable = MobileAppSurveySchema.getInstance().getTableInfoEnrollmentToken();
            MobileAppSurveySchema schema = MobileAppSurveySchema.getInstance();
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
            return batch.getRowId();
        }
    }

    public static void purgeContainer(Container c, User user)
    {
        MobileAppSurveySchema schema = MobileAppSurveySchema.getInstance();
        try (DbScope.Transaction transaction = schema.getSchema().getScope().ensureTransaction())
        {
            ContainerUtil.purgeTable(schema.getTableInfoStudy(), c, null);
            ContainerUtil.purgeTable(schema.getTableInfoParticipant(), c, null);
            ContainerUtil.purgeTable(schema.getTableInfoEnrollmentToken(), c, null);
            ContainerUtil.purgeTable(schema.getTableInfoEnrollmentTokenBatch(), c, null);

            transaction.commit();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}