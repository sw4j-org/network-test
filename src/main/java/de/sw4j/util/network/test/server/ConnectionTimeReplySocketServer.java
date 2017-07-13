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
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Uwe Plonus &lt;uplonus@gmail.com&gt;
 */
public class ConnectionTimeReplySocketServer implements Server {

    private final ServerSocket serverSocket;

    public ConnectionTimeReplySocketServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public ServerRunnable accept() throws IOException {
        Socket socket = this.serverSocket.accept();
        return new SocketServerRunnable(new ConnectionTimeReplyRequestHandler(), socket);
    }

    @Override
    public void shutdown() throws IOException {
        this.serverSocket.close();
    }

}
