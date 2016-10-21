package org.labkey.mobileappsurvey.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.view.ActionURL;
import org.labkey.mobileappsurvey.MobileAppSurveyController;
import org.labkey.mobileappsurvey.MobileAppSurveySchema;

import java.util.Collections;

/**
 * Created by susanh on 10/11/16.
 */
public class EnrollmentTokenBatchTable extends SimpleUserSchema.SimpleTable<MobileAppSurveyQuerySchema>
{
    public EnrollmentTokenBatchTable(MobileAppSurveyQuerySchema schema)
    {
        super(schema, schema.getDbSchema().getTable(MobileAppSurveySchema.ENROLLMENT_TOKEN_BATCH_TABLE));

        // wrap all the existing columns
        wrapAllColumns();

        ColumnInfo idColumn = getColumn("RowId");

        ActionURL base = new ActionURL(MobileAppSurveyController.TokenListAction.class, getContainer());
        DetailsURL detailsURL = new DetailsURL(base, Collections.singletonMap("query.BatchId/RowId~eq", "rowId"));

        idColumn.setURL(detailsURL);
        addCountColumn();
    }

    private void addCountColumn()
    {
        SQLFragment sql = new SQLFragment("(SELECT COUNT(*) FROM " + MobileAppSurveySchema.NAME + "." + MobileAppSurveySchema.ENROLLMENT_TOKEN_TABLE + " T  WHERE T.BatchId = " + ExprColumn.STR_TABLE_ALIAS + ".rowId" +
                " AND T.ParticipantId IS NOT NULL)");
        ExprColumn col = new ExprColumn(this, "TokensInUse", sql, JdbcType.INTEGER);
        addColumn(col);
    }
}
