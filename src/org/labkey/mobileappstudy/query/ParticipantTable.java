package org.labkey.mobileappstudy.query;

import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.SimpleUserSchema.SimpleTable;

import static org.labkey.mobileappstudy.MobileAppStudySchema.ENROLLMENT_TOKEN_TABLE;
import static org.labkey.mobileappstudy.query.EnrollmentTokenTable.TOKEN_DISPLAY_COLUMN_FACTORY;

public class ParticipantTable extends SimpleTable<MobileAppStudyQuerySchema>
{
    ParticipantTable(MobileAppStudyQuerySchema schema, SchemaTableInfo dbTable, ContainerFilter cf)
    {
        super(schema, dbTable, cf);

        // wrap all the existing columns
        wrapAllColumns();

        // graft the enrollment token into this table for convenience - can be added to every response list, if desired
        SQLFragment sql = new SQLFragment("(SELECT Token FROM mobileappstudy.")
            .append(ENROLLMENT_TOKEN_TABLE)
            .append(" tt WHERE tt.ParticipantId=")
            .append(ExprColumn.STR_TABLE_ALIAS)
            .append(".RowId)");
        BaseColumnInfo token = new ExprColumn(this, "Token", sql, JdbcType.VARCHAR, getColumn("RowId"));
        token.setDisplayColumnFactory(TOKEN_DISPLAY_COLUMN_FACTORY);
        addColumn(token);

        setReadOnly(true);
    }
}