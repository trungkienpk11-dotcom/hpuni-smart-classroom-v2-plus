/* BAI 10.1 - Capybara hpuni: floating UI + local Spring Boot chat API. */
(() => {
    "use strict";

    function start() {
        if (document.getElementById("capy-launcher")) return;

        const launcher = document.createElement("button");
        launcher.type = "button";
        launcher.id = "capy-launcher";
        launcher.setAttribute("aria-label", "Mở trợ lý capybara hpuni");
        launcher.setAttribute("aria-controls", "capy-window");
        launcher.setAttribute("aria-expanded", "false");
        launcher.title = "Trò chuyện với capybara hpuni";
        // Hai anh nguoi dung tu thiet ke: dung yen va vay tay.
        // CSS chuyen anh khi hover/focus, khong can sua backend Java.
        launcher.innerHTML = `
          <img class="capy-image capy-image-idle"
               src="/capybara-idle.webp" alt="Capybara hpuni đứng bình thường"
               draggable="false" width="500" height="500">
          <img class="capy-image capy-image-wave"
               src="/capybara-wave.webp" alt=""
               aria-hidden="true" draggable="false" width="500" height="500">`;

        const windowEl = document.createElement("section");
        windowEl.id = "capy-window";
        windowEl.hidden = true;
        windowEl.setAttribute("aria-label", "Hộp thoại capybara hpuni");
        windowEl.innerHTML = `
          <div class="capy-header">
            <img class="capy-avatar" src="/capybara-idle.webp" alt="" aria-hidden="true">
            <div class="capy-header-copy"><strong>capybara hpuni</strong><span>Trợ lý quản lý phòng & lịch học</span></div>
            <button id="capy-close" type="button" aria-label="Đóng cửa sổ chat">×</button>
          </div>
          <div id="capy-messages" role="log" aria-live="polite" aria-relevant="additions text"></div>
          <div id="capy-suggestions">
            <button type="button" data-ask="Danh sách phòng học">Danh sách phòng</button>
            <button type="button" data-ask="Lịch tuần này">Lịch tuần này</button>
            <button type="button" data-ask="Cách xếp lịch">Cách xếp lịch</button>
          </div>
          <form id="capy-form">
            <textarea id="capy-text" rows="2" maxlength="500" aria-label="Nhập câu hỏi" placeholder="Hỏi capybara hpuni..."></textarea>
            <button id="capy-send" type="submit">Gửi</button>
          </form>
          <div id="capy-mode">Chế độ cơ bản · Chưa kết nối mô hình AI</div>`;

        document.body.append(launcher, windowEl);

        const $ = id => document.getElementById(id);
        const messages = $("capy-messages");
        const input = $("capy-text");
        const sendButton = $("capy-send");
        let busy = false;

        function bubble(text, mine = false) {
            const node = document.createElement("div");
            node.className = "capy-bubble " + (mine ? "capy-user" : "capy-bot");
            node.textContent = text; // Tránh chèn HTML từ dữ liệu trả về.
            messages.appendChild(node);
            messages.scrollTop = messages.scrollHeight;
            return node;
        }

        function openChat() {
            windowEl.hidden = false;
            launcher.setAttribute("aria-expanded", "true");
            input.focus();
        }

        function closeChat() {
            windowEl.hidden = true;
            launcher.setAttribute("aria-expanded", "false");
            launcher.focus();
        }

        async function send(text) {
            const message = text.trim();
            if (!message || busy) return;
            bubble(message, true);
            input.value = "";
            busy = true;
            sendButton.disabled = true;
            const typing = bubble("Capybara đang trả lời...");
            try {
                const response = await fetch("/api/assistant/chat", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ message })
                });
                const data = await response.json();
                if (!response.ok) throw new Error(data.detail || data.message || `HTTP ${response.status}`);
                typing.textContent = data.reply || "Mình chưa có câu trả lời.";
                $("capy-mode").textContent = data.mode === "local-ai"
                    ? "AI Local · Ollama (không cần API key)"
                    : "Trợ lý nội bộ · Trả lời từ dữ liệu/quy tắc";
            } catch (error) {
                typing.textContent = "Không kết nối được chatbot: " + error.message;
            } finally {
                busy = false;
                sendButton.disabled = false;
                messages.scrollTop = messages.scrollHeight;
                input.focus();
            }
        }

        launcher.addEventListener("click", () => {
            if (windowEl.hidden) openChat(); else closeChat();
        });
        $("capy-close").addEventListener("click", closeChat);
        $("capy-form").addEventListener("submit", e => {
            e.preventDefault();
            send(input.value);
        });
        input.addEventListener("keydown", e => {
            if (e.key === "Enter" && !e.shiftKey && !e.isComposing) {
                e.preventDefault();
                send(input.value);
            }
        });
        windowEl.querySelectorAll("[data-ask]").forEach(button => {
            button.addEventListener("click", () => send(button.dataset.ask));
        });
        document.addEventListener("keydown", e => {
            if (e.key === "Escape" && !windowEl.hidden) closeChat();
        });
        const sidebarLink = $("capySidebarOpen");
        if (sidebarLink) sidebarLink.addEventListener("click", openChat);
        bubble("Xin chào! 🐾 Mình là capybara hpuni. Bạn có thể hỏi về phòng học, lịch tuần này hoặc cách xếp lịch nhé!");
        fetch("/api/assistant/health")
            .then(res => res.ok ? res.json() : Promise.reject())
            .then(data => {
                $("capy-mode").textContent = data.mode === "local-ai-optional"
                    ? "AI Local đã bật · Có thể sử dụng Ollama"
                    : "Trợ lý nội bộ · AI Local chưa bật";
            }).catch(() => { $("capy-mode").textContent = "Không kết nối được chatbot"; });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start, { once: true });
    } else {
        start();
    }
})();
