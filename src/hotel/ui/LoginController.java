package hotel.ui;

import hotel.MainApp;
import hotel.dao.UserDAO;
import hotel.model.User;
import hotel.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;

// Message passing: sends the authenticated User object to Session before switching screens
// Polymorphism via role: STAFF routes to StaffDashboard, CUSTOMER routes to CustomerDashboard
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in both fields.");
            return;
        }

        User user = userDAO.login(username, password);

        if (user == null) {
            showError("Incorrect username or password.");
            return;
        }

        Session.setCurrentUser(user); // store logged-in user for the whole session

        // Role-based routing: different dashboards for different user types
        if (user.getRole() == User.Role.STAFF) {
            MainApp.showScreen("StaffDashboard");
        } else {
            MainApp.showScreen("CustomerDashboard");
        }
    }

    @FXML
    private void handleGoToRegister() {
        MainApp.showScreen("Register");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
