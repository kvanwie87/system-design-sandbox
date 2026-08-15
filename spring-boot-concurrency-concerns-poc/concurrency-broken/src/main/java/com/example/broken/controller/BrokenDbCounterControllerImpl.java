package com.example.broken.controller;

import com.example.broken.entity.BrokenCounterEntity;
import com.example.broken.service.BrokenDbCounterService;
import com.example.common.controller.DbCounterController;
import com.example.common.dto.CounterResponse;
import com.example.common.dto.TransferRequest;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BrokenDbCounterControllerImpl implements DbCounterController {

    private final BrokenDbCounterService dbCounterService;

    public BrokenDbCounterControllerImpl(BrokenDbCounterService dbCounterService) {
        this.dbCounterService = dbCounterService;
    }

    @Override
    public CounterResponse increment(String name) {
        BrokenCounterEntity entity = dbCounterService.increment(name);
        return new CounterResponse(entity.getName(), entity.getValue());
    }

    @Override
    public CounterResponse decrement(String name) {
        BrokenCounterEntity entity = dbCounterService.decrement(name);
        return new CounterResponse(entity.getName(), entity.getValue());
    }

    @Override
    public CounterResponse getValue(String name) {
        BrokenCounterEntity entity = dbCounterService.getValue(name);
        return new CounterResponse(entity.getName(), entity.getValue());
    }

    @Override
    public CounterResponse transfer(TransferRequest request) {
        // Delegate to transfer service (implemented in Task 7)
        throw new UnsupportedOperationException("DB transfer not yet implemented");
    }
}
