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

import de.sw4j.util.network.test.report.file.FileReportController;
import de.sw4j.util.network.test.report.live.ClientResultAcceptor;
import de.sw4j.util.network.test.report.live.LiveDataRunnable;
import de.sw4j.util.network.test.report.live.LiveReportController;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * FXML Controller class
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class MainWindowController {

    private static final Logger LOG = Logger.getLogger(MainWindowController.class.getName());

    private final ExecutorService serverAcceptService = Executors.newCachedThreadPool();

    private final ExecutorService connectionRunService = Executors.newCachedThreadPool();

    private ClientResultAcceptor acceptor;

    private ApplicationShutdown applicationShutdown;

    private Window window;

    private ReportConfigType reportConfig;

    @FXML
    private TabPane tabPane;

    public MainWindowController() {
    }

    public void setReportConfig(ReportConfigType reportConfig) {
        this.reportConfig = reportConfig;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public void setApplicationShutdown(ApplicationShutdown applicationShutdown) {
        this.applicationShutdown = applicationShutdown;
    }

    @FXML
    void closeApplication(ActionEvent event) {
        this.applicationShutdown.shutdown();
    }

    @FXML
    void loadData(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Data");
        File dataFile = fc.showOpenDialog(this.window);

        if (dataFile != null && dataFile.canRead()) {
            FXMLLoader loader = new FXMLLoader(FileReportController.class.getResource("FileReport.fxml"));
            Parent fileReport = null;
            try {
                fileReport = loader.load();
            } catch (IOException ioex) {
                LOG.log(Level.WARNING, "Cannot load FileReport.", ioex);
            }
            FileReportController fileReportController = loader.getController();
            if (fileReport != null) {
                Tab tab = new Tab(dataFile.getName());
                tab.setClosable(true);
                tab.setContent(fileReport);

                this.tabPane.getTabs().add(tab);
                this.tabPane.getSelectionModel().select(tab);

                fileReportController.loadData(dataFile);
            }
        }

    }

    public void shutdown() {
        if (this.acceptor != null) {
            try {
                this.acceptor.shutdown();
            } catch (IOException ioex) {
                LOG.log(Level.INFO, "Exception while shutting down acceptor.", ioex);
            }
        }
        this.serverAcceptService.shutdown();
    }

    public void liveData(ClientResultAcceptor acceptor) {
        this.acceptor = acceptor;
        this.serverAcceptService.submit(() -> {
            while (true) {
                LiveDataRunnable runnable = this.acceptor.accept();

                FXMLLoader loader = new FXMLLoader(LiveReportController.class.getResource("LiveReport.fxml"));
                Parent liveReport = null;
                try {
                    liveReport = loader.load();
                } catch (IOException ioex) {
                    LOG.log(Level.WARNING, "Cannot load FileReport.", ioex);
                }
                if (liveReport != null) {
                    LiveReportController liveReportController = loader.getController();
                    liveReportController.collectLiveData(runnable);

                    Tab tab = new Tab(runnable.getTitle());
                    tab.setClosable(true);
                    tab.setContent(liveReport);

                    Platform.runLater(() -> {
                            this.tabPane.getTabs().add(tab);
                    });
                }

                connectionRunService.submit(runnable);
            }
        });
    }

    @FXML
    void configure(ActionEvent event) {
        FXMLLoader loader = new FXMLLoader(ConfigurationController.class.getResource("Configuration.fxml"));
        DialogPane configuration = null;
        try {
            configuration = loader.load();
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Cannot load Configuration.", ioex);
        }
        if (configuration != null) {
            ConfigurationController configurationController = loader.getController();

            configurationController.setListenPort(this.reportConfig.getListenPort());

            Dialog dialog = new Dialog();
            dialog.setDialogPane(configuration);

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.APPLY) {
                    this.reportConfig.setListenPort(configurationController.getListenPort());
                }
            });
        }
    }

}
