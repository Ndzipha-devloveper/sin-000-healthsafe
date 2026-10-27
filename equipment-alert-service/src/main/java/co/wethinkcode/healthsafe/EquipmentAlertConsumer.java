package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;

public class EquipmentAlertConsumer implements AutoCloseable {

    private static final long RETRY_DELAY_MS = 3000;

    private final ActiveMQConnectionFactory factory;
    private final AlertStore store;
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile boolean running;
    private volatile Connection connection;

    public EquipmentAlertConsumer(AlertStore store) {
        this(store, MqConfig.BROKER_URL);
    }

    public EquipmentAlertConsumer(AlertStore store, String brokerUrl) {
        this.store = store;
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;

        Thread thread = new Thread(
                this::consumeUntilStopped,
                "equipment-alert-consumer"
        );

        thread.setDaemon(true);
        thread.start();
    }

    private void consumeUntilStopped() {
        while (running) {
            try (Connection conn = factory.createConnection()) {

                connection = conn;

                Session session = conn.createSession(
                        true,
                        Session.SESSION_TRANSACTED
                );

                try {
                    MessageConsumer consumer = session.createConsumer(
                            session.createQueue(MqConfig.QUEUE)
                    );

                    consumer.setMessageListener(
                            message -> process(message, session)
                    );

                    conn.start();

                    while (running) {
                        Thread.sleep(500);
                    }

                    consumer.close();

                } finally {
                    session.close();
                }

            } catch (JMSException e) {
                if (running) {
                    System.err.println(
                            "Equipment alert consumer connection failed: "
                                    + e.getMessage()
                    );
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;

            } finally {
                connection = null;
            }

            if (running) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void process(Message message, Session session) {

        // Reject messages that aren't text messages.
        if (!(message instanceof TextMessage textMessage)) {
            commitOrRollback(
                    session,
                    "Discarded non-text equipment alert message"
            );
            return;
        }

        final EquipmentFailureAlert alert;

        // Parse the JSON before attempting to store the alert.
        try {
            alert = mapper.readValue(
                    textMessage.getText(),
                    EquipmentFailureAlert.class
            );

        } catch (IOException | IllegalArgumentException | JMSException e) {
            commitOrRollback(
                    session,
                    "Discarded invalid equipment alert message: "
                            + e.getMessage()
            );
            return;
        }

        // Store first, then commit the JMS transaction.
        try {
            store.append(alert);
            session.commit();

        } catch (IOException | JMSException e) {

            try {
                session.rollback();
            } catch (JMSException rollbackError) {
                System.err.println(
                        "Could not roll back alert message: "
                                + rollbackError.getMessage()
                );
            }

            System.err.println(
                    "Equipment alert processing failed; "
                            + "message rolled back: "
                            + e.getMessage()
            );
        }
    }

    private void commitOrRollback(
            Session session,
            String message
    ) {
        try {
            session.commit();

            System.err.println(message);

        } catch (JMSException commitError) {

            try {
                session.rollback();
            } catch (JMSException rollbackError) {
                System.err.println(
                        "Could not roll back alert message: "
                                + rollbackError.getMessage()
                );
            }

            System.err.println(
                    "Could not acknowledge invalid alert message: "
                            + commitError.getMessage()
            );
        }
    }

    @Override
    public void close() {
        running = false;

        Connection current = connection;

        if (current != null) {
            try {
                current.close();
            } catch (JMSException ignored) {
                // Connection may already be closed.
            }
        }
    }
}