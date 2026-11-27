# Keep kotlinx.serialization generated serializers for the mock JSON store.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.centralia.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.centralia.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Sprint 4: release builds now run R8 (minify + shrink resources).
# kotlinx.serialization: keep the generated serializers of every @Serializable
# class (the API models, the offline JSON copy, the outbox, Trends, Insights).
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, Signature
-keep,includedescriptorclasses class com.centralia.app.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.centralia.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# Enums sent as JSON strings (@SerialName) and sealed outbox actions.
-keepclassmembers enum com.centralia.app.** { *; }
# Room, OkHttp, Coil, DataStore and Compose ship their own consumer rules.
