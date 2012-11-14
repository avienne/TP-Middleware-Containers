package midcontainers.remote;

import midcontainers.local.LocalContainer;
import midcontainers.ContainerException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;



public class RemoteContainerServer extends LocalContainer {

    private final ServerSocket serverSocket;
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Thread acceptingThread;
    private List<Worker> workers = new ArrayList<Worker>();

    public RemoteContainerServer(int port) {
        super();
        this.port = port;
        try {
            this.serverSocket = new ServerSocket();
        } catch (IOException e) {
            throw new ContainerException(e);
        }

        final RemoteContainerServer reference = this;
        acceptingThread = new Thread() {
            public void run() {
                while (running.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();

                        Worker w = new Worker(clientSocket,reference);
                        w.start();
                        workers.add(w);
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        throw new ContainerException(e);
                    }
                }
            }
        };

    }

    public void start() {                                  
        try {                                              
            serverSocket.setSoTimeout(10000);              
            serverSocket.bind(new InetSocketAddress(port));
        } catch (SocketException e) {                      
            throw new ContainerException(e);               
        } catch (IOException e) {                          
            throw new ContainerException(e);               
        }                                                  
        running.set(true);                                 
        acceptingThread.start();                           
    }

    public void stop(){
        running.set(false);
        try {
            serverSocket.close();
        }catch (IOException e){
            throw new ContainerException(e);
        }
        for(Worker worker : workers)
            worker.end();
    }
}