// dosen-dashboard.js - Lecturer Dashboard: quick actions and scheduled presentation overview
(function () {
  'use strict';

  // ---------------------------------------------------------------------
  // Profile dropdown toggle (consistent across all lecturer pages)
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
  const topbarName = document.getElementById('topbar-lecturer-name');
  const welcomeName = document.getElementById('welcome-lecturer-name');
  const dropdownName = document.getElementById('dropdown-lecturer-name');
  const dropdownEmail = document.getElementById('dropdown-lecturer-email');
  const semesterLabel = document.getElementById('dashboardSemesterLabel');

  const countPill = document.getElementById('dashboard-presentasi-count');
  const listPresentasi = document.getElementById('list-dashboard-presentasi');

  const modalPresentasi = document.getElementById('modal-detail-presentasi-db');
  const btnCloseModal = document.getElementById('db-btn-close-modal-presentasi');
  const btnTutupModal = document.getElementById('db-btn-tutup-modal-presentasi');
  const detailKelompok = document.getElementById('db-detail-kelompok');
  const detailMataKuliah = document.getElementById('db-detail-mata-kuliah');
  const detailTopik = document.getElementById('db-detail-topik');
  const detailTanggal = document.getElementById('db-detail-tanggal');
  const detailWaktu = document.getElementById('db-detail-waktu');
  const detailRuangan = document.getElementById('db-detail-ruangan');
  const detailAnggotaCount = document.getElementById('db-detail-anggota-count');
  const detailAnggota = document.getElementById('db-detail-anggota');

  // ---------------------------------------------------------------------
  // State & Constants
  // ---------------------------------------------------------------------
  let lastFocusedElement = null;

  const BULAN_SINGKAT = ['JAN', 'FEB', 'MAR', 'APR', 'MEI', 'JUN', 'JUL', 'AGU', 'SEP', 'OKT', 'NOV', 'DES'];
  const NAMA_HARI = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];

  const ICONS = {
    clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>',
    mapPin: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>',
    eye: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>',
    calendar: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>',
    arrowRight: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>'
  };

  // ---------------------------------------------------------------------
  // DOM & formatting helpers (all dynamic text goes through textContent)
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

  function clear(node) {
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

  function timeRange(mulai, selesai) {
    return mulai + ' - ' + selesai + ' WIB';
  }

  // ---------------------------------------------------------------------
  // Lecturer identity
  // ---------------------------------------------------------------------
  function applyLecturerIdentity(summary) {
    const lecturerName = (summary && summary.namaDosen) ? summary.namaDosen : 'Dr. Budi Santoso';
    const lecturerEmail = (summary && summary.email) ? summary.email : 'budi.santoso@unpar.ac.id';

    if (topbarName) topbarName.textContent = lecturerName;
    if (welcomeName) welcomeName.textContent = lecturerName;
    if (dropdownName) dropdownName.textContent = lecturerName;
    if (dropdownEmail) dropdownEmail.textContent = lecturerEmail;
    if (semesterLabel && summary && summary.semesterAktif) {
      semesterLabel.textContent = summary.semesterAktif;
    }
  }

  // ---------------------------------------------------------------------
  // Scheduled Presentations Rendering (ONLY Jadwal Presentasi)
  // ---------------------------------------------------------------------
  function buildDateBlock(iso) {
    const p = dateParts(iso);
    const block = el('div', 'db-date-block');
    block.appendChild(el('span', 'db-date-month', p.month));
    block.appendChild(el('span', 'db-date-day', p.day));
    block.appendChild(el('span', 'db-date-year', p.year));
    return block;
  }

  function buildInlineMetaItem(iconName, text) {
    const item = el('span', 'db-meta-inline-item');
    item.appendChild(icon(iconName));
    item.appendChild(el('span', null, text));
    return item;
  }

  function renderEmptyState() {
    const box = el('div', 'db-empty-card');

    const iconChip = el('div', 'db-empty-icon');
    iconChip.setAttribute('aria-hidden', 'true');
    iconChip.appendChild(icon('calendar'));
    box.appendChild(iconChip);

    box.appendChild(el('p', 'db-empty-message', 'Belum ada jadwal presentasi yang ditetapkan.'));

    const cta = el('a', 'db-btn-primary-link');
    cta.href = 'penjadwalan.html';
    cta.appendChild(el('span', null, 'Atur Jadwal Presentasi'));
    cta.appendChild(icon('arrowRight'));
    box.appendChild(cta);

    listPresentasi.appendChild(box);
  }

  function renderPresentasiList(items) {
    if (!listPresentasi) return;
    clear(listPresentasi);

    if (countPill) {
      countPill.textContent = items.length + ' Terjadwal';
    }

    if (!items.length) {
      renderEmptyState();
      return;
    }

    items.forEach(function (item) {
      const card = el('article', 'db-event-card');
      card.appendChild(buildDateBlock(item.tanggal));

      const content = el('div', 'db-event-content');
      const badges = el('div', 'db-event-badges');
      badges.appendChild(el('span', 'db-badge-green', 'Presentasi Tugas Besar'));
      badges.appendChild(el('span', 'db-pill-code-sm', item.kodeMatkul || '-'));
      content.appendChild(badges);

      const titleText = (item.namaMatkul || '-') + ' \u2014 ' + (item.namaKelompok || '-');
      content.appendChild(el('h3', 'db-card-title', titleText));

      const meta = el('div', 'db-meta-inline');
      meta.appendChild(buildInlineMetaItem('clock', timeRange(item.waktuMulai, item.waktuSelesai)));
      const dot = el('span', 'db-meta-dot');
      dot.setAttribute('aria-hidden', 'true');
      meta.appendChild(dot);
      meta.appendChild(buildInlineMetaItem('mapPin', item.ruangan || '-'));
      content.appendChild(meta);

      card.appendChild(content);

      const actionZone = el('div', 'db-event-action');
      const btn = el('button', 'db-btn-detail');
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
  // Detail Presentasi Modal
  // ---------------------------------------------------------------------
  function openPresentasiModal(item) {
    if (!modalPresentasi) return;
    const p = dateParts(item.tanggal);

    lastFocusedElement = document.activeElement;
    if (detailKelompok) detailKelompok.textContent = item.namaKelompok || '-';
    detailMataKuliah.textContent = (item.kodeMatkul || '-') + ' - ' + (item.namaMatkul || '-');
    detailTopik.textContent = item.topik || 'Belum ada topik tugas besar';
    if (detailTanggal) detailTanggal.textContent = p.hari + ', ' + p.day + ' ' + p.bulanTitle + ' ' + p.year;
    detailWaktu.textContent = (item.waktuMulai || '-') + ' \u2013 ' + (item.waktuSelesai || '-') + ' WIB';
    detailRuangan.textContent = item.ruangan || '-';

    clear(detailAnggota);
    const anggota = item.anggotaKelompok || [];
    if (detailAnggotaCount) {
      detailAnggotaCount.textContent = anggota.length + ' Anggota';
    }

    if (!anggota.length) {
      detailAnggota.appendChild(el('div', 'jp-pres-members-empty', 'Belum ada anggota pada kelompok ini.'));
    } else {
      anggota.forEach(function (member) {
        const row = el('div', 'jp-pres-member-row');
        row.appendChild(el('span', 'jp-pres-member-name', member.nama || '-'));
        row.appendChild(el('span', 'jp-pres-npm-badge', member.npm || '-'));
        detailAnggota.appendChild(row);
      });
    }

    modalPresentasi.classList.add('active');
    if (btnTutupModal) btnTutupModal.focus();
  }

  function closePresentasiModal() {
    if (!modalPresentasi || !modalPresentasi.classList.contains('active')) return;
    modalPresentasi.classList.remove('active');
    if (lastFocusedElement && typeof lastFocusedElement.focus === 'function') {
      lastFocusedElement.focus();
    }
    lastFocusedElement = null;
  }

  [btnCloseModal, btnTutupModal].forEach(function (btn) {
    if (btn) btn.addEventListener('click', closePresentasiModal);
  });

  if (modalPresentasi) {
    modalPresentasi.addEventListener('click', function (e) {
      if (e.target === modalPresentasi) closePresentasiModal();
    });
  }

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closePresentasiModal();
  });

  // ---------------------------------------------------------------------
  // Data Loading
  // ---------------------------------------------------------------------
  async function fetchJson(url) {
    const response = await fetch(url, { headers: { 'Accept': 'application/json' } });
    if (!response.ok) {
      throw new Error('Request gagal (' + response.status + ')');
    }
    return response.json();
  }

  async function loadDashboardData() {
    const authNidn = localStorage.getItem('auth_id');
    const queryParam = authNidn ? '?dosenId=' + encodeURIComponent(authNidn) : '';

    const results = await Promise.allSettled([
      fetchJson('/api/dosen/dashboard/summary' + queryParam),
      fetchJson('/api/dosen/jadwal-pribadi' + queryParam)
    ]);

    if (results[0].status === 'fulfilled') {
      applyLecturerIdentity(results[0].value);
    } else {
      console.error('Error fetching dashboard summary:', results[0].reason);
      applyLecturerIdentity(null);
    }

    if (results[1].status === 'fulfilled') {
      const payload = results[1].value || {};
      if (semesterLabel && payload.semesterAktif && results[0].status !== 'fulfilled') {
        semesterLabel.textContent = payload.semesterAktif;
      }
      renderPresentasiList(payload.jadwalPresentasi || []);
    } else {
      console.error('Error fetching jadwal presentasi:', results[1].reason);
      renderPresentasiList([]);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadDashboardData);
  } else {
    loadDashboardData();
  }
})();
