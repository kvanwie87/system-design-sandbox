package com.example.broken.controller;

import com.example.broken.service.BrokenInMemoryCounterService;
import com.example.broken.service.BrokenTransferService;
import com.example.common.controller.CounterController;
import com.example.common.dto.CounterResponse;
import com.example.common.dto.TransferRequest;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BrokenCounterControllerImpl implements CounterController {

    private final BrokenInMemoryCounterService counterService;
    private final BrokenTransferService transferService;

    public BrokenCounterControllerImpl(BrokenInMemoryCounterService counterService,
                                       BrokenTransferService transferService) {
        this.counterService = counterService;
        this.transferService = transferService;
    }

    @Override
    public CounterResponse increment(String name) {
        long value = counterService.increment(name);
        return new CounterResponse(name, value);
    }

    @Override
    public CounterResponse decrement(String name) {
        long value = counterService.decrement(name);
        return new CounterResponse(name, value);
    }

    @Override
    public CounterResponse getValue(String name) {
        long value = counterService.getValue(name);
        return new CounterResponse(name, value);
    }

    @Override
    public CounterResponse transfer(TransferRequest request) {
        transferService.transfer(request.fromCounter(), request.toCounter(), request.amount());
        long fromValue = counterService.getValue(request.fromCounter());
        return new CounterResponse(request.fromCounter(), fromValue);
    }
}
