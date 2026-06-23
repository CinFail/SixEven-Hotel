package hotel.dao;

import hotel.model.BaseEntity;
import java.util.List;

// Generic DAO interface — defines a contract all data access classes must fulfill
// Polymorphism: RoomDAO, UserDAO, ReservationDAO, and BillDAO each implement
// these methods differently while sharing the same interface type
// T must be a BaseEntity subtype so only model objects are accepted
public interface Dao<T extends BaseEntity> {
    List<T> getAll();       // retrieve all records
    T getById(int id);      // retrieve one by primary key
    boolean save(T entity); // insert or create
    boolean delete(int id); // remove by primary key
}
