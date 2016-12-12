package org.labkey.test.data.mobileappstudy;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Created by iansigmon on 11/30/16.
 */
public abstract class Survey extends Form
{
    private static final String RESPONSE_FORMAT = "{\n" + "%1$s\n" + "}";

    private String _appToken;
    private String _surveyId;
    private String _version;
    private Date _start;
    private Date _end;

    private boolean _omitStart;
    private boolean _omitEnd;
    private boolean _omitResults;

    public Survey(String appToken, String surveyId, String version, Date start, Date end)
    {
        _appToken = appToken;
        _surveyId = surveyId;
        _version = version;
        _start = start;
        _end = end;
    }

    public String getVersion()
    {
        return _version;
    }

    public String getSurveyId()
    {
        return _surveyId;
    }

    public String getAppToken()
    {
        return _appToken;
    }

    public Date getEnd()
    {
        return _end;
    }

    public Date getStart()
    {
        return _start;
    }

    public void setOmitResults(boolean omitResults)
    {
        _omitResults = omitResults;
    }

    public void setOmitEnd(boolean omitEnd)
    {
        _omitEnd = omitEnd;
    }

    public void setOmitStart(boolean omitStart)
    {
        _omitStart = omitStart;
    }



    public String getResponseJson()
    {
        String format = getFormatString();



        return String.format(getFormatString(), getStart(), getEnd(),
            String.join(",\n",
                    responses.stream().map(QuestionResponse::getJsonString).collect(Collectors.toList())
            )
        );
    }

    private String getFormatString()
    {
        ArrayList<String> props = new ArrayList();
        if (!_omitStart)
            props.add("\"start\": \"%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %1$tz\"");
        if (!_omitEnd)
            props.add("\"end\": \"%2$tY-%2$tm-%2$td %2$tH:%2$tM:%2$tS %2$tz\"");
        if (!_omitResults)
            props.add("\"results\": [%3$s]");

        return "{\n" + String.join(",\n", props) + "}";
    }
}
