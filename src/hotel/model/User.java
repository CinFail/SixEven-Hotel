package hotel.model;

// Inheritance: extends BaseEntity to reuse the id field and getSummary contract
// Encapsulation: private fields enforce access through getters and setters
// User-defined type: Role enum limits values to STAFF and CUSTOMER
public class User extends BaseEntity {

    public enum Role { STAFF, CUSTOMER }

    private String username;
    private String password;
    private Role   role;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String idType;
    private String idNumber;
    private String createdAt;

    public User() {}

    public User(String username, String password, Role role, String fullName) {
        this.username = username;
        this.password = password;
        this.role     = role;
        this.fullName = fullName;
    }

    // Polymorphism: each entity subclass returns a different getSummary() description
    // Dynamic binding: BaseEntity.toString() calls this at runtime via the overridden method
    @Override
    public String getSummary() { return fullName + " (@" + username + ")"; }

    public String getUsername()            { return username; }
    public void   setUsername(String u)    { this.username = u; }

    public String getPassword()            { return password; }
    public void   setPassword(String p)    { this.password = p; }

    public Role   getRole()                { return role; }
    public void   setRole(Role r)          { this.role = r; }

    public String getFullName()            { return fullName; }
    public void   setFullName(String n)    { this.fullName = n; }

    public String getEmail()               { return email; }
    public void   setEmail(String e)       { this.email = e; }

    public String getPhone()               { return phone; }
    public void   setPhone(String p)       { this.phone = p; }

    public String getAddress()             { return address; }
    public void   setAddress(String a)     { this.address = a; }

    public String getIdType()              { return idType; }
    public void   setIdType(String t)      { this.idType = t; }

    public String getIdNumber()            { return idNumber; }
    public void   setIdNumber(String n)    { this.idNumber = n; }

    public String getCreatedAt()           { return createdAt; }
    public void   setCreatedAt(String c)   { this.createdAt = c; }
}
