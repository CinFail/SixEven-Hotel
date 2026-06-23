package hotel.model;

// Inheritance: extends BaseEntity to reuse the id field and getSummary contract
// Encapsulation: all fields are private, accessed only through getters and setters
public class Room extends BaseEntity {

    // User-defined type: enum restricts status to only valid values
    public enum Status { AVAILABLE, OCCUPIED, MAINTENANCE }

    private String roomNumber;
    private String roomType;
    private double price;
    private int    capacity;
    private String description;
    private Status status;

    public Room() {}

    public Room(String roomNumber, String roomType, double price, int capacity, String description) {
        this.roomNumber  = roomNumber;
        this.roomType    = roomType;
        this.price       = price;
        this.capacity    = capacity;
        this.description = description;
        this.status      = Status.AVAILABLE;
    }

    // Polymorphism: overrides the abstract getSummary() from BaseEntity
    // Dynamic binding: when BaseEntity.toString() calls getSummary(), this method runs
    @Override
    public String getSummary() {
        return "Room " + roomNumber + " - " + roomType + " (₱" + String.format("%.2f", price) + "/night)";
    }

    public String getRoomNumber()              { return roomNumber; }
    public void   setRoomNumber(String r)      { this.roomNumber = r; }

    public String getRoomType()                { return roomType; }
    public void   setRoomType(String t)        { this.roomType = t; }

    public double getPrice()                   { return price; }
    public void   setPrice(double p)           { this.price = p; }

    public int    getCapacity()                { return capacity; }
    public void   setCapacity(int c)           { this.capacity = c; }

    public String getDescription()             { return description; }
    public void   setDescription(String d)     { this.description = d; }

    public Status getStatus()                  { return status; }
    public void   setStatus(Status s)          { this.status = s; }
}
