package com.centralia.app.domain.greeting

import java.time.LocalTime


object TimeOfDayGreeting {

    enum class PartOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

    data class Greeting(val title: String, val subtitle: String, val partOfDay: PartOfDay)

    fun partOfDay(hour: Int): PartOfDay = when (hour) {
        in 5..11 -> PartOfDay.MORNING
        in 12..17 -> PartOfDay.AFTERNOON
        in 18..21 -> PartOfDay.EVENING
        else -> PartOfDay.NIGHT
    }


    fun firstName(displayName: String?): String? =
        displayName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotEmpty() }

    fun greeting(
        time: LocalTime = LocalTime.now(),
        displayName: String? = null
    ): Greeting {
        val part = partOfDay(time.hour)
        val salutation = when (part) {
            PartOfDay.MORNING -> "Good morning"
            PartOfDay.AFTERNOON -> "Good afternoon"
            PartOfDay.EVENING, PartOfDay.NIGHT -> "Good evening"
        }
        val subtitle = when (part) {
            PartOfDay.MORNING -> "Start your day with something you saved."
            PartOfDay.AFTERNOON -> "A good moment to catch up on your shorts."
            PartOfDay.EVENING -> "Wind down with something from your library."
            PartOfDay.NIGHT -> "Up late? Your shorts are waiting."
        }
        val name = firstName(displayName)
        return Greeting(
            title = if (name != null) "$salutation, $name" else salutation,
            subtitle = subtitle,
            partOfDay = part
        )
    }
}
