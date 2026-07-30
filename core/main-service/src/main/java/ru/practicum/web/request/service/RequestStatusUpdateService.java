package ru.practicum.web.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.web.request.dto.ParticipationRequestDto;
import ru.practicum.web.request.entity.ParticipationRequest;
import ru.practicum.web.request.entity.RequestStatus;
import ru.practicum.web.request.mapper.RequestMapperService;
import ru.practicum.web.request.repository.ParticipationRequestRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestStatusUpdateService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final RequestMapperService mapperService;

    @Transactional
    public EventRequestStatusUpdateResult confirmRequests(
            List<ParticipationRequest> requests,
            Event event,
            int availableSlots) {

        log.debug("Подтверждение заявок: всего {}, доступно мест {}", requests.size(), availableSlots);

        List<ParticipationRequestDto> confirmedList = new ArrayList<>();
        List<ParticipationRequestDto> rejectedList = new ArrayList<>();

        int confirmedCount = 0;
        for (ParticipationRequest request : requests) {
            if (confirmedCount < availableSlots) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmedList.add(mapperService.toDto(request));
                log.debug("Заявка {} подтверждена", request.getId());
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejectedList.add(mapperService.toDto(request));
                log.debug("Заявка {} отклонена (нет мест)", request.getId());
            }
            requestRepository.save(request);
        }

        event.setConfirmedRequests(event.getConfirmedRequests() + confirmedCount);
        eventRepository.save(event);
        log.debug("Количество подтвержденных заявок для события {} увеличено на {}",
                event.getId(), confirmedCount);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedList)
                .rejectedRequests(rejectedList)
                .build();
    }

    @Transactional
    public EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        log.debug("Отклонение {} заявок", requests.size());

        List<ParticipationRequestDto> rejectedList = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            request.setStatus(RequestStatus.REJECTED);
            requestRepository.save(request);
            rejectedList.add(mapperService.toDto(request));
            log.debug("Заявка {} отклонена", request.getId());
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(new ArrayList<>())
                .rejectedRequests(rejectedList)
                .build();
    }
}