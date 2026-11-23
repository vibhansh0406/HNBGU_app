package `in`.campusone.app.data.repository

import `in`.campusone.app.data.local.entity.DirectoryContactEntity

/**
 * Curated public directory data sourced from HNBGU's official campus, officer,
 * computer-centre and library pages. Contact details are intentionally omitted
 * where the university does not publish a reliable public value.
 *
 * Official sources:
 * - https://www.hnbgu.ac.in/index.php/srinagar-campus
 * - https://hnbgu.ac.in/rti-list-of-officers
 * - https://hnbgu.ac.in/computer-center
 * - https://library.hnbgu.ac.in/pages.pl?p=elibrary
 */
object HnbguDirectorySeed {
    fun contacts(collegeId: String): List<DirectoryContactEntity> {
        fun department(id: String, school: String, name: String, campus: String = "Birla Campus, Srinagar") =
            DirectoryContactEntity(
                id = "official_hnbgu_dept_$id",
                collegeId = collegeId,
                category = "DEPARTMENT",
                department = school,
                name = name,
                designation = "Academic department",
                phone = "",
                email = "",
                officeLocation = campus,
                officeHours = "See the official HNBGU campus page for current office details",
                isEmergencyOrImportant = false
            )

        fun facility(id: String, name: String, department: String, location: String, details: String) =
            DirectoryContactEntity(
                id = "official_hnbgu_facility_$id",
                collegeId = collegeId,
                category = "FACILITY",
                department = department,
                name = name,
                designation = "University facility",
                phone = "",
                email = "",
                officeLocation = location,
                officeHours = details,
                isEmergencyOrImportant = true
            )

        return listOf(
            DirectoryContactEntity(
                id = "official_hnbgu_registrar",
                collegeId = collegeId,
                category = "IMPORTANT_OFFICE",
                department = "University Administration",
                name = "Registrar Office",
                designation = "University administration",
                phone = "01346-252143",
                email = "registrar.hnbgu@gmail.com",
                officeLocation = "Birla Campus, Srinagar",
                officeHours = "Use the official contact page for current timings",
                isEmergencyOrImportant = true
            ),
            DirectoryContactEntity(
                id = "official_hnbgu_controller_exams",
                collegeId = collegeId,
                category = "IMPORTANT_OFFICE",
                department = "Examinations",
                name = "Controller of Examinations",
                designation = "Controller of Examination",
                phone = "7895360001",
                email = "",
                officeLocation = "University Administration, Srinagar",
                officeHours = "Official officer listing; check the university site for current timings",
                isEmergencyOrImportant = true
            ),
            DirectoryContactEntity(
                id = "official_hnbgu_chief_proctor",
                collegeId = collegeId,
                category = "IMPORTANT_OFFICE",
                department = "Student Discipline",
                name = "Chief Proctor Office",
                designation = "Chief Proctor",
                phone = "8650050639",
                email = "",
                officeLocation = "Birla Campus, Srinagar",
                officeHours = "Official officer listing; check the university site for current timings",
                isEmergencyOrImportant = true
            ),
            DirectoryContactEntity(
                id = "official_hnbgu_chauras_director",
                collegeId = collegeId,
                category = "IMPORTANT_OFFICE",
                department = "Campus Administration",
                name = "Chauras Campus Director",
                designation = "Director, Chauras Campus",
                phone = "9412079426",
                email = "",
                officeLocation = "Chauras Campus, Srinagar",
                officeHours = "University officer listing last updated 30 April 2025; verify before calling",
                isEmergencyOrImportant = true
            ),
            DirectoryContactEntity(
                id = "official_hnbgu_bgr_director",
                collegeId = collegeId,
                category = "IMPORTANT_OFFICE",
                department = "Campus Administration",
                name = "B.G.R. Campus Director",
                designation = "Director, B.G.R. Campus",
                phone = "9412079434",
                email = "",
                officeLocation = "B.G.R. Campus, Pauri",
                officeHours = "University officer listing last updated 30 April 2025; verify before calling",
                isEmergencyOrImportant = true
            ),
            DirectoryContactEntity(
                id = "official_hnbgu_srt_director",
                collegeId = collegeId,
                category = "IMPORTANT_OFFICE",
                department = "Campus Administration",
                name = "S.R.T. Campus Director",
                designation = "Director, Swami Ram Tirth Campus",
                phone = "8755551936",
                email = "",
                officeLocation = "S.R.T. Campus, Tehri",
                officeHours = "University officer listing last updated 30 April 2025; verify before calling",
                isEmergencyOrImportant = true
            ),
            DirectoryContactEntity(
                id = "official_hnbgu_education_contact",
                collegeId = collegeId,
                category = "FACULTY",
                department = "School of Education",
                name = "Department of Education Office",
                designation = "Department contact",
                phone = "01346-297423",
                email = "edu.hnbgu@gmail.com",
                officeLocation = "Birla Campus, Srinagar",
                officeHours = "Contact details from the official department page",
                isEmergencyOrImportant = false
            ),
            DirectoryContactEntity(
                id = "official_hnbgu_library_email",
                collegeId = collegeId,
                category = "IMPORTANT_OFFICE",
                department = "Central Library",
                name = "HNBGU Central Library / E-Library",
                designation = "Library help and e-resource access",
                phone = "",
                email = "library73@rediffmail.com",
                officeLocation = "Central Library, Srinagar; library system also serves other campuses",
                officeHours = "KNIMBUS e-Library provides remote access; contact the library for login help",
                isEmergencyOrImportant = true
            ),
            DirectoryContactEntity(
                id = "official_hnbgu_computer_centre",
                collegeId = collegeId,
                category = "FACILITY",
                department = "Central IT Services",
                name = "University Computer Centre",
                designation = "Campus network, Wi-Fi and digital services",
                phone = "",
                email = "web.portal@hnbgu.ac.in",
                officeLocation = "Birla / Chauras and university campuses",
                officeHours = "HNBGU lists campus Wi-Fi at Srinagar, Pauri and Tehri; use official Computer Centre page",
                isEmergencyOrImportant = true
            ),
            facility(
                id = "wifi",
                name = "Campus Wi-Fi",
                department = "Central IT Services",
                location = "Birla, Chauras, B.G.R. Pauri and S.R.T. Tehri campuses",
                details = "Wireless network access is listed by HNBGU Computer Centre; ask campus IT staff for current student access steps"
            ),
            facility(
                id = "usic",
                name = "University Science Instrumentation Centre (USIC)",
                department = "Research and Instrumentation",
                location = "University campuses, Srinagar",
                details = "Shared scientific instrumentation facility for research; ask your department about equipment access"
            ),
            facility(
                id = "career_guidance",
                name = "Employment Information and Guidance Centre",
                department = "Career Guidance",
                location = "HNBGU, Srinagar",
                details = "Career information and guidance facility; check the official university website for current services"
            ),
            facility(
                id = "sports",
                name = "University Sports Board",
                department = "Sports and Student Activities",
                location = "HNBGU, Srinagar",
                details = "University sports activities and facilities; contact the university for current schedules"
            ),
            facility(
                id = "guest_house",
                name = "University Guest House",
                department = "Campus Facilities",
                location = "HNBGU, Srinagar",
                details = "Availability and booking are managed by the university; confirm directly before visiting"
            ),
            facility(
                id = "elibrary",
                name = "HNBGU E-Library (KNIMBUS)",
                department = "Central Library / Digital Resources",
                location = "Online — hnbgu.knimbus.com",
                details = "Remote access to e-books, e-journals and databases; contact Central Library for login assistance"
            ),
            facility(
                id = "central_library",
                name = "Central Library",
                department = "Library System",
                location = "Srinagar, with campus libraries at Chauras, Pauri and Tehri",
                details = "Library services and digital resources are listed on the official HNBGU Library website"
            ),
            facility(
                id = "campus_main",
                name = "Birla Main Campus",
                department = "Campus",
                location = "Srinagar, Uttarakhand",
                details = "Part of Birla Campus; see the official campus page for location and departments"
            ),
            facility(
                id = "campus_chauras",
                name = "Chauras Campus",
                department = "Campus",
                location = "Across the Alaknanda river from Birla Main Campus",
                details = "HNBGU lists Chauras approximately 4.5 km from the Main Campus, connected by a bridge"
            ),
            facility(
                id = "campus_bgr",
                name = "B.G.R. Campus",
                department = "Campus",
                location = "Pauri, Uttarakhand",
                details = "Official HNBGU campus; see the B.G.R. campus page for departments and contact details"
            ),
            facility(
                id = "campus_srt",
                name = "S.R.T. Campus",
                department = "Campus",
                location = "Badshahithaul, Tehri, Uttarakhand",
                details = "Official HNBGU campus; see the S.R.T. campus page for departments and contact details"
            ),

            department("forestry", "School of Agriculture and Allied Sciences", "Forestry & Natural Resources"),
            department("happrc", "School of Agriculture and Allied Sciences", "High Altitude Plant Physiology Research Centre"),
            department("horticulture", "School of Agriculture and Allied Sciences", "Horticulture"),
            department("rural_technology", "School of Agriculture and Allied Sciences", "Rural Technology"),
            department("seed_science", "School of Agriculture and Allied Sciences", "Seed Science & Technology"),

            department("chemistry", "School of Sciences", "Chemistry"),
            department("home_science", "School of Sciences", "Home Science"),
            department("mathematics", "School of Sciences", "Mathematics"),
            department("physics", "School of Sciences", "Physics"),
            department("pharmaceutical_science", "School of Sciences", "Pharmaceutical Science"),
            department("pharmaceutical_chemistry", "School of Sciences", "Pharmaceutical Chemistry"),
            department("statistics", "School of Sciences", "Statistics"),

            department("defence", "School of Earth Sciences", "Defence & Strategic Studies"),
            department("geology", "School of Earth Sciences", "Geology"),
            department("geography", "School of Earth Sciences", "Geography"),
            department("remote_sensing", "School of Earth Sciences", "Remote Sensing and GIS"),

            department("education", "School of Education", "Education"),
            department("naturopathy", "School of Education", "Naturopathy & Yoga"),
            department("physical_education", "School of Education", "Physical Education"),

            department("computer_science", "School of Engineering & Technology", "Computer Science & Engineering"),
            department("ece", "School of Engineering & Technology", "Electronic & Communication Engineering"),
            department("instrumentation", "School of Engineering & Technology", "Instrumentation Engineering"),
            department("mechanical", "School of Engineering & Technology", "Mechanical Engineering"),
            department("information_technology", "School of Engineering & Technology", "Information Technology"),

            department("drawing_painting", "School of Arts, Communication and Languages", "Drawing and Painting"),
            department("journalism", "School of Arts, Communication and Languages", "Centre for Journalism & Mass Communication"),
            department("english", "School of Arts, Communication and Languages", "English / Modern European Languages"),
            department("hindi", "School of Arts, Communication and Languages", "Hindi and Modern Indian Languages"),
            department("library_science", "School of Arts, Communication and Languages", "Library & Information Science"),
            department("music", "School of Arts, Communication and Languages", "Music"),
            department("sanskrit", "School of Arts, Communication and Languages", "Sanskrit"),

            department("anthropology", "School of Humanities and Social Sciences", "Anthropology"),
            department("economics", "School of Humanities and Social Sciences", "Economics"),
            department("history", "School of Humanities and Social Sciences", "History"),
            department("philosophy", "School of Humanities and Social Sciences", "Philosophy"),
            department("political_science", "School of Humanities and Social Sciences", "Political Science"),
            department("psychology", "School of Humanities and Social Sciences", "Psychology"),
            department("sociology", "School of Humanities and Social Sciences", "Sociology & Social Work"),

            department("botany_microbiology", "School of Life Sciences", "Botany & Microbiology"),
            department("biochemistry", "School of Life Sciences", "Bio-Chemistry"),
            department("environmental_sciences", "School of Life Sciences", "Environmental Sciences"),
            department("zoology", "School of Life Sciences", "Zoology"),
            department("biotechnology", "School of Life Sciences", "Biotechnology"),
            department("aquatic_biodiversity", "School of Life Sciences", "Himalayan Aquatic Biodiversity"),

            department("commerce", "School of Commerce", "Commerce"),
            department("business_management", "School of Management", "Business Management"),
            department("mountain_tourism", "School of Management", "Centre for Mountain Tourism & Hospitality Studies")
        )
    }
}
