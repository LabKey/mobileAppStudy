package org.labkey.mobileappstudy.data;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;

import java.util.Date;

/**
 * Created by iansigmon on 11/4/16.
 */
public class SurveyResponse
{
    public enum ResponseStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        ERROR;
    }

    private Integer _rowId;
    private String _response;
    private Integer _participantId;
    private String _surveyVersion;
    private String _surveyId;
    private ResponseStatus _status;
    private Date _processed;
    private User _processedBy;
    private String _errorMessage;
    private Date _created;
    private Container _container;
    private String _appToken;


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

    public String getErrorMessage()
    {
        return _errorMessage;
    }
    public void setErrorMessage(String errorMessage)
    {
        _errorMessage = errorMessage;
    }

    public User getProcessedBy()
    {
        return _processedBy;
    }
    public void setProcessedBy(User processedBy)
    {
        _processedBy = processedBy;
    }

    public Date getProcessed()
    {
        return _processed;
    }
    public void setProcessed(Date processed)
    {
        _processed = processed;
    }

    public ResponseStatus getStatus()
    {
        return _status;
    }
    public void setStatus(ResponseStatus status)
    {
        _status = status;
    }

    public String getSurveyId()
    {
        return _surveyId;
    }
    public void setSurveyId(String surveyId)
    {
        _surveyId = surveyId;
    }

    public String getSurveyVersion()
    {
        return _surveyVersion;
    }
    public void setSurveyVersion(String surveyVersion)
    {
        _surveyVersion = surveyVersion;
    }

    public Integer getParticipantId()
    {
        return _participantId;
    }
    public void setParticipantId(Integer participantId)
    {
        _participantId = participantId;
    }

    public String getResponse()
    {
        return _response;
    }
    public void setResponse(String response)
    {
        _response = response;
    }

    public Integer getRowId()
    {
        return _rowId;
    }
    public void setRowId(Integer rowId)
    {
        _rowId = rowId;
    }

    public String getAppToken()
    {
        return _appToken;
    }
    public void setAppToken(String appToken)
    {
        _appToken = appToken;
    }
}
