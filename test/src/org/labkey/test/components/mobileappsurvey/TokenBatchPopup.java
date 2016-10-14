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

package org.labkey.test.components.mobileappsurvey;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Input.Input;

public class TokenBatchPopup extends LabKeyPage
{
    Elements _elements;

    public TokenBatchPopup(BaseWebDriverTest test)
    {
        super(test);
    }

    public boolean isSubmitEnabled()
    {
        return Locators.enabledSubmitButton.findElements(this.getDriver()).size() > 0;
    }

    public boolean isCancelEnabled()
    {
        return Locators.enabledCancelButton.findElements(this.getDriver()).size() > 0;
    }

    public void selectOtherBatchSize()
    {
        RadioButton otherCount = elements().findCountChoice("Other");
        otherCount.check();
    }

    public void setOtherBatchSize(String size)
    {
        log("Selecting (other) batch size " + size);
        elements().otherCountInput.set(size);
        waitForElement(Locators.enabledSubmitButton);
    }

    public void selectBatchSize(String size)
    {
        log("Selecting batch size " + size);
        RadioButton countButton = elements().findCountChoice(size);
        if (countButton.isDisplayed())
            countButton.check();
        else
        {
            selectOtherBatchSize();
            setOtherBatchSize(size);
        }
    }

    public void createNewBatch(String size)
    {
        log("Creating new batch of size " + size);
        selectBatchSize(size);
        clickAndWait(elements().submitButton);
    }

    public void cancelNewBatch()
    {
        log("Canceling new batch");
        elements().cancelButton.click();
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    private class Elements extends ElementCache
    {
        Input otherCountInput = Input(Locator.name("otherCount"), getDriver()).findWhenNeeded(this);
        WebElement submitButton =  new LazyWebElement(Ext4Helper.Locators.ext4Button("Submit"), this);
        WebElement cancelButton =  new LazyWebElement(Ext4Helper.Locators.ext4Button("Cancel"), this);

        RadioButton findCountChoice(String size)
        {
            return RadioButton.RadioButton().withLabel(size).findWhenNeeded(this);
        }

    }

    public static class Locators extends org.labkey.test.Locators
    {
        public static final Locator.XPathLocator enabledSubmitButton = Ext4Helper.Locators.ext4Button("Submit").enabled();
        public static final Locator.XPathLocator enabledCancelButton = Ext4Helper.Locators.ext4Button("Cancel").enabled();
    }
}
