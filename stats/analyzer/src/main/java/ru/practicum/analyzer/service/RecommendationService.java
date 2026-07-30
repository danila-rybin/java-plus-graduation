package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.stats.service.dashboard.RecommendedEventProto;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository similarityRepository;

    private static final int DEFAULT_K = 10;
    private static final int DEFAULT_RECENT_LIMIT = 5;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.info("Getting recommendations for user {} (max {})", userId, maxResults);

        List<UserAction> allUserActions = userActionRepository.findByUserIdOrderByTimestampDesc(userId);
        if (allUserActions.isEmpty()) {
            log.info("User {} has no actions, returning empty", userId);
            return Collections.emptyList();
        }

        int limit = Math.min(DEFAULT_RECENT_LIMIT, allUserActions.size());
        List<UserAction> recentActions = allUserActions.subList(0, limit);

        Set<Long> interactedEvents = allUserActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        Map<Long, Double> candidateScores = new HashMap<>();

        for (UserAction action : recentActions) {
            Long eventId = action.getEventId();
            List<EventSimilarity> similarities = similarityRepository.findByEventAOrEventB(eventId);

            for (EventSimilarity sim : similarities) {
                Long otherEvent = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                if (!interactedEvents.contains(otherEvent)) {
                    candidateScores.merge(otherEvent, sim.getScore(), Double::max);
                }
            }
        }

        return candidateScores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(maxResults)
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        log.info("Getting similar events for event {}, user {}, max {}", eventId, userId, maxResults);

        List<EventSimilarity> similarities = similarityRepository.findByEventAOrEventB(eventId);
        if (similarities.isEmpty()) {
            log.info("No similarities found for event {}", eventId);
            return Collections.emptyList();
        }

        log.info("Found {} similarities for event {}", similarities.size(), eventId);
        for (EventSimilarity sim : similarities) {
            log.debug("Similarity: {} <-> {} = {}", sim.getEventA(), sim.getEventB(), sim.getScore());
        }

        Set<Long> interacted = userActionRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        log.info("User {} interacted with {} events", userId, interacted.size());

        return similarities.stream()
                .map(sim -> {
                    Long other = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return Map.entry(other, sim.getScore());
                })
                .filter(e -> {
                    boolean notInteracted = !interacted.contains(e.getKey());
                    if (!notInteracted) {
                        log.debug("Filtering out event {} because user already interacted with it", e.getKey());
                    }
                    return notInteracted;
                })
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(maxResults)
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("Getting interactions count for events: {}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecommendedEventProto> result = new ArrayList<>();

        for (Long eventId : eventIds) {
            log.info("=== Processing event {} ===", eventId);

            List<UserAction> allActions = userActionRepository.findByEventId(eventId);
            log.info("Found {} total actions for event {}", allActions.size(), eventId);

            for (UserAction action : allActions) {
                log.debug("Action: userId={}, weight={}, type={}",
                        action.getUserId(), action.getWeight(), action.getActionType());
            }

            List<Object[]> maxWeightsByUser = userActionRepository.findMaxWeightsByEventId(eventId);
            log.info("Found {} unique users with max weights for event {}", maxWeightsByUser.size(), eventId);

            double totalWeight = 0.0;
            for (Object[] row : maxWeightsByUser) {
                Long userId = (Long) row[0];
                Double maxWeight = (Double) row[1];
                log.debug("User {} max weight: {}", userId, maxWeight);
                totalWeight += maxWeight != null ? maxWeight : 0.0;
            }

            log.info("Event {}: totalWeight={}, userCount={}", eventId, totalWeight, maxWeightsByUser.size());

            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(totalWeight)
                    .build());
        }

        log.info("Returning interactions count for {} events", result.size());
        return result;
    }
}