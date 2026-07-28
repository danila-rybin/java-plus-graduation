package ru.practicum.web.request.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.request.dto.ParticipationRequestDto;
import ru.practicum.web.request.entity.ParticipationRequest;
import ru.practicum.web.request.entity.RequestStatus;
import ru.practicum.web.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestMapperService {

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestMapper.toDto(request);
    }

    public List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        return requests.stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    public ParticipationRequest createRequest(User user, Event event) {
        RequestStatus status = determineInitialStatus(event);

        return ParticipationRequest.builder()
                .created(LocalDateTime.now().withNano(0))
                .event(event)
                .requester(user)
                .status(status)
                .build();
    }

    private RequestStatus determineInitialStatus(Event event) {
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            return RequestStatus.CONFIRMED;
        }
        return RequestStatus.PENDING;
    }
}