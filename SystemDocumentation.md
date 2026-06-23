# Hotel Reservation System — Full System Documentation

**System Name:** Hotel Reservation System (Sixeven Hotel)  
**Language:** Java 24 (OpenJDK Temurin 24.0.2)  
**UI Framework:** JavaFX 21.0.2  
**Database:** SQLite (hotel.db via sqlite-jdbc-3.53.1.0)  
**PDF Reports:** JasperReports 6.21.0  
**Total Source Files:** 20 Java files across 5 packages

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Package Structure](#2-package-structure)
3. [Model Layer](#3-model-layer)
   - [BaseEntity.java](#31-baseentityjava)
   - [Room.java](#32-roomjava)
   - [User.java](#33-userjava)
   - [Reservation.java](#34-reservationjava)
   - [Bill.java](#35-billjava)
4. [DAO Layer](#4-dao-layer)
   - [Dao.java (Interface)](#41-daojava-interface)
   - [RoomDAO.java](#42-roomdaojava)
   - [UserDAO.java](#43-userdaojava)
   - [ReservationDAO.java](#44-reservationdaojava)
   - [BillDAO.java](#45-billdaojava)
5. [Utility Layer](#5-utility-layer)
   - [DatabaseManager.java](#51-databasemanagerjava)
   - [PasswordUtil.java](#52-passwordutiljava)
   - [Session.java](#53-sessionjava)
6. [Application Entry Point](#6-application-entry-point)
   - [MainApp.java](#61-mainapppjava)
7. [UI Controller Layer](#7-ui-controller-layer)
   - [LoginController.java](#71-logincontrollerjava)
   - [RegisterController.java](#72-registercontrollerjava)
   - [StaffDashboardController.java](#73-staffdashboardcontrollerjava)
   - [CustomerDashboardController.java](#74-customerdashboardcontrollerjava)
   - [NewReservationController.java](#75-newreservationcontrollerjava)
8. [Reports Layer](#8-reports-layer)
   - [BillReport.java](#81-billreportjava)
9. [OOP Concepts Summary](#9-oop-concepts-summary)
10. [How the Layers Connect](#10-how-the-layers-connect)

---

## 1. System Overview

The Hotel Reservation System is a desktop application built using Java and JavaFX. It manages the full lifecycle of a hotel stay: from creating a customer account, making a room reservation, performing check-out with payment, and generating a PDF bill receipt.

The system supports two types of users:
- **Staff** — can manage rooms, view all reservations, check out guests, cancel reservations, edit customer profiles, and generate PDF receipts.
- **Customer** — can view their own reservations, update their profile, and view or download their bill.

The system is architected in layers: the **Model** layer represents data, the **DAO** layer handles all database operations, the **Utility** layer provides shared services (database connection, password hashing, session tracking), the **UI Controller** layer drives the JavaFX screens, and the **Reports** layer generates PDF documents.

---

## 2. Package Structure

```
hotel/
├── model/
│   ├── BaseEntity.java        ← Abstract parent class for all models
│   ├── Room.java              ← Room data and status
│   ├── User.java              ← User accounts (staff and customer)
│   ├── Reservation.java       ← Booking records
│   └── Bill.java              ← Payment and billing records
│
├── dao/
│   ├── Dao.java               ← Generic CRUD interface
│   ├── RoomDAO.java           ← Room database operations
│   ├── UserDAO.java           ← User/login database operations
│   ├── ReservationDAO.java    ← Reservation database operations
│   └── BillDAO.java           ← Billing database operations
│
├── util/
│   ├── DatabaseManager.java   ← SQLite connection, schema creation, seed data
│   ├── PasswordUtil.java      ← SHA-256 hashing
│   └── Session.java           ← Logged-in user state
│
├── ui/
│   ├── LoginController.java             ← Login screen logic
│   ├── RegisterController.java          ← Account registration logic
│   ├── StaffDashboardController.java    ← Staff 4-tab dashboard
│   ├── CustomerDashboardController.java ← Customer 2-tab dashboard
│   └── NewReservationController.java    ← Room search and booking
│
├── reports/
│   └── BillReport.java        ← JasperReports PDF generator
│
└── MainApp.java               ← JavaFX Application entry point
```

---

## 3. Model Layer

The model layer contains Java classes that represent the real-world data entities of the hotel system. Each class holds fields for a single entity and provides getter and setter methods to access those fields. All model classes extend `BaseEntity`, which provides the shared `id` field and enforces the `getSummary()` method through abstraction.

---

### 3.1 BaseEntity.java

**Package:** `hotel.model`  
**File:** `src/hotel/model/BaseEntity.java`

#### What it does

`BaseEntity` is the **abstract parent class** for all four model entities: Room, User, Reservation, and Bill. Its job is to provide a single, shared definition of the primary key field (`id`) and to declare an abstract contract that every model must implement — the `getSummary()` method.

Without `BaseEntity`, each model class would have to independently declare its own `int id` field and its own `getId()`/`setId()` methods. That is code repetition, which violates the DRY (Don't Repeat Yourself) principle. With `BaseEntity`, the `id` field is written once and inherited by all four models.

#### How it works

The class is declared `abstract`, meaning no one can create a `BaseEntity` object directly. It can only be used as a base for other classes. When Room, User, Reservation, or Bill is instantiated, they each silently carry the `id` field and getter/setter inherited from this class.

The most important mechanism here is the `toString()` method. In Java, `toString()` is called automatically in many situations — when you print an object, when a JavaFX `ComboBox` or `ListView` displays an item, or when you concatenate an object with a string. By overriding `toString()` in `BaseEntity` to call `getSummary()`, the system ensures that every model object will display a meaningful, human-readable string automatically.

The critical mechanism here is **dynamic binding**: at runtime, Java does not call `BaseEntity.getSummary()` — it calls the `getSummary()` of whichever subclass is actually stored in memory. So if a `Room` object is referenced by a `BaseEntity` variable, calling `toString()` still runs `Room.getSummary()`, not a generic version.

#### Crucial Parts

**The `protected` modifier on `id`:**  
The `id` field is `protected` instead of `private`. This means subclasses (Room, User, Reservation, Bill) can access it directly without going through `getId()`. This is intentional because `getSummary()` implementations in subclasses sometimes reference `id` directly in their string output.

**The abstract method declaration:**  
```java
public abstract String getSummary();
```
This line forces every subclass to write its own version of `getSummary()`. If a class extends `BaseEntity` and does not implement `getSummary()`, the code will not compile. This is a compile-time enforcement of a behavioral contract.

**The `toString()` delegation:**  
```java
@Override
public String toString() { return getSummary(); }
```
This is where dynamic binding happens. At runtime, `getSummary()` resolves to the specific subclass implementation. This is how a `Room` displays as `"Room 101 - Standard Single (₱1200.00/night)"` and a `User` displays as `"Juan Dela Cruz (@juan)"` automatically in JavaFX UI components like `ListView` and `ComboBox`.

#### Most Important Lines

```java
public abstract class BaseEntity {
```
Declaring `abstract` prevents this class from being instantiated. You cannot write `new BaseEntity()` — you can only create objects from its concrete subclasses.

```java
protected int id;
```
Single declaration of the primary key field. All four model classes inherit this rather than each declaring their own.

```java
public abstract String getSummary();
```
The contract. Every model must explain itself in a single string. This is what drives polymorphism throughout the system.

```java
@Override
public String toString() { return getSummary(); }
```
Dynamic binding in one line. When JavaFX calls `toString()` on any model object (for display purposes), it gets the correct subclass implementation without any additional code.

---

### 3.2 Room.java

**Package:** `hotel.model`  
**File:** `src/hotel/model/Room.java`

#### What it does

`Room` represents a single hotel room in the system. It stores the room number, type (Standard Single, Deluxe, Suite, etc.), price per night, guest capacity, description, and availability status. This class is used everywhere — when staff add or edit rooms, when customers search for available rooms, and when reservations are created and linked to a specific room.

#### How it works

`Room` extends `BaseEntity`, which means it inherits the `id` field automatically. When a Room is retrieved from the database, the DAO sets all fields through the setters. When a Room is displayed in a JavaFX `TableView` or `ListView`, JavaFX calls `toString()` on the room object, which through inheritance runs `getSummary()` and displays a formatted room description.

The `Status` enum inside Room restricts the room's possible states to only three values: `AVAILABLE`, `OCCUPIED`, and `MAINTENANCE`. This prevents any code from accidentally assigning an invalid status like `"available"` or `"occupied"` (as raw strings), because the compiler enforces that only the three defined enum values are used.

#### Crucial Parts

**The `Status` enum — user-defined type:**  
```java
public enum Status { AVAILABLE, OCCUPIED, MAINTENANCE }
```
This is a user-defined type. The `status` field must be one of these three values — nothing else is allowed. This is far safer than using a plain `String` for status, which could cause bugs if someone types `"Occupied"` with a capital O or misspells `"MANTENANCE"`.

**The constructor with 5 parameters:**  
```java
public Room(String roomNumber, String roomType, double price, int capacity, String description) {
    ...
    this.status = Status.AVAILABLE;
}
```
When a new room is created, its status is automatically set to `AVAILABLE`. No one has to remember to set it — the constructor enforces this default.

**The `getSummary()` override:**  
```java
@Override
public String getSummary() {
    return "Room " + roomNumber + " - " + roomType + " (₱" + String.format("%.2f", price) + "/night)";
}
```
This is the polymorphic implementation. When a `Room` appears in the `NewReservation` screen's `ListView`, JavaFX automatically displays it as `"Room 101 - Standard Single (₱1200.00/night)"` — because `ListView` calls `toString()` on each item, which calls this method through dynamic binding.

#### Most Important Lines

```java
public enum Status { AVAILABLE, OCCUPIED, MAINTENANCE }
```
A user-defined type that restricts status to valid values only. The database also enforces this with a `CHECK` constraint.

```java
this.status = Status.AVAILABLE;
```
Default value set in the constructor — newly added rooms are always available.

```java
r.setStatus(Room.Status.valueOf(rs.getString("status")));
```
(In `RoomDAO.mapRow`) Converts the string stored in the database back into the enum type. `valueOf()` will throw an exception if the database contains an invalid value, which is a safety check.

---

### 3.3 User.java

**Package:** `hotel.model`  
**File:** `src/hotel/model/User.java`

#### What it does

`User` represents a person who has an account in the system. It covers both staff accounts (those who manage the hotel) and customer accounts (guests who make reservations). A single `User` object can hold all personal information: username, password hash, role, full name, email, phone number, home address, government ID type, and ID number.

#### How it works

`User` extends `BaseEntity` to inherit the `id` field. The `Role` enum restricts the user type to either `STAFF` or `CUSTOMER`. This role is the deciding factor in `LoginController` — after successful authentication, the system checks the user's role and routes them to either the Staff Dashboard or the Customer Dashboard.

The password field in a `User` object holds the SHA-256 hashed value, never the original plain text. When a user is retrieved from the database, the stored hash goes into `user.password`. When the user logs in, their typed password is hashed and compared against the stored hash.

#### Crucial Parts

**The `Role` enum:**  
```java
public enum Role { STAFF, CUSTOMER }
```
The role determines which dashboard the user sees after login. It also filters which users appear in certain lists — `getAllCustomers()` in `UserDAO` only returns `CUSTOMER` role accounts, ensuring staff accounts are never shown in the customer dropdown of the reservation form.

**The `getSummary()` override:**  
```java
@Override
public String getSummary() { return fullName + " (@" + username + ")"; }
```
When a `User` is displayed in the customer selection `ComboBox` in `NewReservationController`, it automatically appears as `"Juan Dela Cruz (@juan)"`. This is possible because `ComboBox` calls `toString()` on each item, which chains to this method.

**Separation of profile update and password change:**  
The `User` class stores both the profile fields and the password. In `UserDAO`, the `update()` method only updates profile fields. The `changePassword()` method is a completely separate operation. This design prevents accidental password overwrites during normal profile saves.

#### Most Important Lines

```java
public enum Role { STAFF, CUSTOMER }
```
The two possible user types. The system branches on this value at login.

```java
public User(String username, String password, Role role, String fullName) {
```
The constructor used during registration. Notice the password here is the plain text typed by the user — `UserDAO.register()` is responsible for hashing it before storing it.

```java
@Override
public String getSummary() { return fullName + " (@" + username + ")"; }
```
Determines how a user appears in all JavaFX components that display them.

---

### 3.4 Reservation.java

**Package:** `hotel.model`  
**File:** `src/hotel/model/Reservation.java`

#### What it does

`Reservation` represents a booking made by a customer for a specific room between two dates. It links a customer (by `customerId`) to a room (by `roomId`) and stores the check-in date, check-out date, number of guests, and any special notes. It also carries a status that changes over its lifetime: starts as `ACTIVE`, becomes `CHECKED_OUT` after the guest leaves, or `CANCELLED` if the booking is cancelled.

#### How it works

`Reservation` extends `BaseEntity` to inherit the `id` field. The class has two kinds of fields: **stored fields** (written directly to the `reservations` database table) and **joined fields** (populated at query time by joining with the `users` and `rooms` tables).

The stored fields are: `customerId`, `roomId`, `checkIn`, `checkOut`, `guests`, `notes`, `status`, and `createdAt`.

The joined fields are: `customerName`, `roomNumber`, `roomType`, and `roomPrice`. These are not columns in the `reservations` table — they come from joining with the `users` and `rooms` tables when querying. They exist in this Java class so that the UI can display them without making additional database calls.

The `getNights()` and `getTotalAmount()` methods are **computed/derived** — they are never stored in the database. They are calculated on the fly from the dates and room price.

#### Crucial Parts

**The `Status` enum:**  
```java
public enum Status { ACTIVE, CHECKED_OUT, CANCELLED }
```
This enforces valid state transitions. The system only allows check-out or cancellation of `ACTIVE` reservations — this is checked in `StaffDashboardController` before calling the DAO.

**The `getNights()` method:**  
```java
public long getNights() {
    if (checkIn == null || checkOut == null) return 0;
    return ChronoUnit.DAYS.between(checkIn, checkOut);
}
```
This uses Java's `ChronoUnit.DAYS.between()` to calculate the exact number of days between two `LocalDate` objects. It is null-safe (returns 0 if dates are not set yet). This method is called whenever the total amount needs to be calculated, and its result is shown in the nights column of every reservation table.

**The `getTotalAmount()` method:**  
```java
public double getTotalAmount() {
    return roomPrice * getNights();
}
```
A derived calculation. The `roomPrice` comes from the JOIN query, and `getNights()` is computed from dates. The actual monetary total is never stored in the reservations table — it is always recalculated from the source data.

**JOIN-populated fields:**  
```java
private String customerName;
private String roomNumber;
private String roomType;
private double roomPrice;
```
These fields do not map to any column in the `reservations` database table. They are populated by the `mapRow()` method in `ReservationDAO` after executing a JOIN query. This design avoids N+1 query problems — one SQL statement fetches everything needed to display a complete reservation row.

#### Most Important Lines

```java
public enum Status { ACTIVE, CHECKED_OUT, CANCELLED }
```
All valid states of a reservation. Invalid state changes (e.g., checking out an already cancelled reservation) are blocked at the controller level.

```java
return ChronoUnit.DAYS.between(checkIn, checkOut);
```
The exact night count calculation. `ChronoUnit.DAYS` from the `java.time.temporal` package handles month boundaries, leap years, and daylight saving time correctly.

```java
private String customerName;
private String roomNumber;
```
Not database columns — these are populated by JOIN queries so the UI can show a customer's name and room number without running extra queries.

---

### 3.5 Bill.java

**Package:** `hotel.model`  
**File:** `src/hotel/model/Bill.java`

#### What it does

`Bill` represents the financial record generated when a customer checks out. It stores the total amount charged, any discount applied, any tax added, the payment status (paid or unpaid), and the payment method used. A bill is always linked to exactly one reservation and is created automatically during the check-out process.

Bills serve as permanent audit records — they are never deleted from the database, which is why `BillDAO.delete()` always returns false.

#### How it works

`Bill` extends `BaseEntity` for the `id` field. Like `Reservation`, it has two categories of fields: stored fields (those written to the `bills` table) and joined fields (those pulled in by a JOIN across the `bills`, `reservations`, `users`, and `rooms` tables).

The billing math is handled by two computed methods:
- `getSubtotal()` — the raw room cost before any adjustments
- `getGrandTotal()` — the final amount after applying discount and tax

These computed methods are used when generating PDF receipts and when displaying billing information in the staff dashboard.

#### Crucial Parts

**Computed financial methods:**  
```java
public double getSubtotal() { return roomPrice * nights; }
public double getGrandTotal() { return getSubtotal() - discount + tax; }
```
The subtotal is the base room charge. The grand total subtracts any discount and adds any tax. These are computed each time they are called — they depend on the `roomPrice` and `nights` fields that come from the JOIN query.

**The `paid` field as boolean:**  
```java
private boolean paid;
```
In the database, this is stored as `INTEGER (0 or 1)`. In `BillDAO.mapRow()`, the raw integer is converted to a boolean:
```java
b.setPaid(rs.getInt("paid") == 1);
```
This conversion means the Java layer works cleanly with `true`/`false` while SQLite stores it as `0`/`1`.

**The `getSummary()` override:**  
```java
@Override
public String getSummary() {
    return "Bill #" + id + " | " + customerName
           + " | ₱" + String.format("%.2f", getGrandTotal())
           + " | " + (paid ? "PAID" : "UNPAID");
}
```
When a bill appears in a list or is printed to the console, this is the description that shows up. It uses the computed `getGrandTotal()` value, so it always reflects the correct final amount.

#### Most Important Lines

```java
public double getSubtotal() { return roomPrice * nights; }
```
Room price multiplied by number of nights. The base charge before discounts or taxes.

```java
public double getGrandTotal() { return getSubtotal() - discount + tax; }
```
The final amount the customer pays. Used in PDF generation and the billing table.

```java
b.setPaid(rs.getInt("paid") == 1);
```
(In `BillDAO.mapRow`) Converts the SQLite integer (0 or 1) to a Java boolean. This is a type boundary conversion — SQLite does not have a native boolean type.

---

## 4. DAO Layer

The DAO (Data Access Object) layer is responsible for all communication with the SQLite database. No other layer — not controllers, not models — ever writes SQL directly. Controllers call DAO methods, which return Java objects. This separation keeps the database logic in one place and shields the rest of the system from SQL details.

---

### 4.1 Dao.java (Interface)

**Package:** `hotel.dao`  
**File:** `src/hotel/dao/Dao.java`

#### What it does

`Dao<T>` is a **generic interface** that defines the standard CRUD (Create, Read, Update, Delete) contract that every DAO class must implement. It declares four method signatures that describe the minimum operations any data access class must support.

This interface enables polymorphism: code can refer to a `Dao<Room>` or `Dao<User>` without needing to know whether it is talking to `RoomDAO`, `UserDAO`, or any other implementation. The interface also enforces consistency — all DAOs provide `getAll()`, `getById()`, `save()`, and `delete()` methods, even if some of those operations are intentionally disabled (like `delete()` for bills).

#### How it works

The type parameter `T extends BaseEntity` is a **bounded type parameter**. It means the generic type `T` is not just any class — it must be a subclass of `BaseEntity`. This constraint ensures that only model objects can be used with the Dao interface. You cannot accidentally create a `Dao<String>` or `Dao<Integer>`.

Each of the four concrete DAO classes (`RoomDAO`, `UserDAO`, `ReservationDAO`, `BillDAO`) must implement all four methods. When a method is intentionally not supported (like deleting a bill), the implementation returns `false` and does nothing — the interface is still satisfied.

#### Crucial Parts

**The generic type constraint:**  
```java
public interface Dao<T extends BaseEntity> {
```
The `extends BaseEntity` bound restricts what type can be used as `T`. Only `Room`, `User`, `Reservation`, and `Bill` qualify, because they are the only classes that extend `BaseEntity`. Any other class would cause a compile error.

**The four method signatures:**  
```java
List<T> getAll();
T getById(int id);
boolean save(T entity);
boolean delete(int id);
```
These are the minimal operations for any data store. The interface does not say how they work — just that they must exist and what they return.

#### Most Important Lines

```java
public interface Dao<T extends BaseEntity> {
```
A generic, bounded interface. This single line enables type-safe polymorphism across all four DAO implementations.

```java
List<T> getAll();
```
Returns all records of type T. The actual SQL and object mapping is hidden inside each implementing class.

```java
boolean save(T entity);
```
Accepts any object that extends `BaseEntity`. The specific insert or update logic is up to each DAO to define.

---

### 4.2 RoomDAO.java

**Package:** `hotel.dao`  
**File:** `src/hotel/dao/RoomDAO.java`

#### What it does

`RoomDAO` is responsible for all database operations related to the `rooms` table. It provides methods to get all rooms, get available rooms for a date range, add a new room, update an existing room's details and status, and delete a room. It implements the `Dao<Room>` interface, meaning it is a concrete implementation of the generic CRUD contract.

#### How it works

Every method in `RoomDAO` follows the same pattern:
1. Get the database connection from `DatabaseManager.getConnection()`
2. Create a `PreparedStatement` with the SQL query
3. Set parameters on the prepared statement (to avoid SQL injection)
4. Execute the query
5. If it is a read operation, convert each row of the `ResultSet` to a `Room` object using the private `mapRow()` method
6. Return the result or close the statement

The try-with-resources syntax (`try (PreparedStatement ps = ...)`) is used throughout. This automatically closes the prepared statement after the block ends, preventing memory and connection leaks.

#### Crucial Parts

**The availability query with NOT IN subquery:**  
```java
public List<Room> getAvailableRooms(String checkIn, String checkOut) {
    String sql = """
        SELECT * FROM rooms
        WHERE status != 'MAINTENANCE'
          AND id NOT IN (
              SELECT room_id FROM reservations
              WHERE status = 'ACTIVE'
                AND check_in  < ?
                AND check_out > ?
          )
        ORDER BY room_type, price
    """;
```
This is the most important query in the system. It finds rooms that are NOT already booked during the requested date range. The date overlap logic `check_in < checkOut AND check_out > checkIn` correctly identifies any reservation that overlaps with the requested window — even partial overlaps. A room that is under maintenance is also excluded.

The parameters are passed in reverse — `checkOut` first, then `checkIn`:
```java
ps.setString(1, checkOut);
ps.setString(2, checkIn);
```
This is because the SQL uses `check_in < ?` (the first `?` is the requested check-out) and `check_out > ?` (the second `?` is the requested check-in). The logic reads: exclude rooms where an existing reservation's check-in is before your requested check-out AND where the existing reservation's check-out is after your requested check-in — meaning the stays overlap.

**The private `mapRow()` method:**  
```java
private Room mapRow(ResultSet rs) throws SQLException {
    Room r = new Room();
    r.setId(rs.getInt("id"));
    r.setRoomNumber(rs.getString("room_number"));
    ...
    r.setStatus(Room.Status.valueOf(rs.getString("status")));
    return r;
}
```
This method is private because it is an internal implementation detail — controllers never touch `ResultSet` objects. All the column-to-field mapping is isolated here. `Room.Status.valueOf()` converts the stored string back to the enum type.

#### Most Important Lines

```java
public class RoomDAO implements Dao<Room> {
```
Declares that this class implements the generic interface with `Room` as the type. This satisfies the polymorphism contract.

```java
AND id NOT IN (
    SELECT room_id FROM reservations
    WHERE status = 'ACTIVE'
      AND check_in  < ?
      AND check_out > ?
)
```
The availability check. This subquery finds all rooms currently occupied during the requested date window, and the outer query excludes those rooms.

```java
r.setStatus(Room.Status.valueOf(rs.getString("status")));
```
Converts the database string to an enum value. If the database contains an invalid status, this throws an exception at the data access layer, not silently inside the UI.

---

### 4.3 UserDAO.java

**Package:** `hotel.dao`  
**File:** `src/hotel/dao/UserDAO.java`

#### What it does

`UserDAO` handles all database operations related to the `users` table. This includes logging in, registering a new customer account, retrieving all customers, updating a customer's profile, and changing a password. It implements the `Dao<User>` interface.

The `UserDAO` is also responsible for password security — it always uses `PasswordUtil.hash()` before storing a password and `PasswordUtil.verify()` before accepting a login.

#### How it works

The most complex method is `login()`. It retrieves the user record for the given username, then compares the typed password against the stored hash using `PasswordUtil.verify()`. If that fails, it also checks whether the stored value is a plain-text match (for backward compatibility with older accounts that may not have been hashed). If a plain-text match is found, it immediately re-hashes the password and saves it back to the database — this is an automatic password migration.

The `register()` method hashes the password immediately before constructing the INSERT statement. The plain-text password that arrives in the `User` object never touches the database.

The `update()` method intentionally excludes the password field. Profile updates (name, email, phone, address) and password changes are separate operations. This prevents a profile update from accidentally overwriting the hashed password with an empty string.

#### Crucial Parts

**The login method with hash verification and migration:**  
```java
public User login(String username, String password) {
    ...
    if (PasswordUtil.verify(password, stored)) {
        return user;
    }
    // Fallback: plain-text match from old DB — auto-migrate to hash
    if (password.equals(stored)) {
        changePassword(user.getId(), password);
        return user;
    }
    ...
}
```
This handles two cases: (1) a modern account where the password is SHA-256 hashed, and (2) a legacy account where the password was stored in plain text. In case 2, the system still logs the user in but immediately re-hashes and saves the password so it is secure going forward. This is transparent to the user.

**Password hashing on register:**  
```java
ps.setString(2, PasswordUtil.hash(user.getPassword()));
```
The plain-text password in `user.getPassword()` is hashed before being placed into the SQL statement. The raw password never appears in the database.

**Soft delete via `delete()` returning false:**  
```java
@Override
public boolean delete(int id) { return false; }
```
User accounts are never hard-deleted. The interface requires a `delete()` method, but this implementation deliberately does nothing. This is a design decision — deleting a customer account would break all historical reservations linked to that customer ID.

#### Most Important Lines

```java
ps.setString(2, PasswordUtil.hash(user.getPassword()));
```
The password is hashed before being stored. If this line were `user.getPassword()` without hashing, every password would be stored in plain text — a critical security vulnerability.

```java
if (PasswordUtil.verify(password, stored)) { return user; }
```
The correct way to check a password: hash the typed input and compare against the stored hash. Never compare hashes directly or decrypt them.

```java
if (password.equals(stored)) { changePassword(user.getId(), password); return user; }
```
The legacy migration check. If someone was registered before hashing was added, they can still log in and the system silently upgrades their stored password.

```java
String sql = "SELECT * FROM users WHERE role='CUSTOMER' ORDER BY full_name";
```
The `getAllCustomers()` query. It specifically filters for STAFF accounts to be excluded, so staff members never appear in the customer selection dropdown.

---

### 4.4 ReservationDAO.java

**Package:** `hotel.dao`  
**File:** `src/hotel/dao/ReservationDAO.java`

#### What it does

`ReservationDAO` manages all database operations for the `reservations` table. It handles creating new reservations, retrieving all or specific reservations, checking out a guest (which also creates a bill), and cancelling a reservation. It implements the `Dao<Reservation>` interface.

This is the most complex DAO because several operations must affect multiple tables at once — and they must do so atomically (all or nothing) using database transactions.

#### How it works

The key design feature in `ReservationDAO` is the use of **database transactions** in the three write operations: `create()`, `checkOut()`, and `cancel()`. A transaction is a group of SQL statements that either all succeed together or all fail together. If any statement in the middle fails, the entire group is rolled back to the previous state.

For read operations, `ReservationDAO` uses a constant JOIN query fragment called `JOIN_SQL` that is reused across `getAll()`, `getById()`, and `getByCustomer()`. This avoids repeating the same JOIN logic in every method.

The `query()` private helper method runs any SQL that returns reservation results, accepting an optional integer parameter. This single helper is used by three different read methods.

#### Crucial Parts

**The JOIN_SQL constant:**  
```java
private static final String JOIN_SQL =
    "SELECT r.*, u.full_name, rm.room_number, rm.room_type, rm.price " +
    "FROM reservations r " +
    "JOIN users u  ON r.customer_id = u.id " +
    "JOIN rooms rm ON r.room_id = rm.id ";
```
This joins three tables in a single query: reservations (aliased as `r`), users (aliased as `u`), and rooms (aliased as `rm`). Every reservation query gets the customer name, room number, room type, and price in one round-trip to the database. This is used by appending `WHERE` or `ORDER BY` clauses as needed.

**The `create()` method with atomic transaction:**  
```java
public boolean create(Reservation res) {
    Connection conn = DatabaseManager.getConnection();
    conn.setAutoCommit(false);
    // 1. Insert the reservation
    // 2. Mark the room as OCCUPIED
    conn.commit();
    ...
    tryRollback(conn);
}
```
Both steps must succeed together. If the room status update fails after the reservation is inserted, the entire operation rolls back and neither change is saved. This prevents the system from having an inconsistent state (a reservation with an AVAILABLE room, or an OCCUPIED room with no reservation).

**The `checkOut()` method — 3 operations in one transaction:**  
```java
public boolean checkOut(int reservationId, int roomId, double subtotal, ...) {
    conn.setAutoCommit(false);
    // 1. Mark reservation as CHECKED_OUT
    // 2. Mark room as AVAILABLE
    // 3. Insert bill record (INSERT OR IGNORE to prevent duplicates)
    conn.commit();
}
```
Three separate SQL statements run in a single transaction. The `INSERT OR IGNORE` on the bill prevents duplicate bills if checkOut is accidentally called twice.

**The `tryRollback()` and `trySetAutoCommit()` helpers:**  
```java
private void tryRollback(Connection c) {
    try { if (c != null) c.rollback(); } catch (SQLException ignored) {}
}
private void trySetAutoCommit(Connection c) {
    try { if (c != null) c.setAutoCommit(true); } catch (SQLException ignored) {}
}
```
These are cleanup helpers called in `finally` blocks. The `finally` block always runs — whether the transaction succeeded or failed. Setting `autoCommit` back to `true` in `finally` ensures the connection is not left in a broken state for future queries.

#### Most Important Lines

```java
conn.setAutoCommit(false);
```
Turns off automatic commits. From this point, no SQL change is saved to the database until `conn.commit()` is explicitly called.

```java
conn.commit();
```
Finalizes all changes in the transaction. This is the point of no return — once committed, the changes are permanent.

```java
tryRollback(conn);
```
Called in the `catch` block. If anything went wrong, this reverses all changes made during the transaction.

```java
"INSERT OR IGNORE INTO bills ..."
```
The `OR IGNORE` prevents a duplicate bill from being created if the check-out button is clicked twice. The `UNIQUE` constraint on `bills.reservation_id` enforces one bill per reservation.

---

### 4.5 BillDAO.java

**Package:** `hotel.dao`  
**File:** `src/hotel/dao/BillDAO.java`

#### What it does

`BillDAO` manages all database operations for the `bills` table. It provides methods to retrieve all bills, retrieve a bill by its own ID or by reservation ID, and mark a bill as paid. It implements the `Dao<Bill>` interface.

Importantly, both `save()` and `delete()` return `false` — bills are created exclusively by `ReservationDAO.checkOut()` as part of its transaction, and they are never deleted because they serve as permanent financial records.

#### How it works

`BillDAO` has the most complex JOIN query in the system. A bill record on its own only contains `reservation_id`, `total_amount`, `discount`, `tax`, `paid`, and `payment_method`. To display a complete bill (with customer name, room number, check-in/out dates, and number of nights), the query must join four tables.

The number of nights is calculated in SQL itself using SQLite's `julianday()` function:
```sql
CAST(julianday(r.check_out) - julianday(r.check_in) AS INTEGER) AS nights
```
This computes the night count at the database level, so the Java `Bill` object already has the `nights` field populated when it arrives from the query.

#### Crucial Parts

**The 4-table JOIN query:**  
```java
private static final String JOIN_SQL = """
    SELECT b.*,
           u.full_name,
           rm.room_number,
           rm.room_type,
           rm.price       AS room_price,
           r.check_in,
           r.check_out,
           CAST(julianday(r.check_out) - julianday(r.check_in) AS INTEGER) AS nights
    FROM bills b
    JOIN reservations r ON b.reservation_id = r.id
    JOIN users u        ON r.customer_id = u.id
    JOIN rooms rm       ON r.room_id = rm.id
""";
```
This single SQL statement connects four tables. The `julianday()` function converts dates to fractional day numbers, and their difference gives the number of nights stayed. The result is cast to `INTEGER` to remove the decimal part.

**The `getByReservationId()` method:**  
```java
public Bill getByReservationId(int reservationId) {
    String sql = JOIN_SQL + "WHERE b.reservation_id = ?";
    ...
}
```
Used by `CustomerDashboardController` when a customer selects a reservation and clicks "View Bill." The customer does not know the bill ID — they only know the reservation. This method finds the bill that belongs to their selected reservation.

**The `markPaid()` method:**  
```java
public boolean markPaid(int billId, String paymentMethod) {
    String sql = "UPDATE bills SET paid=1, payment_method=? WHERE id=?";
    ...
}
```
Sets the `paid` column to `1` (true in SQLite) and records the payment method. This is called from `StaffDashboardController` when staff manually mark a bill as paid after the fact.

#### Most Important Lines

```java
CAST(julianday(r.check_out) - julianday(r.check_in) AS INTEGER) AS nights
```
Night count calculated in SQL. `julianday()` converts a date string to a decimal day number. The subtraction gives the number of days between check-in and check-out.

```java
@Override
public boolean save(Bill bill) { return false; }
@Override
public boolean delete(int id) { return false; }
```
Bills cannot be created through this DAO or deleted. They are audit records. The interface is satisfied, but the operations are intentionally disabled.

```java
b.setPaid(rs.getInt("paid") == 1);
```
Converts SQLite's integer 0/1 to Java boolean `false`/`true`. The comparison `== 1` returns true only when the stored value is exactly 1.

---

## 5. Utility Layer

The utility layer contains classes that provide shared services used across the entire system. None of these classes can be instantiated — they use the utility class pattern with private constructors and only static methods.

---

### 5.1 DatabaseManager.java

**Package:** `hotel.util`  
**File:** `src/hotel/util/DatabaseManager.java`

#### What it does

`DatabaseManager` is the central point of control for the SQLite database connection. It does four things:
1. Creates and maintains a single shared database connection
2. Applies performance and correctness settings (PRAGMAs) every time the connection is opened
3. Creates the four database tables when the app first runs (`initializeDatabase()`)
4. Seeds default data (one admin staff account and eight sample rooms) on first launch

Every DAO in the system calls `DatabaseManager.getConnection()` to get the database connection. There is only ever one connection, shared by all DAOs.

#### How it works

The connection is stored in a `private static Connection connection` field. The `getConnection()` method checks whether the connection is null or closed before returning it. If it is, it creates a new connection and applies four SQLite PRAGMA settings.

The `initializeDatabase()` method runs on application startup (called from `MainApp.start()`). It uses `CREATE TABLE IF NOT EXISTS` so it is safe to run every time — it only creates tables that do not already exist. This means the app can be launched repeatedly and the tables are only created once on the first launch.

The database file is created automatically by SQLite in the working directory when the connection string `jdbc:sqlite:hotel.db` is used. No manual setup is needed.

#### Crucial Parts

**The PRAGMA settings on connection:**  
```java
s.execute("PRAGMA journal_mode=WAL");
s.execute("PRAGMA foreign_keys=ON");
s.execute("PRAGMA synchronous=NORMAL");
s.execute("PRAGMA cache_size=10000");
```
These four settings are applied every time a new connection is created:
- `WAL` (Write-Ahead Logging) allows reads to happen concurrently with writes — better performance during updates.
- `foreign_keys=ON` enforces referential integrity — a reservation cannot reference a non-existent customer or room.
- `synchronous=NORMAL` balances write safety and speed — the OS flushes data periodically instead of on every write.
- `cache_size=10000` keeps 10,000 database pages in memory for faster repeated queries.

**The synchronized keyword on `getConnection()`:**  
```java
public static synchronized Connection getConnection() throws SQLException {
```
The `synchronized` keyword ensures only one thread at a time can enter this method. JavaFX runs some background operations on different threads, and this prevents two threads from both finding `connection == null` and both trying to create a connection simultaneously.

**The `CHECK` constraints in table definitions:**  
```java
role TEXT NOT NULL CHECK(role IN ('STAFF','CUSTOMER'))
status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK(status IN ('AVAILABLE','OCCUPIED','MAINTENANCE'))
```
These SQLite constraints enforce at the database level that only valid values can be stored. Even if a bug in the Java code tries to insert `"admin"` as a role or `"EMPTY"` as a status, the database will reject it.

**The `seedDefaultData()` method:**  
```java
stmt.execute("INSERT OR IGNORE INTO users (username,password,role,full_name,email) "
           + "VALUES ('admin','" + adminHash + "','STAFF','Administrator','admin@hotel.com')");
```
`INSERT OR IGNORE` only inserts if the record does not already exist. The admin password is hashed using `PasswordUtil.hash("admin123")` before being stored. This ensures that even the seed data uses secure passwords.

**Database indexes:**  
```java
stmt.execute("CREATE INDEX IF NOT EXISTS idx_res_room   ON reservations(room_id)");
stmt.execute("CREATE INDEX IF NOT EXISTS idx_res_cust   ON reservations(customer_id)");
stmt.execute("CREATE INDEX IF NOT EXISTS idx_res_status ON reservations(status)");
stmt.execute("CREATE INDEX IF NOT EXISTS idx_res_dates  ON reservations(check_in,check_out)");
```
Four indexes on the `reservations` table improve query speed for the most common lookups: finding reservations by room (used in availability check), by customer (used in customer history), by status, and by date range.

#### Most Important Lines

```java
private static final String DB_URL = "jdbc:sqlite:hotel.db";
```
The database file path. `hotel.db` is created in the working directory (the project root) when the application first runs.

```java
s.execute("PRAGMA foreign_keys=ON");
```
Without this, SQLite ignores `FOREIGN KEY` constraints by default. This line enables them, so inserting a reservation with an invalid `customer_id` will fail.

```java
s.execute("PRAGMA journal_mode=WAL");
```
Enables WAL mode, which allows concurrent reads during writes. This makes the application more responsive during data updates.

```java
String adminHash = PasswordUtil.hash("admin123");
```
The default admin password is hashed before being inserted into the database. The plain text `"admin123"` never appears in the stored record.

---

### 5.2 PasswordUtil.java

**Package:** `hotel.util`  
**File:** `src/hotel/util/PasswordUtil.java`

#### What it does

`PasswordUtil` is a utility class that provides password hashing and verification using the SHA-256 cryptographic algorithm. Its sole purpose is to ensure that passwords are never stored or compared in plain text.

SHA-256 is a one-way hash function: given a password, it always produces the same 64-character hexadecimal string, but you cannot reverse a hash back to the original password. To verify a login, you hash the typed password and compare the hash — not the original text.

#### How it works

The `hash()` method uses Java's built-in `MessageDigest` class from the `java.security` package. It gets an instance of the SHA-256 algorithm, calls `digest()` on the plain-text password bytes, and converts the resulting byte array to a hexadecimal string.

The output is always exactly 64 characters long, regardless of how long the input password is. This is the value that gets stored in the database's `password` column.

The `verify()` method is simple: it hashes the provided plain-text password and checks whether the result equals the stored hash.

#### Crucial Parts

**The `MessageDigest` usage:**  
```java
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] bytes = md.digest(plainText.getBytes());
```
`MessageDigest.getInstance("SHA-256")` requests the SHA-256 implementation from the Java security provider. `digest()` takes a byte array and returns the hash as a 64-byte array. SHA-256 is the same algorithm used in banking systems and cryptocurrency.

**The hex conversion:**  
```java
StringBuilder sb = new StringBuilder();
for (byte b : bytes) sb.append(String.format("%02x", b));
return sb.toString();
```
Each byte from the hash result (which can be 0–255) is formatted as two lowercase hexadecimal characters (`%02x` means "at least 2 hex digits, padded with zero"). 32 bytes × 2 characters = 64 character hex string.

**The private constructor:**  
```java
private PasswordUtil() {}
```
Prevents anyone from creating a `PasswordUtil` instance. This is the utility class pattern — all methods are static and can be called without instantiation.

#### Most Important Lines

```java
MessageDigest md = MessageDigest.getInstance("SHA-256");
```
Requests the SHA-256 hashing algorithm from the Java runtime. The `NoSuchAlgorithmException` is caught and re-thrown as a `RuntimeException` because SHA-256 is guaranteed to be available in all Java implementations.

```java
byte[] bytes = md.digest(plainText.getBytes());
```
The hash calculation. The plain-text string is converted to bytes, and SHA-256 produces a fixed 32-byte (256-bit) output.

```java
sb.append(String.format("%02x", b));
```
Converts each byte to two hexadecimal characters. The format specifier `%02x` pads with a leading zero if the hex value is a single digit (e.g., byte value 9 becomes `"09"` not `"9"`).

```java
public static boolean verify(String plainText, String hash) {
    return hash(plainText).equals(hash);
}
```
The verification logic. Hash the typed password and compare — never decrypt.

---

### 5.3 Session.java

**Package:** `hotel.util`  
**File:** `src/hotel/util/Session.java`

#### What it does

`Session` is a singleton class that holds the currently logged-in user for the duration of the application's runtime. After a successful login, `LoginController` stores the authenticated `User` object in `Session`. Every other controller that needs to know who is logged in reads from `Session` without needing any parameters passed to them.

#### How it works

`Session` stores one `private static User currentUser` field. Being `static` means it is shared across the entire application — there is only one copy regardless of how many controllers are active. When a user logs in, `setCurrentUser(user)` saves the object. When they log out, `logout()` sets it back to `null`.

The `isStaff()` helper method provides a clean boolean check that can be used anywhere without casting or null checks.

#### Crucial Parts

**The static `currentUser` field:**  
```java
private static User currentUser;
```
Static means there is one instance shared across the entire JVM. When `LoginController` calls `Session.setCurrentUser(user)`, the value is immediately visible to `StaffDashboardController`, `CustomerDashboardController`, or any other class that calls `Session.getCurrentUser()` — even though they are different objects.

**The `logout()` method:**  
```java
public static void logout() { currentUser = null; }
```
Setting `currentUser` to null effectively ends the session. Any controller that calls `Session.getCurrentUser()` after logout will receive `null`, which prevents unauthorized access to data.

**The `isStaff()` helper:**  
```java
public static boolean isStaff() {
    return currentUser != null && currentUser.getRole() == User.Role.STAFF;
}
```
A null-safe role check. If `currentUser` is null (not logged in), this returns `false` without throwing a `NullPointerException`. The short-circuit `&&` ensures the second condition is only evaluated if the first is true.

#### Most Important Lines

```java
private static User currentUser;
```
The shared state. One field, accessible from anywhere in the application.

```java
public static void setCurrentUser(User user) { currentUser = user; }
```
Called by `LoginController` immediately after a successful login. All subsequent screens access this to know who the logged-in user is.

```java
public static void logout() { currentUser = null; }
```
Called by both dashboard controllers. Clears the session and returns to the login screen.

---

## 6. Application Entry Point

---

### 6.1 MainApp.java

**Package:** `hotel`  
**File:** `src/hotel/MainApp.java`

#### What it does

`MainApp` is the entry point of the entire application. It is the class that Java runs first when the program starts. It inherits from JavaFX's `Application` class (making it a proper JavaFX application), initializes the database, shows the first screen maximized, and provides a static `showScreen()` method that every controller calls to navigate between screens. Every screen switch always opens the window in fullscreen/maximized mode.

#### How it works

JavaFX requires the entry point class to extend `Application` and implement the `start()` method. When the application launches, the JavaFX runtime calls `start()` with the primary `Stage` object (the main application window).

`MainApp.start()` saves that stage as a `private static Stage primaryStage`, initializes the database, then calls `stage.show()` **before** calling `showScreen("Login")`. This ordering is critical: on Windows, `setMaximized(true)` only works reliably on a stage that is already visible. By showing the stage first and then loading the Login screen through `showScreen()`, the maximization fires on a live window and takes effect immediately.

The `showScreen()` method is the navigation mechanism of the entire application. Every controller that wants to switch screens calls `MainApp.showScreen("ScreenName")`. The method loads the corresponding FXML file from the classpath, creates a new `Scene` from it, and uses a `setMaximized(false)` → `setScene()` → `setMaximized(true)` sequence to guarantee the window is fullscreen after every screen switch.

The `stop()` method is automatically called by JavaFX when the application window is closed. It closes the database connection cleanly.

#### Crucial Parts

**Inheritance from `Application`:**  
```java
public class MainApp extends Application {
```
JavaFX requires the main class to extend `Application`. This gives `MainApp` access to the `launch()` mechanism and the `start()`/`stop()` lifecycle methods. This is the main example of standard library inheritance in the system.

**The static primary stage:**  
```java
private static Stage primaryStage;
```
The stage is stored as `static` so that `showScreen()` (also static) can access it from anywhere. Controllers call `MainApp.showScreen("X")` without needing a reference to the `MainApp` instance.

**The `start()` method — show before load:**  
```java
stage.show();        // show first so setMaximized works reliably on Windows
showScreen("Login");
```
The stage is made visible with `show()` before `showScreen()` is called. This is intentional: JavaFX's `setMaximized(true)` only produces a visible maximized window when called on an already-showing stage. If `showScreen()` were called first, the Login screen would appear at its natural preferred size (400×500) instead of fullscreen.

**The `showScreen()` fullscreen sequence:**  
```java
primaryStage.setMaximized(false); // reset so the next true triggers a real resize
primaryStage.setScene(scene);
primaryStage.setMaximized(true);
```
This three-step sequence is used for every screen switch. The reason `setMaximized(false)` is needed first is that when the stage is already marked as maximized (property value is `true`) and a new scene is loaded, calling `setScene()` can cause JavaFX to resize the window to the new scene's preferred dimensions. At that point the maximized property is still `true` but the window is visually smaller. Calling `setMaximized(true)` again would be a no-op because the property did not change. Resetting to `false` first forces a real property transition from `false` to `true`, which triggers the OS to actually maximize the window.

**The `stop()` lifecycle method:**  
```java
@Override
public void stop() {
    DatabaseManager.closeConnection();
}
```
JavaFX calls `stop()` automatically when the user closes the window. This ensures the SQLite connection is properly closed, preventing database file corruption and connection leaks.

#### Most Important Lines

```java
public class MainApp extends Application {
```
Inheritance from `javafx.application.Application`. This is required for any JavaFX program to launch.

```java
DatabaseManager.initializeDatabase();
```
Called once on startup. Creates all tables and seeds default data if they do not exist yet.

```java
stage.show();
showScreen("Login");
```
Order matters. The stage must be visible before `showScreen()` runs so that `setMaximized(true)` inside `showScreen()` actually maximizes the window.

```java
primaryStage.setMaximized(false);
primaryStage.setScene(scene);
primaryStage.setMaximized(true);
```
The guaranteed fullscreen sequence used on every screen transition. Resetting to `false` first ensures the subsequent `true` always fires a real property change event, regardless of the previous maximized state.

---

## 7. UI Controller Layer

UI controllers are the bridge between the JavaFX layouts (FXML files) and the DAO layer. Each controller handles user interactions (button clicks, form submissions, table selections) on one screen. Controllers never write SQL — they call DAO methods and update the UI based on the results.

---

### 7.1 LoginController.java

**Package:** `hotel.ui`  
**File:** `src/hotel/ui/LoginController.java`

#### What it does

`LoginController` handles the login screen. When the user types their username and password and clicks the Login button, it authenticates them using `UserDAO.login()`. If authentication succeeds, it stores the user in `Session` and routes them to the correct dashboard based on their role. It also handles the "Go to Register" button to switch to the registration screen.

#### How it works

The `handleLogin()` method is annotated with `@FXML`, meaning it is linked to a button in the Login.fxml file. When the button is clicked, JavaFX automatically calls this method.

The method first validates that both fields are filled. Then it calls `userDAO.login()`, which returns either a `User` object (successful login) or `null` (failed login). On success, it calls `Session.setCurrentUser(user)` to store the logged-in user globally, then checks the user's role to decide which dashboard to show.

#### Crucial Parts

**Role-based routing:**  
```java
if (user.getRole() == User.Role.STAFF) {
    MainApp.showScreen("StaffDashboard");
} else {
    MainApp.showScreen("CustomerDashboard");
}
```
This is polymorphic behavior through role checking. The same login screen leads to completely different experiences depending on who logs in. A staff member sees room management and all customer data; a customer sees only their own reservations.

**Session storage before navigation:**  
```java
Session.setCurrentUser(user); // store logged-in user for the whole session
if (user.getRole() == User.Role.STAFF) { ... }
```
The user must be stored in `Session` before the screen switches. The dashboard controllers call `Session.getCurrentUser()` inside their `initialize()` methods, which run immediately when the FXML loads. If `Session` is empty at that point, a NullPointerException would occur.

#### Most Important Lines

```java
User user = userDAO.login(username, password);
```
Delegates authentication entirely to the DAO. The controller does not know about hashing or database queries — it just calls `login()` and gets back a result.

```java
Session.setCurrentUser(user);
```
Stores the authenticated user globally. Every subsequent controller reads from this to know who is logged in.

```java
if (user.getRole() == User.Role.STAFF) {
    MainApp.showScreen("StaffDashboard");
} else {
    MainApp.showScreen("CustomerDashboard");
}
```
Role-based routing. One login button, two possible outcomes depending on the role stored in the database.

---

### 7.2 RegisterController.java

**Package:** `hotel.ui`  
**File:** `src/hotel/ui/RegisterController.java`

#### What it does

`RegisterController` handles the self-registration screen where new customers create their own accounts. It collects the required information (name, username, password) and optional information (email, phone, address, government ID), validates the inputs, and calls `UserDAO.register()` to create the account. It also enforces a phone number input rule: digits only, maximum 11 characters.

This screen is only for customer registration. Staff accounts are created separately by the system administrator.

#### How it works

`RegisterController` implements `Initializable`, which means it has an `initialize()` method that runs automatically after all `@FXML` fields are injected. Inside `initialize()`, it sets up the dropdown list of Philippine government ID types, applies the `setButtonCell()` fix so the selected ID type is visible on dark backgrounds, and attaches a `TextFormatter` to the phone number field.

The `TextFormatter` is applied in the code:
```java
phoneField.setTextFormatter(new TextFormatter<>(change -> {
    if (change.getControlNewText().matches("\\d{0,11}")) return change;
    return null;
}));
```
This validates the input character by character. The regex `\\d{0,11}` means "zero to eleven digits." If the new proposed text after a keystroke would violate this pattern (a letter was typed, or an 12th digit was typed), the change is rejected by returning `null` and the field stays unchanged.

#### Crucial Parts

**Phone number restriction with `TextFormatter`:**  
```java
phoneField.setTextFormatter(new TextFormatter<>(change -> {
    if (change.getControlNewText().matches("\\d{0,11}")) return change;
    return null;
}));
```
A `TextFormatter` intercepts every proposed change to a text field before it is applied. The lambda receives a `TextFormatter.Change` object that contains the current field content plus the proposed new character. If the result of applying the change (`getControlNewText()`) matches the allowed pattern, the change proceeds. Otherwise, `null` is returned and the change is silently rejected — the user simply cannot type a letter or an extra digit.

**ID type ComboBox button cell fix:**  
```java
idTypeCombo.setButtonCell(new ListCell<>() {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : item);
        setStyle("-fx-text-fill:white;-fx-background-color:#16213e;");
    }
});
```
JavaFX ComboBoxes have two parts: the dropdown list (where items are shown when open) and the button cell (what is shown when the dropdown is closed and an item is selected). By default, the button cell ignores `-fx-text-fill` from CSS. This manual override creates a custom `ListCell` for the button cell and forces white text on dark background. Without this, the selected ID type would be invisible against the dark UI theme.

**Hardcoded CUSTOMER role:**  
```java
User user = new User(username, password, User.Role.CUSTOMER, name);
```
The role is always `CUSTOMER` during self-registration. There is no way to self-register as staff — this is a security control. Staff accounts must be created by the administrator through the database.

#### Most Important Lines

```java
if (change.getControlNewText().matches("\\d{0,11}")) return change;
return null;
```
The phone validation. Accepts the keystroke if it results in 0–11 digits. Rejects anything else.

```java
User user = new User(username, password, User.Role.CUSTOMER, name);
```
Always creates a CUSTOMER account. The role is not user-selectable.

```java
boolean success = userDAO.register(user);
```
The registration call. `UserDAO.register()` will hash the password before storing it. If the username is already taken (UNIQUE constraint in the database), it returns `false`.

---

### 7.3 StaffDashboardController.java

**Package:** `hotel.ui`  
**File:** `src/hotel/ui/StaffDashboardController.java`

#### What it does

`StaffDashboardController` is the largest controller in the system. It manages the staff dashboard, which contains four tabs:
- **Rooms tab** — view, add, edit, and delete rooms
- **Reservations tab** — view all reservations, check out guests, cancel reservations
- **Customers tab** — view all customers, edit customer profiles, view customer reservation history
- **Billing tab** — view all bills, mark bills as paid, generate PDF receipts

#### How it works

The controller implements `Initializable` and its `initialize()` method performs three things: sets up the welcome label, binds all table columns to their data fields (`setupColumns()`), and loads fresh data from all four DAOs (`refreshAll()`).

The `setupColumns()` method tells each `TableColumn` which property of the corresponding model to display. Some columns use `PropertyValueFactory` (for simple properties like `roomNumber`), while others use lambda expressions (for computed values like `getNights()` which are not JavaFX properties).

The `refreshAll()` method is called after every write operation (add, edit, delete, check-out, cancel, mark paid) to reload all four tables with fresh data from the database.

Dialogs are used for all write operations. Instead of separate screens, the staff dashboard shows pop-up dialog boxes (built programmatically using `Dialog<T>`) for adding/editing rooms and processing check-outs.

#### Crucial Parts

**The `setupColumns()` lambda pattern for computed fields:**  
```java
colResNights.setCellValueFactory(c ->
    new SimpleLongProperty(c.getValue().getNights()).asObject());
colResTotal.setCellValueFactory(c ->
    new SimpleDoubleProperty(c.getValue().getTotalAmount()).asObject());
```
`getNights()` and `getTotalAmount()` are computed methods, not JavaFX properties. `PropertyValueFactory` only works with JavaFX observable properties or JavaBeans-style getters with no arguments. For computed values, a lambda is required. `SimpleLongProperty(value).asObject()` wraps the primitive in a JavaFX observable wrapper that the table column can bind to.

**The `refreshAll()` pattern:**  
```java
private void refreshAll() {
    roomsTable.setItems(FXCollections.observableArrayList(roomDAO.getAllRooms()));
    reservationsTable.setItems(FXCollections.observableArrayList(reservationDAO.getAll()));
    customersTable.setItems(FXCollections.observableArrayList(userDAO.getAllCustomers()));
    billsTable.setItems(FXCollections.observableArrayList(billDAO.getAllBills()));
}
```
After any write operation, all four tables are refreshed with fresh database data. This ensures the UI always reflects the current state of the database, even if a check-out also changed the room status.

**The checkout dialog with transaction:**  
```java
dialog.showAndWait().ifPresent(vals -> {
    String method = methodCombo.getValue();
    boolean ok = reservationDAO.checkOut(sel.getId(), sel.getRoomId(),
                                          subtotal, vals[0], vals[1], method);
    if (ok) {
        alert("Success", "Check-out complete. Bill generated.");
        refreshAll();
    }
});
```
`showAndWait()` is a blocking call — it freezes the code here until the user clicks OK or Cancel. `ifPresent()` only runs the inner block if the user confirmed (did not cancel). The result passes the discount and tax values to `reservationDAO.checkOut()`, which executes a 3-statement transaction.

**White label placeholders for dark-background tables:**  
```java
Label phRooms = new Label("No records found");
phRooms.setStyle("-fx-text-fill:white;");
roomsTable.setPlaceholder(phRooms);
```
By default, the "No content in table" text is dark-colored, making it invisible on dark-themed `TableView` components. Setting a custom `Label` with white text solves this for all five tables in the dashboard.

**Customer history listener:**  
```java
customersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
    if (sel != null) loadCustomerHistory(sel);
});
```
This is a change listener that fires whenever the selection in the customers table changes. When a customer row is clicked, `loadCustomerHistory(sel)` is called automatically, populating the customer history table below with that customer's reservations. This is the binding/listener pattern in JavaFX.

#### Most Important Lines

```java
colResNights.setCellValueFactory(c ->
    new SimpleLongProperty(c.getValue().getNights()).asObject());
```
Computed value binding for a table column. `getNights()` is not a JavaFX property, so it needs this wrapper to work with `TableColumn`.

```java
reservationDAO.checkOut(sel.getId(), sel.getRoomId(), subtotal, vals[0], vals[1], method);
```
The check-out call. This single method call triggers a 3-part database transaction: update reservation status, free the room, and insert the bill.

```java
customersTable.getSelectionModel().selectedItemProperty().addListener(...)
```
A reactive listener. When the user clicks a customer row, this automatically loads that customer's history in the lower table without requiring a separate button click.

---

### 7.4 CustomerDashboardController.java

**Package:** `hotel.ui`  
**File:** `src/hotel/ui/CustomerDashboardController.java`

#### What it does

`CustomerDashboardController` manages the customer dashboard, which has two tabs:
- **My Reservations tab** — shows all reservations for the logged-in customer, with an option to view and download the bill for any reservation
- **My Profile tab** — allows the customer to update their personal information, ID details, and optionally change their password

The customer sees only their own data — no other customer's reservations or profile information is accessible.

#### How it works

The controller implements `Initializable`. The `initialize()` method loads the current user from `Session`, sets the welcome label, populates the ID type dropdown, applies the phone number `TextFormatter`, sets up the reservations table columns, loads the customer's reservations, and sets the white placeholder label for the table.

`loadProfile()` fetches the latest version of the user from the database (via `userDAO.getById()`) to ensure the form always shows the current stored values, not potentially stale `Session` data.

`handleUpdateProfile()` reads all the form fields and calls `userDAO.update()`. Then, if the password fields are not empty, it performs a separate password change after validating that both fields match and the new password is at least 6 characters.

#### Crucial Parts

**Session-scoped data loading:**  
```java
User me = Session.getCurrentUser();
welcomeLabel.setText("Welcome, " + me.getFullName());
```
The customer only sees their own data. `Session.getCurrentUser()` returns the logged-in user. Every data query uses this user's ID as a filter.

**Profile refresh from database:**  
```java
private void loadProfile() {
    User me = userDAO.getById(Session.getCurrentUser().getId());
    if (me != null) {
        Session.setCurrentUser(me);
        nameField.setText(nvl(me.getFullName()));
        ...
    }
}
```
The profile form is populated from a fresh database read, not from the `Session` object. This handles the case where a staff member edited this customer's profile in a different screen — the customer's form always shows the true current database values. The `Session` is also updated so it stays in sync.

**Separated password change logic:**  
```java
String newPw = newPasswordField.getText();
if (!newPw.isEmpty()) {
    if (newPw.length() < 6) { ... return; }
    if (!newPw.equals(confirmPasswordField.getText())) { ... return; }
    userDAO.changePassword(me.getId(), newPw);
}
```
Password change is only triggered if the new password field is not empty. This lets a customer save their profile without changing their password (the default case). The `changePassword()` method in `UserDAO` hashes the new password before storing it.

**Bill viewing by reservation lookup:**  
```java
Bill bill = billDAO.getByReservationId(sel.getId());
if (bill == null) {
    // reservation not yet checked out
}
BillReport.print(bill);
```
The customer selects a reservation and clicks "View Bill." The system looks up the bill using the reservation ID. If no bill exists (the reservation is still `ACTIVE`), an informational message is shown instead of crashing.

#### Most Important Lines

```java
User me = Session.getCurrentUser();
```
Reads the globally stored logged-in user. The entire customer dashboard is scoped to this person's data.

```java
reservationDAO.getByCustomer(Session.getCurrentUser().getId())
```
Loads only the logged-in customer's reservations. The SQL query filters by `customer_id`, so a customer never sees another customer's data.

```java
phoneField.setTextFormatter(new TextFormatter<>(change -> {
    if (change.getControlNewText().matches("\\d{0,11}")) return change;
    return null;
}));
```
Same phone validation as in `RegisterController`. Digits only, maximum 11 characters.

---

### 7.5 NewReservationController.java

**Package:** `hotel.ui`  
**File:** `src/hotel/ui/NewReservationController.java`

#### What it does

`NewReservationController` manages the room booking screen available to staff. Staff select a customer, enter check-in and check-out dates, and click "Search Available Rooms." The system queries the database for rooms not already booked during that period and displays them in a list. Staff then select a room from the list, review a live-updating summary, and click "Confirm Reservation" to book it.

#### How it works

`NewReservationController` implements `Initializable`. The `initialize()` method loads all customers into the `customerCombo` dropdown, sets default dates (today and tomorrow), and attaches change listeners to the room list and the two date pickers.

The change listeners enable the live summary: whenever the user selects a different room or changes either date, the summary box automatically recalculates and updates (showing room name, type, number of nights, and total cost). This provides instant feedback without requiring a button click.

The `handleSearch()` method validates the dates, then calls `roomDAO.getAvailableRooms()` which runs the NOT IN subquery. The results populate the room `ListView`.

The `handleConfirm()` method validates that a customer and room are selected, constructs a `Reservation` object, and calls `reservationDAO.create()`, which runs a 2-statement transaction (insert reservation + mark room occupied).

#### Crucial Parts

**Date validation before availability search:**  
```java
if (!out.isAfter(in)) {
    showMessage("Check-out must be after check-in.", false);
    return;
}
if (in.isBefore(LocalDate.now())) {
    showMessage("Check-in date cannot be in the past.", false);
    return;
}
```
These checks prevent nonsensical searches. A check-out on the same day as check-in would produce zero nights. A check-in in the past is logically invalid. Both are caught before hitting the database.

**Live summary update listeners:**  
```java
roomList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
    if (sel != null) updateSummary(sel);
});
checkInPicker.valueProperty().addListener((obs, o, n) -> refreshSummaryIfSelected());
checkOutPicker.valueProperty().addListener((obs, o, n) -> refreshSummaryIfSelected());
```
Three listeners attached to three different controls. Any time a room is selected or a date changes, the summary recalculates. The `obs` parameter is the observable property, `old` is the previous value, and `sel`/`n` is the new value. The lambda is called automatically by JavaFX's property binding system.

**The `updateSummary()` method:**  
```java
private void updateSummary(Room room) {
    long   nights = ChronoUnit.DAYS.between(in, out);
    double total  = nights * room.getPrice();
    summaryRoom.setText("Room " + room.getRoomNumber() + " — " + room.getRoomType());
    summaryNights.setText(nights + " night(s) × ₱" + String.format("%.2f", room.getPrice()) + "/night");
    summaryTotal.setText("Total: ₱" + String.format("%.2f", total));
    summaryBox.setVisible(true);
}
```
Displays a formatted price summary without any database calls. The room price comes from the `Room` object already retrieved during the availability search. The night count is computed from the date pickers.

**Reservation creation:**  
```java
Reservation res = new Reservation();
res.setCustomerId(customer.getId());
res.setRoomId(room.getId());
res.setRoomPrice(room.getPrice());
res.setCheckIn(in);
res.setCheckOut(out);
res.setGuests(guestSpinner.getValue());
res.setNotes(notesArea.getText().trim());

if (reservationDAO.create(res)) {
    MainApp.showScreen("StaffDashboard");
}
```
A `Reservation` object is assembled from the UI fields and passed to the DAO. The DAO takes care of the SQL INSERT and the room status update in one transaction. On success, the controller returns to the staff dashboard and does nothing else — the dashboard's `refreshAll()` (called when it loads) will show the new reservation.

#### Most Important Lines

```java
List<Room> available = roomDAO.getAvailableRooms(in.toString(), out.toString());
```
The availability search. The dates are converted to strings (ISO format `"YYYY-MM-DD"`) which SQLite stores and compares as strings.

```java
long nights = ChronoUnit.DAYS.between(in, out);
```
Exact night count between two `LocalDate` objects. Used in the live summary display.

```java
if (reservationDAO.create(res)) {
    MainApp.showScreen("StaffDashboard");
}
```
After a successful reservation creation, the system navigates back to the staff dashboard. The `StaffDashboard` screen reloads its data on initialization, so the new reservation appears automatically.

---

## 8. Reports Layer

---

### 8.1 BillReport.java

**Package:** `hotel.reports`  
**File:** `src/hotel/reports/BillReport.java`

#### What it does

`BillReport` generates a professional PDF receipt for a bill using the JasperReports library (version 6.21.0). When called, it produces a formatted A4 PDF file containing the hotel name ("SIXEVEN HOTEL"), an "OFFICIAL RECEIPT" heading, reservation details, billing breakdown (subtotal, discount, tax, grand total), and payment status. The PDF is saved to a `receipts/` folder in the project directory and automatically opened in the system's default PDF viewer.

The receipt is intentionally minimal — it does not include a tagline below the hotel name or a closing thank-you message at the bottom. Only factual billing information is shown.

#### How it works

The `print()` method follows a 6-step pipeline:

1. **Create the output folder** — `receipts/` directory is created if it does not exist
2. **Generate JRXML** — a JasperReports XML template is built as a Java string and written to a file
3. **Compile the JRXML** — JasperReports compiles the XML template into a `JasperReport` object
4. **Build parameters** — all bill data is placed into a `HashMap<String, Object>` as named parameters
5. **Fill the report** — `JasperFillManager.fillReport()` merges the parameters into the compiled report using a `JREmptyDataSource` (because all data comes from parameters, not a database query)
6. **Export to PDF** — `JRPdfExporter` writes the final PDF to disk
7. **Open the PDF** — `Desktop.getDesktop().open()` opens the file in the OS's default PDF viewer

The design choice of using `JREmptyDataSource` is significant. JasperReports is normally used with database queries or data sources to populate repeating rows. In this system, a bill has no repeating rows — it is a single document with fixed fields. All data is passed as parameters, so an empty data source is used to satisfy JasperReports' requirement of a datasource.

#### Crucial Parts

**The JRXML generation approach:**  
```java
String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
             "<jasperReport ...>\n" +
             "    <parameter name=\"BILL_ID\" class=\"java.lang.String\"/>\n" +
             ...
             "</jasperReport>";
try (FileWriter fw = new FileWriter(outputPath)) {
    fw.write(xml);
}
```
The JRXML template is written as a Java string and saved to a file. This approach avoids the need to ship a separate template file with the project. Every time a bill is printed, the template is regenerated fresh. This is both a design choice (self-contained) and a limitation (the template cannot be edited without recompiling).

**The parameters map:**  
```java
Map<String, Object> params = new HashMap<>();
params.put("BILL_ID",     String.valueOf(bill.getId()));
params.put("GRAND_TOTAL", String.format("%.2f", bill.getGrandTotal()));
params.put("PAYMENT_STATUS", bill.isPaid() ? "PAID" : "UNPAID");
...
```
All 16 bill data points are placed into a map with string keys that match the `<parameter name="...">` declarations in the JRXML. The values are all formatted as strings before being added to the map.

**The `JREmptyDataSource` usage:**  
```java
JasperPrint jasperPrint = JasperFillManager.fillReport(
    jasperReport, params, new JREmptyDataSource());
```
`JREmptyDataSource` provides a single empty row to JasperReports to satisfy its requirement of at least one row. The `whenNoDataType="AllSectionsNoDetail"` attribute in the JRXML tells JasperReports to render the report even when the data source has no data rows, preventing a blank PDF.

**Auto-open behavior:**  
```java
File pdf = new File(pdfPath);
if (Desktop.isDesktopSupported()) {
    Desktop.getDesktop().open(pdf);
}
```
`Desktop.isDesktopSupported()` checks whether the current platform supports the `Desktop` API (it does on Windows). If so, `open()` hands the PDF file to the operating system, which opens it in the default PDF viewer (usually Adobe Acrobat, Edge, or Chrome on Windows).

**The file naming convention:**  
```java
String pdfPath = folder + File.separator + "Bill_" + bill.getId() + ".pdf";
```
Each bill gets its own PDF file named `Bill_{id}.pdf`. Bill #1 produces `receipts/Bill_1.pdf`, Bill #3 produces `receipts/Bill_3.pdf`, etc. This means printing the same bill multiple times overwrites the previous PDF file for that bill.

#### Most Important Lines

```java
JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlPath);
```
Compiles the XML template into a binary `JasperReport` object. This step validates the template structure.

```java
JasperPrint jasperPrint = JasperFillManager.fillReport(
    jasperReport, params, new JREmptyDataSource());
```
Merges the parameters into the compiled template. This produces a `JasperPrint` object containing the actual rendered pages.

```java
JRPdfExporter exporter = new JRPdfExporter();
exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfPath));
exporter.exportReport();
```
The export step. The `JRPdfExporter` takes the filled report and writes it as a PDF to the specified file path.

```java
params.put("GRAND_TOTAL", String.format("%.2f", bill.getGrandTotal()));
```
The grand total uses the computed `getGrandTotal()` method (subtotal − discount + tax), formatted to two decimal places. This is the final amount shown in large bold text on the receipt.

---

## 9. OOP Concepts Summary

The following table shows where each required OOP concept is implemented in the system.

| OOP Concept | Where It Is Used |
|---|---|
| **Inheritance** | `Room`, `User`, `Reservation`, `Bill` all extend `BaseEntity` — inheriting the `id` field and `getId()`/`setId()` without redeclaring them |
| **Abstract Class** | `BaseEntity` is declared `abstract` — it cannot be instantiated, only extended. It declares `getSummary()` as abstract, forcing all subclasses to implement it |
| **Polymorphism** | Each model class (`Room`, `User`, `Reservation`, `Bill`) overrides `getSummary()` with its own specific implementation. The same method name produces four different outputs depending on the object type |
| **Dynamic Binding** | `BaseEntity.toString()` calls `getSummary()`. When called on a `Room` object via a `BaseEntity` reference, the JVM automatically selects `Room.getSummary()` at runtime — not a generic version |
| **Encapsulation** | All model fields are `private` with public getters/setters. DAO `mapRow()` methods are `private`. `DatabaseManager.connection` is private and only accessible through `getConnection()`. `Session.currentUser` is private |
| **Interface** | `Dao<T extends BaseEntity>` is a generic interface implemented by all four DAO classes, enforcing the CRUD contract |
| **User-Defined Types (Enums)** | `Room.Status` (AVAILABLE, OCCUPIED, MAINTENANCE), `User.Role` (STAFF, CUSTOMER), `Reservation.Status` (ACTIVE, CHECKED_OUT, CANCELLED) |
| **Generics** | `Dao<T extends BaseEntity>` — a bounded generic interface. `T` is resolved to `Room`, `User`, `Reservation`, or `Bill` at compile time |

---

## 10. How the Layers Connect

The following describes the full data flow for one complete operation: a staff member checking out a guest.

```
1. Staff opens the Reservations tab in StaffDashboardController
   └── StaffDashboardController.initialize()
       └── ReservationDAO.getAll()
           └── DatabaseManager.getConnection()
               └── SQL: SELECT r.*, u.full_name, rm.room_number ... FROM reservations JOIN users JOIN rooms
           └── mapRow() → List<Reservation>
       └── reservationsTable.setItems(observableList)

2. Staff selects a reservation row and clicks "Check Out"
   └── StaffDashboardController.handleCheckOut()
       └── Dialog shown: enter discount, tax, payment method
       └── dialog.showAndWait() → returns double[] {discount, tax}

3. Staff confirms the checkout
   └── ReservationDAO.checkOut(reservationId, roomId, subtotal, discount, tax, method)
       └── DatabaseManager.getConnection().setAutoCommit(false)
       └── SQL: UPDATE reservations SET status='CHECKED_OUT' WHERE id=?
       └── SQL: UPDATE rooms SET status='AVAILABLE' WHERE id=?
       └── SQL: INSERT OR IGNORE INTO bills (...) VALUES (...)
       └── conn.commit()

4. StaffDashboardController.refreshAll() is called
   └── All four tables reload from database

5. Staff clicks "Generate Bill" on the Billing tab
   └── StaffDashboardController.handleGenerateBill()
       └── BillReport.print(bill)
           └── generateJrxml() → writes XML template to receipts/Bill_N.jrxml
           └── JasperCompileManager.compileReport(jrxmlPath)
           └── JasperFillManager.fillReport(report, params, emptySource)
           └── JRPdfExporter.exportReport() → receipts/Bill_N.pdf
           └── Desktop.getDesktop().open(pdf) → opens in system PDF viewer
```

This flow demonstrates all layers working together: the **UI layer** (controller) collects input, the **DAO layer** executes SQL with transactions, the **database layer** (SQLite via `DatabaseManager`) persists the changes, the **model layer** carries data between layers, and the **reports layer** consumes the final model data to produce a PDF.
