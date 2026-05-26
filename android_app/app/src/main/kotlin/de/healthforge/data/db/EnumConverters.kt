package de.healthforge.data.db

import androidx.room.TypeConverter
import de.healthforge.data.db.entities.ActivityLevel
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.BiologicalSex
import de.healthforge.data.db.entities.DietGoal
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.db.entities.HistamineSensitivity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.ReminderFrequency

/** Stores enums as their `name` string. Nullable variants preserve `null` round-trip. */
class EnumConverters {
    @TypeConverter fun bioSexToString(v: BiologicalSex?): String? = v?.name
    @TypeConverter fun stringToBioSex(v: String?): BiologicalSex? = v?.let(BiologicalSex::valueOf)

    @TypeConverter fun activityToString(v: ActivityLevel?): String? = v?.name
    @TypeConverter fun stringToActivity(v: String?): ActivityLevel? = v?.let(ActivityLevel::valueOf)

    @TypeConverter fun goalToString(v: DietGoal?): String? = v?.name
    @TypeConverter fun stringToGoal(v: String?): DietGoal? = v?.let(DietGoal::valueOf)

    @TypeConverter fun histamineToString(v: HistamineSensitivity): String = v.name
    @TypeConverter fun stringToHistamine(v: String): HistamineSensitivity =
        HistamineSensitivity.valueOf(v)

    @TypeConverter fun allergenToString(v: AllergenType): String = v.name
    @TypeConverter fun stringToAllergen(v: String): AllergenType = AllergenType.valueOf(v)

    @TypeConverter fun fodmapToString(v: FodmapType): String = v.name
    @TypeConverter fun stringToFodmap(v: String): FodmapType = FodmapType.valueOf(v)

    @TypeConverter fun intakeSourceToString(v: IntakeSourceType): String = v.name
    @TypeConverter fun stringToIntakeSource(v: String): IntakeSourceType = IntakeSourceType.valueOf(v)

    @TypeConverter fun reminderFreqToString(v: ReminderFrequency): String = v.name
    @TypeConverter fun stringToReminderFreq(v: String): ReminderFrequency =
        ReminderFrequency.valueOf(v)
}
