/*
 * Copyright (C) 2017 Uwe Plonus
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.sw4j.util.network.test.report.common;

import de.sw4j.util.network.test.common.ClientResult;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public final class DataProcessor {

    private DataProcessor() {}

    public static List<Instant> listInstants(List<ClientResult> data,
            Function<ClientResult, Instant> toInstantFunction) {
        List<Instant> instants = data.stream().map(toInstantFunction).distinct().collect(Collectors.toList());
        return instants;
    }

    public static SortedMap<Instant, List<ClientResult>> partitionData(List<ClientResult> data,
            Function<ClientResult, Instant> timeAggregateFunction) {
        List<Instant> intervals = listInstants(data, timeAggregateFunction);
        SortedMap<Instant, List<ClientResult>> partitions = new TreeMap<>();

        for (int i = 0; i < intervals.size(); i++) {
            final int j = i;
            List<ClientResult> dataInInterval = data.stream().filter((ClientResult t) -> {
                boolean inInterval = ! t.getStart().isBefore(intervals.get(j));
                if (j < intervals.size() - 1) {
                    inInterval &= t.getStart().isBefore(intervals.get(j + 1));
                }
                return inInterval;
            }).collect(Collectors.toList());

            partitions.put(intervals.get(i), dataInInterval);
        }

        return partitions;
    }

    public static SortedMap<Instant, StatisticData> calculateStatistics(Map<Instant, List<ClientResult>> data,
            ToDoubleFunction<ClientResult> valueFunction) {
        SortedMap<Instant, StatisticData> result = new TreeMap<>();
        data.keySet().stream().forEach((interval) -> {
            double[] timeValues = data.get(interval).stream()
                    .mapToDouble(valueFunction)
                    .sorted()
                    .toArray();
            OptionalDouble averageTime = DoubleStream.of(timeValues)
                    .average();
            result.put(interval, new StatisticData(timeValues[0], timeValues[timeValues.length - 1],
                    averageTime.orElse(0.0), timeValues[timeValues.length * 50 / 100],
                    timeValues[timeValues.length * 75 / 100], timeValues[timeValues.length * 90 / 100],
                    timeValues[timeValues.length * 95 / 100], timeValues[timeValues.length * 99 / 100]));
        });
        return result;
    }


    public static class StatisticData {

        private final double min;

        private final double max;

        private final double average;

        private final double p50;

        private final double p75;

        private final double p90;

        private final double p95;

        private final double p99;

        public StatisticData(double min, double max, double average, double p50, double p75, double p90, double p95,
                double p99) {
            this.min = min;
            this.max = max;
            this.average = average;
            this.p50 = p50;
            this.p75 = p75;
            this.p90 = p90;
            this.p95 = p95;
            this.p99 = p99;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getAverage() {
            return average;
        }

        public double getP50() {
            return p50;
        }

        public double getP75() {
            return p75;
        }

        public double getP90() {
            return p90;
        }

        public double getP95() {
            return p95;
        }

        public double getP99() {
            return p99;
        }

    }

}
