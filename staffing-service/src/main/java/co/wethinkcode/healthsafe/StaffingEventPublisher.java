package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.DeliveryMode;
import java.util.Objects;

/** Publishes computed staffing schedules as events to the shared topic. */
public class StaffingEventPublisher implements AutoCloseable {
    private final ActiveMQConnectionFactory factory;
    private final ObjectMapper mapper = new ObjectMapper();

    public StaffingEventPublisher() { this(MqConfig.BROKER_URL); }

    public StaffingEventPublisher(String brokerUrl) {
        this.factory = new ActiveMQConnectionFactory(Objects.requireNonNull(brokerUrl));
    }

    public void publish(Schedule schedule) throws Exception {
        Objects.requireNonNull(schedule, "schedule");
        try (Connection connection = factory.createConnection()) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                MessageProducer producer = session.createProducer(session.createTopic(MqConfig.TOPIC));
                try {
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    TextMessage message = session.createTextMessage(mapper.writeValueAsString(schedule));
                    message.setStringProperty("wardId", schedule.wardId());
                    producer.send(message);
                } finally {
                    producer.close();
                }
            } finally {
                session.close();
            }
        }
    }

    @Override public void close() { /* Each publish call owns and closes its JMS resources. */ }
}
