package hotel.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Inheritance: extends BaseEntity to reuse the id field and getSummary contract
// Encapsulation: private fields including join-populated fields not stored in this table
// User-defined type: Status enum restricts reservation state to valid transitions
public class Reservation extends BaseEntity {

    public enum Status { ACTIVE, CHECKED_OUT, CANCELLED }

    private int       customerId;
    private int       roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int       guests;
    private String    notes;
    private Status    status;
    private String    createdAt;

    // Populated by JOIN queries, not stored directly in the reservations table
    private String    customerName;
    private String    roomNumber;
    private String    roomType;
    private double    roomPrice;

    public Reservation() {}

    // Polymorphism: Reservation's getSummary is distinct from Room's or User's
    // Dynamic binding: calling toString() on a BaseEntity reference executes this at runtime
    @Override
    public String getSummary() {
        return "Reservation #" + id + " | " + customerName
               + " | Room " + roomNumber + " (" + checkIn + " to " + checkOut + ")";
    }

    public int       getCustomerId()               { return customerId; }
    public void      setCustomerId(int c)          { this.customerId = c; }

    public int       getRoomId()                   { return roomId; }
    public void      setRoomId(int r)              { this.roomId = r; }

    public LocalDate getCheckIn()                  { return checkIn; }
    public void      setCheckIn(LocalDate d)       { this.checkIn = d; }

    public LocalDate getCheckOut()                 { return checkOut; }
    public void      setCheckOut(LocalDate d)      { this.checkOut = d; }

    public int       getGuests()                   { return guests; }
    public void      setGuests(int g)              { this.guests = g; }

    public String    getNotes()                    { return notes; }
    public void      setNotes(String n)            { this.notes = n; }

    public Status    getStatus()                   { return status; }
    public void      setStatus(Status s)           { this.status = s; }

    public String    getCreatedAt()                { return createdAt; }
    public void      setCreatedAt(String c)        { this.createdAt = c; }

    public String    getCustomerName()             { return customerName; }
    public void      setCustomerName(String n)     { this.customerName = n; }

    public String    getRoomNumber()               { return roomNumber; }
    public void      setRoomNumber(String r)       { this.roomNumber = r; }

    public String    getRoomType()                 { return roomType; }
    public void      setRoomType(String t)         { this.roomType = t; }

    public double    getRoomPrice()                { return roomPrice; }
    public void      setRoomPrice(double p)        { this.roomPrice = p; }

    // Derived from check-in/out dates, not stored in DB
    public long getNights() {
        if (checkIn == null || checkOut == null) return 0;
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public double getTotalAmount() {
        return roomPrice * getNights();
    }
}
