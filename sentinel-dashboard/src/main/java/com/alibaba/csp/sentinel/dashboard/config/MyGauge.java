package com.alibaba.csp.sentinel.dashboard.config;

import io.prometheus.client.*;
import io.prometheus.client.Gauge;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.Callable;

public class MyGauge extends SimpleCollector<Gauge.Child> implements Collector.Describable {
    private List<MetricFamilySamples> samples;
    private long time = System.currentTimeMillis();

    public List<MetricFamilySamples> getSamples() {
        return samples;
    }

    public void setSamples(List<MetricFamilySamples> samples) {
        this.samples = samples;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    MyGauge(Builder b) {
        super(b);
    }

    public static class Builder extends SimpleCollector.Builder<Builder, MyGauge> {
        @Override
        public MyGauge create() {
            return new MyGauge(this);
        }
    }

    /**
     *  Return a Builder to allow configuration of a new Gauge. Ensures required fields are provided.
     *
     *  @param name The name of the metric
     *  @param help The help string of the metric
     */
    public static Builder build(String name, String help) {
        return new Builder().name(name).help(help);
    }

    /**
     *  Return a Builder to allow configuration of a new Gauge.
     */
    public static Builder build() {
        return new Builder();
    }

    @Override
    protected Gauge.Child newChild() {
        return new Gauge.Child();
    }

    /**
     * Represents an event being timed.
     */
//    public static class Timer implements Closeable {
//        private final MyChild child;
//        private final long start;
//
//        public Timer(MyChild child, long start) {
//            super();
//            this.child = child;
//            this.start = start;
//        }
//
//        /**
//         * Set the amount of time in seconds since {@link MyChild#startTimer} was called.
//         * @return Measured duration in seconds since {@link MyChild#startTimer} was called.
//         */
//        public double setDuration() {
//            double elapsed = (MyChild.timeProvider.nanoTime() - start) / NANOSECONDS_PER_SECOND;
//            child.set(elapsed);
//            return elapsed;
//        }
//
//        /**
//         * Equivalent to calling {@link #setDuration()}.
//         */
//        @Override
//        public void close() {
//            setDuration();
//        }
//    }

//    /**
//     * The value of a single Gauge.
//     * <p>
//     * <em>Warning:</em> References to a Child become invalid after using
//     * {@link SimpleCollector#remove} or {@link SimpleCollector#clear},
//     */
//    public static class MyChild extends Gauge.Child {
//
//        private final DoubleAdder value = new DoubleAdder();
//
//        static TimeProvider timeProvider = new TimeProvider();
//
//        /**
//         * Increment the gauge by 1.
//         */
//        public void inc() {
//            inc(1);
//        }
//        /**
//         * Increment the gauge by the given amount.
//         */
//        public void inc(double amt) {
//            value.add(amt);
//        }
//        /**
//         * Decrement the gauge by 1.
//         */
//        public void dec() {
//            dec(1);
//        }
//        /**
//         * Decrement the gauge by the given amount.
//         */
//        public void dec(double amt) {
//            value.add(-amt);
//        }
//        /**
//         * Set the gauge to the given value.
//         */
//        public void set(double val) {
//            value.set(val);
//        }
//        /**
//         * Set the gauge to the current unixtime.
//         */
//        public void setToCurrentTime() {
//            set(timeProvider.currentTimeMillis() / MILLISECONDS_PER_SECOND);
//        }
//
//        /**
//         * Get the value of the gauge.
//         */
//        public double get() {
//            return value.sum();
//        }
//    }

    @Override
    public List<MetricFamilySamples> collect() {

        return samples;
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return Collections.<MetricFamilySamples>singletonList(new GaugeMetricFamily(fullname, help, labelNames));
    }

//    static class TimeProvider {
//        long currentTimeMillis() {
//            return System.currentTimeMillis();
//        }
//        long nanoTime() {
//            return System.nanoTime();
//        }
//    }
}