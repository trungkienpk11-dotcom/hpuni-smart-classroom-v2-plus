/* HPUNI V2+ enhancement. Giữ nguyên app.js và các thao tác CRUD/xếp lịch. */
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const escapeText = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const iso = d => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  const monday = str => {const d=new Date(str+'T12:00:00'); d.setDate(d.getDate() - ((d.getDay()+6)%7)); return iso(d);};
  const api = async path => {const response=await fetch(path,{headers:{Accept:'application/json'}});if(!response.ok)throw Error(`API HTTP ${response.status}`);return response.json();};
  let lastReport=null;
  const today=iso(new Date());
  const themes=['dark','light'];
  function setTheme(t){document.documentElement.dataset.hpuniTheme=themes.includes(t)?t:'dark';localStorage.setItem('hpuni-v2-theme',document.documentElement.dataset.hpuniTheme);$('v2Theme').textContent=t==='light'?'☾':'☀';}
  function closeMobile(){document.querySelector('.sidebar').classList.remove('v2-open');$('v2Backdrop').hidden=true;}
  function openMobile(){document.querySelector('.sidebar').classList.add('v2-open');$('v2Backdrop').hidden=false;}
  function chart(target,days){let max=Math.max(1,...days.map(x=>Number(x.sessions)||0));const labels=['T2','T3','T4','T5','T6','T7','CN'];
    target.innerHTML=days.map((x,i)=>`<div class="v2-bar-slot"><b>${Number(x.sessions)||0}</b><div class="v2-bar-track"><i style="height:${(Number(x.sessions)||0)*100/max}%"></i></div><small>${labels[i]}</small></div>`).join('');}
  function kpi(label,value){return `<div class="v2-mini-stat"><small>${escapeText(label)}</small><strong>${escapeText(value)}</strong></div>`;}
  const vnDate = value => { const d = new Date(String(value).length <= 10 ? value + 'T12:00:00' : value); return new Intl.DateTimeFormat('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric'}).format(d); };
  const shiftIso = (value, days) => { const d = new Date(value + 'T12:00:00'); d.setDate(d.getDate()+days); return iso(d); };
  function reportLegacyKpis(res){
    const totalRooms = Number(res.totalRooms ?? res.total_rooms ?? 0);
    const activeRooms = Number(res.activeRooms ?? res.active_rooms ?? 0);
    const weeklySessions = Number(res.weeklySessions ?? res.weekly_sessions ?? 0);
    return `
      <div class="report-kpi"><span>Buổi học trong tuần</span><b>${escapeText(weeklySessions)}</b></div>
      <div class="report-kpi"><span>Phòng được xếp lịch</span><b>${escapeText(activeRooms)}/${escapeText(totalRooms)}</b></div>
      <div class="report-kpi"><span>Phòng chưa xếp lịch trong tuần</span><b>${escapeText(Math.max(0,totalRooms-activeRooms))}</b></div>`;
  }
  function renderUsageRanking(rows){
    const host = $('v2ReportRanking');
    if(!host) return;
    host.replaceChildren();
    const list = (Array.isArray(rows) ? rows.slice() : []).sort((a,b)=>Number((b.SESSIONS??b.sessions) || 0)-Number((a.SESSIONS??a.sessions) || 0));
    const max = Math.max(1,...list.map(r=>Number(r.SESSIONS??r.sessions)||0));
    list.slice(0,5).forEach(room => {
      const code = room.CODE ?? room.code ?? '---';
      const sessions = Number(room.SESSIONS ?? room.sessions ?? 0);
      const row = document.createElement('div');
      row.className = 'report-legacy-rank-row';
      row.innerHTML = `<strong>${escapeText(code)}</strong><div class="report-legacy-rank-track"><i style="width:${sessions*100/max}%"></i></div><span>${escapeText(sessions)} buổi</span>`;
      host.append(row);
    });
    if(!list.length){
      host.innerHTML = '<p class="legacy-empty">Chưa có dữ liệu sử dụng phòng trong tuần này.</p>';
    }
  }
  async function report(week,todashboard=false){
    const safeWeek=monday(week);
    const res=await api(`/api/upgrade/insights?week=${encodeURIComponent(safeWeek)}`);lastReport={...res,weekStart:safeWeek};
    if(todashboard){chart($('v2DashChart'),res.byDay);$('v2DashRooms').replaceChildren();res.roomUsage.slice(0,6).forEach(room=>{
        const row=document.createElement('div');row.className='v2-usage-row';row.textContent=`${room.CODE??room.code}: ${room.SESSIONS??room.sessions} buổi`;$('v2DashRooms').append(row);
      });return;}
    if($('v2ReportWeek')) $('v2ReportWeek').value=safeWeek;
    if($('v2ReportRange')) $('v2ReportRange').textContent=`${vnDate(safeWeek)} – ${vnDate(shiftIso(safeWeek,6))}`;
    if($('v2ReportChartCaption')) $('v2ReportChartCaption').textContent=`Số buổi học theo ngày · tuần ${vnDate(safeWeek)}`;
    $('v2ReportKpis').innerHTML=reportLegacyKpis(res);
    chart($('v2ReportChart'),res.byDay);
    renderUsageRanking(res.roomUsage || []);
    if($('v2ReportRoomCount')) $('v2ReportRoomCount').textContent=`${(res.roomUsage||[]).length} phòng`;
    const tbody=$('v2ReportTable');tbody.replaceChildren();(res.roomUsage||[]).forEach(room=>{
      const tr=document.createElement('tr');
      const code=room.CODE??room.code??'';
      const name=room.NAME??room.name??`Phòng ${code}`;
      const capacity=room.CAPACITY??room.capacity??'';
      const sessions=Number(room.SESSIONS??room.sessions??0);
      tr.innerHTML=`<td><strong>${escapeText(code)}</strong></td><td>${escapeText(name)}</td><td>${escapeText(capacity)}</td><td><span class="report-session-tag ${sessions>0?'active':''}">${escapeText(sessions)} buổi</span></td>`;
      tbody.append(tr);
    });
  }
  function alertText(id,text,error=false){const x=$(id);x.textContent=text;x.className=error?'v2-alert bad':'v2-alert';}
  // Trạng thái phòng được tính từ lịch ĐÃ LƯU, không tính phương án xếp thử.
  let roomStatusData = null;
  function renderRoomStatuses() {
    const target = $('v2FreeResults');
    if (!target) return;
    target.replaceChildren();
    if (!roomStatusData) return;
    const selected = $('v2FreeFilter')?.value || 'all';
    const list = roomStatusData.rooms.filter(room =>
      selected === 'all' ||
      (selected === 'available' && room.status === 'AVAILABLE') ||
      (selected === 'occupied' && room.status === 'OCCUPIED') ||
      (selected === 'unsuitable' && room.status === 'UNSUITABLE'));
    if (!list.length) {
      const empty = document.createElement('p');
      empty.className = 'v2-alert';
      empty.textContent = 'Không có phòng thuộc trạng thái này trong khoảng thời gian đã chọn.';
      target.append(empty);
      return;
    }
    const labels = {AVAILABLE:'Còn trống', OCCUPIED:'Đã có lịch', UNSUITABLE:'Không phù hợp'};
    for (const room of list) {
      const card = document.createElement('article');
      card.className = 'v2-free-room v2-room-status-card status-' + room.status.toLowerCase();

      const head = document.createElement('div');
      head.className = 'v2-room-status-heading';
      const name = document.createElement('h3');
      name.textContent = room.code + ' · ' + room.name;
      const badge = document.createElement('span');
      badge.className = 'v2-room-status-badge';
      badge.textContent = labels[room.status] || room.status;
      head.append(name, badge);
      card.append(head);

      const detail = document.createElement('p');
      detail.textContent = room.capacity + ' chỗ · ' + (room.roomType || 'Phòng học') +
        ' · ' + (room.computers ? 'Có máy tính' : 'Không có máy tính') +
        ' · ' + (room.projector ? 'Có máy chiếu' : 'Không có máy chiếu');
      card.append(detail);

      if (room.reasons && room.reasons.length) {
        const reasons = document.createElement('p');
        reasons.className = 'v2-room-reasons';
        reasons.textContent = room.reasons.join(' · ');
        card.append(reasons);
      }
      if (room.conflicts && room.conflicts.length) {
        const title = document.createElement('strong');
        title.className = 'v2-conflict-title';
        title.textContent = 'Lịch học trùng với khoảng tìm:';
        card.append(title);
        const listEl = document.createElement('ul');
        listEl.className = 'v2-conflict-list';
        for (const session of room.conflicts) {
          const item = document.createElement('li');
          item.textContent = (session.startTime || '') + '–' + (session.endTime || '') +
            ' · ' + (session.courseName || session.courseCode || 'Học phần') +
            ' · ' + (session.groupCode || 'Lớp chưa rõ') +
            ' · ' + (session.teacherName || 'Giảng viên chưa rõ');
          listEl.append(item);
        }
        card.append(listEl);
      }
      target.append(card);
    }
  }
  async function findAvailability(e) {
    e.preventDefault();
    roomStatusData = null;
    $('v2FreeResults').replaceChildren();
    const params = new URLSearchParams({
      date:$('v2FreeDate').value, start:$('v2FreeStart').value, end:$('v2FreeEnd').value,
      capacity:$('v2FreeCapacity').value, computers:String($('v2FreeComputers').checked),
      projector:String($('v2FreeProjector').checked)
    });
    if (!params.get('date') || !params.get('start') || !params.get('end') ||
        params.get('start') >= params.get('end')) {
      return alertText('v2FreeMessage','Ngày/giờ không hợp lệ: giờ bắt đầu phải trước giờ kết thúc.',true);
    }
    if (Number(params.get('capacity')) < 1 || Number(params.get('capacity')) > 10000) {
      return alertText('v2FreeMessage','Sức chứa phải từ 1 đến 10.000.',true);
    }
    try {
      alertText('v2FreeMessage','Đang kiểm tra phòng và các buổi học đã lưu...');
      const data = await api('/api/upgrade/room-status?' + params);
      roomStatusData = data;
      const summary = $('v2RoomSummary');
      if (summary) {
        summary.hidden = false;
        summary.textContent = 'Tổng ' + data.totalRooms + ' phòng · ' +
          data.availableCount + ' còn trống và phù hợp · ' +
          data.occupiedCount + ' đã có lịch trùng giờ · ' +
          data.unsuitableCount + ' trống nhưng không đáp ứng điều kiện.';
      }
      alertText('v2FreeMessage',
        'Kết quả theo ' + params.get('date') + ', ' + params.get('start') +
        '–' + params.get('end') + '. Chỉ tính lịch đã lưu trong thời khóa biểu.');
      renderRoomStatuses();
    } catch (err) {
      if ($('v2RoomSummary')) $('v2RoomSummary').hidden = true;
      alertText('v2FreeMessage','Không thể kiểm tra phòng: ' + err.message,true);
    }
  }
  // Search toàn hệ thống, chỉ hiển thị dữ liệu chữ (không chèn HTML từ DB).
  const pages={rooms:'searchRoom',teachers:'searchTeacher',groups:'searchGroup',courses:'searchCourse',offerings:'searchOffering'};
  const pageNames={rooms:'Phòng học',teachers:'Giảng viên',groups:'Lớp sinh viên',courses:'Môn học',offerings:'Lớp học phần'};
  const endpoint={rooms:'/api/rooms',teachers:'/api/catalog/teachers',groups:'/api/catalog/groups',courses:'/api/catalog/courses',offerings:'/api/catalog/offerings'};
  let searchCache=[];
  async function initSearch(){const settled=await Promise.allSettled(Object.entries(endpoint).map(async ([page,path])=>({page,items:await api(path)})));
    searchCache=settled.flatMap(o=>o.status==='fulfilled'&&Array.isArray(o.value.items)?o.value.items.map(item=>({page:o.value.page,item})):[]);}
  function normalize(s){return String(s??'').toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g,'');}
  function searchRender(){const query=normalize($('v2Search').value.trim());let dropdown=$('v2SearchResults');dropdown.replaceChildren();if(query.length<2){dropdown.hidden=true;return;}
    let matches=searchCache.filter(row=>normalize(Object.values(row.item).join(' ')).includes(query)).slice(0,10);
    if(!matches.length){const p=document.createElement('p');p.textContent='Không có kết quả. Kiểm tra dữ liệu đã nhập.';dropdown.append(p);}
    matches.forEach(({page,item})=>{let btn=document.createElement('button');btn.type='button';btn.className='v2-search-item';btn.textContent=`${pageNames[page]} · ${item.code??item.courseCode??item.groupCode??item.name??item.fullName??''} ${item.name??item.fullName??''}`;
      btn.addEventListener('click',()=>{showPage(page);const input=$(pages[page]);if(input){input.value=item.code??item.courseCode??item.groupCode??item.name??item.fullName??'';input.dispatchEvent(new Event('input'));}dropdown.hidden=true;closeMobile();});dropdown.append(btn);});dropdown.hidden=false;
  }
  function go(page){showPage(page);closeMobile(); if(page==='reports')report($('v2ReportWeek').value).catch(err=>alertText('v2ReportKpis',err.message,true));if(page==='dashboard')report(monday(today),true).catch(()=>{});}
  function init(){
    setTheme(localStorage.getItem('hpuni-v2-theme')||'dark');
    $('v2Theme').addEventListener('click',()=>setTheme(document.documentElement.dataset.hpuniTheme==='light'?'dark':'light'));
    $('v2Menu').addEventListener('click',openMobile);$('v2Backdrop').addEventListener('click',closeMobile);
    document.querySelectorAll('.sidebar .nav-item').forEach(btn=>btn.addEventListener('click',()=>{closeMobile();const p=btn.dataset.page;if(p==='reports'||p==='availability')go(p);}));
    document.querySelectorAll('[data-go]').forEach(btn=>btn.addEventListener('click',()=>go(btn.dataset.go)));
    $('v2FreeDate').value=today;$('v2ReportWeek').value=monday(today);
    $('v2FreeForm').addEventListener('submit',findAvailability);
    if ($('v2FreeFilter')) $('v2FreeFilter').addEventListener('change',renderRoomStatuses);
    $('v2RefreshReport').addEventListener('click',()=>report($('v2ReportWeek').value).catch(err=>alertText('v2ReportKpis',err.message,true)));
    $('v2ReportWeek').addEventListener('change',()=>report($('v2ReportWeek').value).catch(err=>alertText('v2ReportKpis',err.message,true)));
    if($('v2ReportPrevWeek')) $('v2ReportPrevWeek').addEventListener('click',()=>report(shiftIso($('v2ReportWeek').value || monday(today),-7)).catch(err=>alertText('v2ReportKpis',err.message,true)));
    if($('v2ReportNextWeek')) $('v2ReportNextWeek').addEventListener('click',()=>report(shiftIso($('v2ReportWeek').value || monday(today),7)).catch(err=>alertText('v2ReportKpis',err.message,true)));
    $('v2ExportCsv').addEventListener('click',()=>{
      if(!lastReport){alert('Hãy tải báo cáo trước.');return;}
      let csv='Mã phòng,Tên phòng,Sức chứa,Số buổi\r\n'+(lastReport.roomUsage||[])
        .map(r=>[r.CODE??r.code,r.NAME??r.name??`Phòng ${r.CODE??r.code??''}`,r.CAPACITY??r.capacity,r.SESSIONS??r.sessions]
          .map(v=>'"'+String(v??'').replace(/"/g,'""')+'"').join(','))
        .join('\r\n');
      const blob=new Blob(['﻿'+csv],{type:'text/csv;charset=utf-8'}),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=`hpuni-thong-ke-${lastReport.weekStart}.csv`;a.click();URL.revokeObjectURL(url);
    });
    $('v2Search').addEventListener('input',searchRender);
    $('v2Search').addEventListener('focus',()=>{if($('v2Search').value.length>=2)searchRender();});
    document.addEventListener('click',e=>{if(!e.target.closest('.v2-search-wrap'))$('v2SearchResults').hidden=true;});
    document.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='k'){e.preventDefault();$('v2Search').focus();}if(e.key==='Escape'){closeMobile();$('v2SearchResults').hidden=true;}});
    initSearch().then(()=>$('v2ApiStatus').textContent='● Dữ liệu trực tiếp').catch(()=>{$('v2ApiStatus').textContent='● Không kết nối';});
    report(monday(today),true).catch(()=>{$('v2DashChart').textContent='Chưa tải được thống kê.';});
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init,{once:true});else init();
})();
