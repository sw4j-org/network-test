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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
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

    @FXML
    private TextField connectField;

    @FXML
    private TextField serverField;

    @FXML
    private TextField latencyField;

    @FXML
    private TextField responseField;

    @FXML
    private BarChart<String, Number> drops;

    @FXML
    private CategoryAxis dropCategory;

    private LiveDataRunnable liveDataRunnable;

    private final SortedMap<Instant, List<ClientResult>> collectedData = new TreeMap<>();

    private final Object dataLock = new Object();

    private List<ClientResult> partialData;

    public LiveReportController() {
        synchronized(dataLock) {
            this.partialData = new LinkedList<>();
        }
    }

    public void collectLiveData(LiveDataRunnable liveDataRunnable) {
        this.liveDataRunnable = liveDataRunnable;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(this.liveDataRunnable);
        Platform.runLater(() -> {
            LiveDataService liveDataService = new LiveDataService(liveDataRunnable, this);
            liveDataService.setPeriod(Duration.millis(250.0));
            liveDataService.start();

            ChartDataService chartDataService = new ChartDataService(this);
            chartDataService.setPeriod(Duration.seconds(2.0));
            chartDataService.start();
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
    public void addPartialData(List<ClientResult> data) {
        synchronized(dataLock) {
            this.partialData.addAll(data);
        }
        calculationService.submit(() -> {
            OptionalDouble avarageConnect = data.stream()
                    .filter((ClientResult t) -> t.getConnectTime() != null)
                    .mapToDouble((ClientResult t) -> Long.valueOf(t.getConnectTime().toMillis()).doubleValue())
                    .average();
            if (avarageConnect.isPresent()) {
                setConnectAvarage(avarageConnect.getAsDouble());
            }
        });
        calculationService.submit(() -> {
            OptionalDouble averageServer = data.stream()
                    .filter((ClientResult t) -> t.getServerReceivedTime() != null)
                    .mapToDouble((ClientResult t) -> Long.valueOf(t.getServerReceivedTime().toMillis()).doubleValue())
                    .average();
            if (averageServer.isPresent()) {
                setServerAvarage(averageServer.getAsDouble());
            }
        });
        calculationService.submit(() -> {
            OptionalDouble averageLatency = data.stream()
                    .filter((ClientResult t) -> t.getLatency()!= null)
                    .mapToDouble((ClientResult t) -> Long.valueOf(t.getLatency().toMillis()).doubleValue())
                    .average();
            if (averageLatency.isPresent()) {
                setLatencyAvarage(averageLatency.getAsDouble());
            }
        });
        calculationService.submit(() -> {
            OptionalDouble averageResponse = data.stream()
                    .filter((ClientResult t) -> t.getResponseTime() != null)
                    .mapToDouble((ClientResult t) -> Long.valueOf(t.getResponseTime().toMillis()).doubleValue())
                    .average();
            if (averageResponse.isPresent()) {
                setResponseAvarage(averageResponse.getAsDouble());
            }
        });

    }

    @Override
    public void aggregatePartialData() {
        List<ClientResult> data;
        synchronized(dataLock) {
            data = this.partialData;
            this.partialData = new LinkedList<>();
        }
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
                    DataProcessor.calculateStatistics(calculationData, (ClientResult r) -> r.getConnectTime());
            updateChart(connectTimeChart, partitionedData, categoriesLabels, connectCalculated);
        });

        calculationService.submit(() -> {
            SortedMap<Instant, DataProcessor.StatisticData> serverCalculated =
                    DataProcessor.calculateStatistics(calculationData, (ClientResult r) -> r.getServerReceivedTime());
            updateChart(serverTimeChart, partitionedData, categoriesLabels, serverCalculated);
        });

        calculationService.submit(() -> {
            SortedMap<Instant, DataProcessor.StatisticData> latencyCalculated =
                    DataProcessor.calculateStatistics(calculationData, (ClientResult r) -> r.getLatency());
            updateChart(latencyChart, partitionedData, categoriesLabels, latencyCalculated);
        });

        calculationService.submit(() -> {
            SortedMap<Instant, DataProcessor.StatisticData> responseCalculated =
                    DataProcessor.calculateStatistics(calculationData, (ClientResult r) -> r.getResponseTime());
            updateChart(responseTimeChart, partitionedData, categoriesLabels,
                    responseCalculated);
        });

        calculationService.submit(() -> {
            SortedMap<Instant, DataProcessor.DropData> dropsCalculated = DataProcessor.calculateDrops(calculationData);
            updateDrops(drops, partitionedData, categoriesLabels, dropsCalculated);
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

    private void updateChart(LineChart<String, Number> timeChart,
            SortedMap<Instant, List<ClientResult>> partitionedData, SortedMap<Instant, String> categoriesLabels,
            SortedMap<Instant, DataProcessor.StatisticData> calculated) {
        Platform.runLater(() -> {
//            ObservableList<String> connectCategories = timeCategory.getCategories();
            ObservableList<String> timeCategories = ((CategoryAxis)timeChart.getXAxis()).getCategories();
//            ObservableList<String> dropCategories = null;
//            if (dropChart != null) {
//                dropCategories = ((CategoryAxis)dropChart.getXAxis()).getCategories();
//            }
            timeChart.setCreateSymbols(timeCategories.size() <= 1);
            for (Instant interval: partitionedData.keySet()) {
                if (timeCategories.isEmpty()) {
                    String categoryLabel = categoriesLabels.get(interval);
                    timeCategories.add(categoryLabel);
//                    if (dropCategories != null) {
//                        dropCategories.add(categoryLabel);
//                    }

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

//                    if (dropChart != null) {
//                        XYChart.Series<String, Number> dropSeries = new XYChart.Series<>();
//                        dropSeries.setName("Drops");
//                        dropSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getDrops()));
//                        dropChart.getData().add(dropSeries);
//                    }
                } else {
                    int pos;
                    if ((pos = timeCategories.indexOf(categoriesLabels.get(interval))) >= 0) {
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

//                        if (dropChart != null) {
//                            XYChart.Series<String, Number> dropSeries = dropChart.getData().get(0);
//                            dropSeries.getData().get(pos).setYValue(calculated.get(interval).getDrops());
//                        }
                    } else {
                        String categoryLabel = categoriesLabels.get(interval);
                        timeCategories.add(categoryLabel);
//                        if (dropCategories != null) {
//                            dropCategories.add(categoryLabel);
//                        }

                        XYChart.Series<String, Number> minSeries = timeChart.getData().get(0);
                        minSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getMin()));

                        XYChart.Series<String, Number> maxSeries = timeChart.getData().get(1);
                        maxSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getMax()));

                        XYChart.Series<String, Number> averageSeries = timeChart.getData().get(2);
                        averageSeries.getData().add(new XYChart.Data<>(categoryLabel,
                                calculated.get(interval).getAverage()));

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

//                        if (dropChart != null) {
//                            XYChart.Series<String, Number> dropSeries = dropChart.getData().get(0);
//                            dropSeries.getData().add(new XYChart.Data<>(categoryLabel,
//                                    calculated.get(interval).getDrops()));
//                        }
                    }
                }
            }
        });
    }

    private void updateDrops(BarChart<String, Number> dropChart, SortedMap<Instant, List<ClientResult>> partitionedData,
            SortedMap<Instant, String> categoriesLabels, SortedMap<Instant, DataProcessor.DropData> calculated) {
        Platform.runLater(() -> {
            ObservableList<String> dropCategories = ((CategoryAxis)dropChart.getXAxis()).getCategories();
            for (Instant interval: partitionedData.keySet()) {
                if (dropCategories.isEmpty()) {
                    String categoryLabel = categoriesLabels.get(interval);
                    dropCategories.add(categoryLabel);

                    XYChart.Series<String, Number> connectSeries = new XYChart.Series<>();
                    connectSeries.setName("Connect");
                    connectSeries.getData().add(
                            new XYChart.Data<>(categoryLabel, calculated.get(interval).getConnect()));
                    dropChart.getData().add(connectSeries);

                    XYChart.Series<String, Number> serverSeries = new XYChart.Series<>();
                    serverSeries.setName("Server");
                    serverSeries.getData().add(new XYChart.Data<>(categoryLabel, calculated.get(interval).getServer()));
                    dropChart.getData().add(serverSeries);

                    XYChart.Series<String, Number> latencySeries = new XYChart.Series<>();
                    latencySeries.setName("Latency");
                    latencySeries.getData().add(
                            new XYChart.Data<>(categoryLabel, calculated.get(interval).getLatency()));
                    dropChart.getData().add(latencySeries);

                    XYChart.Series<String, Number> responseSeries = new XYChart.Series<>();
                    responseSeries.setName("Response");
                    responseSeries.getData().add(
                            new XYChart.Data<>(categoryLabel, calculated.get(interval).getResponse()));
                    dropChart.getData().add(responseSeries);
                } else {
                    int pos;
                    if ((pos = dropCategories.indexOf(categoriesLabels.get(interval))) >= 0) {
                        XYChart.Series<String, Number> connectSeries = dropChart.getData().get(0);
                        connectSeries.getData().get(pos).setYValue(calculated.get(interval).getConnect());

                        XYChart.Series<String, Number> serverSeries = dropChart.getData().get(1);
                        serverSeries.getData().get(pos).setYValue(calculated.get(interval).getServer());

                        XYChart.Series<String, Number> latencySeries = dropChart.getData().get(2);
                        latencySeries.getData().get(pos).setYValue(calculated.get(interval).getLatency());

                        XYChart.Series<String, Number> responseSeries = dropChart.getData().get(3);
                        responseSeries.getData().get(pos).setYValue(calculated.get(interval).getResponse());
                    } else {
                        String categoryLabel = categoriesLabels.get(interval);
                        dropCategories.add(categoryLabel);

                        XYChart.Series<String, Number> connectSeries = dropChart.getData().get(0);
                        connectSeries.getData().add(
                                new XYChart.Data<>(categoryLabel, calculated.get(interval).getConnect()));

                        XYChart.Series<String, Number> serverSeries = dropChart.getData().get(1);
                        serverSeries.getData().add(
                                new XYChart.Data<>(categoryLabel, calculated.get(interval).getServer()));

                        XYChart.Series<String, Number> latencySeries = dropChart.getData().get(2);
                        latencySeries.getData().add(
                                new XYChart.Data<>(categoryLabel, calculated.get(interval).getLatency()));

                        XYChart.Series<String, Number> responseSeries = dropChart.getData().get(3);
                        responseSeries.getData().add(
                                new XYChart.Data<>(categoryLabel, calculated.get(interval).getResponse()));
                    }
                }
            }
        });
    }

}
