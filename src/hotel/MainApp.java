package hotel;

import hotel.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Inheritance: extends Application — JavaFX requires this to launch a GUI app
// Message passing: showScreen() is called by controllers to navigate between screens
public class MainApp extends Application {

    // Single shared Stage held statically so any controller can switch screens
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        DatabaseManager.initializeDatabase(); // create tables and seed default data
        stage.setTitle("Hotel Reservation System");
        stage.setResizable(true);
        stage.show();        // show first so setMaximized works reliably on Windows
        showScreen("Login");
    }

    // Loads an FXML screen by name and replaces the current scene
    public static void showScreen(String screenName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/hotel/ui/" + screenName + ".fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setMaximized(false); // reset so the next true triggers a real resize
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
        } catch (Exception e) {
            System.err.println("[App] Could not load screen: " + screenName);
            e.printStackTrace();
            String msg   = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            String cause = e.getCause() != null ? "\nCause: " + e.getCause().getMessage() : "";
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "Failed to load screen: " + screenName + "\n" + msg + cause,
                javafx.scene.control.ButtonType.OK);
            alert.setTitle("Screen Load Error");
            alert.showAndWait();
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    @Override
    public void stop() {
        DatabaseManager.closeConnection(); // release SQLite connection on exit
    }

    public static void main(String[] args) {
        launch(args);
    }
}
