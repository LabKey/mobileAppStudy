package org.labkey.mobileappstudy.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.LookupForeignKey;
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
        BaseColumnInfo token = new AliasedColumn(this, "Token", dbTable.getColumn("RowId"));
        token.setFk(new LookupForeignKey("ParticipantId", "Token")
        {
            @Override
            public @Nullable TableInfo getLookupTableInfo()
            {
                return schema.getTable(ENROLLMENT_TOKEN_TABLE, cf);
            }
        });
        token.setDisplayColumnFactory(TOKEN_DISPLAY_COLUMN_FACTORY);

        addColumn(token);
        setReadOnly(true);
    }
}