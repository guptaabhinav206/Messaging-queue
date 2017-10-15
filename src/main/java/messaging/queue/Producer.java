package messaging.queue;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;

public class Producer {
    private Jedis jedis;
    private Request topic;
    private Request subscriber;

    public Producer(final Jedis jedis, final String topic) {
        this.jedis = jedis;
        this.topic = new Request("topic:" + topic, jedis);
        this.subscriber = new Request(this.topic.cat("subscribers").key(), jedis);
    }

    public void publish(final String message) {
        publish(message, 0);
    }

    protected Integer getNextMessageId() {
        final String slastMessageId = topic.get();
        Integer lastMessageId = 0;
        if (slastMessageId != null) {
            lastMessageId = Integer.parseInt(slastMessageId);
        }
        lastMessageId++;
        return lastMessageId;
    }

    public void clean() {
        Set<Tuple> zrangeWithScores = subscriber.zrangeWithScores(0, 1);
        Tuple next = zrangeWithScores.iterator().next();
        Integer lowest = (int) next.getScore();
        topic.cat("message").cat(lowest).del();
    }

    /**
     * 
     * @param message
     *            message
     * @param seconds
     *            expiry time
     */
    public void publish(String message, int seconds) {
        List<Object> exec = null;
        Integer lastMessageId = null;
        do {
            topic.watch();
            lastMessageId = getNextMessageId();
            Transaction trans = jedis.multi();
            String msgKey = topic.cat("message").cat(lastMessageId).key();
            trans.set(msgKey, message);
            trans.set(topic.key(), lastMessageId.toString());
            if (seconds > 0)
                trans.expire(msgKey, seconds);
            exec = trans.exec();
        } while (exec == null);
    }
}