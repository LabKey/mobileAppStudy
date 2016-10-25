package org.labkey.mobileappstudy.data;

import org.labkey.api.data.Container;

import java.util.Date;

/**
 * Representation of a database object for Participant in the mobileappstudy schema
 */
public class Participant
{
    private Integer _rowId;
    private String _appToken;
    private Integer _studyId;
    private Container _container;
    private Date _created;

    public String getAppToken()
    {
        return _appToken;
    }

    public void setAppToken(String appToken)
    {
        _appToken = appToken;
    }

    public Integer getStudyId()
    {
        return _studyId;
    }

    public void setStudyId(Integer studyId)
    {
        _studyId = studyId;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public Date getCreated()
    {
        return _created;
    }

    public void setCreated(Date created)
    {
        _created = created;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public void setRowId(Integer rowId)
    {
        _rowId = rowId;
    }
}
