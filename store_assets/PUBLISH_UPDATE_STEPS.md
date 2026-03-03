# How to publish your latest update to Google Play Console

## 1. Bump the app version (required for each release)

In **`app/build.gradle`**, under `defaultConfig`:

- **versionCode** – Increase by 1 (e.g. `4` → `5`). Play Store requires this to be greater than the last uploaded version.
- **versionName** – Update for users (e.g. `"1.1"` → `"1.2"`). This is what appears in the store and on the device.

Example:
```gradle
versionCode 5
versionName "1.2"
```

## 2. Build a release AAB (Android App Bundle)

In Android Studio:

1. **Build → Generate Signed Bundle / APK**
2. Choose **Android App Bundle** → Next
3. Select your **keystore** (or create one). Use the same keystore you used for the first release.
4. Enter **key alias**, **passwords** → Next
5. Select **release** build variant → Create
6. The AAB is saved (e.g. `app/release/app-release.aab`)

Or from terminal (from project root):
```bash
./gradlew bundleRelease
```
The AAB will be at: `app/build/outputs/bundle/release/app-release.aab`

**Important:** You must sign with the same keystore you used for the app’s first Play Store upload. If you lose that keystore, you cannot update the app on Play.

## 3. Upload to Google Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app **Calculator: Sci, EMI, Age, Int**
3. In the left menu: **Release** → **Production** (or **Testing** if you use internal/closed track first)
4. Click **Create new release**
5. **Upload** the new AAB (`app-release.aab`)
6. Add **Release name** (e.g. “1.2 – Scientific, EMI, Currency, Date”)
7. Add **Release notes** (what’s new for users)
8. Click **Save** → **Review release** → **Start rollout to Production** (or to your chosen track)

## 4. Store listing (if you changed it)

If you updated store text or graphics:

- Go to **Store presence** → **Main store listing**
- Update **Short description**, **Full description**, **Graphics** (screenshots, feature graphic, icon) as needed
- Save

## 5. After rollout

- **Production** rollout can take from a few hours to a few days to reach all users.
- Check **Release** → **Production** (or your track) for status and any errors.
- Use **Pre-launch report** and **Android Vitals** to spot issues.

## Checklist before publishing

- [ ] `versionCode` increased in `app/build.gradle`
- [ ] `versionName` updated (e.g. 1.2)
- [ ] Release AAB built and signed with your **original** keystore
- [ ] AAB uploaded in Play Console on the correct track
- [ ] Release notes filled in
- [ ] Store listing (and graphics) updated if needed
