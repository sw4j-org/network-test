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

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ConnectionTimeReplyRequestHandler implements RequestHandler {

    private static final Logger LOG = Logger.getLogger(ConnectionTimeReplyRequestHandler.class.getName());

    @Override
    public StringBuilder handleRequest(StringBuilder request) throws RequestHandlerException {
        return this.handleRequest(request, Instant.now());
    }

    @Override
    public StringBuilder handleRequest(StringBuilder request, Instant received) throws RequestHandlerException {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;

        int newLinePos = request.indexOf("\n");
        if (newLinePos >= 0) {
            request.delete(newLinePos, request.length());
        }
        LOG.log(Level.FINEST, "Shortened request");

        Instant sent;
        try {
            sent = Instant.from(dtf.parse(request));
        } catch (DateTimeParseException dtpex) {
            LOG.log(Level.WARNING, "Error while parsing the request.", dtpex);
            throw new RequestHandlerException("Error while parsing the request.", dtpex);
        }
        request.append("\n");

        try {
            request.append(dtf.format(received));
        } catch (DateTimeException dtex) {
            LOG.log(Level.WARNING, "Error while formating the response.", dtex);
            throw new RequestHandlerException("Error while formating the response.", dtex);
        }
        request.append("\n");

        StringBuilder log = new StringBuilder();
        log.append(request);
        Duration duration = Duration.between(sent, received);
        log.append(duration.toString());
        log.append("\n");
        LOG.log(Level.FINE, log.toString());

        return request;
    }

}
