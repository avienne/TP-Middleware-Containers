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

import java.io.Serializable;

/**
 * Definition of a message.
 * <p/>
 * A message has a destination which is the name of the queue it should be delivered and made
 * available from.
 * <p/>
 * A message also has a payload which is the serializable object it must carry.
 *
 * @author Julien Ponge
 */
public final class Message implements Serializable {

    private final String destination;
    private final Serializable payload;

    /**
     * Builds a new message.
     *
     * @param destination the destination queue name
     * @param payload     the message payload
     */
    public Message(String destination, Serializable payload) {
        this.destination = destination;
        this.payload = payload;
    }

    /**
     * Gets the destination name.
     *
     * @return the destination name
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Gets the payload.
     *
     * @return the payload
     */
    public Serializable getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return destination.equals(message.destination) && payload.equals(message.payload);

    }

    @Override
    public int hashCode() {
        int result = destination.hashCode();
        result = 31 * result + payload.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "destination='" + destination + '\'' +
                ", payload=" + payload +
                '}';
    }
}
