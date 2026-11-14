package br.com.fiap.colabs.events;

import br.com.fiap.colabs.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DonationEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void onDonationEvent(DonationEvent event) {
        log.info("[RabbitMQ Consumer] Processando evento {} | Doação #{} ('{}') | Status: {} | Urgência: {}",
                event.eventType(), event.donationId(), event.title(), event.status(), event.urgency());
    }
}