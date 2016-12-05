package org.labkey.mobileappstudy.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.labkey.api.data.Container;

import java.util.Date;

/**
 * Representation of the mobileappstudy.responsemetadata table.
 */
public class ResponseMetadata
{
    private Integer _rowId;
    private Container _container;
    private String _listName;
    private Integer _surveyId;
    private String _questionId;
    private Date _startTime;
    private Date _endTime;
    private Boolean _skipped;
    private Date _created;

    public Integer getRowId()
    {
        return _rowId;
    }

    public void setRowId(Integer rowId)
    {
        this._rowId = rowId;
    }

    @JsonIgnore
    public Container getContainer()
    {
        return _container;
    }

    @JsonIgnore
    public void setContainer(Container container)
    {
        this._container = container;
    }

    public String getListName()
    {
        return _listName;
    }

    public void setListName(String listName)
    {
        this._listName = listName;
    }

    public Integer getSurveyId()
    {
        return _surveyId;
    }

    public void setSurveyId(Integer surveyId)
    {
        this._surveyId = surveyId;
    }

    public String getQuestionId()
    {
        return _questionId;
    }

    public void setQuestionId(String questionId)
    {
        this._questionId = questionId;
    }

    public Date getStartTime()
    {
        return _startTime;
    }

    public void setStart(Date startTime)
    {
        this._startTime = startTime;
    }

    public void setStartTime(Date startTime)
    {
        this._startTime = startTime;
    }

    public Date getEndTime()
    {
        return _endTime;
    }

    public void setEnd(Date endTime)
    {
        this._endTime = endTime;
    }

    public void setEndTime(Date endTime)
    {
        this._endTime = endTime;
    }

    public Boolean getSkipped()
    {
        return _skipped;
    }

    public void setSkipped(Boolean skipped)
    {
        this._skipped = skipped;
    }

    public Date getCreated()
    {
        return _created;
    }

    public void setCreated(Date created)
    {
        this._created = created;
    }

}
