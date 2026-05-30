# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.nadelon.chess.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.nadelon.chess.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
