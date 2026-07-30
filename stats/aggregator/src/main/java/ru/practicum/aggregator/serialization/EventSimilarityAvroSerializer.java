package ru.practicum.aggregator.serialization;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityAvroSerializer extends BaseAvroSerializer<EventSimilarityAvro> {
    public EventSimilarityAvroSerializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}