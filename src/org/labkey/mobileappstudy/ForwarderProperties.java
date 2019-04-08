package org.labkey.mobileappstudy;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.security.User;

import java.util.Map;

public class ForwarderProperties
{
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

    /**
     * Set the forwarding enabled property for a container
     * @param container to set property
     * @param enable true to enable, false to disable
     */
    public void setForwarding(Container container, boolean enable)
    {
        PropertyManager.PropertyMap propertyMap = PropertyManager.getEncryptedStore().getWritableProperties(container, FORWARDER_CATEGORY, true);
        propertyMap.put(ENABLED_PROPERTY_NAME, String.valueOf(enable));
        propertyMap.save();
    }
}
