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

package midcontainers.mq;

import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * A message broker client.
 *
 * @see midcontainers.mq.Message
 * @see midcontainers.mq.MessageBroker
 */
public class MessageBrokerClient {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    /**
     * Connects to a message broker.
     *
     * @param address the broker host address
     * @param port    the broker server port
     * @throws MqException if the connection to the broker failed
     */
    public MessageBrokerClient(String address, int port) {
        try{
            Socket socket = new Socket(address, port);
        // /!\ ORDER MATTERS OR YOU DEADLOCK /!\                
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream (socket.getInputStream());
        }catch (IOException e){
            throw new MqException(e);
        }

    }

    /**
     * Closes the connection to the broker.
     *
     * @throws MqException if closing the connection failed
     */
    public void close() {
        // TODO
    }

    /**
     * Sends a message to the broker.
     *
     * @param message the message to be sent, cannot be <code>null</code>
     * @throws MqException if the message could not be reliably delivered
     */
    public void send(Message message) {
        try{
            System.out.println("------- send -----");
            out.writeObject(Operand.SEND);
            out.writeObject(message);
            out.flush();
            Operand ack = (Operand) in.readObject();
            System.out.println("------- ACK -----");
        }catch (IOException e){
            throw new MqException (e);
        }catch (ClassNotFoundException e){
            throw new MqException(e);
        }
    }

    /**
     * Checks for the availability of a message on a specific queue.
     *
     * @param destination the queue name
     * @return <code>true</code> if a message is available; <code>false</code>otherwise
     * @throws MqException if the connection to the broker failed
     */
    public boolean checkAvailabilityFrom(String destination) {
        
        try{
            System.out.println("------- chk -----");
            out.writeObject(Operand.CHECK);
            out.writeObject(destination);
            out.flush();
            Boolean b = (Boolean) in.readBoolean();
            return b;
        }catch (IOException e){
            throw new MqException (e);
        }
    }

    /**
     * Receive a message from the broker. This operation is not blocking if no message is available,
     * and returns <code>null</code> instead.
     *
     * @param destination the queue name
     * @return the oldest message to be delivered, or <code>null</code> if the queue was empty
     * @throws MqException if the connection to the broker failed
     */
    public Message receiveFrom(String destination) {
     try{
            System.out.println("------- chk -----");
            out.writeObject(Operand.RECEIVE);
            out.writeObject(destination);
            out.flush();
            Message msg = (Message) in.readObject();
            return msg;
        }catch (IOException e){
            throw new MqException (e);
        }catch(ClassNotFoundException e){
            throw new MqException (e);
        }
    }
}
