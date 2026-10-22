# Preserve JNI method names without pinning the full downloader implementation.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
