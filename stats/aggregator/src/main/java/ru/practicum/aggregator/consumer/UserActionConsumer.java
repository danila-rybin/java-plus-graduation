package ru.practicum.aggregator.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.service.SimilarityCalculator;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final SimilarityCalculator similarityCalculator;

    @KafkaListener(
            topics = "stats.user-actions.v1",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(UserActionAvro action, Acknowledgment acknowledgment) {
        log.info("Received user action in aggregator: userId={}, eventId={}, action={}, timestamp={}",
                action.getUserId(), action.getEventId(), action.getActionType(), action.getTimestamp());

        try {
            similarityCalculator.processAction(action);
            acknowledgment.acknowledge();
            log.info("Successfully processed action");
        } catch (Exception e) {
            log.error("Error processing action: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}