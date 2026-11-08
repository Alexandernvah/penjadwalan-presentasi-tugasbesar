// dosen-kelola-kelompok.js - Lecturer Group Management Controller
(function () {
  'use strict';

  // Profile Dropdown Toggle
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

  // DOM Elements
  const selectKelas = document.getElementById('select-kelas');
  const containerKelompok = document.getElementById('container-kelompok');
  const modalTambahKelompok = document.getElementById('modal-tambah-kelompok');
  const btnOpenModal = document.getElementById('btn-open-modal-tambah');
  const btnCloseModal = document.getElementById('btn-close-modal-tambah');
  const btnCancelModal = document.getElementById('btn-cancel-modal-tambah');
  const formTambahKelompok = document.getElementById('form-tambah-kelompok');

  // Modal Control
  function openModal() {
    if (modalTambahKelompok) {
      modalTambahKelompok.classList.add('active');
    }
  }

  function closeModal() {
    if (modalTambahKelompok) {
      modalTambahKelompok.classList.remove('active');
      if (formTambahKelompok) formTambahKelompok.reset();
    }
  }

  if (btnOpenModal) btnOpenModal.addEventListener('click', openModal);
  if (btnCloseModal) btnCloseModal.addEventListener('click', closeModal);
  if (btnCancelModal) btnCancelModal.addEventListener('click', closeModal);

  if (modalTambahKelompok) {
    modalTambahKelompok.addEventListener('click', function (e) {
      if (e.target === modalTambahKelompok) {
        closeModal();
      }
    });
  }

  // Load Classes into Filter
  async function loadClasses() {
    try {
      const authNidn = (typeof API !== 'undefined' && API.getToken) ? localStorage.getItem('auth_id') : null;
      const queryParam = authNidn ? `?dosenId=${encodeURIComponent(authNidn)}` : '';

      const response = await fetch(`/api/dosen/dashboard/summary${queryParam}`);
      if (response.ok) {
        const data = await response.json();
        if (data.namaDosen) {
          const topbarName = document.getElementById('topbar-lecturer-name');
          const dropdownName = document.getElementById('dropdown-lecturer-name');
          const dropdownEmail = document.getElementById('dropdown-lecturer-email');
          if (topbarName) topbarName.textContent = data.namaDosen;
          if (dropdownName) dropdownName.textContent = data.namaDosen;
          if (dropdownEmail && data.email) dropdownEmail.textContent = data.email;
        }

        const classes = data.mataKuliahAjar || [];
        populateClassDropdown(classes);
      } else {
        populateFallbackClasses();
      }
    } catch (err) {
      console.warn('Fallback loading class list:', err);
      populateFallbackClasses();
    }
  }

  function populateClassDropdown(classes) {
    if (!selectKelas) return;
    selectKelas.innerHTML = '';

    if (!classes || classes.length === 0) {
      populateFallbackClasses();
      return;
    }

    const seen = new Set();
    classes.forEach(function (c) {
      const classId = c.idKelas || 102;
      if (!seen.has(classId)) {
        seen.add(classId);
        const opt = document.createElement('option');
        opt.value = classId;
        const code = c.idMatkul || 'IF3130';
        const name = c.namaMatkul || 'Jaringan Komputer';
        const kls = c.kodeKelas ? `Kelas ${c.kodeKelas}` : 'Kelas B';
        opt.textContent = `${code} - ${name} (${kls})`;
        selectKelas.appendChild(opt);
      }
    });

    loadKelompokData(selectKelas.value);
  }

  function populateFallbackClasses() {
    if (!selectKelas) return;
    selectKelas.innerHTML = `
      <option value="102">IF3130 - Jaringan Komputer (Kelas B)</option>
      <option value="101">IF2110 - Algoritma dan Struktur Data (Kelas A)</option>
    `;
    loadKelompokData(selectKelas.value);
  }


  // Load Groups
  async function loadKelompokData(kelasId) {
    if (!containerKelompok) return;
    containerKelompok.innerHTML = '<div class="dosen-course-empty">Memuat daftar kelompok...</div>';

    try {
      const q = kelasId ? `?kelasId=${encodeURIComponent(kelasId)}` : '';
      const response = await fetch(`/api/dosen/kelompok${q}`);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const list = await response.json();
      renderKelompokCards(list);
    } catch (err) {
      console.error('Error fetching kelompok:', err);
      renderKelompokCards(getFallbackKelompok());
    }
  }

  function renderKelompokCards(groups) {
    if (!containerKelompok) return;
    containerKelompok.innerHTML = '';

    if (!groups || groups.length === 0) {
      containerKelompok.innerHTML = `
        <div class="dosen-course-empty">
          Belum ada kelompok yang terdaftar untuk kelas ini.
        </div>
      `;
      return;
    }

    groups.forEach(function (group) {
      const card = document.createElement('article');
      card.className = 'dosen-kelompok-card';

      const nomor = group.nomorKelompok || 1;
      const topik = group.topik || 'Belum Ditentukan';
      const kapasitas = group.batasKapasitas || 4;
      const durasi = group.durasiMenit || 30;
      const anggota = group.anggota || [];
      const currentAnggota = anggota.length;
      const isLengkap = group.status === 'Lengkap' || currentAnggota >= kapasitas;

      let statusBadgeHtml = '';
      if (isLengkap) {
        statusBadgeHtml = `
          <span class="dosen-badge-status status-emerald">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            <span>Lengkap</span>
          </span>
        `;
      } else {
        statusBadgeHtml = `
          <span class="dosen-badge-status status-amber">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
            <span>Menunggu Mahasiswa</span>
          </span>
        `;
      }

      let memberItemsHtml = '';
      if (anggota.length > 0) {
        memberItemsHtml = anggota.map(function (m) {
          const name = m.nama || '-';
          const npm = m.npm ? ` (${m.npm})` : '';
          return `
            <div class="dosen-member-item">
              <svg class="dosen-member-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              <span>${name}${npm}</span>
            </div>
          `;
        }).join('');
      } else {
        memberItemsHtml = '<div class="dosen-members-empty">Belum ada anggota terdaftar</div>';
      }

      card.innerHTML = `
        <div class="dosen-kelompok-card-header">
          <h3 class="dosen-kelompok-card-title">Kelompok ${nomor}</h3>
          <button type="button" class="dosen-card-action-btn" aria-label="Menu Opsi Kelompok ${nomor}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" width="18" height="18" aria-hidden="true">
              <circle cx="12" cy="12" r="1"></circle>
              <circle cx="12" cy="5" r="1"></circle>
              <circle cx="12" cy="19" r="1"></circle>
            </svg>
          </button>
        </div>

        <hr class="dosen-kelompok-card-divider">

        <p class="dosen-topic-label">Topik / Judul:</p>
        <p class="dosen-topic-text">${topik}</p>

        <div class="dosen-chips-row">
          <span class="dosen-soft-chip">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            <span>Kapasitas: ${kapasitas} Orang</span>
          </span>
          <span class="dosen-soft-chip">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
            <span>Durasi: ${durasi} Menit</span>
          </span>
        </div>

        <div class="dosen-members-section">
          <div class="dosen-members-header">Anggota Terdaftar (${currentAnggota}/${kapasitas}):</div>
          <div class="dosen-members-list">
            ${memberItemsHtml}
          </div>
        </div>

        <div class="dosen-status-footer">
          ${statusBadgeHtml}
        </div>
      `;

      containerKelompok.appendChild(card);
    });
  }

  // Handle Form Submission
  if (formTambahKelompok) {
    formTambahKelompok.addEventListener('submit', async function (e) {
      e.preventDefault();

      const nomorVal = parseInt(document.getElementById('input-nomor-kelompok').value, 10);
      const topikVal = document.getElementById('input-topik-kelompok').value.trim();
      const kapasitasVal = parseInt(document.getElementById('input-kapasitas-kelompok').value, 10);
      const durasiVal = parseInt(document.getElementById('input-durasi-kelompok').value, 10);
      const currentClassId = selectKelas ? parseInt(selectKelas.value, 10) : 102;

      const payload = {
        idKelas: currentClassId,
        nomorKelompok: nomorVal,
        topik: topikVal,
        batasKapasitas: kapasitasVal,
        durasiMenit: durasiVal
      };

      try {
        const response = await fetch('/api/dosen/kelompok', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (!response.ok) {
          throw new Error(`Gagal menyimpan kelompok (${response.status})`);
        }

        closeModal();
        loadKelompokData(currentClassId);
      } catch (err) {
        console.error('Error saving kelompok:', err);
        closeModal();
        loadKelompokData(currentClassId);
      }
    });
  }

  // Change Event on Filter Select
  if (selectKelas) {
    selectKelas.addEventListener('change', function () {
      loadKelompokData(this.value);
    });
  }

  function getFallbackKelompok() {
    return [
      {
        idKelompok: 1,
        nomorKelompok: 1,
        topik: 'Sistem Pendeteksi Intrusi Jaringan',
        batasKapasitas: 4,
        durasiMenit: 30,
        status: 'Menunggu Mahasiswa',
        anggota: [
          { npm: '6182101041', nama: 'Budi' },
          { npm: '6182101044', nama: 'Citra' },
          { npm: '6182101034', nama: 'Andi' }
        ]
      },
      {
        idKelompok: 2,
        nomorKelompok: 2,
        topik: 'Perancangan Topologi Jaringan Kampus',
        batasKapasitas: 4,
        durasiMenit: 30,
        status: 'Lengkap',
        anggota: [
          { npm: '6182101001', nama: 'Andi Saputra' },
          { npm: '6182101002', nama: 'Nurul Hidayah' },
          { npm: '6182101003', nama: 'Rizky Maulana' },
          { npm: '6182101004', nama: 'Salsabila Putri' }
        ]
      },
      {
        idKelompok: 3,
        nomorKelompok: 3,
        topik: 'E-Commerce UMKM',
        batasKapasitas: 5,
        durasiMenit: 30,
        status: 'Menunggu Mahasiswa',
        anggota: [
          { npm: '6182101015', nama: 'Hana' },
          { npm: '6182101016', nama: 'Iqbal' }
        ]
      }
    ];
  }

  // Initialize
  loadClasses();
})();

