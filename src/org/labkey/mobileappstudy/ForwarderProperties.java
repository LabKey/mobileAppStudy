package org.labkey.mobileappstudy;

import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.security.User;

import java.net.URL;
import java.util.Map;

public class ForwarderProperties
{
    private static final String FORWARDER_CATEGORY = "MobileAppForwarder";
    public static final String URL_PROPERTY_NAME = "URL";
    public static final String USER_PROPERTY_NAME = "USER";
    public static final String PASSWORD_PROPERTY_NAME = "PASSWORD";
    public static final String ENABLED_PROPERTY_NAME = "ENABLED";


    public boolean isForwardingEnabled(User user, Container container)
    {
        return Boolean.valueOf(PropertyManager.getProperty(user, container, FORWARDER_CATEGORY, ENABLED_PROPERTY_NAME));
    }

    public void setForwardingProperties(Container container, URL url, String username, String password, Boolean enable)
    {
        PropertyManager.PropertyMap propertyMap = PropertyManager.getEncryptedStore().getWritableProperties(container, FORWARDER_CATEGORY ,true);
        propertyMap.put(URL_PROPERTY_NAME, url.toString());
        propertyMap.put(USER_PROPERTY_NAME, username);
        propertyMap.put(PASSWORD_PROPERTY_NAME, password);
        propertyMap.put(ENABLED_PROPERTY_NAME, String.valueOf(enable));

        propertyMap.save();
    }

    public Map<String, String> getForwarderConnection(Container container)
    {
        return PropertyManager.getEncryptedStore().getProperties(container, FORWARDER_CATEGORY);
    }
}
