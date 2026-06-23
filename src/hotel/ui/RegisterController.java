package hotel.ui;

import hotel.MainApp;
import hotel.dao.UserDAO;
import hotel.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

// Handles self-registration for customer accounts only — staff accounts are created by admin
// Implements Initializable: initialize() runs automatically after FXML fields are injected
public class RegisterController implements Initializable {

    @FXML private TextField     nameField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private TextField     addressField;
    @FXML private ComboBox<String> idTypeCombo;
    @FXML private TextField     idNumberField;
    @FXML private Label         messageLabel;

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idTypeCombo.setItems(FXCollections.observableArrayList(
            "Philippine Passport",
            "Driver's License",
            "SSS ID",
            "GSIS ID",
            "PhilHealth ID",
            "Pag-IBIG ID",
            "National ID (PhilSys)",
            "Voter's ID",
            "PRC ID",
            "Senior Citizen ID",
            "Other"
        ));
        idTypeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill:white;-fx-background-color:#16213e;");
            }
        });
        phoneField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d{0,11}")) return change;
            return null;
        }));
    }

    @FXML
    private void handleRegister() {
        String name     = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showMessage("Name, username, and password are required.", false);
            return;
        }
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters.", false);
            return;
        }

        User user = new User(username, password, User.Role.CUSTOMER, name);
        user.setEmail(emailField.getText().trim());
        user.setPhone(phoneField.getText().trim());
        user.setAddress(addressField.getText().trim());
        user.setIdType(idTypeCombo.getValue());
        user.setIdNumber(idNumberField.getText().trim());

        boolean success = userDAO.register(user);

        if (success) {
            showMessage("Account created! You can now log in.", true);
        } else {
            showMessage("Username already taken. Please choose another.", false);
        }
    }

    @FXML
    private void handleBack() {
        MainApp.showScreen("Login");
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setStyle(success
            ? "-fx-text-fill:#2ecc71;-fx-font-size:12px;"
            : "-fx-text-fill:#e74c3c;-fx-font-size:12px;");
        messageLabel.setVisible(true);
    }
}
