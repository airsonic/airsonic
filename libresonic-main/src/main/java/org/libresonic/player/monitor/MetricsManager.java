package org.libresonic.player.monitor;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.TimeUnit;

/**
 * Created by remi on 17/01/17.
 */
public class MetricsManager {

    // Main metrics registry
    private static final MetricRegistry metrics = new MetricRegistry();

    // Potential metrics reporters
    private static JmxReporter reporter;

    static {
        reporter = JmxReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start();
    }

    /**
     * Creates a {@link Timer} whose name is based on a class name and a
     * qualified name.
     * @param clazz
     * @param name
     * @return
     */
    public static Timer timer(Class clazz, String name) {
        return new TimerBuilder().timer(clazz,name);
    }

    /**
     * Creates a {@link Timer} whose name is based on an object's class name and a
     * qualified name.
     * @param ref
     * @param name
     * @return
     */
    public static Timer timer(Object ref, String name) {
        return timer(ref.getClass(),name);
    }

    /**
     * Initiate a {@link TimerBuilder} using a condition.
     * If the condition is false, a void {@link Timer} will finally be built thus
     * no timer will be registered in the Metrics registry.
     *
     * @param ifTrue
     * @return
     */
    public static TimerBuilder condition(boolean ifTrue) {
        if (ifTrue == false) {
            return conditionFalseTimerBuilderSingleton;
        }
        return new TimerBuilder();
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
    private static final ConditionFalseTimerBuilder conditionFalseTimerBuilderSingleton = new ConditionFalseTimerBuilder();

    private static class NullTimer extends Timer {

        protected NullTimer(com.codahale.metrics.Timer.Context timerContext) {
            super(timerContext);
        }

        @Override
        public void close()  {
            // Does nothing
        }
    }

    private static class ConditionFalseTimerBuilder extends TimerBuilder {
        @Override
        public Timer timer(Class clazz, String name) {
            return nullTimerSingleton;
        }
    }

}
