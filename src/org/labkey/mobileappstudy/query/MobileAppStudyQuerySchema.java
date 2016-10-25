package org.labkey.mobileappstudy.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.security.User;
import org.labkey.mobileappstudy.MobileAppStudyModule;

import static org.labkey.mobileappstudy.MobileAppStudySchema.ENROLLMENT_TOKEN_BATCH_TABLE;
import static org.labkey.mobileappstudy.MobileAppStudySchema.ENROLLMENT_TOKEN_TABLE;

/**
 * Created by susanh on 10/10/16.
 */
public class MobileAppStudyQuerySchema extends SimpleUserSchema
{
    public static final String NAME = "mobileappstudy";
    public static final String DESCRIPTION = "Provides data about study enrollment and survey responses";

    public MobileAppStudyQuerySchema(User user, Container container)
    {
        super(NAME, DESCRIPTION, user, container, DbSchema.get(NAME, DbSchemaType.Module));
    }

    public static void register(final MobileAppStudyModule module)
    {
        DefaultSchema.registerProvider(NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new MobileAppStudyQuerySchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    @Nullable
    @Override
    protected TableInfo createTable(String name)
    {
        if (ENROLLMENT_TOKEN_BATCH_TABLE.equalsIgnoreCase(name))
        {
            return new EnrollmentTokenBatchTable(this);
        }
        else if (ENROLLMENT_TOKEN_TABLE.equalsIgnoreCase(name))
        {
            return new EnrollmentTokenTable(this);
        }
        else
            return super.createTable(name);
    }
}
