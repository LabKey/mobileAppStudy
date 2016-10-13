package org.labkey.mobileappsurvey.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.view.ActionURL;
import org.labkey.mobileappsurvey.MobileAppSurveyController;
import org.labkey.mobileappsurvey.MobileAppSurveySchema;

import java.util.Collections;

/**
 * Created by susanh on 10/11/16.
 */
public class EnrollmentTokenTable extends SimpleUserSchema.SimpleTable<MobileAppSurveyQuerySchema>
{
    public EnrollmentTokenTable(MobileAppSurveyQuerySchema schema)
    {
        super(schema, schema.getDbSchema().getTable(MobileAppSurveySchema.ENROLLMENT_TOKEN_TABLE));

        // wrap all the existing columns
        wrapAllColumns();

        ColumnInfo idColumn = getColumn("BatchId");

        ActionURL base = new ActionURL(MobileAppSurveyController.TokenBatchAction.class, getContainer());
        DetailsURL detailsURL = new DetailsURL(base, Collections.singletonMap("query.RowId~eq", "batchId"));

        idColumn.setURL(detailsURL);

        // don't link out to the schema browser details (where the data can be edited).
        getColumn("Token").setURL(null);

    }
}
