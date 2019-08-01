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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.mobileappstudy.MobileAppStudyManager.ForwardingType;

import java.util.Map;

public class ForwarderProperties
{
    private static final String FORWARDER_CATEGORY = "MobileAppForwarder";
    public static final String PASSWORD_PLACEHOLDER = "***REDACTED***";
    public static final String URL_PROPERTY_NAME = "URL";
    public static final String USER_PROPERTY_NAME = "USER";
    public static final String PASSWORD_PROPERTY_NAME = "PASSWORD";
    public static final String ENABLED_PROPERTY_NAME = "ENABLED";
    public static final String FORWARDING_TYPE = "FORWARDING_TYPE";
    public static final String TOKEN_REQUEST_URL = "TOKEN_REQUEST_URL";
    public static final String TOKEN_FIELD = "TOKEN_FIELD";
    public static final String TOKEN_HEADER = "TOKEN_HEADER";
    public static final String OAUTH_URL = "OAUTH_URL";

    /**
     * Set the connection properties for the forwarding endpoint
     * @param container containing study
     * @param url endpoint to forward to
     * @param username to authenticate with
     * @param password to authenticate with
     * @param authType flag indicating whether forwarding is enabled, and which authentication type to use
     */
    public void setForwarderProperties(Container container, ForwardingType authType,
                                       String url, String username, String password,
                                       String tokenRequestURL, String tokenField, String tokenHeader, String oauthURL)
    {
        PropertyManager.PropertyMap propertyMap = PropertyManager.getEncryptedStore().getWritableProperties(container, FORWARDER_CATEGORY ,true);
        propertyMap.put(URL_PROPERTY_NAME, url);
        propertyMap.put(USER_PROPERTY_NAME, username);
        if (StringUtils.isNotBlank(password) && !password.equals(PASSWORD_PLACEHOLDER))
            propertyMap.put(PASSWORD_PROPERTY_NAME, password);
        propertyMap.put(TOKEN_REQUEST_URL, tokenRequestURL);
        propertyMap.put(TOKEN_FIELD, tokenField);
        propertyMap.put(TOKEN_HEADER, tokenHeader);
        propertyMap.put(OAUTH_URL, oauthURL);
        propertyMap.put(FORWARDING_TYPE, authType.name());

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

    /**
     * Check is forwarding enabled for the study container
     * @param container to check
     * @return True if forwarding is enabled
     */
    public static ForwardingType getForwardingType(Container container)
    {
        String authType = PropertyManager.getEncryptedStore().getProperties(container, FORWARDER_CATEGORY).getOrDefault(FORWARDING_TYPE, ForwardingType.Disabled.name());
        return ForwardingType.valueOf(authType);
    }

}
