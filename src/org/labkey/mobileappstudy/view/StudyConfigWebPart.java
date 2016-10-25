package org.labkey.mobileappstudy.view;

import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.mobileappstudy.MobileAppStudyManager;
import org.labkey.mobileappstudy.data.MobileAppStudy;

/**
 * Web part for associating a study short name with a container.
 */
public class StudyConfigWebPart extends JspView<MobileAppStudy>
{
    public StudyConfigWebPart(ViewContext viewContext)
    {
        super("/org/labkey/mobileappstudy/view/studySetup.jsp");
        setTitle("Study Setup");

        MobileAppStudy bean = new MobileAppStudy();
        bean.setShortName(MobileAppStudyManager.get().getStudyShortName(viewContext.getContainer()));
        bean.setEditable(!MobileAppStudyManager.get().hasStudyParticipants(viewContext.getContainer()));
        this.setModelBean(bean);
    }
}
