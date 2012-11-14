package midcontainers.distsession; 

import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import midcontainers.ContainerException;
import midcontainers.Container;
import midcontainers.local.LocalContainer;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import midcontainers.Binding;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DistributedSessionContainer extends LocalContainer {

    private final MulticastSocket socket;
    private final InetAddress group;     
    private final String groupAddress;   
    private final int port;
    private static final int BUFFER_SIZE = 8192;
    private final byte[] incomingBuffer = new byte[BUFFER_SIZE];
    private final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream(BUFFER_SIZE);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread networkThread;

    public class DistSession implements Session{
        private Map<String,Serializable> map = new ConcurrentHashMap<String,Serializable>();

        public Serializable get(String key) {
            return map.get(key);
        }

        public void delete(String key) {
            send(SessionCommand.DELETE, key, null);
            map.remove(key);
        }

        public void set(String key, Serializable value) {
            send(SessionCommand.SET, key, value);
            map.put(key, value);
        }

        public void deleteLocal(String key) {
            map.remove(key);
        }

        public void setLocal(String key, Serializable value) {

            map.put(key, value);
        }
        public void sync(){
            for (Map.Entry<String, Serializable> entry : map.entrySet()){
                send(SessionCommand.SET,entry.getKey(), entry.getValue());
            }
        }
    }

    public DistributedSessionContainer(String groupAddress, int port) {
        try {                                                          
            this.groupAddress = groupAddress;                          
            this.port = port;                                          
            this.group = InetAddress.getByName(groupAddress);          
            socket = new MulticastSocket(port);                        
            socket.setSoTimeout(10000);

            Binding b = new Binding(Session.class, DistSession.class, null, Binding.Policy.SINGLETON);
            this.declare(b);
            this.singletons.put(b.getKey(), new DistSession());

            networkThread = new Thread(){
                public void run(){
                    DatagramPacket inPacket = new DatagramPacket(incomingBuffer, incomingBuffer.length);
                    DistSession session = (DistSession) DistributedSessionContainer.this.obtainReference(Session.class);
                    while (running.get()) {
                        try {
                            socket.receive(inPacket);
                            Serializable[] command = decode(inPacket.getData(), 3);
                            String key = (String) command[1];
                            Serializable value = command[2];
                            switch ((SessionCommand) command[0]) {
                                case SET:
                                    session.setLocal(key, value);
                                break;

                                case DELETE:
                                    session.deleteLocal(key);
                                break;

                                case SYNC:
                                    session.sync();
                                break;
                            }
                        } catch (IOException e) {throw new ContainerException(e);}
                    }
                }
            };
        } catch (UnknownHostException e) {
            throw new ContainerException(e);                           
        } catch (IOException e) {                                      
            throw new ContainerException(e);                           
        }                                                              
    }

    public void start() {                    
        try {                                
            socket.joinGroup(group);  
            this.running.set(true);
            networkThread.start();
            send(SessionCommand.SYNC,null,null);    
        } catch (IOException e) {            
            throw new ContainerException(e); 
        }
    } 

    public void stop() {                                 
        try {                               
            socket.leaveGroup(group);
            running.set(false);    
        } catch (IOException e) {           
            throw new ContainerException(e);
        }
    }

    private Serializable[] decode(byte[] buffer, int count) {                              
        try {                                                                              
            Serializable[] decoded = new Serializable[count];
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer));
            for (int i = 0; i < count; i++) {                                              
                decoded[i] = (Serializable) in.readObject();                               
            }                                                                              
            in.close();                                                                    
            return decoded;                                                                
        } catch (IOException e) {                                                          
            throw new ContainerException(e);                                               
        } catch (ClassNotFoundException e) {                                               
            throw new ContainerException(e);                                               
        }                                                                                  
    }

    private byte[] encode(Serializable... objects) {                                          
        outputBuffer.reset();                                                                 
        try {                                                                                 
            ObjectOutputStream out = new ObjectOutputStream(outputBuffer);                    
            for (Serializable object : objects) {                                             
                out.writeObject(object);                                                      
            }                                                                                 
            out.close();                                                                      
            return outputBuffer.toByteArray();                                                
        } catch (IOException e) {                                                             
            throw new ContainerException(e);                                                  
        }                                                                                     
    }
    private void send(SessionCommand command, String key, Serializable value) {  
        byte[] bytes = encode(command, key, value);                                  
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, port);
        try {                                                                        
            socket.send(packet);                                                     
        } catch (IOException e) {                                                    
            throw new ContainerException(e);                                         
        }                                                                            
    }                                       
}

