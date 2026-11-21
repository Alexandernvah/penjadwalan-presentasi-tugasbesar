// dosen-penjadwalan.js - 5-Screen Presentation Scheduling & Weekly Combined Matrix Controller
(function () {
  'use strict';

  // Profile Dropdown Toggle (Consistent with dosen-kelola-kelompok.js)
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

  // Screen 1: Main Page DOM Elements
  const selectKelas = document.getElementById('select-kelas');
  const selectKelompok = document.getElementById('select-kelompok');
  const feedbackBanner = document.getElementById('penjadwalan-feedback-banner');
  const feedbackText = document.getElementById('penjadwalan-feedback-text');
  const btnCloseFeedback = document.getElementById('btn-close-feedback-banner');

  const titleInformasiKelompok = document.getElementById('title-informasi-kelompok');
  const infoMataKuliah = document.getElementById('info-mata-kuliah');
  const infoTopikKelompok = document.getElementById('info-topik-kelompok');
  const listAnggotaKelompok = document.getElementById('list-anggota-kelompok');
  const badgeDurasiKelompok = document.getElementById('badge-durasi-kelompok');
  const bannerJadwalAktif = document.getElementById('banner-jadwal-aktif');
  const teksJadwalAktif = document.getElementById('teks-jadwal-aktif');
  const btnBatalkanJadwalAktif = document.getElementById('btn-batalkan-jadwal-aktif');
  const btnLihatVisualisasi = document.getElementById('btn-lihat-visualisasi');

  // Screen 2 (Modal 1): Weekly Combined Matrix Elements
  const modalVisualisasi = document.getElementById('modal-visualisasi-jadwal');
  const btnCloseModalVisualisasi = document.getElementById('btn-close-modal-visualisasi');
  const btnTutupModalVisualisasi = document.getElementById('btn-tutup-modal-visualisasi');
  const modeTabs = document.querySelectorAll('.jadwal-mode-tab');
  const btnPekanSebelumnya = document.getElementById('btn-pekan-sebelumnya');
  const btnPekanSelanjutnya = document.getElementById('btn-pekan-selanjutnya');
  const labelRentangPekan = document.getElementById('label-rentang-pekan');
  const theadRowHari = document.getElementById('thead-row-hari');
  const tbodyMatriksMingguan = document.getElementById('tbody-matriks-mingguan');

  // Screen 3 (Modal 2A): Safe Slot Confirmation Elements
  const modalKonfirmasiSlot = document.getElementById('modal-konfirmasi-slot');
  const btnCloseModalSlotAman = document.getElementById('btn-close-modal-slot-aman');
  const teksKonfirmasiSlotAman = document.getElementById('teks-konfirmasi-slot-aman');
  const labelSubslotAman = document.getElementById('label-subslot-aman');
  const selectSubslotAman = document.getElementById('select-subslot-aman');
  const btnBatalSlotAman = document.getElementById('btn-batal-slot-aman');
  const btnSimpanSlotAman = document.getElementById('btn-simpan-slot-aman');

  // Screen 4 (Modal 2B): Partial Conflict Warning & Override Elements
  const modalPeringatanBentrok = document.getElementById('modal-peringatan-bentrok');
  const btnCloseModalBentrok = document.getElementById('btn-close-modal-bentrok');
  const teksPeringatanBentrok = document.getElementById('teks-peringatan-bentrok');
  const labelSubslotBentrok = document.getElementById('label-subslot-bentrok');
  const selectSubslotBentrok = document.getElementById('select-subslot-bentrok');
  const btnCariWaktuLain = document.getElementById('btn-cari-waktu-lain');
  const btnTetapSimpanBentrok = document.getElementById('btn-tetap-simpan-bentrok');

  // Cancel Schedule Confirmation Modal
  const modalKonfirmasiBatal = document.getElementById('modal-konfirmasi-batal');
  const batalGroupNameEl = document.getElementById('batal-group-name');
  const btnCancelBatal = document.getElementById('btn-cancel-batal');
  const btnConfirmBatal = document.getElementById('btn-confirm-batal');

  // State Management
  let currentClasses = [];
  let currentKelompokList = [];
  let activeMode = 'REGULER';
  let activeTanggalMulai = null;
  let currentWeeklyMatrix = null;
  let selectedCellContext = null;
  let presentasiToCancel = null;
  let activeMatrixAbortController = null;
  let latestMatrixRequestId = 0;

  function showFeedback(message, isError) {
    if (!feedbackBanner || !feedbackText) return;
    feedbackText.textContent = message;
    feedbackBanner.classList.remove('hidden', 'banner-success', 'banner-danger');
    feedbackBanner.classList.add(isError ? 'banner-danger' : 'banner-success');
  }

  function hideNotificationBanner() {
    if (feedbackBanner) {
      feedbackBanner.classList.add('hidden');
    }
  }

  if (btnCloseFeedback) {
    btnCloseFeedback.addEventListener('click', hideNotificationBanner);
  }

  function getSelectedClassObj() {
    const idKelas = selectKelas && selectKelas.value ? parseInt(selectKelas.value, 10) : null;
    return currentClasses.find(function (c) { return c.idKelas === idKelas; }) || currentClasses[0] || {
      idKelas: 102,
      idMatkul: 'IF3130',
      namaMatkul: 'Jaringan Komputer',
      kodeKelas: 'B',
      ruanganDefault: 'Ruang 9122'
    };
  }

  function getSelectedGroupObj() {
    const idKel = selectKelompok && selectKelompok.value ? parseInt(selectKelompok.value, 10) : null;
    return currentKelompokList.find(function (g) { return g.idKelompok === idKel; }) || currentKelompokList[0] || null;
  }

  function formatCourseDisplay(kelas) {
    if (!kelas) return 'IF3130 - Jaringan Komputer (Kelas B)';
    return `${kelas.idMatkul || 'IF3130'} - ${kelas.namaMatkul || 'Jaringan Komputer'} (Kelas ${kelas.kodeKelas || 'B'})`;
  }

  function parseMinutes(timeStr) {
    if (!timeStr) return 480;
    const parts = timeStr.split(':');
    return parseInt(parts[0], 10) * 60 + parseInt(parts[1] || '0', 10);
  }

  function formatMinutes(totalMins) {
    const h = String(Math.floor(totalMins / 60) % 24).padStart(2, '0');
    const m = String(totalMins % 60).padStart(2, '0');
    return `${h}:${m}`;
  }

  function generateSubSlots(jamMulaiBlock, jamSelesaiBlock, durasiMenit, intervalTerpakai) {
    const durasi = durasiMenit && durasiMenit > 0 ? durasiMenit : 30;
    const startMins = parseMinutes(jamMulaiBlock);
    const endMins = parseMinutes(jamSelesaiBlock);
    const subSlots = [];

    const bookedRanges = [];
    if (Array.isArray(intervalTerpakai)) {
      intervalTerpakai.forEach(function (rangeStr) {
        if (!rangeStr) return;
        const parts = rangeStr.split('-').map(function (s) { return s.trim(); });
        if (parts.length === 2) {
          bookedRanges.push({
            start: parseMinutes(parts[0]),
            end: parseMinutes(parts[1])
          });
        }
      });
    }

    let cursor = startMins;
    while (cursor + durasi <= endMins) {
      const subStartMins = cursor;
      const subEndMins = cursor + durasi;

      const isBooked = bookedRanges.some(function (b) {
        return subStartMins < b.end && b.start < subEndMins;
      });

      if (!isBooked) {
        const subStart = formatMinutes(subStartMins);
        const subEnd = formatMinutes(subEndMins);
        subSlots.push({
          jamMulai: subStart,
          jamSelesai: subEnd,
          label: `${subStart} - ${subEnd}`
        });
      }

      cursor += durasi;
    }

    return subSlots;
  }

  // =========================================================================
  // Screen 1: Load Classes, Groups & Render Centered Group Information Card
  // =========================================================================

  async function initializePage() {
    await loadKelasList();
    const selectedClass = getSelectedClassObj();
    await loadKelompokList(selectedClass.idKelas);
  }

  async function loadKelasList() {
    try {
      const authNidn = localStorage.getItem('auth_id');
      const query = authNidn ? `?dosenId=${encodeURIComponent(authNidn)}` : '';
      const response = await fetch(`/api/dosen/penjadwalan/kelas${query}`, {
        headers: { 'Accept': 'application/json' }
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const list = await response.json();
      currentClasses = Array.isArray(list) && list.length > 0 ? list : getFallbackClasses();
    } catch (err) {
      console.warn('Menggunakan daftar kelas fallback:', err);
      currentClasses = getFallbackClasses();
    }

    if (currentClasses.length > 0) {
      const first = currentClasses[0];
      const topbarName = document.getElementById('topbar-lecturer-name');
      const dropdownName = document.getElementById('dropdown-lecturer-name');
      const dropdownEmail = document.getElementById('dropdown-lecturer-email');
      if (topbarName && first.namaDosen) topbarName.textContent = first.namaDosen;
      if (dropdownName && first.namaDosen) dropdownName.textContent = first.namaDosen;
      if (dropdownEmail && first.emailDosen) dropdownEmail.textContent = first.emailDosen;
    }

    if (selectKelas) {
      selectKelas.innerHTML = '';
      currentClasses.forEach(function (c) {
        const opt = document.createElement('option');
        opt.value = c.idKelas;
        opt.textContent = formatCourseDisplay(c);
        selectKelas.appendChild(opt);
      });
    }
  }

  async function loadKelompokList(idKelas, preserveKelompokId) {
    try {
      const response = await fetch(`/api/dosen/penjadwalan/kelas/${encodeURIComponent(idKelas)}/kelompok`, {
        headers: { 'Accept': 'application/json' }
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      currentKelompokList = Array.isArray(data) && data.length > 0 ? data : getFallbackKelompok(idKelas);
    } catch (err) {
      console.warn('Menggunakan daftar kelompok fallback:', err);
      currentKelompokList = getFallbackKelompok(idKelas);
    }

    if (selectKelompok) {
      selectKelompok.innerHTML = '';
      currentKelompokList.forEach(function (g) {
        const noKel = g.noKelompok || g.nomorKelompok || 1;
        const opt = document.createElement('option');
        opt.value = g.idKelompok;
        opt.textContent = `Kelompok ${noKel}`;
        selectKelompok.appendChild(opt);
      });

      if (preserveKelompokId && currentKelompokList.some(function (g) { return g.idKelompok === preserveKelompokId; })) {
        selectKelompok.value = String(preserveKelompokId);
      }
    }

    renderGroupInfoCard();
  }

  function renderGroupInfoCard() {
    const kelas = getSelectedClassObj();
    const group = getSelectedGroupObj();

    if (infoMataKuliah) {
      infoMataKuliah.textContent = formatCourseDisplay(kelas);
    }

    if (!group) {
      if (titleInformasiKelompok) titleInformasiKelompok.textContent = 'Informasi Kelompok';
      if (infoTopikKelompok) {
        infoTopikKelompok.textContent = 'Belum ada topik tugas besar';
        infoTopikKelompok.classList.add('is-muted');
      }
      if (listAnggotaKelompok) {
        listAnggotaKelompok.innerHTML = '<div class="dosen-members-empty">Belum ada kelompok terdaftar pada kelas ini.</div>';
      }
      if (badgeDurasiKelompok) badgeDurasiKelompok.textContent = '30 Menit';
      if (bannerJadwalAktif) bannerJadwalAktif.classList.add('hidden');
      return;
    }

    const noKel = group.noKelompok || group.nomorKelompok || 1;
    if (titleInformasiKelompok) {
      titleInformasiKelompok.textContent = `Informasi Kelompok ${noKel}`;
    }

    const rawTopik = group.topik ? group.topik.trim() : '';
    const isBlankTopik = !rawTopik
      || rawTopik.toLowerCase() === 'belum ada topik tugas besar'
      || rawTopik.toLowerCase() === 'belum ditentukan'
      || /^Kelompok\s+\d+$/i.test(rawTopik);

    if (infoTopikKelompok) {
      infoTopikKelompok.textContent = isBlankTopik ? 'Belum ada topik tugas besar' : rawTopik;
      if (isBlankTopik) {
        infoTopikKelompok.classList.add('is-muted');
      } else {
        infoTopikKelompok.classList.remove('is-muted');
      }
    }

    const durasi = group.durasiMenit || 30;
    if (badgeDurasiKelompok) {
      badgeDurasiKelompok.textContent = `${durasi} Menit`;
    }

    if (listAnggotaKelompok) {
      const anggota = Array.isArray(group.anggota) ? group.anggota : [];
      if (anggota.length === 0) {
        listAnggotaKelompok.innerHTML = '<div class="dosen-members-empty">Belum ada anggota terdaftar pada kelompok ini.</div>';
      } else {
        listAnggotaKelompok.innerHTML = anggota.map(function (m, idx) {
          const isLeader = typeof m.isKetua === 'boolean' ? m.isKetua : idx === 0;
          const ketuaBadgeHtml = isLeader ? '<span class="dosen-badge-ketua">Ketua</span>' : '';
          return `
            <div class="dosen-group-member-row">
              <div class="dosen-group-member-left">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <span class="dosen-group-member-name">${m.nama || '-'}</span>
                <span class="dosen-group-member-npm">(${m.npm || '-'})</span>
              </div>
              ${ketuaBadgeHtml}
            </div>
          `;
        }).join('');
      }
    }

    if (bannerJadwalAktif && teksJadwalAktif) {
      if (group.terjadwal && group.idPresentasi) {
        const hariStr = group.hariNama ? `${group.hariNama}, ` : '';
        const ruanganStr = group.ruangan || kelas.ruanganDefault || 'Ruang 9122';
        teksJadwalAktif.textContent = `${hariStr}${group.tanggal || ''} (${group.jamMulai || ''} - ${group.jamSelesai || ''} WIB) di ${ruanganStr}`;
        bannerJadwalAktif.classList.remove('hidden');
      } else {
        bannerJadwalAktif.classList.add('hidden');
      }
    }
  }

  if (selectKelas) {
    selectKelas.addEventListener('change', function () {
      hideNotificationBanner();
      const idKelas = parseInt(this.value, 10);
      loadKelompokList(idKelas);
    });
  }

  if (selectKelompok) {
    selectKelompok.addEventListener('change', function () {
      hideNotificationBanner();
      renderGroupInfoCard();
    });
  }

  if (btnBatalkanJadwalAktif) {
    btnBatalkanJadwalAktif.addEventListener('click', function () {
      const group = getSelectedGroupObj();
      if (!group || !group.idPresentasi) return;
      openModalBatal(group.idPresentasi, group.noKelompok || group.nomorKelompok || 1);
    });
  }

  // =========================================================================
  // Screen 2 (Modal 1): Weekly Combined Schedule Matrix
  // =========================================================================

  function openModalVisualisasi() {
    if (!modalVisualisasi) return;
    hideNotificationBanner();
    modalVisualisasi.classList.add('active');
    loadWeeklyMatrixData();
  }

  function closeModalVisualisasi() {
    if (!modalVisualisasi) return;
    modalVisualisasi.classList.remove('active');
  }

  if (btnLihatVisualisasi) {
    btnLihatVisualisasi.addEventListener('click', openModalVisualisasi);
  }
  if (btnCloseModalVisualisasi) {
    btnCloseModalVisualisasi.addEventListener('click', closeModalVisualisasi);
  }
  if (btnTutupModalVisualisasi) {
    btnTutupModalVisualisasi.addEventListener('click', closeModalVisualisasi);
  }

  modeTabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      const targetMode = this.getAttribute('data-mode') || 'REGULER';
      activeMode = targetMode;
      activeTanggalMulai = null; // Reset to default week for that period mode
      modeTabs.forEach(function (t) {
        const isCurrent = t === tab;
        t.classList.toggle('active', isCurrent);
        t.setAttribute('aria-selected', String(isCurrent));
      });
      loadWeeklyMatrixData();
    });
  });

  if (btnPekanSebelumnya) {
    btnPekanSebelumnya.addEventListener('click', function () {
      if (currentWeeklyMatrix && currentWeeklyMatrix.prevTanggalMulai) {
        activeTanggalMulai = currentWeeklyMatrix.prevTanggalMulai;
        loadWeeklyMatrixData();
      }
    });
  }

  if (btnPekanSelanjutnya) {
    btnPekanSelanjutnya.addEventListener('click', function () {
      if (currentWeeklyMatrix && currentWeeklyMatrix.nextTanggalMulai) {
        activeTanggalMulai = currentWeeklyMatrix.nextTanggalMulai;
        loadWeeklyMatrixData();
      }
    });
  }

  async function loadWeeklyMatrixData() {
    if (!tbodyMatriksMingguan) return;

    if (activeMatrixAbortController) {
      activeMatrixAbortController.abort();
    }
    activeMatrixAbortController = new AbortController();
    const currentAbortController = activeMatrixAbortController;
    const currentRequestId = ++latestMatrixRequestId;

    const tableEl = document.querySelector('.jadwal-weekly-table');
    const hasExistingRows = tbodyMatriksMingguan.children.length > 0 &&
      !tbodyMatriksMingguan.querySelector('.dosen-course-empty');

    if (tableEl) {
      tableEl.classList.add('is-loading');
    }
    if (!hasExistingRows) {
      tbodyMatriksMingguan.innerHTML = '<tr><td colspan="6" class="dosen-course-empty">Memuat matriks jadwal gabungan...</td></tr>';
    }

    const kelas = getSelectedClassObj();
    const group = getSelectedGroupObj();
    const idKelas = kelas ? kelas.idKelas : 102;
    const idKelompok = group ? group.idKelompok : 1;

    try {
      const params = new URLSearchParams({
        idKelas: String(idKelas),
        idKelompok: String(idKelompok),
        mode: activeMode,
        durasiMenit: String((group && group.durasiMenit) ? group.durasiMenit : 30)
      });
      if (activeTanggalMulai) {
        params.set('tanggalMulai', activeTanggalMulai);
      }

      const response = await fetch(`/api/dosen/penjadwalan/matriks-mingguan?${params.toString()}`, {
        headers: { 'Accept': 'application/json' },
        signal: currentAbortController.signal
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();

      if (currentRequestId === latestMatrixRequestId) {
        currentWeeklyMatrix = data;
        renderWeeklyMatrixTable(currentWeeklyMatrix);
      }
    } catch (err) {
      if (err.name === 'AbortError') {
        return;
      }
      console.warn('Gagal mengambil matriks mingguan dari API:', err);
      if (currentRequestId === latestMatrixRequestId) {
        if (!hasExistingRows) {
          tbodyMatriksMingguan.innerHTML = '<tr><td colspan="6" class="dosen-course-empty">Gagal memuat matriks jadwal gabungan.</td></tr>';
        }
      }
    } finally {
      if (currentRequestId === latestMatrixRequestId) {
        if (tableEl) {
          tableEl.classList.remove('is-loading');
        }
      }
    }
  }

  function renderWeeklyMatrixTable(matrix) {
    if (!matrix || !tbodyMatriksMingguan) return;

    if (labelRentangPekan && matrix.rentangTanggalLabel) {
      labelRentangPekan.textContent = matrix.rentangTanggalLabel;
    }

    if (theadRowHari && Array.isArray(matrix.hariList)) {
      const dayHeadersHtml = matrix.hariList.map(function (h) {
        return `<th scope="col"><span class="jadwal-th-day">${h.namaHari}</span><span class="jadwal-th-date">${h.labelSingkat}</span></th>`;
      }).join('');
      theadRowHari.innerHTML = `<th scope="col">Waktu</th>${dayHeadersHtml}`;
    }

    tbodyMatriksMingguan.innerHTML = '';
    const barisList = Array.isArray(matrix.barisList) ? matrix.barisList : [];

    barisList.forEach(function (baris) {
      const tr = document.createElement('tr');

      const tdTime = document.createElement('td');
      tdTime.className = 'jadwal-td-time';
      tdTime.textContent = baris.labelWaktu;
      tr.appendChild(tdTime);

      const selList = Array.isArray(baris.selList) ? baris.selList : [];
      selList.forEach(function (sel) {
        if (sel.skipRender) {
          return;
        }

        const td = document.createElement('td');
        if (sel.rowSpan && sel.rowSpan > 1) {
          td.setAttribute('rowspan', String(sel.rowSpan));
        }

        const status = sel.status || 'KOSONG';

        if (status === 'TERJADWAL') {
          td.className = 'cell-state-terjadwal';
          td.setAttribute('role', 'button');
          td.setAttribute('tabindex', '0');
          td.appendChild(createStackedCellBox(
            'pill-indigo',
            sel.badgeText || 'Terjadwal',
            sel.deskripsi || 'Jadwal Presentasi',
            'cell-subtext-terjadwal',
            ['cell-terjadwal-group', 'cell-terjadwal-course', 'cell-terjadwal-time']
          ));
          bindCellClick(td, function () {
            if (sel.idPresentasiTerjadwal) {
              const group = getSelectedGroupObj();
              const noKel = (group && (group.noKelompok || group.nomorKelompok)) || (currentWeeklyMatrix && currentWeeklyMatrix.noKelompok) || 1;
              openModalBatal(sel.idPresentasiTerjadwal, noKel);
            }
          });
        } else if (status === 'TERSEDIA_SEBAGIAN' || status === 'PARTIAL_FREE') {
          td.className = 'cell-state-tersedia-sebagian';
          td.setAttribute('role', 'button');
          td.setAttribute('tabindex', '0');
          td.appendChild(createPartialFreeCellBox(
            sel.badgeText || 'Kosong',
            sel.deskripsi || ''
          ));
          bindCellClick(td, function () {
            openModalSlotAman(sel);
          });
        } else if (status === 'KOSONG') {
          td.className = 'cell-state-kosong';
          td.setAttribute('role', 'button');
          td.setAttribute('tabindex', '0');
          td.innerHTML = `
            <div class="jadwal-cell-box">
              <span class="cell-kosong-label">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
                <span>Kosong</span>
              </span>
            </div>
          `;
          bindCellClick(td, function () {
            openModalSlotAman(sel);
          });
        } else if (status === 'PRIORITAS_KELAS') {
          td.className = 'cell-state-prioritas';
          td.setAttribute('role', 'button');
          td.setAttribute('tabindex', '0');
          td.innerHTML = `
            <div class="jadwal-cell-box">
              <span class="cell-pill-badge pill-blue">${sel.badgeText || 'Jam Reguler'}</span>
              <span class="cell-subtext-prioritas">${sel.deskripsi || 'Aman, Semua Bisa Hadir'}</span>
            </div>
          `;
          bindCellClick(td, function () {
            openModalSlotAman(sel);
          });
        } else if (status === 'BENTROK_MAHASISWA') {
          td.className = 'cell-state-bentrok-mhs';
          td.setAttribute('role', 'button');
          td.setAttribute('tabindex', '0');
          td.innerHTML = `
            <div class="jadwal-cell-box">
              <span class="cell-pill-badge pill-red">${sel.badgeText || 'Mahasiswa Sibuk'}</span>
              <span class="cell-subtext-bentrok-mhs">${sel.deskripsi || 'Bentrok Jadwal Mahasiswa'}</span>
            </div>
          `;
          bindCellClick(td, function () {
            openModalPeringatanBentrok(sel);
          });
        } else {
          // State D: BENTROK_DOSEN
          td.className = 'cell-state-bentrok-dosen';
          td.appendChild(createStackedCellBox(
            'pill-amber',
            sel.badgeText || 'Dosen Sibuk',
            sel.deskripsi || 'Mengajar Kelas Lain',
            'cell-subtext-bentrok-dosen',
            ['cell-bentrok-dosen-group', 'cell-bentrok-dosen-course', 'cell-bentrok-dosen-time']
          ));
        }

        tr.appendChild(td);
      });

      tbodyMatriksMingguan.appendChild(tr);
    });
  }

  function createStackedCellBox(pillClass, badgeText, deskripsiText, subtextClass, lineClasses) {
    const boxDiv = document.createElement('div');
    boxDiv.className = 'jadwal-cell-box';

    const badgeSpan = document.createElement('span');
    badgeSpan.className = `cell-pill-badge ${pillClass}`;
    badgeSpan.textContent = badgeText;
    boxDiv.appendChild(badgeSpan);

    const subtitleDiv = document.createElement('div');
    subtitleDiv.className = subtextClass;
    const lines = String(deskripsiText || '').split('\n');
    const isResolverFn = typeof lineClasses === 'function';
    const fallbackClass = !isResolverFn && Array.isArray(lineClasses)
      ? (lineClasses[lineClasses.length - 1] || '')
      : '';
    lines.forEach(function (lineText, idx) {
      const lineEl = document.createElement('div');
      lineEl.className = isResolverFn
        ? lineClasses(lineText, idx)
        : (lineClasses[idx] || fallbackClass);
      lineEl.textContent = lineText;
      subtitleDiv.appendChild(lineEl);
    });
    boxDiv.appendChild(subtitleDiv);

    return boxDiv;
  }

  function formatNoBreakTime(text) {
    return String(text || '').replace(/(\d{2}\.\d{2})\s-\s(\d{2}\.\d{2})/g, '$1\u00A0-\u00A0$2');
  }

  function createPartialFreeCellBox(primaryLabel, secondaryText) {
    const boxDiv = document.createElement('div');
    boxDiv.className = 'jadwal-cell-box cell-partial-box';

    const primarySpan = document.createElement('span');
    primarySpan.className = 'cell-partial-primary';
    primarySpan.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
        <polyline points="22 4 12 14.01 9 11.01"></polyline>
      </svg>
    `;
    const labelSpan = document.createElement('span');
    labelSpan.textContent = formatNoBreakTime(primaryLabel);
    primarySpan.appendChild(labelSpan);
    boxDiv.appendChild(primarySpan);

    if (secondaryText) {
      const secondaryDiv = document.createElement('div');
      secondaryDiv.className = 'cell-partial-secondary';
      String(secondaryText).split('\n').forEach(function (lineText) {
        const lineEl = document.createElement('div');
        lineEl.className = 'cell-partial-booked';
        lineEl.textContent = formatNoBreakTime(lineText);
        secondaryDiv.appendChild(lineEl);
      });
      boxDiv.appendChild(secondaryDiv);
    }

    return boxDiv;
  }

  function bindCellClick(element, handler) {
    element.addEventListener('click', handler);
    element.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handler();
      }
    });
  }

  // =========================================================================
  // Screen 3 (Modal 2A): Safe Slot Confirmation
  // =========================================================================

  function openModalSlotAman(sel) {
    if (!modalKonfirmasiSlot || !sel) return;
    selectedCellContext = sel;

    const group = getSelectedGroupObj();
    const durasi = group && group.durasiMenit ? group.durasiMenit : 30;
    const labelHariTanggal = sel.labelHariTanggal || `${sel.namaHari}, ${sel.tanggal}`;
    const labelRentang = sel.labelRentangJam || `${sel.jamMulai} - ${sel.jamSelesai}`;
    const isPartial = sel.status === 'TERSEDIA_SEBAGIAN' || sel.status === 'PARTIAL_FREE';

    if (teksKonfirmasiSlotAman) {
      teksKonfirmasiSlotAman.textContent = isPartial
        ? `Anda memilih hari ${labelHariTanggal} pada rentang jam ${labelRentang}. Sisa sub-slot yang tersedia di blok ini dapat langsung dipilih tanpa bentrok.`
        : `Anda memilih hari ${labelHariTanggal} pada rentang jam ${labelRentang}. Semua anggota kelompok dapat hadir.`;
    }
    if (labelSubslotAman) {
      labelSubslotAman.textContent = `Pilih Slot Waktu Spesifik (Durasi: ${durasi} Menit)`;
    }

    const subSlots = generateSubSlots(sel.jamMulai, sel.jamSelesai, durasi, sel.intervalTerpakai);
    if (selectSubslotAman) {
      selectSubslotAman.innerHTML = '';
      if (subSlots.length === 0) {
        const opt = document.createElement('option');
        opt.value = '';
        opt.textContent = 'Tidak ada slot waktu tersisa';
        selectSubslotAman.appendChild(opt);
        if (btnSimpanSlotAman) btnSimpanSlotAman.disabled = true;
      } else {
        if (btnSimpanSlotAman) btnSimpanSlotAman.disabled = false;
        subSlots.forEach(function (sub) {
          const opt = document.createElement('option');
          opt.value = `${sub.jamMulai}|${sub.jamSelesai}`;
          opt.textContent = sub.label;
          selectSubslotAman.appendChild(opt);
        });
        selectSubslotAman.selectedIndex = 0;
      }
    }

    modalKonfirmasiSlot.classList.add('active');
  }

  function closeModalSlotAman() {
    if (!modalKonfirmasiSlot) return;
    modalKonfirmasiSlot.classList.remove('active');
  }

  if (btnCloseModalSlotAman) btnCloseModalSlotAman.addEventListener('click', closeModalSlotAman);
  if (btnBatalSlotAman) btnBatalSlotAman.addEventListener('click', closeModalSlotAman);

  if (btnSimpanSlotAman) {
    btnSimpanSlotAman.addEventListener('click', async function () {
      if (!selectedCellContext || !selectSubslotAman) return;
      const parts = (selectSubslotAman.value || '').split('|');
      const jamMulai = parts[0] || selectedCellContext.jamMulai;
      const jamSelesai = parts[1] || selectedCellContext.jamSelesai;
      await submitPresentasiSchedule(selectedCellContext.tanggal, jamMulai, jamSelesai, false);
    });
  }

  // =========================================================================
  // Screen 4 (Modal 2B): Partial Conflict Warning & Override
  // =========================================================================

  function openModalPeringatanBentrok(sel) {
    if (!modalPeringatanBentrok || !sel) return;
    selectedCellContext = sel;

    const group = getSelectedGroupObj();
    const durasi = group && group.durasiMenit ? group.durasiMenit : 30;
    const labelHariTanggal = sel.labelHariTanggal || `${sel.namaHari}, ${sel.tanggal}`;
    const labelRentang = sel.labelRentangJam || `${sel.jamMulai} - ${sel.jamSelesai}`;
    const daftarMhs = sel.daftarMahasiswaBentrok || 'anggota kelompok';
    const namaAgenda = sel.namaAgendaBentrok || sel.deskripsi || 'Kuliah/Ujian';

    if (teksPeringatanBentrok) {
      teksPeringatanBentrok.textContent = `Anda memilih hari ${labelHariTanggal} pada rentang jam ${labelRentang}. Mahasiswa atas nama ${daftarMhs} berhalangan hadir (${namaAgenda}). Apakah Anda yakin ingin memaksakan jadwal ini?`;
    }
    if (labelSubslotBentrok) {
      labelSubslotBentrok.textContent = `Pilih Slot Waktu Spesifik (Durasi: ${durasi} Menit)`;
    }

    const subSlots = generateSubSlots(sel.jamMulai, sel.jamSelesai, durasi, sel.intervalTerpakai);
    if (selectSubslotBentrok) {
      selectSubslotBentrok.innerHTML = '';
      if (subSlots.length === 0) {
        const opt = document.createElement('option');
        opt.value = '';
        opt.textContent = 'Tidak ada slot waktu tersisa';
        selectSubslotBentrok.appendChild(opt);
        if (btnTetapSimpanBentrok) btnTetapSimpanBentrok.disabled = true;
      } else {
        if (btnTetapSimpanBentrok) btnTetapSimpanBentrok.disabled = false;
        subSlots.forEach(function (sub) {
          const opt = document.createElement('option');
          opt.value = `${sub.jamMulai}|${sub.jamSelesai}`;
          opt.textContent = sub.label;
          selectSubslotBentrok.appendChild(opt);
        });
      }
    }

    modalPeringatanBentrok.classList.add('active');
  }

  function closeModalPeringatanBentrok() {
    if (!modalPeringatanBentrok) return;
    modalPeringatanBentrok.classList.remove('active');
  }

  if (btnCloseModalBentrok) btnCloseModalBentrok.addEventListener('click', closeModalPeringatanBentrok);
  if (btnCariWaktuLain) btnCariWaktuLain.addEventListener('click', closeModalPeringatanBentrok);

  if (btnTetapSimpanBentrok) {
    btnTetapSimpanBentrok.addEventListener('click', async function () {
      if (!selectedCellContext || !selectSubslotBentrok) return;
      const parts = (selectSubslotBentrok.value || '').split('|');
      const jamMulai = parts[0] || selectedCellContext.jamMulai;
      const jamSelesai = parts[1] || selectedCellContext.jamSelesai;
      await submitPresentasiSchedule(selectedCellContext.tanggal, jamMulai, jamSelesai, true);
    });
  }

  // =========================================================================
  // Submit & Cancel Presentation Schedule Calls
  // =========================================================================

  async function submitPresentasiSchedule(tanggal, jamMulai, jamSelesai, forceOverride) {
    const kelas = getSelectedClassObj();
    const group = getSelectedGroupObj();
    if (!group) return;

    const durasi = group.durasiMenit || 30;
    const ruangan = kelas && kelas.ruanganDefault ? kelas.ruanganDefault : 'Ruang 9122';

    // If this group already has an active presentation, delete it first when rescheduling
    if (group.terjadwal && group.idPresentasi) {
      try {
        await fetch(`/api/dosen/penjadwalan/presentasi/${encodeURIComponent(group.idPresentasi)}`, {
          method: 'DELETE'
        });
      } catch (ignored) {
        // Proceed with save
      }
    }

    const payload = {
      idKelas: kelas ? kelas.idKelas : 102,
      idKelompok: group.idKelompok,
      tanggal: tanggal,
      jamMulai: jamMulai,
      jamSelesai: jamSelesai,
      durasiMenit: durasi,
      idRuangan: ruangan,
      ruangan: ruangan,
      forceOverride: Boolean(forceOverride)
    };

    try {
      const response = await fetch('/api/dosen/penjadwalan/presentasi', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      if (response.status === 409) {
        const errData = await response.json();
        closeModalSlotAman();
        closeModalPeringatanBentrok();
        closeModalVisualisasi();
        showFeedback((errData && errData.message) || 'Jadwal bentrok dan tidak dapat disimpan.', true);
        return;
      }

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const saved = await response.json();
      closeModalSlotAman();
      closeModalPeringatanBentrok();
      closeModalVisualisasi();

      const noKel = group.noKelompok || group.nomorKelompok || 1;
      showFeedback(
        `Jadwal presentasi Kelompok ${noKel} berhasil disimpan pada ${saved.tanggal} (${saved.jamMulai} - ${saved.jamSelesai} WIB) di ${saved.ruangan}.`,
        false
      );

      await loadKelompokList(kelas ? kelas.idKelas : 102, group.idKelompok);
      await loadWeeklyMatrixData();
    } catch (err) {
      console.error('Gagal menyimpan jadwal presentasi:', err);
      closeModalSlotAman();
      closeModalPeringatanBentrok();
      showFeedback('Gagal menyimpan jadwal presentasi. Silakan coba kembali.', true);
    }
  }

  function openModalBatal(idPresentasi, noKelompok) {
    if (!modalKonfirmasiBatal || !idPresentasi) return;
    presentasiToCancel = { idPresentasi: idPresentasi, noKelompok: noKelompok || 1 };
    if (batalGroupNameEl) {
      batalGroupNameEl.textContent = `Kelompok ${presentasiToCancel.noKelompok}`;
    }
    modalKonfirmasiBatal.classList.remove('hidden');
    modalKonfirmasiBatal.classList.add('active');
  }

  function closeModalBatal() {
    if (!modalKonfirmasiBatal) return;
    modalKonfirmasiBatal.classList.remove('active');
    modalKonfirmasiBatal.classList.add('hidden');
    presentasiToCancel = null;
  }

  if (btnCancelBatal) btnCancelBatal.addEventListener('click', closeModalBatal);
  if (btnConfirmBatal) {
    btnConfirmBatal.addEventListener('click', async function () {
      if (!presentasiToCancel || !presentasiToCancel.idPresentasi) return;
      const target = presentasiToCancel;
      closeModalBatal();

      try {
        const response = await fetch(`/api/dosen/penjadwalan/presentasi/${encodeURIComponent(target.idPresentasi)}`, {
          method: 'DELETE'
        });
        if (!response.ok && response.status !== 204) {
          throw new Error(`HTTP ${response.status}`);
        }
        const kelas = getSelectedClassObj();
        const group = getSelectedGroupObj();
        showFeedback(`Jadwal presentasi Kelompok ${target.noKelompok} berhasil dibatalkan.`, false);
        await loadKelompokList(kelas ? kelas.idKelas : 102, group ? group.idKelompok : null);
        await loadWeeklyMatrixData();
      } catch (err) {
        console.error('Gagal membatalkan jadwal:', err);
        showFeedback('Gagal membatalkan jadwal presentasi.', true);
      }
    });
  }

  // Global Modal Overlay Click & Escape Key Support
  [modalVisualisasi, modalKonfirmasiSlot, modalPeringatanBentrok, modalKonfirmasiBatal].forEach(function (overlay) {
    if (overlay) {
      overlay.addEventListener('click', function (e) {
        if (e.target === overlay) {
          if (overlay === modalKonfirmasiSlot) closeModalSlotAman();
          else if (overlay === modalPeringatanBentrok) closeModalPeringatanBentrok();
          else if (overlay === modalVisualisasi) closeModalVisualisasi();
          else if (overlay === modalKonfirmasiBatal) closeModalBatal();
        }
      });
    }
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      if (modalKonfirmasiSlot && modalKonfirmasiSlot.classList.contains('active')) {
        closeModalSlotAman();
      } else if (modalPeringatanBentrok && modalPeringatanBentrok.classList.contains('active')) {
        closeModalPeringatanBentrok();
      } else if (modalKonfirmasiBatal && modalKonfirmasiBatal.classList.contains('active')) {
        closeModalBatal();
      } else if (modalVisualisasi && modalVisualisasi.classList.contains('active')) {
        closeModalVisualisasi();
      }
    }
  });

  function getFallbackClasses() {
    return [
      {
        idKelas: 102,
        idMatkul: 'IF3130',
        namaMatkul: 'Jaringan Komputer',
        kodeKelas: 'B',
        sks: 3,
        ruanganDefault: 'Ruang 9122',
        namaDosen: 'Dr. Budi Santoso',
        emailDosen: 'budi.santoso@unpar.ac.id'
      },
      {
        idKelas: 101,
        idMatkul: 'IF2110',
        namaMatkul: 'Algoritma dan Struktur Data',
        kodeKelas: 'A',
        sks: 4,
        ruanganDefault: 'Lab Komputer 1',
        namaDosen: 'Dr. Budi Santoso',
        emailDosen: 'budi.santoso@unpar.ac.id'
      }
    ];
  }

  function getFallbackKelompok(idKelas) {
    if (Number(idKelas) === 101) {
      return [
        {
          idKelompok: 11,
          noKelompok: 1,
          nomorKelompok: 1,
          topik: 'Visualisasi Struktur Data Pohon Berimbang',
          batasKapasitas: 4,
          durasiMenit: 30,
          terjadwal: false,
          anggota: [
            { npm: '6182201001', nama: 'Dimas Pratama', isKetua: true },
            { npm: '6182201002', nama: 'Evelyn Wijaya', isKetua: false },
            { npm: '6182201003', nama: 'Farhan Akbar', isKetua: false }
          ]
        }
      ];
    }
    return [
      {
        idKelompok: 1,
        noKelompok: 1,
        nomorKelompok: 1,
        topik: 'Sistem Pendeteksi Intrusi Jaringan',
        batasKapasitas: 4,
        durasiMenit: 30,
        terjadwal: false,
        anggota: [
          { npm: '6182101041', nama: 'Budi', isKetua: true },
          { npm: '6182101044', nama: 'Citra', isKetua: false },
          { npm: '6182101034', nama: 'Andi', isKetua: false }
        ]
      },
      {
        idKelompok: 2,
        noKelompok: 2,
        nomorKelompok: 2,
        topik: 'Perancangan Topologi Jaringan Kampus',
        batasKapasitas: 4,
        durasiMenit: 30,
        terjadwal: false,
        anggota: [
          { npm: '6182101001', nama: 'Andi Saputra', isKetua: true },
          { npm: '6182101002', nama: 'Nurul Hidayah', isKetua: false },
          { npm: '6182101003', nama: 'Rizky Maulana', isKetua: false },
          { npm: '6182101004', nama: 'Salsabila Putri', isKetua: false }
        ]
      }
    ];
  }

  document.addEventListener('DOMContentLoaded', initializePage);
})();
