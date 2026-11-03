
const API_URL = "/api/rooms";

let rooms = [];
let editingId = null;

const table = document.getElementById("roomTable");
const dialog = document.getElementById("roomDialog");
const form = document.getElementById("roomForm");
const message = document.getElementById("roomMessage");
const formMessage = document.getElementById("formMessage");
const saveBtn = document.getElementById("saveBtn");

// 1. Hàm gọi REST API
async function callApi(url, options = {}) {

    const response = await fetch(url, {
        ...options,
        headers: {
            "Accept": "application/json",
            ...(options.body
                ? {"Content-Type": "application/json"}
                : {}),
            ...options.headers
        }
    });

    if (!response.ok) {
        let errorText = `Lỗi HTTP ${response.status}`;

        try {
            const errorData = await response.json();

            errorText =
                errorData.detail ||
                errorData.message ||
                errorText;

        } catch (error) {
            // Không có JSON lỗi thì giữ thông báo HTTP.
        }

        throw new Error(errorText);
    }

    if (response.status === 204) {
        return null;
    }

    return await response.json();
}

// 2. Tải danh sách phòng
async function loadRooms() {

    message.textContent = "Đang tải dữ liệu...";

    try {
        rooms = await callApi(API_URL);

        message.textContent = "";
        renderRooms();
        updateDashboard();

    } catch (error) {
        message.textContent = error.message;
    }
}

// 3. Hiển thị danh sách phòng
function renderRooms() {

    const keyword = document.getElementById("searchRoom")
        .value.trim().toLowerCase();

    const filtered = rooms.filter(room =>
        room.code.toLowerCase().includes(keyword) ||
        room.name.toLowerCase().includes(keyword)
    );

    table.replaceChildren();

    if (filtered.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 8;
        cell.textContent = "Không tìm thấy phòng học.";
        row.appendChild(cell);
        table.appendChild(row);
        return;
    }

    filtered.forEach(room => {

        const row = document.createElement("tr");

        const values = [
            room.code,
            room.name,
            room.capacity,
            room.roomType,
            room.projector ? "Có" : "Không",
            room.computers ? "Có" : "Không"
        ];

        values.forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        });

        const actionCell = document.createElement("td");
        const actions = document.createElement("div");

        actions.className = "table-actions";

        const editBtn = document.createElement("button");
        editBtn.textContent = "Sửa";
        editBtn.className = "edit-btn";
        editBtn.onclick = () => openEditForm(room.id);

        const deleteBtn = document.createElement("button");
        deleteBtn.textContent = "Xóa";
        deleteBtn.className = "danger-btn";
        deleteBtn.onclick = () => deleteRoom(room.id);

        actions.append(editBtn, deleteBtn);
        actionCell.appendChild(actions);
        row.appendChild(actionCell);

        table.appendChild(row);
    });
}

// 4. Cập nhật số liệu Dashboard
function updateDashboard() {

    document.getElementById("totalRooms").textContent =
        rooms.length;

    document.getElementById("projectorRooms").textContent =
        rooms.filter(room => room.projector).length;

    document.getElementById("computerRooms").textContent =
        rooms.filter(room => room.computers).length;
}

// 5. Chuyển trang Sidebar






function showPage(pageName) {

    const pageTitles = {
        dashboard: "Tổng quan",
        rooms: "Quản lý phòng học",
        teachers: "Quản lý giảng viên",
        groups: "Quản lý lớp sinh viên",
        courses: "Quản lý môn học",
        offerings: "Quản lý lớp học phần",
        schedule: "Thời khóa biểu",
        autoSchedule: "Xếp lịch tự động",
        availability: "Tìm phòng trống",
        reports: "Thống kê & báo cáo"
    };

    document.querySelectorAll(".page").forEach(page => {

        page.classList.toggle(
            "hidden",
            page.id !== pageName + "Page"
        );
    });

    document.getElementById("pageTitle").textContent =
        pageTitles[pageName] || "HPUNI Smart Classroom";

    document.querySelectorAll(".nav-item").forEach(btn => {

        btn.classList.toggle(
            "active",
            btn.dataset.page === pageName
        );
    });

    if (pageName === "teachers") loadTeachers();
    if (pageName === "groups") loadGroups();
    if (pageName === "courses") loadCourses();
    if (pageName === "offerings") loadOfferings();
    if (pageName === "schedule") loadSchedule();

    if (pageName === "autoSchedule") {

        const picker = document.getElementById("autoWeek");

        if (!picker.value) {
            picker.value = localDateString(new Date());
        }
    }
}



// 6. Mở form thêm phòng
function openAddForm() {

    editingId = null;
    form.reset();

    document.getElementById("formTitle").textContent =
        "Thêm phòng học";

    formMessage.textContent = "";
    dialog.showModal();
}

// 7. Mở form sửa phòng
function openEditForm(id) {

    const room = rooms.find(room => room.id === id);

    if (!room) {
        alert("Không tìm thấy phòng học.");
        return;
    }

    editingId = id;

    document.getElementById("formTitle").textContent =
        "Sửa thông tin phòng";

    document.getElementById("code").value = room.code;
    document.getElementById("name").value = room.name;
    document.getElementById("capacity").value = room.capacity;
    document.getElementById("roomType").value = room.roomType;

    document.getElementById("projector").checked =
        room.projector;

    document.getElementById("computers").checked =
        room.computers;

    formMessage.textContent = "";
    dialog.showModal();
}

// 8. Lưu phòng mới hoặc cập nhật
form.addEventListener("submit", async function(event) {

    event.preventDefault();

    const roomData = {
        code: document.getElementById("code").value.trim(),
        name: document.getElementById("name").value.trim(),
        capacity: Number(
            document.getElementById("capacity").value
        ),
        roomType: document.getElementById("roomType").value,
        projector: document.getElementById("projector").checked,
        computers: document.getElementById("computers").checked
    };

    const isEditing = editingId !== null;

    const url = isEditing
        ? `${API_URL}/${editingId}`
        : API_URL;

    const method = isEditing ? "PUT" : "POST";

    saveBtn.disabled = true;
    formMessage.textContent = "";

    try {
        await callApi(url, {
            method: method,
            body: JSON.stringify(roomData)
        });

        dialog.close();
        await loadRooms();

        message.textContent = isEditing
            ? "Đã cập nhật phòng thành công!"
            : "Đã thêm phòng thành công!";

    } catch (error) {
        formMessage.textContent = error.message;

    } finally {
        saveBtn.disabled = false;
    }
});

// 9. Xóa phòng
async function deleteRoom(id) {

    const room = rooms.find(room => room.id === id);

    if (!room) return;

    const confirmed = confirm(
        `Bạn có chắc muốn xóa phòng ${room.code}?`
    );

    if (!confirmed) return;

    try {
        await callApi(`${API_URL}/${id}`, {
            method: "DELETE"
        });

        await loadRooms();
        message.textContent = "Đã xóa phòng thành công!";

    } catch (error) {
        message.textContent = error.message;
    }
}

// 10. Gắn sự kiện cho các nút
document.querySelectorAll(".nav-item").forEach(button => {

    button.addEventListener("click", () => {
        // Nút mở chatbot không phải trang; không ẩn trang hiện tại.
        if (button.dataset.page) showPage(button.dataset.page);
    });
});

document.getElementById("goRooms")
    .addEventListener("click", () => showPage("rooms"));

document.getElementById("addRoomBtn")
    .addEventListener("click", openAddForm);

document.getElementById("cancelBtn")
    .addEventListener("click", () => dialog.close());

document.getElementById("searchRoom")
    .addEventListener("input", renderRooms);

// 11. Khởi động trang
showPage("dashboard");
loadRooms();


const TEACHER_API = "/api/catalog/teachers";

let teachers = [];
let editingTeacherId = null;

const teacherTable =
    document.getElementById("teacherTable");

const teacherDialog =
    document.getElementById("teacherDialog");

const teacherForm =
    document.getElementById("teacherForm");

const teacherMessage =
    document.getElementById("teacherMessage");

const teacherFormMessage =
    document.getElementById("teacherFormMessage");

// 1. Lấy danh sách giảng viên
async function loadTeachers() {

    teacherMessage.textContent = "Đang tải dữ liệu...";

    try {
        teachers = await callApi(TEACHER_API);

        teacherMessage.textContent = "";
        renderTeachers();

    } catch (error) {
        teacherMessage.textContent = error.message;
    }
}

// 2. Hiển thị giảng viên lên bảng
function renderTeachers() {

    const keyword = document
        .getElementById("searchTeacher")
        .value.trim().toLowerCase();

    const filtered = teachers.filter(t =>
        t.code.toLowerCase().includes(keyword) ||
        t.fullName.toLowerCase().includes(keyword) ||
        t.email.toLowerCase().includes(keyword)
    );

    teacherTable.replaceChildren();

    if (filtered.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 4;
        cell.textContent = "Không tìm thấy giảng viên.";

        row.appendChild(cell);
        teacherTable.appendChild(row);
        return;
    }

    filtered.forEach(teacher => {

        const row = document.createElement("tr");

        [
            teacher.code,
            teacher.fullName,
            teacher.email
        ].forEach(value => {

            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        });

        const actionCell = document.createElement("td");
        const actions = document.createElement("div");
        actions.className = "table-actions";

        const editBtn = document.createElement("button");
        editBtn.textContent = "Sửa";
        editBtn.className = "edit-btn";
        editBtn.onclick = () => openEditTeacher(teacher.id);

        const deleteBtn = document.createElement("button");
        deleteBtn.textContent = "Xóa";
        deleteBtn.className = "danger-btn";
        deleteBtn.onclick = () => deleteTeacher(teacher.id);

        actions.append(editBtn, deleteBtn);
        actionCell.appendChild(actions);
        row.appendChild(actionCell);

        teacherTable.appendChild(row);
    });
}

// 3. Mở form thêm giảng viên
function openAddTeacher() {

    editingTeacherId = null;
    teacherForm.reset();

    document.getElementById("teacherFormTitle")
        .textContent = "Thêm giảng viên";

    teacherFormMessage.textContent = "";
    teacherDialog.showModal();
}

// 4. Mở form sửa giảng viên
function openEditTeacher(id) {

    const teacher = teachers.find(t => t.id === id);

    if (!teacher) return;

    editingTeacherId = id;

    document.getElementById("teacherFormTitle")
        .textContent = "Sửa giảng viên";

    document.getElementById("teacherCode").value =
        teacher.code;

    document.getElementById("teacherName").value =
        teacher.fullName;

    document.getElementById("teacherEmail").value =
        teacher.email;

    teacherFormMessage.textContent = "";
    teacherDialog.showModal();
}

// 5. Lưu giảng viên
teacherForm.addEventListener("submit", async event => {

    event.preventDefault();

    const data = {
        code: document.getElementById("teacherCode")
            .value.trim(),

        fullName: document.getElementById("teacherName")
            .value.trim(),

        email: document.getElementById("teacherEmail")
            .value.trim()
    };

    const editing = editingTeacherId !== null;

    const url = editing
        ? `${TEACHER_API}/${editingTeacherId}`
        : TEACHER_API;

    const saveBtn =
        document.getElementById("saveTeacherBtn");

    saveBtn.disabled = true;
    teacherFormMessage.textContent = "";

    try {
        await callApi(url, {
            method: editing ? "PUT" : "POST",
            body: JSON.stringify(data)
        });

        teacherDialog.close();
        await loadTeachers();

        teacherMessage.textContent = editing
            ? "Đã cập nhật giảng viên!"
            : "Đã thêm giảng viên!";

    } catch (error) {
        teacherFormMessage.textContent = error.message;

    } finally {
        saveBtn.disabled = false;
    }
});

// 6. Xóa giảng viên
async function deleteTeacher(id) {

    const teacher = teachers.find(t => t.id === id);
    if (!teacher) return;

    if (!confirm(
        `Bạn có chắc muốn xóa giảng viên ${teacher.fullName}?`
    )) return;

    try {
        await callApi(`${TEACHER_API}/${id}`, {
            method: "DELETE"
        });

        await loadTeachers();
        teacherMessage.textContent = "Đã xóa giảng viên!";

    } catch (error) {
        teacherMessage.textContent = error.message;
    }
}

// 7. Gắn sự kiện
document.getElementById("addTeacherBtn")
    .addEventListener("click", openAddTeacher);

document.getElementById("cancelTeacherBtn")
    .addEventListener("click", () => teacherDialog.close());

document.getElementById("searchTeacher")
    .addEventListener("input", renderTeachers);

/* =========================================
   MODULE QUẢN LÝ LỚP SINH VIÊN
========================================= */

const GROUP_API = "/api/catalog/groups";

let studentGroups = [];
let editingGroupId = null;

const groupTable =
    document.getElementById("groupTable");

const groupDialog =
    document.getElementById("groupDialog");

const groupForm =
    document.getElementById("groupForm");

const groupMessage =
    document.getElementById("groupMessage");

const groupFormMessage =
    document.getElementById("groupFormMessage");

// 1. Tải danh sách lớp từ backend
async function loadGroups() {

    groupMessage.textContent = "Đang tải danh sách lớp...";

    try {
        studentGroups = await callApi(GROUP_API);

        groupMessage.textContent = "";
        renderGroups();

    } catch (error) {
        groupMessage.textContent = error.message;
    }
}

// 2. Hiển thị danh sách lớp
function renderGroups() {

    const keyword = document
        .getElementById("searchGroup")
        .value.trim().toLowerCase();

    const filtered = studentGroups.filter(group =>
        group.code.toLowerCase().includes(keyword) ||
        group.name.toLowerCase().includes(keyword)
    );

    groupTable.replaceChildren();

    if (filtered.length === 0) {

        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 4;
        cell.textContent = "Không tìm thấy lớp sinh viên.";

        row.appendChild(cell);
        groupTable.appendChild(row);

        return;
    }

    filtered.forEach(group => {

        const row = document.createElement("tr");

        const values = [
            group.code,
            group.name,
            group.studentCount
        ];

        values.forEach(value => {

            const cell = document.createElement("td");
            cell.textContent = value;

            row.appendChild(cell);
        });

        const actionCell = document.createElement("td");
        const actions = document.createElement("div");

        actions.className = "table-actions";

        // Nút Sửa
        const editBtn = document.createElement("button");

        editBtn.textContent = "Sửa";
        editBtn.className = "edit-btn";

        editBtn.onclick = () => openEditGroup(group.id);

        // Nút Xóa
        const deleteBtn = document.createElement("button");

        deleteBtn.textContent = "Xóa";
        deleteBtn.className = "danger-btn";

        deleteBtn.onclick = () => deleteGroup(group.id);

        actions.append(editBtn, deleteBtn);

        actionCell.appendChild(actions);
        row.appendChild(actionCell);

        groupTable.appendChild(row);
    });
}

// 3. Mở form thêm lớp
function openAddGroup() {

    editingGroupId = null;
    groupForm.reset();

    document.getElementById("groupFormTitle")
        .textContent = "Thêm lớp sinh viên";

    groupFormMessage.textContent = "";

    groupDialog.showModal();
}

// 4. Mở form sửa lớp
function openEditGroup(id) {

    const group = studentGroups.find(g => g.id === id);

    if (!group) {
        alert("Không tìm thấy lớp sinh viên.");
        return;
    }

    editingGroupId = id;

    document.getElementById("groupFormTitle")
        .textContent = "Cập nhật lớp sinh viên";

    document.getElementById("groupCode").value =
        group.code;

    document.getElementById("groupName").value =
        group.name;

    document.getElementById("groupCount").value =
        group.studentCount;

    groupFormMessage.textContent = "";
    groupDialog.showModal();
}

// 5. Lưu lớp mới hoặc sửa lớp
groupForm.addEventListener("submit", async event => {

    event.preventDefault();

    const groupData = {

        code: document.getElementById("groupCode")
            .value.trim(),

        name: document.getElementById("groupName")
            .value.trim(),

        studentCount: Number(
            document.getElementById("groupCount").value
        )
    };

    const editing = editingGroupId !== null;

    const url = editing
        ? `${GROUP_API}/${editingGroupId}`
        : GROUP_API;

    const saveBtn =
        document.getElementById("saveGroupBtn");

    saveBtn.disabled = true;
    groupFormMessage.textContent = "";

    try {
        await callApi(url, {
            method: editing ? "PUT" : "POST",
            body: JSON.stringify(groupData)
        });

        groupDialog.close();

        await loadGroups();

        groupMessage.textContent = editing
            ? "Đã cập nhật lớp thành công!"
            : "Đã thêm lớp thành công!";

    } catch (error) {

        groupFormMessage.textContent = error.message;

    } finally {

        saveBtn.disabled = false;
    }
});

// 6. Xóa lớp sinh viên
async function deleteGroup(id) {

    const group = studentGroups.find(g => g.id === id);

    if (!group) return;

    const confirmed = confirm(
        `Bạn có chắc muốn xóa lớp ${group.code}?`
    );

    if (!confirmed) return;

    try {
        await callApi(`${GROUP_API}/${id}`, {
            method: "DELETE"
        });

        await loadGroups();

        groupMessage.textContent =
            "Đã xóa lớp thành công!";

    } catch (error) {

        groupMessage.textContent = error.message;
    }
}

// 7. Gắn sự kiện vào các nút
document.getElementById("addGroupBtn")
    .addEventListener("click", openAddGroup);

document.getElementById("cancelGroupBtn")
    .addEventListener("click", () => groupDialog.close());

document.getElementById("searchGroup")
    .addEventListener("input", renderGroups);

/* =========================================
   MODULE QUẢN LÝ MÔN HỌC
========================================= */

const COURSE_API = "/api/catalog/courses";

let courses = [];
let editingCourseId = null;

const courseTable =
    document.getElementById("courseTable");

const courseDialog =
    document.getElementById("courseDialog");

const courseForm =
    document.getElementById("courseForm");

const courseMessage =
    document.getElementById("courseMessage");

const courseFormMessage =
    document.getElementById("courseFormMessage");

// 1. Tải danh sách môn học
async function loadCourses() {

    courseMessage.textContent = "Đang tải môn học...";

    try {
        courses = await callApi(COURSE_API);

        courseMessage.textContent = "";
        renderCourses();

    } catch (error) {
        courseMessage.textContent = error.message;
    }
}

// 2. Hiển thị danh sách môn học
function renderCourses() {

    const keyword = document
        .getElementById("searchCourse")
        .value.trim().toLowerCase();

    const filtered = courses.filter(course =>
        course.code.toLowerCase().includes(keyword) ||
        course.name.toLowerCase().includes(keyword)
    );

    courseTable.replaceChildren();

    if (filtered.length === 0) {

        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 4;
        cell.textContent = "Không tìm thấy môn học.";

        row.appendChild(cell);
        courseTable.appendChild(row);

        return;
    }

    filtered.forEach(course => {

        const row = document.createElement("tr");

        // Các cột dữ liệu
        const values = [
            course.code,
            course.name,
            course.credits
        ];

        values.forEach(value => {

            const cell = document.createElement("td");
            cell.textContent = value;

            row.appendChild(cell);
        });

        // Cột thao tác
        const actionCell = document.createElement("td");
        const actions = document.createElement("div");

        actions.className = "table-actions";

        // Nút sửa
        const editBtn = document.createElement("button");

        editBtn.textContent = "Sửa";
        editBtn.className = "edit-btn";

        editBtn.onclick = () => openEditCourse(course.id);

        // Nút xóa
        const deleteBtn = document.createElement("button");

        deleteBtn.textContent = "Xóa";
        deleteBtn.className = "danger-btn";

        deleteBtn.onclick = () => deleteCourse(course.id);

        actions.append(editBtn, deleteBtn);

        actionCell.appendChild(actions);
        row.appendChild(actionCell);

        courseTable.appendChild(row);
    });
}

// 3. Mở form thêm môn học
function openAddCourse() {

    editingCourseId = null;
    courseForm.reset();

    document.getElementById("courseFormTitle")
        .textContent = "Thêm môn học";

    courseFormMessage.textContent = "";
    courseDialog.showModal();
}

// 4. Mở form sửa môn học
function openEditCourse(id) {

    const course = courses.find(c => c.id === id);

    if (!course) {
        alert("Không tìm thấy môn học.");
        return;
    }

    editingCourseId = id;

    document.getElementById("courseFormTitle")
        .textContent = "Cập nhật môn học";

    document.getElementById("courseCode").value =
        course.code;

    document.getElementById("courseName").value =
        course.name;

    document.getElementById("courseCredits").value =
        course.credits;

    courseFormMessage.textContent = "";
    courseDialog.showModal();
}

// 5. Lưu môn học
courseForm.addEventListener("submit", async event => {

    event.preventDefault();

    const courseData = {

        code: document.getElementById("courseCode")
            .value.trim(),

        name: document.getElementById("courseName")
            .value.trim(),

        credits: Number(
            document.getElementById("courseCredits").value
        )
    };

    const editing = editingCourseId !== null;

    const url = editing
        ? `${COURSE_API}/${editingCourseId}`
        : COURSE_API;

    const saveBtn =
        document.getElementById("saveCourseBtn");

    saveBtn.disabled = true;
    courseFormMessage.textContent = "";

    try {
        await callApi(url, {
            method: editing ? "PUT" : "POST",
            body: JSON.stringify(courseData)
        });

        courseDialog.close();
        await loadCourses();

        courseMessage.textContent = editing
            ? "Đã cập nhật môn học thành công!"
            : "Đã thêm môn học thành công!";

    } catch (error) {
        courseFormMessage.textContent = error.message;

    } finally {
        saveBtn.disabled = false;
    }
});

// 6. Xóa môn học
async function deleteCourse(id) {

    const course = courses.find(c => c.id === id);

    if (!course) return;

    const confirmed = confirm(
        `Bạn có chắc muốn xóa môn ${course.name}?`
    );

    if (!confirmed) return;

    try {
        await callApi(`${COURSE_API}/${id}`, {
            method: "DELETE"
        });

        await loadCourses();

        courseMessage.textContent =
            "Đã xóa môn học thành công!";

    } catch (error) {
        courseMessage.textContent = error.message;
    }
}

// 7. Gắn sự kiện cho các nút
document.getElementById("addCourseBtn")
    .addEventListener("click", openAddCourse);

document.getElementById("cancelCourseBtn")
    .addEventListener(
        "click",
        () => courseDialog.close()
    );

document.getElementById("searchCourse")
    .addEventListener("input", renderCourses);

/* =======================================
   QUẢN LÝ LỚP HỌC PHẦN
======================================= */

const OFFERING_API = "/api/catalog/offerings";

let offerings = [];
let editingOfferingId = null;

const offeringTable =
    document.getElementById("offeringTable");

const offeringDialog =
    document.getElementById("offeringDialog");

const offeringForm =
    document.getElementById("offeringForm");

const offeringMessage =
    document.getElementById("offeringMessage");

const offeringFormMessage =
    document.getElementById("offeringFormMessage");

// 1. Tải danh sách lớp học phần
async function loadOfferings() {

    offeringMessage.textContent = "Đang tải dữ liệu...";

    try {
        offerings = await callApi(OFFERING_API);
        offeringMessage.textContent = "";
        renderOfferings();

    } catch (error) {
        offeringMessage.textContent = error.message;
    }
}

// 2. Hiển thị dữ liệu lên bảng
function renderOfferings() {

    const keyword = document.getElementById("searchOffering")
        .value.trim().toLowerCase();

    const filtered = offerings.filter(o =>
        [
            o.courseCode, o.courseName,
            o.teacherName, o.groupCode,
            o.semester, o.academicYear
        ].some(value =>
            String(value).toLowerCase().includes(keyword)
        )
    );

    offeringTable.replaceChildren();

    if (filtered.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 8;
        cell.textContent = "Không tìm thấy lớp học phần.";

        row.appendChild(cell);
        offeringTable.appendChild(row);
        return;
    }

    filtered.forEach(o => {

        const row = document.createElement("tr");

        const values = [
            o.courseCode,
            o.courseName,
            o.teacherName,
            o.groupCode,
            o.semester,
            o.academicYear,
            o.sessionsPerWeek
        ];

        values.forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        });

        const actionCell = document.createElement("td");
        const actions = document.createElement("div");

        actions.className = "table-actions";

        const editBtn = document.createElement("button");
        editBtn.textContent = "Sửa";
        editBtn.className = "edit-btn";
        editBtn.onclick = () => openEditOffering(o.id);

        const deleteBtn = document.createElement("button");
        deleteBtn.textContent = "Xóa";
        deleteBtn.className = "danger-btn";
        deleteBtn.onclick = () => deleteOffering(o.id);

        actions.append(editBtn, deleteBtn);
        actionCell.appendChild(actions);
        row.appendChild(actionCell);

        offeringTable.appendChild(row);
    });
}

// 3. Tạo danh sách lựa chọn trong select
function fillSelect(selectId, data, labelFunction) {

    const select = document.getElementById(selectId);
    select.replaceChildren();

    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "-- Chọn --";
    select.appendChild(placeholder);

    data.forEach(item => {
        const option = document.createElement("option");
        option.value = String(item.id);
        option.textContent = labelFunction(item);
        select.appendChild(option);
    });
}

// 4. Tải môn học, giảng viên, lớp sinh viên
async function loadOfferingOptions() {

    const [courseItems, teacherItems, groupItems] =
        await Promise.all([
            callApi("/api/catalog/courses"),
            callApi("/api/catalog/teachers"),
            callApi("/api/catalog/groups")
        ]);

    fillSelect(
        "offeringCourse",
        courseItems,
        c => `${c.code} - ${c.name}`
    );

    fillSelect(
        "offeringTeacher",
        teacherItems,
        t => `${t.code} - ${t.fullName}`
    );

    fillSelect(
        "offeringGroup",
        groupItems,
        g => `${g.code} - ${g.name}`
    );

    if (!courseItems.length ||
        !teacherItems.length ||
        !groupItems.length) {

        throw new Error(
            "Cần có môn học, giảng viên và lớp sinh viên"
        );
    }
}

// 5. Mở form tạo lớp học phần
async function openAddOffering() {

    editingOfferingId = null;
    offeringForm.reset();

    document.getElementById("offeringFormTitle")
        .textContent = "Tạo lớp học phần";

    offeringFormMessage.textContent = "";
    offeringMessage.textContent = "";

    try {
        await loadOfferingOptions();

        document.getElementById("offeringYear").value =
            "2026-2027";

        document.getElementById("offeringSessions").value = 2;

        offeringDialog.showModal();

    } catch (error) {
        offeringMessage.textContent = error.message;
    }
}

// 6. Mở form sửa phân công
async function openEditOffering(id) {

    offeringMessage.textContent = "";

    try {
        const [data] = await Promise.all([
            callApi(`${OFFERING_API}/${id}`),
            loadOfferingOptions()
        ]);

        editingOfferingId = id;

        document.getElementById("offeringFormTitle")
            .textContent = "Sửa lớp học phần";

        document.getElementById("offeringCourse").value =
            String(data.courseId);

        document.getElementById("offeringTeacher").value =
            String(data.teacherId);

        document.getElementById("offeringGroup").value =
            String(data.groupId);

        document.getElementById("offeringSemester").value =
            data.semester;

        document.getElementById("offeringYear").value =
            data.academicYear;

        document.getElementById("offeringSessions").value =
            data.sessionsPerWeek;

        offeringFormMessage.textContent = "";
        offeringDialog.showModal();

    } catch (error) {
        offeringMessage.textContent = error.message;
    }
}

// 7. Lưu phân công
offeringForm.addEventListener("submit", async event => {

    event.preventDefault();

    const data = {
        courseId: Number(
            document.getElementById("offeringCourse").value
        ),
        teacherId: Number(
            document.getElementById("offeringTeacher").value
        ),
        groupId: Number(
            document.getElementById("offeringGroup").value
        ),
        semester:
        document.getElementById("offeringSemester").value,
        academicYear:
            document.getElementById("offeringYear").value.trim(),
        sessionsPerWeek: Number(
            document.getElementById("offeringSessions").value
        )
    };

    const editing = editingOfferingId !== null;

    const url = editing
        ? `${OFFERING_API}/${editingOfferingId}`
        : OFFERING_API;

    const saveBtn =
        document.getElementById("saveOfferingBtn");

    saveBtn.disabled = true;
    offeringFormMessage.textContent = "";

    try {
        await callApi(url, {
            method: editing ? "PUT" : "POST",
            body: JSON.stringify(data)
        });

        offeringDialog.close();
        await loadOfferings();

        offeringMessage.textContent = editing
            ? "Đã cập nhật lớp học phần!"
            : "Đã tạo lớp học phần!";

    } catch (error) {
        offeringFormMessage.textContent = error.message;

    } finally {
        saveBtn.disabled = false;
    }
});

// 8. Xóa lớp học phần
async function deleteOffering(id) {

    const offering = offerings.find(o => o.id === id);
    if (!offering) return;

    const confirmed = confirm(
        `Xóa phân công ${offering.courseName} - ` +
        `${offering.groupCode}?`
    );

    if (!confirmed) return;

    try {
        await callApi(`${OFFERING_API}/${id}`, {
            method: "DELETE"
        });

        await loadOfferings();
        offeringMessage.textContent =
            "Đã xóa lớp học phần!";

    } catch (error) {
        offeringMessage.textContent = error.message;
    }
}

// 9. Gắn sự kiện các nút
document.getElementById("addOfferingBtn")
    .addEventListener("click", openAddOffering);

document.getElementById("cancelOfferingBtn")
    .addEventListener("click", () => offeringDialog.close());

document.getElementById("searchOffering")
    .addEventListener("input", renderOfferings);

/* ========================================
   MODULE THỜI KHÓA BIỂU
======================================== */

const SCHEDULE_API = "/api/schedule";

let scheduleEntries = [];
let scheduleOfferings = [];
let scheduleRooms = [];
let scheduleGroups = [];
let editingSessionId = null;

const weekPicker =
    document.getElementById("weekPicker");

const weekGrid =
    document.getElementById("weekGrid");

const scheduleDialog =
    document.getElementById("scheduleDialog");

const scheduleForm =
    document.getElementById("scheduleForm");

const scheduleMessage =
    document.getElementById("scheduleMessage");

const scheduleFormMessage =
    document.getElementById("scheduleFormMessage");

// 1. Chuyển Date thành YYYY-MM-DD theo giờ địa phương
function localDateString(date) {

    const year = date.getFullYear();

    const month = String(
        date.getMonth() + 1
    ).padStart(2, "0");

    const day = String(
        date.getDate()
    ).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

// 2. Tìm ngày thứ Hai của một tuần
function getMonday(dateString) {

    const date = new Date(
        dateString + "T12:00:00"
    );

    const day = date.getDay();
    const diff = (day + 6) % 7;

    date.setDate(date.getDate() - diff);
    return date;
}

// V2.1: Lịch tuần, lọc và xuất CSV. Giữ nguyên CRUD của bản tự học.
let scheduleLoadVersion = 0;

function scheduleFilteredEntries() {
    // Bản vá hợp nhất: bộ lọc là tính năng bổ sung, không được làm hỏng lịch nếu HTML cũ được mở.
    const room = document.getElementById('scheduleFilterRoom')?.value ?? '';
    const group = document.getElementById('scheduleFilterGroup')?.value ?? '';
    const teacher = document.getElementById('scheduleFilterTeacher')?.value ?? '';
    return scheduleEntries.filter(item =>
        (!room || String(item.roomCode ?? '') === room) &&
        (!group || String(item.groupCode ?? '') === group) &&
        (!teacher || String(item.teacherName ?? '') === teacher)
    );
}

function updateScheduleFilterOptions() {
    const fields = [
        ['scheduleFilterRoom', 'roomCode', 'Tất cả phòng'],
        ['scheduleFilterGroup', 'groupCode', 'Tất cả lớp'],
        ['scheduleFilterTeacher', 'teacherName', 'Tất cả giảng viên']
    ];
    fields.forEach(([id, key, placeholder]) => {
        const select = document.getElementById(id);
        if (!select) return;  // optional UI control missing => base schedule still works
        const selected = select.value;
        const values = [...new Set(scheduleEntries.map(item => String(item[key] ?? '').trim()).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b, 'vi'));
        select.replaceChildren();
        select.add(new Option(placeholder, ''));
        values.forEach(value => select.add(new Option(value, value)));
        select.value = values.includes(selected) ? selected : '';
    });
}

// 3. Tải thời khóa biểu đã LƯU, không tự động sử dụng dữ liệu preview
async function loadSchedule() {
    if (!weekPicker.value) weekPicker.value = localDateString(new Date());
    const monday = getMonday(weekPicker.value);
    const mondayString = localDateString(monday);
    const version = ++scheduleLoadVersion;
    scheduleMessage.textContent = 'Đang tải thời khóa biểu...';
    try {
        const result = await callApi(`${SCHEDULE_API}?weekStart=${encodeURIComponent(mondayString)}`);
        if (version !== scheduleLoadVersion) return; // tránh dữ liệu tuần cũ ghi đè tuần mới
        if (!Array.isArray(result)) throw new Error('API không trả về danh sách lịch học.');
        scheduleEntries = result;
        updateScheduleFilterOptions();
        renderSchedule(monday);
        scheduleMessage.textContent = '';
    } catch (error) {
        if (version !== scheduleLoadVersion) return;
        scheduleEntries = [];
        updateScheduleFilterOptions();
        renderSchedule(monday);
        scheduleMessage.textContent =
            'Chưa thể tải thời khóa biểu: ' + error.message +
            '. Kiểm tra Spring Boot trên cổng 8081 rồi nhấn Làm mới.';
    }
}

// 4. Lưới 7 ngày: ngắn gọn như V2 cũ, giữ Sửa/Xóa của bản tự học
function renderSchedule(monday) {
    weekGrid.replaceChildren();
    const sunday = new Date(monday);
    sunday.setDate(sunday.getDate() + 6);
    const formatDate = date => `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}`;
    const weekLabel = document.getElementById('scheduleWeekLabel');
    weekLabel.textContent = `Tuần ${formatDate(monday)} – ${formatDate(sunday)} / ${sunday.getFullYear()}`;
    const entries = scheduleFilteredEntries();
    const scheduleCounter = document.getElementById('scheduleSessionCount');
    if (scheduleCounter) scheduleCounter.textContent = `${entries.length} buổi`;
    const dayNames = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];
    const today = localDateString(new Date());

    for (let dayIndex = 0; dayIndex < 7; dayIndex++) {
        const date = new Date(monday);
        date.setDate(date.getDate() + dayIndex);
        const key = localDateString(date);
        const sessions = entries.filter(s => s.sessionDate === key)
            .sort((a,b) => String(a.startTime).localeCompare(String(b.startTime)));
        const col = document.createElement('section');
        col.className = 'schedule-day hpuni-ux-day' + (key === today ? ' hpuni-ux-today' : '');
        col.setAttribute('aria-label', `${dayNames[dayIndex]} ${formatDate(date)}, ${sessions.length} buổi`);
        const head = document.createElement('div');
        head.className = 'hpuni-ux-day-head';
        const title = document.createElement('strong');
        title.textContent = dayNames[dayIndex];
        const dateEl = document.createElement('span');
        dateEl.textContent = formatDate(date);
        head.append(title, dateEl);
        col.appendChild(head);
        const total = document.createElement('div');
        total.className = 'hpuni-ux-day-count';
        total.textContent = `${sessions.length} buổi học`;
        col.appendChild(total);
        if (!sessions.length) {
            const empty = document.createElement('p');
            empty.className = 'schedule-empty hpuni-ux-day-empty';
            empty.textContent = 'Chưa có lịch học';
            col.appendChild(empty);
        }
        sessions.forEach(session => {
            const card = document.createElement('article');
            card.className = 'schedule-card hpuni-ux-session';
            const subject = document.createElement('strong');
            subject.className = 'hpuni-ux-subject';
            subject.textContent = session.courseName ?? 'Môn học';
            const time = document.createElement('div');
            time.className = 'hpuni-ux-time';
            time.textContent = `${String(session.startTime ?? '').slice(0,5)} – ${String(session.endTime ?? '').slice(0,5)}`;
            const room = document.createElement('span');
            room.className = 'hpuni-ux-room';
            room.textContent = `▣ ${session.roomCode ?? '–'}`;
            const info = document.createElement('div');
            info.className = 'hpuni-ux-session-meta';
            const teacher = document.createElement('span');
            teacher.textContent = `GV: ${session.teacherName ?? '–'}`;
            const group = document.createElement('span');
            group.textContent = `Lớp: ${session.groupCode ?? '–'}`;
            info.append(teacher, group);
            const actions = document.createElement('div');
            actions.className = 'hpuni-ux-card-actions';
            const edit = document.createElement('button');
            edit.type = 'button';
            edit.textContent = 'Sửa';
            edit.setAttribute('aria-label', `Sửa lịch ${session.courseName ?? ''} ngày ${key}`);
            edit.addEventListener('click', () => openEditSession(session.id));
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.textContent = 'Xóa';
            remove.setAttribute('aria-label', `Xóa lịch ${session.courseName ?? ''} ngày ${key}`);
            remove.addEventListener('click', () => deleteSession(session.id));
            actions.append(edit, remove);
            card.append(subject, time, room, info, actions);
            col.appendChild(card);
        });
        weekGrid.appendChild(col);
    }
}

// Bảng tính có BOM để Excel hiển thị tiếng Việt; chỉ xuất lịch đã lưu.
function exportWeekCsv() {
    const rows = scheduleFilteredEntries();
    if (!rows.length) {
        scheduleMessage.textContent = 'Không có buổi học đã lưu phù hợp bộ lọc để xuất CSV.';
        return;
    }
    const safeCell = value => {
        let s = String(value ?? '');
        if (/^[\s]*[=+@-]/.test(s)) s = "'" + s; // tránh công thức CSV khi mở bằng Excel
        return '"' + s.replace(/"/g, '""') + '"';
    };
    const records = [['Ngày', 'Giờ bắt đầu', 'Giờ kết thúc', 'Môn học', 'Lớp', 'Giảng viên', 'Phòng'],
        ...rows.map(s => [s.sessionDate, s.startTime, s.endTime, s.courseName, s.groupCode, s.teacherName, s.roomCode])];
    const csv = '\ufeff' + records.map(row => row.map(safeCell).join(',')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], {type:'text/csv;charset=utf-8'}));
    const a = document.createElement('a');
    a.href = url;
    a.download = `HPUNI_thoi_khoa_bieu_${localDateString(getMonday(weekPicker.value))}.csv`;
    document.body.append(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 2000);
    scheduleMessage.textContent = `Đã chuẩn bị file CSV của ${rows.length} buổi đã lưu.`;
}

// 5. Chuyển tuần
function changeWeek(days) {

    const current = weekPicker.value ||
        localDateString(new Date());

    const date = new Date(current + "T12:00:00");

    date.setDate(date.getDate() + days);

    weekPicker.value = localDateString(date);

    loadSchedule();
}

// 6. Tạo option cho select
function fillScheduleSelect(selectId, items, format) {

    const select = document.getElementById(selectId);

    select.replaceChildren();

    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "-- Chọn --";

    select.appendChild(placeholder);

    items.forEach(item => {

        const option = document.createElement("option");

        option.value = String(item.id);
        option.textContent = format(item);

        select.appendChild(option);
    });
}

// 7. Tải danh sách lớp học phần, phòng và lớp
async function loadScheduleOptions() {

    [
        scheduleOfferings,
        scheduleRooms,
        scheduleGroups
    ] = await Promise.all([
        callApi("/api/catalog/offerings"),
        callApi("/api/rooms"),
        callApi("/api/catalog/groups")
    ]);

    fillScheduleSelect(
        "scheduleOffering",
        scheduleOfferings,
        o => `${o.courseName} - ${o.groupCode} ` +
            `(${o.teacherName} · ${o.semester})`
    );

    fillScheduleSelect(
        "scheduleRoom",
        scheduleRooms,
        r => `${r.code} - ${r.capacity} chỗ`
    );

    if (!scheduleOfferings.length ||
        !scheduleRooms.length ||
        !scheduleGroups.length) {

        throw new Error(
            "Cần có học phần, phòng và lớp sinh viên"
        );
    }
}

// 8. Mở form tạo lịch học
async function openAddSession() {

    editingSessionId = null;
    scheduleForm.reset();

    document.getElementById("scheduleFormTitle")
        .textContent = "Thêm buổi học";

    scheduleFormMessage.textContent = "";
    document.getElementById("freeRoomsResult")
        .textContent = "";

    try {
        await loadScheduleOptions();

        document.getElementById("scheduleDate").value =
            weekPicker.value || localDateString(new Date());

        document.getElementById("scheduleStart").value =
            "07:00";

        document.getElementById("scheduleEnd").value =
            "09:00";

        scheduleDialog.showModal();

    } catch (error) {
        scheduleMessage.textContent = error.message;
    }
}

// 9. Mở form sửa lịch học
async function openEditSession(id) {

    const session = scheduleEntries.find(s => s.id === id);

    if (!session) return;

    try {
        await loadScheduleOptions();

        editingSessionId = id;

        document.getElementById("scheduleFormTitle")
            .textContent = "Sửa buổi học";

        document.getElementById("scheduleOffering").value =
            String(session.offeringId);

        document.getElementById("scheduleRoom").value =
            String(session.roomId);

        document.getElementById("scheduleDate").value =
            session.sessionDate;

        document.getElementById("scheduleStart").value =
            session.startTime.slice(0, 5);

        document.getElementById("scheduleEnd").value =
            session.endTime.slice(0, 5);

        scheduleFormMessage.textContent = "";

        document.getElementById("freeRoomsResult")
            .textContent = "";

        scheduleDialog.showModal();

    } catch (error) {
        scheduleMessage.textContent = error.message;
    }
}

// 10. Tìm phòng trống phù hợp
async function findFreeRooms() {

    const offeringId = Number(
        document.getElementById("scheduleOffering").value
    );

    const offering = scheduleOfferings.find(
        o => o.id === offeringId
    );

    const date =
        document.getElementById("scheduleDate").value;

    const start =
        document.getElementById("scheduleStart").value;

    const end =
        document.getElementById("scheduleEnd").value;

    const result =
        document.getElementById("freeRoomsResult");

    if (!offering || !date || !start || !end ||
        start >= end) {

        result.textContent =
            "Hãy chọn học phần, ngày và giờ hợp lệ.";
        return;
    }

    const group = scheduleGroups.find(
        g => g.code === offering.groupCode
    );

    if (!group) {
        result.textContent =
            "Không tìm thấy thông tin sĩ số lớp.";
        return;
    }

    const params = new URLSearchParams({
        date: date,
        start: start,
        end: end,
        minCapacity: group.studentCount
    });

    result.textContent = "Đang tìm phòng trống...";

    try {
        const freeRooms = await callApi(
            `${SCHEDULE_API}/available-rooms?${params}`
        );

        result.textContent = freeRooms.length
            ? "Phòng phù hợp: " +
            freeRooms.map(r => r.code).join(", ")
            : "Không có phòng trống phù hợp.";

    } catch (error) {
        result.textContent = error.message;
    }
}

// 11. Lưu lịch học
scheduleForm.addEventListener("submit", async event => {

    event.preventDefault();

    const data = {
        offeringId: Number(
            document.getElementById("scheduleOffering").value
        ),
        roomId: Number(
            document.getElementById("scheduleRoom").value
        ),
        sessionDate:
        document.getElementById("scheduleDate").value,
        startTime:
        document.getElementById("scheduleStart").value,
        endTime:
        document.getElementById("scheduleEnd").value
    };

    const editing = editingSessionId !== null;

    const url = editing
        ? `${SCHEDULE_API}/${editingSessionId}`
        : SCHEDULE_API;

    const saveBtn =
        document.getElementById("saveSessionBtn");

    saveBtn.disabled = true;
    scheduleFormMessage.textContent = "";

    try {
        await callApi(url, {
            method: editing ? "PUT" : "POST",
            body: JSON.stringify(data)
        });

        scheduleDialog.close();

        // Tự chuyển đến tuần chứa buổi vừa lưu
        weekPicker.value = data.sessionDate;

        await loadSchedule();

        scheduleMessage.textContent = editing
            ? "Đã cập nhật lịch học!"
            : "Đã tạo lịch học!";

    } catch (error) {
        scheduleFormMessage.textContent = error.message;

    } finally {
        saveBtn.disabled = false;
    }
});

// 12. Xóa lịch học
async function deleteSession(id) {

    const confirmed = confirm(
        "Bạn có chắc muốn xóa buổi học này?"
    );

    if (!confirmed) return;

    try {
        await callApi(`${SCHEDULE_API}/${id}`, {
            method: "DELETE"
        });

        await loadSchedule();

        scheduleMessage.textContent =
            "Đã xóa buổi học!";

    } catch (error) {
        scheduleMessage.textContent = error.message;
    }
}

// 13. Gắn sự kiện cho giao diện
document.getElementById("addSessionBtn")
    .addEventListener("click", openAddSession);

document.getElementById("cancelSessionBtn")
    .addEventListener("click", () => scheduleDialog.close());

document.getElementById("prevWeekBtn")
    .addEventListener("click", () => changeWeek(-7));

document.getElementById("nextWeekBtn")
    .addEventListener("click", () => changeWeek(7));

weekPicker.addEventListener("change", loadSchedule);

// V2.1: Các bộ lọc hoạt động trên danh sách đã lưu, không thay đổi database.
['scheduleFilterRoom', 'scheduleFilterGroup', 'scheduleFilterTeacher'].forEach(id => {
    document.getElementById(id)?.addEventListener('change', () => {
        if (!weekPicker.value) weekPicker.value = localDateString(new Date());
        renderSchedule(getMonday(weekPicker.value));
    });
});
document.getElementById('scheduleResetFilters')?.addEventListener('click', () => {
    ['scheduleFilterRoom', 'scheduleFilterGroup', 'scheduleFilterTeacher'].forEach(id => {
        const select = document.getElementById(id);
        if (select) select.value = '';
    });
    renderSchedule(getMonday(weekPicker.value || localDateString(new Date())));
});
document.getElementById('scheduleTodayBtn')?.addEventListener('click', () => {
    weekPicker.value = localDateString(new Date());
    loadSchedule();
});
document.getElementById('scheduleRefreshBtn')?.addEventListener('click', loadSchedule);
document.getElementById('scheduleExportCsv')?.addEventListener('click', exportWeekCsv);


document.getElementById("findFreeRoomsBtn")
    .addEventListener("click", findFreeRooms);

/* ==========================================
     XẾP LỊCH THỬ VÀ XÁC NHẬN
========================================== */

(function () {
    "use strict";

    const API = "/api/auto-schedule";

    let previewToken = null;
    let lastPlan = null;
    let lastPlanSaved = false;

    // Chỉ khởi tạo khi HTML đã tải xong
    function initAutoSchedule() {

        const $ = id => document.getElementById(id);

        const previewBtn = $("autoPreviewBtn");
        const confirmBtn = $("autoApplyBtn");

        const message = $("autoMessage");
        const summary = $("autoSummary");

        const table = $("autoTable");
        const warnings = $("autoUnscheduled");

        const weekInput = $("autoWeek");

        // Kiểm tra giao diện có đủ thành phần không
        const elements = [
            previewBtn, confirmBtn,
            message, summary, table, warnings,
            weekInput, $("autoSemester"), $("autoYear"),
            $("autoResultState"), $("autoResultStats"), $("autoExportCsvBtn"), $("autoOpenWeekBtn")
        ];

        if (elements.some(el => el === null)) {
            console.error(
                "Lỗi: Thiếu thành phần HTML của Auto Schedule"
            );
            return;
        }

        // 1. Thông báo giữ nguyên ý nghĩa API của bản tự học
        function showMessage(text, isError = false) {
            message.textContent = text;
            message.classList.toggle('hpuni-ux-error', isError);
            message.classList.toggle('hpuni-ux-success', !isError);
        }

        // 2. Một preview không còn hợp lệ thì không được phê duyệt.
        function invalidatePreview(text) {
            previewToken = null;
            lastPlan = null;
            lastPlanSaved = false;
            confirmBtn.disabled = true;
            $('autoExportCsvBtn').disabled = true;
            $('autoOpenWeekBtn').hidden = true;
            const badge = $('autoResultState');
            badge.textContent = 'Chưa xếp thử';
            badge.className = 'hpuni-ux-state idle';
            $('autoResultStats').replaceChildren();
            table.replaceChildren();
            warnings.replaceChildren();
            summary.textContent = 'Chưa có phương án mới. Nhấn Xếp lịch thử để xem kết quả.';
            if (text) showMessage(text);
        }

        // 3. LẤY NGÀY HÔM NAY
        function todayString() {

            const date = new Date();

            const year = date.getFullYear();

            const month = String(
                date.getMonth() + 1
            ).padStart(2, "0");

            const day = String(
                date.getDate()
            ).padStart(2, "0");

            return `${year}-${month}-${day}`;
        }

        if (!weekInput.value) {
            weekInput.value = todayString();
        }

        invalidatePreview(
            "Chọn thông tin rồi nhấn Xếp lịch thử. " +
            "Thao tác này chưa lưu dữ liệu."
        );

        // 4. ĐỌC THÔNG TIN FORM
        function readRequest() {

            return {
                semester: $("autoSemester").value,

                academicYear:
                    $("autoYear").value.trim(),

                weekStart: weekInput.value
            };
        }

        // 5. GỌI BACKEND
        async function postJson(path, data) {

            const response = await fetch(
                API + path,
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    body: JSON.stringify(data)
                }
            );

            const raw = await response.text();

            let result;

            try {
                result = raw ? JSON.parse(raw) : {};
            } catch {
                throw new Error(
                    "API không trả về JSON hợp lệ. " +
                    "HTTP " + response.status
                );
            }

            if (!response.ok) {

                throw new Error(
                    result.detail ||
                    result.message ||
                    result.error ||
                    "Lỗi HTTP " + response.status
                );
            }

            return result;
        }

        // 6. Trình bày kết quả dễ đọc như V2, không thay đổi dữ liệu server.
        function renderPlan(plan, saved = false) {
            lastPlan = plan;
            lastPlanSaved = saved;
            table.replaceChildren();
            warnings.replaceChildren();
            const suggestions = Array.isArray(plan.suggestions) ? plan.suggestions : [];
            const notes = Array.isArray(plan.unscheduled) ? plan.unscheduled : [];
            const count = value => Number.isFinite(Number(value)) ? Number(value) : 0;
            const existing = count(plan.existingSessions);
            const needed = count(plan.neededSessions);
            const badge = $('autoResultState');
            badge.className = 'hpuni-ux-state ' + (saved ? 'saved' : 'preview');
            badge.textContent = saved ? '✓ Đã lưu' : '◌ Chưa lưu · Bản xem trước';
            summary.textContent = `Tuần ${plan.weekStart ?? '–'} · ${suggestions.length} buổi đề xuất · ${notes.length} cảnh báo`;
            confirmBtn.disabled = saved || !previewToken || suggestions.length === 0;
            $('autoExportCsvBtn').disabled = suggestions.length === 0;
            $('autoOpenWeekBtn').hidden = !saved;
            const stats = $('autoResultStats');
            stats.replaceChildren();
            [
                ['Đã có trong tuần', existing],
                ['Cần bổ sung', needed],
                [saved ? 'Vừa lưu' : 'Có thể xếp thử', suggestions.length],
                ['Chưa xếp được', notes.length]
            ].forEach(([label, value]) => {
                const tile = document.createElement('div');
                tile.className = 'hpuni-ux-stat';
                const small = document.createElement('span');
                small.textContent = label;
                const strong = document.createElement('strong');
                strong.textContent = value;
                tile.append(small, strong);
                stats.append(tile);
            });

            if (!suggestions.length) {
                const tr = document.createElement('tr');
                const td = document.createElement('td');
                td.colSpan = 7;
                td.className = 'hpuni-ux-empty-row';
                td.textContent = 'Không có buổi mới. Kiểm tra học phần hoặc các ràng buộc bên dưới.';
                tr.append(td);
                table.append(tr);
            }
            const dateVN = str => {
                const parts = String(str ?? '').split('-');
                return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : String(str ?? '');
            };
            suggestions.forEach(item => {
                const tr = document.createElement('tr');
                const vals = [
                    dateVN(item.sessionDate),
                    `${String(item.startTime ?? '').slice(0,5)} – ${String(item.endTime ?? '').slice(0,5)}`,
                    item.courseName ?? '', item.groupCode ?? '', item.teacherName ?? '', item.roomCode ?? ''
                ];
                vals.forEach((value, i) => {
                    const td = document.createElement('td');
                    td.textContent = value;
                    if (i === 2) td.className = 'hpuni-ux-bold-subject';
                    if (i === 0 || i === 1) td.className = 'hpuni-ux-keep';
                    if (i === 5) td.className = 'hpuni-ux-room-badge';
                    tr.append(td);
                });
                const explanation = document.createElement('td');
                const details = document.createElement('details');
                details.className = 'hpuni-ux-explanation';
                const title = document.createElement('summary');
                title.textContent = 'Giải thích';
                const p = document.createElement('p');
                p.textContent = String(item.explanation || 'Chưa có thông tin giải thích cho phương án này.');
                details.append(title,p);
                explanation.append(details);
                tr.append(explanation);
                table.append(tr);
            });
            if (!notes.length) {
                const li = document.createElement('li');
                li.className = 'hpuni-ux-note-ok';
                li.textContent = '✓ Không có cảnh báo.';
                warnings.append(li);
            } else {
                notes.forEach(note => {
                    const li = document.createElement('li');
                    li.textContent = String(note);
                    warnings.append(li);
                });
            }
        }

        // File CSV này là ĐỀ XUẤT, khác file CSV lịch đã lưu ở trang Thời khóa biểu.
        function exportPlanCsv() {
            if (!lastPlan || !Array.isArray(lastPlan.suggestions) || !lastPlan.suggestions.length) return;
            const cell = value => {
                let text = String(value ?? '');
                if (/^[\s]*[=+@-]/.test(text)) text = "'" + text;
                return '"' + text.replace(/"/g,'""') + '"';
            };
            const rows = [['Trạng thái', 'Ngày', 'Bắt đầu', 'Kết thúc', 'Môn', 'Lớp', 'Giảng viên', 'Phòng', 'Lý do'],
                ...lastPlan.suggestions.map(x => [lastPlanSaved ? 'ĐÃ LƯU' : 'CHƯA LƯU', x.sessionDate, x.startTime, x.endTime,
                    x.courseName, x.groupCode, x.teacherName, x.roomCode, x.explanation])];
            const content = '\ufeff' + rows.map(row=>row.map(cell).join(',')).join('\r\n');
            const url = URL.createObjectURL(new Blob([content], {type:'text/csv;charset=utf-8'}));
            const link = document.createElement('a');
            link.href = url;
            link.download = `HPUNI_de_xuat_${lastPlan.weekStart ?? 'lich'}.csv`;
            document.body.append(link);
            link.click();
            link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 2000);
            showMessage('Đã xuất CSV. Dữ liệu trong file có ghi rõ trạng thái CHƯA LƯU/ĐÃ LƯU.');
        }
        $('autoExportCsvBtn').addEventListener('click', exportPlanCsv);
        $('autoOpenWeekBtn').addEventListener('click', () => {
            if (!lastPlanSaved || !lastPlan) return;
            const picker = $('weekPicker');
            picker.value = lastPlan.weekStart;
            showPage('schedule');
        });

        // 7. NÚT XẾP LỊCH THỬ
        previewBtn.addEventListener(
            "click",
            async function () {

                invalidatePreview(
                    "Đang tính toán phương án xếp lịch..."
                );

                const request = readRequest();

                if (!request.weekStart ||
                    !/^20\d{2}-20\d{2}$/.test(
                        request.academicYear
                    )) {

                    showMessage(
                        "Nhập năm học dạng 2026-2027 " +
                        "và chọn tuần cần xếp.",
                        true
                    );

                    return;
                }

                previewBtn.disabled = true;

                try {

                    const result = await postJson(
                        "/preview",
                        request
                    );

                    if (!result.plan) {
                        throw new Error(
                            "Backend không trả về plan."
                        );
                    }

                    // Nếu người dùng đã đổi thông tin
                    // trong lúc chờ, không dùng kết quả cũ
                    if (JSON.stringify(readRequest()) !==
                        JSON.stringify(request)) {

                        invalidatePreview(
                            "Thông tin đã thay đổi. " +
                            "Hãy xếp lịch thử lại."
                        );

                        return;
                    }

                    renderPlan(result.plan, false);

                    previewToken = result.token || null;

                    confirmBtn.disabled =
                        !previewToken ||
                        result.plan.suggestions.length === 0;

                    if (result.plan.suggestions.length > 0) {

                        showMessage(
                            "Đã xếp thử thành công. " +
                            "CHƯA lưu vào database. " +
                            "Hãy kiểm tra kết quả bên dưới."
                        );

                    } else if (
                        result.plan.neededSessions === 0
                    ) {

                        showMessage(
                            "Không còn buổi cần xếp. " +
                            "Lịch đã đủ hoặc chưa có học phần."
                        );

                    } else {

                        showMessage(
                            "Chưa tìm được phương án phù hợp. " +
                            "Xem các cảnh báo bên dưới.",
                            true
                        );
                    }

                } catch (error) {

                    console.error(
                        "Xếp lịch thử lỗi:",
                        error
                    );

                    showMessage(
                        "Không thể xếp lịch thử: " +
                        error.message,
                        true
                    );

                } finally {

                    previewBtn.disabled = false;
                }
            }
        );

        // 8. NÚT XÁC NHẬN LƯU LỊCH
        confirmBtn.addEventListener(
            "click",
            async function () {

                if (!previewToken) {

                    showMessage(
                        "Bạn cần xếp lịch thử trước.",
                        true
                    );

                    return;
                }

                const accepted = confirm(
                    "Bạn có chắc muốn lưu các buổi " +
                    "đã được đề xuất vào thời khóa biểu?"
                );

                if (!accepted) return;

                const token = previewToken;

                confirmBtn.disabled = true;
                previewBtn.disabled = true;

                showMessage(
                    "Đang xác nhận và lưu lịch học..."
                );

                try {

                    const result = await postJson(
                        "/confirm",
                        { token: token }
                    );

                    renderPlan(result, true);

                    previewToken = null;

                    // Chuyển lịch tuần tới tuần vừa lưu
                    const picker = $("weekPicker");

                    if (picker) {
                        picker.value = result.weekStart;
                    }

                    showMessage(
                        `Đã lưu thành công ` +
                        `${result.suggestions.length} buổi. ` +
                        `Mở Thời khóa biểu để kiểm tra.`
                    );

                } catch (error) {

                    console.error(
                        "Xác nhận lịch lỗi:",
                        error
                    );

                    invalidatePreview();

                    showMessage(
                        "Không thể lưu lịch: " +
                        error.message +
                        ". Hãy xếp thử lại.",
                        true
                    );

                } finally {

                    previewBtn.disabled = false;
                    confirmBtn.disabled = true;
                }
            }
        );

        // 9. THAY ĐỔI THÔNG TIN => HỦY XÁC NHẬN CŨ
        [
            "autoSemester",
            "autoYear",
            "autoWeek"
        ].forEach(id => {

            const field = $(id);

            const onChange = () => {

                invalidatePreview(
                    "Thông số đã thay đổi. " +
                    "Hãy xếp lịch thử lại."
                );

                summary.textContent =
                    "Chưa có phương án mới.";

                table.replaceChildren();
                warnings.replaceChildren();
            };

            field.addEventListener("input", onChange);
            field.addEventListener("change", onChange);
        });
    }

    // 10. CHỜ HTML TẢI XONG
    if (document.readyState === "loading") {

        document.addEventListener(
            "DOMContentLoaded",
            initAutoSchedule,
            { once: true }
        );

    } else {

        initAutoSchedule();
    }

})();
