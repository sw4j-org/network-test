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
package de.sw4j.util.network.test.report;

import de.sw4j.util.network.test.common.ClientResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public final class ClientReportLoader extends Service<Void> {

    private final ObjectProperty<File> dataFileProperty = new SimpleObjectProperty<>(this, "dataFile");

    private final ObjectProperty<GridPane> gridPaneProperty = new SimpleObjectProperty<>(this, "gridPane");

    private final ObjectProperty<ProgressIndicator> progressIndicatorProperty =
            new SimpleObjectProperty<>(this, "progressIndicator");

    public void setDataFile(File dataFile) {
        this.dataFileProperty.setValue(dataFile);
    }

    public File getDataFile() {
        return this.dataFileProperty.getValue();
    }

    public ObjectProperty<File> dataFileProperty() {
        return this.dataFileProperty;
    }

    public void setGridPane(GridPane gridPane) {
        this.gridPaneProperty.setValue(gridPane);
    }

    public GridPane getGridPane() {
        return this.gridPaneProperty.getValue();
    }

    public ObjectProperty<GridPane> gridPaneProperty() {
        return this.gridPaneProperty;
    }

    public void setProgressIndicator(ProgressIndicator progressIndicator) {
        this.progressIndicatorProperty.setValue(progressIndicator);
    }

    public ProgressIndicator getProgressIndicator() {
        return this.progressIndicatorProperty.getValue();
    }

    public ObjectProperty<ProgressIndicator> progressIndicatorProperty() {
        return this.progressIndicatorProperty;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                LineChart<String, Number> connectTimeChart = createLineChart("Connect Time");
                LineChart<String, Number> serverTimeChart = createLineChart("Server Time");
                LineChart<String, Number> latencyChart = createLineChart("Latency");
                LineChart<String, Number> responseTimeChart = createLineChart("Response Time");
                ProgressIndicator progressIndicator = new ProgressIndicator();
                Platform.runLater(() -> {
                    getGridPane().add(connectTimeChart, 0, 0);
                    getGridPane().add(serverTimeChart, 1, 0);
                    getGridPane().add(latencyChart, 0, 1);
                    getGridPane().add(responseTimeChart, 1, 1);

                    progressIndicator.setProgress(-1.0);
                    progressIndicator.setOpacity(0.5);
                    progressIndicator.setMaxSize(100.0, 100.0);
                    GridPane.setConstraints(progressIndicator, 0, 0, 2, 2, HPos.CENTER, VPos.CENTER);
                    getGridPane().getChildren().add(progressIndicator);
                });

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(getDataFile()), "UTF-8"));
                XMLEventReader xmlReader = XMLInputFactory.newFactory().createXMLEventReader(reader);

                ClientResult[] data = readData(xmlReader);

                fillChart(connectTimeChart, (CategoryAxis)connectTimeChart.getXAxis(), data,
                        (ClientResult r) -> Long.valueOf(r.getConnectTime().toMillis()).doubleValue());

                fillChart(serverTimeChart, (CategoryAxis)serverTimeChart.getXAxis(), data,
                        (ClientResult r) -> Long.valueOf(r.getServerReceivedTime().toMillis()).doubleValue());

                fillChart(latencyChart, (CategoryAxis)latencyChart.getXAxis(), data,
                        (ClientResult r) -> Long.valueOf(r.getLatency().toMillis()).doubleValue());

                fillChart(responseTimeChart, (CategoryAxis)responseTimeChart.getXAxis(), data,
                        (ClientResult r) -> Long.valueOf(r.getResponseTime().toMillis()).doubleValue());

                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                });

                return null;
            }
        };
    }

    private LineChart<String, Number> createLineChart(String title) {
        CategoryAxis timeAxis = new CategoryAxis();
        NumberAxis valueAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(timeAxis, valueAxis);
        chart.setCreateSymbols(false);
        chart.setLegendSide(Side.RIGHT);
        chart.setTitle(title);

        return chart;
    }

    private void fillChart(LineChart<String, Number> chart, CategoryAxis timeAxis, ClientResult[] data,
            ToDoubleFunction<ClientResult> valueFunction) {
        DateTimeFormatter categoryFormatter = DateTimeFormatter.ofPattern("HH:mm");

        Instant[] minutes = Stream.of(data)
                .map((ClientResult t) -> t.getStart().truncatedTo(ChronoUnit.MINUTES))
                .distinct().toArray(Instant[]::new);

        String[] categoriesLabels = Stream.of(minutes).map((Instant t) -> {
            return categoryFormatter.format(t.atZone(ZoneId.systemDefault()));
        }).sorted().toArray(String[]::new);

        Map<String, Double> minTimeMap = new HashMap<>();
        Map<String, Double> maxTimeMap = new HashMap<>();
        Map<String, Double> averageTimeMap = new HashMap<>();
        Map<String, Double> p50TimeMap = new HashMap<>();
        Map<String, Double> p75TimeMap = new HashMap<>();
        Map<String, Double> p90TimeMap = new HashMap<>();
        Map<String, Double> p95TimeMap = new HashMap<>();

        for (int i = 0; i < minutes.length; i++) {
            int j = i;
            ClientResult[] inMinuteData = Stream.of(data)
                    .filter((ClientResult t) -> {
                        boolean positive = minutes[j].isBefore(t.getStart()) || minutes[j].equals(t.getStart());
                        if (j < minutes.length - 1) {
                            positive &= minutes[j + 1].isAfter(t.getStart());
                        }
                        return positive;
                    })
                    .toArray(ClientResult[]::new);

            double[] timeValues = Stream.of(inMinuteData)
                    .mapToDouble(valueFunction)
                    .sorted()
                    .toArray();

            minTimeMap.put(categoriesLabels[i], timeValues[0]);
            maxTimeMap.put(categoriesLabels[i], timeValues[timeValues.length - 1]);

            OptionalDouble averageConnectTime = DoubleStream.of(timeValues)
                    .average();
            if (averageConnectTime.isPresent()) {
                averageTimeMap.put(categoriesLabels[i], averageConnectTime.getAsDouble());
            }

            p50TimeMap.put(categoriesLabels[i], timeValues[timeValues.length * 50 / 100]);
            p75TimeMap.put(categoriesLabels[i], timeValues[timeValues.length * 75 / 100]);
            p90TimeMap.put(categoriesLabels[i], timeValues[timeValues.length * 90 / 100]);
            p95TimeMap.put(categoriesLabels[i], timeValues[timeValues.length * 95 / 100]);
        }

        timeAxis.setCategories(FXCollections.observableList(Arrays.asList(categoriesLabels)));

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

        for (String categoryLabel : categoriesLabels) {
            XYChart.Data<String, Number> minTimeData = new XYChart.Data<>(categoryLabel, minTimeMap.get(categoryLabel));
            minTimeSeries.getData().add(minTimeData);
            XYChart.Data<String, Number> maxTimeData = new XYChart.Data<>(categoryLabel, maxTimeMap.get(categoryLabel));
            maxTimeSeries.getData().add(maxTimeData);
            XYChart.Data<String, Number> averageTimeData = new XYChart.Data<>(categoryLabel,
                    averageTimeMap.get(categoryLabel));
            averageTimeSeries.getData().add(averageTimeData);
            p50TimeSeries.getData().add(
                    new XYChart.Data<>(categoryLabel, p50TimeMap.get(categoryLabel)));
            p75TimeSeries.getData().add(
                    new XYChart.Data<>(categoryLabel, p75TimeMap.get(categoryLabel)));
            p90TimeSeries.getData().add(
                    new XYChart.Data<>(categoryLabel, p90TimeMap.get(categoryLabel)));
            p95TimeSeries.getData().add(
                    new XYChart.Data<>(categoryLabel, p95TimeMap.get(categoryLabel)));
        }

        Platform.runLater(() -> {
            chart.getData().add(minTimeSeries);
            chart.getData().add(maxTimeSeries);
            chart.getData().add(averageTimeSeries);
            chart.getData().add(p50TimeSeries);
            chart.getData().add(p75TimeSeries);
            chart.getData().add(p90TimeSeries);
            chart.getData().add(p95TimeSeries);
        });
    }

    private ClientResult[] readData(XMLEventReader xmlReader) throws XMLStreamException {
        List<ClientResult> result = new LinkedList<>();
        while (xmlReader.hasNext()) {
            XMLEvent event = xmlReader.nextEvent();
            if (event.isStartElement() && "result".equals(event.asStartElement().getName().getLocalPart())) {
                StartElement resultElement = event.asStartElement();
                ClientResult.Builder clientResultBuilder = new ClientResult.Builder();

                Attribute attribute = resultElement.getAttributeByName(QName.valueOf("start"));
                if (attribute != null) {
                    clientResultBuilder.setStart(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                attribute = resultElement.getAttributeByName(QName.valueOf("connected"));
                if (attribute != null) {
                    clientResultBuilder.setConnected(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                attribute = resultElement.getAttributeByName(QName.valueOf("serverReceived"));
                if (attribute != null) {
                    clientResultBuilder.setServerReceived(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                attribute = resultElement.getAttributeByName(QName.valueOf("firstResponse"));
                if (attribute != null) {
                    clientResultBuilder.setFirstResponse(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                attribute = resultElement.getAttributeByName(QName.valueOf("completed"));
                if (attribute != null) {
                    clientResultBuilder.setCompleted(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                            attribute.getValue())));
                }

                result.add(clientResultBuilder.build());
            }
        }
        return result.toArray(new ClientResult[0]);
    }

}
