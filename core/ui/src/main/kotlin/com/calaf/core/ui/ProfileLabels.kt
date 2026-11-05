package com.calaf.core.ui

import androidx.annotation.StringRes
import com.calaf.core.model.Heritage
import com.calaf.core.model.MaritalStatus
import com.calaf.core.model.MarriageTimeline
import com.calaf.core.model.PrayerHabit
import com.calaf.core.model.Religiosity

/** Display text for profile choices shown to other members. Null means there is nothing to show. */
@StringRes
fun MarriageTimeline.labelRes(): Int = when (this) {
    MarriageTimeline.WHEN_READY -> R.string.lbl_timeline_when_ready
    MarriageTimeline.WITHIN_YEAR -> R.string.lbl_timeline_within_year
    MarriageTimeline.ONE_TO_TWO_YEARS -> R.string.lbl_timeline_two_years
}

@StringRes
fun MaritalStatus.labelRes(): Int? = when (this) {
    MaritalStatus.NEVER_MARRIED -> R.string.lbl_marital_never
    MaritalStatus.DIVORCED -> R.string.lbl_marital_divorced
    MaritalStatus.WIDOWED -> R.string.lbl_marital_widowed
    MaritalStatus.UNSPECIFIED -> null
}

@StringRes
fun Heritage.labelRes(): Int? = when (this) {
    Heritage.SOMALI -> R.string.lbl_heritage_somali
    Heritage.SOMALILAND -> R.string.lbl_heritage_somaliland
    Heritage.ETHIOPIAN -> R.string.lbl_heritage_ethiopian
    Heritage.ERITREAN -> R.string.lbl_heritage_eritrean
    Heritage.DJIBOUTIAN -> R.string.lbl_heritage_djiboutian
    Heritage.KENYAN -> R.string.lbl_heritage_kenyan
    Heritage.SUDANESE -> R.string.lbl_heritage_sudanese
    Heritage.OTHER -> R.string.lbl_heritage_other
    Heritage.UNSPECIFIED -> null
}

@StringRes
fun Religiosity.labelRes(): Int? = when (this) {
    Religiosity.VERY_PRACTISING -> R.string.lbl_relig_very
    Religiosity.PRACTISING -> R.string.lbl_relig_practising
    Religiosity.MODERATELY -> R.string.lbl_relig_moderately
    Religiosity.NOT_PRACTISING -> R.string.lbl_relig_not
    Religiosity.UNSPECIFIED -> null
}

@StringRes
fun PrayerHabit.labelRes(): Int? = when (this) {
    PrayerHabit.ALWAYS -> R.string.lbl_prays_always
    PrayerHabit.USUALLY -> R.string.lbl_prays_usually
    PrayerHabit.SOMETIMES -> R.string.lbl_prays_sometimes
    PrayerHabit.NEVER -> R.string.lbl_prays_never
    PrayerHabit.UNSPECIFIED -> null
}

@StringRes
fun com.calaf.core.model.Answer.labelRes(): Int? = when (this) {
    com.calaf.core.model.Answer.YES -> R.string.lbl_answer_yes
    com.calaf.core.model.Answer.NO -> R.string.lbl_answer_no
    com.calaf.core.model.Answer.OPEN -> R.string.lbl_answer_open
    com.calaf.core.model.Answer.UNSPECIFIED -> null
}
