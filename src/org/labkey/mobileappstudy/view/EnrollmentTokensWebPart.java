package org.labkey.mobileappstudy.view;

import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.ViewContext;
import org.labkey.mobileappstudy.MobileAppStudySchema;

/**
 * Web part for showing a set of enrollment tokens.
 */
public class EnrollmentTokensWebPart extends QueryView
{
    public EnrollmentTokensWebPart(ViewContext viewContext)
    {
        super(QueryService.get().getUserSchema(viewContext.getUser(), viewContext.getContainer(), MobileAppStudySchema.NAME));
        setSettings(createQuerySettings(viewContext));
        setShowInsertNewButton(false);
        setShowImportDataButton(false);
        setShowDeleteButton(false);
        setShowReports(false);
        setShowUpdateColumn(false);
        setShowDetailsColumn(false);
    }

    private QuerySettings createQuerySettings(ViewContext viewContext)
    {
        UserSchema schema = getSchema();
        return schema.getSettings(viewContext,  "enrollmentTokens",  MobileAppStudySchema.ENROLLMENT_TOKEN_TABLE);
    }
}
