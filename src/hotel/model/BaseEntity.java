package hotel.model;

// Abstract base class shared by all model entities — demonstrates inheritance and abstraction
// Room, User, Reservation, and Bill all extend this class to reuse the id field
// and fulfill the getSummary() contract
public abstract class BaseEntity {

    // Shared primary key field — inherited by every subclass instead of redeclaring it
    protected int id;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Abstract method — every entity must define how it describes itself
    // This is the hook for polymorphism: each subclass returns a different string
    public abstract String getSummary();

    // toString delegates to getSummary() — dynamic binding ensures the correct
    // subclass implementation runs even when the reference type is BaseEntity
    @Override
    public String toString() { return getSummary(); }
}
