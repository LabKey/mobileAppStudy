package org.labkey.mobileappstudy.forwarder;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.mobileappstudy.MobileAppStudyManager;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class ForwardingScheduler
{
    private static JobDetail job = null;
    private static final Logger logger = Logger.getLogger(ForwardingScheduler.class);
    private static final int INTERVAL_MINUTES = 5;
    private static final ForwardingScheduler instance = new ForwardingScheduler();
    private static volatile Set<String> enabledContainers = new HashSet<>();

    private ForwardingScheduler()
    {
        enabledContainers = refreshEnabledContainers();
    }

    public static ForwardingScheduler get()
    {
        return instance;
    }

    public synchronized void schedule()
    {
        enabledContainers = refreshEnabledContainers();
//        refreshEnabledContainers();

        if (job == null)
        {
            job = JobBuilder.newJob(SurveyResponseForwardingJob.class)
                    .withIdentity(ForwardingScheduler.class.getCanonicalName(), ForwardingScheduler.class.getCanonicalName())
                    .usingJobData("surveyResponseForwarder", ForwardingScheduler.class.getCanonicalName())
                    .build();
        }

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(ForwardingScheduler.class.getCanonicalName(), ForwardingScheduler.class.getCanonicalName())
                .withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule().withIntervalInMinutes(INTERVAL_MINUTES))
                .forJob(job)
                .build();

        try
        {
            StdSchedulerFactory.getDefaultScheduler().scheduleJob(job, trigger);
            logger.info(String.format("SurveyResponseForwarder scheduled to run every %1$S minutes. Next runtime %2$s", INTERVAL_MINUTES, trigger.getNextFireTime()));
        }
        catch (SchedulerException e)
        {
            logger.error("Failed to schedule SurveryResponseForwarder.", e);
        }
    }

    private Set<String> refreshEnabledContainers()
    {
        Set<String> refreshedContainers = new HashSet<>();
        Collection<String> containers = MobileAppStudyManager.get().getStudyContainers();
        final ForwarderProperties properties = new ForwarderProperties();
        containers.stream().distinct().forEach(c -> {
            if (properties.isForwardingEnabled(ContainerManager.getForId(c)))
                refreshedContainers.add(c);
        });

        return refreshedContainers;
    }

    public Collection<String> enabledContainers()
    {
        return Collections.unmodifiableSet(enabledContainers);
    }

    public void enableContainer(Container c, boolean enable)
    {
        if (enable)
            enabledContainers.add(c.getId());
        else
            enabledContainers.remove(c.getId());
    }

    public boolean forwardingIsEnabled(Container c)
    {
        return enabledContainers.contains(c.getId());
    }


}
