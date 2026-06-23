# SixEven-Hotel

Open Eclipse and Import the Project

1. Open **Eclipse IDE**
2. Go to **File → Import**
3. Expand **General** and select **Existing Projects into Workspace**
4. Click **Next**
  
### Step 3 — Select the Project Folder

1. Click **Browse** next to *Select root directory*
2. Navigate to the cloned/extracted folder and select the **`HotelReservationSystem`** folder (the one that contains `src`, `lib`, `lib-fx`, `.classpath`, and `.project`)
3. Click **Open**
4. A checkbox entry for **HotelReservationSystem** will appear — make sure it is checked
5. Click **Finish**

> **Note:** If you see an error saying a project with the same name already exists  
> right-click the existing project in the Package Explorer → **Delete** → **uncheck** "Delete project contents on disk" → OK. Then repeat the import.

### Step 4 — Run the Application

The repository includes a pre-configured Eclipse launch file. To use it:

1. In the **Package Explorer**, expand the project
2. Find **`HotelReservationSystem.launch`** in the project root
3. Right-click it → **Run As → HotelReservationSystem**

**Or** run it manually:

1. Right-click the project → **Run As → Run Configurations**
2. Double-click **Java Application** to create a new configuration
3. Set **Main class** to `hotel.MainApp`
4. Click the **Arguments** tab
5. In the **VM arguments** box, paste:
   ```
   --module-path "${project_loc}/lib-fx" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base
   ```
6. Set **Working directory** to `${project_loc}`
7. Click **Run**
