/*- CAU HINH QUY TAC XEP LICH */

document.addEventListener("DOMContentLoaded", () => {

    const $ = id => document.getElementById(id);

    const BASE = "/api/scheduling-rules";

    let courseRules = [];

    function notify(text, error = false) {
        $("rulesMessage").textContent = text;

        $("rulesMessage").style.color =
            error ? "#fda4af" : "#86efac";
    }

    async function api(url, options = {}) {

        const res = await fetch(url, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
            }
        });

        const raw = await res.text();

        let body = {};

        if (raw) {
            try {
                body = JSON.parse(raw);
            } catch {
                throw new Error("API trả về dữ liệu không hợp lệ");
            }
        }

        if (!res.ok) {
            throw new Error(
                body.detail ||
                body.message ||
                body.error ||
                `Lỗi HTTP ${res.status}`
            );
        }

        return body;
    }

    // Đặt dữ liệu vào select

    function fillSelect(id, list, display) {

        const select = $(id);

        select.replaceChildren();

        // Option mac dinh
        const placeholder =
            document.createElement("option");

        placeholder.value = "";

        placeholder.textContent = list.length
            ? "-- Chọn một mục --"
            : "-- Không có dữ liệu --";

        select.appendChild(placeholder);

        // Them danh sach
        list.forEach(item => {

            const option =
                document.createElement("option");

            option.value = String(
                item.id ?? item.courseId
            );

            option.textContent = display(item);

            select.appendChild(option);
        });

        select.disabled = list.length === 0;
    }


    // Tải dữ liệu giảng viên, môn học

    async function loadOptions() {

        // 1. Tai danh sach giang vien
        try {

            const teachers = await api(
                BASE + "/teachers"
            );

            fillSelect(
                "ruleTeacher",
                teachers,
         t => `${t.code} - ${t.fullName}`
            );

            if (!teachers.length) {
                notify(
                    "Database chưa có giảng viên.",
                    true
                );
            }

        } catch (error) {

            console.error("Lỗi tải giảng viên:", error);

            notify(
                "Không tải được giảng viên: " +
                error.message,
          true
            );
        }

        // 2. Tai danh sach mon hoc
        try {

            const rules = await api(
                BASE + "/courses"
            );

            courseRules = rules;

            fillSelect(
                "ruleCourse",
                rules,
                c => `${c.courseCode} - ${c.courseName}`
            );

            updateCheckboxes();

            if (!rules.length) {
                notify(
                    "Database chưa có môn học.",
                    true
                );
            }

        } catch (error) {

            console.error("Lỗi tải môn học:", error);

            notify(
                "Không tải được môn học: " +
                error.message,
                true
            );
        }
    }


    function updateCheckboxes() {

        const id = Number($("ruleCourse").value);

        const rule = courseRules.find(
            c => c.courseId === id
        );

        $("needsComputers").checked =
            rule?.needsComputers ?? false;

        $("needsProjector").checked =
            rule?.needsProjector ?? false;
    }

    // Thông báo module Xếp lịch thử phải tính lại
    function invalidatePreview() {

        $("autoWeek").dispatchEvent(
            new Event("change")
        );
    }

    // Tải lịch bận của tuần đang xem
    async function loadBusy() {

        const week = $("autoWeek").value;

        if (!week) return;

        const list = await api(
            `${BASE}/busy?weekStart=${encodeURIComponent(week)}`
        );

        const container = $("teacherBusyList");
        container.replaceChildren();

        if (!list.length) {
            const p = document.createElement("p");
            p.textContent = "Chưa có lịch bận trong tuần.";
            container.appendChild(p);
        }

        list.forEach(item => {

            const row = document.createElement("div");
            row.className = "busy-item";

            const text = document.createElement("span");

            text.textContent =
                `${item.teacherName} - ${item.date} ` +
                `${item.start.slice(0, 5)}–` +
                `${item.end.slice(0, 5)} ` +
                (item.reason || "");

            const button = document.createElement("button");
            button.textContent = "Xóa";

            button.addEventListener("click", async () => {

                if (!confirm("Xóa lịch bận này?")) return;

                try {
                    await api(`${BASE}/busy/${item.id}`, {
                        method: "DELETE"
                    });

                    await loadBusy();
                    invalidatePreview();
                    notify("Đã xóa lịch bận.");

                } catch (err) {
                    notify(err.message, true);
                }
            });

            row.append(text, button);
            container.appendChild(row);
        });
    }

    // Lưu giờ bận
    $("saveBusyBtn").addEventListener("click", async () => {

        const data = {
            teacherId: Number($("ruleTeacher").value),
            date: $("ruleDate").value,
            start: $("ruleStart").value,
            end: $("ruleEnd").value,
            reason: $("ruleReason").value.trim()
        };

        if (!data.teacherId || !data.date ||
            !data.start || !data.end ||
            data.start >= data.end) {

            notify("Hãy nhập lịch bận hợp lệ.", true);
            return;
        }

        try {
            await api(BASE + "/busy", {
                method: "POST",
                body: JSON.stringify(data)
            });

            $("autoWeek").value = data.date;

            await loadBusy();
            invalidatePreview();

            notify("Đã lưu lịch bận giảng viên.");

        } catch (err) {
            notify(err.message, true);
        }
    });

    // Cập nhật yêu cầu phòng
    $("saveCourseRuleBtn").addEventListener(
        "click", async () => {

            const courseId = Number(
                $("ruleCourse").value
            );

            if (!courseId) {
                notify("Hãy chọn môn học.", true);
                return;
            }

            const data = {
                needsComputers:
                $("needsComputers").checked,
                needsProjector:
                $("needsProjector").checked
            };

            try {
                await api(`${BASE}/courses/${courseId}`, {
                    method: "PUT",
                    body: JSON.stringify(data)
                });

                // Tải lại trạng thái cấu hình
                courseRules = await api(
                    BASE + "/courses"
                );

                updateCheckboxes();
                invalidatePreview();

                notify("Đã lưu yêu cầu thiết bị của môn.");

            } catch (err) {
                notify(err.message, true);
            }
        }
    );

    $("ruleCourse").addEventListener(
        "change",
        updateCheckboxes
    );

    $("autoWeek").addEventListener(
        "change",
        () => {
            loadBusy().catch(
                err => notify(err.message, true)
            );
        }
    );

    // Khởi động
    loadOptions()
        .then(loadBusy)
        .catch(err => notify(err.message, true));
});
