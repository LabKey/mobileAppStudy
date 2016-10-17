package org.labkey.mobileappsurvey.view;

import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.mobileappsurvey.MobileAppSurveyManager;
import org.labkey.mobileappsurvey.data.MobileAppStudy;

/**
 * Web part for associating a study short name with a container.
 */
public class StudyConfigWebPart extends JspView<MobileAppStudy>
{
    public StudyConfigWebPart(ViewContext viewContext)
    {
        super("/org/labkey/mobileappsurvey/view/studySetup.jsp");
        setTitle("Study Setup");

        MobileAppStudy bean = new MobileAppStudy();
        bean.setShortName(MobileAppSurveyManager.get().getStudyShortName(viewContext.getContainer()));
        bean.setEditable(!MobileAppSurveyManager.get().hasStudyParticipants(viewContext.getContainer()));
        this.setModelBean(bean);
    }
}
