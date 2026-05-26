# R8/Proguard rules for release builds.
# Refined in later sprints. Hilt + Moshi + Room rules are auto-included from libs.
-keep class de.healthforge.** { *; }
-keep class de.healthforge.data.network.dto.** { *; }
