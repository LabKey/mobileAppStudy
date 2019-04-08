package org.labkey.mobileappstudy;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.User;
import org.labkey.api.util.ConfigurationException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class SurveyResponseForwardingJob implements org.quartz.Job, Callable<String>
{
    private static final Logger logger = Logger.getLogger(SurveyResponseForwardingJob.class);
    private static volatile Set<String> unsuccessful = new HashSet<>();
    private static final Set<String> enabledContainerIds = new HashSet<>();

    public SurveyResponseForwardingJob()
    {
//        unsuccessful ;
//        enabledContainerIds ;
    }

    @Override
    public String call() throws Exception
    {
        //TODO get a task user instead, can look at Accounts Maintenance tasks
        User user = User.getSearchUser();
        Collection<Container> containers = MobileAppStudyManager.get().getContainersWithProcessedResponses();

        for(Container c : containers)
        {
            call(user, c);
        }

        return null;
    }

    public String call(User user, Container c) throws Exception
    {
        if (!new ForwarderProperties().isForwardingEnabled(c))
        {
            logger.debug(String.format("Forwarding not enabled for container [%1$s].", c.getId()));
            return null;
        }

        //Check if container was unsuccessful recently
        if (c != null && unsuccessful.contains(c.getId()))
        {
            logger.debug("Skipping survey response, because target endpoint is marked as unsuccessful.");
            return null;
        }

        logger.info(String.format("Adding pipeline job to forward responses for container [%1$s].", c.getId()));

        try
        {
            return enqueuePipelineJob(user, c);
        }
        catch (ConfigurationException e)
        {
            logger.info(e.getLocalizedMessage());
            setUnsuccessful(c);
            return null;
        }
    }

    private String enqueuePipelineJob(User user, Container c)
    {
        ViewBackgroundInfo vbi = new ViewBackgroundInfo(c, user, null);
        PipeRoot root = PipelineService.get().findPipelineRoot(c);

        if (null == root)
            throw new ConfigurationException(String.format("Invalid pipeline configuration: No pipeline root for container [%1$s]", c.getId()));

        if (!root.isValid())
            throw new ConfigurationException(String.format("Invalid pipeline root configuration: %1$s", root.getRootPath().getPath()));

        final String jobGuid;

        try
        {
            PipelineJob job = new SurveyResponseForwardingPipelineJob(vbi, root);
            logger.info(String.format("Queuing forwarder for container %1$s on [thread %2$s to %3$s]",
                    c.getName(), Thread.currentThread().getName(), PipelineService.get().toString()));
            PipelineService.get().queueJob(job);
            jobGuid = job.getJobGUID();
        }
        catch (PipelineValidationException e)
        {
            throw new RuntimeException(e);
        }

        return jobGuid;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        try
        {
            // Reset success status
            unsuccessful = new HashSet<>();
            this.call();
        }
        catch (Exception e)
        {
            logger.error("The timer triggered SurveyResponse forwarding job failed with an error: " + e.getLocalizedMessage(), e);
        }
    }

    public void setUnsuccessful(Container c)
    {
        unsuccessful.add(c.getId());
    }






















//TODO Cleanup
//    private User _user = null;
//    private Container _container = null;
//
//    public Container getContainer()
//    {
//        return _container != null ? _container : super.getContainer();
//    }
//
//    public User getUser()
//    {
//        return _user != null ? _user : super.getUser();
//    }
//
//
//    @Override
//    public URLHelper getStatusHref()
//    {
//        return null;
//    }
//
//    @Override
//    public String getDescription()
//    {
//        return "Forward processed responses to configured remote server.";
//    }
//
//    @Override
//    public void run()
//    {
//        //Query responses from table
//        Collection<SurveyResponse> responses = MobileAppStudyManager.get().getResponsesByStatus(SurveyResponse.ResponseStatus.PROCESSED, getContainer());
//        getLogger().info(String.format("Forwarding %1$s responses to configured server for %2$%s container.", responses.size(), getContainer().getName()));
//
//        if (responses.size() == 0)
//            return;
//
//        Map<String, String> connectionProperties = new ForwarderProperties().getForwarderConnection(getContainer());
//        String url = connectionProperties.get(ForwarderProperties.URL_PROPERTY_NAME);
//        URI forwarderUri = URI.create(url);
//        String username = connectionProperties.get(ForwarderProperties.USER_PROPERTY_NAME);
//        String password = connectionProperties.get(ForwarderProperties.PASSWORD_PROPERTY_NAME);
//
//        try (CloseableHttpClient httpClient = HttpClients.createDefault())
//        {
//            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
//
//            for (SurveyResponse response : responses)
//            {
//                HttpPost post = new HttpPost(forwarderUri);
//                post.addHeader(BasicScheme.authenticate());
//
//            }
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//    }
}
