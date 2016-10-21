package org.labkey.mobileappsurvey.data;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;

import java.util.Date;

/**
 * Created by susanh on 10/10/16.
 */
public class EnrollmentToken
{
    private int _rowId;
    private int _batchId;
    private String _token;
    private Integer _participantId;

    private Date _created;
    private User _createdBy;
    private Container _container;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getBatchId()
    {
        return _batchId;
    }

    public void setBatchId(int batchId)
    {
        _batchId = batchId;
    }

    public String getToken()
    {
        return _token;
    }

    public void setToken(String token)
    {
        _token = token;
    }

    public Integer getParticipantId()
    {
        return _participantId;
    }

    public void setParticipantId(Integer participantId)
    {
        _participantId = participantId;
    }

    public Date getCreated()
    {
        return _created;
    }

    public void setCreated(Date created)
    {
        _created = created;
    }

    public User getCreatedBy()
    {
        return _createdBy;
    }

    public void setCreatedBy(User createdBy)
    {
        _createdBy = createdBy;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }
}
