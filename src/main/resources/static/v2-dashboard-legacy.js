
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const today = new Date();
  const escapeText = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const iso = d => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  const getMonday = date => {
    const d = new Date(date);
    d.setHours(12,0,0,0);
    const diff = (d.getDay() + 6) % 7;
    d.setDate(d.getDate() - diff);
    return d;
  };
  const addDays = (date, days) => {
    const d = new Date(date);
    d.setDate(d.getDate() + days);
    return d;
  };
  const viDate = dateLike => {
    const d = typeof dateLike === 'string' ? new Date(dateLike + 'T12:00:00') : new Date(dateLike);
    return new Intl.DateTimeFormat('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric'}).format(d);
  };
  const dayShort = dateLike => {
    const d = typeof dateLike === 'string' ? new Date(dateLike + 'T12:00:00') : new Date(dateLike);
    return new Intl.DateTimeFormat('vi-VN',{weekday:'short'}).format(d);
  };
  async function getJson(url){
    const response = await fetch(url,{headers:{Accept:'application/json'}});
    if(!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
  }
  function setText(id, value){ const el = $(id); if(el) el.textContent = value; }
  function renderSessions(list){
    const host = $('legacyDashWeekList');
    if(!host) return;
    host.innerHTML = '';
    if(!Array.isArray(list) || !list.length){
      host.innerHTML = '<p class="legacy-empty">Chưa có lịch học trong tuần này. Hãy thử chức năng Xếp lịch tự động.</p>';
      return;
    }
    list.sort((a,b)=> {
      const ak = `${a.sessionDate||''} ${a.startTime||''}`;
      const bk = `${b.sessionDate||''} ${b.startTime||''}`;
      return ak.localeCompare(bk);
    }).slice(0,6).forEach(item => {
      const article = document.createElement('article');
      article.className = 'legacy-week-item';
      article.innerHTML = `
        <div class="legacy-week-time">
          <span>${escapeText((item.startTime||'').slice(0,5))} - ${escapeText((item.endTime||'').slice(0,5))}</span>
          <small>${escapeText(dayShort(item.sessionDate))}, ${escapeText(viDate(item.sessionDate))}</small>
        </div>
        <div class="legacy-week-main">
          <strong>${escapeText(item.courseName || 'Buổi học')}</strong>
          <div class="legacy-week-meta">
            <span>👨‍🏫 ${escapeText(item.teacherName || 'Chưa rõ')}</span>
            <span>👥 ${escapeText(item.groupCode || '---')}</span>
          </div>
        </div>
        <div class="legacy-room-pill">${escapeText(item.roomCode || '---')}</div>`;
      host.appendChild(article);
    });
  }
  async function loadOverview(){
    const monday = getMonday(today);
    const sunday = addDays(monday, 6);
    setText('legacyDashWeekLabel', `Tuần ${viDate(monday)} — ${viDate(sunday)}`);
    try {
      const [teachersRes, offeringsRes, sessionsRes] = await Promise.allSettled([
        getJson('/api/catalog/teachers'),
        getJson('/api/catalog/offerings'),
        getJson(`/api/schedule?weekStart=${encodeURIComponent(iso(monday))}`)
      ]);
      const teachers = teachersRes.status === 'fulfilled' && Array.isArray(teachersRes.value) ? teachersRes.value : [];
      const offerings = offeringsRes.status === 'fulfilled' && Array.isArray(offeringsRes.value) ? offeringsRes.value : [];
      const sessions = sessionsRes.status === 'fulfilled' && Array.isArray(sessionsRes.value) ? sessionsRes.value : [];
      setText('dashTeacherCount', String(teachers.length));
      setText('dashOfferingCount', String(offerings.length));
      setText('dashSessionCount', String(sessions.length));
      setText('legacyDashSessionBadge', `${sessions.length} buổi`);
      renderSessions(sessions);
    } catch (error) {
      setText('legacyDashSessionBadge', 'Lỗi tải dữ liệu');
      const host = $('legacyDashWeekList');
      if(host) host.innerHTML = '<p class="legacy-empty">Không tải được dữ liệu lịch học tuần này.</p>';
    }
  }
  function init(){
    if (!$('dashboardPage')) return;
    const capy = $('dashboardOpenCapybara');
    if(capy){
      capy.addEventListener('click', () => {
        const toggle = $('capySidebarOpen');
        if(toggle) toggle.click();
      });
    }
    document.querySelectorAll('.nav-item[data-page="dashboard"]').forEach(btn => btn.addEventListener('click', () => setTimeout(loadOverview, 120)));
    document.querySelectorAll('[data-go="dashboard"]').forEach(btn => btn.addEventListener('click', () => setTimeout(loadOverview, 120)));
    loadOverview();
  }
  if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, {once:true});
  else init();
})();
