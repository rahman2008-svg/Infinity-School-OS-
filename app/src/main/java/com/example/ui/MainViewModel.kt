package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AcademicYear
import com.example.data.AppDatabase
import com.example.data.AttendanceEntity
import com.example.data.ClassEntity
import com.example.data.Repository
import com.example.data.SchoolConfig
import com.example.data.SectionEntity
import com.example.data.StudentEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = Repository(db)

    // Splash Screen finished state
    private val _splashFinished = MutableStateFlow(false)
    val splashFinished = _splashFinished.asStateFlow()

    // Active Language (false = English, true = Bengali)
    private val _isBengali = MutableStateFlow(false)
    val isBengali = _isBengali.asStateFlow()

    // User Role ("Admin", "Principal", "Teacher", "Office Staff")
    private val _userRole = MutableStateFlow("Teacher")
    val userRole = _userRole.asStateFlow()

    // School config flow
    val schoolConfig: StateFlow<SchoolConfig?> = repository.schoolConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Academic Years
    val academicYears: StateFlow<List<AcademicYear>> = repository.academicYears.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeAcademicYear: StateFlow<AcademicYear?> = repository.activeAcademicYear.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Class and Section setups
    val classes: StateFlow<List<ClassEntity>> = repository.classes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sections: StateFlow<List<SectionEntity>> = repository.sections.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Currently Selected filters for register and lists
    private val _selectedYear = MutableStateFlow(2026)
    val selectedYear = _selectedYear.asStateFlow()

    private val _selectedClassId = MutableStateFlow<Int?>(null)
    val selectedClassId = _selectedClassId.asStateFlow()

    private val _selectedSectionId = MutableStateFlow<Int?>(null)
    val selectedSectionId = _selectedSectionId.asStateFlow()

    private val _selectedMonth = MutableStateFlow(6) // 0-indexed (6 = July)
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedDay = MutableStateFlow(15) // 1-indexed (15th July)
    val selectedDay = _selectedDay.asStateFlow()

    // Active students in selected Class & Section
    val activeStudents: StateFlow<List<StudentEntity>> = _selectedSectionId
        .flatMapLatest { sectionId ->
            if (sectionId == null) flowOf(emptyList())
            else repository.getStudentsForSection(sectionId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active month's attendance matrix map: studentId -> dateStr -> status
    val activeMonthAttendance: StateFlow<Map<Int, Map<String, String>>> = combine(
        activeStudents,
        _selectedYear,
        _selectedMonth
    ) { students, year, month ->
        val studentIds = students.map { it.id }
        if (studentIds.isEmpty()) return@combine emptyMap<Int, Map<String, String>>()

        val monthStr = String.format(Locale.US, "%d-%02d", year, month + 1)
        val list: List<AttendanceEntity> = repository.getAttendanceForSectionAndMonth(studentIds, monthStr).firstOrNull() ?: emptyList()

        // Map it: studentId -> dateStr -> status
        val matrix = mutableMapOf<Int, MutableMap<String, String>>()
        for (att in list) {
            val studentMap = matrix.getOrPut(att.studentId) { mutableMapOf() }
            studentMap[att.dateStr] = att.status
        }
        matrix
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Search students
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchedStudents: StateFlow<List<StudentEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllStudents()
            else repository.searchStudents(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Student profile detail view
    private val _selectedStudent = MutableStateFlow<StudentEntity?>(null)
    val selectedStudent = _selectedStudent.asStateFlow()

    val selectedStudentAttendance: StateFlow<List<AttendanceEntity>> = _selectedStudent
        .flatMapLatest { student ->
            if (student == null) flowOf(emptyList())
            else repository.getAttendanceForStudent(student.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Smart Simulator Status
    private val _simulatorLog = MutableStateFlow<List<String>>(emptyList())
    val simulatorLog = _simulatorLog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    private val _jsonBackupText = MutableStateFlow("")
    val jsonBackupText = _jsonBackupText.asStateFlow()

    init {
        // Start 3 second timer for Splash Screen
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _splashFinished.value = true
        }

        // Auto pre-populate class and section filters once loaded
        viewModelScope.launch {
            classes.collect { list ->
                if (list.isNotEmpty() && _selectedClassId.value == null) {
                    _selectedClassId.value = list.first().id
                }
            }
        }
        viewModelScope.launch {
            combine(sections, _selectedClassId) { sects, classId ->
                sects.filter { it.classId == classId }
            }.collect { list ->
                if (list.isNotEmpty()) {
                    _selectedSectionId.value = list.first().id
                } else {
                    _selectedSectionId.value = null
                }
            }
        }
    }

    fun setLanguage(bengali: Boolean) {
        _isBengali.value = bengali
    }

    fun setUserRole(role: String) {
        _userRole.value = role
    }

    fun setSelectedClass(classId: Int) {
        _selectedClassId.value = classId
        // Update selected section to first of new class
        viewModelScope.launch {
            val classSects = sections.value.filter { it.classId == classId }
            if (classSects.isNotEmpty()) {
                _selectedSectionId.value = classSects.first().id
            } else {
                _selectedSectionId.value = null
            }
        }
    }

    fun setSelectedSection(sectionId: Int) {
        _selectedSectionId.value = sectionId
    }

    fun setSelectedMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectStudent(student: StudentEntity?) {
        _selectedStudent.value = student
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Database mutators
    fun saveSchoolSetup(
        name: String,
        eiin: String,
        code: String,
        type: String,
        address: String,
        phone: String,
        email: String,
        website: String,
        principal: String,
        start: String,
        end: String
    ) {
        viewModelScope.launch {
            val config = SchoolConfig(
                id = 1,
                name = name,
                eiin = eiin,
                code = code,
                type = type,
                address = address,
                phone = phone,
                email = email,
                website = website,
                principalName = principal,
                academicStart = start,
                academicEnd = end
            )
            repository.saveSchoolConfig(config)
            _statusMessage.value = "School setup saved!"
        }
    }

    fun setYearActive(year: Int) {
        viewModelScope.launch {
            repository.setActiveYear(year)
            _selectedYear.value = year
        }
    }

    fun addClass(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val id = repository.insertClass(name)
                if (_selectedClassId.value == null) {
                    _selectedClassId.value = id.toInt()
                }
            }
        }
    }

    fun addSection(classId: Int, name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val id = repository.insertSection(classId, name)
                if (_selectedClassId.value == classId && _selectedSectionId.value == null) {
                    _selectedSectionId.value = id.toInt()
                }
            }
        }
    }

    fun addStudent(
        roll: Int,
        name: String,
        father: String,
        mother: String,
        dob: String,
        gender: String,
        blood: String,
        mobile: String,
        guardian: String,
        address: String
    ) {
        viewModelScope.launch {
            val classId = _selectedClassId.value ?: return@launch
            val sectionId = _selectedSectionId.value ?: return@launch
            val idCode = "STD2026" + String.format(Locale.US, "%04d", (searchedStudents.value.size + 1))
            val student = StudentEntity(
                studentIdCode = idCode,
                rollNumber = roll,
                name = name,
                fathersName = father,
                mothersName = mother,
                dob = dob,
                gender = gender,
                bloodGroup = blood,
                classId = classId,
                sectionId = sectionId,
                mobile = mobile,
                guardianPhone = guardian,
                address = address,
                admissionDate = "2026-07-01",
                photoUri = ""
            )
            repository.insertStudent(student)
            _statusMessage.value = "Student $name added!"
        }
    }

    fun deleteStudent(id: Int) {
        viewModelScope.launch {
            repository.deleteStudent(id)
            _statusMessage.value = "Student deleted"
        }
    }

    fun updateStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.insertStudent(student)
            _selectedStudent.value = student
            _statusMessage.value = "Student ${student.name} updated!"
        }
    }

    fun toggleAttendance(studentId: Int, day: Int) {
        viewModelScope.launch {
            val year = _selectedYear.value
            val month = _selectedMonth.value + 1
            val dateStr = String.format(Locale.US, "%d-%02d-%02d", year, month, day)

            // Current status
            val currentMap = activeMonthAttendance.value[studentId]
            val currentStatus = currentMap?.get(dateStr)

            // Toggle logic: Blank -> P -> A -> L -> LV -> Blank
            val nextStatus = when (currentStatus) {
                null, "" -> "P"
                "P" -> "A"
                "A" -> "L"
                "L" -> "LV"
                "LV" -> ""
                else -> "P"
            }

            if (nextStatus.isEmpty()) {
                repository.deleteAttendance(studentId, dateStr)
            } else {
                repository.saveAttendance(
                    AttendanceEntity(
                        studentId = studentId,
                        dateStr = dateStr,
                        status = nextStatus
                    )
                )
            }
        }
    }

    fun setAttendanceStatusDirectly(studentId: Int, day: Int, status: String) {
        viewModelScope.launch {
            val year = _selectedYear.value
            val month = _selectedMonth.value + 1
            val dateStr = String.format(Locale.US, "%d-%02d-%02d", year, month, day)

            if (status.isEmpty()) {
                repository.deleteAttendance(studentId, dateStr)
            } else {
                repository.saveAttendance(
                    AttendanceEntity(
                        studentId = studentId,
                        dateStr = dateStr,
                        status = status
                    )
                )
            }
        }
    }

    // Demo Data
    fun generateDemoData() {
        viewModelScope.launch {
            _statusMessage.value = "Generating high-quality demo data..."
            repository.generateDemoData()
            // Reset filters to Class 6 A, Month July
            _selectedYear.value = 2026
            _selectedMonth.value = 6
            _selectedDay.value = 15
            _statusMessage.value = "Demo Data generated successfully!"
        }
    }

    // JSON Backup / Restore
    fun loadBackupText() {
        viewModelScope.launch {
            try {
                val json = repository.exportToJson()
                _jsonBackupText.value = json
            } catch (e: Exception) {
                _jsonBackupText.value = "Error generating backup string."
            }
        }
    }

    fun restoreBackup(json: String) {
        viewModelScope.launch {
            val success = repository.importFromJson(json)
            if (success) {
                _statusMessage.value = "Database restored successfully!"
                // Trigger reload by resetting filters
                val temp = _selectedYear.value
                _selectedYear.value = 0
                _selectedYear.value = temp
            } else {
                _statusMessage.value = "Failed to restore database! Invalid JSON structure."
            }
        }
    }

    // Scanner check-in simulation
    fun simulateCardOrFingerprintTap(studentIdCode: String, method: String) {
        viewModelScope.launch {
            // Find student with this ID code
            val all: List<StudentEntity> = repository.getAllStudents().firstOrNull() ?: emptyList()
            val student = all.find { it.studentIdCode.equals(studentIdCode, ignoreCase = true) }
            if (student != null) {
                val year = _selectedYear.value
                val month = _selectedMonth.value + 1
                val day = _selectedDay.value
                val dateStr = String.format(Locale.US, "%d-%02d-%02d", year, month, day)

                // Save as Present
                repository.saveAttendance(AttendanceEntity(studentId = student.id, dateStr = dateStr, status = "P"))

                val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
                val timeStr = formatter.format(Date())
                val logMsg = "[$timeStr] Checked in ${student.name} (Roll ${student.rollNumber}) via $method for $dateStr"
                _simulatorLog.value = listOf(logMsg) + _simulatorLog.value
                _statusMessage.value = "Checked in ${student.name} successfully!"
            } else {
                _statusMessage.value = "Student ID code '$studentIdCode' not found."
            }
        }
    }

    fun clearSimulatorLog() {
        _simulatorLog.value = emptyList()
    }
}
