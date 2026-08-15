package com.example.fixed.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Fix #2: Request-scoped bean — each HTTP request gets its own instance.
 * 
 * Spring creates a new instance of this bean for every incoming request.
 * Instance fields are safe because no two requests share the same object.
 * 
 * Use this approach when:
 * - You need to accumulate state across multiple method calls within a request
 * - Multiple services need to share request-specific context
 * - The workflow is too complex to pass everything through parameters
 * 
 * Note: @RequestScope only works within a web request context (DispatcherServlet).
 * For tests without a real HTTP request, we simulate the scoping manually.
 */
@Component
@RequestScope
public class RequestScopedContext {

    private String currentUser;
    private long runningTotal;
    private final List<String> operationLog = new ArrayList<>();

    public void beginRequest(String user) {
        this.currentUser = user;
        this.runningTotal = 0;
        this.operationLog.clear();
    }

    public void addAmount(long amount) {
        this.runningTotal += amount;
        this.operationLog.add(currentUser + " added " + amount);
    }

    public RequestResult finishRequest() {
        return new RequestResult(currentUser, runningTotal, List.copyOf(operationLog));
    }

    public record RequestResult(String user, long total, List<String> log) {
    }
}
