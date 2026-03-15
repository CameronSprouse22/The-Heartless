package com.heartless.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stub SMS sender that logs instead of sending.
 */
@Service("smsSender")
public class SmsMessageSender implements MessageSenderInterface {

    private static final Logger log = LoggerFactory.getLogger(SmsMessageSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[SMS] To: {}, Body: {}", to, body);
    }
}
