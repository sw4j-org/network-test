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

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
    private TextField liveConnect;

    @FXML
    private TextField liveServer;

    @FXML
    private TextField liveLatency;

    @FXML
    private TextField liveResponse;

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

            ClientReportLoader loader = new ClientReportLoader();
            loader.setDataFile(dataFile);
            loader.setGridPane(gridPane);
            loader.start();
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

}
