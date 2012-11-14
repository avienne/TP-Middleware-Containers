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

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MessageBrokingTest {
/*
    @Test
    public void smoke_test() throws InterruptedException, IOException {
        MessageBroker broker = new MessageBroker(2011, MessageQueueTest.createTempDirectory());
        broker.start();

        Thread.sleep(250);
        MessageBrokerClient client = new MessageBrokerClient("127.0.0.1", 2011);

        client.close();
        broker.stop();
    }

    @Test
    public void communication_test() throws InterruptedException, IOException {
        MessageBroker broker = new MessageBroker(2012, MessageQueueTest.createTempDirectory());
        broker.start();

        Thread.sleep(250);
        MessageBrokerClient client = new MessageBrokerClient("127.0.0.1", 2012);

        assertThat(client.checkAvailabilityFrom("plop.da.plop"), is(false));

        client.send(new Message("plop.da.plop", "Plop!"));
        client.send(new Message("plop.da.plop", "Plip!"));
        client.send(new Message("plop.da.plop", "Plap!"));

        assertThat(client.checkAvailabilityFrom("plop.da.plop"), is(true));
        assertThat(client.checkAvailabilityFrom("plop=da=plop"), is(false));

        assertThat((String) client.receiveFrom("plop.da.plop").getPayload(), is("Plop!"));
        assertThat((String) client.receiveFrom("plop.da.plop").getPayload(), is("Plip!"));
        assertThat((String) client.receiveFrom("plop.da.plop").getPayload(), is("Plap!"));

        assertThat(client.checkAvailabilityFrom("plop.da.plop"), is(false));

        client.close();
        broker.stop();
    }

    @Test
    public void two_clients_communication_test() throws InterruptedException, IOException {
        MessageBroker broker = new MessageBroker(2013, MessageQueueTest.createTempDirectory());
        broker.start();

        Thread.sleep(250);
        MessageBrokerClient client1 = new MessageBrokerClient("127.0.0.1", 2013);
        MessageBrokerClient client2 = new MessageBrokerClient("127.0.0.1", 2013);

        client1.send(new Message("queue", "Hello world!"));
        assertThat(client1.checkAvailabilityFrom("queue"), is(true));
        assertThat(client2.checkAvailabilityFrom("queue"), is(true));
        assertThat((String) client2.receiveFrom("queue").getPayload(), is("Hello world!"));
        assertThat(client1.checkAvailabilityFrom("queue"), is(false));
        assertThat(client2.checkAvailabilityFrom("queue"), is(false));

        client1.close();
        client2.close();
        broker.stop();
    }
*/
}
