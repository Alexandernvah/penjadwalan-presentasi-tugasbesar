/**
 * Mahasiswa Portal Controller (mahasiswa.js)
 * Handles Input Jadwal Kuliah & Dashboard Mahasiswa
 */
(function () {
  const FALLBACK_SCHEDULES = [
    {
      id: 1,
      namaMataKuliah: 'Rekayasa Perangkat Lunak',
      hari: 'SENIN',
      jamMulai: '08:00',
      jamSelesai: '10:00',
      ruangan: 'A302'
    },
    {
      id: 2,
      namaMataKuliah: 'Basis Data',
      hari: 'RABU',
      jamMulai: '10:00',
      jamSelesai: '12:00',
      ruangan: 'Lab Komputer B.201'
    }
  ];

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
      .replace(/&/g, '&')
      .replace(/</g, '<')
      .replace(/>/g, '>')
      .replace(/"/g, '"')
      .replace(/'/g, "'");
    }

  function formatDayLabel(hari) {
    const dayMap = {
      SENIN: 'Senin',
      SELASA: 'Selasa',
      RABU: 'Rabu',
      KAMIS: 'Kamis',
      JUMAT: 'Jumat',
      SABTU: 'Sabtu'
    };
    return dayMap[String(hari || '').toUpperCase()] || hari || '-';
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

  function renderSchedulesTable(schedules) {
    const tbody = document.getElementById('tbody-jadwal-mahasiswa');
    if (!tbody) return;

    if (!Array.isArray(schedules) || schedules.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6" class="text-center text-muted">Belum ada jadwal kuliah yang dimasukkan.</td>
        </tr>`;
      return;
    }

    tbody.innerHTML = schedules
      .map((item, index) => {
        const id = item.id ?? item.idJadwal ?? index + 1;
        return `
          <tr data-schedule-id="${escapeHtml(id)}">
            <td>${index + 1}</td>
            <td>${escapeHtml(item.namaMataKuliah || item.mataKuliah || '-')}</td>
            <td><span class="day-badge">${escapeHtml(formatDayLabel(item.hari))}</span></td>
            <td>${escapeHtml(item.jamMulai || '')} - ${escapeHtml(item.jamSelesai || '')}</td>
            <td>${escapeHtml(item.ruangan || '-')}</td>
            <td class="text-right">
              <div class="table-action-group">
                <button type="button" class="btn btn-sm btn-delete-row" data-delete-id="${escapeHtml(id)}">Hapus</button>
              </div>
            </td>
          </tr>`;
      })
      .join('');

    tbody.querySelectorAll('[data-delete-id]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        const scheduleId = btn.getAttribute('data-delete-id');
        try {
          if (typeof API !== 'undefined') {
            await API.delete(`/mahasiswa/jadwal-kuliah/${scheduleId}`);
          }
          showAlert('jadwal-form-alert', 'jadwal-form-alert-message', 'Jadwal kuliah berhasil dihapus.', 'success');
          await loadSchedules();
        } catch (err) {
          showAlert(
            'jadwal-form-alert',
            'jadwal-form-alert-message',
            err.message || 'Gagal menghapus jadwal kuliah.',
            'danger'
          );
        }
      });
    });
  }

  async function loadSchedules() {
    const tbody = document.getElementById('tbody-jadwal-mahasiswa');
    if (!tbody) return;

    try {
      if (typeof API !== 'undefined') {
        const data = await API.get('/mahasiswa/jadwal-kuliah');
        if (Array.isArray(data)) {
          renderSchedulesTable(data);
          return;
        }
      }
      renderSchedulesTable(FALLBACK_SCHEDULES);
    } catch (err) {
      renderSchedulesTable(FALLBACK_SCHEDULES);
      showAlert(
        'jadwal-form-alert',
        'jadwal-form-alert-message',
        'Menampilkan data contoh sementara karena endpoint jadwal belum merespons.',
        'warning'
      );
    }
  }

  function initInputJadwalPage() {
    const form = document.getElementById('form-input-jadwal');
    if (!form) return;

    bindAlertClose('btn-close-form-alert', 'jadwal-form-alert');
    loadSchedules();

    const resetBtn = document.getElementById('btn-reset-jadwal');
    if (resetBtn) {
      resetBtn.addEventListener('click', () => {
        const alertEl = document.getElementById('jadwal-form-alert');
        if (alertEl) alertEl.classList.add('hidden');
      });
    }

    form.addEventListener('submit', async (e) => {
      e.preventDefault();

      const namaMataKuliah = document.getElementById('input-mata-kuliah').value.trim();
      const hari = document.getElementById('select-hari-kuliah').value;
      const jamMulai = document.getElementById('input-jam-mulai').value;
      const jamSelesai = document.getElementById('input-jam-selesai').value;
      const ruangan = document.getElementById('input-ruang-kelas').value.trim();

      if (!namaMataKuliah || !hari || !jamMulai || !jamSelesai || !ruangan) {
        showAlert('jadwal-form-alert', 'jadwal-form-alert-message', 'Mohon lengkapi seluruh field wajib.', 'warning');
        return;
      }

      if (jamMulai >= jamSelesai) {
        showAlert(
          'jadwal-form-alert',
          'jadwal-form-alert-message',
          'Jam selesai harus lebih lambat daripada jam mulai.',
          'danger'
        );
        return;
      }

      const payload = { namaMataKuliah, hari, jamMulai, jamSelesai, ruangan };

      try {
        if (typeof API !== 'undefined') {
          await API.post('/mahasiswa/jadwal-kuliah', payload);
        }
        form.reset();
        showAlert(
          'jadwal-form-alert',
          'jadwal-form-alert-message',
          'Jadwal kuliah berhasil ditambahkan ke daftar jadwal Anda.',
          'success'
        );
        await loadSchedules();
      } catch (err) {
        showAlert(
          'jadwal-form-alert',
          'jadwal-form-alert-message',
          err.message || 'Gagal menyimpan jadwal kuliah.',
          'danger'
        );
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