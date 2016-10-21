package org.labkey.mobileappsurvey.view;

import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.ViewContext;
import org.labkey.mobileappsurvey.MobileAppSurveySchema;

/**
 * Web part for showing a set of enrollment tokens.
 */
public class EnrollmentTokensWebPart extends QueryView
{
    public EnrollmentTokensWebPart(ViewContext viewContext)
    {
        super(QueryService.get().getUserSchema(viewContext.getUser(), viewContext.getContainer(), MobileAppSurveySchema.NAME));
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
        return schema.getSettings(viewContext,  QueryView.DATAREGIONNAME_DEFAULT,  MobileAppSurveySchema.ENROLLMENT_TOKEN_TABLE);
    }
}
