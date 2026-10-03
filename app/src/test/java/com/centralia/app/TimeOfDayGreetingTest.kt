package com.centralia.app

import com.centralia.app.domain.greeting.TimeOfDayGreeting
import com.centralia.app.domain.greeting.TimeOfDayGreeting.PartOfDay
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class TimeOfDayGreetingTest {
    @Test
    fun boundariesFollowThePhoneClock() {
        assertEquals(PartOfDay.NIGHT, TimeOfDayGreeting.partOfDay(4))
        assertEquals(PartOfDay.MORNING, TimeOfDayGreeting.partOfDay(5))
        assertEquals(PartOfDay.MORNING, TimeOfDayGreeting.partOfDay(11))
        assertEquals(PartOfDay.AFTERNOON, TimeOfDayGreeting.partOfDay(12))
        assertEquals(PartOfDay.AFTERNOON, TimeOfDayGreeting.partOfDay(17))
        assertEquals(PartOfDay.EVENING, TimeOfDayGreeting.partOfDay(18))
        assertEquals(PartOfDay.EVENING, TimeOfDayGreeting.partOfDay(21))
        assertEquals(PartOfDay.NIGHT, TimeOfDayGreeting.partOfDay(22))
        assertEquals(PartOfDay.NIGHT, TimeOfDayGreeting.partOfDay(0))
    }

    @Test
    fun englishGreetingUsesTheFirstName() {
        val greeting = TimeOfDayGreeting.greeting(LocalTime.of(15, 30), "David Caro")
        assertEquals("Good afternoon, David", greeting.title)
        assertEquals("A good moment to catch up on your shorts.", greeting.subtitle)
    }

    @Test
    fun alwaysEnglishWhateverThePhoneLanguage() {
        assertEquals("Good morning, María", TimeOfDayGreeting.greeting(LocalTime.of(7, 0), "María").title)
        assertEquals("Good afternoon", TimeOfDayGreeting.greeting(LocalTime.of(13, 0), "  ").title)
        assertEquals("Good evening, Ana", TimeOfDayGreeting.greeting(LocalTime.of(23, 10), "Ana").title)
    }

    @Test
    fun lateNightSaysGoodEvening() {
        assertEquals("Good evening", TimeOfDayGreeting.greeting(LocalTime.of(2, 0), null).title)
        assertNull(TimeOfDayGreeting.firstName(""))
    }
}
