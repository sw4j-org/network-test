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
package de.sw4j.util.network.test.report.file;

import de.sw4j.util.network.test.common.ClientResult;
import de.sw4j.util.network.test.report.common.DataProcessor;
import de.sw4j.util.network.test.report.xml.ResultReader;
import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ProgressIndicator;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class FileReportLoader extends Service<Void> {

    private final File dataFile;

    private final LineChart<String, Number> connectTimeChart;

    private final LineChart<String, Number> serverTimeChart;

    private final LineChart<String, Number> latencyChart;

    private final LineChart<String, Number> responseTimeChart;

    private final BarChart<String, Number> dropChart;

    private final ProgressIndicator progressIndicator;

    public FileReportLoader(File dataFile, LineChart<String, Number> connectTimeChart,
            LineChart<String, Number> serverTimeChart, LineChart<String, Number> latencyChart,
            LineChart<String, Number> responseTimeChart, BarChart<String, Number> dropChart,
            ProgressIndicator progressIndicator) {
        this.dataFile = dataFile;
        this.connectTimeChart = connectTimeChart;
        this.serverTimeChart = serverTimeChart;
        this.latencyChart = latencyChart;
        this.responseTimeChart = responseTimeChart;
        this.dropChart = dropChart;
        this.progressIndicator = progressIndicator;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ExecutorService chartExecutors = Executors.newCachedThreadPool();

                ResultReader resultReader = new ResultReader(new FileInputStream(dataFile));
                resultReader.readData();
                ClientResult[] data = resultReader.getFinalResult().toArray(new ClientResult[0]);
                SortedMap<Instant, List<ClientResult>> partitionedData = DataProcessor.partitionData(
                        resultReader.getFinalResult(),
                        (ClientResult t) -> t.getStart().truncatedTo(ChronoUnit.MINUTES));


                Future connectTimeChartFuture = chartExecutors.submit(() -> {
                    fillChart(connectTimeChart, data, (ClientResult r) -> r.getConnectTime());
                });

                Future serverReceivedTimeChartFuture = chartExecutors.submit(() -> {
                    fillChart(serverTimeChart, data, (ClientResult r) -> r.getServerReceivedTime());
                });

                Future latencyChartFuture = chartExecutors.submit(() -> {
                    fillChart(latencyChart, data, (ClientResult r) -> r.getLatency());
                });

                Future responseTimeChartFuture = chartExecutors.submit(() -> {
                    fillChart(responseTimeChart, data, (ClientResult r) -> r.getResponseTime());
                });

                Future dropChartFuture = chartExecutors.submit(() -> {
                    SortedMap<Instant, DataProcessor.DropData> dropData = DataProcessor.calculateDrops(partitionedData);
                    updateDrops(dropChart, resultReader.getFinalResult(), dropData);
                });

                connectTimeChartFuture.get();
                serverReceivedTimeChartFuture.get();
                latencyChartFuture.get();
                responseTimeChartFuture.get();
                dropChartFuture.get();

                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                });

                chartExecutors.shutdown();

                return null;
            }
        };
    }

    private void fillChart(LineChart<String, Number> chart, ClientResult[] data,
            Function<ClientResult, Duration> timeFunction) {
        DateTimeFormatter categoryFormatter = DateTimeFormatter.ofPattern("HH:mm");

        SortedMap<Instant, List<ClientResult>> minutesData = DataProcessor.partitionData(Arrays.asList(data),
                (ClientResult t) -> t.getStart().truncatedTo(ChronoUnit.MINUTES));

        SortedMap<Instant, String> categoriesLabels = new TreeMap<>();
        minutesData.keySet().stream().forEach((minute) -> {
            categoriesLabels.put(minute, categoryFormatter.format(minute.atZone(ZoneId.systemDefault())));
        });

        SortedMap<Instant, DataProcessor.StatisticData> calculatedData =
                DataProcessor.calculateStatistics(minutesData, timeFunction);

        ((CategoryAxis)chart.getXAxis()).setCategories(FXCollections.observableList(
                new LinkedList<>(categoriesLabels.values())));

        XYChart.Series<String, Number> minTimeSeries = new XYChart.Series<>();
        minTimeSeries.setName("min");

        XYChart.Series<String, Number> maxTimeSeries = new XYChart.Series<>();
        maxTimeSeries.setName("max");

        XYChart.Series<String, Number> averageTimeSeries = new XYChart.Series<>();
        averageTimeSeries.setName("average");

        XYChart.Series<String, Number> p50TimeSeries = new XYChart.Series<>();
        p50TimeSeries.setName("50% percentile");

        XYChart.Series<String, Number> p75TimeSeries = new XYChart.Series<>();
        p75TimeSeries.setName("75% percentile");

        XYChart.Series<String, Number> p90TimeSeries = new XYChart.Series<>();
        p90TimeSeries.setName("90% percentile");

        XYChart.Series<String, Number> p95TimeSeries = new XYChart.Series<>();
        p95TimeSeries.setName("95% percentile");

        XYChart.Series<String, Number> p99TimeSeries = new XYChart.Series<>();
        p99TimeSeries.setName("99% percentile");

        categoriesLabels.entrySet().stream().forEach((categories) -> {
            String categoryLabel = categories.getValue();
            DataProcessor.StatisticData statisticData = calculatedData.get(categories.getKey());
            minTimeSeries.getData().add(new XYChart.Data<>(categoryLabel, statisticData.getMin()));
            maxTimeSeries.getData().add(new XYChart.Data<>(categoryLabel, statisticData.getMax()));
            averageTimeSeries.getData().add(new XYChart.Data<>(categoryLabel, statisticData.getAverage()));
            p50TimeSeries.getData().add(new XYChart.Data<>(categoryLabel, statisticData.getP50()));
            p75TimeSeries.getData().add(new XYChart.Data<>(categoryLabel, statisticData.getP75()));
            p90TimeSeries.getData().add(new XYChart.Data<>(categoryLabel, statisticData.getP90()));
            p95TimeSeries.getData().add(new XYChart.Data<>(categoryLabel, statisticData.getP95()));
            p99TimeSeries.getData().add(new XYChart.Data<>(categoryLabel, statisticData.getP99()));
        });

        Platform.runLater(() -> {
            chart.getData().add(minTimeSeries);
            chart.getData().add(maxTimeSeries);
            chart.getData().add(averageTimeSeries);
            chart.getData().add(p50TimeSeries);
            chart.getData().add(p75TimeSeries);
            chart.getData().add(p90TimeSeries);
            chart.getData().add(p95TimeSeries);
            chart.getData().add(p99TimeSeries);
        });
    }

//    private void fillChart(LineChart<String, Number> chart, ClientResult[] data,
//            Function<ClientResult, Duration> timeFunction) {
    private void updateDrops(BarChart<String, Number> dropChart, List<ClientResult> data,
            SortedMap<Instant, DataProcessor.DropData> calculated) {
        DateTimeFormatter categoryFormatter = DateTimeFormatter.ofPattern("HH:mm");

        SortedMap<Instant, List<ClientResult>> partitionedData = DataProcessor.partitionData(data,
                (ClientResult t) -> t.getStart().truncatedTo(ChronoUnit.MINUTES));

        SortedMap<Instant, String> categoriesLabels = new TreeMap<>();
        partitionedData.keySet().stream().forEach((interval) -> {
            categoriesLabels.put(interval, categoryFormatter.format(interval.atZone(ZoneId.systemDefault())));
        });

//        SortedMap<Instant, DataProcessor.StatisticData> calculatedData =
//                DataProcessor.calculateStatistics(partitionedData, timeFunction);

        ((CategoryAxis)dropChart.getXAxis()).setCategories(FXCollections.observableList(
                new LinkedList<>(categoriesLabels.values())));

        XYChart.Series<String, Number> connectSeries = new XYChart.Series<>();
        connectSeries.setName("Connect");

        XYChart.Series<String, Number> serverSeries = new XYChart.Series<>();
        serverSeries.setName("Server");

        XYChart.Series<String, Number> LatencySeries = new XYChart.Series<>();
        LatencySeries.setName("Latency");

        XYChart.Series<String, Number> responseSeries = new XYChart.Series<>();
        responseSeries.setName("Response");

        categoriesLabels.entrySet().stream().forEach((categories) -> {
            String categoryLabel = categories.getValue();
            DataProcessor.DropData dropData = calculated.get(categories.getKey());
            connectSeries.getData().add(new XYChart.Data<>(categoryLabel, dropData.getConnect()));
            serverSeries.getData().add(new XYChart.Data<>(categoryLabel, dropData.getServer()));
            LatencySeries.getData().add(new XYChart.Data<>(categoryLabel, dropData.getLatency()));
            responseSeries.getData().add(new XYChart.Data<>(categoryLabel, dropData.getResponse()));
        });

        Platform.runLater(() -> {
            dropChart.getData().add(connectSeries);
            dropChart.getData().add(serverSeries);
            dropChart.getData().add(LatencySeries);
            dropChart.getData().add(responseSeries);
        });
    }

}
