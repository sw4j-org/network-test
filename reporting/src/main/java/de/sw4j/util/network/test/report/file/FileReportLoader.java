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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.ToDoubleFunction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
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

    private final CategoryAxis connectTimeCategory;

    private final LineChart<String, Number> serverTimeChart;

    private final CategoryAxis serverTimeCategory;

    private final LineChart<String, Number> latencyChart;

    private final CategoryAxis latencyCategory;

    private final LineChart<String, Number> responseTimeChart;

    private final CategoryAxis responseTimeCategory;

    private final ProgressIndicator progressIndicator;

    public FileReportLoader(File dataFile,
            LineChart<String, Number> connectTimeChart, CategoryAxis connectTimeCategory,
            LineChart<String, Number> serverTimeChart, CategoryAxis serverTimeCategory,
            LineChart<String, Number> latencyChart, CategoryAxis latencyCategory,
            LineChart<String, Number> responseTimeChart, CategoryAxis reponseTimeCategory,
            ProgressIndicator progressIndicator) {
        this.dataFile = dataFile;
        this.connectTimeChart = connectTimeChart;
        this.connectTimeCategory = connectTimeCategory;
        this.serverTimeChart = serverTimeChart;
        this.serverTimeCategory = serverTimeCategory;
        this.latencyChart = latencyChart;
        this.latencyCategory = latencyCategory;
        this.responseTimeChart = responseTimeChart;
        this.responseTimeCategory = reponseTimeCategory;
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

                Future connectTimeChartFuture = chartExecutors.submit(() -> {
                    fillChart(connectTimeChart, connectTimeCategory, data,
                            (ClientResult r) -> Long.valueOf(r.getConnectTime().toMillis()).doubleValue());
                });

                Future serverReceivedTimeChartFuture = chartExecutors.submit(() -> {
                    fillChart(serverTimeChart, serverTimeCategory, data,
                            (ClientResult r) -> Long.valueOf(r.getServerReceivedTime().toMillis()).doubleValue());
                });

                Future latencyChartFuture = chartExecutors.submit(() -> {
                    fillChart(latencyChart, latencyCategory, data,
                            (ClientResult r) -> Long.valueOf(r.getLatency().toMillis()).doubleValue());
                });

                Future responseTimeChartFuture = chartExecutors.submit(() -> {
                    fillChart(responseTimeChart, responseTimeCategory, data,
                            (ClientResult r) -> Long.valueOf(r.getResponseTime().toMillis()).doubleValue());
                });

                connectTimeChartFuture.get();
                serverReceivedTimeChartFuture.get();
                latencyChartFuture.get();
                responseTimeChartFuture.get();

                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                });

                chartExecutors.shutdown();

                return null;
            }
        };
    }

    private void fillChart(LineChart<String, Number> chart, CategoryAxis timeAxis, ClientResult[] data,
            ToDoubleFunction<ClientResult> timeAggregateFunction) {
        DateTimeFormatter categoryFormatter = DateTimeFormatter.ofPattern("HH:mm");

        SortedMap<Instant, List<ClientResult>> minutesData = DataProcessor.partitionData(Arrays.asList(data),
                (ClientResult t) -> t.getStart().truncatedTo(ChronoUnit.MINUTES));

        SortedMap<Instant, String> categoriesLabels = new TreeMap<>();
        minutesData.keySet().stream().forEach((minute) -> {
            categoriesLabels.put(minute, categoryFormatter.format(minute.atZone(ZoneId.systemDefault())));
        });

        SortedMap<Instant, DataProcessor.StatisticData> calculatedData =
                DataProcessor.calculateStatistics(minutesData, timeAggregateFunction);

        timeAxis.setCategories(FXCollections.observableList(new LinkedList<>(categoriesLabels.values())));

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

}
