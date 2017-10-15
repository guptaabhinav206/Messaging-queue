package messaging.queue;

import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

public class Consumer {
    private Request topic;
    private Request subscriber;
    private String id;

    public Consumer(final Jedis jedis, final String id, final String topic) {
        this.topic = new Request("topic:" + topic, jedis);
        this.subscriber = new Request(this.topic.cat("subscribers").key(), jedis);
        this.id = id;
    }

    private void waitForMessages() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    public void consume(Callback callback) {
        while (true) {
            String message = readUntilEnd(callback.toString());
            if (message != null)
                callback.onMessage(message);
            else
                waitForMessages();
        }
    }


    public String consume(String str) {
        return readUntilEnd(str);
    }

    private String readUntilEnd(String str) {
        while (unreadMessages() > 0) {
            String message = read();
            if(message.contains(str)){
                goNext();
                return message;
            }
            else{
                System.out.println("Expression not consumed...Retry..");
                return null;
            }
        }
        return null;
    }

    private void goNext() {
        subscriber.zincrby(1, id);
    }

    private int getLastReadMessage() {
        Double lastMessageRead = subscriber.zscore(id);
        if (lastMessageRead == null) {
            Set<Tuple> zrangeWithScores = subscriber.zrangeWithScores(0, 1);
            if (zrangeWithScores.iterator().hasNext()) {
                Tuple next = zrangeWithScores.iterator().next();
                Integer lowest = (int) next.getScore() - 1;
                subscriber.zadd(lowest, id);
                return lowest;
            } else {
                return 0;
            }
        }
        return lastMessageRead.intValue();
    }

    private int getTopicSize() {
        String stopicSize = topic.get();
        int topicSize = 0;
        if (stopicSize != null) {
            topicSize = Integer.valueOf(stopicSize);
        }
        return topicSize;
    }

    public String read() {
        int lastReadMessage = getLastReadMessage();
        return topic.cat("message").cat(lastReadMessage + 1).get();
    }

    public int unreadMessages() {
        return getTopicSize() - getLastReadMessage();
    }
}