package org.airsonic.player.monitor;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import org.airsonic.player.service.ApacheCommonsConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created by remi on 17/01/17.
 */
@Service
public class MetricsManager {

    @Autowired
    private ApacheCommonsConfigurationService configurationService;

    // Main metrics registry
    private static final MetricRegistry metrics = new MetricRegistry();

    private static volatile Boolean metricsActivatedByConfiguration = null;
    private static Object _lock = new Object();

    // Potential metrics reporters
    private static JmxReporter reporter;

    private void configureMetricsActivation() {
        if (configurationService.containsKey("Metrics")) {
            metricsActivatedByConfiguration = Boolean.TRUE;

            // Start a Metrics JMX reporter
            reporter = JmxReporter.forRegistry(metrics)
                    .convertRatesTo(TimeUnit.SECONDS.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();
            reporter.start();
        } else {
            metricsActivatedByConfiguration = Boolean.FALSE;
        }
    }

    private boolean metricsActivatedByConfiguration() {
        if (metricsActivatedByConfiguration == null) {
            synchronized (_lock) {
                if (metricsActivatedByConfiguration == null) {
                    configureMetricsActivation();
                }
            }
        }
        return metricsActivatedByConfiguration;
    }

    /**
     * Creates a {@link Timer} whose name is based on a class name and a
     * qualified name.
     */
    public Timer timer(Class clazz, String name) {
        if (metricsActivatedByConfiguration()) {
            return new TimerBuilder().timer(clazz, name);
        } else {
            return nullTimerSingleton;
        }
    }

    /**
     * Creates a {@link Timer} whose name is based on an object's class name and a
     * qualified name.
     */
    public Timer timer(Object ref, String name) {
        return timer(ref.getClass(),name);
    }

    /**
     * Initiate a {@link TimerBuilder} using a condition.
     * If the condition is false, a void {@link Timer} will finally be built thus
     * no timer will be registered in the Metrics registry.
     */
    public TimerBuilder condition(boolean ifTrue) {
        if (metricsActivatedByConfiguration()) {
            if (!ifTrue) {
                return conditionFalseTimerBuilderSingleton;
            }
            return new TimerBuilder();
        } else {
            return nullTimerBuilderSingleton;
        }
    }

    public void setConfigurationService(ApacheCommonsConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * A class that builds a {@link Timer}
     */
    public static class TimerBuilder {

        public Timer timer(Class clazz, String name) {
            com.codahale.metrics.Timer t = metrics.timer(MetricRegistry.name(clazz,name));
            com.codahale.metrics.Timer.Context tContext =  t.time();
            return new Timer(tContext);
        }

        public Timer timer(Object ref, String name) {
            return timer(ref.getClass(),name);
        }

    }

    /**
     * A class that holds a Metrics timer context implementing {@link AutoCloseable}
     * thus it can be used in a try-with-resources statement.
     */
    public static class Timer implements AutoCloseable {

        private com.codahale.metrics.Timer.Context timerContext;

        protected Timer(com.codahale.metrics.Timer.Context timerContext) {
            this.timerContext = timerContext;
        }

        @Override
        public void close() {
            timerContext.stop();
        }

    }


    // -----------------------------------------------------------------
    // Convenient singletons to avoid creating useless objects instances
    // -----------------------------------------------------------------
    private static final NullTimer nullTimerSingleton = new NullTimer(null);
    private static final NullTimerBuilder conditionFalseTimerBuilderSingleton = new NullTimerBuilder();
    private static final NullTimerBuilder nullTimerBuilderSingleton = new NullTimerBuilder();

    private static class NullTimer extends Timer {

        protected NullTimer(com.codahale.metrics.Timer.Context timerContext) {
            super(timerContext);
        }

        @Override
        public void close()  {
            // Does nothing
        }
    }

    private static class NullTimerBuilder extends TimerBuilder {
        @Override
        public Timer timer(Class clazz, String name) {
            return nullTimerSingleton;
        }
    }

}
