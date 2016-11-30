package org.labkey.mobileappstudy.data;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.ArrayListMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SubmitterRole;
import org.labkey.mobileappstudy.MobileAppStudySchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by susanh on 11/28/16.
 */
public class Response
{
    private Date startTime;
    private Date endTime;
    private ArrayList<SurveyResult> results;

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public void setStart(Date start)
    {
        this.startTime = start;
    }

    public void setEnd(Date end)
    {
        this.endTime = end;
    }

    public ArrayList<SurveyResult> getResults()
    {
        return results;
    }

    public void setResults(ArrayList<SurveyResult> results)
    {
        this.results = results;
    }

    public void store(@Nullable User user, @NotNull Container container, @NotNull ListDefinition listDef, @NotNull Integer participantId) throws Exception
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        DbScope scope = schema.getSchema().getScope();

        TableInfo surveyTable = listDef.getTable(user, container);
        if (surveyTable == null)
            throw new Exception("Unable to find table for list '" + listDef.getName() + "' in container '" + listDef.getContainer().getName() + "'");

        List<String> errors = new ArrayList<>();
        Map<String, Object> data = new ArrayListMap<>();

        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            data.put("startTime", getStartTime());
            data.put("endTime", getEndTime());
            data.put("participantId", participantId);

            // find all the single-value results, check if they are in the list, check the type, and add them to the
            // map if everything is good
            List<SurveyResult> multiValuedResults = new ArrayList<>();
            List<SurveyResult> singleValuedResults = new ArrayList<>();
            for (SurveyResult result : getResults())
            {
                if (result.getValueType().isSingleValued())
                {
                    ColumnInfo column = surveyTable.getColumn(result.getIdentifier());
                    if (column == null)
                        errors.add("Unable to find column '" + result.getIdentifier() + "' in list '" + listDef.getName() + "' in container '" + listDef.getContainer().getName() + "'");
                    else if (column.getJdbcType() != result.getValueType().getJdbcType())
                    {
                        errors.add("Type '" + result.getType() + "' (" + result.getValueType().getJdbcType() + ") of result '" + result.getIdentifier() + "' does not match expected type (" + column.getJdbcType() + ")");
                    }
                    else
                    {
                        result.setListName(listDef.getName());
                        singleValuedResults.add(result);

                        data.put(result.getIdentifier(), result.getResult());
                    }
                }
                else
                {
                    multiValuedResults.add(result);
                }
            }

            if (surveyTable.getUpdateService() != null)
            {
                // Add an entry to the survey list and get the id.
                BatchValidationException exception = new BatchValidationException();
                // may need to create a LimitedUser to use for checking permissions, wrapping the Guest user
                User insertUser = new LimitedUser(UserManager.getGuestUser(), new int[0], Collections.singleton(RoleManager.getRole(SubmitterRole.class)), false);
                List<Map<String, Object>> rows = surveyTable.getUpdateService().insertRows(insertUser, container, Collections.singletonList(data), exception, null, null);
                if (exception.hasErrors())
                    errors.add(exception.getMessage());
                if (errors.isEmpty())
                {
                    // Add a resultMetadata row for each of the individual rows using the given surveyId
                    storeResultMetadata(user, container, singleValuedResults, (Integer) rows.get(0).get("Key"));
                    // TODO Process the multi-valued rows
                }
            }

            if (!errors.isEmpty())
                throw new Exception("Problem storing data to list '" + listDef.getName() + "' in container '" + listDef.getContainer().getName() + "'.\n" + StringUtils.join(errors, "\n"));
            else
                transaction.commit();
        }
    }

    private void storeResultMetadata(@Nullable User user, Container container, List<SurveyResult> results, Integer surveyId)
    {
        for (SurveyResult result : results)
        {
            MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
            TableInfo responseMetadataTable = schema.getTableInfoResponseMetadata();
            result.setSurveyId(surveyId);
            result.setContainer(container);
            result.setQuestionId(result.getIdentifier());


            Table.insert(user, responseMetadataTable, result);
        }
    }
}
