package org.labkey.mobileappstudy.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.security.User;
import org.labkey.mobileappstudy.MobileAppStudyManager;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SurveyResponse
{
    private static final Logger logger = Logger.getLogger(SurveyResponse.class);
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    public enum ResponseStatus {

        /** list order doesn't matter, but don't change id's unless you also update mobileappstudy.response.status **/
        PENDING(0, "Pending"),
        PROCESSED(1, "Processed"),
        ERROR(2, "Error");

        private final int pkId;
        private final String displayText;

        ResponseStatus(int pkId, String displayText)
        {
            this.pkId = pkId;
            this.displayText = displayText;
        }

        public String getDisplayText()
        {
            return displayText;
        }

        public final int getPkId()
        {
            return pkId;
        }


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

    public SurveyResponse()
    {
    }

    public SurveyResponse(String participantId, String response, String surveyId, String version)
    {
        setStatus(SurveyResponse.ResponseStatus.PENDING);
        setAppToken(participantId);
        setResponse(response);
        setSurveyVersion(version);
        setSurveyId(surveyId);
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

    public void shred(@Nullable User user)
    {
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            MobileAppStudyManager studyManager = MobileAppStudyManager.get();
            // first check for the survey list
            ListDefinition listDef = ListService.get().getList(getContainer(), getSurveyId());
            if (listDef == null)
            {
                logProcessingError(user, "Invalid surveyId (" + getSurveyId() + ") for container " + getContainer().getName());
                return;
            }
            mapper.setDateFormat(DATE_TIME_FORMAT);
            Response r = mapper.readValue(getResponse(), Response.class);
        }
        catch (IOException e)
        {
            logProcessingError(user, e);
            MobileAppStudyManager.get().updateProcessingStatus(user, getRowId(), ResponseStatus.ERROR, e.getMessage());
        }

    }

    private void logProcessingError(@Nullable User user, @NotNull String message)
    {
        logProcessingError(user, message, null);
    }

    private void logProcessingError(@Nullable User user, @NotNull Exception e)
    {
        logProcessingError(user, e.getMessage(), e);
    }

    private void logProcessingError(@Nullable User user, @Nullable String message, @Nullable Exception e)
    {
        if (message == null)
            message = "Unable to process response for survey " + getSurveyId() + " in container " + getContainer().getName();

        logger.error(message, e);
        MobileAppStudyManager.get().updateProcessingStatus(user, getRowId(), ResponseStatus.ERROR, message);
    }
}
