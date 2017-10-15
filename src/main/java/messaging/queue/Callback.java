package messaging.queue;

public interface Callback {
    public void onMessage(String message);
}
