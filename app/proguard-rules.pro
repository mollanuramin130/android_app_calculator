# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep Play Core (in-app update / review)
-keep class com.google.android.play.** { *; }

# Keep Activities
-keep public class * extends android.app.Activity

# Keep Fragments
-keep public class * extends androidx.fragment.app.Fragment

# Keep View bindings / models
-keepclassmembers class * {
    public <init>(...);
}

# Keep Serializable / Parcelable
-keep class * implements java.io.Serializable { *; }
-keep class * implements android.os.Parcelable { *; }

# PDFBox-Android & AndroidSVG (libraries use reflection; keep without changing app code)
-keep class org.apache.pdfbox.** { *; }
-keep class com.tom_roush.** { *; }
-dontwarn org.apache.pdfbox.**
-dontwarn com.tom_roush.**
-keep class com.caverock.androidsvg.** { *; }

# AppCompat activities (manifest-registered; extends ComponentActivity, not android.app.Activity)
-keep public class * extends androidx.appcompat.app.AppCompatActivity { <init>(...); }
