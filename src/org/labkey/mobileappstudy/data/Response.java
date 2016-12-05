package org.labkey.mobileappstudy.data;

import java.util.Date;
import java.util.List;

/**
 * A response element in a SurveyResponse type.
 */
public class Response
{
    private Date _startTime;
    private Date _endTime;
    private List<SurveyResult> _results;

    public Date getStartTime()
    {
        return _startTime;
    }

    public void setStartTime(Date startTime)
    {
        this._startTime = startTime;
    }

    public Date getEndTime()
    {
        return _endTime;
    }

    public void setEndTime(Date endTime)
    {
        this._endTime = endTime;
    }

    public void setStart(Date start)
    {
        this._startTime = start;
    }

    public void setEnd(Date end)
    {
        this._endTime = end;
    }

    public List<SurveyResult> getResults()
    {
        return _results;
    }

    public void setResults(List<SurveyResult> results)
    {
        this._results = results;
    }
}
