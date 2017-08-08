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
package de.sw4j.util.network.test.report.live;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ServerSocketFactory;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Uwe Plonus &lt;u.plonus@gmail.com&gt;
 */
public class ClientResultAcceptor {

    private final ServerSocket serverSocket;

    public ClientResultAcceptor(int port) throws IOException {
        this.serverSocket = ServerSocketFactory.getDefault().createServerSocket(port);
    }

    public LiveDataRunnable accept() throws IOException, XMLStreamException {
        Socket socket = this.serverSocket.accept();
        return new LiveSocketRunnable(socket);
    }

    public void shutdown() throws IOException {
        this.serverSocket.close();
    }

}
