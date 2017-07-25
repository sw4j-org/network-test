/*
 * Copyright (C) 2017 plonus
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
import java.util.ResourceBundle;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * FXML Controller class
 *
 * @author plonus
 */
public class ClientReportController implements Initializable {

    private static final Logger LOG = Logger.getLogger(ClientReportController.class.getName());

    @FXML
    private TabPane resultTabPane;

    @FXML
    private Tab liveTab;

    @FXML
    private LineChart<?, ?> connectTimeChart;

    @FXML
    private CategoryAxis connectTimeTimeAxis;

    @FXML
    private LineChart<?, ?> serverTimeChart;

    @FXML
    private LineChart<?, ?> latencyChart;

    @FXML
    private LineChart<?, ?> responseTimeChart;

    @FXML
    private CategoryAxis responseTimeAxis;

    private Stage stage;

    public void setData(String fileName) {
        System.out.println(fileName);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void closeApplication(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    void loadData(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Data");
        File dataFile = fc.showOpenDialog(stage);
        if (dataFile != null && dataFile.canRead()) {
            try {
                Tab tab = new Tab(dataFile.getName());
                tab.setClosable(true);
                GridPane gridPane = new GridPane();
                ColumnConstraints columnConstraints = new ColumnConstraints(100, 100, Double.MAX_VALUE,
                        Priority.SOMETIMES, HPos.LEFT, true);
                gridPane.getColumnConstraints().addAll(columnConstraints, columnConstraints);
                RowConstraints rowConstraints = new RowConstraints(100, 100, Double.MAX_VALUE,
                        Priority.SOMETIMES, VPos.TOP, true);
                gridPane.getRowConstraints().addAll(rowConstraints, rowConstraints);

                tab.setContent(gridPane);
                this.resultTabPane.getTabs().add(tab);
                this.resultTabPane.getSelectionModel().select(tab);

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(dataFile), "UTF-8"));
                XMLEventReader xmlReader = XMLInputFactory.newFactory().createXMLEventReader(reader);

                ClientResult[] data = readData(xmlReader);

                gridPane.add(createLineChart("Connect Time", data,
                        (ClientResult r) -> Long.valueOf(r.getConnectTime().toMillis()).doubleValue()), 0, 0);

                gridPane.add(createLineChart("Server Time", data,
                        (ClientResult r) -> Long.valueOf(r.getServerReceivedTime().toMillis()).doubleValue()), 1, 0);

                gridPane.add(createLineChart("Latency", data,
                        (ClientResult r) -> Long.valueOf(r.getLatency().toMillis()).doubleValue()), 0, 1);

                gridPane.add(createLineChart("Response Time", data,
                        (ClientResult r) -> Long.valueOf(r.getResponseTime().toMillis()).doubleValue()), 1, 1);

            } catch (IOException | XMLStreamException ex) {
                LOG.log(Level.WARNING, "Cannot read data file.", ex);
            }
        }
    }

    private LineChart<String, Number> createLineChart(String title, ClientResult[] data,
            ToDoubleFunction<ClientResult> valueFunction) {
        CategoryAxis timeAxis = new CategoryAxis();
        NumberAxis valueAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(timeAxis, valueAxis);
        chart.setCreateSymbols(false);
        chart.setLegendSide(Side.RIGHT);
        chart.setTitle(title);

        fillChart(chart, timeAxis, data, valueFunction);

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

        chart.getData().clear();
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

        chart.getData().add(minTimeSeries);
        chart.getData().add(maxTimeSeries);
        chart.getData().add(averageTimeSeries);
        chart.getData().add(p50TimeSeries);
        chart.getData().add(p75TimeSeries);
        chart.getData().add(p90TimeSeries);
        chart.getData().add(p95TimeSeries);
    }

    private ClientResult[] readData(XMLEventReader xmlReader) throws XMLStreamException {
        List<ClientResult> result = new LinkedList<>();
        while (xmlReader.hasNext()) {
            XMLEvent event = xmlReader.nextEvent();
            if (event.isStartElement() && "result".equals(event.asStartElement().getName().getLocalPart())) {
                StartElement resultElement = event.asStartElement();
                result.add(new ClientResult.Builder()
                        .setStart(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                                resultElement.getAttributeByName(QName.valueOf("start")).getValue())))
                        .setConnected(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                                resultElement.getAttributeByName(QName.valueOf("connected")).getValue())))
                        .setServerReceived(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                                resultElement.getAttributeByName(QName.valueOf("serverReceived")).getValue())))
                        .setFirstResponse(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                                resultElement.getAttributeByName(QName.valueOf("firstResponse")).getValue())))
                        .setCompleted(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                                resultElement.getAttributeByName(QName.valueOf("completed")).getValue())))
                        .build());
            }
        }
        return result.toArray(new ClientResult[0]);
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    private static class ConnectTimeFunction implements ToDoubleFunction<ClientResult> {

        @Override
        public double applyAsDouble(ClientResult value) {
            return Long.valueOf(value.getConnectTime().toMillis()).doubleValue();
        }

    }


    private static class ResponseTimeFunction implements ToDoubleFunction<ClientResult> {

        @Override
        public double applyAsDouble(ClientResult value) {
            return Long.valueOf(value.getResponseTime().toMillis()).doubleValue();
        }

    }

}
