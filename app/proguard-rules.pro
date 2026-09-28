# R8 / ProGuard Optimization & Obfuscation improvements for Google Play
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'com.christianjoel.geophoto.internal'

# Keep essential annotations and attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep app core packages
-keep class com.christianjoel.geophoto.data.** { *; }
-keep class com.christianjoel.geophoto.viewmodel.** { *; }
