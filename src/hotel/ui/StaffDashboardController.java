package hotel.ui;

import hotel.MainApp;
import hotel.dao.*;
import hotel.model.*;
import hotel.reports.BillReport;
import hotel.util.Session;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

// Controls the staff dashboard with four tabs: Rooms, Reservations, Customers, Billing
// Implements Initializable: initialize() sets up columns, placeholders, and listeners
// Message passing: each action method sends data to a DAO then calls refreshAll()
public class StaffDashboardController implements Initializable {

    // Top bar
    @FXML private Label welcomeLabel;

    // Rooms tab
    @FXML private TableView<Room>               roomsTable;
    @FXML private TableColumn<Room, String>     colRoomNum;
    @FXML private TableColumn<Room, String>     colRoomType;
    @FXML private TableColumn<Room, Double>     colRoomPrice;
    @FXML private TableColumn<Room, Integer>    colRoomCap;
    @FXML private TableColumn<Room, String>     colRoomDesc;
    @FXML private TableColumn<Room, Room.Status> colRoomStatus;

    // Reservations tab
    @FXML private TableView<Reservation>             reservationsTable;
    @FXML private TableColumn<Reservation, Integer>  colResId;
    @FXML private TableColumn<Reservation, String>   colResCustomer;
    @FXML private TableColumn<Reservation, String>   colResRoom;
    @FXML private TableColumn<Reservation, String>   colResType;
    @FXML private TableColumn<Reservation, String>   colResCheckIn;
    @FXML private TableColumn<Reservation, String>   colResCheckOut;
    @FXML private TableColumn<Reservation, Long>     colResNights;
    @FXML private TableColumn<Reservation, Integer>  colResGuests;
    @FXML private TableColumn<Reservation, String>   colResStatus;
    @FXML private TableColumn<Reservation, Double>   colResTotal;

    // Customers tab
    @FXML private TableView<User>              customersTable;
    @FXML private TableColumn<User, Integer>   colCustId;
    @FXML private TableColumn<User, String>    colCustName;
    @FXML private TableColumn<User, String>    colCustUser;
    @FXML private TableColumn<User, String>    colCustPhone;
    @FXML private Label                        custDetailLabel;
    @FXML private TableView<Reservation>       custHistoryTable;
    @FXML private TableColumn<Reservation, Integer> colHistId;
    @FXML private TableColumn<Reservation, String>  colHistRoom;
    @FXML private TableColumn<Reservation, String>  colHistIn;
    @FXML private TableColumn<Reservation, String>  colHistOut;
    @FXML private TableColumn<Reservation, Long>    colHistNights;
    @FXML private TableColumn<Reservation, Double>  colHistTotal;
    @FXML private TableColumn<Reservation, String>  colHistStatus;

    // Billing tab
    @FXML private TableView<Bill>              billsTable;
    @FXML private TableColumn<Bill, Integer>   colBillId;
    @FXML private TableColumn<Bill, String>    colBillCustomer;
    @FXML private TableColumn<Bill, String>    colBillRoom;
    @FXML private TableColumn<Bill, Long>      colBillNights;
    @FXML private TableColumn<Bill, String>    colBillCheckIn;
    @FXML private TableColumn<Bill, String>    colBillCheckOut;
    @FXML private TableColumn<Bill, Double>    colBillSubtotal;
    @FXML private TableColumn<Bill, Double>    colBillDiscount;
    @FXML private TableColumn<Bill, Double>    colBillAmount;
    @FXML private TableColumn<Bill, String>    colBillPaid;
    @FXML private TableColumn<Bill, String>    colBillMethod;
    @FXML private TableColumn<Bill, String>    colBillDate;

    private final RoomDAO        roomDAO        = new RoomDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final UserDAO        userDAO        = new UserDAO();
    private final BillDAO        billDAO        = new BillDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeLabel.setText("Welcome, " + Session.getCurrentUser().getFullName() + "  |  Staff");
        setupColumns();
        refreshAll();

        Label phRooms = new Label("No records found"); phRooms.setStyle("-fx-text-fill:white;");
        Label phRes   = new Label("No records found"); phRes.setStyle("-fx-text-fill:white;");
        Label phCust  = new Label("No records found"); phCust.setStyle("-fx-text-fill:white;");
        Label phHist  = new Label("No records found"); phHist.setStyle("-fx-text-fill:white;");
        Label phBill  = new Label("No records found"); phBill.setStyle("-fx-text-fill:white;");
        roomsTable.setPlaceholder(phRooms);
        reservationsTable.setPlaceholder(phRes);
        customersTable.setPlaceholder(phCust);
        custHistoryTable.setPlaceholder(phHist);
        billsTable.setPlaceholder(phBill);

        // Load reservation history when a customer is selected
        customersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) loadCustomerHistory(sel);
        });
    }

    // ─── Setup ───────────────────────────────────────────────────────────────

    private void setupColumns() {
        // Rooms
        colRoomNum.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colRoomType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colRoomPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colRoomCap.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colRoomDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colRoomStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Reservations — use lambda for computed / joined fields
        colResId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colResCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colResRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colResType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colResCheckIn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCheckIn().toString()));
        colResCheckOut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCheckOut().toString()));
        colResNights.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getNights()).asObject());
        colResGuests.setCellValueFactory(new PropertyValueFactory<>("guests"));
        colResStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colResTotal.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTotalAmount()).asObject());

        // Customers
        colCustId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colCustUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colCustPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        // Customer history (same Reservation type)
        colHistId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colHistRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colHistIn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCheckIn().toString()));
        colHistOut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCheckOut().toString()));
        colHistNights.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getNights()).asObject());
        colHistTotal.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTotalAmount()).asObject());
        colHistStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        // Bills
        colBillId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colBillCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colBillRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colBillNights.setCellValueFactory(new PropertyValueFactory<>("nights"));
        colBillCheckIn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        colBillCheckOut.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        colBillSubtotal.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getSubtotal()).asObject());
        colBillDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colBillAmount.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getGrandTotal()).asObject());
        colBillPaid.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isPaid() ? "PAID" : "UNPAID"));
        colBillMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colBillDate.setCellValueFactory(new PropertyValueFactory<>("issuedAt"));
    }

    private void refreshAll() {
        roomsTable.setItems(FXCollections.observableArrayList(roomDAO.getAllRooms()));
        reservationsTable.setItems(FXCollections.observableArrayList(reservationDAO.getAll()));
        customersTable.setItems(FXCollections.observableArrayList(userDAO.getAllCustomers()));
        billsTable.setItems(FXCollections.observableArrayList(billDAO.getAllBills()));
        custDetailLabel.setText("Select a customer to view their history.");
        custHistoryTable.setItems(FXCollections.emptyObservableList());
    }

    private void loadCustomerHistory(User customer) {
        List<Reservation> hist = reservationDAO.getByCustomer(customer.getId());
        custHistoryTable.setItems(FXCollections.observableArrayList(hist));
        custDetailLabel.setText(customer.getFullName() + " | " + customer.getEmail() + " | " + customer.getPhone()
            + "  —  " + hist.size() + " reservation(s)");
    }

    // ─── Room Actions ─────────────────────────────────────────────────────────

    @FXML
    private void handleAddRoom() {
        showRoomDialog(null).ifPresent(room -> {
            if (roomDAO.addRoom(room)) {
                refreshAll();
            } else {
                alert("Duplicate Room", "A room with that number already exists.");
            }
        });
    }

    @FXML
    private void handleEditRoom() {
        Room sel = roomsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("No Selection", "Please select a room to edit."); return; }
        showRoomDialog(sel).ifPresent(updated -> {
            updated.setId(sel.getId());
            roomDAO.updateRoom(updated);
            refreshAll();
        });
    }

    @FXML
    private void handleDeleteRoom() {
        Room sel = roomsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("No Selection", "Please select a room to delete."); return; }
        if (sel.getStatus() == Room.Status.OCCUPIED) {
            alert("Cannot Delete", "Room is currently occupied."); return;
        }
        confirm("Delete Room", "Delete Room " + sel.getRoomNumber() + "?").ifPresent(btn -> {
            if (btn == ButtonType.YES) { roomDAO.deleteRoom(sel.getId()); refreshAll(); }
        });
    }

    private Optional<Room> showRoomDialog(Room existing) {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Room" : "Edit Room");
        dialog.getDialogPane().setStyle("-fx-background-color:#1a1a2e;");

        TextField numField   = field(existing != null ? existing.getRoomNumber()  : "", "e.g. 105");
        TextField typeField  = field(existing != null ? existing.getRoomType()    : "", "e.g. Standard Double");
        TextField priceField = field(existing != null ? String.valueOf(existing.getPrice()) : "", "e.g. 1800.00");
        TextField capField   = field(existing != null ? String.valueOf(existing.getCapacity()) : "1", "1-10");
        TextField descField  = field(existing != null ? nvl(existing.getDescription()) : "", "Short description");
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("AVAILABLE","OCCUPIED","MAINTENANCE"));
        statusCombo.setValue(existing != null ? existing.getStatus().name() : "AVAILABLE");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(14));
        grid.setStyle("-fx-background-color:#16213e;");
        addRow(grid, 0, "Room Number:", numField);
        addRow(grid, 1, "Room Type:",   typeField);
        addRow(grid, 2, "Price/Night:", priceField);
        addRow(grid, 3, "Capacity:",    capField);
        addRow(grid, 4, "Description:", descField);
        addRow(grid, 5, "Status:",      statusCombo);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                Room r = new Room(
                    numField.getText().trim(),
                    typeField.getText().trim(),
                    Double.parseDouble(priceField.getText().trim()),
                    Integer.parseInt(capField.getText().trim()),
                    descField.getText().trim()
                );
                r.setStatus(Room.Status.valueOf(statusCombo.getValue()));
                return r;
            } catch (NumberFormatException e) {
                alert("Invalid Input", "Price and capacity must be valid numbers.");
                return null;
            }
        });

        return dialog.showAndWait();
    }

    // ─── Reservation Actions ──────────────────────────────────────────────────

    @FXML private void handleNewReservation() { MainApp.showScreen("NewReservation"); }

    @FXML
    private void handleCheckOut() {
        Reservation sel = reservationsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("No Selection", "Select a reservation first."); return; }
        if (sel.getStatus() != Reservation.Status.ACTIVE) {
            alert("Invalid", "Only ACTIVE reservations can be checked out."); return;
        }

        // Checkout dialog: discount, tax, payment method
        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Checkout – " + sel.getCustomerName());
        dialog.getDialogPane().setStyle("-fx-background-color:#1a1a2e;");

        double subtotal = sel.getTotalAmount();
        TextField discountField = field("0.00", "Discount in ₱");
        TextField taxField      = field("0.00", "Tax in ₱");
        ComboBox<String> methodCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Cash","GCash","Maya","Credit Card","Debit Card","Bank Transfer"));
        methodCombo.setValue("Cash");

        Label subtotalLbl = new Label("Subtotal: ₱" + String.format("%.2f", subtotal));
        subtotalLbl.setStyle("-fx-text-fill:#2ecc71;-fx-font-weight:bold;-fx-font-size:14px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(14));
        grid.setStyle("-fx-background-color:#16213e;");
        addRow(grid, 0, "Room / Nights:", new Label(sel.getRoomNumber() + "  ×  " + sel.getNights() + " night(s)"));
        grid.add(subtotalLbl, 0, 1, 2, 1);
        addRow(grid, 2, "Discount (₱):", discountField);
        addRow(grid, 3, "Tax (₱):",      taxField);
        addRow(grid, 4, "Payment Method:", methodCombo);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                return new double[] {
                    Double.parseDouble(discountField.getText().trim()),
                    Double.parseDouble(taxField.getText().trim())
                };
            } catch (NumberFormatException e) { return new double[]{0,0}; }
        });

        dialog.showAndWait().ifPresent(vals -> {
            String method = methodCombo.getValue();
            boolean ok = reservationDAO.checkOut(sel.getId(), sel.getRoomId(),
                                                  subtotal, vals[0], vals[1], method);
            if (ok) {
                alert("Success", "Check-out complete. Bill generated.");
                refreshAll();
            }
        });
    }

    @FXML
    private void handleCancelReservation() {
        Reservation sel = reservationsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("No Selection", "Select a reservation first."); return; }
        if (sel.getStatus() != Reservation.Status.ACTIVE) {
            alert("Invalid", "Only ACTIVE reservations can be cancelled."); return;
        }
        confirm("Cancel Reservation", "Cancel reservation for " + sel.getCustomerName() + "?")
            .ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    reservationDAO.cancel(sel.getId(), sel.getRoomId());
                    refreshAll();
                }
            });
    }

    @FXML private void handleRefresh() { refreshAll(); }

    // ─── Customer Actions ─────────────────────────────────────────────────────

    @FXML
    private void handleEditCustomer() {
        User sel = customersTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("No Selection", "Select a customer first."); return; }

        // Fetch full profile
        User full = userDAO.getById(sel.getId());
        if (full == null) return;

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer: " + full.getFullName());
        dialog.getDialogPane().setStyle("-fx-background-color:#1a1a2e;");

        TextField nameF    = field(nvl(full.getFullName()), "Full name");
        TextField emailF   = field(nvl(full.getEmail()),    "Email");
        TextField phoneF   = field(nvl(full.getPhone()),    "Phone");
        phoneF.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d{0,11}")) return change;
            return null;
        }));
        TextField addrF    = field(nvl(full.getAddress()),  "Address");
        ComboBox<String> idTypeCb = new ComboBox<>(FXCollections.observableArrayList(
            "Philippine Passport","Driver's License","SSS ID","GSIS ID","PhilHealth ID",
            "Pag-IBIG ID","National ID (PhilSys)","Voter's ID","PRC ID","Senior Citizen ID","Other"));
        idTypeCb.setValue(nvl(full.getIdType()));
        TextField idNumF   = field(nvl(full.getIdNumber()), "ID Number");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(14));
        grid.setStyle("-fx-background-color:#16213e;");
        addRow(grid, 0, "Full Name:", nameF);
        addRow(grid, 1, "Email:",     emailF);
        addRow(grid, 2, "Phone:",     phoneF);
        addRow(grid, 3, "Address:",   addrF);
        addRow(grid, 4, "ID Type:",   idTypeCb);
        addRow(grid, 5, "ID Number:", idNumF);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            full.setFullName(nameF.getText().trim());
            full.setEmail(emailF.getText().trim());
            full.setPhone(phoneF.getText().trim());
            full.setAddress(addrF.getText().trim());
            full.setIdType(idTypeCb.getValue());
            full.setIdNumber(idNumF.getText().trim());
            return full;
        });

        dialog.showAndWait().ifPresent(u -> { userDAO.update(u); refreshAll(); });
    }

    // ─── Billing Actions ──────────────────────────────────────────────────────

    @FXML
    private void handleGenerateBill() {
        Bill sel = billsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("No Selection", "Select a bill first."); return; }
        String path = BillReport.print(sel);
        if (path != null) {
            alert("Bill Generated", "PDF saved to:\n" + path);
        } else {
            alert("Note", "Bill could not be generated. Check the console for details.");
        }
    }

    @FXML
    private void handleMarkPaid() {
        Bill sel = billsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("No Selection", "Select a bill first."); return; }
        if (sel.isPaid()) { alert("Already Paid", "This bill is already marked as paid."); return; }

        ComboBox<String> methodCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Cash","GCash","Maya","Credit Card","Debit Card","Bank Transfer"));
        methodCombo.setValue("Cash");

        Dialog<String> d = new Dialog<>();
        d.setTitle("Mark as Paid");
        d.getDialogPane().setStyle("-fx-background-color:#1a1a2e;");
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(14));
        g.setStyle("-fx-background-color:#16213e;");
        addRow(g, 0, "Payment Method:", methodCombo);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn == ButtonType.OK ? methodCombo.getValue() : null);

        d.showAndWait().ifPresent(method -> {
            billDAO.markPaid(sel.getId(), method);
            refreshAll();
        });
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        Session.logout();
        MainApp.showScreen("Login");
    }

    // ─── Utility helpers ─────────────────────────────────────────────────────

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    private Optional<ButtonType> confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        return a.showAndWait();
    }

    private TextField field(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setStyle("-fx-background-color:#1a1a2e;-fx-text-fill:white;-fx-border-color:#444;"
                  + "-fx-border-radius:4;-fx-background-radius:4;-fx-padding:6;");
        tf.setPrefWidth(260);
        return tf;
    }

    private void addRow(GridPane g, int row, String labelText, javafx.scene.Node control) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill:#cccccc;-fx-font-size:12px;");
        g.add(lbl, 0, row);
        g.add(control, 1, row);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
