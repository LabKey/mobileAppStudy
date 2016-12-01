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
import org.labkey.api.exp.list.ListService;
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
    private Date _startTime;
    private Date _endTime;
    private List<SurveyResult> _results;

    public Date getStartTime()
    {
        return _startTime;
    }

    public void setStartTime(Date startTime)
    {
        this._startTime = startTime;
    }

    public Date getEndTime()
    {
        return _endTime;
    }

    public void setEndTime(Date endTime)
    {
        this._endTime = endTime;
    }

    public void setStart(Date start)
    {
        this._startTime = start;
    }

    public void setEnd(Date end)
    {
        this._endTime = end;
    }

    public List<SurveyResult> getResults()
    {
        return _results;
    }

    public List<SurveyResult> getSingleValuedResults()
    {
        List<SurveyResult> results = new ArrayList<>();
        for (SurveyResult result : getResults())
        {
            if (result.getValueType().isSingleValued())
                results.add(result);
        }
        return results;
    }

    public List<SurveyResult> getMultiValuedResults()
    {
        List<SurveyResult> results = new ArrayList<>();
        for (SurveyResult result : getResults())
        {
            if (!result.getValueType().isSingleValued())
                results.add(result);
        }
        return results;
    }

    public void setResults(List<SurveyResult> results)
    {
        this._results = results;
    }

    public void store(@Nullable User user, @NotNull Container container, @NotNull String listName, @NotNull Integer participantId) throws Exception
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        DbScope scope = schema.getSchema().getScope();

        List<String> errors = new ArrayList<>();

        try (DbScope.Transaction transaction = scope.ensureTransaction())
        {
            storeSurveyResult(listName, participantId, getResults(), errors, container, user);
            if (!errors.isEmpty())
                throw new Exception("Problem storing data to list '" + listName + "' in container '" + container.getName() + "'.\n" + StringUtils.join(errors, "\n"));
            else
                transaction.commit();
        }
    }

    private Map<String, Object> storeSurveyResult(String listName, Integer participantId, List<SurveyResult> results, List<String> errors, Container container, User user) throws Exception
    {
        TableInfo surveyTable = getResultTable(listName, container, user);

        Map<String, Object> data = new ArrayListMap<>();
        data.put("startTime", getStartTime());
        data.put("endTime", getEndTime());
        data.put("participantId", participantId);

        // find all the single-value results, check if they are in the list, check the type, and add them to the
        // map if everything is good
        List<SurveyResult> multiValuedResults = new ArrayList<>();
        List<SurveyResult> singleValuedResults = new ArrayList<>();

        for (SurveyResult result : results)
        {
            if (result.getValueType().isSingleValued())
            {
                result.setListName(listName);
                if (validateListColumn(container, surveyTable, result.getIdentifier(), errors, result.getValueType()))
                {
                    singleValuedResults.add(result);

                    data.put(result.getIdentifier(), result.getResult());
                }
            }
            else
            {
                result.setListName(listName + StringUtils.capitalize(result.getIdentifier()));
                multiValuedResults.add(result);
            }
        }

        Map<String, Object> row = Collections.emptyMap();
        if (surveyTable.getUpdateService() != null && errors.isEmpty())
        {
            row = storeListData(surveyTable, data, container, user);
            if (errors.isEmpty())
            {
                Integer surveyId = (Integer) row.get("Key");

                storeMultiValuedResults(multiValuedResults, surveyId, participantId, user, container, errors);
                // Add a resultMetadata row for each of the individual rows using the given surveyId
                storeResultMetadata(user, container, singleValuedResults, surveyId);
                // Add a resultMetadata row for each of the multi-valued results as well
                storeResultMetadata(user, container, multiValuedResults, surveyId);
            }
        }
        if (row.isEmpty())
            return Collections.emptyMap();
        else
            return row;

    }

    private TableInfo getResultTable(String listName, Container container, User user) throws Exception
    {
        ListDefinition listDef = ListService.get().getList(container, listName);
        if (listDef == null)
        {
            throw new Exception("Invalid list '" + listName + "' for container '" + container.getName() + "'");
        }
        TableInfo resultTable = listDef.getTable(user, container);
        if (resultTable == null)
            throw new Exception("Unable to find table for list '" + listDef.getName() + "' in container '" + container.getName() + "'");
        return resultTable;
    }

    private boolean validateListColumn(Container container, TableInfo table, String columnName, List<String> errors, SurveyResult.ValueType resultValueType) throws Exception
    {
        ColumnInfo column = table.getColumn(columnName);
        if (column == null)
        {
            errors.add("Unable to find column '" + columnName + "' in list '" + table.getName() + "' in container '" + container.getName() + "'");
            return false;
        }
        else if (column.getJdbcType() != resultValueType.getJdbcType())
        {
            errors.add("Type '" + resultValueType + "' (" + resultValueType.getJdbcType() + ") of result '" + columnName + "' does not match expected type (" + column.getJdbcType() + ")");
            return false;
        }
        return true;
    }

    private Map<String, Object> storeListData(TableInfo table, Map<String, Object> data, Container container, User user) throws Exception
    {
        // Add an entry to the survey list and get the id.
        BatchValidationException exception = new BatchValidationException();
        // need to create a LimitedUser to use for checking permissions, wrapping the Guest user
        User insertUser = new LimitedUser((user == null)? UserManager.getGuestUser() : user,
                new int[0], Collections.singleton(RoleManager.getRole(SubmitterRole.class)), false);

        if (table.getUpdateService() == null)
            throw new Exception("Unable to get update service for table " + table.getName());
        List<Map<String, Object>> rows = table.getUpdateService().insertRows(insertUser, container, Collections.singletonList(data), exception, null, null);
        if (exception.hasErrors())
            throw exception;
        else
            return rows.get(0);
    }

    private void storeMultiValuedResults(List<SurveyResult> results, Integer surveyId, Integer participantId, User user, Container container, List<String> errors) throws Exception
    {
        for (SurveyResult result : results)
        {
            if (result.getValueType() == SurveyResult.ValueType.CHOICE)
            {
                TableInfo table = getResultTable(result.getListName(), container, user);
                validateListColumn(container, table, result.getIdentifier(), errors, SurveyResult.ValueType.STRING);

                if (errors.isEmpty())
                {
                    for (Object value : (ArrayList) result.getResult())
                    {
                        Map<String, Object> data = new ArrayListMap<>();
                        data.put("surveyId", surveyId);
                        data.put(result.getIdentifier(), value);

                        storeListData(table, data, container, user);
                    }
                    storeResultMetadata(user, container, Collections.singletonList(result), surveyId);
                }
            }
//            else // result is of type GROUPED_RESULT
//            {
//                // a grouped result is an array of SurveyResult objects, but there may be multiple of these, so we check which case we have
//                for (Object gr : (ArrayList) result.getValue())
//                {
//                    if (gr instanceof SurveyResult) // this means we have a single set of grouped results to process.
//                    {
//                        SurveyResult groupedResult = (SurveyResult) gr;
//                        Map<String, Object> data = new ArrayListMap<>();
//                        data.put("surveyId", surveyId);
//                        if (groupedResult.getValueType().isSingleValued())
//                        {
//                            if (validateListColumn(container, surveyTable, result.getIdentifier(), errors, result.getValueType()))
//                            {
//                                data.put(groupedResult.getIdentifier(), result.getResult());
//                            }
//                        }
//                        else
//                        {
//                            result.setListName(listName + StringUtils.capitalize(result.getIdentifier()));
//                            multiValuedResults.add(result);
//                        }
//                        // for multi-valued results, look for/create separate tables
//                    }
//                    else // (groupedResult instanceof ArrayList)
//                    {
//                        for (Object groupedResultItem : (ArrayList) gr)
//                        {
//
//                        }
//                    }
//                }
//            }
        }
    }

    private void storeSurveyResultList(List<SurveyResult> results, String listName)
    {
        for (SurveyResult result : results)
        {

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
            if (result.getValueType().isSingleValued())
                result.setQuestionId(result.getIdentifier());

            Table.insert(user, responseMetadataTable, result);
        }
    }
}
