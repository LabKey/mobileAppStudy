package org.labkey.test.data.mobileappstudy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantPropertiesResponse
{
    private Collection<ParticipantProperty> _preEnrollmentParticipantProperties;


    public void setPreEnrollmentParticipantProperties(Collection<ParticipantProperty> _participantProperties)
    {
        this._preEnrollmentParticipantProperties = _participantProperties;
    }

    public Collection<ParticipantProperty> getPreEnrollmentParticipantProperties()
    {
        return this._preEnrollmentParticipantProperties;
    }
}
