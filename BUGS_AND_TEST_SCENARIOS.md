# Bugs Fixed & Test Scenarios

## Bug fix applied

### 1. **AmountFormatter.formatExpressionForDisplay(null)**
- **Issue:** When `rawExpression` was `null`, the method returned `null`, which could cause `NullPointerException` in callers (e.g. `BasicCalculatorScreen.updateDisplay()` when building display string).
- **Fix:** Return `""` instead of `null` when input is null or empty.

---

## Unit tests added

Tests are in `app/src/test/java/`. Run with:
- **Android Studio:** Right-click `app/src/test` → Run Tests
- **Command line:** `./gradlew testDebugUnitTest` (or `testReleaseUnitTest`)

### ExprParserTest
- Basic arithmetic: +, −, ×, ÷, ^
- Order of operations and parentheses
- Division by zero throws
- Factorial: 0!, 1!, 4!, negative/non-integer throws
- sqrt, sin/cos/tan (degrees), ln, log, constants π, e
- Decimal and scientific numbers (1e2)
- Null/empty and invalid characters throw
- Unclosed parenthesis throws
- Power right-associativity

### AmountFormatterTest
- format: integers, decimals, very small (scientific), very large, NaN/Infinity → "Error"
- stripGrouping: null, with commas
- parse: null, empty, with grouping
- formatExpressionForDisplay: null → "", empty → "", grouping
- rawIndexToFormattedIndex / formattedIndexToRawIndex (cursor mapping)

### CalculatorUtilsTest
- filterExpressionChars: allows valid, strips invalid, strips spaces, null → ""
- sanitizeSingleDecimalPerNumber: multiple dots per number, null
- formatNumber delegation

### MathSpeedGameEngineTest
- Initial state: score 0, lives, timer, equation and options present
- Correct answer: score increases, game keeps running
- Wrong answer: lives decrease
- Three wrong: game over
- tickTimer, addTimeBonus, reset
- submitAnswer after game over returns false

### NumberTargetPuzzleEngineTest
- Initial state: level 1, attempts, numbers and target set
- submitExpression: empty → INVALID_EXPRESSION, wrong numbers → INVALID_NUMBERS
- Valid expression uses attempts; reset restores state
- getPerformanceRating

### MemoryGridGameEngineTest
- reset: initial level and score
- startLevel: numbers count for level 1
- getNumbersCount in valid range

---

## Manual / scenario checklist (avoid crashes)

Use these when testing the app by hand or in automated UI tests.

### Calculator (Basic)
- [ ] Enter `0.00056×0.00564` → result shows scientific (e.g. 3.16E-6), not "0"
- [ ] Enter `0.008` and `0.002` → live result shows 0.008, 0.002 (not 0.01 or 0)
- [ ] After `=`, press `%` → % applies to result (e.g. 100 → 100%)
- [ ] Cursor between digits, insert number → inserted at cursor; no missing digits
- [ ] Cursor in middle, press backspace → deletes character before cursor
- [ ] Expression with multiple decimals (e.g. 2.06.9) → sanitized to one decimal per number
- [ ] Division by zero → show Error, no crash
- [ ] Very long expression → no crash, no hang

### Panels (Temperature, Currency, EMI, Interest, etc.)
- [ ] Open each panel from drawer → no crash
- [ ] Rotate device on each panel → layout ok, no crash
- [ ] Leave fields empty and tap Convert/Calculate → handled gracefully

### Math Speed Game
- [ ] Start game → equations and falling answers appear
- [ ] Tap correct answer → score up, time bonus, next equation
- [ ] Tap wrong answer → lives decrease
- [ ] Lose all lives → game over dialog, no crash
- [ ] Navigate away (drawer) while game running → game pauses, no dialog on other screen
- [ ] Timer reaches 0 → game over

### Number Target Puzzle
- [ ] Start puzzle → 4 numbers and target shown
- [ ] Submit valid expression using each number once → correct/wrong feedback
- [ ] Exhaust attempts → appropriate state, no crash

### Memory Grid Game
- [ ] Start level → numbers flash, then tap in order
- [ ] Wrong order → fail handling, no crash
- [ ] Complete level → next level or completion

### General
- [ ] Open overflow (3-dots) on each screen → menu opens, no crash
- [ ] Theme switch (light/dark/system) → applies, no crash
- [ ] History: use expression, use result → inserts correctly
- [ ] Clear history → list clears, no crash
- [ ] Background/foreground: open app, send to background, return → no crash
- [ ] Low memory: use app then trigger low memory (e.g. many apps) → no crash

---

## Files changed

- `app/src/main/java/com/nuramin/calculator/util/AmountFormatter.java` – null/empty return fix
- `app/build.gradle` – `testImplementation 'junit:junit:4.13.2'`
- `app/src/test/java/.../ExprParserTest.java` – new
- `app/src/test/java/.../AmountFormatterTest.java` – new
- `app/src/test/java/.../CalculatorUtilsTest.java` – new
- `app/src/test/java/.../MathSpeedGameEngineTest.java` – new
- `app/src/test/java/.../NumberTargetPuzzleEngineTest.java` – new
- `app/src/test/java/.../MemoryGridGameEngineTest.java` – new
