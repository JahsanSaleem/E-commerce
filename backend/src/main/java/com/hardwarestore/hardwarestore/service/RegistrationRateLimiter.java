package com.hardwarestore.hardwarestore.service;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

// Local-instance abuse protection, in addition to persistent email/code limits.
@Component
public class RegistrationRateLimiter {
    private final Clock clock;
    private final Map<String, List<Instant>> requests = new HashMap<>();
    public RegistrationRateLimiter(Clock clock) { this.clock = clock; }
    public synchronized void check(String address) {
        var now = clock.instant();
        requests.values().forEach(times -> times.removeIf(time -> time.isBefore(now.minusSeconds(600))));
        requests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        var times = requests.computeIfAbsent(address, key -> new ArrayList<>());
        if (times.size() >= 5 || requests.size() > 10000)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many email requests. Please wait 10 minutes.");
        times.add(now);
    }
}
