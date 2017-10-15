A simple messaging queue based on Jedis (Java client for Redis). I have used redis to store all the transactions in memory.
The application is based on producer consumer concept. Consumer will be notified once the producer produces any json message
in the queue. When the queue is full, producer will wait for any consumer to consume the json message.  If the queue is
empty, consumer will wait for producer to produce and notify.

Functional specs followed:
1. Queue holds json messages
2. Allow subscription of Consumers to messages that match a particular expression
3. Consumers register callbacks that will be invoked whenever there is a new message
4. Queue will have one producer and multiple consumers
5. Queue is bounded in size and completely held in-memory. Size is configurable.
6. Handle concurrent writes and reads consistently between producer and consumers.

Future scope:

1. Consumers dependency on one another. This can be done using a config or it can checked using dependent consumer
objects are null or not before consuming any message.
2. Json expression matching can be done without using any external library. We can convert a json expression
into a n-ary tree and parse the json expression using that n-ary tree.
3. We can make this application distributed across multiple nodes.

Steps To run this application:

1. Clone the git repo.
2. Compile all java files (javac *.java)
3. Run the main file.

To use the application just as a producer:

	Producer p = new Producer(new Jedis("localhost"),"json messages");
	p.publish("some json message");

To use the application just as a consumer you can consume messages as they become available (this will block if there are no new messages):

	Consumer c = new Consumer(new Jedis("localhost"),"consumer identifier","json messages");
	c.consume(new Callback() {
		public void onMessage(String message) {
			//do something here with the message
		}
	});

Consume next waiting message and return right away:

	Consumer c = new Consumer(new Jedis("localhost"),"consumer identifier","some json message");
	String message = c.consume();

Read next message without removing it from the queue:

	Consumer c = new Consumer(new Jedis("localhost"),"consumer identifier","some json message");
	String message = c.read();