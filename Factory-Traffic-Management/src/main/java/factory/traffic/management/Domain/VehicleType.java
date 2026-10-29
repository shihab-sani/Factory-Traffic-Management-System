package factory.traffic.management.Domain;

public enum VehicleType {
    EMERGENCY(1000),
    TRUCK(100),
    FORKLIFT(50),
    EMPLOYEE_VEHICLE(10);

    private final int priorityWeight;

    VehicleType(int w) {
        this.priorityWeight = w;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }
}
