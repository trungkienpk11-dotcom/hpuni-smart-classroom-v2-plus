
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

        cell.colSpan = 7;
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
        schedule: "Thời khóa biểu"
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
        showPage(button.dataset.page);
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

// 3. Tải lịch học theo tuần
async function loadSchedule() {

    if (!weekPicker.value) {
        weekPicker.value = localDateString(new Date());
    }

    const monday = getMonday(weekPicker.value);
    const weekStart = localDateString(monday);

    scheduleMessage.textContent = "Đang tải lịch học...";

    try {
        scheduleEntries = await callApi(
            `${SCHEDULE_API}?weekStart=${weekStart}`
        );

        scheduleMessage.textContent = "";
        renderSchedule(monday);

    } catch (error) {
        scheduleMessage.textContent = error.message;
    }
}

// 4. Hiển thị bảy ngày trong tuần
function renderSchedule(monday) {

    weekGrid.replaceChildren();

    const sunday = new Date(monday);
    sunday.setDate(sunday.getDate() + 6);

    document.getElementById("scheduleWeekLabel")
        .textContent =
        `Tuần ${localDateString(monday)} đến ` +
        localDateString(sunday);

    const dayNames = [
        "Thứ Hai", "Thứ Ba", "Thứ Tư",
        "Thứ Năm", "Thứ Sáu", "Thứ Bảy",
        "Chủ Nhật"
    ];

    for (let i = 0; i < 7; i++) {

        const date = new Date(monday);
        date.setDate(date.getDate() + i);

        const dateKey = localDateString(date);

        const column = document.createElement("div");
        column.className = "schedule-day";

        const heading = document.createElement("div");
        heading.className = "schedule-day-title";

        heading.textContent =
            `${dayNames[i]} - ${dateKey}`;

        column.appendChild(heading);

        const sessions = scheduleEntries
            .filter(s => s.sessionDate === dateKey)
            .sort((a, b) =>
                a.startTime.localeCompare(b.startTime)
            );

        if (sessions.length === 0) {

            const empty = document.createElement("p");
            empty.className = "schedule-empty";
            empty.textContent = "Chưa có lịch học.";

            column.appendChild(empty);
        }

        sessions.forEach(session => {

            const card = document.createElement("div");
            card.className = "schedule-card";

            const title = document.createElement("strong");
            title.textContent = session.courseName;

            const time = document.createElement("p");
            time.textContent =
                session.startTime.slice(0, 5) +
                " - " +
                session.endTime.slice(0, 5);

            const room = document.createElement("p");
            room.textContent =
                "Phòng: " + session.roomCode;

            const teacher = document.createElement("p");
            teacher.textContent =
                "GV: " + session.teacherName;

            const group = document.createElement("p");
            group.textContent =
                "Lớp: " + session.groupCode;

            const editBtn = document.createElement("button");
            editBtn.textContent = "Sửa";
            editBtn.onclick = () =>
                openEditSession(session.id);

            const deleteBtn = document.createElement("button");
            deleteBtn.textContent = "Xóa";
            deleteBtn.onclick = () =>
                deleteSession(session.id);

            card.append(
                title, time, room, teacher,
                group, editBtn, deleteBtn
            );

            column.appendChild(card);
        });

        weekGrid.appendChild(column);
    }
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

document.getElementById("findFreeRoomsBtn")
    .addEventListener("click", findFreeRooms);
