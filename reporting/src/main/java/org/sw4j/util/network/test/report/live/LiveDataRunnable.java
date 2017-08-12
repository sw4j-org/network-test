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
package org.sw4j.util.network.test.report.live;

import org.sw4j.util.network.test.report.file.FileReportController;
import org.sw4j.util.network.test.report.xml.ResultReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public abstract class LiveDataRunnable implements Runnable {

    private static final Logger LOG = Logger.getLogger(FileReportController.class.getName());

    private final ResultReader resultReader;

    public LiveDataRunnable(InputStream inputStream) throws UnsupportedEncodingException, XMLStreamException {
        this.resultReader = new ResultReader(inputStream);
    }

    @Override
    public final void run() {
        try {
            resultReader.readData();
        } catch (XMLStreamException ex) {
            LOG.log(Level.WARNING, "Error while retrieving live results.", ex);
        }
    }

    public final ResultReader getResultReader() {
        return this.resultReader;
    }

    public abstract String getTitle();

    public abstract InputStream getInputStream() throws IOException;

    public abstract OutputStream getOutputStream() throws IOException;

    public abstract void closeConnection() throws IOException;

}
