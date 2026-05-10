# Consumer ProGuard rules for `@poppy/client-android`.
#
# Keep kotlinx.serialization metadata for the @Serializable Component sealed
# class hierarchy. Without this, R8 can strip the synthesized Companion
# serializers and decoding fails at runtime.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class dev.poppy.android.** {
    *** Companion;
}
-keepclasseswithmembers class dev.poppy.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
