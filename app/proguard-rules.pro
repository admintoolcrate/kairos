# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.kairos.** { *** Companion; }
-keepclasseswithmembers class app.kairos.** { kotlinx.serialization.KSerializer serializer(...); }
