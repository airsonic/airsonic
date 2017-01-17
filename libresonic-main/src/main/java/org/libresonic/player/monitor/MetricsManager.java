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

    static {
        reporter = JmxReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start();
    }


    public static TimerBuilder buildTimer(Class clazz, String name) {
        return new TimerBuilder(clazz,name);
    }

    public static TimerBuilder buildTimer(Object ref, String name) {
        return new TimerBuilder(ref.getClass(),name);
    }

    public interface TimerExecutor {
        void doWithTimer() throws Exception;
    }


    public static class TimerBuilder {
        private Timer theTimer;
        private Class clazz;
        private String name;

        public TimerBuilder() {
        }

        public TimerBuilder(Timer theTimer) {
            this.theTimer = theTimer;
        }

        public TimerBuilder(Class clazz, String name) {
            this.clazz = clazz;
            this.name = name;
        }

        public TimerBuilder condition(boolean ifTrue) {
            if (ifTrue == false) {
                theTimer = nullTimerSingleton;
            }
            return this;
        }

        public Timer timer() {
            if (theTimer == null) {
                com.codahale.metrics.Timer t = metrics.timer(MetricRegistry.name(clazz,name));
                com.codahale.metrics.Timer.Context tContext =  t.time();
                theTimer = new Timer(tContext);
            }
            return theTimer;
        }

        public void exec(TimerExecutor executor) throws Exception {
            if (theTimer == null) {
                com.codahale.metrics.Timer t = metrics.timer(MetricRegistry.name(clazz, name));
                com.codahale.metrics.Timer.Context tContext = t.time();
                theTimer = new Timer(tContext);
            }

            executor.doWithTimer();

            theTimer.close();
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

        public Timer condition() {
            return null;
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
