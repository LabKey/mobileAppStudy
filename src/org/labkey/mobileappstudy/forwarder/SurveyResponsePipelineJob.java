package org.labkey.mobileappstudy.forwarder;

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.mobileappstudy.MobileAppStudyManager;
import org.labkey.mobileappstudy.data.SurveyResponse;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Map;

public class SurveyResponsePipelineJob extends PipelineJob
{
    private static final String FORWARD_JSON_FORMAT = "{\"token\": \"%1$s\", \"response\": %2$s}";

    // For serialization
    protected SurveyResponsePipelineJob()
    {}

    public SurveyResponsePipelineJob(ViewBackgroundInfo vbi, PipeRoot root)
    {
        super(null, vbi, root);
        setLogFile(new File(root.getLogDirectory(), FileUtil.makeFileNameWithTimestamp("surveyResponseForwarder", "log")));
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return String.format("Survey Response forwarding for %1$s", getContainer().getName());
    }

    @Override
    public void run()
    {
        this.setStatus(TaskStatus.running);
        Map<String, String> connProps = new ForwarderProperties().getForwarderConnection(getContainer());

        //Sanity Check
        if(!Boolean.valueOf(connProps.get(ForwarderProperties.ENABLED_PROPERTY_NAME)))
        {
            info("Forwarding not enabled. Please verify configuration for this container.");
            this.setStatus(TaskStatus.error);
            return;
        }

        Collection<SurveyResponse> responses = MobileAppStudyManager.get().getResponsesByStatus(SurveyResponse.ResponseStatus.PROCESSED, getContainer());
        if (responses.size() == 0)
        {
            info("No responses to forward");
            this.setStatus(TaskStatus.complete);
            return;
        }

        String url = connProps.get(ForwarderProperties.URL_PROPERTY_NAME);
        String username = connProps.get(ForwarderProperties.USER_PROPERTY_NAME);
        String pass = connProps.get(ForwarderProperties.PASSWORD_PROPERTY_NAME);

        debug(String.format("Forwarding %1$s response(s) to: %2$s", responses.size(), url));

        HttpClient client = HttpClient.newBuilder()
                .authenticator(getPasswordAuthenticator(username, pass))
                .build();

        for (SurveyResponse response : responses)
        {
            try
            {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(getRequestBody(response)))
                    .build();

                var httpResponse = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() < 200 || 300 < httpResponse.statusCode())
                {
                    this.setStatus(TaskStatus.error);
                    error(String.format("Stopping forwarding job. ResponseId [%1$s] received error response %2$s:\n%3$s", response.getRowId(), httpResponse.statusCode(), httpResponse.body()));
                    MobileAppStudyManager.get().setForwardingJobUnsucessful(getContainer());
                    return;
                }
                info(String.format("Successfully forwarded response [%1$s].", response.getRowId()));
                MobileAppStudyManager.get().updateProcessingStatus(getUser(), response.getRowId(), SurveyResponse.ResponseStatus.FORWARDED);
            }
            catch (Throwable e)
            {
                this.setStatus(TaskStatus.error);
                error(String.format("Failed forwarding responseId [%1$s] with: %2$s", response.getRowId(), e.getLocalizedMessage()), e);
                MobileAppStudyManager.get().setForwardingJobUnsucessful(getContainer());
                return;
            }
        }

        info(String.format("Forwarding completed. %1$s response(s) sent to %2$s.", responses.size(), url));
        this.setStatus(TaskStatus.complete);
    }

    private String getRequestBody(SurveyResponse response)
    {
        String token = MobileAppStudyManager.get().getEnrollmentToken(response.getContainer(), response.getParticipantId());
        return String.format(FORWARD_JSON_FORMAT, token, response.getData());
    }

    /**
     * Provide a basic auth authenticator
     * @param username for auth
     * @param pass for auth
     * @return java.net.Authenticator to use for basic auth with provided credentials
     */
    private Authenticator getPasswordAuthenticator(String username, String pass)
    {
        return new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(username, pass.toCharArray());
            }
        };
    }
}
