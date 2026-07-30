package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityCalculator {

    private static final String TOPIC = "stats.events-similarity.v1";

    private static final double VIEW = 0.4;
    private static final double REGISTER = 0.8;
    private static final double LIKE = 1.0;

    private final Map<Long, Map<Long, Double>> eventWeights = new ConcurrentHashMap<>();

    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();

    private final Map<Long, Map<Long, Double>> minWeightSums = new ConcurrentHashMap<>();

    private final KafkaTemplate<Void, EventSimilarityAvro> kafkaTemplate;

    public void processAction(UserActionAvro action) {

        long userId = action.getUserId();
        long eventId = action.getEventId();

        double newWeight = weight(action.getActionType());

        Map<Long, Double> users =
                eventWeights.computeIfAbsent(eventId, id -> new ConcurrentHashMap<>());

        double oldWeight = users.getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            return;
        }

        users.put(userId, newWeight);

        eventWeightSums.merge(eventId, newWeight - oldWeight, Double::sum);

        for (Map.Entry<Long, Map<Long, Double>> entry : eventWeights.entrySet()) {

            long otherEvent = entry.getKey();

            if (otherEvent == eventId) {
                continue;
            }

            Map<Long, Double> otherUsers = entry.getValue();

            Double otherWeight = otherUsers.get(userId);

            if (otherWeight == null) {
                continue;
            }

            long first = Math.min(eventId, otherEvent);
            long second = Math.max(eventId, otherEvent);

            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);

            double delta = newMin - oldMin;

            Map<Long, Double> mins =
                    minWeightSums.computeIfAbsent(first, id -> new ConcurrentHashMap<>());

            mins.merge(second, delta, Double::sum);

            sendSimilarity(first, second, action);
        }
    }

    private void sendSimilarity(long first,
                                long second,
                                UserActionAvro action) {

        double sMin = minWeightSums
                .getOrDefault(first, Map.of())
                .getOrDefault(second, 0.0);

        double sA = eventWeightSums.getOrDefault(first, 0.0);
        double sB = eventWeightSums.getOrDefault(second, 0.0);

        if (sA == 0 || sB == 0) {
            return;
        }

        double similarity = sMin / (Math.sqrt(sA) * Math.sqrt(sB));

        EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(similarity)
                .setTimestamp(Instant.ofEpochMilli(action.getTimestamp()))
                .build();

        kafkaTemplate.send(TOPIC, similarityAvro);

        log.debug("Similarity {}-{} = {}", first, second, similarity);
    }

    private double weight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW;
            case REGISTER -> REGISTER;
            case LIKE -> LIKE;
        };
    }
}