package com.gamemoyeo.member.application.port.in;

public interface RiotLinkUseCase {
    Status status(long memberId);
    Start start(long memberId);
    String callback(String state, String browser, String code);
    void complete(long memberId, String ticket);
    void unlink(long memberId);
    record Status(boolean enabled, boolean linked, String riotId) { }
    record Start(String authorizationUrl, String browser) { }
}
