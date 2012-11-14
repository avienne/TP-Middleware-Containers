package midcontainers.mq;

import java.io.File;

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

    /**
     * Creates and prepares a message broker.
     *
     * @param port             the port number for incoming client connections
     * @param storageDirectory the storage directory for reliable queues
     * @throws MqException if the broker could not be started, or if the reloading
     *                     of durable queues failed
     */
    public MessageBroker(int port, File storageDirectory) {
        // TODO
    }

    /**
     * Starts the server.
     *
     * @throws MqException if the server could not be started
     */
    public void start() {
        // TODO
    }

    /**
     * Stops the server.
     *
     * @throws MqException if the server could not be stopped
     */
    public void stop() {
        // TODO
    }
}
