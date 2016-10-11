package org.labkey.mobileappsurvey.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.security.User;
import org.labkey.mobileappsurvey.MobileAppSurveyModule;

/**
 * Created by susanh on 10/10/16.
 */
public class MobileAppSurveyQuerySchema extends SimpleUserSchema
{
    public static final String NAME = "mobileappsurvey";
    public static final String DESCRIPTION = "Provides data about survey enrollment and responses";

    public MobileAppSurveyQuerySchema(User user, Container container)
    {
        super(NAME, DESCRIPTION, user, container, DbSchema.get(NAME, DbSchemaType.Module));
    }

    public static void register(final MobileAppSurveyModule module)
    {
        DefaultSchema.registerProvider(NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new MobileAppSurveyQuerySchema(schema.getUser(), schema.getContainer());
            }
        });
    }
}
