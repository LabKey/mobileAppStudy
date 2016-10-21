package org.labkey.test.pages.mobileappsurvey;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.mobileappsurvey.StudySetupWebPart;
import org.labkey.test.components.mobileappsurvey.TokenBatchesWebPart;
import org.labkey.test.pages.LabKeyPage;

public class SetupPage extends LabKeyPage
{

    BaseWebDriverTest _test;
    public StudySetupWebPart studySetupWebPart;
    public TokenBatchesWebPart tokenBatchesWebPart;

    public SetupPage(BaseWebDriverTest test)
    {
        super(test);
        _test = test;
        tokenBatchesWebPart = new TokenBatchesWebPart(_test);
        studySetupWebPart = new StudySetupWebPart(_test);
    }

}
