package hotel.dao;

import hotel.model.User;
import hotel.util.DatabaseManager;
import hotel.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Implements Dao<User> — polymorphism via interface, User-specific behavior defined here
// Abstraction: login, register, and profile operations are hidden from UI controllers
public class UserDAO implements Dao<User> {

    // Interface method — returns all customer accounts ordered by name
    @Override
    public List<User> getAll() { return getAllCustomers(); }

    // Interface method — finds any user (staff or customer) by primary key
    @Override
    public User getById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("GetById error: " + e.getMessage());
        }
        return null;
    }

    // Interface method — maps to register for the self-registration flow
    @Override
    public boolean save(User user) { return register(user); }

    // Interface method — user accounts are not hard-deleted in this system
    @Override
    public boolean delete(int id) { return false; }

    // Verifies credentials using SHA-256; falls back to migrate plain-text legacy passwords
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("password");
                User user = mapRow(rs);
                if (PasswordUtil.verify(password, stored)) {
                    return user;
                }
                // Fallback: plain-text match from old DB — auto-migrate to hash
                if (password.equals(stored)) {
                    System.out.println("[Auth] Migrating plain-text password to hash for: " + username);
                    changePassword(user.getId(), password);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return null;
    }

    // Stores password as SHA-256 hash — plain text is never persisted
    public boolean register(User user) {
        String sql = "INSERT INTO users (username,password,role,full_name,email,phone,address,id_type,id_number) "
                   + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, PasswordUtil.hash(user.getPassword()));
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getPhone());
            ps.setString(7, user.getAddress());
            ps.setString(8, user.getIdType());
            ps.setString(9, user.getIdNumber());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Register error: " + e.getMessage());
            return false;
        }
    }

    public List<User> getAllCustomers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role='CUSTOMER' ORDER BY full_name";
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("GetAllCustomers error: " + e.getMessage());
        }
        return list;
    }

    // Updates profile fields only — password change is handled separately for safety
    public boolean update(User user) {
        String sql = "UPDATE users SET full_name=?,email=?,phone=?,address=?,id_type=?,id_number=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getAddress());
            ps.setString(5, user.getIdType());
            ps.setString(6, user.getIdNumber());
            ps.setInt(7, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update user error: " + e.getMessage());
            return false;
        }
    }

    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ChangePassword error: " + e.getMessage());
            return false;
        }
    }

    // Encapsulation: DB column mapping is private so callers never touch raw ResultSet
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(User.Role.valueOf(rs.getString("role")));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setIdType(rs.getString("id_type"));
        u.setIdNumber(rs.getString("id_number"));
        u.setCreatedAt(rs.getString("created_at"));
        return u;
    }
}
