/*
 * Copyright (c) 2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.pages.mobileappstudy;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.mobileappstudy.StudySetupWebPart;
import org.labkey.test.components.mobileappstudy.TokenBatchesWebPart;
import org.labkey.test.pages.LabKeyPage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    public void validateSubmitButtonDisabled()
    {
        log("Validate that the submit button is disabled.");
        assertFalse("Submit button is showing as enabled, it should not be.", studySetupWebPart.isSubmitEnabled());
    }

    public void validateSubmitButtonEnabled()
    {
        log("Validate that the submit button is now enabled.");
        assertTrue("Submit button is not showing as enabled, it should be.", studySetupWebPart.isSubmitEnabled());
    }
}
