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
package de.sw4j.util.network.test.report.live;

import de.sw4j.util.network.test.common.ClientResult;
import de.sw4j.util.network.test.report.common.DataProcessor;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;

/**
 * FXML Controller class
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class LiveReportController implements DataReporter {

    private final ExecutorService calculationService = Executors.newCachedThreadPool();

    @FXML
    private TextField connectField;

    @FXML
    private TextField serverField;

    @FXML
    private TextField latencyField;

    @FXML
    private TextField responseField;

    @FXML
    private LineChart<String, Number> connectTimeChart;

    @FXML
    private CategoryAxis connectTimeCategory;

    @FXML
    private LineChart<String, Number> serverTimeChart;

    @FXML
    private CategoryAxis serverTimeCategory;

    @FXML
    private LineChart<String, Number> latencyChart;

    @FXML
    private CategoryAxis latencyCategory;

    @FXML
    private LineChart<String, Number> responseTimeChart;

    @FXML
    private CategoryAxis responseTimeCategory;

    private LiveDataRunnable liveDataRunnable;

    private final SortedMap<Instant, List<ClientResult>> collectedData = new TreeMap<>();

    public void collectLiveData(LiveDataRunnable liveDataRunnable) {
        this.liveDataRunnable = liveDataRunnable;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(this.liveDataRunnable);
        Platform.runLater(() -> {
            LiveDataService dataService = new LiveDataService(liveDataRunnable, this);
            dataService.setPeriod(Duration.millis(250.0));
            dataService.start();
        });
    }

    @FXML
    void mouseScroll(ScrollEvent event) {
        final double SCALE_DELTA = 1.1;
        event.consume();
        if (event.getDeltaY() == 0) {
            return;
        }
        double scaleFactor = (event.getDeltaY() < 0) ? SCALE_DELTA : 1 / SCALE_DELTA;
        NumberAxis yAxis = (NumberAxis)((LineChart)event.getSource()).getYAxis();
        yAxis.setAutoRanging(false);
        double newUpperBound = Math.round(yAxis.getUpperBound() * scaleFactor);
        yAxis.setUpperBound(newUpperBound);
        yAxis.setTickUnit(Math.round(newUpperBound / 10));
    }

    @Override
    public void setPartialData(List<ClientResult> data) {
        calculationService.submit(() -> {
            OptionalDouble avarageConnect = data.stream()
                    .mapToDouble((ClientResult t) -> Long.valueOf(t.getConnectTime().toMillis()).doubleValue())
                    .average();
            if (avarageConnect.isPresent()) {
                setConnectAvarage(avarageConnect.getAsDouble());
            }
        });
        calculationService.submit(() -> {
            OptionalDouble averageServer = data.stream()
                    .mapToDouble((ClientResult t) -> Long.valueOf(t.getServerReceivedTime().toMillis()).doubleValue())
                    .average();
            if (averageServer.isPresent()) {
                setServerAvarage(averageServer.getAsDouble());
            }
        });
        calculationService.submit(() -> {
            OptionalDouble averageLatency = data.stream()
                    .mapToDouble((ClientResult t) -> Long.valueOf(t.getLatency().toMillis()).doubleValue())
                    .average();
            if (averageLatency.isPresent()) {
                setLatencyAvarage(averageLatency.getAsDouble());
            }
        });
        calculationService.submit(() -> {
            OptionalDouble averageResponse = data.stream()
                    .mapToDouble((ClientResult t) -> Long.valueOf(t.getResponseTime().toMillis()).doubleValue())
                    .average();
            if (averageResponse.isPresent()) {
                setResponseAvarage(averageResponse.getAsDouble());
            }
        });

        DateTimeFormatter categoryFormatter = DateTimeFormatter.ofPattern("HH:mm");

        SortedMap<Instant, List<ClientResult>> partitionedData = DataProcessor.partitionData(data,
                (ClientResult t) -> t.getStart().truncatedTo(ChronoUnit.MINUTES));
        SortedMap<Instant, List<ClientResult>> calculationData = new TreeMap<>();
        partitionedData.keySet().stream().forEach((partition) -> {
            if (collectedData.containsKey(partition)) {
                List<ClientResult> partData = collectedData.get(partition);
                partData.addAll(partitionedData.get(partition));
                calculationData.put(partition, partData);
            } else {
                calculationData.put(partition, partitionedData.get(partition));
                collectedData.put(partition, partitionedData.get(partition));
            }
        });

        SortedMap<Instant, String> categoriesLabels = new TreeMap<>();
        partitionedData.keySet().stream().forEach((interval) -> {
            categoriesLabels.put(interval, categoryFormatter.format(interval.atZone(ZoneId.systemDefault())));
        });

        calculationService.submit(() -> {
            SortedMap<Instant, DataProcessor.StatisticData> connectCalculated =
                    DataProcessor.calculateStatistics(calculationData,
                            (ClientResult r) -> Long.valueOf(r.getConnectTime().toMillis()).doubleValue());
            updateCharts(connectTimeChart, connectTimeCategory, partitionedData, categoriesLabels, connectCalculated);
        });

        calculationService.submit(() -> {
            SortedMap<Instant, DataProcessor.StatisticData> serverCalculated =
                    DataProcessor.calculateStatistics(calculationData,
                            (ClientResult r) -> Long.valueOf(r.getServerReceivedTime().toMillis()).doubleValue());
            updateCharts(serverTimeChart, serverTimeCategory, partitionedData, categoriesLabels, serverCalculated);
        });

        calculationService.submit(() -> {
            SortedMap<Instant, DataProcessor.StatisticData> latencyCalculated =
                    DataProcessor.calculateStatistics(calculationData,
                            (ClientResult r) -> Long.valueOf(r.getLatency().toMillis()).doubleValue());
            updateCharts(latencyChart, latencyCategory, partitionedData, categoriesLabels, latencyCalculated);
        });

        calculationService.submit(() -> {
            SortedMap<Instant, DataProcessor.StatisticData> responseCalculated =
                    DataProcessor.calculateStatistics(calculationData,
                            (ClientResult r) -> Long.valueOf(r.getResponseTime().toMillis()).doubleValue());
            updateCharts(responseTimeChart, responseTimeCategory, partitionedData, categoriesLabels, responseCalculated);
        });

    }

    private void setConnectAvarage(double average) {
        NumberFormat nf = new DecimalFormat("###,##0.0");
        Platform.runLater(() -> this.connectField.setText(nf.format(average)));
    }

    private void setServerAvarage(double average) {
        NumberFormat nf = new DecimalFormat("###,##0.0");
        Platform.runLater(() -> this.serverField.setText(nf.format(average)));
    }

    private void setLatencyAvarage(double average) {
        NumberFormat nf = new DecimalFormat("###,##0.0");
        Platform.runLater(() -> this.latencyField.setText(nf.format(average)));
    }

    private void setResponseAvarage(double average) {
        NumberFormat nf = new DecimalFormat("###,##0.0");
        Platform.runLater(() -> this.responseField.setText(nf.format(average)));
    }

    private void updateCharts(LineChart<String, Number> timeChart, CategoryAxis timeCategory,
            SortedMap<Instant, List<ClientResult>> partitionedData, SortedMap<Instant, String> categoriesLabels,
            SortedMap<Instant, DataProcessor.StatisticData> calculated) {
        Platform.runLater(() -> {
            ObservableList<String> connectCategories = timeCategory.getCategories();
            timeChart.setCreateSymbols(connectCategories.size() <= 1);
            for (Instant interval: partitionedData.keySet()) {
                if (connectCategories.isEmpty()) {
                    String categoryLabel = categoriesLabels.get(interval);
                    connectCategories.add(categoryLabel);

                    XYChart.Series<String, Number> minSeries = new XYChart.Series<>();
                    minSeries.setName("min");
                    minSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getMin()));
                    timeChart.getData().add(minSeries);

                    XYChart.Series<String, Number> maxSeries = new XYChart.Series<>();
                    maxSeries.setName("max");
                    maxSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getMax()));
                    timeChart.getData().add(maxSeries);

                    XYChart.Series<String, Number> averageSeries = new XYChart.Series<>();
                    averageSeries.setName("average");
                    averageSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getAverage()));
                    timeChart.getData().add(averageSeries);

                    XYChart.Series<String, Number> p50Series = new XYChart.Series<>();
                    p50Series.setName("50% percentile");
                    p50Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP50()));
                    timeChart.getData().add(p50Series);

                    XYChart.Series<String, Number> p75Series = new XYChart.Series<>();
                    p75Series.setName("75% percentile");
                    p75Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP75()));
                    timeChart.getData().add(p75Series);

                    XYChart.Series<String, Number> p90Series = new XYChart.Series<>();
                    p90Series.setName("90% percentile");
                    p90Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP90()));
                    timeChart.getData().add(p90Series);

                    XYChart.Series<String, Number> p95Series = new XYChart.Series<>();
                    p95Series.setName("95% percentile");
                    p95Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP95()));
                    timeChart.getData().add(p95Series);

                    XYChart.Series<String, Number> p99Series = new XYChart.Series<>();
                    p99Series.setName("99% percentile");
                    p99Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP99()));
                    timeChart.getData().add(p99Series);
                } else {
                    int pos;
                    if ((pos = connectCategories.indexOf(categoriesLabels.get(interval))) >= 0) {
                        XYChart.Series<String, Number> minSeries = timeChart.getData().get(0);
                        minSeries.getData().get(pos).setYValue(calculated.get(interval).getMin());

                        XYChart.Series<String, Number> maxSeries = timeChart.getData().get(1);
                        maxSeries.getData().get(pos).setYValue(calculated.get(interval).getMax());

                        XYChart.Series<String, Number> averageSeries = timeChart.getData().get(2);
                        averageSeries.getData().get(pos).setYValue(calculated.get(interval).getAverage());

                        XYChart.Series<String, Number> p50Series = timeChart.getData().get(3);
                        p50Series.getData().get(pos).setYValue(calculated.get(interval).getP50());

                        XYChart.Series<String, Number> p75Series = timeChart.getData().get(4);
                        p75Series.getData().get(pos).setYValue(calculated.get(interval).getP75());

                        XYChart.Series<String, Number> p90Series = timeChart.getData().get(5);
                        p90Series.getData().get(pos).setYValue(calculated.get(interval).getP90());

                        XYChart.Series<String, Number> p95Series = timeChart.getData().get(6);
                        p95Series.getData().get(pos).setYValue(calculated.get(interval).getP95());

                        XYChart.Series<String, Number> p99Series = timeChart.getData().get(7);
                        p99Series.getData().get(pos).setYValue(calculated.get(interval).getP99());
                    } else {
                        String categoryLabel = categoriesLabels.get(interval);
                        connectCategories.add(categoryLabel);

                        XYChart.Series<String, Number> minSeries = timeChart.getData().get(0);
                        minSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getMin()));

                        XYChart.Series<String, Number> maxSeries = timeChart.getData().get(1);
                        maxSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getMax()));

                        XYChart.Series<String, Number> averageSeries = timeChart.getData().get(2);
                        averageSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getAverage()));

                        XYChart.Series<String, Number> p50Series = timeChart.getData().get(3);
                        p50Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP50()));

                        XYChart.Series<String, Number> p75Series = timeChart.getData().get(4);
                        p75Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP75()));

                        XYChart.Series<String, Number> p90Series = timeChart.getData().get(5);
                        p90Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP90()));

                        XYChart.Series<String, Number> p95Series = timeChart.getData().get(6);
                        p95Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP95()));

                        XYChart.Series<String, Number> p99Series = timeChart.getData().get(7);
                        p99Series.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getP99()));
                    }
                }
            }
        });
    }

}
