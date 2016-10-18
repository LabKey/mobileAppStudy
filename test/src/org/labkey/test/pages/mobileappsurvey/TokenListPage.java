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

public class TokenListPage extends LabKeyPage
{
    Elements _elements;

    public TokenListPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public static TokenListPage beginAt(BaseWebDriverTest test, String containerPath)
    {
        test.beginAt(WebTestHelper.buildURL("mobileappsurvey", containerPath, "tokenList"));
        return new TokenListPage(test);
    }

    public int getNumTokens()
    {
        DataRegionTable dataRegion = new DataRegionTable("enrollmentTokens", getDriver());
        return dataRegion.getDataRowCount();
    }

    public void goToBatches()
    {
        doAndWaitForPageToLoad(() -> elements().tokenBatchLink.click());
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        WebElement tokenBatchLink = new LazyWebElement(Locators.tokenBatchLink, this);
    }


    public static class Locators extends org.labkey.test.Locators
    {
        public static final Locator.XPathLocator tokenBatchLink = Locator.linkWithText("Token Batches");
    }
}
