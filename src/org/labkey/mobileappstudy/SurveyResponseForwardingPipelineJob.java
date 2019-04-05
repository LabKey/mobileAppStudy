package org.labkey.mobileappstudy;

import org.apache.commons.lang3.NotImplementedException;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;

public class SurveyResponseForwardingPipelineJob extends PipelineJob
{
    public SurveyResponseForwardingPipelineJob(ViewBackgroundInfo vbi, PipeRoot root)
    {
        throw new NotImplementedException("Haven't gotten to this yet: SurveyResponseForwardingPipelineJob");
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return null;
    }
}
