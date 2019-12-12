package org.labkey.mobileappstudy.participantproperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.labkey.mobileappstudy.data.SurveyMetadata;

import java.util.Collection;
import java.util.Collections;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantPropertiesDesign
{
    private Collection<ParticipantProperty> participantProperties;
    private String message;
    private SurveyMetadata metadata;

    public void setMetadata(SurveyMetadata metadata)
    {
        this.metadata = metadata;
    }

    public SurveyMetadata getMetadata()
    {
        return this.metadata;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getMessage()
    {
        return this.message;
    }

    public void setParticipantProperties(Collection<ParticipantProperty> properties)
    {
        this.participantProperties = properties;
    }

    public Collection<ParticipantProperty> getParticipantProperties()
    {
        return Collections.unmodifiableCollection(participantProperties);
    }

    @JsonIgnore
    public boolean isValid()
    {
        return metadata != null && participantProperties != null;
    }

    @JsonIgnore
    public String getStudyVersion()
    {
        return metadata.getStudyVersion();
    }
}
