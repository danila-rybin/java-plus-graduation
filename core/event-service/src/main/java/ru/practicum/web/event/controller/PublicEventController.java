package ru.practicum.web.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.RecommendedEventDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.feign.RequestServiceClient;
import ru.practicum.stats.service.collector.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionProto;
import ru.practicum.stats.service.dashboard.InteractionsCountRequestProto;
import ru.practicum.stats.service.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.stats.service.dashboard.UserPredictionsRequestProto;
import ru.practicum.web.event.service.PublicEventService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final PublicEventService publicEventService;
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;
    private final RequestServiceClient requestServiceClient;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(required = false) Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /events");
        List<EventShortDto> events = publicEventService.getEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size
        );
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEvent(
            @PathVariable Long id,
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId) {
        log.info("GET /events/{}", id);

        EventDto event = publicEventService.getEvent(id);

        if (userId != null) {
            try {
                collectorStub.collectUserAction(
                        UserActionProto.newBuilder()
                                .setUserId(userId)
                                .setEventId(id)
                                .setActionType(ActionTypeProto.ACTION_VIEW)
                                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                                        .setSeconds(Instant.now().getEpochSecond())
                                        .build())
                                .build()
                );
                log.debug("Sent VIEW action for event {} by user {}", id, userId);
            } catch (Exception e) {
                log.error("Error sending VIEW action to collector", e);
            }
        }

        Double rating = getEventRating(id);
        event.setRating(rating);

        return ResponseEntity.ok(event);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendedEventDto>> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") long userId,
            @RequestParam(defaultValue = "10") int maxResults) {
        log.info("GET /events/recommendations for user {}", userId);

        try {
            Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(
                    UserPredictionsRequestProto.newBuilder()
                            .setUserId(userId)
                            .setMaxResults(maxResults)
                            .build()
            );
            List<RecommendedEventDto> result = new ArrayList<>();
            iterator.forEachRemaining(proto -> {
                RecommendedEventDto dto = new RecommendedEventDto();
                dto.setEventId(proto.getEventId());
                dto.setScore(proto.getScore());
                result.add(dto);
            });
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting recommendations for user {}", userId, e);
            throw new RuntimeException("Error getting recommendations", e);
        }
    }

    @PutMapping("/{eventId}/like")
    public ResponseEntity<Void> likeEvent(
            @RequestHeader("X-EWM-USER-ID") long userId,
            @PathVariable long eventId) {
        log.info("PUT /events/{}/like by user {}", eventId, userId);

        boolean participated = requestServiceClient.isUserParticipated(userId, eventId);
        if (!participated) {
            throw new BadRequestException("User has not participated in this event");
        }

        try {
            collectorStub.collectUserAction(
                    UserActionProto.newBuilder()
                            .setUserId(userId)
                            .setEventId(eventId)
                            .setActionType(ActionTypeProto.ACTION_LIKE)
                            .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                                    .setSeconds(Instant.now().getEpochSecond())
                                    .build())
                            .build()
            );
            log.debug("Sent LIKE action for event {} by user {}", eventId, userId);
        } catch (Exception e) {
            log.error("Error sending LIKE action to collector", e);
            throw new RuntimeException("Error sending like", e);
        }

        return ResponseEntity.ok().build();
    }

    private Double getEventRating(Long eventId) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addEventIds(eventId)
                    .build();
            Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(request);
            if (iterator.hasNext()) {
                return iterator.next().getScore();
            }
        } catch (Exception e) {
            log.error("Error getting rating for event {}", eventId, e);
        }
        return 0.0;
    }
}