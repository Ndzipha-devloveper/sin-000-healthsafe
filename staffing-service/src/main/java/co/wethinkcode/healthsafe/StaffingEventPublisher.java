package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.jetbrains.annotations.Async;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.Objects;

public class StaffingEventPublisher implements AutoCloseable {

    private final ActiveMQConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StaffingEventPublisher() {
        this(MqConfig.BROKER_URL);
    }

    public StaffingEventPublisher(String brokerUrl) {
        this.connectionFactory =
                new ActiveMQConnectionFactory(
                        Objects.requireNonNull(brokerUrl)
                );
    }

    public void publish(Async.Schedule schedule) throws Exception {
        Objects.requireNonNull(schedule, "schedule");

        try (Connection connection =
                     connectionFactory.createConnection()) {

            Session session = connection.createSession(
                    false,
                    Session.AUTO_ACKNOWLEDGE
            );

            try {
                MessageProducer producer = session.createProducer(
                        session.createTopic(MqConfig.TOPIC)
                );

                try {
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);

                    String json = objectMapper.writeValueAsString(schedule);
                    TextMessage message = session.createTextMessage(json);

                    message.setStringProperty(
                            "wardId",
                            schedule.wardId()
                    );

                    producer.send(message);

                } finally {
                    producer.close();
                }

            } finally {
                session.close();
            }
        }
    }

    @Override
    public void close() {
        // Each publish operation closes its own JMS resources.
    }
}