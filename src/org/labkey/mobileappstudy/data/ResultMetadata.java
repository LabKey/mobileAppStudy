package org.labkey.mobileappstudy.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.labkey.api.data.Container;

import java.util.Date;

/**
 * Representation of the mobileappstudy.responsemetadata table.
 */
public class ResultMetadata
{
    private Integer rowId;
    private Container container;
    private String listName;
    private Integer surveyId;
    private String questionId;
    private Date startTime;
    private Date endTime;
    private Boolean skipped;
    private Date created;


    public Integer getRowId()
    {
        return rowId;
    }

    public void setRowId(Integer rowId)
    {
        this.rowId = rowId;
    }

    @JsonIgnore
    public Container getContainer()
    {
        return container;
    }

    @JsonIgnore
    public void setContainer(Container container)
    {
        this.container = container;
    }

    public String getListName()
    {
        return listName;
    }

    public void setListName(String listName)
    {
        this.listName = listName;
    }

    public Integer getSurveyId()
    {
        return surveyId;
    }

    public void setSurveyId(Integer surveyId)
    {
        this.surveyId = surveyId;
    }

    public String getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(String questionId)
    {
        this.questionId = questionId;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStart(Date startTime)
    {
        this.startTime = startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEnd(Date endTime)
    {
        this.endTime = endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public Boolean getSkipped()
    {
        return skipped;
    }

    public void setSkipped(Boolean skipped)
    {
        this.skipped = skipped;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date created)
    {
        this.created = created;
    }

}
