package com.hardwarestore.hardwarestore.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class LoginRateLimiterTests {
 @Test void accountsAndIpsAreBoundedWithoutChangingAccountState() {
  var limiter=new LoginRateLimiter(Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"),ZoneOffset.UTC));
  for(int i=0;i<10;i++)limiter.check("ip-"+i,"Test@Example.com");
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->limiter.check("different-ip","test@example.com"));
  for(int i=0;i<30;i++)limiter.check("shared-ip","account"+i+"@example.com");
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->limiter.check("shared-ip","fresh@example.com"));
 }
}
