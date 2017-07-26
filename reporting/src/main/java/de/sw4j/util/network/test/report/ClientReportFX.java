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

import java.io.IOException;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ClientReportFX extends Application {

    private static final Logger LOG = Logger.getLogger(ClientReportFX.class.getName());

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("ClientReport.fxml"));
        Parent root = loader.load();

        ClientReportController controller = loader.getController();

//        controller.setConnectTimes();

//        Parent root = FXMLLoader.load(getClass().getResource("ClientReport.fxml"));

//        stage.setTitle("");
//        List<String> parameters = getParameters().getRaw();
//
//        System.out.println(parameters.size());
//        for (String parameterKey: parameters) {
//            System.out.println(parameterKey);
//        }
//
//        CategoryAxis xAxis = new CategoryAxis();
//        xAxis.setLabel("Time");
//        xAxis.setTickLabelRotation(-90.0);
//
//        NumberAxis yAxis = new NumberAxis();
//        yAxis.setTickUnit(1.0);
//
//        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
//
//        lineChart.setTitle("Connect Time");
//        lineChart.setCreateSymbols(false);
//
//        XYChart.Series<String, Number> series = new XYChart.Series<>();
//        series.setName("Connect Time");
//        series.getData().add(new XYChart.Data<>("09:00", 20, 2));
//        series.getData().add(new XYChart.Data<>("09:01", 22, 1));
//        series.getData().add(new XYChart.Data<>("09:02", 18, 2));
//        series.getData().add(new XYChart.Data<>("09:03", 19, 2));
//        series.getData().add(new XYChart.Data<>("09:04", 21, 2));
//        series.getData().add(new XYChart.Data<>("09:05", 30, 2));
//        series.getData().add(new XYChart.Data<>("09:06", 29, 2));
//        series.getData().add(new XYChart.Data<>("09:07", 35, 2));
//        series.getData().add(new XYChart.Data<>("09:08", 33, 2));
//        series.getData().add(new XYChart.Data<>("09:09", 34, 2));
//        series.getData().add(new XYChart.Data<>("09:10", 32, 2));
//        series.getData().add(new XYChart.Data<>("09:11", 30, 2));
//        series.getData().add(new XYChart.Data<>("09:12", 33, 2));
//        series.getData().add(new XYChart.Data<>("09:13", 32, 2));
//        series.getData().add(new XYChart.Data<>("09:14", 30, 2));
//        series.getData().add(new XYChart.Data<>("09:15", 28, 2));
//        series.getData().add(new XYChart.Data<>("09:16", 29, 2));
//        series.getData().add(new XYChart.Data<>("09:17", 27, 2));
//        series.getData().add(new XYChart.Data<>("09:18", 25, 2));
//        series.getData().add(new XYChart.Data<>("09:19", 24, 2));
//        series.getData().add(new XYChart.Data<>("09:20", 25, 2));
//        series.getData().add(new XYChart.Data<>("09:21", 23, 2));
//        series.getData().add(new XYChart.Data<>("09:22", 20, 2));
//        series.getData().add(new XYChart.Data<>("09:23", 21, 2));
//        series.getData().add(new XYChart.Data<>("09:24", 23, 2));
//        series.getData().add(new XYChart.Data<>("09:25", 22, 2));
//        series.getData().add(new XYChart.Data<>("09:26", 21, 2));
//        series.getData().add(new XYChart.Data<>("09:27", 25, 2));
//        series.getData().add(new XYChart.Data<>("09:28", 24, 2));
//        series.getData().add(new XYChart.Data<>("09:29", 20, 2));
//
        Scene scene = new Scene(root);
//        lineChart.getData().add(series);
//
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

}
