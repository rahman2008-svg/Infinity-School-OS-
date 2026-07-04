package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class Repository(private val db: AppDatabase) {

    val schoolConfig: Flow<SchoolConfig?> = db.schoolConfigDao().getSchoolConfig()
    val academicYears: Flow<List<AcademicYear>> = db.academicYearDao().getAllAcademicYears()
    val activeAcademicYear: Flow<AcademicYear?> = db.academicYearDao().getActiveAcademicYear()
    val classes: Flow<List<ClassEntity>> = db.classDao().getAllClasses()
    val sections: Flow<List<SectionEntity>> = db.sectionDao().getAllSections()

    fun getSectionsForClass(classId: Int): Flow<List<SectionEntity>> =
        db.sectionDao().getSectionsForClass(classId)

    fun getStudentsForSection(sectionId: Int): Flow<List<StudentEntity>> =
        db.studentDao().getStudentsForSection(sectionId)

    fun getAllStudents(): Flow<List<StudentEntity>> =
        db.studentDao().getAllStudents()

    fun searchStudents(query: String): Flow<List<StudentEntity>> =
        db.studentDao().searchStudents(query)

    fun getAttendanceForDate(dateStr: String): Flow<List<AttendanceEntity>> =
        db.attendanceDao().getAttendanceForDate(dateStr)

    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceEntity>> =
        db.attendanceDao().getAttendanceForStudent(studentId)

    fun getAttendanceForSectionAndMonth(studentIds: List<Int>, monthPattern: String): Flow<List<AttendanceEntity>> =
        db.attendanceDao().getAttendanceForSectionAndMonth(studentIds, monthPattern)

    // Suspend operations
    suspend fun saveSchoolConfig(config: SchoolConfig) = withContext(Dispatchers.IO) {
        db.schoolConfigDao().insertSchoolConfig(config)
    }

    suspend fun insertAcademicYear(year: AcademicYear) = withContext(Dispatchers.IO) {
        db.academicYearDao().insertAcademicYear(year)
    }

    suspend fun setActiveYear(year: Int) = withContext(Dispatchers.IO) {
        db.academicYearDao().setActiveYear(year)
    }

    suspend fun insertClass(name: String): Long = withContext(Dispatchers.IO) {
        db.classDao().insertClass(ClassEntity(name = name))
    }

    suspend fun deleteClass(id: Int) = withContext(Dispatchers.IO) {
        db.classDao().deleteClass(id)
    }

    suspend fun insertSection(classId: Int, name: String): Long = withContext(Dispatchers.IO) {
        db.sectionDao().insertSection(SectionEntity(classId = classId, name = name))
    }

    suspend fun deleteSection(id: Int) = withContext(Dispatchers.IO) {
        db.sectionDao().deleteSection(id)
    }

    suspend fun insertStudent(student: StudentEntity): Long = withContext(Dispatchers.IO) {
        db.studentDao().insertStudent(student)
    }

    suspend fun deleteStudent(id: Int) = withContext(Dispatchers.IO) {
        db.studentDao().deleteStudent(id)
    }

    suspend fun saveAttendance(attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        db.attendanceDao().insertAttendance(attendance)
    }

    suspend fun saveAttendanceList(list: List<AttendanceEntity>) = withContext(Dispatchers.IO) {
        db.attendanceDao().insertAttendanceList(list)
    }

    suspend fun deleteAttendance(studentId: Int, dateStr: String) = withContext(Dispatchers.IO) {
        db.attendanceDao().deleteAttendance(studentId, dateStr)
    }

    /**
     * Backup whole database to JSON string.
     */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // 1. School Config
        val config = db.schoolConfigDao().getSchoolConfig().firstOrNull()
        if (config != null) {
            val cfgJson = JSONObject()
            cfgJson.put("name", config.name)
            cfgJson.put("logoPath", config.logoPath)
            cfgJson.put("eiin", config.eiin)
            cfgJson.put("code", config.code)
            cfgJson.put("type", config.type)
            cfgJson.put("address", config.address)
            cfgJson.put("phone", config.phone)
            cfgJson.put("email", config.email)
            cfgJson.put("website", config.website)
            cfgJson.put("principalName", config.principalName)
            cfgJson.put("academicStart", config.academicStart)
            cfgJson.put("academicEnd", config.academicEnd)
            root.put("school_config", cfgJson)
        }

        // 2. Academic Years
        val years = db.academicYearDao().getAllAcademicYears().firstOrNull() ?: emptyList()
        val yearsArray = JSONArray()
        for (y in years) {
            val yObj = JSONObject()
            yObj.put("year", y.year)
            yObj.put("isActive", y.isActive)
            yearsArray.put(yObj)
        }
        root.put("academic_years", yearsArray)

        // 3. Classes
        val classesList = db.classDao().getAllClasses().firstOrNull() ?: emptyList()
        val classesArray = JSONArray()
        for (c in classesList) {
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("name", c.name)
            classesArray.put(cObj)
        }
        root.put("classes", classesArray)

        // 4. Sections
        val sectionsList = db.sectionDao().getAllSections().firstOrNull() ?: emptyList()
        val sectionsArray = JSONArray()
        for (s in sectionsList) {
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("classId", s.classId)
            sObj.put("name", s.name)
            sectionsArray.put(sObj)
        }
        root.put("sections", sectionsArray)

        // 5. Students
        val studentsList = db.studentDao().getAllStudents().firstOrNull() ?: emptyList()
        val studentsArray = JSONArray()
        for (st in studentsList) {
            val stObj = JSONObject()
            stObj.put("id", st.id)
            stObj.put("studentIdCode", st.studentIdCode)
            stObj.put("rollNumber", st.rollNumber)
            stObj.put("name", st.name)
            stObj.put("fathersName", st.fathersName)
            stObj.put("mothersName", st.mothersName)
            stObj.put("dob", st.dob)
            stObj.put("gender", st.gender)
            stObj.put("bloodGroup", st.bloodGroup)
            stObj.put("classId", st.classId)
            stObj.put("sectionId", st.sectionId)
            stObj.put("mobile", st.mobile)
            stObj.put("guardianPhone", st.guardianPhone)
            stObj.put("address", st.address)
            stObj.put("admissionDate", st.admissionDate)
            stObj.put("photoUri", st.photoUri)
            studentsArray.put(stObj)
        }
        root.put("students", studentsArray)

        // 6. Attendance
        val attendanceList = mutableListOf<AttendanceEntity>()
        // We can query all attendance by using a blank/empty filter or just fetching from DB via a Query
        // For simplicity, let's clear up a query for all attendance, or just query it here.
        // Wait, the AttendanceDao has getAttendanceForDate, getAttendanceForStudent, etc.
        // Let's query studentIds and loop to fetch, or query directly:
        val stdIds = studentsList.map { it.id }
        if (stdIds.isNotEmpty()) {
            val allAtt = db.attendanceDao().getAttendanceForSectionAndMonth(stdIds, "").firstOrNull() ?: emptyList()
            attendanceList.addAll(allAtt)
        }
        val attArray = JSONArray()
        for (a in attendanceList) {
            val aObj = JSONObject()
            aObj.put("studentId", a.studentId)
            aObj.put("dateStr", a.dateStr)
            aObj.put("status", a.status)
            attArray.put(aObj)
        }
        root.put("attendance", attArray)

        root.toString(2)
    }

    /**
     * Restore database from JSON string.
     */
    suspend fun importFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)

            // Clear database first
            db.schoolConfigDao().clear()
            db.attendanceDao().clear()
            // We'll insert class, sections, students fresh

            // 1. School Config
            if (root.has("school_config")) {
                val cfg = root.getJSONObject("school_config")
                db.schoolConfigDao().insertSchoolConfig(
                    SchoolConfig(
                        id = 1,
                        name = cfg.getString("name"),
                        logoPath = cfg.optString("logoPath", ""),
                        eiin = cfg.optString("eiin", ""),
                        code = cfg.optString("code", ""),
                        type = cfg.optString("type", "High School"),
                        address = cfg.optString("address", ""),
                        phone = cfg.optString("phone", ""),
                        email = cfg.optString("email", ""),
                        website = cfg.optString("website", ""),
                        principalName = cfg.optString("principalName", ""),
                        academicStart = cfg.optString("academicStart", ""),
                        academicEnd = cfg.optString("academicEnd", "")
                    )
                )
            }

            // 2. Academic Years
            if (root.has("academic_years")) {
                val array = root.getJSONArray("academic_years")
                for (i in 0 until array.length()) {
                    val y = array.getJSONObject(i)
                    db.academicYearDao().insertAcademicYear(
                        AcademicYear(
                            year = y.getInt("year"),
                            isActive = y.getBoolean("isActive")
                        )
                    )
                }
            }

            // Map old IDs to newly generated/inserted IDs if needed,
            // but since we can insert with explicit IDs, Room allows inserting with explicit primary key values.
            // Let's just insert with explicit IDs.

            // 3. Classes
            if (root.has("classes")) {
                val array = root.getJSONArray("classes")
                for (i in 0 until array.length()) {
                    val c = array.getJSONObject(i)
                    db.classDao().insertClass(
                        ClassEntity(
                            id = c.getInt("id"),
                            name = c.getString("name")
                        )
                    )
                }
            }

            // 4. Sections
            if (root.has("sections")) {
                val array = root.getJSONArray("sections")
                for (i in 0 until array.length()) {
                    val s = array.getJSONObject(i)
                    db.sectionDao().insertSection(
                        SectionEntity(
                            id = s.getInt("id"),
                            classId = s.getInt("classId"),
                            name = s.getString("name")
                        )
                    )
                }
            }

            // 5. Students
            if (root.has("students")) {
                val array = root.getJSONArray("students")
                for (i in 0 until array.length()) {
                    val st = array.getJSONObject(i)
                    db.studentDao().insertStudent(
                        StudentEntity(
                            id = st.getInt("id"),
                            studentIdCode = st.getString("studentIdCode"),
                            rollNumber = st.getInt("rollNumber"),
                            name = st.getString("name"),
                            fathersName = st.optString("fathersName", ""),
                            mothersName = st.optString("mothersName", ""),
                            dob = st.optString("dob", ""),
                            gender = st.optString("gender", "Male"),
                            bloodGroup = st.optString("bloodGroup", "A+"),
                            classId = st.getInt("classId"),
                            sectionId = st.getInt("sectionId"),
                            mobile = st.optString("mobile", ""),
                            guardianPhone = st.optString("guardianPhone", ""),
                            address = st.optString("address", ""),
                            admissionDate = st.optString("admissionDate", ""),
                            photoUri = st.optString("photoUri", "")
                        )
                    )
                }
            }

            // 6. Attendance
            if (root.has("attendance")) {
                val array = root.getJSONArray("attendance")
                val attList = mutableListOf<AttendanceEntity>()
                for (i in 0 until array.length()) {
                    val a = array.getJSONObject(i)
                    attList.add(
                        AttendanceEntity(
                            studentId = a.getInt("studentId"),
                            dateStr = a.getString("dateStr"),
                            status = a.getString("status")
                        )
                    )
                }
                if (attList.isNotEmpty()) {
                    db.attendanceDao().insertAttendanceList(attList)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Clear and prepopulate database with beautiful realistic demo data.
     */
    suspend fun generateDemoData() = withContext(Dispatchers.IO) {
        // Clear old database data
        db.schoolConfigDao().clear()
        db.attendanceDao().clear()

        // 1. School Setup
        val config = SchoolConfig(
            id = 1,
            name = "Infinity School OS",
            logoPath = "",
            eiin = "554432",
            code = "ISO-9988",
            type = "English & Bangla Medium High School",
            address = "Infinity Campus, Road 01, Sector 03, Uttara, Dhaka, Bangladesh",
            phone = "+8801999999999",
            email = "info@infinityschoolos.edu.bd",
            website = "www.infinityschoolos.edu.bd",
            principalName = "Prof. Dr. Shakil Ahmed",
            academicStart = "2026-01-01",
            academicEnd = "2026-12-31"
        )
        db.schoolConfigDao().insertSchoolConfig(config)

        // 2. Academic Years 2025 to 2050
        for (y in 2025..2050) {
            db.academicYearDao().insertAcademicYear(AcademicYear(year = y, isActive = (y == 2026)))
        }

        // 3. Setup Classes: KG, Class 1, Class 5, Class 6, Class 7, Class 8, Class 9, Class 10
        val classNames = listOf("KG", "Class 1", "Class 6", "Class 7", "Class 8", "Class 9", "Class 10")
        val classIds = mutableListOf<Int>()
        for (name in classNames) {
            val cId = db.classDao().insertClass(ClassEntity(name = name))
            classIds.add(cId.toInt())
        }

        // 4. Setup Sections: Class 6, 7, 8 will have sections A, B, C
        val sectionIdsMap = mutableMapOf<Int, List<Int>>() // classId to sectionIds
        for (cId in classIds) {
            val sectionsForThisClass = mutableListOf<Int>()
            // Always have at least Section 'A'
            val sIdA = db.sectionDao().insertSection(SectionEntity(classId = cId, name = "A"))
            sectionsForThisClass.add(sIdA.toInt())

            if (cId % 2 == 0) { // Some classes have sections B and C
                val sIdB = db.sectionDao().insertSection(SectionEntity(classId = cId, name = "B"))
                sectionsForThisClass.add(sIdB.toInt())
                val sIdC = db.sectionDao().insertSection(SectionEntity(classId = cId, name = "C"))
                sectionsForThisClass.add(sIdC.toInt())
            } else if (cId % 3 == 0) {
                val sIdB = db.sectionDao().insertSection(SectionEntity(classId = cId, name = "B"))
                sectionsForThisClass.add(sIdB.toInt())
            }
            sectionIdsMap[cId] = sectionsForThisClass
        }

        // 5. Generate Students: 500 students per active Section
        val lastNames = listOf(
            "Ahmed", "Hasan", "Khan", "Rahman", "Islam", "Chowdhury", "Akter", "Sultana", "Uddin", "Miah", "Ali",
            "Hossain", "Talukder", "Bhuiyan", "Sarker", "Patwary", "Mahmud", "Siddique", "Karim", "Kabir"
        )
        val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

        var studentCount = 1
        val allStudents = mutableListOf<StudentEntity>()

        for ((classId, sectionList) in sectionIdsMap) {
            for (sectId in sectionList) {
                val sectionStudents = mutableListOf<StudentEntity>()
                for (roll in 1..500) {
                    val gender = if (roll % 2 == 0) "Female" else "Male"
                    val fName = if (gender == "Female") {
                        val femaleNames = listOf("Sumaiya", "Nusrat", "Fatima", "Anika", "Tasnim", "Ayesha", "Jannat", "Sadia", "Mim", "Rumana")
                        femaleNames[roll % femaleNames.size]
                    } else {
                        val maleNames = listOf("Rifat", "Rayhan", "Rahim", "Siam", "Hasan", "Tariq", "Arif", "Sajid", "Tanvir", "Fahim", "Kabir", "Mehedi")
                        maleNames[roll % maleNames.size]
                    }
                    val lName = lastNames[roll % lastNames.size]
                    val name = "$fName $lName"
                    val stdIdCode = String.format(Locale.US, "STD2026%05d", studentCount)

                    val student = StudentEntity(
                        id = studentCount,
                        studentIdCode = stdIdCode,
                        rollNumber = roll,
                        name = name,
                        fathersName = "M. ${lastNames[(roll + 1) % lastNames.size]} Rahman",
                        mothersName = "Mrs. ${lastNames[(roll + 2) % lastNames.size]} Begum",
                        dob = String.format(Locale.US, "201%d-%02d-%02d", 2 + (roll % 5), 1 + (roll % 11), 1 + (roll % 27)),
                        gender = gender,
                        bloodGroup = bloodGroups[roll % bloodGroups.size],
                        classId = classId,
                        sectionId = sectId,
                        mobile = "017" + String.format(Locale.US, "%08d", 50000000 + studentCount),
                        guardianPhone = "018" + String.format(Locale.US, "%08d", 60000000 + studentCount),
                        address = "House ${1 + (roll % 100)}, Road ${1 + (roll % 20)}, Uttara, Dhaka",
                        admissionDate = "2026-01-10",
                        photoUri = ""
                    )
                    sectionStudents.add(student)
                    allStudents.add(student)
                    studentCount++
                }
                db.studentDao().insertStudentList(sectionStudents)
            }
        }

        // 6. Generate historical attendance records (5 days total for all students to keep it snappy but complete)
        val attendanceList = mutableListOf<AttendanceEntity>()
        val statuses = listOf("P", "P", "P", "P", "P", "P", "P", "P", "A", "L", "LV") // Heavy bias towards Present!

        // July 1, July 2, July 3 2026
        for (day in 1..3) {
            val dateStr = String.format(Locale.US, "2026-07-%02d", day)
            for (st in allStudents) {
                val statusIdx = (st.id + day) % statuses.size
                val status = statuses[statusIdx]
                attendanceList.add(AttendanceEntity(studentId = st.id, dateStr = dateStr, status = status))
            }
        }
        // June 1, June 2 2026
        for (day in 1..2) {
            val dateStr = String.format(Locale.US, "2026-06-%02d", day)
            for (st in allStudents) {
                val statusIdx = (st.id + day * 3) % statuses.size
                val status = statuses[statusIdx]
                attendanceList.add(AttendanceEntity(studentId = st.id, dateStr = dateStr, status = status))
            }
        }

        // Insert in chunks of 1000 to prevent SQLite argument binding limits or memory spikes
        val chunkSize = 1000
        for (i in 0 until attendanceList.size step chunkSize) {
            val end = minOf(i + chunkSize, attendanceList.size)
            db.attendanceDao().insertAttendanceList(attendanceList.subList(i, end))
        }
    }
}
