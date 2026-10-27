package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.Objects;

/** Sends persistent equipment-failure messages to the alert queue. */
public class EquipmentFailurePublisher {
    private final ActiveMQConnectionFactory factory;
    private final ObjectMapper mapper = new ObjectMapper();
    public EquipmentFailurePublisher() { this(MqConfig.BROKER_URL); }
    public EquipmentFailurePublisher(String brokerUrl) {
        factory = new ActiveMQConnectionFactory(Objects.requireNonNull(brokerUrl));
    }
    public void publish(EquipmentFailure failure) throws Exception {
        try (Connection connection = factory.createConnection()) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                MessageProducer producer = session.createProducer(session.createQueue(MqConfig.QUEUE));
                try {
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    TextMessage message = session.createTextMessage(mapper.writeValueAsString(failure));
                    message.setStringProperty("wardId", failure.wardId());
                    message.setStringProperty("equipment", failure.equipment());
                    producer.send(message);
                } finally { producer.close(); }
            } finally { session.close(); }
        }
    }
}
