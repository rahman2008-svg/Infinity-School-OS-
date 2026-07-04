package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "school_config")
data class SchoolConfig(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val logoPath: String = "",
    val eiin: String = "",
    val code: String = "",
    val type: String = "High School",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val principalName: String = "",
    val academicStart: String = "",
    val academicEnd: String = ""
)

@Entity(tableName = "academic_years")
data class AcademicYear(
    @PrimaryKey val year: Int,
    val isActive: Boolean = false
)

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int,
    val name: String
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentIdCode: String, // Auto-generated ID (e.g. STD20260001)
    val rollNumber: Int,
    val name: String,
    val fathersName: String = "",
    val mothersName: String = "",
    val dob: String = "",
    val gender: String = "Male",
    val bloodGroup: String = "A+",
    val classId: Int,
    val sectionId: Int,
    val mobile: String = "",
    val guardianPhone: String = "",
    val address: String = "",
    val admissionDate: String = "",
    val photoUri: String = ""
)

@Entity(tableName = "attendance", primaryKeys = ["studentId", "dateStr"])
data class AttendanceEntity(
    val studentId: Int,
    val dateStr: String, // Format: yyyy-MM-dd
    val status: String // P = Present, A = Absent, L = Late, LV = Leave, H = Holiday
)
