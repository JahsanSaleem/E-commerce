package com.hardwarestore.hardwarestore.service;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;

@Component
public class PasswordResetMailDispatcher {
    private final VerificationMailer mail;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), runnable -> {
                var thread = new Thread(runnable, "store-mail");
                thread.setDaemon(true);
                return thread;
            });

    public PasswordResetMailDispatcher(VerificationMailer mail) { this.mail = mail; }

    public void sendCode(String email, String code) {
        afterCommit(() -> mail.sendResetCode(email, code));
    }
    public void sendConfirmation(String email) {
        afterCommit(() -> mail.sendPasswordChanged(email));
    }
    public void sendOrder(String email,String subject,String body) { afterCommit(() -> mail.sendOrderMessage(email,subject,body)); }
    private void afterCommit(Runnable send) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    executor.execute(() -> {
                        try { send.run(); }
                        catch (RuntimeException failure) { warn(); }
                    });
                } catch (RejectedExecutionException failure) { warn(); }
            }
        });
    }
    private void warn() {
        // Provider messages may contain secrets; log only an operational hint.
        LoggerFactory.getLogger(PasswordResetMailDispatcher.class)
                .warn("Store email delivery failed; check mail provider settings or queue capacity.");
    }
    @PreDestroy public void shutdown() { executor.shutdownNow(); }
}
