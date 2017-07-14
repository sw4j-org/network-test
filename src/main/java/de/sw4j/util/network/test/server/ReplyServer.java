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
package de.sw4j.util.network.test.server;

import java.io.File;
import java.io.IOException;
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
public class ReplyServer {

    private static final Logger LOG = Logger.getLogger(ReplyServer.class.getName());

    private static final String DEFAULT_CONFIGURATION_FILE_NAME = "config/server-config.xml";

    private ServerConfigType serverConfig;

    public static void main(String... args) throws Exception {
        ReplyServer server = new ReplyServer();
        server.configure();
        server.start();
    }

    public void configure() {
        String configurationFileName = DEFAULT_CONFIGURATION_FILE_NAME;
        File configurationFile = new File(configurationFileName);

        if (configurationFile.exists()) {
            try {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(
                        XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema configSchema = schemaFactory.newSchema(
                        getClass().getClassLoader().getResource(
                                "de/sw4j/util/network/test/server/config.xsd"));
                JAXBContext jaxbContext
                        = JAXBContext.newInstance("de.sw4j.util.network.test.server");
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                unmarshaller.setSchema(configSchema);
                JAXBElement<ServerConfigType> conf = unmarshaller.unmarshal(
                        new StreamSource(configurationFile), ServerConfigType.class);
                serverConfig = conf.getValue();
            } catch (SAXException | JAXBException ex) {
                LOG.log(Level.WARNING, new StringBuilder("Configuration file ")
                        .append(configurationFile.getAbsolutePath())
                        .append(" cannot be parsed.").toString(), ex);
                System.exit(-1);
            }
        } else {
            LOG.log(Level.WARNING, new StringBuilder("Configuration file ")
                    .append(configurationFile.getAbsolutePath())
                    .append(" does not exist.").toString());
            System.exit(-2);
        }
    }

    public void start() throws IOException {
        if (serverConfig == null) {
            throw new IllegalStateException("Server not configured.");
        }
        Server server = new ConnectionTimeReplySocketServer(serverConfig.getTcpPort());
        while (true) {
            ServerRunnable runnable = server.accept();
            Thread t = new Thread(runnable);
            t.start();
        }
    }

}
