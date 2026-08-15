package com.example.fixed.service;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

/**
 * Service that depends on the request-scoped context.
 * The proxy ensures each request gets its own RequestScopedContext instance.
 */
@Service
public class RequestScopedCounterService {

    private final RequestScopedContext context;

    public RequestScopedCounterService(RequestScopedContext context) {
        this.context = context;
    }

    public void beginRequest(String user) {
        context.beginRequest(user);
    }

    public void addAmount(long amount) {
        context.addAmount(amount);
    }

    public RequestScopedContext.RequestResult finishRequest() {
        return context.finishRequest();
    }
}
