package org.labkey.mobileappstudy.data;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by susanh on 11/28/16.
 */
public class Response
{
    private Date startTime;
    private Date endTime;
    private ArrayList<SurveyResult> results;

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public void setStart(Date start)
    {
        this.startTime = start;
    }

    public void setEnd(Date end)
    {
        this.endTime = end;
    }

    public ArrayList<SurveyResult> getResults()
    {
        return results;
    }

    public void setResults(ArrayList<SurveyResult> results)
    {
        this.results = results;
    }
}
