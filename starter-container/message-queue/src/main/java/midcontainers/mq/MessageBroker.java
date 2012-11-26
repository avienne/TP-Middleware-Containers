package midcontainers.mq;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * A networked message broker.
 * <p/>
 * Message queues are created when a first message is posted to them. They provide durability
 * in case the broker needs to be restarted.
 *
 * @see midcontainers.mq.Message
 * @see midcontainers.mq.MessageBrokerClient
 */
public class MessageBroker {

   private ServerSocket serverSocket;
   private int port;
   private final AtomicBoolean running = new AtomicBoolean(false);
   private Thread acceptingThread;
   private File fMessages; 
   private Map<String,MessageQueue> mapQueue = new HashMap<String,MessageQueue>();
   private List<MessageWorker> msgWorkers = new ArrayList<MessageWorker>();
    /**
     * Creates and prepares a message broker.
     *
     * @param port             the port number for incoming client connections
     * @param storageDirectory the storage directory for reliable queues
     * @throws MqException if the broker could not be started, or if the reloading
     *                     of durable queues failed
     */
    public MessageBroker(int port, File storageDirectory) {
        try {
            this.serverSocket = new ServerSocket();
            this.fMessages = storageDirectory;
            this.port = port;
        }catch(IOException e){
            throw new MqException(e);
        }
        final MessageBroker reference = this;
        acceptingThread = new Thread(){
            public void run(){
                while (running.get()){
                    try{
                        Socket clientSocket = serverSocket.accept();
                        MessageWorker w = new MessageWorker(clientSocket,reference);
                        msgWorkers.add(w);
                        w.start();
                    }catch(SocketTimeoutException nope){
                    }catch(IOException e){ throw new MqException(e);}
                }
            }
        };
    }

    /**
     * Starts the server.
     *
     * @throws MqException if the server could not be started
     */
    public void start() {
        try{
            this.serverSocket.setSoTimeout(1000);
            this.serverSocket.bind(new InetSocketAddress(this.port)); 
        }catch(SocketException e){
            throw (new MqException(e));
        }catch(IOException e){
            throw new MqException(e);
        }
        this.running.set(true);
        acceptingThread.start();
    }

    /**
     * Stops the server.
     *
     * @throws MqException if the server could not be stopped
     */
    public void stop() {
        running.set(false);   
         try {
            serverSocket.close();
            for (MessageWorker worker : msgWorkers){
                worker.end();
            }
        }catch (IOException e){
            throw new MqException(e);
        } 
    }

    public void addMessage(Message msg){
        MessageQueue msgQueue = mapQueue.get(msg.getDestination());
        if(msgQueue == null){
            msgQueue = new MessageQueue(msg.getDestination(),fMessages);
            mapQueue.put(msg.getDestination(),msgQueue);
        }
        msgQueue.add(msg);

    }

    public Boolean checkAvailability(String destination){
        MessageQueue queue = mapQueue.get(destination);
        return (queue == null) ? false : !queue.isEmpty();
    }
    
    public Message getMessage(String destination){
        MessageQueue queue = mapQueue.get(destination);
        return queue.remove();
    }
}
