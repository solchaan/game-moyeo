package com.gamemoyeo.member.domain;

public record RiotAccount(String puuid, String gameName, String tagLine) {
    public RiotAccount {
        if (puuid == null || !puuid.matches("[A-Za-z0-9_-]{1,191}")
            || gameName == null || gameName.isBlank() || gameName.length() > 100
            || tagLine == null || tagLine.isBlank() || tagLine.length() > 32) {
            throw new IllegalArgumentException("Invalid Riot account response");
        }
    }
    public String riotId() {
        return gameName + "#" + tagLine;
    }
}
