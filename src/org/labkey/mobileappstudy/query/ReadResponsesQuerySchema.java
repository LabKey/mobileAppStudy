package org.labkey.mobileappstudy.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.util.StringExpression;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.mobileappstudy.MobileAppStudyManager;
import org.labkey.mobileappstudy.data.Participant;

import java.util.Set;

/**
 * Created by adam on 1/31/2017.
 */
public class ReadResponsesQuerySchema extends UserSchema
{
    private static final String NAME = "MobileAppResponse";

    private final UserSchema _listSchema;
    private final int _participantId = 1;

    private ReadResponsesQuerySchema(User user, Container container, UserSchema listSchema)
    {
        super(NAME, "Special query schema that allows the mobile application to read responses submitted by the participant using that device", user, container, listSchema.getDbSchema());
        _listSchema = listSchema;

        Participant participant = null;

        if (HttpView.hasCurrentView())
        {
            String token = HttpView.currentView().getViewContext().getRequest().getParameter("token");

            if (null != token)
                participant = MobileAppStudyManager.get().getParticipantFromAppToken(token);
        }

//        if (null != participant)
//            _participantId = participant.getRowId();
//        else
//            throw new UnauthorizedException("Missing or invalid app token");
    }

    public static void register(Module module)
    {
        DefaultSchema.registerProvider(NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public boolean isAvailable(DefaultSchema schema, Module module)
            {
                return true;
            }

            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                Container c = schema.getContainer();
                UserSchema list = schema.getUserSchema("lists");
                return new ReadResponsesQuerySchema(User.guest, c, list);
            }
        });
    }

    @Override
    public boolean canReadSchema() throws UnauthorizedException
    {
        return true;  // We checked app token in the constructor
    }

    @Nullable
    @Override
    public TableInfo createTable(String name)
    {
        TableInfo listTable = _listSchema.createTable(name);
        return listTable == null ? null : new ResponseTable(listTable, _participantId, this);
    }

    private static class ResponseTable extends FilteredTable<ReadResponsesQuerySchema>
    {
        private final TableInfo _listTable;

        public ResponseTable(@NotNull TableInfo table, int participantId, @NotNull ReadResponsesQuerySchema userSchema)
        {
            super(table, userSchema);

            _listTable = table;
            _listTable.getColumns().forEach(this::addWrapColumn);  // Limit to NotPHI columns only?

            setDefaultVisibleColumns(_listTable.getDefaultVisibleColumns());

            addCondition(_listTable.getColumn("Key"), participantId);
        }

        @Override
        public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
        {
            return false;
        }

        @Override
        public ActionURL getGridURL(Container container)
        {
            return null;
        }

        @Override
        public StringExpression getDetailsURL(@Nullable Set<FieldKey> columns, Container container)
        {
            return null;
        }
    }

    @Override
    public Set<String> getTableNames()
    {
        return _listSchema.getTableNames();
    }
}
