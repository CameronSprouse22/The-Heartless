package com.heartless.service;

import com.heartless.messaging.MessageSenderInterface;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Dispatches invitations via email or SMS based on contact format.
 */
@Service
public class InvitationService {

    private final MessageSenderInterface emailSender;
    private final MessageSenderInterface smsSender;

    public InvitationService(
            @Qualifier("emailSender") MessageSenderInterface emailSender,
            @Qualifier("smsSender") MessageSenderInterface smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    public void sendInvitation(String contact, String gameCode) {
        String subject = "Game Invitation - The Heartless";
        String body = "You've been invited to play! Join: /join/" + gameCode;
        if (isEmail(contact)) {
            emailSender.send(contact, subject, body);
        } else {
            smsSender.send(contact, subject, body);
        }
    }

    private boolean isEmail(String contact) {
        return contact != null && contact.contains("@");
    }
}
