package org.labkey.test.pages.mobileappstudy;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.mobileappstudy.StudySetupWebPart;
import org.labkey.test.components.mobileappstudy.TokenBatchesWebPart;
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
