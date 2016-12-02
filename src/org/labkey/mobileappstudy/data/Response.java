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
import org.labkey.api.view.NotFoundException;
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

    private void storeSurveyResult(String listName, Integer participantId, List<SurveyResult> results, List<String> errors, Container container, User user) throws Exception
    {
        // initialize the data map with the survey result values
        Map<String, Object> data = new ArrayListMap<>();
        data.put("startTime", getStartTime());
        data.put("endTime", getEndTime());
        data.put("participantId", participantId);

        Map<String, Object> row = storeListResults(container, user, null, listName, results, data, errors);
        if (!row.isEmpty())
        {
            Integer surveyId = (Integer) row.get("Key");
            List<SurveyResult> multiValuedResults = getMultiValuedResults(listName, results);
            storeMultiValuedResults(multiValuedResults, surveyId, user, container, errors);
        }
    }

    private List<SurveyResult> getSingleValuedResults(Container container, TableInfo list, Map<String, Object> data, List<SurveyResult> results, List<String> errors)
    {
        List<SurveyResult> singleValuedResults = new ArrayList<>();
        for (SurveyResult result : results)
        {
            if (result.getValueType().isSingleValued())
            {
                result.setListName(list.getName());
                if (validateListColumn(container, list, result.getIdentifier(), errors, result.getValueType()))
                {
                    singleValuedResults.add(result);
                    data.put(result.getIdentifier(), result.getResult());
                }
            }
        }

        return singleValuedResults;
    }

    private List<SurveyResult> getMultiValuedResults(String baseListName, List<SurveyResult> results)
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

    private TableInfo getResultTable(String listName, Container container, User user) throws NotFoundException
    {
        ListDefinition listDef = ListService.get().getList(container, listName);
        if (listDef == null)
            throw new NotFoundException("Invalid list '" + listName + "' for container '" + container.getName() + "'");

        TableInfo resultTable = listDef.getTable(user, container);
        if (resultTable == null)
            throw new NotFoundException("Unable to find table for list '" + listDef.getName() + "' in container '" + container.getName() + "'");

        return resultTable;
    }

    private boolean validateListColumn(Container container, TableInfo table, String columnName, List<String> errors, SurveyResult.ValueType resultValueType)
    {
        ColumnInfo column = table.getColumn(columnName);
        if (column == null)
        {
            errors.add("Unable to find column '" + columnName + "' in list '" + table.getName() + "' in container '" + container.getName() + "'");
        }
        else if (column.getJdbcType() != resultValueType.getJdbcType())
        {
            errors.add("Type '" + resultValueType + "' (" + resultValueType.getJdbcType() + ") of result '" + columnName + "' does not match expected type (" + column.getJdbcType() + ")");
        }

        return errors.isEmpty();
    }

    private Map<String, Object> storeListData(TableInfo table, Map<String, Object> data, Container container, User user) throws Exception
    {
        // Add an entry to the survey list and get the id.
        BatchValidationException exception = new BatchValidationException();
        // need to create a LimitedUser to use for checking permissions, wrapping the Guest user
        User insertUser = new LimitedUser((user == null)? UserManager.getGuestUser() : user,
                new int[0], Collections.singleton(RoleManager.getRole(SubmitterRole.class)), false);

        if (table.getUpdateService() == null)
            throw new NotFoundException("Unable to get update service for table " + table.getName());
        List<Map<String, Object>> rows = table.getUpdateService().insertRows(insertUser, container, Collections.singletonList(data), exception, null, null);
        if (exception.hasErrors())
            throw exception;
        else
            return rows.get(0);
    }

    private Map<String, Object> storeListResults(Container container, User user, Integer surveyId, String listName, List<SurveyResult> results, Map<String, Object> data, List<String> errors) throws Exception
    {
        TableInfo surveyTable = getResultTable(listName, container, user);
        if (surveyTable.getUpdateService() == null)
        {
            errors.add("No update service available for the given survey table: " + listName);
            return Collections.emptyMap();
        }

        // find all the single-value results, check if they are in the list, check the type, and add them to the data map if everything is good
        List<SurveyResult> singleValuedResults = getSingleValuedResults(container, surveyTable, data, results, errors);
        if (!errors.isEmpty())
            return Collections.emptyMap();

        Map<String, Object> row = storeListData(surveyTable, data, container, user);
        if (surveyId == null)
            surveyId = (Integer) row.get("Key");

        // Add a resultMetadata row for each of the individual rows using the given surveyId
        storeResultMetadata(user, container, singleValuedResults, surveyId);

        return row;
    }

    private void storeMultiValuedResults(List<SurveyResult> results, Integer surveyId, User user, Container container, List<String> errors) throws Exception
    {
        for (SurveyResult result : results)
        {
            if (result.getValueType() == SurveyResult.ValueType.CHOICE)
            {
                storeResultChoices(container, user, result, surveyId, errors);
            }
            else // result is of type GROUPED_RESULT
            {
                // two scenarios, groupedResult is a an array of SurveyResult objects or is an array of an array of SurveyResult objects
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
                    data.put("surveyId", surveyId);
                    Map<String, Object> row = storeListResults(container, user, surveyId, listName, groupResults, data, errors);
                    if (!row.isEmpty())
                    {
                        // TODO the key should come from row instead of passing along surveyId (i.e. change surveyId to parentListId)
                        List<SurveyResult> multiValuedResults = getMultiValuedResults(listName, groupResults);
                        storeMultiValuedResults(multiValuedResults, surveyId, user, container, errors);
                    }
                }
            }
        }
    }

    private void storeResultChoices(Container container, User user, SurveyResult result, Integer surveyId, List<String> errors) throws Exception
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
