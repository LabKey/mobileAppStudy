package org.labkey.mobileappstudy.data;

import java.util.Map;

public class TextChoiceResult
{
    private String value;
    public String getValue()
    {
        return value;
    }
    private String otherText = null;
    public String getOtherText()
    {
        return otherText;
    }

    public TextChoiceResult(Object val)
    {
        if (val instanceof String)
            value = (String)val;
        else if (val instanceof Map)
        {
            Map<String, String> valMap = (Map<String, String>) val;
            value = valMap.getOrDefault("other", "");
            otherText = valMap.getOrDefault("text", "");
        }
    }

//        public TextChoiceResult(String val)
//        {
//            value = val;
//        }
//        public TextChoiceResult(Map<String,String> val)
//        {
//        }
}
