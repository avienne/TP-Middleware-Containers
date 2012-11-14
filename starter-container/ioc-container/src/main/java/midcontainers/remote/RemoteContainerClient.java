package midcontainers.remote;

import midcontainers.Container;
import midcontainers.Binding;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import midcontainers.ContainerException;
import java.io.IOException;
import java.net.Socket;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RemoteContainerClient implements Container {
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public RemoteContainerClient(String host, int port) {
        try {                                                       
            Socket socket = new Socket(host, port);                 
            // /!\ ORDER MATTERS OR YOU DEADLOCK /!\                
            out = new ObjectOutputStream(socket.getOutputStream()); 
            in = new ObjectInputStream(socket.getInputStream());    
        } catch (IOException e) {                                   
            throw new ContainerException(e);                        
        }                                                           
    }
                                                                                              
    public Container delegateTo(Container container) throws UnsupportedOperationException {                                
        throw new UnsupportedOperationException("A remote container client does not support delegation");                  
    }                                                                                                                      

    public Container declare(Binding binding) throws UnsupportedOperationException {                                       
        throw new UnsupportedOperationException("A remote container client can only obtain references and defined values");
    }                                                                                                                      

    public Container define(String name, Object value) throws UnsupportedOperationException {                              
        throw new UnsupportedOperationException("A remote container client can only obtain references and defined values");
    }

    public <T> T obtainReference(Class<T> interfaceClass){
    	return obtainReference(interfaceClass,null);
    }
 
    public <T> T obtainReference(Class<T> interfaceClass, String qualifier){
        try {
            out.writeObject(RemoteCommand.GET_REFERENCE);
            out.writeObject(interfaceClass.getName());
            out.writeObject(qualifier);
            out.flush();
            final int objectId = (Integer) in.readObject();
            InvocationHandler handler = new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
                    // TODO: la suite du protocole pour un INVOKE, en particulier lui passer objectId et le nom de méthode
                    out.writeObject(RemoteCommand.INVOKE);
                    out.writeInt(objectId);
                    out.writeObject(method.getName());
                    if (parameters == null) {
                        out.writeInt(0);
                    } else {
                        out.writeInt(parameters.length);
                        for (Class<?> type : method.getParameterTypes()) {
                            out.writeObject(type.getName());
                        }
                        for (Object param : parameters){
                            out.writeObject(param);
                        }
                    }
                    out.flush();
                    return in.readObject();
                }
        };

        // Fabrique un proxy sur l'interface interfaceClass
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{interfaceClass}, handler);

        } catch (IOException e) {
            throw new ContainerException(e);
        } catch (ClassNotFoundException e) {
            throw new ContainerException(e);
        }
    }

    public Object definitionValue(String name){
    	try {
            out.writeObject(RemoteCommand.GET_DEFINITION);
            out.writeObject(name);
            out.flush();
            return in.readObject();
        } catch (IOException e) {                                                      
            throw new ContainerException(e);
        } catch (ClassNotFoundException e) {                                           
            throw new ContainerException(e);                                           
        } 
    }

    public boolean hasReferenceDeclaredFor(Class<?> interfaceClass){
        return hasReferenceDeclaredFor(interfaceClass,null);
    }

    public boolean hasReferenceDeclaredFor(Class<?> interfaceClass, String qualifier){
        try {                                                                          
            out.writeObject(RemoteCommand.CHECK_REFERENCE);                                          
            out.writeObject(interfaceClass.getName());                                 
            out.writeObject(qualifier);                                                
            out.flush();                                                               
            return (Boolean) in.readObject();                                          
        } catch (IOException e) {                                                      
            throw new ContainerException(e);                                           
        } catch (ClassNotFoundException e) {                                           
            throw new ContainerException(e);                                           
        }            
    }
    public boolean hasValueDefinedFor(String name){
    	try{
            out.writeObject(RemoteCommand.CHECK_DEFINITION);                                          
            out.writeObject(name);                                                                             
            out.flush();                                                               
            return (Boolean) in.readObject();                                          
        } catch (IOException e) {                                                      
            throw new ContainerException(e);                                           
        } catch (ClassNotFoundException e) {                                           
            throw new ContainerException(e);                                           
        }           
    }




}