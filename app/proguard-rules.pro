# Keep kotlinx.serialization generated serializers for the mock JSON store.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.centralia.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.centralia.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
