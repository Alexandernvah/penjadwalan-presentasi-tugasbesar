// dosen-jadwal-pribadi.js - Lecturer personal schedule: routine classes, exam supervision, presentations
(function () {
  'use strict';

  // ---------------------------------------------------------------------
  // Profile dropdown (consistent with the other lecturer pages)
  // ---------------------------------------------------------------------
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
      if (typeof API !== 'undefined' && API.clearAuth) {
        API.clearAuth();
      }
      window.location.href = 'index.html';
    });
  }

  // ---------------------------------------------------------------------
  // DOM references
  // ---------------------------------------------------------------------
  const filterButtons = document.querySelectorAll('.jp-segmented-btn');
  const filterCountSemua = document.getElementById('filter-count-semua');
  const filterCountUjian = document.getElementById('filter-count-ujian');
  const filterCountPresentasi = document.getElementById('filter-count-presentasi');

  const summaryRutinValue = document.getElementById('summary-rutin-value');
  const summaryUjianValue = document.getElementById('summary-ujian-value');
  const summaryPresentasiValue = document.getElementById('summary-presentasi-value');

  const sectionCountRutin = document.getElementById('section-count-rutin');
  const sectionCountUjian = document.getElementById('section-count-ujian');
  const sectionCountPresentasi = document.getElementById('section-count-presentasi');

  const sectionRutin = document.getElementById('section-rutin');
  const sectionUjian = document.getElementById('section-ujian');
  const sectionPresentasi = document.getElementById('section-presentasi');
  const listRutin = document.getElementById('list-jadwal-rutin');
  const listUjian = document.getElementById('list-jadwal-ujian');
  const listPresentasi = document.getElementById('list-jadwal-presentasi');
  const errorBanner = document.getElementById('jp-error-banner');
  const errorText = document.getElementById('jp-error-text');

  const modalMahasiswa = document.getElementById('modal-daftar-mahasiswa');
  const btnCloseMahasiswa = document.getElementById('btn-close-modal-mahasiswa');
  const btnTutupMahasiswa = document.getElementById('btn-tutup-modal-mahasiswa');
  const mahasiswaCourseCode = document.getElementById('mahasiswa-course-code');
  const mahasiswaCourseClass = document.getElementById('mahasiswa-course-class');
  const mahasiswaCourseTitle = document.getElementById('mahasiswa-course-title');
  const mahasiswaCourseMeta = document.getElementById('mahasiswa-course-meta');
  const mahasiswaCountBadge = document.getElementById('mahasiswa-count-badge');
  const inputCariMahasiswa = document.getElementById('input-cari-mahasiswa');
  const tbodyMahasiswa = document.getElementById('tbody-daftar-mahasiswa');

  const modalPresentasi = document.getElementById('modal-detail-presentasi');
  const btnClosePresentasi = document.getElementById('btn-close-modal-presentasi');
  const btnTutupPresentasi = document.getElementById('btn-tutup-modal-presentasi');
  const modalPresentasiTitle = document.getElementById('modal-presentasi-title');
  const detailMataKuliah = document.getElementById('detail-mata-kuliah');
  const detailTopik = document.getElementById('detail-topik');
  const detailWaktu = document.getElementById('detail-waktu');
  const detailRuangan = document.getElementById('detail-ruangan');
  const detailAnggotaCount = document.getElementById('detail-anggota-count');
  const detailAnggota = document.getElementById('detail-anggota');

  // ---------------------------------------------------------------------
  // State
  // ---------------------------------------------------------------------
  let activeFilter = 'SEMUA';
  let activeStudents = [];
  let lastFocusedElement = null;

  const BULAN_SINGKAT = ['JAN', 'FEB', 'MAR', 'APR', 'MEI', 'JUN', 'JUL', 'AGU', 'SEP', 'OKT', 'NOV', 'DES'];
  const NAMA_HARI = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];

  const ICONS = {
    clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>',
    mapPin: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>',
    users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>',
    arrowRight: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>',
    eye: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>'
  };

  // ---------------------------------------------------------------------
  // DOM helpers (all dynamic text goes through textContent)
  // ---------------------------------------------------------------------
  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  }

  function icon(name) {
    const holder = document.createElement('template');
    holder.innerHTML = ICONS[name];
    return holder.content.firstElementChild;
  }

  function metaRow(iconName, text) {
    const row = el('div', 'jp-meta-row');
    row.appendChild(icon(iconName));
    row.appendChild(el('span', null, text));
    return row;
  }

  function inlineMetaItem(iconName, text) {
    const item = el('span', 'jp-meta-inline-item');
    item.appendChild(icon(iconName));
    item.appendChild(el('span', null, text));
    return item;
  }

  function clear(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  function emptyState(message) {
    return el('div', 'jp-empty', message);
  }

  // ---------------------------------------------------------------------
  // Formatting
  // ---------------------------------------------------------------------
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

  function timeRange(mulai, selesai) {
    return mulai + ' - ' + selesai + ' WIB';
  }

  function courseTitle(kode, nama) {
    return kode + ' - ' + nama;
  }

  function extractInitials(fullName) {
    const clean = String(fullName || '').trim();
    if (!clean) return '?';
    const parts = clean.split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    }
    return clean.slice(0, 2).toUpperCase();
  }

  // ---------------------------------------------------------------------
  // Counters & Summary Strip
  // ---------------------------------------------------------------------
  function updateCounts(rutinCount, ujianCount, presentasiCount) {
    const totalSemua = rutinCount + ujianCount + presentasiCount;

    if (filterCountSemua) filterCountSemua.textContent = '(' + totalSemua + ')';
    if (filterCountUjian) filterCountUjian.textContent = '(' + ujianCount + ')';
    if (filterCountPresentasi) filterCountPresentasi.textContent = '(' + presentasiCount + ')';

    if (summaryRutinValue) summaryRutinValue.textContent = rutinCount + ' Kelas Aktif';
    if (summaryUjianValue) summaryUjianValue.textContent = ujianCount + ' Jadwal UTS/UAS';
    if (summaryPresentasiValue) summaryPresentasiValue.textContent = presentasiCount + ' Kelompok Terjadwal';

    if (sectionCountRutin) sectionCountRutin.textContent = rutinCount + ' Kelas';
    if (sectionCountUjian) sectionCountUjian.textContent = ujianCount + ' Jadwal';
    if (sectionCountPresentasi) sectionCountPresentasi.textContent = presentasiCount + ' Kelompok';
  }

  // ---------------------------------------------------------------------
  // Rendering: Jadwal Rutin
  // ---------------------------------------------------------------------
  function renderRutin(items) {
    clear(listRutin);
    if (!items.length) {
      listRutin.appendChild(emptyState('Belum ada jadwal mengajar pada semester aktif.'));
      return;
    }

    items.forEach(function (item) {
      const card = el('article', 'jp-rutin-card');

      const topRow = el('div', 'jp-rutin-top');
      const pills = el('div', 'jp-rutin-pills');
      pills.appendChild(el('span', 'jp-pill-code', item.kodeMatkul));
      pills.appendChild(el('span', 'jp-pill-class', item.namaKelas));
      topRow.appendChild(pills);

      const status = el('span', 'jp-status-mengajar');
      const dot = el('span', 'jp-status-dot');
      dot.setAttribute('aria-hidden', 'true');
      status.appendChild(dot);
      status.appendChild(el('span', null, 'Mengajar'));
      topRow.appendChild(status);
      card.appendChild(topRow);

      card.appendChild(el('h3', 'jp-card-title', item.namaMatkul));

      const meta = el('div', 'jp-meta-list');
      meta.appendChild(metaRow('clock', item.jadwalRingkas));
      meta.appendChild(metaRow('mapPin', item.ruangan));
      card.appendChild(meta);

      const footer = el('div', 'jp-rutin-footer');
      const btn = el('button', 'jp-btn-card-footer');
      btn.type = 'button';

      const btnLeft = el('span', 'jp-btn-card-footer-left');
      btnLeft.appendChild(icon('users'));
      btnLeft.appendChild(el('span', null, 'Daftar Mahasiswa (' + item.jumlahMahasiswa + ')'));
      btn.appendChild(btnLeft);

      const arrow = el('span', 'jp-btn-card-footer-arrow');
      arrow.appendChild(icon('arrowRight'));
      btn.appendChild(arrow);

      btn.addEventListener('click', function () {
        openMahasiswaModal(item);
      });
      footer.appendChild(btn);
      card.appendChild(footer);

      listRutin.appendChild(card);
    });
  }

  // ---------------------------------------------------------------------
  // Rendering: Jadwal Ujian & Presentasi (3-Zone Horizontal Cards)
  // ---------------------------------------------------------------------
  function dateBlock(iso) {
    const p = dateParts(iso);
    const block = el('div', 'jp-date-block');
    block.appendChild(el('span', 'jp-date-month', p.month));
    block.appendChild(el('span', 'jp-date-day', p.day));
    block.appendChild(el('span', 'jp-date-year', p.year));
    return block;
  }

  function buildInlineMeta(waktuMulai, waktuSelesai, ruangan) {
    const meta = el('div', 'jp-meta-inline');
    meta.appendChild(inlineMetaItem('clock', timeRange(waktuMulai, waktuSelesai)));
    const sep = el('span', 'jp-meta-dot');
    sep.setAttribute('aria-hidden', 'true');
    meta.appendChild(sep);
    meta.appendChild(inlineMetaItem('mapPin', ruangan));
    return meta;
  }

  function renderUjian(items) {
    clear(listUjian);
    if (!items.length) {
      listUjian.appendChild(emptyState('Belum ada jadwal pengawasan ujian.'));
      return;
    }

    items.forEach(function (item) {
      const isUas = item.jenisUjian === 'UAS';
      const card = el('article', 'jp-event-card ' + (isUas ? 'jp-accent-uas' : 'jp-accent-uts'));
      card.appendChild(dateBlock(item.tanggal));

      const content = el('div', 'jp-event-content');
      const badges = el('div', 'jp-event-badges');
      badges.appendChild(el(
        'span',
        'jp-badge ' + (isUas ? 'jp-badge-red' : 'jp-badge-amber'),
        'Mengawas ' + item.jenisUjian
      ));
      badges.appendChild(el('span', 'jp-pill-code-sm', item.kodeMatkul));
      content.appendChild(badges);

      content.appendChild(el('h3', 'jp-card-title', item.namaMatkul));
      content.appendChild(buildInlineMeta(item.waktuMulai, item.waktuSelesai, item.ruangan));
      card.appendChild(content);

      const actionZone = el('div', 'jp-event-action');
      actionZone.appendChild(el(
        'span',
        'jp-event-status-chip ' + (isUas ? 'jp-chip-uas' : 'jp-chip-uts'),
        'Pengawasan'
      ));
      card.appendChild(actionZone);

      listUjian.appendChild(card);
    });
  }

  function renderPresentasi(items) {
    clear(listPresentasi);
    if (!items.length) {
      listPresentasi.appendChild(emptyState('Belum ada presentasi tugas besar yang dijadwalkan.'));
      return;
    }

    items.forEach(function (item) {
      const card = el('article', 'jp-event-card jp-accent-presentasi');
      card.appendChild(dateBlock(item.tanggal));

      const content = el('div', 'jp-event-content');
      const badges = el('div', 'jp-event-badges');
      badges.appendChild(el('span', 'jp-badge jp-badge-green', 'Presentasi Tugas Besar'));
      badges.appendChild(el('span', 'jp-pill-code-sm', item.kodeMatkul));
      content.appendChild(badges);

      content.appendChild(el(
        'h3',
        'jp-card-title',
        item.namaMatkul + ' (' + item.namaKelompok + ')'
      ));
      content.appendChild(buildInlineMeta(item.waktuMulai, item.waktuSelesai, item.ruangan));
      card.appendChild(content);

      const actionZone = el('div', 'jp-event-action');
      const btn = el('button', 'jp-btn-detail');
      btn.type = 'button';
      btn.appendChild(icon('eye'));
      btn.appendChild(el('span', null, 'Lihat Detail'));
      btn.addEventListener('click', function () {
        openPresentasiModal(item);
      });
      actionZone.appendChild(btn);
      card.appendChild(actionZone);

      listPresentasi.appendChild(card);
    });
  }

  // ---------------------------------------------------------------------
  // Modals
  // ---------------------------------------------------------------------
  function openModal(modal, focusTarget) {
    lastFocusedElement = document.activeElement;
    modal.classList.add('active');
    if (focusTarget) focusTarget.focus();
  }

  function closeModal(modal) {
    if (!modal.classList.contains('active')) return;
    modal.classList.remove('active');
    if (lastFocusedElement && typeof lastFocusedElement.focus === 'function') {
      lastFocusedElement.focus();
    }
    lastFocusedElement = null;
  }

  function renderStudentRows(filterText) {
    clear(tbodyMahasiswa);
    const keyword = (filterText || '').trim().toLowerCase();
    const matches = activeStudents.filter(function (s) {
      if (!keyword) return true;
      return String(s.nama).toLowerCase().indexOf(keyword) !== -1
        || String(s.npm).toLowerCase().indexOf(keyword) !== -1;
    });

    if (!matches.length) {
      const row = document.createElement('tr');
      const cell = el('td', 'jp-student-empty', keyword
        ? 'Tidak ada mahasiswa yang cocok dengan pencarian.'
        : 'Belum ada mahasiswa terdaftar pada kelas ini.');
      cell.colSpan = 3;
      row.appendChild(cell);
      tbodyMahasiswa.appendChild(row);
      return;
    }

    matches.forEach(function (student, index) {
      const row = document.createElement('tr');
      row.appendChild(el('td', 'jp-col-no', String(index + 1)));

      const npmCell = el('td', 'jp-col-npm');
      npmCell.appendChild(el('span', 'jp-npm-pill', student.npm));
      row.appendChild(npmCell);

      row.appendChild(el('td', 'jp-col-nama', student.nama));
      tbodyMahasiswa.appendChild(row);
    });
  }

  function openMahasiswaModal(item) {
    activeStudents = (item.daftarMahasiswa || []).slice().sort(function (a, b) {
      return String(a.npm).localeCompare(String(b.npm));
    });

    if (mahasiswaCourseCode) mahasiswaCourseCode.textContent = item.kodeMatkul;
    if (mahasiswaCourseClass) mahasiswaCourseClass.textContent = item.namaKelas;
    mahasiswaCourseTitle.textContent = item.namaMatkul;
    mahasiswaCourseMeta.textContent = item.jadwalRingkas + ' | ' + item.ruangan;
    mahasiswaCountBadge.textContent = item.jumlahMahasiswa + ' Mahasiswa';
    inputCariMahasiswa.value = '';
    renderStudentRows('');
    openModal(modalMahasiswa, inputCariMahasiswa);
  }

  function openPresentasiModal(item) {
    const p = dateParts(item.tanggal);

    modalPresentasiTitle.textContent = 'Detail Presentasi - ' + item.namaKelompok;
    detailMataKuliah.textContent = courseTitle(item.kodeMatkul, item.namaMatkul);
    detailTopik.textContent = item.topik || 'Belum ada topik tugas besar';
    detailWaktu.textContent = p.hari + ', ' + p.day + ' ' + p.bulanTitle + ' ' + p.year
      + ', ' + timeRange(item.waktuMulai, item.waktuSelesai);
    detailRuangan.textContent = item.ruangan;

    clear(detailAnggota);
    const anggota = item.anggotaKelompok || [];
    if (detailAnggotaCount) detailAnggotaCount.textContent = anggota.length + ' Anggota';

    if (!anggota.length) {
      detailAnggota.appendChild(el('div', 'jp-members-empty', 'Belum ada anggota pada kelompok ini.'));
    } else {
      anggota.forEach(function (member) {
        const row = el('div', 'jp-member-row');

        const left = el('div', 'jp-member-left');
        const avatar = el('span', 'jp-avatar-circle', extractInitials(member.nama));
        avatar.setAttribute('aria-hidden', 'true');
        left.appendChild(avatar);
        left.appendChild(el('span', 'jp-member-name', member.nama));
        row.appendChild(left);

        row.appendChild(el('span', 'jp-npm-pill', member.npm));
        detailAnggota.appendChild(row);
      });
    }

    openModal(modalPresentasi, btnTutupPresentasi);
  }

  function wireModal(modal, closeButtons) {
    closeButtons.forEach(function (btn) {
      if (btn) btn.addEventListener('click', function () { closeModal(modal); });
    });
    modal.addEventListener('click', function (e) {
      if (e.target === modal) closeModal(modal);
    });
  }

  wireModal(modalMahasiswa, [btnCloseMahasiswa, btnTutupMahasiswa]);
  wireModal(modalPresentasi, [btnClosePresentasi, btnTutupPresentasi]);

  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    closeModal(modalMahasiswa);
    closeModal(modalPresentasi);
  });

  inputCariMahasiswa.addEventListener('input', function () {
    renderStudentRows(inputCariMahasiswa.value);
  });

  // ---------------------------------------------------------------------
  // Segmented filter
  // ---------------------------------------------------------------------
  function applyFilter(filter) {
    activeFilter = filter;

    filterButtons.forEach(function (btn) {
      const isActive = btn.getAttribute('data-filter') === filter;
      btn.classList.toggle('active', isActive);
      btn.classList.toggle('is-active', isActive);
      btn.setAttribute('aria-pressed', String(isActive));
    });

    sectionRutin.classList.remove('hidden');
    sectionUjian.classList.toggle('hidden', filter === 'PRESENTASI');
    sectionPresentasi.classList.toggle('hidden', filter === 'UJIAN');
  }

  filterButtons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      applyFilter(btn.getAttribute('data-filter'));
    });
  });

  // ---------------------------------------------------------------------
  // Data loading
  // ---------------------------------------------------------------------
  function showError(message) {
    errorText.textContent = message;
    errorBanner.classList.remove('hidden');
  }

  function hideError() {
    errorBanner.classList.add('hidden');
  }

  async function loadJadwalPribadi() {
    try {
      const authNidn = localStorage.getItem('auth_id');
      const queryParam = authNidn ? '?dosenId=' + encodeURIComponent(authNidn) : '';

      const response = await fetch('/api/dosen/jadwal-pribadi' + queryParam, {
        headers: { 'Accept': 'application/json' }
      });
      if (!response.ok) {
        throw new Error('Gagal memuat jadwal pribadi (' + response.status + ')');
      }

      const data = await response.json();
      hideError();

      const rutin = data.jadwalRutin || [];
      const ujian = data.jadwalUjian || [];
      const presentasi = data.jadwalPresentasi || [];

      updateCounts(rutin.length, ujian.length, presentasi.length);
      renderRutin(rutin);
      renderUjian(ujian);
      renderPresentasi(presentasi);
    } catch (error) {
      console.error('Error fetching jadwal pribadi:', error);
      showError('Jadwal pribadi gagal dimuat. Muat ulang halaman atau coba beberapa saat lagi.');
      updateCounts(0, 0, 0);
      clear(listRutin);
      clear(listUjian);
      clear(listPresentasi);
      listRutin.appendChild(emptyState('Jadwal mengajar tidak dapat ditampilkan.'));
      listUjian.appendChild(emptyState('Jadwal ujian tidak dapat ditampilkan.'));
      listPresentasi.appendChild(emptyState('Jadwal presentasi tidak dapat ditampilkan.'));
    }
  }

  applyFilter(activeFilter);
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadJadwalPribadi);
  } else {
    loadJadwalPribadi();
  }
})();
