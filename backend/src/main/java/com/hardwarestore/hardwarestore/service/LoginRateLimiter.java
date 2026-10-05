package com.hardwarestore.hardwarestore.service;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
@Component public class LoginRateLimiter {
 private final Clock clock;private final Map<String,List<Instant>> attempts=new HashMap<>();
 public LoginRateLimiter(Clock clock){this.clock=clock;}
 public synchronized void check(String address,String email) {
  var now=clock.instant();attempts.values().forEach(list->list.removeIf(time->!time.isAfter(now.minusSeconds(600))));attempts.entrySet().removeIf(e->e.getValue().isEmpty());
  String ip="ip:"+address,account="email:"+email.trim().toLowerCase(Locale.ROOT);
  if(attempts.size()>=10000 || attempts.getOrDefault(ip,List.of()).size()>=30 || attempts.getOrDefault(account,List.of()).size()>=10)
   throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many sign-in attempts. Please wait ten minutes.");
  attempts.computeIfAbsent(ip,key->new ArrayList<>()).add(now);attempts.computeIfAbsent(account,key->new ArrayList<>()).add(now);
 }
}
