// dosen-jadwal-mengawas.js - UC-05: Mengelola Jadwal Mengawas Ujian (Pilih, Batalkan, Pindah, Ganti, Tukar, & Override Bentrok)
(function () {
  'use strict';

  const profileBtn = document.getElementById('btn-lecturer-profile-menu');
  const dropdownPanel = document.getElementById('lecturer-dropdown-panel');
  const logoutBtn = document.getElementById('btn-lecturer-logout');

  if (profileBtn && dropdownPanel) {
    profileBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      const isExpanded = profileBtn.getAttribute('aria-expanded') === 'true';
      profileBtn.setAttribute('aria-expanded', String(!isExpanded));
      dropdownPanel.classList.toggle('show');
    });

    document.addEventListener('click', function () {
      profileBtn.setAttribute('aria-expanded', 'false');
      dropdownPanel.classList.remove('show');
    });
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', function () {
      if (typeof API !== 'undefined' && API.logout) {
        API.logout('index.html');
        return;
      }
      if (typeof API !== 'undefined' && API.clearAuth) {
        API.clearAuth();
      }
      window.location.replace('index.html');
    });
  }

  const semesterLabelEl = document.getElementById('jmSemesterLabel');
  const summaryMyCountEl = document.getElementById('summary-my-count');
  const summaryTotalCountEl = document.getElementById('summary-total-count');
  const summaryUnassignedCountEl = document.getElementById('summary-unassigned-count');

  const feedbackBanner = document.getElementById('jm-feedback-banner');
  const feedbackText = document.getElementById('jm-feedback-text');
  const btnCloseBanner = document.getElementById('btn-close-jm-banner');

  const countMengawasSayaEl = document.getElementById('count-mengawas-saya');
  const countUjianSemesterEl = document.getElementById('count-ujian-semester');
  const listMengawasSayaEl = document.getElementById('list-mengawas-saya');
  const listUjianSemesterEl = document.getElementById('list-ujian-semester');

  const filterButtons = document.querySelectorAll('.jm-filter-pills .jp-segmented-btn');
  const pillCountAll = document.getElementById('pill-count-all');
  const pillCountAvailable = document.getElementById('pill-count-available');
  const pillCountMine = document.getElementById('pill-count-mine');
  const pillCountNeed = document.getElementById('pill-count-need');
  const inputCariUjian = document.getElementById('input-cari-ujian');

  const modalPindah = document.getElementById('modal-pindah-mengawas');
  const formPindah = document.getElementById('form-pindah-mengawas');
  const btnCloseModalPindah = document.getElementById('btn-close-modal-pindah');
  const btnBatalModalPindah = document.getElementById('btn-batal-modal-pindah');
  const pindahSourceJenis = document.getElementById('pindah-source-jenis');
  const pindahSourceCode = document.getElementById('pindah-source-code');
  const pindahSourceClass = document.getElementById('pindah-source-class');
  const pindahSourceTitle = document.getElementById('pindah-source-title');
  const pindahSourceMeta = document.getElementById('pindah-source-meta');
  const tabModeMove = document.getElementById('tab-mode-move');
  const tabModeSwap = document.getElementById('tab-mode-swap');
  const panelModeMove = document.getElementById('panel-mode-move');
  const panelModeSwap = document.getElementById('panel-mode-swap');
  const selectTargetUjianMove = document.getElementById('select-target-ujian-move');
  const selectReplacementDosen = document.getElementById('select-replacement-dosen');
  const selectTargetSwap = document.getElementById('select-target-swap');
  const pindahModalError = document.getElementById('pindah-modal-error');
  const pindahModalErrorText = document.getElementById('pindah-modal-error-text');

  const modalBentrok = document.getElementById('modal-peringatan-bentrok-mengawas');
  const btnCloseModalBentrok = document.getElementById('btn-close-modal-bentrok-mengawas');
  const bentrokMessageEl = document.getElementById('bentrok-mengawas-message');
  const listKonflikEl = document.getElementById('list-konflik-mengawas');
  const btnBatalOverride = document.getElementById('btn-batal-override-mengawas');
  const btnKonfirmasiOverride = document.getElementById('btn-konfirmasi-override-mengawas');

  const modalBatalkan = document.getElementById('modal-batalkan-mengawas');
  const batalExamNameEl = document.getElementById('batal-mengawas-exam-name');
  const btnCancelBatal = document.getElementById('btn-cancel-batal-mengawas');
  const btnConfirmBatal = document.getElementById('btn-confirm-batal-mengawas');

  const BULAN_SINGKAT = ['JAN', 'FEB', 'MAR', 'APR', 'MEI', 'JUN', 'JUL', 'AGU', 'SEP', 'OKT', 'NOV', 'DES'];
  const NAMA_HARI = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];

  const ICONS = {
    calendar: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>',
    clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>',
    mapPin: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>',
    users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>',
    check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"></polyline></svg>',
    swap: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="17 1 21 5 17 9"></polyline><path d="M3 11V9a4 4 0 0 1 4-4h14"></path><polyline points="7 23 3 19 7 15"></polyline><path d="M21 13v2a4 4 0 0 1-4 4H3"></path></svg>',
    trash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>',
    warning: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>'
  };

  let state = {
    myProctoringList: [],
    semesterExams: [],
    availableLecturers: [],
    activeFilter: 'ALL',
    searchQuery: '',
    activePindahSource: null,
    activePindahMode: 'MOVE',
    pendingCancelExam: null,
    pendingOverrideAction: null
  };

  let lastFocusedElement = null;

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  }

  function icon(name) {
    const holder = document.createElement('template');
    holder.innerHTML = ICONS[name] || '';
    return holder.content.firstElementChild;
  }

  function clear(node) {
    if (!node) return;
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  function parseIsoDate(iso) {
    const parts = String(iso || '').split('-').map(function (p) { return parseInt(p, 10); });
    if (parts.length !== 3 || parts.some(isNaN)) return null;
    return new Date(parts[0], parts[1] - 1, parts[2]);
  }

  function dateParts(iso) {
    const d = parseIsoDate(iso);
    if (!d) return { day: '-', month: '-', year: '-', hari: '-', bulanTitle: '-' };
    const bulan = BULAN_SINGKAT[d.getMonth()];
    return {
      day: String(d.getDate()).padStart(2, '0'),
      month: bulan,
      year: String(d.getFullYear()),
      hari: NAMA_HARI[d.getDay()],
      bulanTitle: bulan.charAt(0) + bulan.slice(1).toLowerCase()
    };
  }

  function formatFullDateLabel(item) {
    const p = dateParts(item.tanggal);
    const hariLabel = item.hari || p.hari;
    return hariLabel + ', ' + p.day + ' ' + p.bulanTitle + ' ' + p.year;
  }

  function formatTimeRange(mulai, selesai) {
    return (mulai || '-') + ' - ' + (selesai || '-') + ' WIB';
  }

  function buildAuthHeaders() {
    const headers = {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    };
    const token = (typeof API !== 'undefined' && API.getToken) ? API.getToken() : localStorage.getItem('jwt_token');
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }
    return headers;
  }

  function getDosenQueryParam() {
    const authNidn = localStorage.getItem('auth_id');
    return authNidn ? '?dosenId=' + encodeURIComponent(authNidn) : '';
  }

  function getCurrentLecturerNidn() {
    const authNidn = localStorage.getItem('auth_id');
    if (authNidn) return String(authNidn).trim();
    try {
      const profile = JSON.parse(localStorage.getItem('user_profile') || '{}');
      if (profile.nidn) return String(profile.nidn).trim();
      if (profile.id) return String(profile.id).trim();
    } catch (e) {
      return '';
    }
    return '';
  }

  function showFeedback(message, isError) {
    if (!feedbackBanner || !feedbackText) return;
    feedbackText.textContent = message;
    feedbackBanner.classList.remove('hidden', 'banner-success', 'banner-danger');
    feedbackBanner.classList.add(isError ? 'banner-danger' : 'banner-success');
  }

  function hideFeedback() {
    if (!feedbackBanner) return;
    feedbackBanner.classList.add('hidden');
  }

  if (btnCloseBanner) {
    btnCloseBanner.addEventListener('click', hideFeedback);
  }

  function openModal(modal, focusTarget) {
    if (!modal) return;
    lastFocusedElement = document.activeElement;
    modal.classList.remove('hidden');
    modal.classList.add('active');
    if (focusTarget && typeof focusTarget.focus === 'function') {
      focusTarget.focus();
    }
  }

  function closeModal(modal) {
    if (!modal || !modal.classList.contains('active')) return;
    modal.classList.remove('active');
    if (lastFocusedElement && typeof lastFocusedElement.focus === 'function') {
      lastFocusedElement.focus();
    }
    lastFocusedElement = null;
  }

  function dateBlock(iso) {
    const p = dateParts(iso);
    const block = el('div', 'jp-date-block');
    block.appendChild(el('span', 'jp-date-month', p.month));
    block.appendChild(el('span', 'jp-date-day', p.day));
    block.appendChild(el('span', 'jp-date-year', p.year));
    return block;
  }

  function inlineMetaItem(iconName, text) {
    const item = el('span', 'jp-meta-inline-item');
    item.appendChild(icon(iconName));
    item.appendChild(el('span', null, text));
    return item;
  }

  function buildMetaDot() {
    const sep = el('span', 'jp-meta-dot');
    sep.setAttribute('aria-hidden', 'true');
    return sep;
  }

  function buildExamMetaLine(item) {
    const meta = el('div', 'jp-meta-inline');
    meta.appendChild(inlineMetaItem('calendar', formatFullDateLabel(item)));
    meta.appendChild(buildMetaDot());
    meta.appendChild(inlineMetaItem('clock', formatTimeRange(item.jamMulai, item.jamSelesai)));
    meta.appendChild(buildMetaDot());
    meta.appendChild(inlineMetaItem('mapPin', item.ruangan || '-'));
    return meta;
  }

  function buildProctorsRow(item, isMySection) {
    const proctors = item.pengawasList || [];
    const currentNidn = getCurrentLecturerNidn();
    const row = el('div', 'jm-proctors-row');
    row.appendChild(icon('users'));

    if (!proctors.length) {
      row.appendChild(el('span', 'jm-proctors-empty-text', 'Belum ada dosen pengawas terdaftar'));
      return row;
    }

    if (isMySection) {
      const coProctors = proctors.filter(function (p) {
        return currentNidn ? p.nidn !== currentNidn : false;
      });
      if (coProctors.length > 0) {
        const names = coProctors.map(function (p) { return p.nama; }).join(', ');
        row.appendChild(el('span', 'jm-proctors-label', 'Rekan Pengawas:'));
        row.appendChild(el('span', 'jm-proctors-names', names));
      } else {
        row.appendChild(el('span', 'jm-proctors-names', 'Pengawas: ' + proctors.map(function (p) { return p.nama; }).join(', ')));
      }
      return row;
    }

    row.appendChild(el('span', 'jm-proctors-label', 'Pengawas:'));
    row.appendChild(el('span', 'jm-proctors-names', proctors.map(function (p) { return p.nama; }).join(', ')));
    return row;
  }

  function buildProctorCountBadge(count) {
    if (count === 0) {
      return el('span', 'jm-badge jm-badge-unassigned', '0 Pengawas');
    }
    return el('span', 'jm-badge jm-badge-proctors', count + ' Pengawas');
  }

  function updateSummaryAndCounts() {
    const myCount = state.myProctoringList.length;
    const allExams = state.semesterExams;
    const totalCount = allExams.length;
    const unassignedCount = allExams.filter(function (e) { return e.jumlahPengawas === 0; }).length;
    const availableCount = allExams.filter(function (e) { return !e.isAssignedToMe; }).length;
    const mineCount = allExams.filter(function (e) { return e.isAssignedToMe; }).length;

    if (summaryMyCountEl) summaryMyCountEl.textContent = myCount + ' Ujian Dipilih';
    if (summaryTotalCountEl) summaryTotalCountEl.textContent = totalCount + ' Jadwal Ujian';
    if (summaryUnassignedCountEl) summaryUnassignedCountEl.textContent = unassignedCount + ' Ujian (0 Pengawas)';

    if (countMengawasSayaEl) countMengawasSayaEl.textContent = myCount + ' Ujian';

    if (pillCountAll) pillCountAll.textContent = '(' + totalCount + ')';
    if (pillCountAvailable) pillCountAvailable.textContent = '(' + availableCount + ')';
    if (pillCountMine) pillCountMine.textContent = '(' + mineCount + ')';
    if (pillCountNeed) pillCountNeed.textContent = '(' + unassignedCount + ')';
  }

  function renderMyProctoringList() {
    clear(listMengawasSayaEl);
    const items = state.myProctoringList;
    if (!items.length) {
      listMengawasSayaEl.appendChild(
        el('div', 'jp-empty', 'Anda belum memilih jadwal mengawas pada semester ini. Silakan pilih dari daftar ujian di bawah.')
      );
      return;
    }

    items.forEach(function (item) {
      const isUas = item.jenisUjian === 'UAS';
      const card = el('article', 'jp-event-card jm-exam-card ' + (isUas ? 'jp-accent-uas' : 'jp-accent-uts'));
      card.appendChild(dateBlock(item.tanggal));

      const content = el('div', 'jp-event-content');
      const badges = el('div', 'jp-event-badges');
      badges.appendChild(el(
        'span',
        'jp-badge ' + (isUas ? 'jp-badge-red' : 'jp-badge-amber'),
        'Mengawas ' + item.jenisUjian
      ));
      badges.appendChild(el('span', 'jp-pill-code-sm', item.kodeMatkul));
      if (item.namaKelas) {
        badges.appendChild(el('span', 'jp-pill-class', item.namaKelas));
      }
      badges.appendChild(buildProctorCountBadge(item.jumlahPengawas));
      if (item.hasConflict) {
        badges.appendChild(el('span', 'jm-badge jm-badge-conflict', 'Bentrok Jadwal'));
      }
      content.appendChild(badges);

      content.appendChild(el('h3', 'jp-card-title', item.namaMatkul));
      content.appendChild(buildExamMetaLine(item));
      content.appendChild(buildProctorsRow(item, true));
      card.appendChild(content);

      const actionZone = el('div', 'jp-event-action jm-action-group');
      const btnMove = el('button', 'jm-btn-move');
      btnMove.type = 'button';
      btnMove.appendChild(icon('swap'));
      btnMove.appendChild(el('span', null, 'Pindah / Ganti'));
      btnMove.addEventListener('click', function () {
        openPindahModal(item);
      });

      const btnCancel = el('button', 'jm-btn-cancel');
      btnCancel.type = 'button';
      btnCancel.appendChild(icon('trash'));
      btnCancel.appendChild(el('span', null, 'Batalkan'));
      btnCancel.addEventListener('click', function () {
        openBatalkanModal(item);
      });

      actionZone.appendChild(btnMove);
      actionZone.appendChild(btnCancel);
      card.appendChild(actionZone);

      listMengawasSayaEl.appendChild(card);
    });
  }

  function getFilteredSemesterExams() {
    const keyword = (state.searchQuery || '').trim().toLowerCase();
    return state.semesterExams.filter(function (item) {
      if (state.activeFilter === 'AVAILABLE' && item.isAssignedToMe) return false;
      if (state.activeFilter === 'MINE' && !item.isAssignedToMe) return false;
      if (state.activeFilter === 'NEED_PROCTOR' && item.jumlahPengawas !== 0) return false;

      if (!keyword) return true;
      const haystack = [
        item.kodeMatkul,
        item.namaMatkul,
        item.namaKelas,
        item.jenisUjian,
        item.ruangan,
        item.hari,
        item.tanggal
      ].join(' ').toLowerCase();
      return haystack.indexOf(keyword) !== -1;
    });
  }

  function renderSemesterExams() {
    clear(listUjianSemesterEl);
    const filtered = getFilteredSemesterExams();
    if (countUjianSemesterEl) {
      countUjianSemesterEl.textContent = filtered.length + ' Ujian';
    }

    if (!filtered.length) {
      listUjianSemesterEl.appendChild(
        el('div', 'jp-empty', 'Tidak ada jadwal ujian yang sesuai dengan filter atau kata kunci pencarian.')
      );
      return;
    }

    filtered.forEach(function (item) {
      const isUas = item.jenisUjian === 'UAS';
      const card = el('article', 'jp-event-card jm-exam-card ' + (isUas ? 'jp-accent-uas' : 'jp-accent-uts'));
      card.appendChild(dateBlock(item.tanggal));

      const content = el('div', 'jp-event-content');
      const badges = el('div', 'jp-event-badges');
      badges.appendChild(el(
        'span',
        'jp-badge ' + (isUas ? 'jp-badge-red' : 'jp-badge-amber'),
        item.jenisUjian
      ));
      badges.appendChild(el('span', 'jp-pill-code-sm', item.kodeMatkul));
      if (item.namaKelas) {
        badges.appendChild(el('span', 'jp-pill-class', item.namaKelas));
      }
      badges.appendChild(buildProctorCountBadge(item.jumlahPengawas));
      if (item.hasConflict) {
        const conflictBadge = el('span', 'jm-badge jm-badge-conflict');
        conflictBadge.appendChild(icon('warning'));
        conflictBadge.appendChild(el('span', null, 'Bentrok Jadwal'));
        badges.appendChild(conflictBadge);
      }
      content.appendChild(badges);

      content.appendChild(el('h3', 'jp-card-title', item.namaMatkul));
      content.appendChild(buildExamMetaLine(item));
      content.appendChild(buildProctorsRow(item, false));

      if (item.hasConflict && item.conflictDetails && item.conflictDetails.length > 0) {
        const conflictNote = el('div', 'jm-conflict-note');
        conflictNote.appendChild(icon('warning'));
        conflictNote.appendChild(el('span', null, 'Beririsan dengan: ' + item.conflictDetails.join('; ')));
        content.appendChild(conflictNote);
      }

      card.appendChild(content);

      const actionZone = el('div', 'jp-event-action');
      if (item.isAssignedToMe) {
        const badgeBtn = el('button', 'jm-btn-selected');
        badgeBtn.type = 'button';
        badgeBtn.disabled = true;
        badgeBtn.appendChild(icon('check'));
        badgeBtn.appendChild(el('span', null, 'Sudah Dipilih'));
        actionZone.appendChild(badgeBtn);
      } else {
        const selectBtn = el('button', 'jm-btn-select');
        selectBtn.type = 'button';
        selectBtn.appendChild(icon('check'));
        selectBtn.appendChild(el('span', null, 'Pilih Mengawas'));
        selectBtn.addEventListener('click', function () {
          submitPilihMengawas(item.idUjian, false);
        });
        actionZone.appendChild(selectBtn);
      }

      card.appendChild(actionZone);
      listUjianSemesterEl.appendChild(card);
    });
  }

  filterButtons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      const filter = btn.getAttribute('data-filter') || 'ALL';
      state.activeFilter = filter;
      filterButtons.forEach(function (b) {
        const active = b.getAttribute('data-filter') === filter;
        b.classList.toggle('active', active);
        b.classList.toggle('is-active', active);
        b.setAttribute('aria-pressed', String(active));
      });
      renderSemesterExams();
    });
  });

  if (inputCariUjian) {
    inputCariUjian.addEventListener('input', function () {
      state.searchQuery = inputCariUjian.value || '';
      renderSemesterExams();
    });
  }

  function setPindahMode(mode) {
    state.activePindahMode = mode;
    const isMove = mode === 'MOVE';

    if (tabModeMove) {
      tabModeMove.classList.toggle('active', isMove);
      tabModeMove.setAttribute('aria-selected', String(isMove));
    }
    if (tabModeSwap) {
      tabModeSwap.classList.toggle('active', !isMove);
      tabModeSwap.setAttribute('aria-selected', String(!isMove));
    }
    if (panelModeMove) {
      panelModeMove.classList.toggle('hidden', !isMove);
    }
    if (panelModeSwap) {
      panelModeSwap.classList.toggle('hidden', isMove);
    }
    hidePindahModalError();
  }

  if (tabModeMove) {
    tabModeMove.addEventListener('click', function () { setPindahMode('MOVE'); });
  }
  if (tabModeSwap) {
    tabModeSwap.addEventListener('click', function () { setPindahMode('SWAP'); });
  }

  function showPindahModalError(msg) {
    if (!pindahModalError || !pindahModalErrorText) return;
    pindahModalErrorText.textContent = msg;
    pindahModalError.classList.remove('hidden');
  }

  function hidePindahModalError() {
    if (!pindahModalError) return;
    pindahModalError.classList.add('hidden');
  }

  function openPindahModal(sourceExam) {
    state.activePindahSource = sourceExam;
    hidePindahModalError();

    const isUas = sourceExam.jenisUjian === 'UAS';
    if (pindahSourceJenis) {
      pindahSourceJenis.textContent = sourceExam.jenisUjian;
      pindahSourceJenis.className = 'jp-badge ' + (isUas ? 'jp-badge-red' : 'jp-badge-amber');
    }
    if (pindahSourceCode) pindahSourceCode.textContent = sourceExam.kodeMatkul;
    if (pindahSourceClass) pindahSourceClass.textContent = sourceExam.namaKelas || 'Kelas A';
    if (pindahSourceTitle) pindahSourceTitle.textContent = sourceExam.namaMatkul;
    if (pindahSourceMeta) {
      pindahSourceMeta.textContent = formatFullDateLabel(sourceExam) + ' | '
        + formatTimeRange(sourceExam.jamMulai, sourceExam.jamSelesai) + ' | ' + sourceExam.ruangan;
    }

    const sourceProctorNidns = (sourceExam.pengawasList || []).map(function (p) { return p.nidn; });
    const currentNidn = getCurrentLecturerNidn();

    clear(selectTargetUjianMove);
    selectTargetUjianMove.appendChild(el('option', null, '-- Pilih Ujian Tujuan --')).value = '';
    state.semesterExams.forEach(function (exam) {
      if (exam.isAssignedToMe || exam.idUjian === sourceExam.idUjian) return;
      const opt = document.createElement('option');
      opt.value = String(exam.idUjian);
      const conflictSuffix = exam.hasConflict ? ' [Beririsan Jadwal]' : '';
      opt.textContent = exam.kodeMatkul + ' - ' + exam.namaMatkul + ' (' + exam.jenisUjian + ', '
        + formatFullDateLabel(exam) + ', ' + exam.jamMulai + '-' + exam.jamSelesai + ', '
        + exam.ruangan + ' - ' + exam.jumlahPengawas + ' Pengawas)' + conflictSuffix;
      selectTargetUjianMove.appendChild(opt);
    });

    clear(selectReplacementDosen);
    selectReplacementDosen.appendChild(el('option', null, '-- Tanpa Dosen Pengganti (Lepas Slot Asal) --')).value = '';
    state.availableLecturers.forEach(function (lect) {
      if ((currentNidn && lect.nidn === currentNidn) || sourceProctorNidns.indexOf(lect.nidn) !== -1) {
        return;
      }
      const opt = document.createElement('option');
      opt.value = lect.nidn;
      opt.textContent = lect.nama + ' (' + lect.nidn + ')';
      selectReplacementDosen.appendChild(opt);
    });

    clear(selectTargetSwap);
    selectTargetSwap.appendChild(el('option', null, '-- Pilih Dosen dan Ujian Tujuan Tukar --')).value = '';
    state.semesterExams.forEach(function (exam) {
      if (exam.isAssignedToMe || exam.idUjian === sourceExam.idUjian) return;
      (exam.pengawasList || []).forEach(function (proctor) {
        if ((currentNidn && proctor.nidn === currentNidn) || sourceProctorNidns.indexOf(proctor.nidn) !== -1) {
          return;
        }
        const opt = document.createElement('option');
        opt.value = exam.idUjian + ':' + proctor.nidn;
        opt.textContent = proctor.nama + ' - ' + exam.kodeMatkul + ' ' + exam.namaMatkul
          + ' (' + exam.jenisUjian + ', ' + formatFullDateLabel(exam) + ', '
          + exam.jamMulai + '-' + exam.jamSelesai + ')';
        selectTargetSwap.appendChild(opt);
      });
    });

    setPindahMode('MOVE');
    openModal(modalPindah, selectTargetUjianMove);
  }

  function openBatalkanModal(exam) {
    state.pendingCancelExam = exam;
    if (batalExamNameEl) {
      batalExamNameEl.textContent = exam.jenisUjian + ' ' + exam.kodeMatkul + ' - ' + exam.namaMatkul
        + ' (' + formatFullDateLabel(exam) + ')';
    }
    openModal(modalBatalkan, btnCancelBatal);
  }

  function openBentrokModal(message, conflicts, pendingAction) {
    state.pendingOverrideAction = pendingAction;
    if (bentrokMessageEl) {
      bentrokMessageEl.textContent = message || 'Jadwal mengawas bentrok dengan jadwal Anda.';
    }
    clear(listKonflikEl);
    (conflicts || []).forEach(function (item) {
      listKonflikEl.appendChild(el('li', null, item));
    });
    openModal(modalBentrok, btnBatalOverride);
  }

  async function submitPilihMengawas(idUjian, override) {
    try {
      const response = await fetch('/api/dosen/jadwal-mengawas' + getDosenQueryParam(), {
        method: 'POST',
        credentials: 'include',
        headers: buildAuthHeaders(),
        body: JSON.stringify({ idUjian: idUjian, override: Boolean(override) })
      });

      const data = await response.json().catch(function () { return {}; });

      if (response.status === 409 && data && data.conflict) {
        openBentrokModal(data.message, data.conflicts, {
          type: 'PILIH',
          idUjian: idUjian
        });
        return;
      }

      if (!response.ok) {
        showFeedback((data && data.message) || 'Gagal menyimpan jadwal mengawas.', true);
        return;
      }

      closeModal(modalBentrok);
      showFeedback((data && data.message) || 'Jadwal Berhasil Disimpan', false);
      await loadJadwalMengawas();
    } catch (err) {
      console.error('Error choosing proctoring schedule:', err);
      showFeedback('Terjadi kesalahan jaringan saat menyimpan jadwal mengawas.', true);
    }
  }

  async function submitPindahMengawas(payload, override) {
    const requestBody = {
      sourceUjianId: payload.sourceUjianId,
      targetUjianId: payload.targetUjianId,
      mode: payload.mode,
      targetDosenId: payload.targetDosenId || null,
      replacementDosenId: payload.replacementDosenId || null,
      override: Boolean(override)
    };

    try {
      const response = await fetch('/api/dosen/jadwal-mengawas/pindah' + getDosenQueryParam(), {
        method: 'POST',
        credentials: 'include',
        headers: buildAuthHeaders(),
        body: JSON.stringify(requestBody)
      });

      const data = await response.json().catch(function () { return {}; });

      if (response.status === 409 && data && data.conflict) {
        closeModal(modalPindah);
        openBentrokModal(data.message, data.conflicts, {
          type: 'PINDAH',
          payload: requestBody
        });
        return;
      }

      if (!response.ok) {
        showPindahModalError((data && data.message) || 'Gagal menyimpan perpindahan jadwal mengawas.');
        return;
      }

      closeModal(modalPindah);
      closeModal(modalBentrok);
      showFeedback((data && data.message) || 'Perpindahan jadwal mengawas berhasil disimpan.', false);
      await loadJadwalMengawas();
    } catch (err) {
      console.error('Error moving/swapping proctoring schedule:', err);
      showPindahModalError('Terjadi kesalahan jaringan saat memproses perpindahan jadwal.');
    }
  }

  async function confirmBatalkanMengawas() {
    if (!state.pendingCancelExam) return;
    const idUjian = state.pendingCancelExam.idUjian;
    try {
      const response = await fetch('/api/dosen/jadwal-mengawas/' + encodeURIComponent(idUjian) + getDosenQueryParam(), {
        method: 'DELETE',
        credentials: 'include',
        headers: buildAuthHeaders()
      });

      const data = await response.json().catch(function () { return {}; });
      closeModal(modalBatalkan);
      state.pendingCancelExam = null;

      if (!response.ok) {
        showFeedback((data && data.message) || 'Gagal membatalkan jadwal mengawas.', true);
        return;
      }

      showFeedback((data && data.message) || 'Jadwal mengawas berhasil dibatalkan.', false);
      await loadJadwalMengawas();
    } catch (err) {
      console.error('Error canceling proctoring schedule:', err);
      closeModal(modalBatalkan);
      showFeedback('Terjadi kesalahan jaringan saat membatalkan jadwal mengawas.', true);
    }
  }

  if (formPindah) {
    formPindah.addEventListener('submit', function (e) {
      e.preventDefault();
      hidePindahModalError();

      if (!state.activePindahSource) return;
      const sourceUjianId = state.activePindahSource.idUjian;

      if (state.activePindahMode === 'MOVE') {
        const targetIdStr = selectTargetUjianMove ? selectTargetUjianMove.value : '';
        if (!targetIdStr) {
          showPindahModalError('Silakan pilih ujian tujuan terlebih dahulu.');
          return;
        }
        const replacementDosenId = selectReplacementDosen ? selectReplacementDosen.value : '';
        submitPindahMengawas({
          sourceUjianId: sourceUjianId,
          targetUjianId: parseInt(targetIdStr, 10),
          mode: 'MOVE',
          replacementDosenId: replacementDosenId || null,
          targetDosenId: null
        }, false);
      } else {
        const swapVal = selectTargetSwap ? selectTargetSwap.value : '';
        if (!swapVal || swapVal.indexOf(':') === -1) {
          showPindahModalError('Silakan pilih jadwal dosen tujuan untuk ditukar.');
          return;
        }
        const parts = swapVal.split(':');
        submitPindahMengawas({
          sourceUjianId: sourceUjianId,
          targetUjianId: parseInt(parts[0], 10),
          mode: 'SWAP',
          targetDosenId: parts[1],
          replacementDosenId: null
        }, false);
      }
    });
  }

  if (btnKonfirmasiOverride) {
    btnKonfirmasiOverride.addEventListener('click', function () {
      const action = state.pendingOverrideAction;
      if (!action) {
        closeModal(modalBentrok);
        return;
      }
      state.pendingOverrideAction = null;
      if (action.type === 'PILIH') {
        submitPilihMengawas(action.idUjian, true);
      } else if (action.type === 'PINDAH') {
        submitPindahMengawas(action.payload, true);
      }
    });
  }

  if (btnConfirmBatal) {
    btnConfirmBatal.addEventListener('click', confirmBatalkanMengawas);
  }

  function wireModalClose(modal, buttons) {
    if (!modal) return;
    buttons.forEach(function (btn) {
      if (btn) {
        btn.addEventListener('click', function () {
          closeModal(modal);
        });
      }
    });
    modal.addEventListener('click', function (e) {
      if (e.target === modal) {
        closeModal(modal);
      }
    });
  }

  wireModalClose(modalPindah, [btnCloseModalPindah, btnBatalModalPindah]);
  wireModalClose(modalBentrok, [btnCloseModalBentrok, btnBatalOverride]);
  wireModalClose(modalBatalkan, [btnCancelBatal]);

  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    closeModal(modalBentrok);
    closeModal(modalPindah);
    closeModal(modalBatalkan);
  });

  async function loadJadwalMengawas() {
    try {
      const response = await fetch('/api/dosen/jadwal-mengawas' + getDosenQueryParam(), {
        method: 'GET',
        credentials: 'include',
        headers: buildAuthHeaders()
      });

      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }

      const data = await response.json();
      if (semesterLabelEl && data.semesterLabel) {
        semesterLabelEl.textContent = data.semesterLabel;
      }

      state.myProctoringList = data.myProctoringList || [];
      state.semesterExams = data.semesterExams || [];
      state.availableLecturers = data.availableLecturers || [];

      updateSummaryAndCounts();
      renderMyProctoringList();
      renderSemesterExams();
    } catch (err) {
      console.error('Error loading jadwal mengawas:', err);
      showFeedback('Gagal memuat data jadwal mengawas ujian. Silakan muat ulang halaman.', true);
      clear(listMengawasSayaEl);
      clear(listUjianSemesterEl);
      listMengawasSayaEl.appendChild(el('div', 'jp-empty', 'Jadwal mengawas tidak dapat ditampilkan.'));
      listUjianSemesterEl.appendChild(el('div', 'jp-empty', 'Daftar ujian semester tidak dapat ditampilkan.'));
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadJadwalMengawas);
  } else {
    loadJadwalMengawas();
  }
})();
