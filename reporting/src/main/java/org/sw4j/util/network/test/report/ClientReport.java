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
package org.sw4j.util.network.test.report;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ClientReport {

    private static final Logger LOG = Logger.getLogger(ClientReport.class.getName());

    public static void main(String... args) throws Exception {
        ClientReport report = new ClientReport();
        report.run(args);
    }

    public void run(String... args) throws Exception {
        ExecutorService service = Executors.newCachedThreadPool();
        ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(9900);
        while (true) {
            Socket client = serverSocket.accept();
            service.submit(() -> {
                try {
                    Reader reader = new InputStreamReader(new BufferedInputStream(client.getInputStream()));
                    char[] buf = new char[4096];
                    int charsRead = 0;
                    while ((charsRead = reader.read(buf)) > 0) {
                        LOG.log(Level.INFO, new String(buf));
                    }
                } catch (IOException ioex) {
                    LOG.log(Level.SEVERE, ioex.getMessage(), ioex);
                }
            });
        }
    }

}
