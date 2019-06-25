package org.labkey.mobileappstudy.surveydesign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SurveyStepFormat
{
    private List<TextChoice> _textChoices;
    private Integer _selectionStyle;
    private Integer _maxLength;
    private String _style;

    public List<TextChoice> getTextChoices()
    {
        return _textChoices;
    }
    public void setTextChoices(List<TextChoice> textChoices)
    {
        _textChoices = textChoices;
    }

    public Integer getSelectionStyle()
    {
        return _selectionStyle;
    }
    public void setSelectionStyle(Integer selectionStyle)
    {
        _selectionStyle = selectionStyle;
    }

    public Integer getMaxLength()
    {
        return _maxLength;
    }
    public void setMaxLength(Integer maxLength)
    {
        _maxLength = maxLength;
    }

    public String getStyle()
    {
        return _style;
    }
    public void setStyle(String style)
    {
        _style = style;
    }

    public static SurveyStepFormat getIntegerFormat()
    {
        SurveyStepFormat intFormat = new SurveyStepFormat();
        intFormat.setStyle("Integer");
        return intFormat;
    }
}
