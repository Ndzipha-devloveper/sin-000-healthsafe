package co.wethinkcode.healthsafe;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.wethinkcode.healthsafe.mq.MqConfig;

/**
 * Listens to the topic "staffing-events-topic" and saves each staffing update
 * in the StaffingStore.
 *
 * Ward-service must work even when the broker is down, so connecting happens in a
 * background thread that retries every few seconds. It also reconnects if the
 * broker goes away later.
 *
 * A plain topic subscriber only gets messages sent while it is connected.
 * That is fine here: every update replaces the older one, and staffing sends a
 * new update whenever a schedule changes.
 */
public class StaffingEventSubscriber {

    private static final long RETRY_DELAY_MS = 5000;

    private final ActiveMQConnectionFactory connectionFactory;
    private final StaffingStore store;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile boolean running = false;
    private volatile boolean subscribed = false;
    private volatile Connection connection;

    public StaffingEventSubscriber(StaffingStore store) {
        this(store, MqConfig.BROKER_URL);
    }

    public StaffingEventSubscriber(StaffingStore store, String brokerUrl) {
        this.store = store;
        this.connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    }

    /** Starts listening in the background. Returns immediately. */
    public void start() {
        running = true;
        startConnecting();
    }

    /** Stops listening and closes the connection. */
    public void stop() {
        running = false;
        subscribed = false;
        closeQuietly(connection);
    }

    /** @return true while connected to the broker and listening to the topic */
    public boolean isSubscribed() {
        return subscribed;
    }

    private void startConnecting() {
        Thread thread = new Thread(this::connectWithRetry, "staffing-subscriber");
        thread.setDaemon(true);
        thread.start();
    }

    private void connectWithRetry() {
        closeQuietly(connection);

        while (running) {
            try {
                subscribe();
                return;
            } catch (JMSException e) {
                System.err.println("Staffing subscriber cannot reach the broker ("
                        + e.getMessage() + "). Trying again in 5 seconds.");
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void subscribe() throws JMSException {
        Connection newConnection = connectionFactory.createConnection();
        try {
            newConnection.setExceptionListener(e -> {
                subscribed = false;
                System.err.println("Lost the broker connection: " + e.getMessage());
                if (running) {
                    startConnecting();
                }
            });

            Session session = newConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(session.createTopic(MqConfig.TOPIC));
            consumer.setMessageListener(this::handleMessage);
            newConnection.start();
        } catch (JMSException e) {
            closeQuietly(newConnection);
            throw e;
        }

        connection = newConnection;
        if (running) {
            subscribed = true;
        } else {
            closeQuietly(newConnection);
        }
    }

    /** A bad message is logged and skipped. It must never stop the subscriber. */
    private void handleMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                System.err.println("Ignoring a staffing event that is not text");
                return;
            }

            StaffingUpdate update = objectMapper.readValue(
                    ((TextMessage) message).getText(), StaffingUpdate.class);

            if (update.wardId() == null || update.wardId().isBlank()) {
                System.err.println("Ignoring a staffing event without a ward id");
                return;
            }

            store.put(update);
        } catch (JMSException | IOException e) {
            System.err.println("Ignoring a bad staffing event: " + e.getMessage());
        }
    }

    private static void closeQuietly(Connection toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (JMSException ignored) {
                // Nothing useful to do if closing fails.
            }
        }
    }
}