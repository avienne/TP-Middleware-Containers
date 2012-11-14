package midcontainers.remote;

import java.net.Socket;
import midcontainers.ContainerException;
import midcontainers.Container;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;


class Worker extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Socket socket;
    private int clientObjectsCounter = 0;
    private final Map<Integer, Object> clientObjects = new HashMap<Integer, Object>();
    private final Container conteneur; 

    public Worker(Socket socket, Container conteneur) {
        this.socket = socket;
        this.conteneur = conteneur;
    }

    public void end(){
        running.set(false);
    }
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            
            while (running.get()) {
                String name;
                String qualifier;
                Object instance;
                int parametersCount;
                int objectId;
                Class[] parameterTypes;
                Object[] parameters;

                RemoteCommand command = (RemoteCommand) in.readObject();
                switch (command) {

                    case CHECK_DEFINITION:
                        name = (String) in.readObject();
                        out.writeObject(conteneur.hasValueDefinedFor(name));
                        out.flush();
                        break;

                    case CHECK_REFERENCE:
                        name = (String) in.readObject();
                        qualifier = (String) in.readObject();
                        out.writeObject(conteneur.hasReferenceDeclaredFor(Class.forName(name),qualifier));
                        out.flush();
                        break;

                    case GET_DEFINITION:
                        name = (String) in.readObject();
                        out.writeObject(conteneur.definitionValue(name));
                        out.flush();
                        break;

                    case GET_REFERENCE:
                        name = (String) in.readObject();
                        qualifier = (String) in.readObject();
                        instance = conteneur.obtainReference(Class.forName(name), qualifier);
                        clientObjects.put(clientObjectsCounter, instance);
                        out.writeObject(clientObjectsCounter);
                        out.flush();
                        clientObjectsCounter = clientObjectsCounter + 1;
                        break;

                    case INVOKE:
                        objectId = in.readInt();
                        name = (String) in.readObject();
                        parametersCount = in.readInt();
                        parameters = new Object[parametersCount];
                        parameterTypes = new Class<?>[parametersCount];
                        for (int i = 0; i < parametersCount; i++) {
                            parameterTypes[i] = Class.forName((String) in.readObject());
                        }
                        for (int i = 0; i < parametersCount; i++) {
                            parameters[i] = in.readObject();
                        }

                        instance = clientObjects.get(objectId);
                        Method method = instance.getClass().getMethod(name, parameterTypes);
                        out.writeObject(method.invoke(instance, parameters));
                        out.flush();
                        break;
                }
            }
        } catch (IOException e) {
            throw new ContainerException(e);
        } catch (ClassNotFoundException e) {
            throw new ContainerException(e);
        } catch (NoSuchMethodException e) {
            throw new ContainerException(e);
        } catch (InvocationTargetException e) {
            throw new ContainerException(e);
        } catch (IllegalAccessException e) {
            throw new ContainerException(e);
        }
    }
}