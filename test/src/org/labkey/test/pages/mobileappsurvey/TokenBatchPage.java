/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

package org.labkey.test.pages.mobileappsurvey;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebElement;

public class TokenBatchPage extends LabKeyPage
{
    Elements _elements;

    public TokenBatchPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public static TokenBatchPage beginAt(BaseWebDriverTest test, String containerPath)
    {
        test.beginAt(WebTestHelper.buildURL("mobileappsurvey", containerPath, "tokenBatch"));
        return new TokenBatchPage(test);
    }

    public boolean isNewBatchPresent()
    {
        return elements().newBatchButton.isDisplayed();
    }

    public boolean isNewBatchEnabled()
    {
        return elements().newBatchButton.isEnabled();
    }

    public void openNewBatchPopup()
    {
        log("Opening new batch size popup.");
        elements().newBatchButton.click();
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        WebElement batchQueryView = DataRegionTable.Locators.dataRegion("query").findWhenNeeded(this);
        WebElement newBatchButton = new LazyWebElement(Locator.lkButton().withText("New Batch"), this);

    }

    public static class Locators extends org.labkey.test.Locators
    {

    }
}
