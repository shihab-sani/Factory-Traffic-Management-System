package factory.traffic.management.Domain;

public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public boolean isNorthSouth() {
        return this == NORTH || this == SOUTH;
    }

    public boolean conflictsWith(Direction other) {
        return this.isNorthSouth() != other.isNorthSouth();
    }
}
