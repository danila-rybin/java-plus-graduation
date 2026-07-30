package ru.practicum.collector.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.collector.service.KafkaProducerService;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionProto;
import com.google.protobuf.Empty;

@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaProducerService kafkaProducerService;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        kafkaProducerService.sendAction(request);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}