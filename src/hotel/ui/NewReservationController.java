package hotel.ui;

import hotel.MainApp;
import hotel.dao.ReservationDAO;
import hotel.dao.RoomDAO;
import hotel.dao.UserDAO;
import hotel.model.Reservation;
import hotel.model.Room;
import hotel.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

// Handles room search and reservation booking for staff
// Implements Initializable: sets up date pickers and binds summary update listeners
// Message passing: on confirm, creates a Reservation object and passes it to ReservationDAO
public class NewReservationController implements Initializable {

    @FXML private ComboBox<User>   customerCombo;
    @FXML private DatePicker       checkInPicker;
    @FXML private DatePicker       checkOutPicker;
    @FXML private Spinner<Integer> guestSpinner;
    @FXML private TextArea         notesArea;
    @FXML private ListView<Room>   roomList;
    @FXML private javafx.scene.layout.VBox summaryBox;
    @FXML private Label            summaryRoom;
    @FXML private Label            summaryNights;
    @FXML private Label            summaryTotal;
    @FXML private Label            messageLabel;

    private final UserDAO        userDAO        = new UserDAO();
    private final RoomDAO        roomDAO        = new RoomDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        customerCombo.setItems(FXCollections.observableArrayList(userDAO.getAllCustomers()));
        customerCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setStyle("-fx-text-fill:white;-fx-background-color:#1a1a2e;");
            }
        });
        customerCombo.setButtonCell(new ListCell<User>() {
            @Override protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setStyle("-fx-text-fill:white;-fx-background-color:#1a1a2e;");
            }
        });
        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));

        // Live summary update on room selection
        roomList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) updateSummary(sel);
        });

        // Re-run summary when dates change while a room is selected
        checkInPicker.valueProperty().addListener((obs, o, n) -> refreshSummaryIfSelected());
        checkOutPicker.valueProperty().addListener((obs, o, n) -> refreshSummaryIfSelected());
    }

    @FXML
    private void handleSearch() {
        LocalDate in  = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (in == null || out == null) {
            showMessage("Please select both dates.", false);
            return;
        }
        if (!out.isAfter(in)) {
            showMessage("Check-out must be after check-in.", false);
            return;
        }
        if (in.isBefore(LocalDate.now())) {
            showMessage("Check-in date cannot be in the past.", false);
            return;
        }

        List<Room> available = roomDAO.getAvailableRooms(in.toString(), out.toString());
        roomList.setItems(FXCollections.observableArrayList(available));
        summaryBox.setVisible(false);

        if (available.isEmpty()) {
            showMessage("No rooms available for the selected dates.", false);
        } else {
            messageLabel.setVisible(false);
        }
    }

    @FXML
    private void handleConfirm() {
        User      customer = customerCombo.getValue();
        Room      room     = roomList.getSelectionModel().getSelectedItem();
        LocalDate in       = checkInPicker.getValue();
        LocalDate out      = checkOutPicker.getValue();

        if (customer == null)        { showMessage("Please select a customer.", false); return; }
        if (room     == null)        { showMessage("Please select a room from the list.", false); return; }
        if (in == null || out == null) { showMessage("Please select check-in and check-out dates.", false); return; }
        if (!out.isAfter(in))        { showMessage("Check-out must be after check-in.", false); return; }

        Reservation res = new Reservation();
        res.setCustomerId(customer.getId());
        res.setRoomId(room.getId());
        res.setRoomPrice(room.getPrice());
        res.setCheckIn(in);
        res.setCheckOut(out);
        res.setGuests(guestSpinner.getValue());
        res.setNotes(notesArea.getText().trim());

        if (reservationDAO.create(res)) {
            showMessage("Reservation confirmed!", true);
            MainApp.showScreen("StaffDashboard");
        } else {
            showMessage("Failed to create reservation. Please try again.", false);
        }
    }

    @FXML
    private void handleBack() {
        MainApp.showScreen("StaffDashboard");
    }

    private void refreshSummaryIfSelected() {
        Room sel = roomList.getSelectionModel().getSelectedItem();
        if (sel != null) updateSummary(sel);
    }

    private void updateSummary(Room room) {
        LocalDate in  = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in == null || out == null || !out.isAfter(in)) {
            summaryBox.setVisible(false);
            return;
        }
        long   nights = ChronoUnit.DAYS.between(in, out);
        double total  = nights * room.getPrice();

        summaryRoom.setText("Room " + room.getRoomNumber() + " — " + room.getRoomType());
        summaryNights.setText(nights + " night(s) × ₱" + String.format("%.2f", room.getPrice()) + "/night");
        summaryTotal.setText("Total: ₱" + String.format("%.2f", total));
        summaryBox.setVisible(true);
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setStyle(success
            ? "-fx-text-fill:#2ecc71;-fx-font-size:12px;"
            : "-fx-text-fill:#e74c3c;-fx-font-size:12px;");
        messageLabel.setVisible(true);
    }
}
