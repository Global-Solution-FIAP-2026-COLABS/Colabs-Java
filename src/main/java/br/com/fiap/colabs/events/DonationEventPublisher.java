package br.com.fiap.colabs.events;

import br.com.fiap.colabs.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DonationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String routingKey, DonationEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
            log.info("[RabbitMQ] Evento publicado ({}): donationId={}, status={}",
                    routingKey, event.donationId(), event.status());
        } catch (Exception ex) {
            log.warn("[RabbitMQ] Broker indisponível no momento; evento {} registrado apenas em log: {}",
                    routingKey, ex.getMessage());
        }
    }
}