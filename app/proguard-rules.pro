# مكتبة المواقيت تُستخدم بالانعكاس في بعض المسارات — نحافظ عليها كاملة (حجمها صغير)
-keep class com.batoulapps.adhan.** { *; }
# جسر JavaScript في شاشة الخريطة
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
-dontwarn org.json.**
