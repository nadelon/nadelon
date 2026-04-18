# Media3 / ExoPlayer: reflection-based renderer / extension discovery.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# kotlinx.serialization: keep @Serializable classes and their generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static <1>$$serializer INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlin metadata used by reflection.
-keep class kotlin.Metadata { *; }

# Cast framework: OptionsProvider is loaded by name from AndroidManifest meta-data.
-keep class com.nadelon.app.cast.NadelonCastOptionsProvider { *; }
-keep class com.google.android.gms.cast.framework.** { *; }
-dontwarn com.google.android.gms.**
