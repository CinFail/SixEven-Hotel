package hotel.ui;

import hotel.MainApp;
import hotel.dao.BillDAO;
import hotel.dao.ReservationDAO;
import hotel.dao.UserDAO;
import hotel.model.Bill;
import hotel.model.Reservation;
import hotel.model.User;
import hotel.reports.BillReport;
import hotel.util.Session;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

// Controls the customer dashboard with two tabs: My Reservations and My Profile
// Implements Initializable: initialize() loads the current user's data on screen entry
// Encapsulation: only data belonging to the logged-in customer is loaded and shown
public class CustomerDashboardController implements Initializable {

    @FXML private Label welcomeLabel;

    // Reservations tab
    @FXML private TableView<Reservation>             reservationsTable;
    @FXML private TableColumn<Reservation, Integer>  colResId;
    @FXML private TableColumn<Reservation, String>   colResRoom;
    @FXML private TableColumn<Reservation, String>   colResType;
    @FXML private TableColumn<Reservation, String>   colResCheckIn;
    @FXML private TableColumn<Reservation, String>   colResCheckOut;
    @FXML private TableColumn<Reservation, Long>     colResNights;
    @FXML private TableColumn<Reservation, Integer>  colResGuests;
    @FXML private TableColumn<Reservation, Double>   colResTotal;
    @FXML private TableColumn<Reservation, String>   colResStatus;
    @FXML private TableColumn<Reservation, String>   colResDate;

    // Profile tab
    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private TextField     addressField;
    @FXML private ComboBox<String> idTypeCombo;
    @FXML private TextField     idNumberField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         profileMessage;

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final BillDAO        billDAO        = new BillDAO();
    private final UserDAO        userDAO        = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User me = Session.getCurrentUser();
        welcomeLabel.setText("Welcome, " + me.getFullName());

        idTypeCombo.setItems(FXCollections.observableArrayList(
            "Philippine Passport","Driver's License","SSS ID","GSIS ID","PhilHealth ID",
            "Pag-IBIG ID","National ID (PhilSys)","Voter's ID","PRC ID","Senior Citizen ID","Other"));
        idTypeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill:white;-fx-background-color:#16213e;");
            }
        });

        loadProfile();
        setupColumns();
        loadReservations();

        phoneField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d{0,11}")) return change;
            return null;
        }));

        Label ph = new Label("No records found");
        ph.setStyle("-fx-text-fill:white;");
        reservationsTable.setPlaceholder(ph);
    }

    private void loadProfile() {
        // Refresh from DB
        User me = userDAO.getById(Session.getCurrentUser().getId());
        if (me != null) {
            Session.setCurrentUser(me);
            nameField.setText(nvl(me.getFullName()));
            emailField.setText(nvl(me.getEmail()));
            phoneField.setText(nvl(me.getPhone()));
            addressField.setText(nvl(me.getAddress()));
            if (me.getIdType() != null) idTypeCombo.setValue(me.getIdType());
            idNumberField.setText(nvl(me.getIdNumber()));
        }
    }

    private void setupColumns() {
        colResId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colResRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colResType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colResCheckIn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCheckIn().toString()));
        colResCheckOut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCheckOut().toString()));
        colResNights.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getNights()).asObject());
        colResGuests.setCellValueFactory(new PropertyValueFactory<>("guests"));
        colResTotal.setCellValueFactory(c ->
            new javafx.beans.property.SimpleDoubleProperty(c.getValue().getTotalAmount()).asObject());
        colResStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colResDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void loadReservations() {
        reservationsTable.setItems(FXCollections.observableArrayList(
            reservationDAO.getByCustomer(Session.getCurrentUser().getId())));
    }

    @FXML
    private void handleViewBill() {
        Reservation sel = reservationsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION, "Please select a reservation first.", ButtonType.OK).showAndWait();
            return;
        }
        Bill bill = billDAO.getByReservationId(sel.getId());
        if (bill == null) {
            new Alert(Alert.AlertType.INFORMATION,
                "No bill found. The reservation may not have been checked out yet.", ButtonType.OK).showAndWait();
            return;
        }
        BillReport.print(bill);
    }

    @FXML
    private void handleUpdateProfile() {
        User me = Session.getCurrentUser();
        me.setFullName(nameField.getText().trim());
        me.setEmail(emailField.getText().trim());
        me.setPhone(phoneField.getText().trim());
        me.setAddress(addressField.getText().trim());
        me.setIdType(idTypeCombo.getValue());
        me.setIdNumber(idNumberField.getText().trim());

        boolean ok = userDAO.update(me);

        // Optional password change
        String newPw = newPasswordField.getText();
        if (!newPw.isEmpty()) {
            if (newPw.length() < 6) {
                showMsg("Password must be at least 6 characters.", false);
                return;
            }
            if (!newPw.equals(confirmPasswordField.getText())) {
                showMsg("Passwords do not match.", false);
                return;
            }
            userDAO.changePassword(me.getId(), newPw);
            newPasswordField.clear();
            confirmPasswordField.clear();
        }

        showMsg(ok ? "Profile saved successfully." : "Failed to save profile.", ok);
    }

    @FXML
    private void handleLogout() {
        Session.logout();
        MainApp.showScreen("Login");
    }

    private void showMsg(String text, boolean success) {
        profileMessage.setText(text);
        profileMessage.setStyle(success
            ? "-fx-text-fill:#2ecc71;-fx-font-size:12px;"
            : "-fx-text-fill:#e74c3c;-fx-font-size:12px;");
        profileMessage.setVisible(true);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
