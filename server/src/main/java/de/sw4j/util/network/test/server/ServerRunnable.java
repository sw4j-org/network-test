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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public abstract class ServerRunnable implements Runnable {

    private static final Logger LOG = Logger.getLogger(ServerRunnable.class.getName());

    private final RequestHandler requestHandler;

    public ServerRunnable(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public final void run() {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        Instant received = Instant.now();
        try {
            LOG.log(Level.INFO, new StringBuilder("Received Request at ").append(dtf.format(received)).toString());
        } catch (DateTimeException dtex) {
            LOG.log(Level.WARNING, "Error while formating time.", dtex);
        }

        Writer responseWriter;
        Reader requestReader;
        try {
            responseWriter = new OutputStreamWriter(getOutputStream(), "UTF-8");
            requestReader = new InputStreamReader(getInputStream(), "UTF-8");
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while getting input or output stream.", ioex);
            try {
                closeConnection();
            } catch (IOException ioex2) {
                LOG.log(Level.INFO, "Ignoring exception during socket close.", ioex2);
            }
            return;
        }
        LOG.log(Level.FINEST, "Opened reader/writer.");

        char[] requestBuffer = new char[1024];
        StringBuilder reply = new StringBuilder();

        try {
            boolean requestRead = false;
            while (!requestRead) {
                int byteRead = requestReader.read(requestBuffer);
                if (byteRead > 0) {
                    reply.append(requestBuffer, 0, byteRead);
                }
                int endPos = reply.indexOf("\n\n.\n");
                if (endPos >= 0) {
                    reply.delete(endPos, reply.length());
                    requestRead = true;
                }
            }
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while receiving request.", ioex);
            return;
        }
        LOG.log(Level.FINEST, "Read request.");

        try {
            reply = requestHandler.handleRequest(reply, received);
        } catch (RequestHandlerException rhex) {
            LOG.log(Level.WARNING, rhex.getMessage(), rhex);
        }

        try {
            responseWriter.write(reply.toString());
            responseWriter.flush();
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while sending response.", ioex);
            return;
        }
        try {
            closeConnection();
        } catch (IOException ioex) {
            LOG.log(Level.INFO, "Ignoring exception during socket close.", ioex);
            return;
        }
        LOG.log(Level.FINEST, "Closed connection");
    }

    public abstract InputStream getInputStream() throws IOException;

    public abstract OutputStream getOutputStream() throws IOException;

    public abstract void closeConnection() throws IOException;

}
