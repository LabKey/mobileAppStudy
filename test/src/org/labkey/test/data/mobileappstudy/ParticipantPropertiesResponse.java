package org.labkey.test.data.mobileappstudy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantPropertiesResponse
{
    private Collection<ParticipantProperty> _preEnrollmentParticipantProperties;

    @SuppressWarnings("unused")
    public void setPreEnrollmentParticipantProperties(Collection<ParticipantProperty> participantProperties)
    {
        _preEnrollmentParticipantProperties = participantProperties;
    }

    public Collection<ParticipantProperty> getPreEnrollmentParticipantProperties()
    {
        return _preEnrollmentParticipantProperties;
    }
}
