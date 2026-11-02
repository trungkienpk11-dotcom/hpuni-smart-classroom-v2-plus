
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
        teachers: "Quản lý giảng viên"
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

    if (pageName === "teachers") {
        loadTeachers();
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
