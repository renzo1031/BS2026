package com.campus.service.domain;

import java.util.Set;

public enum RequestStatus {
    SUBMITTED,
    ACCEPTED,
    PROCESSING,
    FINISHED,
    EVALUATED,
    REJECTED,
    CANCELLED;

    public boolean canTransitionTo(RequestStatus target) {
        return switch (this) {
            case SUBMITTED -> Set.of(ACCEPTED, CANCELLED).contains(target);
            case ACCEPTED -> Set.of(PROCESSING, REJECTED, CANCELLED).contains(target);
            case PROCESSING -> target == FINISHED;
            case FINISHED -> target == EVALUATED;
            case EVALUATED, REJECTED, CANCELLED -> false;
        };
    }
}
