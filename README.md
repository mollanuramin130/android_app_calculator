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

## Project structure

- `app/src/main/java/com/nuramin/sunsetcoralcalculator/MainActivity.java` – calculator logic  
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
