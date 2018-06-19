/*
 * Copyright (c) 2016-2018 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
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
    private Integer _activityId;
    private String _fieldName;
    private Date _startTime;
    private Date _endTime;
    private Boolean _skipped;
    private Date _created;
    private Integer _participantId;

    public Integer getRowId()
    {
        return _rowId;
    }

    public void setRowId(Integer rowId)
    {
        _rowId = rowId;
    }

    @JsonIgnore
    public Container getContainer()
    {
        return _container;
    }

    @JsonIgnore
    public void setContainer(Container container)
    {
        _container = container;
    }

    public String getListName()
    {
        return _listName;
    }

    public void setListName(String listName)
    {
        _listName = listName;
    }

    public Integer getActivityId()
    {
        return _activityId;
    }

    public void setActivityId(Integer activityId)
    {
        _activityId = activityId;
    }

    public String getFieldName()
    {
        return _fieldName;
    }

    public void setFieldName(String fieldName)
    {
        _fieldName = fieldName;
    }

    public Date getStartTime()
    {
        return _startTime;
    }

    public void setStart(Date startTime)
    {
        _startTime = startTime;
    }

    public void setStartTime(Date startTime)
    {
        _startTime = startTime;
    }

    public Date getEndTime()
    {
        return _endTime;
    }

    public void setEnd(Date endTime)
    {
        _endTime = endTime;
    }

    public void setEndTime(Date endTime)
    {
        _endTime = endTime;
    }

    public Boolean getSkipped()
    {
        return _skipped != null && _skipped;
    }

    public void setSkipped(Boolean skipped)
    {
        _skipped = skipped;
    }

    public Date getCreated()
    {
        return _created;
    }

    public void setCreated(Date created)
    {
        _created = created;
    }

    public Integer getParticipantId()
    {
        return _participantId;
    }

    public void setParticipantId(Integer participantId)
    {
        _participantId = participantId;
    }
}
