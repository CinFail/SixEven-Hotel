package hotel.model;

// Inheritance: extends BaseEntity to reuse the id field and getSummary contract
// Encapsulation: private fields keep billing data hidden behind getters and setters
public class Bill extends BaseEntity {

    private int    reservationId;
    private double totalAmount;
    private double discount;
    private double tax;
    private boolean paid;
    private String paymentMethod;
    private String issuedAt;

    // Populated by JOIN queries across bills, reservations, users, and rooms tables
    private String customerName;
    private String roomNumber;
    private String roomType;
    private String checkIn;
    private String checkOut;
    private double roomPrice;
    private long   nights;

    public Bill() {}

    // Polymorphism: Bill's getSummary shows billing context, distinct from other entities
    // Dynamic binding: BaseEntity.toString() triggers this method at runtime
    @Override
    public String getSummary() {
        return "Bill #" + id + " | " + customerName
               + " | ₱" + String.format("%.2f", getGrandTotal())
               + " | " + (paid ? "PAID" : "UNPAID");
    }

    public int     getReservationId()              { return reservationId; }
    public void    setReservationId(int r)         { this.reservationId = r; }

    public double  getTotalAmount()                { return totalAmount; }
    public void    setTotalAmount(double t)        { this.totalAmount = t; }

    public double  getDiscount()                   { return discount; }
    public void    setDiscount(double d)           { this.discount = d; }

    public double  getTax()                        { return tax; }
    public void    setTax(double t)                { this.tax = t; }

    public boolean isPaid()                        { return paid; }
    public void    setPaid(boolean p)              { this.paid = p; }

    public String  getPaymentMethod()              { return paymentMethod; }
    public void    setPaymentMethod(String m)      { this.paymentMethod = m; }

    public String  getIssuedAt()                   { return issuedAt; }
    public void    setIssuedAt(String i)           { this.issuedAt = i; }

    public String  getCustomerName()               { return customerName; }
    public void    setCustomerName(String n)       { this.customerName = n; }

    public String  getRoomNumber()                 { return roomNumber; }
    public void    setRoomNumber(String r)         { this.roomNumber = r; }

    public String  getRoomType()                   { return roomType; }
    public void    setRoomType(String t)           { this.roomType = t; }

    public String  getCheckIn()                    { return checkIn; }
    public void    setCheckIn(String c)            { this.checkIn = c; }

    public String  getCheckOut()                   { return checkOut; }
    public void    setCheckOut(String c)           { this.checkOut = c; }

    public double  getRoomPrice()                  { return roomPrice; }
    public void    setRoomPrice(double p)          { this.roomPrice = p; }

    public long    getNights()                     { return nights; }
    public void    setNights(long n)               { this.nights = n; }

    // Subtotal before discount and tax adjustments
    public double getSubtotal() { return roomPrice * nights; }

    // Grand total = subtotal minus discount plus tax
    public double getGrandTotal() { return getSubtotal() - discount + tax; }
}
