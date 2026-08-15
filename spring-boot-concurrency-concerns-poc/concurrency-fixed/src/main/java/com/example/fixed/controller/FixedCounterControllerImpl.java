package com.example.fixed.controller;

import com.example.common.controller.CounterController;
import com.example.common.dto.CounterResponse;
import com.example.common.dto.TransferRequest;
import com.example.fixed.service.FixedInMemoryCounterService;
import com.example.fixed.service.FixedTransferService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FixedCounterControllerImpl implements CounterController {

    private final FixedInMemoryCounterService counterService;
    private final FixedTransferService transferService;

    public FixedCounterControllerImpl(FixedInMemoryCounterService counterService,
                                      FixedTransferService transferService) {
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
