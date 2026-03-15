package com.heartless.messaging;

/**
 * Contract for sending notifications (email/SMS).
 */
public interface MessageSenderInterface {

    void send(String to, String subject, String body);
}
