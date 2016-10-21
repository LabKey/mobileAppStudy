package org.labkey.mobileappsurvey.view;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.mobileappsurvey.MobileAppSurveySchema;

/**
 * Created by susanh on 10/10/16.
 */
public class EnrollmentTokenBatchesWebPart extends QueryView
{
    public EnrollmentTokenBatchesWebPart(ViewContext viewContext)
    {
        super(QueryService.get().getUserSchema(viewContext.getUser(), viewContext.getContainer(), MobileAppSurveySchema.NAME));
        setSettings(createQuerySettings(viewContext));
        addClientDependency(ClientDependency.fromPath("survey/panel/enrollmentTokenBatchFormPanel.js"));
        setShowInsertNewButton(false);
        setShowImportDataButton(false);
        setShowDeleteButton(false);
        setShowReports(false);
        setShowUpdateColumn(false);

    }

    private QuerySettings createQuerySettings(ViewContext viewContext)
    {
        UserSchema schema = getSchema();
        QuerySettings settings = schema.getSettings(viewContext,  QueryView.DATAREGIONNAME_DEFAULT,  MobileAppSurveySchema.ENROLLMENT_TOKEN_BATCH_TABLE);

        return settings;
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);
        ActionButton generateBtn = new ActionButton("New Batch");
        generateBtn.setScript("Ext4.create('LABKEY.MobileAppSurvey.EnrollmentTokenBatchFormPanel', {gridButton: this}).show();");
        addButton(bar, generateBtn);
    }

}
