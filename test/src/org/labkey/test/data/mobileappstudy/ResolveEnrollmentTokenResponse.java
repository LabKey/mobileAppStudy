package org.labkey.test.data.mobileappstudy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolveEnrollmentTokenResponse
{
    private String _studyId;

    @SuppressWarnings("unused")
    public void setStudyId(String studyId)
    {
        _studyId = studyId;
    }

    public String getStudyId()
    {
        return _studyId;
    }
}
