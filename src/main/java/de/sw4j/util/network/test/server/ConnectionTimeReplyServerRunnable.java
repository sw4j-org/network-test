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
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus
 */
public abstract class ConnectionTimeReplyServerRunnable implements Runnable {

    private static final Logger LOG = Logger.getLogger(ConnectionTimeReplyServerRunnable.class.getName());

    @Override
    public final void run() {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        Instant now = Instant.now();
        try {
            LOG.log(Level.INFO, new StringBuilder("Received Request at ").append(dtf.format(now)).toString());
        } catch (DateTimeException dtex) {
            LOG.log(Level.WARNING, "Error while formating time.", dtex);
        }

        char[] request = new char[1024];
        StringBuilder reply = new StringBuilder();
        StringBuilder log = new StringBuilder();

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

        try {
            boolean lineRead = false;
            while (!lineRead) {
                int byteRead = requestReader.read(request);
                reply.append(request, 0, byteRead);
                int newLinePos = reply.indexOf("\n");
                if (newLinePos >= 0) {
                    reply.delete(newLinePos, reply.length());
                    lineRead = true;
                }
            }
        } catch (IOException ioex) {
            LOG.log(Level.WARNING, "Error while receiving request.", ioex);
            return;
        }

        Instant sent;
        try {
            sent = Instant.from(dtf.parse(reply));
        } catch (DateTimeParseException dtpex) {
            LOG.log(Level.WARNING, "Error while parsing the request.", dtpex);
            return;
        }
        reply.append("\n");

        try {
            reply.append(dtf.format(now));
        } catch (DateTimeException dtex) {
            LOG.log(Level.WARNING, "Error while formating the response.", dtex);
            return;
        }
        reply.append("\n");

        log.append(reply);
        Duration duration = Duration.between(sent, now);
        log.append(duration.toString());
        log.append("\n");
        LOG.log(Level.FINE, log.toString());

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
    }

    public abstract InputStream getInputStream() throws IOException;

    public abstract OutputStream getOutputStream() throws IOException;

    public abstract void closeConnection() throws IOException;

}
