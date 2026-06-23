package hotel.dao;

import hotel.model.Reservation;
import hotel.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Implements Dao<Reservation> — polymorphism via interface, Reservation-specific behavior here
// Abstraction: transaction management and multi-table operations stay hidden from controllers
public class ReservationDAO implements Dao<Reservation> {

    // Reusable JOIN fragment — joins users and rooms to populate display fields
    private static final String JOIN_SQL =
        "SELECT r.*, u.full_name, rm.room_number, rm.room_type, rm.price " +
        "FROM reservations r " +
        "JOIN users u  ON r.customer_id = u.id " +
        "JOIN rooms rm ON r.room_id = rm.id ";

    // Interface method — retrieves all reservations newest first
    @Override
    public List<Reservation> getAll() {
        return query(JOIN_SQL + "ORDER BY r.id DESC", null);
    }

    // Interface method — finds one reservation by primary key
    @Override
    public Reservation getById(int id) {
        List<Reservation> result = query(JOIN_SQL + "WHERE r.id=?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    // Interface method — maps to create for the booking confirmation flow
    @Override
    public boolean save(Reservation res) { return create(res); }

    // Interface method — reservations are cancelled, not hard-deleted
    @Override
    public boolean delete(int id) { return false; }

    // Creates reservation and marks room OCCUPIED atomically in one transaction
    public boolean create(Reservation res) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO reservations (customer_id,room_id,check_in,check_out,guests,notes) VALUES (?,?,?,?,?,?)")) {
                ps.setInt(1, res.getCustomerId());
                ps.setInt(2, res.getRoomId());
                ps.setString(3, res.getCheckIn().toString());
                ps.setString(4, res.getCheckOut().toString());
                ps.setInt(5, res.getGuests());
                ps.setString(6, res.getNotes());
                ps.executeUpdate();
            }

            // Room status update is part of the same transaction to prevent double-booking
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE rooms SET status='OCCUPIED' WHERE id=?")) {
                ps.setInt(1, res.getRoomId());
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Create reservation error: " + e.getMessage());
            tryRollback(conn);
            return false;
        } finally {
            trySetAutoCommit(conn);
        }
    }

    public List<Reservation> getByCustomer(int customerId) {
        return query(JOIN_SQL + "WHERE r.customer_id=? ORDER BY r.id DESC", customerId);
    }

    // Marks CHECKED_OUT, frees the room, and inserts the bill — all in one transaction
    public boolean checkOut(int reservationId, int roomId, double subtotal,
                             double discount, double tax, String paymentMethod) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reservations SET status='CHECKED_OUT' WHERE id=?")) {
                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE rooms SET status='AVAILABLE' WHERE id=?")) {
                ps.setInt(1, roomId);
                ps.executeUpdate();
            }

            double grandTotal = subtotal - discount + tax;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO bills (reservation_id,total_amount,discount,tax,payment_method) VALUES (?,?,?,?,?)")) {
                ps.setInt(1, reservationId);
                ps.setDouble(2, grandTotal);
                ps.setDouble(3, discount);
                ps.setDouble(4, tax);
                ps.setString(5, paymentMethod);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("CheckOut error: " + e.getMessage());
            tryRollback(conn);
            return false;
        } finally {
            trySetAutoCommit(conn);
        }
    }

    // Marks CANCELLED and frees the room in a single atomic transaction
    public boolean cancel(int reservationId, int roomId) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reservations SET status='CANCELLED' WHERE id=?")) {
                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE rooms SET status='AVAILABLE' WHERE id=?")) {
                ps.setInt(1, roomId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Cancel error: " + e.getMessage());
            tryRollback(conn);
            return false;
        } finally {
            trySetAutoCommit(conn);
        }
    }

    private List<Reservation> query(String sql, Integer param) {
        List<Reservation> list = new ArrayList<>();
        try {
            PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql);
            if (param != null) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Query reservations error: " + e.getMessage());
        }
        return list;
    }

    // Encapsulation: ResultSet mapping is private so callers receive clean objects
    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setCustomerId(rs.getInt("customer_id"));
        r.setRoomId(rs.getInt("room_id"));
        r.setCheckIn(LocalDate.parse(rs.getString("check_in")));
        r.setCheckOut(LocalDate.parse(rs.getString("check_out")));
        r.setGuests(rs.getInt("guests"));
        r.setNotes(rs.getString("notes"));
        r.setStatus(Reservation.Status.valueOf(rs.getString("status")));
        r.setCreatedAt(rs.getString("created_at"));
        r.setCustomerName(rs.getString("full_name"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setRoomType(rs.getString("room_type"));
        r.setRoomPrice(rs.getDouble("price"));
        return r;
    }

    private void tryRollback(Connection c) {
        try { if (c != null) c.rollback(); } catch (SQLException ignored) {}
    }

    private void trySetAutoCommit(Connection c) {
        try { if (c != null) c.setAutoCommit(true); } catch (SQLException ignored) {}
    }
}
