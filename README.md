# Sunset Coral Calculator

Android calculator app matching the **Sunset Coral** design from `Calculator_Screen_Image/calculator_sunset_coral.png`.  
*(Design images in `Calculator_Screen_Image/` and `Extra_Image/` are kept locally only, not in the repo.)*

**Repository:** [github.com/mollanuramin130/android_app_calculator](https://github.com/mollanuramin130/android_app_calculator)

- **Language:** Java  
- **IDE:** Android Studio  
- **Min SDK:** 24  
- **Target SDK:** 34  

## Design

- Peach-to-coral gradient background  
- Rounded display area with calculation history and result (e.g. `1,592` / `+ 800` / `= 2,392`)  
- Neumorphic-style circular buttons: numbers and decimal on light coral, operators (+, −, ×, =, C) on dark coral  
- Zero button is wide (spans two columns)  
- Numbers shown with comma thousands separators  

## How to run

1. Open the project in **Android Studio**.  
2. Use **File → Open** and select the `Custom_Calculator_App` folder.  
3. Wait for Gradle sync.  
4. Run on an emulator or device (Run ▶ or **Shift+F10**).  

If the Gradle wrapper is missing, create the project from Android Studio (**File → New → Import Project**) or run **Gradle → Wrapper** to generate it.

**If you see "Activity class does not exist" when running:**  
Uninstall the old app from the device/emulator (Settings → Apps → your calculator app → Uninstall), then **File → Invalidate Caches → Invalidate and Restart**. Run again. The app package is `com.nuramin.calculator`.

**If the Run button is disabled:**  
- **Option 1 – Use Gradle:** In the toolbar, open the **Run configuration dropdown** (where it says "app" or "Install_Debug"). Select **"Install_Debug"**. Connect an emulator or device, then click **Run**. The app will install; open it from the device.  
- **Option 2 – Create Android App config:** **Run → Edit Configurations…** → click **+** (top-left) → **Android App**. Set **Module** to your app module (e.g. `Custom_Calculator_App.app`), **Launch Options** to **Default Activity** → **Apply** → **OK**. Choose a device in the toolbar and run.  
- **Option 3:** **File → Sync Project with Gradle Files**, then **Build → Rebuild Project**. After sync, the **app** run configuration may appear; select it and a device, then run.

## Project structure

- `app/src/main/java/com/nuramin/calculator/MainActivity.java` – calculator logic  
- `app/src/main/res/layout/activity_main.xml` – layout (title, display, button grid)  
- `app/src/main/res/values/` – colors, dimensions, themes, styles  
- `app/src/main/res/drawable/` – gradients, button backgrounds, launcher icon  

## Operations

- **+** addition  
- **−** subtraction  
- **×** multiplication  
- **=** compute result  
- **C** clear  
- **.** decimal input  

Result and current input are formatted with comma separators (e.g. `2,392`).
