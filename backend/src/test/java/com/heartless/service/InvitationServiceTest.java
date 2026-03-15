package com.heartless.service;

import com.heartless.messaging.MessageSenderInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitationServiceTest {

    private MessageSenderInterface emailSender;
    private MessageSenderInterface smsSender;
    private InvitationService invitationService;

    @BeforeEach
    void setUp() {
        emailSender = mock(MessageSenderInterface.class);
        smsSender = mock(MessageSenderInterface.class);
        invitationService = new InvitationService(emailSender, smsSender);
    }

    @Test
    void sendsEmailForEmailContact() {
        invitationService.sendInvitation("alice@test.com", "ABC123");
        verify(emailSender).send(eq("alice@test.com"), anyString(), contains("ABC123"));
        verifyNoInteractions(smsSender);
    }

    @Test
    void sendsSmsForPhoneContact() {
        invitationService.sendInvitation("+1234567890", "ABC123");
        verify(smsSender).send(eq("+1234567890"), anyString(), contains("ABC123"));
        verifyNoInteractions(emailSender);
    }

    @Test
    void detectsEmailByAtSign() {
        invitationService.sendInvitation("user@domain.com", "XYZ789");
        verify(emailSender).send(eq("user@domain.com"), anyString(), anyString());
    }

    @Test
    void detectsPhoneByNoAtSign() {
        invitationService.sendInvitation("5551234567", "XYZ789");
        verify(smsSender).send(eq("5551234567"), anyString(), anyString());
    }
}
