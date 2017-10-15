package messaging.queue;

import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by abhinav on 12/10/17.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Producer p = new Producer(new Jedis("localhost"),"Json messages");
        System.out.println("Enter number of json messages");
        int n = Integer.parseInt(br.readLine().trim());
        p.clean();
        while(n-->0) {
            System.out.println("Enter a json message to be published");
            /**
             * message format: A valid json
             */
            String json = br.readLine();
            p.publish(json);
        }

        Consumer c1 = new Consumer(new Jedis("localhost"),"consumer identifier","Json messages");
        System.out.println("Enter a expression to be consumed");
        String expr1 = br.readLine();
        String message = c1.consume(expr1);
        System.out.println(message);
        System.out.println("Enter a expression to be consumed");
        String expr2 = br.readLine();
        Consumer c2 = new Consumer(new Jedis("localhost"),"consumer identifier","Json messages");
        System.out.println(c2.consume(expr2));
    }
}
