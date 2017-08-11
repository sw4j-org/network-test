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
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ClientReportFX extends Application implements ApplicationShutdown {

    private static final Logger LOG = Logger.getLogger(ClientReportFX.class.getName());

    private ClientResultAcceptor acceptor;

    private MainWindowController controller;

    private static final String DEFAULT_CONFIGURATION_FILE_NAME = "etc/report-config.xml";

    private ReportConfigType reportConfig = new ReportConfigType();

    private File configurationFile;

    public static void main(String[] args) {
        if (System.getProperty("app.home") == null) {
            System.setProperty("app.home", new File("").getAbsolutePath());
        }
        launch(args);
    }

    @Override
    public void init() throws IOException {
        CommandLine cl = parseCommandLine(getParameters().getRaw().toArray(new String[0]));

        String configurationFileName = cl.getOptionValue("conf");
        if (configurationFileName == null) {
            configurationFileName = DEFAULT_CONFIGURATION_FILE_NAME;
        }
        this.configurationFile = new File(configurationFileName);
        if (!this.configurationFile.isAbsolute()) {
            this.configurationFile = new File(System.getProperty("app.home"), configurationFileName);
        }

        if (this.configurationFile.exists() && this.configurationFile.length() > 0L) {
            try {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema configSchema = schemaFactory.newSchema(getClass().getClassLoader().getResource(
                        "de/sw4j/util/network/test/report/config.xsd"));
                JAXBContext jaxbContext = JAXBContext.newInstance("de.sw4j.util.network.test.report");
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                unmarshaller.setSchema(configSchema);
                JAXBElement<ReportConfigType> conf = unmarshaller.unmarshal(
                        new StreamSource(this.configurationFile), ReportConfigType.class);
                this.reportConfig = conf.getValue();

            } catch (SAXException | JAXBException ex) {
                LOG.log(Level.WARNING, new StringBuilder("Configuration file ")
                        .append(this.configurationFile.getAbsolutePath()).append(" cannot be parsed.").toString(), ex);
                Platform.exit();
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainWindowController.class.getResource("MainWindow.fxml"));
        Parent root = loader.load();

        this.controller = loader.getController();
        controller.setWindow(stage);
        controller.setApplicationShutdown(this);
        controller.setReportConfig(reportConfig);

        Scene scene = new Scene(root);

        stage.setTitle("Network Test Reporting");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        stage.setOnCloseRequest((WindowEvent event) -> {
            ClientReportFX.this.shutdown();
        });

        if (this.reportConfig.getListenPort() != null) {
            this.acceptor = new ClientResultAcceptor(this.reportConfig.getListenPort());
            controller.liveData(acceptor);
        }

    }

    private CommandLine parseCommandLine(String... args) {
        Options options = new Options();
        options.addOption(Option
                .builder("c")
                .longOpt("conf")
                .argName("file")
                .hasArg()
                .desc("The configuration file to be read.")
                .build());

        CommandLineParser clParser = new DefaultParser();
        CommandLine cl = new CommandLine.Builder().build();
        try {
            cl = clParser.parse(options, args);
        } catch (ParseException pex) {
            LOG.log(Level.INFO, "Cannot read configuration file, using default values.", pex);
        }
        return cl;
    }

    @Override
    public void shutdown() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema configSchema = schemaFactory.newSchema(getClass().getClassLoader().getResource(
                    "de/sw4j/util/network/test/report/config.xsd"));
            JAXBContext jaxbContext = JAXBContext.newInstance("de.sw4j.util.network.test.report");
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setSchema(configSchema);

            if (!configurationFile.exists()) {
                configurationFile.getParentFile().mkdirs();
            }
            JAXBElement<ReportConfigType> conf = new JAXBElement<>(
                    new QName("http://www.sw4j.de/schema/util/network/test/server", "reportConfig"),
                    ReportConfigType.class, this.reportConfig);
            marshaller.marshal(conf, configurationFile);
        } catch (SAXException | JAXBException ex) {
            LOG.log(Level.WARNING, new StringBuilder("Configuration file ")
                    .append(this.configurationFile.getAbsolutePath()).append(" cannot be written.").toString(), ex);
            Platform.exit();
        }

        controller.shutdown();
        Platform.exit();
    }

}
