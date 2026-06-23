package hotel.dao;

import hotel.model.Room;
import hotel.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Implements Dao<Room> — polymorphism via interface, Room-specific behavior defined here
// Abstraction: controllers call these methods without knowing any SQL
public class RoomDAO implements Dao<Room> {

    // Interface method — delegates to getAllRooms for compatibility with existing callers
    @Override
    public List<Room> getAll() { return getAllRooms(); }

    // Interface method — fetches a single room by primary key
    @Override
    public Room getById(int id) {
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement("SELECT * FROM rooms WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("GetRoomById error: " + e.getMessage());
        }
        return null;
    }

    // Interface method — maps to addRoom for the room creation flow
    @Override
    public boolean save(Room room) { return addRoom(room); }

    // Interface method — removes a room record by id
    @Override
    public boolean delete(int id) { return deleteRoom(id); }

    public List<Room> getAllRooms() {
        List<Room> list = new ArrayList<>();
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY room_number")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("GetAllRooms error: " + e.getMessage());
        }
        return list;
    }

    // NOT IN subquery against indexed columns — excludes rooms already booked in the window
    public List<Room> getAvailableRooms(String checkIn, String checkOut) {
        List<Room> list = new ArrayList<>();
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
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, checkOut);
            ps.setString(2, checkIn);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("GetAvailableRooms error: " + e.getMessage());
        }
        return list;
    }

    public boolean addRoom(Room room) {
        String sql = "INSERT INTO rooms (room_number,room_type,price,capacity,description) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setDouble(3, room.getPrice());
            ps.setInt(4, room.getCapacity());
            ps.setString(5, room.getDescription());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("AddRoom error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRoom(Room room) {
        String sql = "UPDATE rooms SET room_number=?,room_type=?,price=?,capacity=?,description=?,status=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setDouble(3, room.getPrice());
            ps.setInt(4, room.getCapacity());
            ps.setString(5, room.getDescription());
            ps.setString(6, room.getStatus().name());
            ps.setInt(7, room.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UpdateRoom error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRoom(int roomId) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM rooms WHERE id=?")) {
            ps.setInt(1, roomId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DeleteRoom error: " + e.getMessage());
            return false;
        }
    }

    // Encapsulation: DB column mapping is private so callers never touch raw ResultSet
    private Room mapRow(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setId(rs.getInt("id"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setRoomType(rs.getString("room_type"));
        r.setPrice(rs.getDouble("price"));
        r.setCapacity(rs.getInt("capacity"));
        r.setDescription(rs.getString("description"));
        r.setStatus(Room.Status.valueOf(rs.getString("status")));
        return r;
    }
}
