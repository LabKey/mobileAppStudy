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
package org.labkey.test.components.mobileappstudy;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.WebPart;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class StudySetupWebPart extends WebPart<StudySetupWebPart.ElementCache>
{

    public StudySetupWebPart(BaseWebDriverTest test)
    {
        super(test.getWrappedDriver(), StudySetupWebPart.Locators.dataRegionLocator.findElement(test.getDriver()));
        waitForReady();
    }

    @Override
    public WebElement getComponentElement()
    {
        try
        {
            return StudySetupWebPart.Locators.dataRegionLocator.findElement(_test.getDriver());
        }
        catch(NoSuchElementException nsee)
        {
            return null;
        }
    }

    @Override
    protected void waitForReady()
    {
        _test.waitForElement(StudySetupWebPart.Locators.dataRegionLocator);
    }

    @Override
    public String getTitle()
    {
        return getComponentElement().findElement(By.tagName("th")).getText();
    }

    public String getPrompt()
    {
        return elementCache().shortNamePrompt.getText();
    }

    public String getShortName()
    {
        String textValue;

        if((getWrapper().isElementPresent(Locators.shortNameField)) && (elementCache().shortNameField.isDisplayed()))
            textValue = getWrapper().getFormElement(elementCache().shortNameField);
        else
            textValue = "";

        return textValue;
    }

    public StudySetupWebPart setShortName(String shortName)
    {
        getWrapper().setFormElement(elementCache().shortNameField, shortName);
        getWrapper().waitForFormElementToEqual(elementCache().shortNameField, shortName);
        getWrapper().sleep(500);
        return this;
    }

    public boolean isShortNameVisible()
    {
        if(getWrapper().isElementPresent(Locators.shortNameField))
            return elementCache().shortNameField.isDisplayed();
        else
            return false;
    }

    public boolean isSubmitEnabled()
    {
        getWrapper().sleep(500);
        String classValue = elementCache().submitButton.getAttribute("class");
        return !classValue.toLowerCase().contains("x4-btn-disabled");
    }

    public void clickSubmit()
    {
        getWrapper().sleep(500);

        boolean collectionEnabled = getWrapper().isChecked(Locators.collectionEnabledCheckbox);
        elementCache().submitButton.click();
        if (!collectionEnabled)
        {
            ResponseCollectionDialog warning = new ResponseCollectionDialog(getDriver());
            warning.clickOk();
        }
    }

    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    public class ElementCache extends WebPart.ElementCache
    {
        WebElement submitButton = new LazyWebElement(Locators.submitButton, getDriver());
        WebElement shortNameField = new LazyWebElement(Locators.shortNameField, getDriver());
        WebElement shortNamePrompt = new LazyWebElement(Locators.shortNamePrompt, getDriver());

    }

    public static class Locators extends org.labkey.test.Locators
    {
        protected static final Locator.XPathLocator dataRegionLocator = Locator.xpath("//table[tbody/tr/th[@title='Study Setup']]");
        protected static final Locator.XPathLocator shortNamePrompt = dataRegionLocator.append("//div[@id='labkey-mobileappstudy-studysetup']//span/div/div/div[contains(@class, 'x4-panel-body')]/span/div");
        protected static final Locator.XPathLocator shortNameField = Locator.input("shortName");
        protected static final Locator.XPathLocator submitButton = dataRegionLocator.append(Ext4Helper.Locators.ext4Button("Submit"));
        protected static final Locator.XPathLocator collectionEnabledCheckbox = Locator.tag("input").withAttribute("type", "button").withAttributeContaining("id", "collectionEnabled");
    }

    public class ResponseCollectionDialog extends Window
    {
        public ResponseCollectionDialog(WebDriver wd)
        {
            super("Response collection stopped", wd);
        }

        public void clickCancel()
        {
            clickButton("Cancel", 0);
            waitForClose();
        }

        public void clickOk()
        {
            clickButton("OK", 0);

            //TODO: Issue 28463: Ext.Msg reuses the WebElement for the dialog so dont wait for close, as may already be reopened Issue:
            //waitForClose();     //
            getWrapper().longWait();
        }

        public class Locators extends org.labkey.test.Locators
        {
            public final Locator.XPathLocator okButton = Ext4Helper.Locators.ext4Button("OK");
            public final Locator.XPathLocator cancelButton = Ext4Helper.Locators.ext4Button("Cancel");
        }
    }
}
