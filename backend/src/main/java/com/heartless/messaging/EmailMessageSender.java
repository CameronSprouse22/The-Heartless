package com.heartless.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stub email sender that logs instead of sending.
 */
@Service("emailSender")
public class EmailMessageSender implements MessageSenderInterface {

    private static final Logger log = LoggerFactory.getLogger(EmailMessageSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[EMAIL] To: {}, Subject: {}, Body: {}", to, subject, body);
    }
}
