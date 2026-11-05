package com.calaf.app.demo

import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoState
import org.json.JSONObject
import java.time.LocalDate

/**
 * Twenty fictional people and one ready made guest member, so the demo looks alive in the first minute. Everyone is
 * made up, named with "(Demo)", flagged as a demo profile and given bundled illustrated avatars, never real photos.
 */
object DemoShowcase {
    const val GUEST_ID = "demo-guest"
    const val GUEST_EMAIL = "guest@calaf.test"

    internal class Person(
        val slug: String, val name: String, val woman: Boolean, val age: Int, val city: String, val country: String,
        val heritage: String, val bornIn: String, val job: String, val education: String, val languages: String,
        val interests: String, val values: String, val bio: String, val marital: String, val height: Int,
        val children: String, val wants: String, val relocation: String, val timeline: String,
        val religiosity: String, val prays: String,
    ) {
        val id get() = "demo-$slug"
        val email get() = "$slug@calaf.test"
    }

    internal val people = listOf(
        Person("sagal", "Sagal", true, 27, "London", "United Kingdom", "SOMALI", "Somalia", "Primary school teacher",
            "University degree", "Somali, English", "Reading, Baking, Walking", "Family, Honesty, Patience",
            "I teach year three and love a quiet Sunday with a book and a pot " +
                "of shaah. I want a calm home full of laughter.",
            "NEVER_MARRIED", 165, "NO", "YES", "OPEN", "WITHIN_YEAR", "PRACTISING", "USUALLY"),
        Person("abdi", "Abdi", false, 31, "Birmingham", "United Kingdom", "SOMALI", "United Kingdom",
            "Software engineer",
            "Master's degree", "Somali, English", "Football, Coding, Cooking", "Family, Ambition, Kindness",
            "I build apps by day and cook a very serious baasto by night. " +
                "Looking for someone kind to build a life with.",
            "NEVER_MARRIED", 178, "NO", "YES", "OPEN", "WITHIN_YEAR", "PRACTISING", "ALWAYS"),
        Person("ilhan", "Ilhan", true, 24, "Hargeisa", "Somaliland", "SOMALILAND", "Somaliland", "Pharmacist",
            "University degree", "Somali, English, Arabic", "Travel, Poetry, Volunteering", "Faith, Family, Learning",
            "I run a small pharmacy with my aunt and write poetry when the " +
                "shop is quiet. I hope to meet someone thoughtful.",
            "NEVER_MARRIED", 160, "NO", "YES", "NO", "ONE_TO_TWO_YEARS", "VERY_PRACTISING", "ALWAYS"),
        Person("warsame", "Warsame", false, 35, "Burao", "Somaliland", "SOMALILAND", "Somaliland", "Civil engineer",
            "University degree", "Somali, English", "Hiking, Building, Reading", "Honesty, Hard work, Family",
            "I design roads and bridges and spend weekends with my nephews. I " +
                "would like a partner who enjoys the outdoors.",
            "DIVORCED", 182, "YES", "OPEN", "OPEN", "WHEN_READY", "PRACTISING", "USUALLY"),
        Person("fartun", "Fartun", true, 29, "Mogadishu", "Somalia", "SOMALI", "Somalia", "Midwife",
            "Nursing diploma", "Somali, English, Italian", "Cooking, Gardening, Music", "Compassion, Faith, Family",
            "I care for mothers and new babies and I love my work. At home " +
                "I garden, cook and call my grandmother daily.",
            "NEVER_MARRIED", 168, "NO", "YES", "OPEN", "WITHIN_YEAR", "PRACTISING", "ALWAYS"),
        Person("jamal", "Jamal", false, 38, "Garowe", "Somalia", "SOMALI", "Somalia", "Business owner",
            "Diploma", "Somali, English, Arabic", "Business, Football, Travel", "Respect, Generosity, Faith",
            "I run a small import business and help young people start " +
                "theirs. I want a partner to share both work and rest.",
            "WIDOWED", 180, "YES", "OPEN", "NO", "WHEN_READY", "PRACTISING", "ALWAYS"),
        Person("deeqa", "Deeqa", true, 33, "Djibouti City", "Djibouti", "DJIBOUTIAN", "Djibouti", "Accountant",
            "University degree", "Somali, French, Arabic", "Travel, Swimming, Reading", "Honesty, Family, Learning",
            "I count numbers for a living and the sea is my favourite place. I " +
                "speak three languages and want to learn a fourth.",
            "NEVER_MARRIED", 170, "NO", "YES", "OPEN", "WITHIN_YEAR", "MODERATELY", "USUALLY"),
        Person("omar", "Omar", false, 28, "Djibouti City", "Djibouti", "DJIBOUTIAN", "Djibouti",
            "Port logistics manager",
            "University degree", "Somali, French, English", "Football, Fishing, Cooking", "Family, Humour, Faith",
            "I keep ships moving and I fish whenever I can. I am looking for " +
                "someone warm, curious and not afraid of the sea.",
            "NEVER_MARRIED", 176, "NO", "YES", "YES", "WITHIN_YEAR", "PRACTISING", "USUALLY"),
        Person("khadra", "Khadra", true, 26, "Addis Ababa", "Ethiopia", "ETHIOPIAN", "Ethiopia", "Graphic designer",
            "University degree", "Somali, Amharic, English", "Art, Coffee, Photography", "Creativity, Faith, Family",
            "I design logos for small businesses and drink too much coffee. I " +
                "want a partner who loves ideas and honest talk.",
            "NEVER_MARRIED", 163, "NO", "YES", "OPEN", "ONE_TO_TWO_YEARS", "PRACTISING", "SOMETIMES"),
        Person("bilal", "Bilal", false, 41, "Dire Dawa", "Ethiopia", "ETHIOPIAN", "Ethiopia",
            "Secondary school headteacher",
            "Master's degree", "Somali, Amharic, English", "Teaching, Chess, Gardening", "Patience, Learning, Family",
            "I lead a school and still teach one maths class because I enjoy " +
                "it. I hope to find a gentle, steady partner.",
            "DIVORCED", 179, "YES", "OPEN", "OPEN", "WHEN_READY", "PRACTISING", "ALWAYS"),
        Person("hibo", "Hibo", true, 30, "Nairobi", "Kenya", "KENYAN", "Kenya", "Dentist",
            "Dental degree", "Somali, Swahili, English", "Running, Baking, Podcasts", "Health, Faith, Family",
            "I fix smiles for a living and run in the park most mornings. I " +
                "want someone who laughs easily and plans well.",
            "NEVER_MARRIED", 167, "NO", "YES", "OPEN", "WITHIN_YEAR", "PRACTISING", "USUALLY"),
        Person("idris", "Idris", false, 34, "Mombasa", "Kenya", "KENYAN", "Kenya", "Tour guide",
            "Diploma", "Somali, Swahili, English", "Travel, Photography, Football", "Kindness, Adventure, Family",
            "I show visitors the coast and its history. I would like a " +
                "partner who enjoys exploring new places together.",
            "NEVER_MARRIED", 177, "NO", "YES", "YES", "ONE_TO_TWO_YEARS", "MODERATELY", "SOMETIMES"),
        Person("nimco", "Nimco", true, 36, "Minneapolis", "United States", "SOMALI", "Somalia", "Hospital nurse",
            "Nursing degree", "Somali, English", "Cooking, Family time, Walking", "Faith, Compassion, Honesty",
            "I work night shifts on a hospital ward and love a big family meal " +
                "on my days off. I want a steady, caring partner.",
            "DIVORCED", 164, "YES", "OPEN", "OPEN", "WHEN_READY", "PRACTISING", "ALWAYS"),
        Person("samatar", "Samatar", false, 45, "Columbus", "United States", "SOMALI", "Somalia",
            "Transport company owner",
            "Diploma", "Somali, English, Arabic", "Mentoring, Football, Reading", "Faith, Generosity, Family",
            "I started with one truck and now help others get started too. I " +
                "am ready for a partner and a calm, happy home.",
            "WIDOWED", 183, "YES", "OPEN", "NO", "WHEN_READY", "PRACTISING", "ALWAYS"),
        Person("ayaan", "Ayaan", true, 22, "Toronto", "Canada", "SOMALI", "Canada", "Student of public health",
            "Studying for a degree", "Somali, English, French", "Debating, " +
                "Travel, Volunteering", "Learning, Faith, Family",
            "I am finishing my degree and volunteer at a community clinic. One " +
                "day I would like a home full of books and guests.",
            "NEVER_MARRIED", 161, "NO", "YES", "OPEN", "ONE_TO_TWO_YEARS", "PRACTISING", "USUALLY"),
        Person("farah", "Farah", false, 29, "Ottawa", "Canada", "SOMALI", "Somalia", "Data analyst",
            "Master's degree", "Somali, English, French", "Hockey, Cooking, Podcasts", "Family, Curiosity, Faith",
            "I find stories in numbers and I am learning to skate badly. I " +
                "want someone who enjoys a good conversation.",
            "NEVER_MARRIED", 175, "NO", "YES", "YES", "WITHIN_YEAR", "PRACTISING", "USUALLY"),
        Person("mariam", "Mariam", true, 40, "Stockholm", "Sweden", "SOMALI", "Somalia", "Social worker",
            "University degree", "Somali, Swedish, English", "Walking, Cooking, Reading", "Compassion, Faith, Family",
            "I support families through hard times and recharge in the woods " +
                "near my home. I hope to meet a kind, grounded man.",
            "DIVORCED", 166, "YES", "OPEN", "NO", "WHEN_READY", "PRACTISING", "ALWAYS"),
        Person("liban", "Liban", false, 32, "Malmö", "Sweden", "SOMALI", "Sweden", "Electrician",
            "Vocational training", "Somali, Swedish, English", "Cycling, Football, Cooking", "Hard work, Family, Faith",
            "I wire up new homes and cycle everywhere in all weather. I want a partner to build a warm home with.",
            "NEVER_MARRIED", 181, "NO", "YES", "OPEN", "WITHIN_YEAR", "PRACTISING", "USUALLY"),
        Person("rahma", "Rahma", true, 45, "Manchester", "United Kingdom", "SOMALI", "Somalia", "University lecturer",
            "PhD", "Somali, English, Arabic", "Teaching, Writing, Travel", "Learning, Faith, Family",
            "I teach history and write on weekends. My grown children are my pride " +
                "and I would like a companion for the next chapter.",
            "DIVORCED", 169, "YES", "NO", "NO", "WHEN_READY", "PRACTISING", "ALWAYS"),
        Person("guled", "Guled", false, 21, "Cardiff", "United Kingdom", "SOMALI", "United Kingdom", "Apprentice chef",
            "Apprenticeship", "Somali, English, Welsh", "Cooking, Football, Gaming", "Family, Respect, Ambition",
            "I am training to be a chef and dream of opening a small family " +
                "restaurant. I am looking for someone sincere.",
            "NEVER_MARRIED", 180, "NO", "YES", "YES", "ONE_TO_TWO_YEARS", "MODERATELY", "SOMETIMES"),
    )

    /** Adds the showcase once. Safe to call on every start: it does nothing if the showcase is already there. */
    fun seedShowcase(store: DemoStore, today: LocalDate = LocalDate.now()) {
        if (store.table("meta").optBoolean("showcase")) return
        people.forEachIndexed { index, person ->
            store.table("users").put(
                person.id,
                JSONObject().put("email", person.email).put("seed", true).put("suspended", false).put("demo", true),
            )
            store.table("profiles").put(person.id, profile(person, today, index))
            addAvatars(store, person.id, index, 2 + index % 3)
        }
        seedGuest(store, today)
        store.table("meta").put("showcase", true)
        store.save()
    }

    private fun profile(p: Person, today: LocalDate, index: Int) = JSONObject()
        .put("name", "${p.name} (Demo)").put("demo", true)
        .put("birthDate", today.minusYears(p.age.toLong()).minusDays(30L + index * 7).toString())
        .put("city", p.city).put("country", p.country).put("culture", "Somali / East African")
        .put("languages", p.languages).put("biography", p.bio).put("gender", if (p.woman) "WOMAN" else "MAN")
        .put("occupation", p.job).put("education", p.education).put("interests", p.interests)
        .put("values", p.values).put("children", p.children).put("wantsChildren", p.wants).put("smoking", "NO")
        .put("relocation", p.relocation).put("timeline", p.timeline).put("maritalStatus", p.marital)
        .put("heightCm", p.height).put("countryOfBirth", p.bornIn)
        // Fictional members consent to sharing faith and heritage, so the demo can show those fields.
        .put("sensitiveConsent", true).put("religion", "Islam").put("religiosity", p.religiosity)
        .put("prays", p.prays).put("heritage", p.heritage)

    private fun addAvatars(store: DemoStore, id: String, index: Int, count: Int) {
        val rows = (0 until count).associate { n ->
            "$id-photo-$n" to DemoPhotoRows.newRow(id, DemoAvatars.key(index * 3 + n))
        }
        val photos = rows.keys.mapIndexed { n, photoId -> Photo(photoId, n, n == 0, PhotoState.APPROVED) }
        DemoPhotoRows.write(store, photos, rows)
    }

    /**
     * "Try the demo" signs in here. The guest already has two matches, a conversation under way, one compliment waiting
     * and two more likes, so everything in the app has something to show within thirty seconds.
     */
    private fun seedGuest(store: DemoStore, today: LocalDate) {
        store.table("users").put(
            GUEST_ID,
            JSONObject().put("email", GUEST_EMAIL).put("seed", true).put("suspended", false).put("demo", true),
        )
        store.table("profiles").put(
            GUEST_ID,
            JSONObject().put("name", "Demo Guest").put("demo", true)
                .put("birthDate", today.minusYears(28).minusDays(40).toString())
                .put("city", "London").put("country", "United Kingdom").put("culture", "Somali / East African")
                .put("languages", "Somali, English").put("gender", "WOMAN").put("occupation", "Product designer")
                .put("education", "University degree").put("interests", "Reading, Walking, Cooking")
                .put("values", "Family, Honesty")
                    .put("biography", "A made up guest profile for trying Calaf. Nothing here is real.")
                .put("children", "NO").put("wantsChildren", "YES").put("smoking", "NO").put("relocation", "OPEN")
                .put("timeline", "WITHIN_YEAR").put("maritalStatus", "NEVER_MARRIED").put("heightCm", 166)
                .put("countryOfBirth", "United Kingdom").put("sensitiveConsent", true).put("religion", "Islam")
                .put("religiosity", "PRACTISING").put("prays", "USUALLY").put("heritage", "SOMALI"),
        )
        addAvatars(store, GUEST_ID, 7, 3)
        val chat = store.createMatch(GUEST_ID, "demo-abdi")
        store.createMatch(GUEST_ID, "demo-jamal")
        val now = System.currentTimeMillis()
        listOf(
            Triple("demo-abdi", "Salaam! I really liked your profile.", now - 2 * HOUR),
            Triple(GUEST_ID, "Walaykum salaam, thank you. That is kind of you.", now - 2 * HOUR + 10 * MINUTE),
            Triple("demo-abdi", "What do you enjoy doing at the weekend?", now - HOUR),
        ).forEachIndexed { n, (sender, text, time) ->
            store.table("messages").put(
                "demo-guest-chat-$n", JSONObject().put("pair", chat).put("sender", sender).put("text",
                    text).put("time", time),
            )
        }
        store.addNotification(GUEST_ID, "MESSAGE", chat)
        // One compliment and two plain likes are waiting in Liked you.
        store.table("choices").put("demo-warsame_$GUEST_ID", true)
        store.table("notes").put(
            "demo-warsame_$GUEST_ID",
            JSONObject().put("note", "Salaam, your profile was a joy to read. Would you like to talk?"),
        )
        store.table("choices").put("demo-omar_$GUEST_ID", true)
        store.table("choices").put("demo-bilal_$GUEST_ID", true)
    }

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
}
