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
package org.sw4j.util.network.test.client;

import org.sw4j.util.network.test.common.ClientResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ClientResultOutputRunner implements Runnable, ResultCollector {

    private static final Logger LOG = Logger.getLogger(ClientResultOutputRunner.class.getName());

    private final XMLStreamWriter targetWriter;

    private final BlockingQueue<ClientResult> workingQueue;

    public ClientResultOutputRunner(String outputFileName) throws IOException, XMLStreamException {
        this(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFileName, true), "UTF-8")));
    }

    public ClientResultOutputRunner(File outputFile) throws IOException, XMLStreamException {
        this(new FileOutputStream(outputFile, true));
    }

    public ClientResultOutputRunner(OutputStream outputStream) throws IOException, XMLStreamException {
        this(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")));
    }

    private ClientResultOutputRunner(Writer targetWriter) throws IOException, XMLStreamException {
        this.targetWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(targetWriter);
        this.workingQueue = new LinkedBlockingQueue<>();
        this.targetWriter.writeStartDocument();
        this.targetWriter.writeCharacters("\n");
        this.targetWriter.writeStartElement("results");
        this.targetWriter.writeCharacters("\n");
    }

    @Override
    public void queueResult(ClientResult result) throws InterruptedException {
        LOG.log(Level.FINEST, "Retrieved result.");
        this.workingQueue.put(result);
        LOG.log(Level.FINEST, "Queued result.");
    }

    @Override
    public void run() {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        boolean hasMore = true;
        while (hasMore) {
            ClientResult result = null;
            try {
                LOG.log(Level.FINEST, "Waiting for result.");
                result = this.workingQueue.take();
            } catch (InterruptedException iex) {
                LOG.log(Level.INFO, "Taking from queue interrupted.", iex);
            }
            LOG.log(Level.FINEST, "Received result.");
            if (result == null || result.getStart() == null) {
                LOG.log(Level.FINEST, "Processing ends.");
                hasMore = false;
            } else {
                LOG.log(Level.FINEST, "Processing result.");
                try {
                    this.targetWriter.writeEmptyElement("result");
                    this.targetWriter.writeAttribute("start", dtf.format(result.getStart()));
                    if (result.getConnected() != null) {
                        this.targetWriter.writeAttribute("connected", dtf.format(result.getConnected()));
                    }
                    if (result.getServerReceived()!= null) {
                        this.targetWriter.writeAttribute("serverReceived", dtf.format(result.getServerReceived()));
                    }
                    if (result.getFirstResponse()!= null) {
                        this.targetWriter.writeAttribute("firstResponse", dtf.format(result.getFirstResponse()));
                    }
                    if (result.getCompleted()!= null) {
                        this.targetWriter.writeAttribute("completed", dtf.format(result.getCompleted()));
                    }
                    this.targetWriter.writeCharacters("\n");
                } catch (XMLStreamException xsex) {
                    LOG.log(Level.WARNING, "Problems writing result data set.", xsex);
                }
            }
        }
        try {
            this.targetWriter.writeEndDocument();
            this.targetWriter.flush();
            this.targetWriter.close();
        } catch (XMLStreamException xsex) {
            LOG.log(Level.WARNING, "Problems ending XML document.", xsex);
        }
    }

}
