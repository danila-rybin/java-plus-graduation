package ru.practicum.collector.serialization;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionAvroSerializer extends BaseAvroSerializer<UserActionAvro> {
    public UserActionAvroSerializer() {
        super(UserActionAvro.getClassSchema());
    }
}