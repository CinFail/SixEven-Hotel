# SixEven Hotel Reservation System
### Final Project Presentation

**Course:** Object-Oriented Programming  
**Language:** Java 24 (OpenJDK Temurin)  
**UI Framework:** JavaFX 21.0.2  
**Database:** SQLite via sqlite-jdbc  
**PDF Reports:** JasperReports 6.21.0

---

## System Overview

The SixEven Hotel Reservation System is a fully functional desktop application that manages the complete lifecycle of a hotel stay — from customer registration and room booking to check-out and PDF receipt generation.

The system supports two distinct user roles:

- **Staff** — manages rooms, processes reservations, checks out guests, and handles billing
- **Customer** — books rooms, views reservations, updates profile, and downloads receipts

All data is persisted in a local SQLite database. The application launches in fullscreen on every screen and requires no external server or internet connection.

---

## Functional Requirements

### Implemented Features

| Feature | Description | Status |
|---|---|---|
| User Authentication | SHA-256 hashed login with role-based routing | Complete |
| Customer Registration | Self-registration for customer accounts | Complete |
| Room Management | Add, edit, delete, and set room status | Complete |
| Room Availability Search | Date-range query with overlap detection | Complete |
| Reservation Booking | Staff books rooms for customers | Complete |
| Check-Out Processing | Discount, tax, payment method, bill generation | Complete |
| Reservation Cancellation | Cancel active reservations and free the room | Complete |
| Customer Profile Management | Update personal info, ID details, change password | Complete |
| Billing & PDF Receipts | Auto-generated PDF opened in system viewer | Complete |
| Staff Dashboard | 4-tab view: Rooms, Reservations, Customers, Billing | Complete |
| Customer Dashboard | 2-tab view: My Reservations, My Profile | Complete |

### Staff Workflow
1. Log in with staff credentials → Staff Dashboard opens fullscreen
2. Rooms tab → add, edit, or change room status
3. Reservations tab → click New Reservation → select customer, dates, room → confirm
4. Reservations tab → select active reservation → Check Out → enter discount, tax, payment method
5. Billing tab → select bill → Generate Bill → PDF opens automatically

### Customer Workflow
1. Register a new account from the Login screen
2. Log in → Customer Dashboard opens fullscreen
3. My Reservations tab → view all personal bookings
4. Select a checked-out reservation → View Bill → PDF opens automatically
5. My Profile tab → update personal information and government ID

---

## OOP Design and Implementation

The system is built on eight core OOP principles. Each is actively used throughout the codebase — not just declared but operationally integrated into the system's behavior.

---

### 1. Inheritance

**Definition:** A child class acquires the fields and methods of a parent class, avoiding code repetition and establishing an "is-a" relationship.

**Where it is used:**

All four model classes — `Room`, `User`, `Reservation`, and `Bill` — extend the abstract class `BaseEntity`.

```
BaseEntity (abstract)
├── Room.java
├── User.java
├── Reservation.java
└── Bill.java
```

**What is inherited:**

Every model class automatically receives the `id` field along with `getId()` and `setId()` from `BaseEntity`. None of the four subclasses declare their own `int id` — they all share the single declaration in the parent.

**Code Example — BaseEntity.java:**
```java
public abstract class BaseEntity {
    protected int id;

    public int getId()       { return id; }
    public void setId(int id){ this.id = id; }

    public abstract String getSummary();

    @Override
    public String toString() { return getSummary(); }
}
```

**Code Example — Room.java:**
```java
public class Room extends BaseEntity {
    private String roomNumber;
    private String roomType;
    private double price;
    // id is inherited — not redeclared here
}
```

**Practical effect:** When `RoomDAO` retrieves a room from the database and calls `room.setId(rs.getInt("id"))`, it is calling the method defined once in `BaseEntity` — not a copy written inside `Room`. The same applies to `User`, `Reservation`, and `Bill`.

---

### 2. Abstract Class

**Definition:** A class declared `abstract` cannot be instantiated. It may contain abstract methods that subclasses are forced to implement, acting as a contract enforced at compile time.

**Where it is used:**

`BaseEntity` is declared `abstract`. It cannot be instantiated with `new BaseEntity()`. It declares `getSummary()` as abstract, which means every subclass (`Room`, `User`, `Reservation`, `Bill`) must provide its own implementation or the code will not compile.

**Code Example — BaseEntity.java:**
```java
public abstract class BaseEntity {
    public abstract String getSummary();
}
```

**Code Example — Concrete implementations:**
```java
// Room.java
@Override
public String getSummary() {
    return "Room " + roomNumber + " - " + roomType
           + " (₱" + String.format("%.2f", price) + "/night)";
}

// User.java
@Override
public String getSummary() {
    return fullName + " (@" + username + ")";
}

// Bill.java
@Override
public String getSummary() {
    return "Bill #" + id + " | " + customerName
           + " | ₱" + String.format("%.2f", getGrandTotal())
           + " | " + (paid ? "PAID" : "UNPAID");
}
```

**Practical effect:** The abstract method forces a compile-time guarantee. If a developer adds a fifth model class that extends `BaseEntity` without writing `getSummary()`, the compiler refuses to build the project. The contract cannot be bypassed.

---

### 3. Encapsulation

**Definition:** The internal state of an object is hidden behind private fields and controlled access is provided through public getters and setters. External code cannot directly read or modify private data.

**Where it is used:**

All model fields are `private`. All utility class internals are `private`. Controllers and DAOs can only access data through the defined public interface.

**Code Example — Bill.java (private fields, public accessors):**
```java
public class Bill extends BaseEntity {
    private int    reservationId;
    private double totalAmount;
    private double discount;
    private double tax;
    private boolean paid;
    private String paymentMethod;

    public double getDiscount()              { return discount; }
    public void   setDiscount(double d)      { this.discount = d; }
    public boolean isPaid()                  { return paid; }
    public void    setPaid(boolean p)        { this.paid = p; }
}
```

**Code Example — DatabaseManager.java (private connection, controlled access):**
```java
public class DatabaseManager {
    private static Connection connection;  // hidden from all other classes

    private DatabaseManager() {}           // cannot be instantiated

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }
}
```

**Code Example — Session.java (private state, public methods only):**
```java
public class Session {
    private static User currentUser;  // no class can access this directly

    private Session() {}

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser()          { return currentUser; }
    public static void logout()                  { currentUser = null; }
}
```

**Practical effect:** No controller can directly assign to `currentUser` or `connection`. They must go through the provided methods. This prevents invalid states — for example, a controller cannot set `connection` to null mid-operation, and it cannot bypass the `synchronized` guard that prevents race conditions.

---

### 4. Abstraction

**Definition:** Complexity is hidden behind a simple interface. The caller knows what an operation does but not how it is done internally.

**Where it is used in two forms:**

**Form 1 — The `Dao<T>` Generic Interface**

The `Dao<T>` interface defines a standard CRUD contract that all four DAO classes implement. Controllers call `getAll()`, `getById()`, `save()`, and `delete()` without knowing anything about SQL, `ResultSet`, or `PreparedStatement`.

```java
public interface Dao<T extends BaseEntity> {
    List<T> getAll();
    T       getById(int id);
    boolean save(T entity);
    boolean delete(int id);
}
```

Each DAO implements this interface with its own SQL logic hidden inside:
```java
public class RoomDAO implements Dao<Room> { ... }
public class UserDAO implements Dao<User> { ... }
public class ReservationDAO implements Dao<Reservation> { ... }
public class BillDAO implements Dao<Bill> { ... }
```

**Form 2 — BillReport.java**

The entire 6-step PDF generation pipeline (write JRXML, compile template, fill parameters, export, open file) is hidden behind a single method call:

```java
// What the controller sees:
BillReport.print(bill);

// What actually happens inside (hidden from callers):
// Step 1: Create receipts/ folder if it doesn't exist
// Step 2: Generate XML template as a string and write to file
// Step 3: Compile the JRXML into a JasperReport object
// Step 4: Map all bill fields into a HashMap of named parameters
// Step 5: Fill the template with JREmptyDataSource
// Step 6: Export to PDF and open in the OS default PDF viewer
```

**Practical effect:** `StaffDashboardController` and `CustomerDashboardController` both call `BillReport.print(bill)` as a single line. Neither knows about JasperReports, file I/O, or PDF export. Abstraction keeps the controller focused on UI logic only.

---

### 5. Polymorphism

**Definition:** One method name produces different behavior depending on the actual type of the object it is called on. The same call works correctly for every subclass without any type-checking code.

**Where it is used:**

`getSummary()` is declared once in `BaseEntity` as abstract and overridden differently in all four subclasses. The same method name returns a completely different formatted string for each type.

**Code Example:**
```java
BaseEntity room = new Room("101", "Standard Single", 1200.00, 1, "Garden view");
BaseEntity user = new User("juan", "pass", User.Role.CUSTOMER, "Juan Dela Cruz");
BaseEntity bill = new Bill(); // with fields set

System.out.println(room); // → "Room 101 - Standard Single (₱1200.00/night)"
System.out.println(user); // → "Juan Dela Cruz (@juan)"
System.out.println(bill); // → "Bill #1 | Juan Dela Cruz | ₱2400.00 | PAID"
```

All three objects are typed as `BaseEntity`. All three call the same `toString()` → `getSummary()` chain. Each produces a completely different result based on which subclass is actually stored in memory.

**Practical effect in the UI:** JavaFX's `ListView` and `ComboBox` automatically call `toString()` on every item they display. Because of polymorphism, a `ListView<Room>` displays each room as `"Room 101 - Standard Single (₱1200.00/night)"` and a `ComboBox<User>` displays each customer as `"Juan Dela Cruz (@juan)"` — all without any display formatting code in the controllers.

---

### 6. Dynamic Binding

**Definition:** When a method is called through a parent-class reference, Java determines at runtime which subclass implementation to run — not at compile time.

**Where it is used:**

`BaseEntity.toString()` calls `getSummary()`. At compile time, Java only knows the reference type is `BaseEntity`. At runtime, the JVM looks up the actual object type and calls that type's `getSummary()`.

**Code Example — BaseEntity.java:**
```java
@Override
public String toString() { return getSummary(); }
// At compile time: which getSummary() runs? Unknown — resolved at runtime.
// At runtime: the JVM checks the actual object type and calls its override.
```

**Step-by-step trace with a Room object:**
```
1. roomList.getItems()     → contains Room objects stored as Room references
2. JavaFX calls toString() → defined in BaseEntity
3. toString() calls getSummary() → BaseEntity has no body for this, it is abstract
4. JVM checks actual type at runtime → it is Room
5. JVM calls Room.getSummary() → "Room 101 - Standard Single (₱1200.00/night)"
```

**Practical effect:** If the reference were `BaseEntity entity = new Room(...)` and Java used static binding, it would try to call `BaseEntity.getSummary()` — which has no body (it is abstract) and would fail. Dynamic binding makes sure the correct subclass method is always selected automatically, enabling the entire polymorphism system to work.

---

### 7. Message Passing

**Definition:** Objects communicate by calling methods on other objects, passing data and triggering behavior across class boundaries.

**Where it is used:**

`MainApp.showScreen()` is the central message-passing mechanism. Controllers do not navigate directly — they send a message to `MainApp` by calling its static method, which then loads the target screen.

**Code Example — LoginController.java sending a message to MainApp:**
```java
// After successful login, LoginController tells MainApp to switch screens:
Session.setCurrentUser(user);
if (user.getRole() == User.Role.STAFF) {
    MainApp.showScreen("StaffDashboard");  // message passed to MainApp
} else {
    MainApp.showScreen("CustomerDashboard");
}
```

**Code Example — MainApp.java receiving and processing the message:**
```java
public static void showScreen(String screenName) {
    FXMLLoader loader = new FXMLLoader(
        MainApp.class.getResource("/hotel/ui/" + screenName + ".fxml")
    );
    Parent root = loader.load();
    Scene scene = new Scene(root);
    primaryStage.setMaximized(false);
    primaryStage.setScene(scene);
    primaryStage.setMaximized(true);
}
```

**Full message chain for a reservation confirmation:**
```
NewReservationController
  → reservationDAO.create(res)           [message to ReservationDAO]
      → DatabaseManager.getConnection()  [message to DatabaseManager]
      → conn.commit()                    [message to Connection]
  → MainApp.showScreen("StaffDashboard") [message to MainApp]
```

**Practical effect:** No controller holds a direct reference to another controller or to the stage. All navigation goes through `MainApp.showScreen()`. All database access goes through DAO methods. All connection management goes through `DatabaseManager`. This is the message-passing pattern — objects stay loosely coupled by communicating through well-defined method calls.

---

### 8. User-Defined Types (Enumerations)

**Definition:** A programmer-defined type that restricts a variable to a fixed set of named constants. More type-safe than using plain strings or integers for state values.

**Where it is used:**

Three enums are defined across the model layer. Each restricts a field to only its valid values.

**`Room.Status` — three valid room states:**
```java
public enum Status { AVAILABLE, OCCUPIED, MAINTENANCE }
```
Used in: `Room.java`, `RoomDAO.java`, `StaffDashboardController.java`, and enforced in SQLite with `CHECK(status IN ('AVAILABLE','OCCUPIED','MAINTENANCE'))`.

**`User.Role` — two valid user types:**
```java
public enum Role { STAFF, CUSTOMER }
```
Used in: `User.java`, `LoginController.java` (role-based routing), `UserDAO.java` (filter customers), `Session.java` (`isStaff()` check).

**`Reservation.Status` — three valid booking states:**
```java
public enum Status { ACTIVE, CHECKED_OUT, CANCELLED }
```
Used in: `Reservation.java`, `ReservationDAO.java`, `StaffDashboardController.java` (guards on check-out and cancel), and enforced in SQLite with a `CHECK` constraint.

**Code Example — role-based routing using `User.Role`:**
```java
if (user.getRole() == User.Role.STAFF) {
    MainApp.showScreen("StaffDashboard");
} else {
    MainApp.showScreen("CustomerDashboard");
}
```

**Code Example — status guard using `Reservation.Status`:**
```java
if (sel.getStatus() != Reservation.Status.ACTIVE) {
    alert("Invalid", "Only ACTIVE reservations can be checked out.");
    return;
}
```

**Practical effect:** Without enums, a developer could accidentally write `room.setStatus("available")` (lowercase) or `room.setStatus("AVAILBLE")` (typo) — both would compile but cause runtime failures. With enums, only `Room.Status.AVAILABLE` compiles. Invalid values are caught at compile time, not during a live demonstration.

---

## User Interface Design

The system uses JavaFX 21.0.2 with a consistent dark navy color theme across all screens. Every screen launches and stays fullscreen on every navigation, enforced by the `setMaximized(false) → setScene() → setMaximized(true)` sequence in `MainApp.showScreen()`.

### Screens

| Screen | Access | Purpose |
|---|---|---|
| Login | Both roles | Username/password authentication |
| Register | Public | Customer self-registration |
| Staff Dashboard | Staff only | 4-tab management interface |
| Customer Dashboard | Customer only | Personal reservations and profile |
| New Reservation | Staff only | Date search, room selection, booking |

### Navigation Flow

```
Login Screen
├── [Login as Staff]     → Staff Dashboard (Rooms / Reservations / Customers / Billing tabs)
│                              └── [New Reservation] → New Reservation Screen → back to Staff Dashboard
├── [Login as Customer]  → Customer Dashboard (My Reservations / My Profile tabs)
└── [Register]           → Register Screen → back to Login
```

### UI Design Decisions

- **Dark theme** — consistent `#1a1a2e` / `#16213e` background across all screens
- **Always fullscreen** — no screen ever opens at a small default size
- **White placeholder text** — custom labels override JavaFX's default dark placeholder text on all tables
- **Live booking summary** — the New Reservation screen updates the price summary in real time as the user changes dates or selects a room
- **Inline dialog boxes** — room add/edit, check-out, and mark-paid operations use pop-up dialogs so the user stays on the dashboard

---

## Code Quality

### Layered Architecture

The system is organized into five distinct layers. Each layer has one responsibility and communicates only with adjacent layers.

```
┌─────────────────────────────────┐
│        UI Layer (Controllers)   │  Handles user interaction, calls DAO methods
├─────────────────────────────────┤
│        Reports Layer            │  Generates PDF receipts via JasperReports
├─────────────────────────────────┤
│        DAO Layer                │  All SQL queries, transactions, object mapping
├─────────────────────────────────┤
│        Model Layer              │  Data classes with no SQL or UI dependencies
├─────────────────────────────────┤
│        Utility Layer            │  DatabaseManager, PasswordUtil, Session
└─────────────────────────────────┘
```

### Java Coding Standards Applied

- **Naming conventions** — classes in `PascalCase`, methods and variables in `camelCase`, constants in `UPPER_SNAKE_CASE`, packages in `lowercase`
- **Private fields with accessors** — no public fields on any model class
- **Try-with-resources** — all `PreparedStatement` and `Connection` objects use `try(...)` blocks to prevent resource leaks
- **Meaningful names** — `getAvailableRooms()`, `handleCheckOut()`, `seedDefaultData()` — names describe intent, not implementation
- **Single responsibility** — each class does one thing: `PasswordUtil` only hashes, `Session` only tracks the logged-in user, `BillReport` only generates PDFs

### Security

- Passwords are hashed with **SHA-256** before storage — plain text passwords never appear in the database
- All SQL uses **PreparedStatements** with `?` placeholders — no string concatenation in queries, preventing SQL injection
- SQLite enforces **CHECK constraints** on role and status columns — invalid values are rejected at the database level

---

## Database Design and Management

### Schema

```
users
  id, username (UNIQUE), password, role, full_name,
  email, phone, address, id_type, id_number, created_at

rooms
  id, room_number (UNIQUE), room_type, price,
  capacity, description, status

reservations
  id, customer_id (FK → users), room_id (FK → rooms),
  check_in, check_out, guests, notes, status, created_at

bills
  id, reservation_id (FK → reservations, UNIQUE),
  total_amount, discount, tax, paid, payment_method, issued_at
```

### CRUD Operations

| Operation | Where |
|---|---|
| **Create** user | RegisterController → UserDAO.register() |
| **Create** room | StaffDashboardController → RoomDAO.addRoom() |
| **Create** reservation | NewReservationController → ReservationDAO.create() |
| **Create** bill | ReservationDAO.checkOut() (auto on check-out) |
| **Read** available rooms | NewReservationController → RoomDAO.getAvailableRooms() |
| **Read** reservations | StaffDashboardController → ReservationDAO.getAll() |
| **Read** customer reservations | CustomerDashboardController → ReservationDAO.getByCustomer() |
| **Read** bill by reservation | CustomerDashboardController → BillDAO.getByReservationId() |
| **Update** room | StaffDashboardController → RoomDAO.updateRoom() |
| **Update** customer profile | CustomerDashboardController → UserDAO.update() |
| **Update** bill (mark paid) | StaffDashboardController → BillDAO.markPaid() |
| **Delete** room | StaffDashboardController → RoomDAO.deleteRoom() |

### Integrity Features

- **Foreign keys** enforced with `PRAGMA foreign_keys=ON`
- **Atomic transactions** used in reservation creation, check-out, and cancellation — all steps succeed together or none are saved
- **WAL mode** (`PRAGMA journal_mode=WAL`) allows concurrent reads during writes
- **Four indexes** on the reservations table for fast availability queries and customer history lookups
- **INSERT OR IGNORE** on bills prevents duplicate billing if check-out is triggered twice

---

## Documentation

The following documentation is included with the system:

| File | Purpose |
|---|---|
| `SystemDocumentation.md` | Full technical reference — every class, method, and design decision explained |
| `README.md` | GitHub import guide — step-by-step Eclipse setup with VM arguments and credentials |
| `HotelReservationSystem.launch` | Pre-configured Eclipse run file — one-click launch with all JavaFX module arguments set |
| Generated PDF receipts | Auto-created in `receipts/` folder on each bill generation |

All documentation was written alongside development. The system documentation covers the model layer, DAO layer, utility layer, controller layer, and reports layer — including OOP concept annotations in source code comments.

---

## Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java (OpenJDK Temurin) | 24.0.2 |
| UI Framework | JavaFX (Windows native) | 21.0.2 |
| Database | SQLite via sqlite-jdbc | 3.53.1 |
| PDF Generation | JasperReports | 6.21.0 |
| Password Security | SHA-256 (java.security) | Built-in |
| IDE | Eclipse | Any recent |
| Version Control | Git / GitHub | — |
