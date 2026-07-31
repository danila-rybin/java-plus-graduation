package ru.practicum.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumerService {

    private final UserActionRepository userActionRepository;

    @KafkaListener(
            topics = "stats.user-actions.v1",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(UserActionAvro action, Acknowledgment acknowledgment) {
        log.info("Received user action: userId={}, eventId={}, action={}, timestamp={}",
                action.getUserId(), action.getEventId(), action.getActionType(), action.getTimestamp());

        long timestampMillis = action.getTimestamp();
        LocalDateTime timestamp = Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();

        double weight = getWeight(action.getActionType());
        log.info("Converted weight: {} for action type {}", weight, action.getActionType());

        List<UserAction> existing = userActionRepository.findByUserIdAndEventId(action.getUserId(), action.getEventId());
        if (!existing.isEmpty()) {
            log.info("Found existing actions for user {} and event {}: {}",
                    action.getUserId(), action.getEventId(), existing.size());
            for (UserAction e : existing) {
                log.info("Existing: userId={}, eventId={}, weight={}, timestamp={}",
                        e.getUserId(), e.getEventId(), e.getWeight(), e.getTimestamp());
            }
        }

        UserAction entity = UserAction.builder()
                .userId(action.getUserId())
                .eventId(action.getEventId())
                .actionType(action.getActionType().toString())
                .weight(weight)
                .timestamp(timestamp)
                .build();

        UserAction saved = userActionRepository.save(entity);
        log.info("Saved user action: id={}, userId={}, eventId={}, weight={}, timestamp={}",
                saved.getId(), saved.getUserId(), saved.getEventId(), saved.getWeight(), saved.getTimestamp());

        acknowledgment.acknowledge();
    }

    private double getWeight(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW:
                return 0.4;
            case REGISTER:
                return 0.8;
            case LIKE:
                return 1.0;
            default:
                return 0.0;
        }
    }
}