package com.gamemoyeo.reservation.domain;

public record PositionVacancy(int participantCount, Integer capacity, Integer remainingCapacity, String status) {
    public static PositionVacancy calculate(int participants, Integer capacity, int totalRemaining, boolean open) {
        Integer remaining = capacity == null ? null : Math.min(Math.max(0, capacity - participants), totalRemaining);
        String status;
        if (!open) {
            status = "CLOSED";
        } else if (remaining != null) {
            status = remaining > 0 ? "OPEN" : "FILLED";
        } else {
            status = participants == 0 ? "EMPTY" : "OCCUPIED";
        }
        return new PositionVacancy(participants, capacity, remaining, status);
    }
}
