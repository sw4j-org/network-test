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

import de.sw4j.util.network.test.report.live.ClientResultAcceptor;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ClientReportFX extends Application implements ApplicationShutdown {

    private static final Logger LOG = Logger.getLogger(ClientReportFX.class.getName());

    private final ExecutorService serverAcceptService = Executors.newSingleThreadExecutor();

    private final ExecutorService connectionRunService = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;

    private ClientResultAcceptor acceptor;

    private MainWindowController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainWindowController.class.getResource("MainWindow.fxml"));
        Parent root = loader.load();

        this.controller = loader.getController();
        controller.setWindow(stage);
        controller.setApplicationShutdown(this);

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        stage.setOnCloseRequest((WindowEvent event) -> {
            ClientReportFX.this.shutdown();
        });

        this.acceptor = new ClientResultAcceptor(9900);
        controller.liveData(acceptor);

    }

    @Override
    public void shutdown() {
        controller.shutdown();
        Platform.exit();
    }

}
