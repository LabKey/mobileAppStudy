/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.mobileappstudy.forwarder;

import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;

import java.util.Map;

public class ForwarderProperties
{
    public static final String PASSWORD_PLACEHOLDER = "***REDACTED***";
    private static final String FORWARDER_CATEGORY = "MobileAppForwarder";
    public static final String URL_PROPERTY_NAME = "URL";
    public static final String USER_PROPERTY_NAME = "USER";
    public static final String PASSWORD_PROPERTY_NAME = "PASSWORD";
    public static final String ENABLED_PROPERTY_NAME = "ENABLED";

    /**
     * Check is forwarding enabled for the study container
     * @param container to check
     * @return True if forwarding is enabled
     */
    public boolean isForwardingEnabled( Container container)
    {
        return Boolean.valueOf(PropertyManager.getEncryptedStore().getProperties(container, FORWARDER_CATEGORY).get(ENABLED_PROPERTY_NAME));
    }

    /**
     * Set the connection properties for the forwarding endpoint
     * @param container containing study
     * @param url endpoint to forward to
     * @param username to authenticate with
     * @param password to authenticate with
     * @param enable flag indicating whether forwarding is enabled
     */
    public void setForwardingProperties(Container container, String url, String username, String password, Boolean enable)
    {
        PropertyManager.PropertyMap propertyMap = PropertyManager.getEncryptedStore().getWritableProperties(container, FORWARDER_CATEGORY ,true);
        propertyMap.put(URL_PROPERTY_NAME, url);
        propertyMap.put(USER_PROPERTY_NAME, username);
        if (!password.equals(PASSWORD_PLACEHOLDER))
            propertyMap.put(PASSWORD_PROPERTY_NAME, password);
        propertyMap.put(ENABLED_PROPERTY_NAME, String.valueOf(enable));

        propertyMap.save();
    }

    /**
     * Get a forwarder connection properties
     * @param container container to get properties for
     * @return Map of strings of connection properties
     */
    public Map<String, String> getForwarderConnection(Container container)
    {
        return PropertyManager.getEncryptedStore().getProperties(container, FORWARDER_CATEGORY);
    }
}
