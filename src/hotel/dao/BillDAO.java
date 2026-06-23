package hotel.dao;

import hotel.model.Bill;
import hotel.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Implements Dao<Bill> — polymorphism via interface, Bill-specific behavior defined here
// Abstraction: the 4-table JOIN and column mapping are hidden from UI controllers
public class BillDAO implements Dao<Bill> {

    // Single JOIN query assembles complete bill data from bills, reservations, users, rooms
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

    // Interface method — returns all bills newest first
    @Override
    public List<Bill> getAll() { return getAllBills(); }

    // Interface method — finds a bill by its own primary key
    @Override
    public Bill getById(int id) {
        String sql = JOIN_SQL + "WHERE b.id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("GetBillById error: " + e.getMessage());
        }
        return null;
    }

    // Interface method — bills are created during checkout, not standalone
    @Override
    public boolean save(Bill bill) { return false; }

    // Interface method — bills are permanent audit records, not deleted
    @Override
    public boolean delete(int id) { return false; }

    public List<Bill> getAllBills() {
        List<Bill> list = new ArrayList<>();
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(JOIN_SQL + "ORDER BY b.id DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("GetAllBills error: " + e.getMessage());
        }
        return list;
    }

    // Used by the customer view to find the bill for a selected reservation
    public Bill getByReservationId(int reservationId) {
        String sql = JOIN_SQL + "WHERE b.reservation_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("GetBillByReservation error: " + e.getMessage());
        }
        return null;
    }

    public boolean markPaid(int billId, String paymentMethod) {
        String sql = "UPDATE bills SET paid=1, payment_method=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, paymentMethod);
            ps.setInt(2, billId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("MarkPaid error: " + e.getMessage());
            return false;
        }
    }

    // Encapsulation: ResultSet mapping is private so callers receive clean Bill objects
    private Bill mapRow(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getInt("id"));
        b.setReservationId(rs.getInt("reservation_id"));
        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setDiscount(rs.getDouble("discount"));
        b.setTax(rs.getDouble("tax"));
        b.setPaid(rs.getInt("paid") == 1);
        b.setPaymentMethod(rs.getString("payment_method"));
        b.setIssuedAt(rs.getString("issued_at"));
        b.setCustomerName(rs.getString("full_name"));
        b.setRoomNumber(rs.getString("room_number"));
        b.setRoomType(rs.getString("room_type"));
        b.setCheckIn(rs.getString("check_in"));
        b.setCheckOut(rs.getString("check_out"));
        b.setRoomPrice(rs.getDouble("room_price"));
        b.setNights(rs.getLong("nights"));
        return b;
    }
}
