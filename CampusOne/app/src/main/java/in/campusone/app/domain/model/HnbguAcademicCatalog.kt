package `in`.campusone.app.domain.model

/**
 * Shared HNBGU campus and course options used by onboarding and profile correction.
 * Course IDs are stable keys already used by existing CampusOne profiles.
 */
object HnbguAcademicCatalog {
    val campuses = listOf(
        "Birla Campus, Srinagar (Garhwal)",
        "Chauras Campus, Srinagar (Garhwal)"
    )

    val courses = listOf(
        "bsc_cs" to "B.Sc. (Hons) Computer Science",
        "bsc_life" to "B.Sc. (Hons) Life Science",
        "bsc_phy" to "B.Sc. (Hons) Physics",
        "bsc_chem" to "B.Sc. (Hons) Chemistry",
        "bsc_math" to "B.Sc. (Hons) Mathematics",
        "bsc_bot" to "B.Sc. (Hons) Botany",
        "bsc_zoo" to "B.Sc. (Hons) Zoology",
        "bsc_micro" to "B.Sc. (Hons) Microbiology",
        "bsc_biochem" to "B.Sc. (Hons) Biochemistry",
        "bsc_biotech" to "B.Sc. (Hons) Biotechnology",
        "bsc_geology" to "B.Sc. (Hons) Geology",
        "bsc_stats" to "B.Sc. (Hons) Statistics",
        "bsc_geog" to "B.Sc. (Hons) Geography",
        "bsc_anthro" to "B.Sc. (Hons) Anthropology",
        "bsc_econ" to "B.Sc. (Hons) Economics",
        "bsc_defence" to "B.Sc. (Hons) Defence Studies",
        "bsc_yogic" to "B.Sc. (Hons) Yogic Science",
        "bsc_forestry" to "B.Sc. Forestry",
        "bsc_horti" to "B.Sc. Horticulture",
        "ba_hons" to "B.A. (Hons) Arts & Humanities",
        "ba_jmc" to "B.A. (Hons) Journalism & Mass Comm.",
        "bcom" to "B.Com. (Hons)",
        "bba" to "BBA",
        "btech_cse" to "B.Tech. Computer Science & Engg.",
        "btech_ece" to "B.Tech. Electronics & Comm. Engg.",
        "btech_it" to "B.Tech. Information Technology",
        "btech_me" to "B.Tech. Mechanical Engineering",
        "btech_ie" to "B.Tech. Instrumentation Engineering",
        "bpharm" to "B.Pharm.",
        "bhm" to "BHM & Catering Technology",
        "bed" to "B.Ed.",
        "bped" to "B.P.Ed.",
        "blib" to "B.Lib.I.Sc."
    )
}
