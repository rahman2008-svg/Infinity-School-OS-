package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolConfigDao {
    @Query("SELECT * FROM school_config WHERE id = 1")
    fun getSchoolConfig(): Flow<SchoolConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolConfig(config: SchoolConfig)

    @Query("DELETE FROM school_config")
    suspend fun clear()
}

@Dao
interface AcademicYearDao {
    @Query("SELECT * FROM academic_years ORDER BY year ASC")
    fun getAllAcademicYears(): Flow<List<AcademicYear>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicYear(year: AcademicYear)

    @Query("UPDATE academic_years SET isActive = (year = :activeYear)")
    suspend fun setActiveYear(activeYear: Int)

    @Query("SELECT * FROM academic_years WHERE isActive = 1 LIMIT 1")
    fun getActiveAcademicYear(): Flow<AcademicYear?>
}

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY id ASC")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity): Long

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClass(id: Int)
}

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections ORDER BY name ASC")
    fun getAllSections(): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE classId = :classId ORDER BY name ASC")
    fun getSectionsForClass(classId: Int): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity): Long

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteSection(id: Int)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY rollNumber ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE sectionId = :sectionId ORDER BY rollNumber ASC")
    fun getStudentsForSection(sectionId: Int): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentList(list: List<StudentEntity>)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: Int)

    @Query("""
        SELECT * FROM students 
        WHERE name LIKE '%' || :query || '%' 
        OR rollNumber LIKE '%' || :query || '%' 
        OR studentIdCode LIKE '%' || :query || '%' 
        OR guardianPhone LIKE '%' || :query || '%'
        ORDER BY rollNumber ASC
    """)
    fun searchStudents(query: String): Flow<List<StudentEntity>>
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE dateStr = :dateStr")
    fun getAttendanceForDate(dateStr: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId")
    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId IN (:studentIds) AND dateStr LIKE :monthPattern || '%'")
    fun getAttendanceForSectionAndMonth(studentIds: List<Int>, monthPattern: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(list: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE studentId = :studentId AND dateStr = :dateStr")
    suspend fun deleteAttendance(studentId: Int, dateStr: String)

    @Query("DELETE FROM attendance")
    suspend fun clear()
}
