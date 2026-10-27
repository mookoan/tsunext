-keepnames class com.tom_roush.** { *; }
# JPEG 2000 bitmap decoding is optional and unused by text extraction.
-dontwarn com.gemalto.jp2.JP2Decoder
