package com.gamemoyeo.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.port.out.RiotAccountPort;
import com.gamemoyeo.member.application.port.out.RiotAuthorizationPort;
import com.gamemoyeo.member.application.port.out.RiotLinkTicketPort;
import com.gamemoyeo.member.domain.RiotAccount;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RiotLinkServiceTest {
    private final RiotAccountPort accounts = mock(RiotAccountPort.class);
    private final RiotAuthorizationPort authorization = mock(RiotAuthorizationPort.class);
    private final RiotLinkTicketPort tickets = mock(RiotLinkTicketPort.class);
    private final RiotLinkService service = new RiotLinkService(accounts, authorization, tickets);
    private final String token = "a".repeat(43);

    @Test
    void disabledStartNeverCallsProviderOrIssuesState() {
        assertThatThrownBy(() -> service.start(7)).isInstanceOf(ApiException.class);
        verifyNoInteractions(tickets, accounts);
    }
    @Test
    void pkceMatchesRfc7636Vector() {
        assertThat(RiotLinkService.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
            .isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }
    @Test
    void invalidBrowserOrExpiredStateDoesNotExchangeCode() {
        when(authorization.enabled()).thenReturn(true);
        when(tickets.consumeState(token, token)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.callback(token, token, "provider-code")).isInstanceOf(ApiException.class);
        verify(authorization, org.mockito.Mockito.never()).exchange(anyString(), anyString());
    }
    @Test
    void callbackOnlyStagesVerifiedIdentityUntilAuthenticatedCompletion() {
        when(authorization.enabled()).thenReturn(true);
        when(tickets.consumeState(token, token)).thenReturn(Optional.of(new RiotLinkTicketPort.State(7, token, "verifier")));
        when(authorization.exchange("provider-code", "verifier")).thenReturn(new RiotAccount("puuid", "Player", "KR1"));
        assertThat(service.callback(token, token, "provider-code")).matches("[A-Za-z0-9_-]{43}");
        verify(tickets).saveCompletion(anyString(), any(), any());
        verifyNoInteractions(accounts);
    }
    @Test
    void completionCannotSwitchMembersOrReplayTicket() {
        when(authorization.enabled()).thenReturn(true);
        when(tickets.consumeCompletion(token, 8)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.complete(8, token)).isInstanceOf(ApiException.class);
        verifyNoInteractions(accounts);
    }
    @Test
    void completionLinksAuthenticatedMemberToProviderVerifiedAccount() {
        var account = new RiotAccount("puuid", "Player", "KR1");
        when(authorization.enabled()).thenReturn(true);
        when(tickets.consumeCompletion(token, 7)).thenReturn(Optional.of(new RiotLinkTicketPort.Completion(7, account)));
        service.complete(7, token);
        verify(accounts).link(7, account);
    }
    @Test
    void cancellationConsumesStateWithoutCallingRiot() {
        when(authorization.enabled()).thenReturn(true);
        when(tickets.consumeState(token, token)).thenReturn(Optional.of(new RiotLinkTicketPort.State(7, token, "verifier")));
        assertThatThrownBy(() -> service.callback(token, token, null)).isInstanceOf(ApiException.class);
        verify(authorization, org.mockito.Mockito.never()).exchange(anyString(), anyString());
    }
}
