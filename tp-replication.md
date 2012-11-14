# TP Réplication de cache #

Nous allons créer un conteneur de composants disposant d'un composant de sessions distribuées.

Une session n'étant rien d'autre qu'un cache, ce composant aura la charge de répliquer ses mises à jour sur les autres composants de sessions appartenant au même groupe multicast.

## Interface et conteneur ##

Dans le package *midcontainers.distsession*, nous allons créer *DistributedSessionContainer* en tant que spécialisation de *LocalContainer*.

Ce conteneur diffère simplement de *LocalContainer* dans le fait qu'il publie implicitement un composant *Session* répondant à l'interface publique suivante :

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

    package midcontainers.distsession;

    import java.io.Serializable;

    /**
     * Interface to a distributed session.
     * <p/>
     * A distributed session is a special component that is available from
     * instances of <code>DistributedSessionContainer</code>. As such, it can
     * be injected into components.
     * <p/>
     * The container should replicate updates to a session store to the other
     * distributed instances. Session values must obviously be serializable.
     *
     * @author Julien Ponge
     * @see java.io.Serializable
     * @see midcontainers.distsession.DistributedSessionContainer
     */
    public interface Session {

        /**
         * Fetch a session value.
         *
         * @param key key of the value
         * @return the value, or <code>null</code> if none was found
         */
        Serializable get(String key);

        /**
         * Remove a value
         *
         * @param key the key of the value to be removed
         */
        void delete(String key);

        /**
         * Define or update a session value
         *
         * @param key   the value key
         * @param value the value
         */
        void set(String key, Serializable value);
    }

Il est ainsi possible d'y accéder de la façon suivante :

    DistributedSessionContainer container = new DistributedSessionContainer("228.5.6.7", 8666);
    container.start();

    Session session = container.obtainReference(Session.class);

    session.set("foo", "bar");
    System.out.println(session.get("foo")); // "bar"

    session.delete("foo");
    System.out.println(session.get("foo")); // "null"

    container.stop();

## Communications Multicast ##

La communication entre les sessions distribuées se fait en multicast.

Nous conseillons de débuter votre implémentation en passant les paramètres réseau ainsi :

    public class DistributedSessionContainer extends LocalContainer {
        
        private final MulticastSocket socket;
        private final InetAddress group;     
        private final String groupAddress;   
        private final int port;

        public DistributedSessionContainer(String groupAddress, int port) {
            try {                                                          
                this.groupAddress = groupAddress;                          
                this.port = port;                                          
                this.group = InetAddress.getByName(groupAddress);          
                socket = new MulticastSocket(port);                        
                socket.setSoTimeout(10000);                                
            } catch (UnknownHostException e) {                             
                throw new ContainerException(e);                           
            } catch (IOException e) {                                      
                throw new ContainerException(e);                           
            }                                                              
        }
        
    }

Vous pouvez joindre un groupe multicast ainsi :

    public void start() {                    
        try {                                
            socket.joinGroup(group);         
        } catch (IOException e) {            
            throw new ContainerException(e); 
        }                                    
    }

et le quitter ainsi :

    public void stop() {                                 
        try {                               
            socket.leaveGroup(group);       
        } catch (IOException e) {           
            throw new ContainerException(e);
        }
    }

Il vous faudra comme dans le cadre du TP précédant un thread pour écouter les évènements sur le groupe multicast : arrivée / départ d'un participant, mise à jour de clé, etc.

L'envoi de données en multicast se fait via la classe *DatagramPacket* qui permet de transporter un tableau de *byte*, *byte[]* :

    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, port);
    socket.send(packet);

La réception se fait via un buffer :

    DatagramPacket inPacket = new DatagramPacket(incomingBuffer, incomingBuffer.length);
    socket.receive(inPacket);

## Protocole de communication ##

Votre protocole s'appuiera sur une énumération pour définir la commande liée à un paquet envoyé en multicast. Nous avons choisi un protocole simple lorsque nous avons fait ce TP : vous êtes libres de le faire varier.

Une énumération Java est parfaite pour indiquer une commande :

    enum SessionCommand {
        SYNC, SET, DELETE
    }

Nous proposons d'encoder les messages sous la forme de 3 objets sérializés contenus dans le même buffer émis dans un *DatagramPaquet* :

    [SYNC, null, null]
        => chaque participant émet des SET pour chaque entrée de son cache local
    
    [SET, String: clé, Object: valeur]
        => mise à jour ou insertion de (clé / valeur)
    
    [DELETE, String: clé, null]
        => suppression de l'entrée

Quand un participant rejoint un groupe multicast, il commence par émettre un *SYNC* afin que ceux déjà en place puissent lui communiquer leurs valeurs. Notez que ce protocole n'est pas efficace...

## Encodage d'objets sérializables ##

Pour préparer vos *DatagramPaquet*, il vous faudra convertir des objets sérializables en *byte[]*.

Pour vous faire gagner du temps, nous vous suggérons d'ajouter ces méthodes et définitions utilitaires à votre conteneur.    

Pour la réception :
    
    private static final int BUFFER_SIZE = 8192;
    private final byte[] incomingBuffer = new byte[BUFFER_SIZE];
    
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

Utilisable ainsi :

    DatagramPacket inPacket = new DatagramPacket(incomingBuffer, incomingBuffer.length);
    Serializable[] command = decode(inPacket.getData(), 3);
    
    switch ((SessionCommand) command[0]) {
        case SET:
            (...)
            break;

        case DELETE:
            (...)
            break;

        case SYNC:
            (...)
            break;
    }

Pour l'émission :

    private final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream(BUFFER_SIZE);

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

Utilisable ainsi :

    private void send(SessionCommand command, String key, Serializable value) {      
        byte[] bytes = encode(command, key, value);                                  
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, port);
        try {                                                                        
            socket.send(packet);                                                     
        } catch (IOException e) {                                                    
            throw new ContainerException(e);                                         
        }                                                                            
    }

## Et la spécification sous forme de tests unitaires ? ##

Elle est ici :

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

    package midcontainers.distsession;

    import midcontainers.Binding;
    import midcontainers.components.Counter;
    import midcontainers.components.InSessionCounter;
    import org.junit.Test;

    import static org.hamcrest.CoreMatchers.*;
    import static org.junit.Assert.assertThat;

    public class DistributedSessionContainerTest {

        private static final int SLEEP_DELAY = 250;

        @Test
        public void check_local_session() {
            DistributedSessionContainer container = new DistributedSessionContainer("228.5.6.7", 8666);
            container.start();

            Session session = container.obtainReference(Session.class);
            assertThat(session.get("foo"), nullValue());
            session.set("foo", "bar");
            assertThat((String) session.get("foo"), is("bar"));
            session.delete("foo");
            assertThat(session.get("foo"), nullValue());

            container.stop();
        }

        @Test
        public void check_distributed_session() throws InterruptedException {
            DistributedSessionContainer container1 = new DistributedSessionContainer("228.5.6.7", 8666);
            DistributedSessionContainer container2 = new DistributedSessionContainer("228.5.6.7", 8666);
            container1.start();
            container2.start();

            Session session1 = container1.obtainReference(Session.class);
            Session session2 = container2.obtainReference(Session.class);

            session1.set("hello", "world");
            session1.set("foo", "bar");

            Thread.sleep(SLEEP_DELAY);

            assertThat((String) session2.get("hello"), is("world"));
            assertThat((String) session2.get("foo"), is("bar"));

            session2.delete("foo");

            Thread.sleep(SLEEP_DELAY);

            assertThat((String) session1.get("foo"), nullValue());

            container1.stop();
            container2.stop();
        }

        @Test
        public void check_assembly() throws InterruptedException {
            DistributedSessionContainer distContainer1 = new DistributedSessionContainer("228.5.6.7", 8666);
            DistributedSessionContainer distContainer2 = new DistributedSessionContainer("228.5.6.7", 8666);

            distContainer1.start();
            distContainer2.start();

            distContainer1
                    .define("counter.key", "my:counter")
                    .declare(new Binding(Counter.class, InSessionCounter.class, null, Binding.Policy.NEW));

            Counter c1 = distContainer1.obtainReference(Counter.class);
            Counter c2 = distContainer1.obtainReference(Counter.class);
            Session session2 = distContainer2.obtainReference(Session.class);

            assertThat(c1, not(sameInstance(c2)));

            c1.increment();
            c2.increment();

            Thread.sleep(SLEEP_DELAY);

            assertThat(c1.get(), is(2));
            assertThat(c2.get(), is(2));
            assertThat((Integer) session2.get("my:counter"), is(2));

            distContainer1.stop();
            distContainer2.stop();
        }

        @Test
        public void check_join_group_sync() throws InterruptedException {
            DistributedSessionContainer container1 = new DistributedSessionContainer("228.5.6.7", 8666);
            DistributedSessionContainer container2 = new DistributedSessionContainer("228.5.6.7", 8666);
            container1.start();
            container2.start();

            Session session1 = container1.obtainReference(Session.class);
            Session session2 = container2.obtainReference(Session.class);

            session1.set("hello", "world");
            session1.set("foo", "bar");

            Thread.sleep(SLEEP_DELAY);

            assertThat((String) session2.get("hello"), is("world"));
            assertThat((String) session2.get("foo"), is("bar"));

            DistributedSessionContainer container3 = new DistributedSessionContainer("228.5.6.7", 8666);
            container3.start();

            Thread.sleep(SLEEP_DELAY);

            Session session3 = container3.obtainReference(Session.class);

            assertThat((String) session3.get("hello"), is("world"));
            assertThat((String) session3.get("foo"), is("bar"));

            session3.delete("foo");

            Thread.sleep(SLEEP_DELAY);

            assertThat(session1.get("foo"), nullValue());
            assertThat(session2.get("foo"), nullValue());
            assertThat(session3.get("foo"), nullValue());

            container1.stop();
            container2.stop();
            container3.stop();
        }
    }
    