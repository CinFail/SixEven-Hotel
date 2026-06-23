package hotel.util;

import java.sql.*;

// Encapsulation: the single SQLite connection is private; all access goes through getConnection()
// Utility class — private constructor prevents instantiation
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:hotel.db";
    private static Connection connection;

    private DatabaseManager() {}

    // Returns the shared connection, creating it if closed or null
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            try (Statement s = connection.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL");      // WAL allows concurrent reads during writes
                s.execute("PRAGMA foreign_keys=ON");       // enforce FK constraints
                s.execute("PRAGMA synchronous=NORMAL");    // balance safety and speed
                s.execute("PRAGMA cache_size=10000");      // keep frequently used pages in memory
            }
            connection.setAutoCommit(true);
        }
        return connection;
    }

    public static void initializeDatabase() {
        try (Statement stmt = getConnection().createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    username   TEXT NOT NULL UNIQUE,
                    password   TEXT NOT NULL,
                    role       TEXT NOT NULL CHECK(role IN ('STAFF','CUSTOMER')),
                    full_name  TEXT NOT NULL,
                    email      TEXT,
                    phone      TEXT,
                    address    TEXT,
                    id_type    TEXT,
                    id_number  TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    room_number TEXT NOT NULL UNIQUE,
                    room_type   TEXT NOT NULL,
                    price       REAL NOT NULL,
                    capacity    INTEGER NOT NULL DEFAULT 1,
                    description TEXT,
                    status      TEXT NOT NULL DEFAULT 'AVAILABLE'
                                CHECK(status IN ('AVAILABLE','OCCUPIED','MAINTENANCE'))
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    room_id     INTEGER NOT NULL,
                    check_in    TEXT NOT NULL,
                    check_out   TEXT NOT NULL,
                    guests      INTEGER NOT NULL DEFAULT 1,
                    notes       TEXT,
                    status      TEXT NOT NULL DEFAULT 'ACTIVE'
                                CHECK(status IN ('ACTIVE','CHECKED_OUT','CANCELLED')),
                    created_at  TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                    FOREIGN KEY (customer_id) REFERENCES users(id),
                    FOREIGN KEY (room_id)     REFERENCES rooms(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bills (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    reservation_id INTEGER NOT NULL UNIQUE,
                    total_amount   REAL NOT NULL,
                    discount       REAL NOT NULL DEFAULT 0,
                    tax            REAL NOT NULL DEFAULT 0,
                    paid           INTEGER NOT NULL DEFAULT 0,
                    payment_method TEXT,
                    issued_at      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                    FOREIGN KEY (reservation_id) REFERENCES reservations(id)
                )
            """);

            // Indexes speed up the availability NOT IN subquery and customer history queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_res_room   ON reservations(room_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_res_cust   ON reservations(customer_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_res_status ON reservations(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_res_dates  ON reservations(check_in,check_out)");

            seedDefaultData(stmt);
            System.out.println("[DB] Database ready (WAL mode).");

        } catch (SQLException e) {
            System.err.println("[DB] Init error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedDefaultData(Statement stmt) throws SQLException {
        // Default staff account: admin / admin123
        String adminHash = PasswordUtil.hash("admin123");
        stmt.execute("INSERT OR IGNORE INTO users (username,password,role,full_name,email) "
                   + "VALUES ('admin','" + adminHash + "','STAFF','Administrator','admin@hotel.com')");

        // Sample rooms inserted only if they don't already exist
        String[][] rooms = {
            {"101","Standard Single",  "1200.00","1","Cozy single room with garden view"},
            {"102","Standard Single",  "1200.00","1","Cozy single room with street view"},
            {"201","Standard Double",  "1800.00","2","Spacious double room, queen bed"},
            {"202","Standard Double",  "1800.00","2","Spacious double room, twin beds"},
            {"301","Deluxe Room",      "2500.00","2","Deluxe room with city view, king bed"},
            {"302","Deluxe Room",      "2500.00","2","Deluxe room with pool view, king bed"},
            {"401","Junior Suite",     "3500.00","3","Junior suite with living area"},
            {"501","Presidential Suite","6000.00","4","Top-floor suite with panoramic view"},
        };
        for (String[] r : rooms) {
            stmt.execute("INSERT OR IGNORE INTO rooms (room_number,room_type,price,capacity,description) "
                       + "VALUES ('" + r[0] + "','" + r[1] + "'," + r[2] + "," + r[3] + ",'" + r[4] + "')");
        }
    }

    public static synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Close error: " + e.getMessage());
        }
    }
}
