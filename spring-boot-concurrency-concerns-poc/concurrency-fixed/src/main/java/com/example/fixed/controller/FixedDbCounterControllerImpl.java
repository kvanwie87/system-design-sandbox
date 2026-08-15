package com.example.fixed.controller;

import com.example.common.controller.DbCounterController;
import com.example.common.dto.CounterResponse;
import com.example.common.dto.TransferRequest;
import com.example.common.entity.CounterEntity;
import com.example.fixed.service.FixedDbCounterService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FixedDbCounterControllerImpl implements DbCounterController {

    private final FixedDbCounterService dbCounterService;

    public FixedDbCounterControllerImpl(FixedDbCounterService dbCounterService) {
        this.dbCounterService = dbCounterService;
    }

    @Override
    public CounterResponse increment(String name) {
        CounterEntity entity = dbCounterService.incrementPessimistic(name);
        return new CounterResponse(entity.getName(), entity.getValue());
    }

    @Override
    public CounterResponse decrement(String name) {
        CounterEntity entity = dbCounterService.decrementPessimistic(name);
        return new CounterResponse(entity.getName(), entity.getValue());
    }

    @Override
    public CounterResponse getValue(String name) {
        CounterEntity entity = dbCounterService.getValue(name);
        return new CounterResponse(entity.getName(), entity.getValue());
    }

    @Override
    public CounterResponse transfer(TransferRequest request) {
        // Delegate to transfer service (implemented in Task 8)
        throw new UnsupportedOperationException("DB transfer not yet implemented");
    }
}
