package com.gamemoyeo.member.domain;

public record PositionPreferences(Position primaryPosition, Position secondaryPosition) {
    public enum Position { TOP, JUNGLE, MID, ADC, SUPPORT, ANY }
    public PositionPreferences {
        if (secondaryPosition != null && primaryPosition == null) {
            throw new IllegalArgumentException("주 포지션을 먼저 선택해 주세요.");
        }
        if (secondaryPosition != null && secondaryPosition == primaryPosition) {
            throw new IllegalArgumentException("주 포지션과 부 포지션은 다르게 선택해 주세요.");
        }
    }
    public static PositionPreferences empty() { return new PositionPreferences(null, null); }
}
