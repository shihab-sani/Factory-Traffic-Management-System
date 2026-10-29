package factory.traffic.management.Domain;

import java.util.Map;

import static factory.traffic.management.Domain.Direction.*;
import static factory.traffic.management.Domain.SignalState.*;

public enum Phase {
    NORTH_SOUTH_GREEN,
    NORTH_SOUTH_YELLOW,
    EAST_WEST_GREEN,
    EAST_WEST_YELLOW,
    ALL_RED;

    public Map<Direction, SignalState> signals() {
        return switch (this) {
            case NORTH_SOUTH_GREEN  -> Map.of(NORTH, GREEN, SOUTH, GREEN, EAST, RED, WEST, RED);
            case NORTH_SOUTH_YELLOW -> Map.of(NORTH, YELLOW, SOUTH, YELLOW, EAST, RED, WEST, RED);
            case EAST_WEST_GREEN    -> Map.of(NORTH, RED, SOUTH, RED, EAST, GREEN, WEST, GREEN);
            case EAST_WEST_YELLOW   -> Map.of(NORTH, RED, SOUTH, RED, EAST, YELLOW, WEST, YELLOW);
            case ALL_RED            -> Map.of(NORTH, RED, SOUTH, RED, EAST, RED, WEST, RED);
        };
    }

    public boolean isGreen() { return this == NORTH_SOUTH_GREEN || this == EAST_WEST_GREEN; }
    public boolean isYellow() { return this == NORTH_SOUTH_YELLOW || this == EAST_WEST_YELLOW; }

    public boolean isGreenFor(Direction d) {
        if (this == NORTH_SOUTH_GREEN) return d.isNorthSouth();
        if (this == EAST_WEST_GREEN) return !d.isNorthSouth();
        return false;
    }

    public Phase nextAfterGreen() {
        return switch (this) {
            case NORTH_SOUTH_GREEN -> NORTH_SOUTH_YELLOW;
            case EAST_WEST_GREEN   -> EAST_WEST_YELLOW;
            default -> throw new IllegalStateException("Not a green phase: " + this);
        };
    }

    public static Phase greenFor(Direction d) {
        return d.isNorthSouth() ? NORTH_SOUTH_GREEN : EAST_WEST_GREEN;
    }
}
