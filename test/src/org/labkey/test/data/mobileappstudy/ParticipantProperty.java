package org.labkey.test.data.mobileappstudy;

public class ParticipantProperty
{
    private String propertyId;
    private Object value;

    public void setValue(Object value)
    {
        this.value = value;
    }

    public Object getValue()
    {
        return this.value;
    }

    public void setPropertyId(String propertyId)
    {
        this.propertyId = propertyId;
    }

    public String getPropertyId()
    {
        return this.propertyId;
    }
}
