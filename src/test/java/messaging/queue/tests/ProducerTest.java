package messaging.queue.tests;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import messaging.queue.Callback;
import messaging.queue.Consumer;
import messaging.queue.Producer;

public class ProducerTest extends Assert {
    @Before
    public void setUp() throws IOException {
        Jedis jedis = new Jedis("localhost");
        jedis.flushAll();
        jedis.disconnect();
    }

    @Test
    public void publishAndConsume() {
        Producer p = new Producer(new Jedis("localhost"), "foo");
        Consumer c = new Consumer(new Jedis("localhost"), "a subscriber", "foo");

        p.publish("hello world!");
        assertEquals("hello world!", c.consume("hello"));
    }

    @Test
    public void publishAndRead() {
        Producer p = new Producer(new Jedis("localhost"), "foo");
        Consumer c = new Consumer(new Jedis("localhost"), "a subscriber", "foo");

        p.publish("hello world!");
        assertEquals("hello world!", c.read());
        assertEquals("hello world!", c.read());
    }

    @Test
    public void unreadMessages() {
        Producer p = new Producer(new Jedis("localhost"), "foo");
        Consumer c = new Consumer(new Jedis("localhost"), "a subscriber", "foo");

        assertEquals(0, c.unreadMessages());
        p.publish("hello world!");
        assertEquals(1, c.unreadMessages());
        p.publish("hello world!");
        assertEquals(2, c.unreadMessages());
        c.consume("hiii");
        assertEquals(1, c.unreadMessages());
    }

    @Test
    public void raceConditionsWhenPublishing() throws InterruptedException {
        Producer slow = new SlowProducer(new Jedis("localhost"), "foo");
        Consumer c = new Consumer(new Jedis("localhost"), "a subscriber", "foo");

        slow.publish("a");
        Thread t = new Thread(new Runnable() {
            public void run() {
                Producer fast = new Producer(new Jedis("localhost"), "foo");
                fast.publish("b");
            }
        });
        t.start();
        t.join();

        assertEquals("a", c.consume("a"));
        assertEquals("b", c.consume("b"));
    }

    @Test
    public void eraseOldMessages() {
        Producer p = new Producer(new Jedis("localhost"), "foo");
        Consumer c = new Consumer(new Jedis("localhost"), "a subscriber", "foo");

        p.publish("a");
        p.publish("b");

        assertEquals("a", c.consume("a"));

        p.clean();

        Consumer nc = new Consumer(new Jedis("localhost"), "new subscriber",
                "foo");

        assertEquals("b", c.consume("b"));
        assertEquals("b", nc.consume("b"));
        assertNull(c.consume("a"));
        assertNull(nc.consume("a"));
    }

    class SlowProducer extends Producer {
        private long sleep;

        public SlowProducer(Jedis jedis, String topic) {
            this(jedis, topic, 500L);
        }

        public SlowProducer(Jedis jedis, String topic, long sleep) {
            super(jedis, topic);
            this.sleep = sleep;
        }

        protected Integer getNextMessageId() {
            Integer nextMessageId = super.getNextMessageId();
            sleep(sleep);
            return nextMessageId;
        }
    }

    class SlowConsumer extends Consumer {
        private long sleep;

        public SlowConsumer(Jedis jedis, String id, String topic) {
            this(jedis, id, topic, 500L);
        }

        public SlowConsumer(Jedis jedis, String id, String topic, long sleep) {
            super(jedis, id, topic);
            this.sleep = sleep;
        }


        @Override
        public void consume(Callback callback) {
            sleep(sleep);
            super.consume(callback);
        }
    }

    @Test
    public void expiredMessages() throws InterruptedException {
        Consumer c = new SlowConsumer(new Jedis("localhost"), "a consumer",
                "foo", 2000L);
        Producer p = new Producer(new Jedis("localhost"), "foo");
        p.publish("un mensaje", 1);
        assertNull(c.consume("a"));
    }

    private void sleep(long sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
        }
    }

    @Test
    public void firstMessageExpired() throws InterruptedException {
        Consumer c = new SlowConsumer(new Jedis("localhost"), "a consumer",
                "foo", 2000L);
        Producer p = new Producer(new Jedis("localhost"), "foo");
        p.publish("1", 1);
        p.publish("2", 0);

        assertEquals("2", c.consume("a"));
    }
}