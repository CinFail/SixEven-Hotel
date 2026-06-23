package hotel.util;

import hotel.model.User;

// Encapsulation: currentUser is private; controllers access it only through static methods
// Acts as a singleton session holder — one user logged in at a time
public class Session {

    private static User currentUser;

    private Session() {}

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser()          { return currentUser; }
    public static boolean isStaff()              { return currentUser != null && currentUser.getRole() == User.Role.STAFF; }
    public static void logout()                  { currentUser = null; }
}
