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

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * A reliable message queue.
 * <p/>
 * Messages stored to this queue are guaranteed durability as queues restore state
 * from their backing storage files.
 * <p/>
 * The current implementation is based on append-only files for the received messages
 * and for delivery snapshots.
 * <p/>
 * <strong>Improvements to this implementation should support a periodical cleanup
 * routine to prevent the append-only files to grow indefinitely!</strong>
 *
 * @author Julien Ponge
 */
class MessageQueue {

    private final String name;

    private final ObjectOutputStream storageOutputStream;
    private final DataOutputStream checkpointOutputStream;

    private long checkpoint = 0L;
    private final Queue<Message> queue = new LinkedList<Message>();

    private static final Logger logger = Logger.getLogger(MessageQueue.class.getName());

    public MessageQueue(String name, File storageDirectory) {
        this.name = name;

        File storageFile = new File(storageDirectory, name);
        File checkpointFile = new File(storageDirectory, name + "-checkpoint");
        reloadQueue(storageFile, checkpointFile);

        try {
            storageOutputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(storageFile, true)));
            checkpointOutputStream = new DataOutputStream(new FileOutputStream(checkpointFile, true));
        } catch (IOException e) {
            throw new MqException(e);
        }
    }

    public void close() {
        try {
            checkpointOutputStream.close();
            storageOutputStream.close();
        } catch (IOException e) {
            throw new MqException(e);
        }
        logger.info("Closed queue: " + name);
    }

    public Message peek() {
        return queue.peek();
    }

    public Message remove() {
        try {
            checkpointOutputStream.writeLong(checkpoint + 1);
            checkpointOutputStream.flush();
        } catch (IOException e) {
            throw new MqException(e);
        }
        checkpoint = checkpoint + 1;
        logger.info("New checkpoint value is " + checkpoint + " for " + name);
        return queue.remove();
    }

    public void add(Message message) {
        try {
            storageOutputStream.writeObject(message);
            storageOutputStream.flush();
        } catch (IOException e) {
            throw new MqException(e);
        }
        logger.info("New message added to " + name);
        queue.add(message);
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    private void reloadQueue(File storageFile, File checkpointFile) {
        if (!storageFile.exists()) {
            return;
        }
        try {
            logger.info("Reloading queue: " + name + " from " + storageFile);
            if (checkpointFile.exists() && checkpointFile.length() > 0) {
                long skipInCheckpoint = (checkpointFile.length() / 8) - 1;
                DataInputStream checkpointIn = new DataInputStream(new FileInputStream(checkpointFile));
                long skipped = 0;
                while (skipped < skipInCheckpoint) {
                    skipped = skipped + checkpointIn.skip(skipInCheckpoint);
                }
                checkpoint = checkpointIn.readLong();
                checkpointIn.close();
            }
            logger.info("Last checkpoint for " + name + " is " + checkpoint + " in " + checkpointFile);

            ObjectInputStream storageIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(storageFile)));
            long cur = 0L;
            while (cur < checkpoint) {
                storageIn.readObject();
                cur = cur + 1;
            }
            logger.info("Skipped " + (cur) + " entries in " + storageFile);
            try {
                while (true) {
                    queue.add((Message) storageIn.readObject());
                }
            } catch (StreamCorruptedException ignored) {
                storageIn.close();
            } catch (EOFException ignored) {
                storageIn.close();
            }
            logger.info("Loaded " + queue.size() + " messages back into " + name);

        } catch (FileNotFoundException e) {
            throw new MqException(e);
        } catch (IOException e) {
            throw new MqException(e);
        } catch (ClassNotFoundException e) {
            throw new MqException(e);
        }
    }
}
