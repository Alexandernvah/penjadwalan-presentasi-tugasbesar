/**
 * Mahasiswa Portal Controller (mahasiswa.js)
 * Handles Input Jadwal Kuliah & Dashboard Mahasiswa
 */
(function () {
  const FALLBACK_DASHBOARD = {
    presentation: {
      statusText: 'Jadwal Presentasi Ditetapkan',
      courseName: 'Rekayasa Perangkat Lunak (IF-43-01)',
      projectTitle: 'Sistem Penjadwalan Presentasi Tugas Besar Terotomasi',
      tanggal: 'Senin, 10 Nov 2026',
      waktu: '09:00 - 10:00 WIB',
      ruangan: 'Lab Komputer (B.201)',
      dosenPenguji: 'Dr. Dosen Pengampu',
      adaBentrok: false
    },
    group: {
      status: 'Terverifikasi',
      namaKelompok: 'Kelompok 01',
      deskripsi: 'Perancangan sistem otomasi berbasis web dan algoritma deteksi tabrakan.',
      anggota: [
        { nama: 'Budi Santoso', npm: '1301210001', role: 'Ketua', isSelf: true },
        { nama: 'Siti Aminah', npm: '1301210002', role: 'Anggota', isSelf: false },
        { nama: 'Andi Wijaya', npm: '1301210003', role: 'Anggota', isSelf: false }
      ]
    }
  };

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function bindAlertClose(btnId, alertId) {
    const btn = document.getElementById(btnId);
    const alertEl = document.getElementById(alertId);
    if (btn && alertEl) {
      btn.addEventListener('click', () => {
        alertEl.classList.add('hidden');
      });
    }
  }

  function showAlert(alertId, messageId, message, variant = 'info') {
    const alertEl = document.getElementById(alertId);
    const msgEl = document.getElementById(messageId);
    if (!alertEl || !msgEl) return;

    alertEl.classList.remove('hidden', 'alert-info', 'alert-success', 'alert-warning', 'alert-danger');
    alertEl.classList.add(`alert-${variant}`);
    msgEl.textContent = message;
  }

  function initUserHeader() {
    if (typeof API === 'undefined') return;

    const profileRaw = localStorage.getItem('user_profile');
    if (profileRaw) {
      try {
        const profile = JSON.parse(profileRaw);
        const nameEl = document.getElementById('current-user-name');
        const roleEl = document.getElementById('current-user-role');
        if (nameEl && (profile.nama || profile.name)) {
          nameEl.textContent = profile.nama || profile.name;
        }
        if (roleEl && (profile.npm || profile.nim)) {
          roleEl.textContent = `NPM: ${profile.npm || profile.nim}`;
        }
      } catch (e) {
        // Abaikan jika JSON profil tidak valid
      }
    }

    const logoutBtn = document.getElementById('btn-mahasiswa-logout');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        API.logout('index.html');
      });
    }
  }

  const ALERT_ID = 'jadwal-form-alert';
  const ALERT_MSG_ID = 'jadwal-form-alert-message';
  // Path absolut ('/api/...') agar tidak diberi prefix /api/v1 oleh helper API
  const API_JADWAL = '/api/mahasiswa/jadwal-kuliah';

  // Katalog mata kuliah semester berjalan (dari database, diisi admin)
  let katalog = { semesterAkademik: null, mataKuliah: [] };

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  }

  function describeSlot(slot) {
    const waktu = `${slot.jamMulai} - ${slot.jamSelesai}`;
    const tipe = slot.tipePertemuan && String(slot.tipePertemuan).toUpperCase() !== 'KULIAH'
      ? ` (${slot.tipePertemuan})`
      : '';
    return { hari: slot.namaHari || '-', waktu, ruangan: slot.ruangan || '-', tipe };
  }

  function fillSlotList(listEl, slots) {
    listEl.replaceChildren();
    if (!Array.isArray(slots) || slots.length === 0) {
      listEl.appendChild(el('li', 'text-muted', 'Jadwal kelas ini belum ditetapkan admin.'));
      return;
    }
    slots.forEach((slot) => {
      const d = describeSlot(slot);
      const li = el('li', 'schedule-slot');
      li.appendChild(el('span', 'day-badge', d.hari));
      li.appendChild(el('span', 'schedule-slot-time', d.waktu));
      li.appendChild(el('span', 'schedule-slot-room', `Ruang ${d.ruangan}${d.tipe}`));
      listEl.appendChild(li);
    });
  }

  function renderSchedulesTable(schedules) {
    const tbody = document.getElementById('tbody-jadwal-mahasiswa');
    if (!tbody) return;
    tbody.replaceChildren();

    if (!Array.isArray(schedules) || schedules.length === 0) {
      const tr = el('tr');
      const td = el('td', 'text-center text-muted', 'Belum ada kelas yang Anda ambil.');
      td.colSpan = 5;
      tr.appendChild(td);
      tbody.appendChild(tr);
      return;
    }

    schedules.forEach((item, index) => {
      const tr = el('tr');
      tr.dataset.scheduleId = String(item.idKelas);

      tr.appendChild(el('td', null, String(index + 1)));

      const tdMatkul = el('td');
      tdMatkul.appendChild(el('div', 'course-name', item.namaMatkul || '-'));
      tdMatkul.appendChild(el('div', 'text-muted course-meta', `${item.idMatkul || ''} · ${item.sks ?? '-'} SKS`));
      tr.appendChild(tdMatkul);

      const tdKelas = el('td');
      tdKelas.appendChild(el('span', 'badge badge-info', `Kelas ${item.kodeKelas || '-'}`));
      if (item.dosenPenanggungJawab) {
        tdKelas.appendChild(el('div', 'text-muted course-meta', item.dosenPenanggungJawab));
      }
      tr.appendChild(tdKelas);

      const tdJadwal = el('td');
      const ul = el('ul', 'schedule-slot-list');
      fillSlotList(ul, item.jadwal);
      tdJadwal.appendChild(ul);
      tr.appendChild(tdJadwal);

      const tdAksi = el('td', 'text-right');
      const group = el('div', 'table-action-group');
      const btn = el('button', 'btn btn-sm btn-delete-row', 'Hapus');
      btn.type = 'button';
      btn.addEventListener('click', () => removeClass(item));
      group.appendChild(btn);
      tdAksi.appendChild(group);
      tr.appendChild(tdAksi);

      tbody.appendChild(tr);
    });
  }

  async function removeClass(item) {
    const label = `${item.namaMatkul} Kelas ${item.kodeKelas}`;
    if (!window.confirm(`Hapus ${label} dari jadwal Anda?`)) return;
    try {
      const res = await API.delete(`${API_JADWAL}/${encodeURIComponent(item.idKelas)}`);
      if (res === null || res === undefined) return; // sesi berakhir, sudah dialihkan
      showAlert(ALERT_ID, ALERT_MSG_ID, res.message || `${label} berhasil dihapus.`, 'success');
      await refreshAll();
    } catch (err) {
      showAlert(ALERT_ID, ALERT_MSG_ID, err.message || 'Gagal menghapus kelas.', 'danger');
    }
  }

  async function loadSchedules() {
    if (!document.getElementById('tbody-jadwal-mahasiswa')) return;
    try {
      const data = await API.get(API_JADWAL);
      if (data === null || data === undefined) return;
      renderSchedulesTable(Array.isArray(data) ? data : []);
    } catch (err) {
      renderSchedulesTable([]);
      showAlert(ALERT_ID, ALERT_MSG_ID, err.message || 'Gagal memuat jadwal kuliah Anda.', 'danger');
    }
  }

  async function loadKatalog() {
    const selMatkul = document.getElementById('select-mata-kuliah');
    const semesterLabel = document.getElementById('semester-label');
    if (!selMatkul) return;

    try {
      const data = await API.get(`${API_JADWAL}/katalog`);
      if (data === null || data === undefined) return;
      katalog = {
        semesterAkademik: data.semesterAkademik || null,
        mataKuliah: Array.isArray(data.mataKuliah) ? data.mataKuliah : []
      };
    } catch (err) {
      katalog = { semesterAkademik: null, mataKuliah: [] };
      showAlert(ALERT_ID, ALERT_MSG_ID, err.message || 'Gagal memuat daftar mata kuliah.', 'danger');
    }

    if (semesterLabel) semesterLabel.textContent = katalog.semesterAkademik || '-';
    populateMatkulSelect();
  }

  function populateMatkulSelect() {
    const selMatkul = document.getElementById('select-mata-kuliah');
    selMatkul.replaceChildren();

    if (katalog.mataKuliah.length === 0) {
      selMatkul.appendChild(new Option('Belum ada mata kuliah terdaftar', ''));
      selMatkul.disabled = true;
      resetKelasSelect('-- Pilih mata kuliah terlebih dahulu --');
      return;
    }

    selMatkul.appendChild(new Option('-- Pilih Mata Kuliah --', ''));
    katalog.mataKuliah.forEach((mk) => {
      const taken = mk.idKelasTerdaftar !== null && mk.idKelasTerdaftar !== undefined;
      const label = `${mk.idMatkul} — ${mk.nama} (${mk.sks} SKS)${taken ? ' ✓ sudah diambil' : ''}`;
      selMatkul.appendChild(new Option(label, String(mk.idMatkulBuka)));
    });
    selMatkul.disabled = false;
    resetKelasSelect('-- Pilih mata kuliah terlebih dahulu --');
  }

  function resetKelasSelect(placeholder) {
    const selKelas = document.getElementById('select-kelas');
    selKelas.replaceChildren(new Option(placeholder, ''));
    selKelas.disabled = true;
    updatePreview();
  }

  function findSelectedMatkul() {
    const id = document.getElementById('select-mata-kuliah').value;
    return katalog.mataKuliah.find((mk) => String(mk.idMatkulBuka) === id) || null;
  }

  function findSelectedKelas() {
    const mk = findSelectedMatkul();
    if (!mk) return null;
    const id = document.getElementById('select-kelas').value;
    return mk.kelas.find((k) => String(k.idKelas) === id) || null;
  }

  function onMatkulChange() {
    const mk = findSelectedMatkul();
    const selKelas = document.getElementById('select-kelas');
    if (!mk) {
      resetKelasSelect('-- Pilih mata kuliah terlebih dahulu --');
      return;
    }
    selKelas.replaceChildren(new Option('-- Pilih Kelas --', ''));
    mk.kelas.forEach((kelas) => {
      const pj = kelas.dosenPenanggungJawab ? ` — ${kelas.dosenPenanggungJawab}` : '';
      const mark = kelas.terdaftar ? ' ✓ sudah diambil' : '';
      selKelas.appendChild(new Option(`Kelas ${kelas.kodeKelas}${pj}${mark}`, String(kelas.idKelas)));
    });
    selKelas.disabled = false;
    // Jika hanya ada satu kelas, langsung pilih agar jadwalnya tampil
    if (mk.kelas.length === 1) selKelas.value = String(mk.kelas[0].idKelas);
    updatePreview();
  }

  function updatePreview() {
    const box = document.getElementById('preview-jadwal-kelas');
    const list = document.getElementById('preview-jadwal-list');
    const hint = document.getElementById('preview-hint');
    const saveBtn = document.getElementById('btn-save-jadwal');
    const mk = findSelectedMatkul();
    const kelas = findSelectedKelas();

    hint.classList.add('hidden');
    hint.textContent = '';

    if (!mk || !kelas) {
      box.classList.add('hidden');
      saveBtn.disabled = true;
      return;
    }

    fillSlotList(list, kelas.jadwal);
    box.classList.remove('hidden');

    const takenId = mk.idKelasTerdaftar;
    const alreadyTaken = takenId !== null && takenId !== undefined;
    if (alreadyTaken) {
      const takenKelas = mk.kelas.find((k) => k.idKelas === takenId);
      const kode = takenKelas ? takenKelas.kodeKelas : '';
      hint.textContent = kelas.terdaftar
        ? 'Kelas ini sudah ada di jadwal Anda.'
        : `Anda sudah mengambil ${mk.nama} di Kelas ${kode}. Hapus kelas tersebut dahulu jika ingin pindah kelas.`;
      hint.classList.remove('hidden');
    }
    saveBtn.disabled = alreadyTaken;
  }

  async function refreshAll() {
    await Promise.all([loadKatalog(), loadSchedules()]);
  }

  function initInputJadwalPage() {
    const form = document.getElementById('form-input-jadwal');
    if (!form || typeof API === 'undefined') return;

    bindAlertClose('btn-close-form-alert', ALERT_ID);
    refreshAll();

    document.getElementById('select-mata-kuliah').addEventListener('change', onMatkulChange);
    document.getElementById('select-kelas').addEventListener('change', updatePreview);

    form.addEventListener('reset', () => {
      const alertEl = document.getElementById(ALERT_ID);
      if (alertEl) alertEl.classList.add('hidden');
      // Tunggu browser mengosongkan field, lalu sinkronkan kelas & pratinjau
      setTimeout(() => resetKelasSelect('-- Pilih mata kuliah terlebih dahulu --'), 0);
    });

    form.addEventListener('submit', async (e) => {
      e.preventDefault();

      const mk = findSelectedMatkul();
      const kelas = findSelectedKelas();
      if (!mk || !kelas) {
        showAlert(ALERT_ID, ALERT_MSG_ID, 'Pilih mata kuliah dan kelas terlebih dahulu.', 'warning');
        return;
      }

      const saveBtn = document.getElementById('btn-save-jadwal');
      saveBtn.disabled = true;
      try {
        const res = await API.post(API_JADWAL, { idKelas: kelas.idKelas });
        if (res === null || res === undefined) return; // sesi berakhir, sudah dialihkan
        // reset dulu: event reset menyembunyikan alert, jadi pesan sukses ditampilkan sesudahnya
        form.reset();
        showAlert(
          ALERT_ID,
          ALERT_MSG_ID,
          res.message || `${mk.nama} Kelas ${kelas.kodeKelas} berhasil ditambahkan.`,
          'success'
        );
        await refreshAll();
      } catch (err) {
        showAlert(ALERT_ID, ALERT_MSG_ID, err.message || 'Gagal menyimpan kelas.', 'danger');
        updatePreview();
      }
    });
  }

  function renderDashboardData(data) {
    const pres = data.presentation || {};
    const grp = data.group || {};

    const setText = (id, text) => {
      const el = document.getElementById(id);
      if (el && text !== undefined && text !== null) {
        el.textContent = text;
      }
    };

    setText('hero-status-text', pres.statusText || 'Jadwal Presentasi Ditetapkan');
    setText('hero-course-name', pres.courseName || '-');
    setText('hero-project-title', pres.projectTitle ? `Judul: ${pres.projectTitle}` : '');
    setText('hero-presentation-date', pres.tanggal || '-');
    setText('hero-presentation-time', pres.waktu || '-');
    setText('hero-presentation-room', pres.ruangan || '-');
    setText('hero-presentation-lecturer', pres.dosenPenguji || '-');

    const heroCard = document.getElementById('presentation-schedule-card');
    if (heroCard && pres.adaBentrok) {
      let badge = heroCard.querySelector('.schedule-conflict-badge');
      if (!badge) {
        badge = document.createElement('div');
        badge.className = 'schedule-conflict-badge';
        badge.textContent = '⚠️ Jadwal memiliki catatan override bentrok oleh dosen';
        heroCard.appendChild(badge);
      }
    }

    setText('badge-kelompok-status', grp.status || 'Terverifikasi');
    setText('group-name-display', grp.namaKelompok || '-');
    setText('group-desc-display', grp.deskripsi || '-');

    const membersContainer = document.getElementById('group-members-container');
    if (membersContainer && Array.isArray(grp.anggota) && grp.anggota.length > 0) {
      membersContainer.innerHTML = grp.anggota
        .map((m) => {
          const initial = (m.nama || '?').trim().charAt(0).toUpperCase();
          const npmLabel = m.isSelf ? `NPM: ${m.npm} (Anda)` : `NPM: ${m.npm}`;
          const badgeClass = m.role === 'Ketua' ? 'badge-info' : 'badge-neutral';
          return `
            <div class="member-item">
              <div class="member-info">
                <div class="member-avatar">${escapeHtml(initial)}</div>
                <div>
                  <div class="member-name">${escapeHtml(m.nama)}</div>
                  <div class="member-npm">${escapeHtml(npmLabel)}</div>
                </div>
              </div>
              <span class="badge ${badgeClass}">${escapeHtml(m.role || 'Anggota')}</span>
            </div>`;
        })
        .join('');
    }
  }

  async function initDashboardPage() {
    const heroCard = document.getElementById('presentation-schedule-card');
    if (!heroCard) return;

    bindAlertClose('btn-close-student-alert', 'student-alert-container');

    try {
      if (typeof API !== 'undefined') {
        const data = await API.get('/mahasiswa/dashboard');
        if (data && (data.presentation || data.group)) {
          renderDashboardData(data);
          return;
        }
      }
      renderDashboardData(FALLBACK_DASHBOARD);
    } catch (err) {
      renderDashboardData(FALLBACK_DASHBOARD);
      showAlert(
        'student-alert-container',
        'student-alert-message',
        'Menampilkan data pratinjau dashboard karena API belum tersedia.',
        'info'
      );
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    initUserHeader();
    initInputJadwalPage();
    initDashboardPage();
  });
})();