# TP Composants distants #

L'objectif de ce TP est de proposer des conteneurs à composants distants.

**Repartez du code du TP conteneur qui doit être opérationnel.**

L'idée sous-jacente est la suivante :

* un conteneur "serveur" expose ses composants à distance,
* un conteneur "client" sert de relai vers un conteneur "serveur",
* les composants s'exécutent au sein de leur conteneur "serveur",
* les composants obtenus via un conteneur client sont en réalité des proxys qui effectuent des appels RPC vers leur serveur.

Vous allez mettre en oeuvre un protocole réseau fonctionnellement équivalent à RMI mais beaucoup plus simple. Cela vous permettra également de comprendre le fonctionnement de ce type de middleware.

Le cas d'utilisation suivant illustre l'emploi de ce type de conteneur. Tout d'abord nous pouvons lancer un conteneur serveur sur le port 1981 et déclarer des composants :

    RemoteContainerServer server = new RemoteContainerServer(1981);
    server
            .declare(new Binding(Echo.class, SomeEcho.class, null, NEW))
            .declare(new Binding(Echo.class, SomeEcho.class, "shared", SINGLETON))
            .define("prefix", "[ ")
            .define("suffix", " ]");
    server.start();

Nous pouvons ensuite déclarer un conteneur client qui se connecte sur ce conteneur serveur :

    RemoteContainerClient client = new RemoteContainerClient("127.0.0.1", 1981);

Nous pouvons obtenir une référence sur un composant distant et l'invoquer.

    Echo echo = client.obtainReference(Echo.class);
    System.out.println(echo.echo("hello")); // => "[ hello ]"

**Attention : ** comme dans le cas de RMIs, l'objet *echo* n'est pas une instance de *SomeEcho*. C'est un proxy généré dynamiquement. Quand la méthode *echo* est invoquée, le proxy envoie une requête au serveur pour exécuter la méthode *hello* avec le paramètre *"hello"* sur l'instance réelle de *SomeEcho* qui existe au sein du conteneur "serveur".

## Mise en oeuvre ##

**Avant de commencer le développement, veuillez lire et comprendre l'intégralité du sujet.**

De nombreux éléments vous sont fournis, à vous de les assembler dans le bon ordre. Cela ne devrait pas être trop complexe si vous prenez le temps de comprendre les fragments de code **au lieu de faire des copier/coller à l'aveuglette**.

## Protocole réseau ##

Nous allons nous appuyer sur TCP/IP pour communiquer entre le client et le serveur,

Nous utiliserons les flux standard Java sur des flux de sockets. Pour envoyer des objets de nature diverse, nous utiliserons des *ObjectInputStream* et *ObjectOutputStream*.

Chaque commande sera précédée par un code. Pour cela nous utiliserons une énumération Java. En effet, les énumérations sont sérializables et donc transportables sur *ObjectOutputStream* et *ObjectInputStream* :

    package midcontainers.remote;

    enum RemoteCommand {
        CHECK_REFERENCE,
        CHECK_DEFINITION,
        GET_REFERENCE,
        GET_DEFINITION,
        INVOKE
    }

Les commandes *CHECK_REFERENCE* et *CHECK_DEFINITION* permettent de vérifier si le conteneur serveur possède des références ou valeurs. Les variantes en *GET* permettent de les obtenir. Enfin, *INVOKE* permet d'effectuer une invocation distante de méthode.

Le protocole est le suivant.

    [CHECK_DEFINITION, String: clé de la valeur]
        => [Boolean]
    
    [CHECK_REFERENCE, String: classe de l'interface, String: qualifier]
        => [Boolean]
    
    [GET_DEFINITION, String: clé]
        => [Object: valeur]
    
    [GET_REFERENCE, String: classe, String: qualifier]
        => [Integer: numéro de l'objet coté serveur]
    
    [INVOKE, Integer: numéro de l'objet coté serveur, String: nom de méthode, Integer: nombre d'arguments, String[]: noms de classes des paramètres, Object[]: valeur des paramètres]
        => [Object: valeur de retour]
        
La gestion des appels distants est basée sur un numéro de session. Nous vous conseillons de le gérer comme un compteur démarrant à 0 dans le conteneur serveur, et incrémenté pour chaque client.

Le proxy coté client se base sur ce numéro. Coté serveur, ce numéro permet de retrouver l'objet réel qui correspond au proxy et qui a été instancié et injecté dans le conteneur serveur.

## Conteneur serveur ##

Notre conteneur serveur sera une extension de *LocalContainer*. Nous vous proposons de partir du code suivant :

    package midcontainers.remote;

    import (...)

    public class RemoteContainerServer extends LocalContainer {

        private final ServerSocket serverSocket;
        private final int port;
        private final AtomicBoolean running = new AtomicBoolean(false);

        public RemoteContainerServer(int port) {
            super();
            this.port = port;
            try {
                this.serverSocket = new ServerSocket();
            } catch (IOException e) {
                throw new ContainerException(e);
            }
        }
    }

Une instance de *ServerSocket* est créée. Nous pouvons définir une méthode *start()* pour écouter sur le réseau :

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

Il vous faut un thread qui accepte les connections entrantes, et qui est lancé depuis *start()* :

    private final Thread acceptingThread = new Thread() {
        public void run() {
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Worker(clientSocket).start();
                } catch (SocketTimeoutException ignored) {
                } catch (IOException e) {
                    throw new ContainerException(e);
                }
            }
        }
    };

Et enfin vous pouvez lancer 1 thread pour gérer chaque connection client. Le thread d'acceptation proposé précédemment utilise une classe *Worker* dont voici un squelette (presque complet ...) pour démarrer :

    private class Worker extends Thread {
        private final Socket socket;
        private int clientObjectsCounter = 0;
        private final Map<Integer, Object> clientObjects = new HashMap<Integer, Object>();

        public Worker(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                
                while (running.get()) {
                    command = (RemoteCommand) in.readObject();
                    switch (command) {

                        case CHECK_DEFINITION:
                            name = (String) in.readObject();
                            out.writeObject(hasValueDefinedFor(name));
                            out.flush();
                            break;

                        case CHECK_REFERENCE:
                            // (...)
                            break;

                        case GET_DEFINITION:
                            // (...)
                            break;

                        case GET_REFERENCE:
                            name = (String) in.readObject();
                            qualifier = (String) in.readObject();
                            instance = obtainReference(Class.forName(name), qualifier);
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

## Conteneur client ##

Notre conteneur client sera aussi une spécialisation de *LocalContainer*. Démarrons avec le code suivant :

    package midcontainers.remote;
    
    import (...)
    
    public class RemoteContainerClient implements Container {
                                                                                                  
        public Container delegateTo(Container container) throws UnsupportedOperationException {                                
            throw new UnsupportedOperationException("A remote container client does not support delegation");                  
        }                                                                                                                      

        public Container declare(Binding binding) throws UnsupportedOperationException {                                       
            throw new UnsupportedOperationException("A remote container client can only obtain references and defined values");
        }                                                                                                                      

        public Container define(String name, Object value) throws UnsupportedOperationException {                              
            throw new UnsupportedOperationException("A remote container client can only obtain references and defined values");
        }
    }

Nous commençons par rendre inactives les méthodes qui n'ont pas de sens pour un conteneur client : définir des composants, des valeurs et mettre en place une délégation.

Pour gérer les connexions avec le serveur, il nous faut 2 flux initialisés à la construction :

    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    
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

Le reste du code consiste à redéfinir les méthodes de l'interface du conteneur en respectant le protocole réseau mis en place. Voici 2 exemples, à vous de compléter le reste.

    public boolean hasReferenceDeclaredFor(Class<?> interfaceClass, String qualifier) {
        try {                                                                          
            out.writeObject(CHECK_REFERENCE);                                          
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
    
    public Object definitionValue(String name) {
        try {                                   
            out.writeObject(GET_DEFINITION);    
            out.writeObject(name);              
            out.flush();                        
            return in.readObject();             
        } catch (IOException e) {               
            throw new ContainerException(e);    
        } catch (ClassNotFoundException e) {    
            throw new ContainerException(e);    
        }                                       
    }

## Génération de proxy ##
    
L'écriture de la méthode *obtainReference()* dans le conteneur client nécessite de générer un proxy.

On peut générer un proxy en Java en :

1. définissant un objet implémentant *InvocationHandler*, et
2. en passant cet objet à *Proxy.newProxyInstance()*, une méthode statique qui va générer ce proxy.

Concrètement, *newProxyInstance()* génère une classe à la volée qui va implémenter un jeu d'interfaces données en paramètre. Quand une méthode est invoquée sur cet objet, la méthode *invoke()* du *InvocationHandler* est invoquée. Il est alors possible de voir quel méthode est appellée, avec quels paramètres, et ainsi d'agir en conséquence.

**Lisez la documentation de *java.lang.reflect.Proxy***.

Voici une version incomplète de *obtainReference()* qui devrait vous mettre sur la voie.

    public <T> T obtainReference(Class<T> interfaceClass, String qualifier) {
        try {

            out.writeObject(GET_REFERENCE);
            out.writeObject(interfaceClass.getName());
            out.writeObject(qualifier);
            out.flush();
            final int objectId = (Integer) in.readObject();

            InvocationHandler handler = new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
                    // TODO: la suite du protocole pour un INVOKE, en particulier lui passer objectId et le nom de méthode
                    out.writeObject(INVOKE);
                    (...)                    
                    
                    if (parameters == null) {
                        out.writeInt(0);
                    } else {
                        out.writeInt(parameters.length);
                        for (Class<?> type : method.getParameterTypes()) {
                            out.writeObject(type.getName());
                        }
                        // TODO: ecrire les valeurs des parametres
                        (...)
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

## Spécification sous forme de tests unitaires ##

Vous pouvez valider votre implémentation de ces 2 conteneurs via la classe de tests unitaires suivante. Vous devez la placer dans **src/test/java/midcontainers/remote/RemoteContainerTest.java**.

**Le plus simple est d'ajouter progressivement les méthodes de test au fur et à mesure que vous progressez...**

    /*
     * Copyright (C) 2011 Julien Ponge, Institut National des Sciences Appliquées de Lyon
     *
     * This program is free software: you can redistribute it and/or modify
     * it under the terms of the GNU Affero General Public License as published by
     * the Free Software Foundation, either version 3 of the License, or
     * (at your option) any later version.
     *
     * This program is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     * GNU Affero General Public License for more details.
     *
     * You should have received a copy of the GNU Affero General Public License
     * along with this program.  If not, see <http://www.gnu.org/licenses/>.
     */

    package midcontainers.remote;

    import midcontainers.Binding;
    import midcontainers.components.*;
    import midcontainers.local.LocalContainer;
    import org.junit.Test;

    import java.util.LinkedList;
    import java.util.List;
    import java.util.Set;

    import static midcontainers.Binding.Policy.NEW;
    import static midcontainers.Binding.Policy.SINGLETON;
    import static org.hamcrest.CoreMatchers.*;
    import static org.junit.Assert.assertThat;
    import static org.junit.Assert.fail;

    public class RemoteContainerTest {

        @Test
        public void smoke_test() {
            RemoteContainerServer server = new RemoteContainerServer(1981);
            server.start();
            server.stop();
        }

        @Test
        public void check_unsupported_operations() {
            RemoteContainerServer server = new RemoteContainerServer(1982);
            server.start();
            RemoteContainerClient client = new RemoteContainerClient("127.0.0.1", 1982);

            try {
                client.declare(new Binding(List.class, LinkedList.class, null, NEW));
                fail("The operation should not be supported");
            } catch (UnsupportedOperationException ignored) {
            }

            try {
                client.define("foo", "bar");
                fail("The operation should not be supported");
            } catch (UnsupportedOperationException ignored) {
            }

            try {
                client.delegateTo(new LocalContainer());
                fail("The operation should not be supported");
            } catch (UnsupportedOperationException ignored) {
            }

            server.stop();
        }

        @Test
        public void check_query_operations() {
            RemoteContainerServer server = new RemoteContainerServer(1983);
            server
                    .define("hello", "world")
                    .declare(new Binding(List.class, LinkedList.class, null, NEW));

            server.start();
            RemoteContainerClient client = new RemoteContainerClient("127.0.0.1", 1983);

            boolean query = client.hasValueDefinedFor("hello");
            assertThat(query, is(true));
            query = client.hasValueDefinedFor("foo");
            assertThat(query, is(false));

            query = client.hasReferenceDeclaredFor(List.class, null);
            assertThat(query, is(true));
            query = client.hasReferenceDeclaredFor(Set.class, null);
            assertThat(query, is(false));

            assertThat((String) client.definitionValue("hello"), is("world"));

            server.stop();
        }

        @Test
        public void check_remote_operations() {
            RemoteContainerServer server = new RemoteContainerServer(1984);
            server
                    .declare(new Binding(Echo.class, SomeEcho.class, null, NEW))
                    .declare(new Binding(Echo.class, SomeEcho.class, "shared", SINGLETON))
                    .define("prefix", "[ ")
                    .define("suffix", " ]");
            server.start();

            RemoteContainerClient client = new RemoteContainerClient("127.0.0.1", 1984);

            Echo echo = client.obtainReference(Echo.class);
            assertThat(echo, notNullValue());
            assertThat(echo, not(instanceOf(SomeEcho.class)));

            assertThat(echo.echo("hello"), is("[ hello ]"));

            Echo echoShared1 = client.obtainReference(Echo.class, "shared");
            Echo echoShared2 = client.obtainReference(Echo.class, "shared");
            assertThat(echoShared1, not(sameInstance(echoShared2)));
            assertThat(echoShared1.echo("hello"), is("[ hello ]"));
            assertThat(echoShared2.echo("hello"), is("[ hello ]"));

            server.declare(new Binding(Counter.class, SomeCounter.class, null, SINGLETON));
            server.declare(new Binding(Counter.class, SomeCounter.class, "private", NEW));

            Counter c1 = client.obtainReference(Counter.class);
            Counter c2 = client.obtainReference(Counter.class);
            c1.increment();
            c1.increment();
            c2.increment();
            assertThat(c1.get(), is(3));
            assertThat(c2.get(), is(3));

            c1 = client.obtainReference(Counter.class, "private");
            c2 = client.obtainReference(Counter.class, "private");
            c1.increment();
            c1.increment();
            c2.increment();
            assertThat(c1.get(), is(2));
            assertThat(c2.get(), is(1));

            server.stop();
        }

        @Test
        public void check_delegation_to_remote() {
            RemoteContainerServer server = new RemoteContainerServer(1985);
            server
                    .declare(new Binding(Counter.class, SomeCounter.class, "shared", SINGLETON))
                    .define("prefix", "[ ")
                    .define("suffix", " ]");
            server.start();

            LocalContainer mainContainer = new LocalContainer();
            mainContainer
                    .declare(new Binding(EchoCounterClient.class, SomeEchoCounterClient.class, null, NEW))
                    .declare(new Binding(Echo.class, SomeEcho.class, null, NEW))
                    .delegateTo(new RemoteContainerClient("127.0.0.1", 1985));

            EchoCounterClient client = mainContainer.obtainReference(EchoCounterClient.class);
            assertThat(client, notNullValue());
            assertThat(client.echoNextIncrement(), is("[ 1 ]"));
            assertThat(client.echoNextIncrement(), is("[ 2 ]"));
            assertThat(client.echoNextIncrement(), is("[ 3 ]"));

            server.stop();
        }
    }
    