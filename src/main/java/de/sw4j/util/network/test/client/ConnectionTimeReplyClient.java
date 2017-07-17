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
package de.sw4j.util.network.test.client;

import de.sw4j.util.network.test.server.ServerConfigType;
import java.io.File;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ConnectionTimeReplyClient {

    private static final Logger LOG = Logger.getLogger(ConnectionTimeReplyClientSocketRunner.class.getName());

    private static final String DEFAULT_CONFIGURATION_FILE_NAME = "config/client-config.xml";

    private static final double[] SERIES = {1.0, 1.6, 2.5, 4.0, 6.3};

    private ClientConfigType clientConfig;

    public static void main(String... args) throws Exception {
        ConnectionTimeReplyClient client = new ConnectionTimeReplyClient();
        client.configure();
        client.run();
    }

    public void configure() {
        String configurationFileName = DEFAULT_CONFIGURATION_FILE_NAME;
        File configurationFile = new File(configurationFileName);

        if (configurationFile.exists()) {
            try {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(
                        XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema configSchema = schemaFactory.newSchema(getClass().getClassLoader().getResource(
                        "de/sw4j/util/network/test/client/config.xsd"));
                JAXBContext jaxbContext = JAXBContext.newInstance("de.sw4j.util.network.test.client");
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                unmarshaller.setSchema(configSchema);
                JAXBElement<ClientConfigType> conf = unmarshaller.unmarshal(
                        new StreamSource(configurationFile), ClientConfigType.class);
                clientConfig = conf.getValue();
            } catch (SAXException | JAXBException ex) {
                LOG.log(Level.WARNING, new StringBuilder("Configuration file ")
                        .append(configurationFile.getAbsolutePath()).append(" cannot be parsed.").toString(), ex);
                System.exit(-1);
            }
        } else {
            LOG.log(Level.WARNING, new StringBuilder("Configuration file ").append(configurationFile.getAbsolutePath())
                    .append(" does not exist.").toString());
            System.exit(-2);
        }
    }

    public void run() throws CancellationException, ExecutionException, InterruptedException {
        if (clientConfig == null) {
            throw new IllegalStateException("Client not configured.");
        }

        ScheduledExecutorService requestExecutorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledExecutorService stopExecutorService = Executors.newSingleThreadScheduledExecutor();
        int startThreads = 100;
        int endThreads = 1000;

        boolean run = true;
        int i = 0;
        while (run) {
            double seriesNumber = i / SERIES.length;
            long threads = Math.round(SERIES[i % SERIES.length] * Math.pow(10, seriesNumber));
            i++;
            if (threads >= startThreads) {
                if (threads <= endThreads) {
                    ConnectionTimeReplyClientSocketRunner runner = new ConnectionTimeReplyClientSocketRunner(
                            threads, clientConfig.getTcpServerPort());
                    final ScheduledFuture future = requestExecutorService.scheduleAtFixedRate(
                            runner, 0, 10, TimeUnit.SECONDS);
                    ScheduledFuture stopFuture = stopExecutorService.schedule(() -> {
                        future.cancel(false);
                    }, 3, TimeUnit.MINUTES);
                    stopFuture.get();
                    LOG.log(Level.INFO, new StringBuilder("Number of calls: ").append(runner.getNumberCalls()).append("\n")
                            .append("Number of connections: ").append(runner.getNumberCalls() * threads).append("\n")
                            .toString());
                } else {
                    run = false;
                }
            }
        }

        requestExecutorService.shutdown();
        stopExecutorService.shutdown();
    }

}
