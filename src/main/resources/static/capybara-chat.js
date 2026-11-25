/* Ưu tiên Java/H2, chọn Ollama theo từng trình duyệt. */
(() => {
    'use strict';

    const DEFAULT_HINTS = [
        'Cách thêm giảng viên',
        'Tìm phòng trống',
        'Cách xếp lịch thử',
        'Mở thống kê báo cáo'
    ];
    const ALLOWED_PAGES = new Set([
        'dashboard','rooms','teachers','groups','courses','offerings',
        'schedule','autoSchedule','availability','reports'
    ]);
    const PAGE_LABELS = {
        dashboard:'Tổng quan',rooms:'Quản lý phòng học',teachers:'Quản lý giảng viên',
        groups:'Quản lý lớp sinh viên',courses:'Quản lý môn học',offerings:'Quản lý lớp học phần',
        schedule:'Thời khóa biểu',autoSchedule:'Xếp lịch tự động',
        availability:'Tìm phòng trống',reports:'Thống kê & báo cáo'
    };
    const $ = id => document.getElementById(id);

    function start() {
        if ($('capy-launcher')) return;

        const launcher = document.createElement('button');
        launcher.type = 'button';
        launcher.id = 'capy-launcher';
        launcher.title = 'Trò chuyện với capybara hpuni';
        launcher.setAttribute('aria-label', 'Mở trợ lý capybara hpuni');
        launcher.setAttribute('aria-controls', 'capy-window');
        launcher.setAttribute('aria-expanded', 'false');
        launcher.innerHTML = `
            <img class="capy-image capy-image-idle" src="/capybara-idle.webp"
                 alt="Capybara hpuni đứng bình thường" draggable="false" width="500" height="500">
            <img class="capy-image capy-image-wave" src="/capybara-wave.webp"
                 alt="" aria-hidden="true" draggable="false" width="500" height="500">`;

        const win = document.createElement('section');
        win.id = 'capy-window';
        win.hidden = true;
        win.setAttribute('aria-label', 'Trò chuyện với capybara hpuni');
        win.innerHTML = `
          <div class="capy-header">
            <img class="capy-avatar" src="/capybara-idle.webp" alt="" aria-hidden="true">
            <div class="capy-header-copy"><strong>capybara hpuni</strong>
              <span>Trợ lý giáo vụ · Không cần API key</span></div>
            <button id="capy-nav-toggle" type="button" aria-label="Hiện danh sách chức năng" aria-expanded="false" title="Danh sách chức năng">☷</button>
            <button id="capy-clear" type="button" aria-label="Xóa nội dung trò chuyện" title="Xóa cuộc trò chuyện">↺</button>
            <button id="capy-close" type="button" aria-label="Đóng cửa sổ chat">×</button>
          </div>
          <button id="capy-page-tip" type="button" title="Hỏi trợ lý cách dùng trang hiện tại" aria-label="Hướng dẫn trang hiện tại">🐾 Trợ giúp tại trang Tổng quan</button>
          <div id="capy-ai-control" class="capy-ai-control">
            <label for="capy-ai-toggle" class="capy-ai-switch">
              <input id="capy-ai-toggle" type="checkbox" role="switch" aria-describedby="capy-ai-status">
              <span class="capy-ai-track" aria-hidden="true"></span>
              <span class="capy-ai-name">AI cộng sinh Ollama</span>
            </label>
            <button id="capy-ai-refresh" type="button" title="Kiểm tra trạng thái Ollama" aria-label="Kiểm tra kết nối Ollama">↻</button>
            <span id="capy-ai-status" role="status" aria-live="polite">Mặc định: trợ lý nội bộ</span>
          </div>
          <div id="capy-nav-panel" aria-label="Mở nhanh chức năng" hidden></div>
          <div id="capy-messages" role="log" aria-live="polite" aria-relevant="additions"></div>
          <div id="capy-suggestions" aria-label="Câu hỏi gợi ý"></div>
          <form id="capy-form">
            <textarea id="capy-text" rows="2" maxlength="500"
                aria-label="Nhập câu hỏi cho capybara hpuni"
                placeholder="Ví dụ: Tìm phòng trống ngày mai 09:00 cho 40 SV..."></textarea>
            <button id="capy-send" type="submit">Gửi</button>
          </form>
          <div id="capy-mode" role="status">Đang kết nối trợ lý nội bộ…</div>`;

        document.body.append(launcher, win);
        const messages = $('capy-messages');
        const input = $('capy-text');
        const sendButton = $('capy-send');
        const navPanel = $('capy-nav-panel');
        const navToggle = $('capy-nav-toggle');
        const pageTip = $('capy-page-tip');
        const aiToggle = $('capy-ai-toggle');
        const aiStatus = $('capy-ai-status');
        const aiRefresh = $('capy-ai-refresh');
        const AI_KEY = 'hpuni.capybara.ai-mode.10-8';
        const getStoredAi = () => {
            try { return localStorage.getItem(AI_KEY) === 'ollama'; }
            catch (_) { return false; }
        };
        const storeAi = enabled => {
            try { localStorage.setItem(AI_KEY, enabled ? 'ollama' : 'off'); }
            catch (_) { /* Chế độ vẫn hoạt động trong tab nếu trình duyệt chặn lưu */ }
        };
        let useOllama = false; // An toàn: mặc định OFF cho tới khi kiểm tra API.
        function renderAi(message) {
            aiToggle.checked = useOllama;
            aiStatus.textContent = message || (useOllama
                ? 'AI ON · Java vẫn xử lý nghiệp vụ trước'
                : 'AI OFF · Chỉ dùng Java và H2');
        }
        async function checkAiAvailability() {
            const response = await fetch('/api/assistant/ai-status', {cache: 'no-store'});
            if (!response.ok) throw new Error('Không kiểm tra được Ollama (HTTP ' + response.status + ')');
            return response.json();
        }
        async function enableAi(showFailure = true) {
            aiToggle.disabled = true;
            aiRefresh.disabled = true;
            aiStatus.textContent = 'Đang kiểm tra Ollama…';
            try {
                const result = await checkAiAvailability();
                if (!result.available) throw new Error(result.message || 'Ollama không sẵn sàng.');
                useOllama = true;
                storeAi(true);
                renderAi('AI ON · Model ' + (result.model || 'Ollama') + ' sẵn sàng');
            } catch (e) {
                useOllama = false;
                storeAi(false);
                renderAi(showFailure ? (e.message || 'Ollama không sẵn sàng')
                    : 'AI OFF · Không kết nối được Ollama');
            } finally {
                aiToggle.disabled = false;
                aiRefresh.disabled = false;
            }
        }
        aiToggle.addEventListener('change', () => {
            if (aiToggle.checked) enableAi(true);
            else { useOllama = false; storeAi(false); renderAi(); }
        });
        aiRefresh.addEventListener('click', async () => {
            if (useOllama) { await enableAi(true); return; }
            aiRefresh.disabled = true;
            aiStatus.textContent = 'Đang kiểm tra Ollama…';
            try {
                const result = await checkAiAvailability();
                renderAi(result.available
                    ? 'Ollama sẵn sàng · Bật công tắc để dùng AI'
                    : (result.message || 'Ollama chưa sẵn sàng.'));
            } catch (e) {
                renderAi('Không kiểm tra được Ollama: ' + e.message);
            } finally { aiRefresh.disabled = false; }
        });
        renderAi();
        if (getStoredAi()) enableAi(false);
        const PAGE_HINTS = {
            dashboard:['Hướng dẫn trang này','Mở thời khóa biểu','Mở thống kê báo cáo'],
            rooms:['Hướng dẫn trang này','Danh sách phòng học','Mở tìm phòng trống'],
            teachers:['Hướng dẫn trang này','Danh sách giảng viên','Mở quản lý lớp học phần'],
            groups:['Hướng dẫn trang này','Mở quản lý lớp học phần','Mở xếp lịch tự động'],
            courses:['Hướng dẫn trang này','Danh sách môn học','Mở quản lý lớp học phần'],
            offerings:['Hướng dẫn trang này','Mở xếp lịch tự động','Cách xếp lịch thử'],
            schedule:['Hướng dẫn trang này','Lịch học tuần này','Mở xếp lịch tự động'],
            autoSchedule:['Hướng dẫn trang này','Cách xếp lịch thử','Cách xác nhận lịch'],
            availability:['Hướng dẫn trang này','Tìm phòng trống','Mở quản lý phòng học'],
            reports:['Hướng dẫn trang này','Thống kê khoa','Cách xuất CSV']
        };
        function currentPage() {
            const nav = document.querySelector('.sidebar .nav-item.active[data-page]');
            if (nav && ALLOWED_PAGES.has(nav.dataset.page)) return nav.dataset.page;
            // Dự phòng nếu menu thay đổi lớp CSS trong các bản vá giao diện sau.
            for (const page of ALLOWED_PAGES) {
                const el = $(page+'Page');
                if (el && !el.classList.contains('hidden')) return page;
            }
            return 'dashboard';
        }
        let lastPage = '';
        function updatePageTip(force = false) {
            const page = currentPage();
            pageTip.textContent = '🐾 Hỗ trợ trang: '+PAGE_LABELS[page];
            if (force || lastPage !== page) {
                lastPage = page;
                suggestions(PAGE_HINTS[page] || DEFAULT_HINTS);
            }
        }

        let busy = false;
        let count = 0;
        // Mỗi lần mở trang là một hội thoại riêng, không chia sẻ ngữ cảnh sang tab khác.
        const newConversationId = () => {
            if (window.crypto && typeof window.crypto.randomUUID === 'function') {
                return window.crypto.randomUUID();
            }
            // Fallback trên trình duyệt cũ: không dùng id định danh cá nhân.
            return 'tab-' + Math.random().toString(36).slice(2) + '-' + Date.now().toString(36);
        };
        let conversationId = newConversationId();

        function bubble(text, mine = false) {
            const node = document.createElement('div');
            node.className = 'capy-bubble ' + (mine ? 'capy-user' : 'capy-bot');
            node.textContent = String(text ?? ''); // Không chèn HTML từ API/DB.
            messages.append(node);
            count++;
            if (count > 80) { messages.firstElementChild?.remove(); count--; }
            messages.scrollTop = messages.scrollHeight;
            return node;
        }

        function suggestions(hints = DEFAULT_HINTS) {
            const host = $('capy-suggestions');
            host.replaceChildren();
            const distinct = [...new Set(Array.isArray(hints) && hints.length ? hints : DEFAULT_HINTS)].slice(0, 4);
            distinct.forEach(hint => {
                if (typeof hint !== 'string' || hint.length > 140) return;
                const button = document.createElement('button');
                button.type = 'button';
                button.textContent = hint;
                button.addEventListener('click', () => send(hint));
                host.append(button);
            });
        }

        function openChat() {
            win.hidden = false;
            launcher.setAttribute('aria-expanded', 'true');
            updatePageTip();
            input.focus();
        }
        function closeChat() {
            win.hidden = true;
            launcher.setAttribute('aria-expanded', 'false');
            launcher.focus();
        }

        function goToPage(page) {
            // Chỉ cho phép các trang đã khai báo; luôn bấm menu thật để giữ listener trong app.js.
            if (!ALLOWED_PAGES.has(page)) return false;
            const nav = document.querySelector(`.sidebar .nav-item[data-page="${page}"]`);
            if (!nav) {
                bubble('Mình chưa thể tìm thấy mục ' + PAGE_LABELS[page] + ' trong menu của phiên bản này.');
                return false;
            }
            nav.click();
            updatePageTip();
            navPanel.hidden = true;
            navToggle.setAttribute('aria-expanded', 'false');
            closeChat();
            return true;
        }

        function attachAction(replyBubble, action) {
            if (!action || action.type !== 'navigate' || !ALLOWED_PAGES.has(action.page)) return;
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'capy-action';
            button.textContent = '↗ Mở ' + PAGE_LABELS[action.page];
            button.addEventListener('click', () => goToPage(action.page));
            replyBubble.append(document.createElement('br'), button);
        }

        // Menu truy cập nhanh: không cần hỏi chatbot mới mở được các trang.
        const favorites = ['dashboard','rooms','teachers','groups','courses','offerings','schedule','autoSchedule','availability','reports'];
        favorites.forEach(page => {
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = PAGE_LABELS[page];
            button.addEventListener('click', () => goToPage(page));
            navPanel.append(button);
        });
        navToggle.addEventListener('click', () => {
            navPanel.hidden = !navPanel.hidden;
            navToggle.setAttribute('aria-expanded', String(!navPanel.hidden));
        });

        async function send(text) {
            const content = String(text ?? '').trim();
            if (!content || busy) return;
            if (content.length > 500) {
                bubble('Câu hỏi tối đa 500 ký tự. Hãy rút gọn một chút nhé.');
                return;
            }
            bubble(content, true);
            input.value = '';
            busy = true;
            sendButton.disabled = true;
            aiToggle.disabled = true;
            aiRefresh.disabled = true;
            const loading = bubble('🐾 Mình đang tra cứu dữ liệu…');
            loading.classList.add('capy-typing');
            try {
                const response = await fetch('/api/assistant/chat', {
                    method: 'POST',
                    headers: { 'Content-Type':'application/json', 'Accept':'application/json',
                               'X-HPUNI-AI-Mode': useOllama ? 'ollama' : 'off' },
                    body: JSON.stringify({ message:content, conversationId, pageContext:currentPage() })
                });
                const data = await response.json().catch(() => ({}));
                if (!response.ok) {
                    throw new Error(data.detail || data.message || `HTTP ${response.status}`);
                }
                loading.classList.remove('capy-typing');
                loading.textContent = typeof data.reply === 'string' && data.reply.trim()
                    ? data.reply : 'Mình chưa tìm được thông tin phù hợp.';
                attachAction(loading, data.action);
                suggestions(data.suggestions);
                $('capy-mode').textContent = data.mode === 'local-ai'
                    ? 'AI local (Ollama) · Không dùng API key'
                    : data.mode === 'page-aware-rule'
                        ? 'Hướng dẫn theo trang đang mở · Không dùng API key'
                        : data.mode === 'contextual-rule'
                        ? 'Hội thoại nhiều lượt · Chỉ nhớ trong tab này · Chỉ đọc'
                        : (useOllama ? 'AI ON · Câu này do chatbot Java xử lý'
                                     : 'AI OFF · Trợ lý nội bộ · Chỉ đọc');
            } catch (error) {
                loading.classList.remove('capy-typing');
                loading.textContent = 'Không kết nối được chatbot. Kiểm tra Spring Boot và thử lại. ' + error.message;
            } finally {
                busy = false;
                sendButton.disabled = false;
                aiToggle.disabled = false;
                aiRefresh.disabled = false;
                messages.scrollTop = messages.scrollHeight;
                if (!win.hidden) input.focus();
            }
        }

        pageTip.addEventListener('click', () => send('Hướng dẫn trang này'));
        // app.js cập nhật class .active khi đổi trang. Observer chỉ theo dõi class,
        // không đọc form hay nội dung nhạy cảm và không gọi API tự động.
        const navButtons = document.querySelectorAll('.sidebar .nav-item[data-page]');
        const observer = new MutationObserver(() => updatePageTip());
        navButtons.forEach(node => observer.observe(node, {attributes:true, attributeFilter:['class']}));

        launcher.addEventListener('click', () => win.hidden ? openChat() : closeChat());
        $('capy-close').addEventListener('click', closeChat);
        $('capy-clear').addEventListener('click', () => {
            if (busy) return;
            messages.replaceChildren();
            navPanel.hidden = true;
            navToggle.setAttribute('aria-expanded', 'false');
            count = 0;
            conversationId = newConversationId(); // Xóa chat đồng thời quên yêu cầu tìm phòng cũ.
            bubble('Đã làm mới cuộc trò chuyện và xóa ngữ cảnh trong tab này. Bạn cần mình giúp gì?');
            updatePageTip(true);
            input.focus();
        });
        $('capy-form').addEventListener('submit', event => {
            event.preventDefault();
            send(input.value);
        });
        input.addEventListener('keydown', event => {
            if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
                event.preventDefault();
                send(input.value);
            }
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape' && !win.hidden) closeChat();
        });
        const sidebar = $('capySidebarOpen');
        if (sidebar) sidebar.addEventListener('click', openChat);

        bubble('Xin chào! 🐾 Mình là capybara hpuni. Mình có thể tra cứu phòng/lịch thật, hướng dẫn nhiều chức năng, ' +
               'giải thích xếp lịch thử, duyệt lịch và mở trang bạn cần. Hãy thử nhắn "Tìm phòng trống", mình sẽ hỏi lần lượt các điều kiện. Nhấn ☷ để xem 10 chức năng.');
        updatePageTip(true);
        fetch('/api/assistant/health')
            .then(r => r.ok ? r.json() : Promise.reject(new Error('HTTP ' + r.status)))
            .then(() => { $('capy-mode').textContent = useOllama
                ? 'AI ON · Java xử lý dữ liệu, Ollama hỗ trợ kiến thức'
                : 'AI OFF · Trợ lý nội bộ · Chỉ đọc'; })
            .catch(() => { $('capy-mode').textContent = 'Không kết nối được chatbot; kiểm tra server'; });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start, {once:true});
    } else start();
})();
