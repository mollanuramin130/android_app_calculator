# Publishing Sunset Coral Calculator to Google Play Store

Follow this guide step by step to publish your app and to update it later.

---

## Part 1: Pre-publish checklist (bugs & security)

### ✅ Security (already verified for your app)

- **No sensitive data:** No API keys, passwords, or tokens in the code.
- **No network:** Calculator does not use the internet, so no data is sent.
- **Minimal permissions:** Your `AndroidManifest.xml` has no extra permissions (only what the system needs).
- **Release build:** You will sign and upload a **release** build (not debug).

### ✅ Stability

- Expression parser handles invalid input and returns "Error" instead of crashing.
- Number formatting handles NaN/Infinity and shows "Error".
- History list is limited (50 items) to avoid memory issues.

### Before you build for release

1. **Use a unique application ID** (recommended for Play Store)  
   In `app/build.gradle`, change:
   ```gradle
   applicationId "com.nuramin.sunsetcoralcalculator"
   ```
   to your own, e.g.:
   ```gradle
   applicationId "com.nuramin.sunsetcoralcalculator"
   ```
   Use a domain-style ID you control; you **cannot** change it after the first publish.

2. **Launcher icon**  
   You already have `ic_launcher`. For a more polished look, add adaptive icons (see Play Console asset requirements).

3. **Test on a real device**  
   Install the release build and test: all buttons, long expressions, history, equals, clear, and horizontal scroll.

---

## Part 2: One-time setup (developer account & app signing)

### Step 1: Pay the one-time registration fee

- In [Google Play Console](https://play.google.com/console), complete registration.
- Pay the **one-time $25 USD** fee if you have not already.
- Finish identity verification if prompted.

### Steps right after $25 payment and identity verification

Do these in order:

1. **Create your app** (below – Step 2).
2. **App signing** → Setup → App signing (or App integrity). Choose *Continue* with Google Play App Signing.
3. **Upload keystore** → Create upload keystore if you don’t have one (Step 4). Create `keystore.properties` and build a signed AAB (Steps 5–6).
4. **Store listing** → Grow → Store presence → Main store listing. Fill app name, short/full description, 512×512 icon, 1024×500 feature graphic, ≥2 screenshots, category, contact email, privacy policy URL.
5. **Content rating** → Policy → App content → Content rating. Complete questionnaire (calculator = Everyone).
6. **Target audience** → Policy → App content → Target audience and content. Set age group.
7. **Data safety** → Policy → App content → Data safety. Declare no data collected.
8. **Ads** → If no ads, declare “No ads” where asked.
9. **Create release** → Release → Production → Create new release. Upload AAB, set release name and notes, Review, then **Start rollout to Production**.

### Step 2: Create your app in Play Console

1. On the **Home** page, click **Create app**.
2. Fill in:
   - **App name:** e.g. Nur's Custom Calculator
   - **Default language:** Your choice (e.g. English – United States)
   - **App or game:** App
   - **Free or paid:** Free (or Paid if you want to charge)
3. Accept the declarations (e.g. export laws, policies).
4. Click **Create app**.

### Step 3: Set up App Signing (Google Play App Signing)

1. In the left menu, go to **Setup** → **App signing** (or **App integrity**).
2. Choose **Continue** with **Google Play App Signing** (recommended).
3. You will need to upload a first build; the console will then use the signing key it generates.  
   - If you already have a keystore, you can use it to sign the first upload; Play will still manage the app signing key.
   - If you do **not** have a keystore yet, create one when building the release APK/AAB (Step 5 below).

### Step 4: Create an upload keystore (if you don’t have one)

On your Mac, in Terminal:

```bash
cd /Users/nuramin/Desktop/Android_App_Project/Custom_Calculator_App
keytool -genkey -v -keystore sunset-coral-upload.keystore -alias sunset-coral -keyalg RSA -keysize 2048 -validity 10000
```

- Use a strong password and remember it.
- Store `sunset-coral-upload.keystore` and the password **securely** (e.g. password manager + backup).  
  If you lose this keystore, you cannot publish updates under the same app.

---

## Part 3: Build the release bundle (AAB)

### Step 5: Configure signing in your project

1. Create a file **`keystore.properties`** in the **project root** (same folder as `build.gradle`), **not** inside `app/`:

   ```properties
   storePassword=YOUR_KEYSTORE_PASSWORD
   keyPassword=YOUR_KEY_PASSWORD
   keyAlias=sunset-coral
   storeFile=../sunset-coral-upload.keystore
   ```

   Replace:
   - `YOUR_KEYSTORE_PASSWORD` and `YOUR_KEY_PASSWORD` with the passwords you used for the keystore.
   - `storeFile`: use the path to your `.keystore` file. If the keystore is in the project root, `../sunset-coral-upload.keystore` from `app/` is correct; adjust if you put it elsewhere.

2. **Important:** Add `keystore.properties` to `.gitignore` so passwords are never committed:

   ```bash
   echo "keystore.properties" >> .gitignore
   echo "*.keystore" >> .gitignore
   ```

3. In **`app/build.gradle`**, add the signing config (before `buildTypes`):

   ```gradle
   android {
       ...
       defaultConfig { ... }

       signingConfigs {
           release {
               def keystorePropertiesFile = rootProject.file("keystore.properties")
               if (keystorePropertiesFile.exists()) {
                   def keystoreProperties = new Properties()
                   keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
                   storeFile file(keystoreProperties['storeFile'])
                   storePassword keystoreProperties['storePassword']
                   keyAlias keystoreProperties['keyAlias']
                   keyPassword keystoreProperties['keyPassword']
               }
           }
       }

       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled false
               ...
           }
       }
   }
   ```

   (If you prefer not to use `keystore.properties`, you can set `storeFile`, `storePassword`, `keyAlias`, `keyPassword` directly in `signingConfigs.release`; never commit those values.)

### Step 6: Build the Android App Bundle (AAB)

In Android Studio:

1. **Build** → **Generate Signed Bundle / APK**.
2. Choose **Android App Bundle** → **Next**.
3. Select your keystore, enter passwords and alias → **Next**.
4. Select **release** build variant, click **Create**.

The AAB will be at:  
`app/release/app-release.aab`.

---

## Part 4: App name ideas and required upload details

### App name suggestions (choose one for store listing)

| Option | Use when |
|--------|----------|
| **Nur's Custom Calculator** | Keep your current name; personal and clear. |
| **Nur Calculator** | Shorter; good if "Custom" feels redundant. |
| **Nur Calc – Expression & History** | Highlights expression + history; 30 chars fits Play. |
| **Calculator by Nur** | Simple and branded. |
| **Nur's Calc** | Very short; easy to remember. |

**Play Store limit:** App name on the store listing can be up to **30 characters**.

---

### Required details when you upload (checklist)

**When creating the app (one-time):** App name, default language, App (not game), Free/Paid, accept declarations.

**Store listing (Main store listing):**

| Field | Requirement | Example |
|-------|-------------|--------|
| App name | Up to 30 characters | Nur's Custom Calculator |
| Short description | Up to 80 characters | Calculator with expressions, %, history. By Nur. |
| Full description | Up to 4000 characters | Describe: math, ( ), %, history, scientific notation, no ads. |
| App icon | **Required.** 512×512 px PNG, no transparency | From your project. |
| Feature graphic | **Required.** 1024×500 px | Logo + tagline banner. |
| Phone screenshots | **Required.** At least 2 (e.g. 1080×1920) | Main screen + history. |
| Category | One primary | **Tools** or **Productivity** |
| Contact email | **Required.** | Your support email. |
| Privacy policy URL | **Required.** | Use free host; "No data collected" is fine. |

**Policy and content:** Content rating (questionnaire → usually Everyone), Target audience (e.g. 13+), Data safety (no data collected), Ads (No), News app (No).

**Each release:** Upload AAB, release name (e.g. 1.0 (1)), release notes.

### Step 7: Store listing (detailed)

In Play Console, open your app → **Grow** → **Store presence** → **Main store listing** (or **Store setup** → **Main store listing**):

- **App name:** e.g. Nur's Custom Calculator (see suggestions above).
- **Short description** (up to 80 characters): e.g.  
  "Calculator with expressions, %, history and clean design. By Nur."
- **Full description** (up to 4000 characters): Describe features (basic math, %, history, parentheses, scientific notation for large numbers, etc.).
- **App icon:** 512 x 512 px PNG (no transparency).
- **Feature graphic:** 1024 x 500 px (optional but recommended).
- **Screenshots:** At least 2 phone screenshots (e.g. 1080x1920 or 9:16). You can add more for 7" and 10" tablets if you support them.
- **Category:** Productivity or Tools.
- **Contact email:** Your support email.
- **Privacy policy URL:** Required. If the app does not collect any personal data, you can use a simple “No data collected” policy hosted on a free page (e.g. GitHub Pages, or a privacy policy generator).

### Step 8: Content rating and other policies

- **Content rating:** Fill the questionnaire (calculator = low risk, usually Everyone).
- **Target audience:** Set age group (e.g. 13+ or 18+ as appropriate).
- **News app:** Declare “No” if it’s not a news app.
- **COVID-19 contact tracing / Data safety:** Declare that you do not collect user data if that’s the case.
- **Ads:** If you don’t use ads, declare “No ads.”

---

## Part 5: Upload and publish

### Step 9: Create a release

1. In Play Console, go to **Release** → **Production** (or **Testing** first).
2. Click **Create new release**.
3. Upload **app-release.aab**.
4. **Release name:** e.g. “1.0 (1)”.
5. **Release notes:** Short description of what’s in this version (e.g. “Initial release – expression calculator with history and scientific notation”).
6. Click **Save** then **Review release**.
7. Fix any errors (e.g. missing store listing, policy, or signing).
8. When everything is green, click **Start rollout to Production** (or to testing track).

### Step 10: Review and publish

- Google will review the app (often 1–3 days, sometimes longer).
- After approval, the app will go live on the Play Store for the countries you selected.

---

## Part 6: Updating your app over time (best practice)

### Version numbers

In **`app/build.gradle`**:

- **versionCode:** Integer that must **increase** for every upload (e.g. 1, 2, 3, …).
- **versionName:** User-visible string (e.g. "1.0", "1.1", "2.0").

Example for first update:

```gradle
defaultConfig {
    applicationId "com.nuramin.sunsetcoralcalculator"  // never change after first publish
    minSdk 24
    targetSdk 34
    versionCode 2
    versionName "1.1"
}
```

### Update workflow

1. Make code and resource changes in Android Studio.
2. Test on device (debug build).
3. Bump **versionCode** (and **versionName** if you want).
4. Build a new signed AAB: **Build** → **Generate Signed Bundle / APK** → choose existing keystore → **release**.
5. In Play Console: **Release** → **Production** (or a testing track) → **Create new release** → upload the new AAB.
6. Add **Release notes** (what’s new in this version).
7. **Review** and **Start rollout**.

### Best practices for updates

- **Keep the same signing key:** Always use the same upload keystore and alias. Back it up safely.
- **Test before uploading:** Run the release build on at least one device.
- **Staged rollout (optional):** For big updates, use “Staged rollout” (e.g. 20% → 50% → 100%) to catch issues.
- **Release notes:** Write clear, short notes for users so they know what changed.

---

## Quick reference

| Item | Where |
|------|--------|
| First-time fee | Play Console registration |
| Create app | Home → Create app |
| Signing | Setup → App signing; build with your upload keystore |
| Store listing | Grow → Store presence → Main store listing |
| Upload AAB | Release → Production → Create new release |
| Version for update | `versionCode` + `versionName` in `app/build.gradle` |

If you want, the next step can be adding the exact `signingConfigs` block and `keystore.properties` template into your project (with placeholders only, no real passwords).
