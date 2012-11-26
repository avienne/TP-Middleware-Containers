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

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MessageQueueTest {

    public static File createTempDirectory() throws IOException {
        final File temp;
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }

    File workingDir;

    @Before
    public void prepare() throws IOException {
        workingDir = createTempDirectory();
    }

    @Test
    public void check_basic_operations() {
        MessageQueue queue = new MessageQueue("check.basic.operations", workingDir);

        queue.add(new Message("check.basic.operations", "hello world"));
        queue.add(new Message("check.basic.operations", "foo"));

        assertThat(queue.size(), is(2));
        assertThat((String) queue.peek().getPayload(), is("hello world"));
        assertThat((String) queue.peek().getPayload(), is("hello world"));
        assertThat((String) queue.remove().getPayload(), is("hello world"));
        assertThat(queue.size(), is(1));
        assertThat((String) queue.remove().getPayload(), is("foo"));
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));

        queue.close();
    }

    @Test
    public void check_close_open_operations() {
        MessageQueue queue = new MessageQueue("check.basic.operations", workingDir);

        queue.add(new Message("check.basic.operations", "hello world"));
        queue.add(new Message("check.basic.operations", "foo"));

        queue.close();
        queue = new MessageQueue("check.basic.operations", workingDir);

        assertThat(queue.size(), is(2));
        assertThat((String) queue.peek().getPayload(), is("hello world"));
        assertThat((String) queue.peek().getPayload(), is("hello world"));
        assertThat((String) queue.remove().getPayload(), is("hello world"));

        queue.close();
        queue = new MessageQueue("check.basic.operations", workingDir);

        assertThat(queue.size(), is(1));
        assertThat((String) queue.remove().getPayload(), is("foo"));
        assertThat(queue.size(), is(0));
        assertThat(queue.isEmpty(), is(true));

        queue.close();
    }
}
