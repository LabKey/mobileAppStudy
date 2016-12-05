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
import org.labkey.api.util.Pair;
import org.labkey.api.view.NotFoundException;
import org.labkey.mobileappstudy.MobileAppStudySchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A response element in a SurveyResponse type.
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

    /**
     * Stores the survey results of this object into their respective lists
     * @param listName the name of the list in which to store results
     * @param participantId identifier of the participant
     * @param container the container in which the lists live
     * @param user the user initiating the store request
     * @throws Exception if there was a problem storing the results in one or more of its lists, in which case none of the lists will be updated
     */
    public void store(@NotNull String listName, @NotNull Integer participantId, @NotNull Container container, @Nullable User user) throws Exception
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

    /**
     * Store a SurveyResult in a given list and then recursively store its multi-valued results in their respective lists
     * @param listName name of the list to store data in
     * @param participantId identifier of the participant corresponding to this survey
     * @param results the collection of results for this survey result
     * @param errors the collection of errors encountered while storing this survey result
     * @param container the container in which the list lives
     * @param user the user who will store the data
     * @throws Exception if there are problems storing the data.
     */
    private void storeSurveyResult(@NotNull String listName, @NotNull Integer participantId, @NotNull List<SurveyResult> results, @NotNull List<String> errors, @NotNull Container container, @Nullable User user) throws Exception
    {
        // initialize the data map with the survey result values
        Map<String, Object> data = new ArrayListMap<>();
        data.put("startTime", getStartTime());
        data.put("endTime", getEndTime());
        data.put("participantId", participantId);

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
        for (SurveyResult result : results)
        {
            if (result.getValueType().isSingleValued())
            {
                result.setListName(list.getName());
                if (validateListColumn(list, result.getIdentifier(), result.getValueType(), errors))
                {
                    singleValuedResults.add(result);
                }
            }
        }

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
            errors.add("Unable to find column '" + columnName + "' in list '" + table.getName() + "' in container '" + table.getDomain().getContainer() + "'");
        }
        else if (column.getJdbcType() != resultValueType.getJdbcType())
        {
            errors.add("Type '" + resultValueType + "' (" + resultValueType.getJdbcType() + ") of result '" + columnName + "' does not match expected type (" + column.getJdbcType() + ")");
        }

        return errors.isEmpty();
    }

    /**
     * Inserts a row for the given list table to add the data provided
     * @param table the list table in which data is to be stored
     * @param data the collection of data elements for the new row
     * @param container the container in which the list (table) lives
     * @param user the user initiating the request
     * @return the newly created row
     * @throws Exception if the table has no update service or there is any other problem inserting the new row
     */
    private Map<String, Object> storeListData(@NotNull TableInfo table, @NotNull Map<String, Object> data, @NotNull Container container, @Nullable User user) throws Exception
    {
        // Add an entry to the survey list and get the id.
        BatchValidationException exception = new BatchValidationException();
        // need to create a LimitedUser to use for checking permissions, wrapping the Guest user if a user isn't provided
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
    private Map<String, Object> storeListResults(@Nullable Integer surveyId, @NotNull String listName, @NotNull List<SurveyResult> results, @NotNull Map<String, Object> data, @NotNull List<String> errors, @NotNull Container container, @Nullable User user) throws Exception
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
        TableInfo table = getResultTable(result.getListName(), container, user);
        validateListColumn(table, result.getIdentifier(), SurveyResult.ValueType.STRING, errors);

        if (errors.isEmpty())
        {
            for (Object value : (ArrayList) result.getValue())
            {
                Map<String, Object> data = new ArrayListMap<>();
                data.put(parentKey.getKey(), parentKey.getValue());
                data.put(result.getIdentifier(), value);
                storeListData(table, data, container, user);
            }

            storeResponseMetadata(Collections.singletonList(result), surveyId, container, user);
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
            result.setQuestionId(result.getIdentifier());

            Table.insert(user, responseMetadataTable, result);
        }
    }
}
