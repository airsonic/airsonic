package org.libresonic.player.monitor;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.TimeUnit;

/**
 * Created by remi on 17/01/17.
 */
public class MetricsManager {

    private static final MetricRegistry metrics = new MetricRegistry();
    private static JmxReporter reporter;
    private static final NullTimer nullTimerSingleton = new NullTimer(null);
    private static ConditionFalseTimerBuilder conditionFalseTimerBuilderSingleton = new ConditionFalseTimerBuilder();

    static {
        reporter = JmxReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start();
    }

    public static Timer timer(Class clazz, String name) {
        return new TimerBuilder().timer(clazz,name);
    }

    public static Timer timer(Object ref, String name) {
        return timer(ref.getClass(),name);
    }

    public static TimerBuilder condition(boolean ifTrue) {
        if (ifTrue == false) {
            return conditionFalseTimerBuilderSingleton;
        }
        return new TimerBuilder();
    }

   /* public interface TimerExecutor {
        void doWithTimer() throws Exception;
    } */

    public static class TimerBuilder {

       /* public TimerBuilder condition(boolean ifTrue) {
            if (ifTrue == false) {
                theTimer = nullTimerSingleton;
            }
            return this;
        } */

        public Timer timer(Class clazz, String name) {
            com.codahale.metrics.Timer t = metrics.timer(MetricRegistry.name(clazz,name));
            com.codahale.metrics.Timer.Context tContext =  t.time();
            return new Timer(tContext);
        }

        public Timer timer(Object ref, String name) {
            return timer(ref.getClass(),name);
        }


     /*   public void exec(TimerExecutor executor) throws Exception {
            if (theTimer == null) {
                com.codahale.metrics.Timer t = metrics.timer(MetricRegistry.name(clazz, name));
                com.codahale.metrics.Timer.Context tContext = t.time();
                theTimer = new Timer(tContext);
            }

            executor.doWithTimer();

            theTimer.close();
        }
        */
    }

    private static class ConditionFalseTimerBuilder extends TimerBuilder {
        @Override
        public Timer timer(Class clazz, String name) {
            return nullTimerSingleton;
        }
    }

    /**
     *
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

    private static class NullTimer extends Timer {

        protected NullTimer(com.codahale.metrics.Timer.Context timerContext) {
            super(timerContext);
        }

        @Override
        public void close()  {
            // Does nothing
        }
    }
}
