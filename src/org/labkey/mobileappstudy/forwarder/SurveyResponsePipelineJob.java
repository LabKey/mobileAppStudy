/*
 * Copyright (c) 2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.mobileappstudy.forwarder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthenticationException;
import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.mobileappstudy.MobileAppStudyManager;
import org.labkey.mobileappstudy.data.SurveyResponse;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SurveyResponsePipelineJob extends PipelineJob
{
    private static final String FORWARD_JSON_FORMAT = "{\"type\": \"SurveyResponse\", \"metadata\": {\"activityid\": \"%1$s\", \"version\": \"%2$s\"}, \"token\": \"%3$s\", \"data\": %4$s }";

    private static volatile Cache<GUID, String> authTokenCache = CacheManager.getCache(100, TimeUnit.HOURS.toMillis(1), "Mobile app forwarding tokens");

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
        Container container = getContainer();

        Collection<SurveyResponse> responses = MobileAppStudyManager.get().getResponsesByStatus(SurveyResponse.ResponseStatus.PROCESSED, container);
        if (responses.size() == 0)
        {
            info("No responses to forward");
            this.setStatus(TaskStatus.complete);
            return;
        }

        Forwarder forwarder = getForwarder(container);
        if (forwarder == null)
            return;

        String url = forwarder.getForwardingEndpoint();
        debug(String.format("Forwarding %1$s response(s) to: %2$s", responses.size(), url));
        for (SurveyResponse response : responses)
        {
            try
            {
                if (forwarder.makeRequest(response) == TaskStatus.error)
                {
                    //Error handled within request method
                    setStatus(TaskStatus.error);
                    return;
                }
            }
            catch (Throwable e)
            {
                this.setStatus(TaskStatus.error);
                error(String.format("Failed forwarding responseId [%1$s] with: %2$s", response.getRowId(), e.getLocalizedMessage()), e);
                MobileAppStudyManager.get().setForwardingJobUnsucessful(container);
                return;
            }
        }

        info(String.format("Forwarding completed. %1$s response(s) sent to %2$s.", responses.size(), url));
        this.setStatus(TaskStatus.complete);
    }

    private Forwarder getForwarder(Container container)
    {
        switch (ForwarderProperties.getForwardingType(container))
        {
            case OAuth:
                return new OAuthForwarder(container);
            case Basic:
                return new BasicAuthForwarder(container);
            default:
            case Disabled:
                //Sanity Check
                info("Forwarding not enabled. Please verify configuration for this container.");
                this.setStatus(TaskStatus.error);
                return null;
        }
    }

    private abstract class Forwarder
    {
        protected final Container container;
        protected final Map<String, String> properties;
        protected HttpClient client;

        public Forwarder(Container container)
        {
            this.container = container;
            this.properties = Collections.unmodifiableMap(new ForwarderProperties().getForwarderConnection(container));
        }

        public abstract TaskStatus makeRequest(SurveyResponse response) throws Exception;
        public abstract String getForwardingEndpoint();

        public TaskStatus handleError(String msg)
        {
            error(msg);
            MobileAppStudyManager.get().setForwardingJobUnsucessful(container);
            return TaskStatus.error;
        }

        protected String getRequestBody(SurveyResponse response)
        {
            String token = MobileAppStudyManager.get().getEnrollmentToken(response.getContainer(), response.getParticipantId());
            return String.format(FORWARD_JSON_FORMAT, response.getActivityId(), response.getSurveyVersion(), token, response.getData());
        }
    }

    private class BasicAuthForwarder extends Forwarder
    {
        BasicAuthForwarder(Container container)
        {
            super(container);
        }

        protected HttpClient getClient()
        {
            String username = properties.get(ForwarderProperties.USER_PROPERTY_NAME);
            String pass = properties.get(ForwarderProperties.PASSWORD_PROPERTY_NAME);

            if (client == null)
            {
                client = HttpClient.newBuilder()
                        .authenticator(getPasswordAuthenticator(username, pass))
                        .build();
            }
            return client;
        }

        @Override
        public TaskStatus makeRequest(SurveyResponse response) throws Exception
        {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getForwardingEndpoint()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(getRequestBody(response)))
                    .build();

            var httpResponse = getClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || 300 < httpResponse.statusCode())
                return handleError(String.format("Stopping forwarding job. ResponseId [%1$s] received error response %2$s:\n%3$s", response.getRowId(), httpResponse.statusCode(), httpResponse.body()));

            info(String.format("Successfully forwarded response [%1$s].", response.getRowId()));
            MobileAppStudyManager.get().updateProcessingStatus(getUser(), response.getRowId(), SurveyResponse.ResponseStatus.FORWARDED);

            return TaskStatus.running;
        }

        @Override
        public String getForwardingEndpoint()
        {
            return properties.get(ForwarderProperties.URL_PROPERTY_NAME);
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

    private class OAuthForwarder extends Forwarder
    {
        private static final int MAX_RETRIES = 1;
        protected HttpClient tokenclient;

        public OAuthForwarder(Container container)
        {
            super(container);
        }


        @Override
        public TaskStatus makeRequest(SurveyResponse response) throws Exception
        {
            return makeRequest(response, 0);
        }

        private TaskStatus makeRequest(SurveyResponse response, int retries) throws Exception
        {
            if (retries >= MAX_RETRIES)
                return handleError("Exceeded max retry attempts");

            String authToken = authTokenCache.get(container.getEntityId());
            if (StringUtils.isBlank(authToken))
                authToken = requestNewAuthToken();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getForwardingEndpoint()))
//                    .header(HttpHeaders.AUTHORIZATION, Base64.getEncoder().encodeToString(("Bearer " + authToken).getBytes(StringUtilsLabKey.DEFAULT_CHARSET)))
                    .header(properties.get(ForwarderProperties.TOKEN_HEADER), "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(getRequestBody(response)))
                    .build();

            var httpResponse = getClient().send(req, HttpResponse.BodyHandlers.ofString());
            int statusCode = httpResponse.statusCode();

            if (retryForwarding(statusCode)) {
                authTokenCache.remove(container.getEntityId());
                debug("Request was unauthorized. Clearing token, and attempting retry.");
                return makeRequest(response, retries + 1);
            }
            else if (statusCode < 200 || 300 < statusCode)
            {
                return handleError(String.format("Stopping forwarding job. ResponseId [%1$s] received error response %2$s:\n%3$s", response.getRowId(), httpResponse.statusCode(), httpResponse.body()));
            }
            else
            {
                info(String.format("Successfully forwarded response [%1$s].", response.getRowId()));
                MobileAppStudyManager.get().updateProcessingStatus(getUser(), response.getRowId(), SurveyResponse.ResponseStatus.FORWARDED);
                return TaskStatus.running;
            }
        }

        private boolean retryForwarding(int statusCode)
        {
            return statusCode == 401; //Unauthorized implies token is invalid/expired, so retry after getting new auth token
        }

        protected HttpClient getClient()
        {
            if (client == null)
            {
                client = HttpClient.newBuilder()
                        .build();
            }

            return client;
        }

        @Override
        public String getForwardingEndpoint()
        {
            return properties.get(ForwarderProperties.OAUTH_URL);
        }
        private String getTokenURL()
        {
            return properties.get(ForwarderProperties.TOKEN_REQUEST_URL);
        }

        private String requestNewAuthToken() throws IOException, InterruptedException, AuthenticationException
        {
            info("Requesting new auth token.");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getTokenURL()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            var httpResponse = getClient().send(req, HttpResponse.BodyHandlers.ofString());
            int statusCode = httpResponse.statusCode();

            if (statusCode < 200 || 300 < statusCode)
            {
                MobileAppStudyManager.get().setForwardingJobUnsucessful(container);
                throw new AuthenticationException(String.format("Unable to obtain new authentication token. Token request status code [%1$s]. Please check forwarding configuration.", statusCode));
            }

            info("Successfully obtained new auth token.");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonMap = mapper.readValue(httpResponse.body(), new TypeReference<Map<String, String>>(){});
            String token = jsonMap.getOrDefault(properties.get(ForwarderProperties.TOKEN_FIELD), "");

            //Update token cache, and return token for use
            authTokenCache.put(getContainer().getEntityId(), token);
            return token;
        }
    }
}
