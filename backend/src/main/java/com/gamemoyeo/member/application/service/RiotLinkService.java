package com.gamemoyeo.member.application.service;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.port.in.RiotLinkUseCase;
import com.gamemoyeo.member.application.port.out.RiotAccountPort;
import com.gamemoyeo.member.application.port.out.RiotAuthorizationPort;
import com.gamemoyeo.member.application.port.out.RiotLinkTicketPort;
import com.gamemoyeo.member.application.port.out.RiotLinkTicketPort.State;
import com.gamemoyeo.member.application.port.out.RiotLinkTicketPort.Completion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RiotLinkService implements RiotLinkUseCase {
    private final RiotAccountPort accounts;
    private final RiotAuthorizationPort authorization;
    private final RiotLinkTicketPort tickets;
    private final SecureRandom random = new SecureRandom();

    public RiotLinkService(RiotAccountPort accounts, RiotAuthorizationPort authorization, RiotLinkTicketPort tickets) {
        this.accounts = accounts;
        this.authorization = authorization;
        this.tickets = tickets;
    }
    @Override
    public Status status(long memberId) {
        return accounts.find(memberId).map(a -> new Status(authorization.enabled(), true, a.riotId()))
            .orElse(new Status(authorization.enabled(), false, null));
    }
    @Override
    public Start start(long memberId) {
        requireEnabled();
        String state = randomToken();
        String browser = randomToken();
        String verifier = randomToken();
        tickets.saveState(state, new State(memberId, browser, verifier), Duration.ofMinutes(5));
        return new Start(authorization.authorizationUrl(state, challenge(verifier)), browser);
    }
    @Override
    public String callback(String state, String browser, String code) {
        requireEnabled();
        if (!validToken(state) || !validToken(browser)) {
            throw invalidFlow();
        }
        State pending = tickets.consumeState(state, browser).orElseThrow(RiotLinkService::invalidFlow);
        if (code == null || code.isBlank() || code.length() > 4096) {
            throw invalidFlow();
        }
        // External HTTP completes before the persistence adapter starts a database transaction.
        var account = authorization.exchange(code, pending.verifier());
        String ticket = randomToken();
        tickets.saveCompletion(ticket, new Completion(pending.memberId(), account), Duration.ofSeconds(60));
        return ticket;
    }
    @Override
    @org.springframework.transaction.annotation.Transactional
    public void complete(long memberId, String ticket) {
        requireEnabled();
        if (!validToken(ticket)) {
            throw invalidFlow();
        }
        Completion pending = tickets.consumeCompletion(ticket, memberId).orElseThrow(RiotLinkService::invalidFlow);
        accounts.link(memberId, pending.account());
    }
    @Override
    @org.springframework.transaction.annotation.Transactional
    public void unlink(long memberId) {
        accounts.unlink(memberId);
    }
    private void requireEnabled() {
        if (!authorization.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RIOT_LINK_UNAVAILABLE", "Riot 계정 연동을 준비 중입니다.");
        }
    }
    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private boolean validToken(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{43}");
    }
    static String challenge(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
    private static ApiException invalidFlow() {
        return new ApiException(HttpStatus.BAD_REQUEST, "RIOT_LINK_EXPIRED", "인증이 만료되었거나 유효하지 않습니다. 다시 연결해 주세요.");
    }
}
