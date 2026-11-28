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

  // DOM Elements
  const selectKelas = document.getElementById('select-kelas');
  const containerKelompok = document.getElementById('container-kelompok');

  // Modal Tambah (Batch) Elements
  const modalTambahKelompok = document.getElementById('modal-tambah-kelompok');
  const btnOpenModal = document.getElementById('btn-open-modal-tambah');
  const btnCloseModal = document.getElementById('btn-close-modal-tambah');
  const btnCancelModal = document.getElementById('btn-cancel-modal-tambah');
  const formTambahKelompok = document.getElementById('form-tambah-kelompok');

  // Modal Edit Elements
  const modalEditKelompok = document.getElementById('modal-edit-kelompok');
  const btnCloseModalEdit = document.getElementById('btn-close-modal-edit');
  const btnCancelModalEdit = document.getElementById('btn-cancel-modal-edit');
  const formEditKelompok = document.getElementById('form-edit-kelompok');
  const modalEditTitle = document.getElementById('modal-edit-title');
  const editIdKelompokInput = document.getElementById('edit-id-kelompok');
  const editTopikInput = document.getElementById('edit-topik');
  const editKapasitasInput = document.getElementById('edit-kapasitas');
  const editDurasiInput = document.getElementById('edit-durasi');

  // Modal Konfirmasi Hapus Elements
  const modalKonfirmasiHapus = document.getElementById('modal-konfirmasi-hapus');
  const deleteGroupNameEl = document.getElementById('delete-group-name');
  const btnCancelDelete = document.getElementById('btn-cancel-delete');
  const btnConfirmDelete = document.getElementById('btn-confirm-delete');
  let groupToDelete = null;

  // Current kelompok cache for fallback / local state updates
  let currentKelompokList = [];

  // Modal Control - Tambah
  function showModalError(bannerId, textId, message) {
    const banner = document.getElementById(bannerId);
    const text = document.getElementById(textId);
    if (text) text.textContent = message;
    if (banner) banner.classList.remove('hidden');
  }

  function hideModalError(bannerId) {
    const banner = document.getElementById(bannerId);
    if (banner) banner.classList.add('hidden');
  }

  function openModal() {
    if (modalTambahKelompok) {
      hideModalError('tambah-modal-error');
      modalTambahKelompok.classList.add('active');
    }
  }

  function closeModal() {
    if (modalTambahKelompok) {
      hideModalError('tambah-modal-error');
      modalTambahKelompok.classList.remove('active');
      if (formTambahKelompok) formTambahKelompok.reset();
    }
  }

  // Modal Control - Edit
  function openEditModal(group) {
    if (!modalEditKelompok) return;
    hideModalError('edit-modal-error');
    if (modalEditTitle) {
      modalEditTitle.textContent = `Edit Kelompok ${group.nomorKelompok || ''}`;
    }
    if (editIdKelompokInput) editIdKelompokInput.value = group.idKelompok;
    if (editTopikInput) editTopikInput.value = group.topik || '';
    if (editKapasitasInput) editKapasitasInput.value = group.batasKapasitas || 4;
    if (editDurasiInput) editDurasiInput.value = group.durasiMenit != null ? group.durasiMenit : '';

    modalEditKelompok.classList.add('active');
  }

  function closeEditModal() {
    if (modalEditKelompok) {
      hideModalError('edit-modal-error');
      modalEditKelompok.classList.remove('active');
      if (formEditKelompok) formEditKelompok.reset();
    }
  }

  // Modal Control - Konfirmasi Hapus
  function openDeleteModal(idKelompok, nomorKelompok) {
    if (!modalKonfirmasiHapus) return;
    groupToDelete = { id: idKelompok, nomor: nomorKelompok };
    if (deleteGroupNameEl) {
      deleteGroupNameEl.textContent = `Kelompok ${nomorKelompok}`;
    }
    modalKonfirmasiHapus.classList.remove('hidden');
    modalKonfirmasiHapus.classList.add('active');
  }

  function closeDeleteModal() {
    if (!modalKonfirmasiHapus) return;
    modalKonfirmasiHapus.classList.remove('active');
    modalKonfirmasiHapus.classList.add('hidden');
    groupToDelete = null;
  }

  if (btnOpenModal) btnOpenModal.addEventListener('click', openModal);
  if (btnCloseModal) btnCloseModal.addEventListener('click', closeModal);
  if (btnCancelModal) btnCancelModal.addEventListener('click', closeModal);

  if (btnCloseModalEdit) btnCloseModalEdit.addEventListener('click', closeEditModal);
  if (btnCancelModalEdit) btnCancelModalEdit.addEventListener('click', closeEditModal);

  if (btnCancelDelete) btnCancelDelete.addEventListener('click', closeDeleteModal);

  if (btnConfirmDelete) {
    btnConfirmDelete.addEventListener('click', async function () {
      if (!groupToDelete || !groupToDelete.id) return;
      const targetId = groupToDelete.id;
      closeDeleteModal();
      await executeDeleteKelompok(targetId);
    });
  }

  if (modalTambahKelompok) {
    modalTambahKelompok.addEventListener('click', function (e) {
      if (e.target === modalTambahKelompok) {
        closeModal();
      }
    });
  }

  if (modalEditKelompok) {
    modalEditKelompok.addEventListener('click', function (e) {
      if (e.target === modalEditKelompok) {
        closeEditModal();
      }
    });
  }

  if (modalKonfirmasiHapus) {
    modalKonfirmasiHapus.addEventListener('click', function (e) {
      if (e.target === modalKonfirmasiHapus) {
        closeDeleteModal();
      }
    });
  }

  // Global Outside Click and Escape Key Handling for Dropdowns & Modals
  function closeAllCardDropdowns() {
    document.querySelectorAll('.card-action-menu').forEach(function (menu) {
      menu.classList.add('hidden');
    });
    document.querySelectorAll('.dosen-card-action-btn').forEach(function (btn) {
      btn.setAttribute('aria-expanded', 'false');
    });
  }

  document.addEventListener('click', function (e) {
    if (!e.target.closest('.dosen-kelompok-card-header')) {
      closeAllCardDropdowns();
    }
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      closeAllCardDropdowns();
      closeModal();
      closeEditModal();
      closeDeleteModal();
    }
  });

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
      currentKelompokList = Array.isArray(list) ? list : [];
      renderKelompokCards(currentKelompokList);
    } catch (err) {
      console.error('Error fetching kelompok:', err);
      // ONLY use local mock data if offline or network connection failure
      currentKelompokList = getFallbackKelompok();
      renderKelompokCards(currentKelompokList);
    }
  }

  function renderKelompokCards(groups, appendOnly) {
    if (!containerKelompok) return;
    if (!appendOnly) {
      containerKelompok.innerHTML = '';
    } else {
      const emptyEl = containerKelompok.querySelector('.dosen-course-empty');
      if (emptyEl) emptyEl.remove();
    }

    if (!groups || groups.length === 0) {
      if (!appendOnly) {
        containerKelompok.innerHTML = `
          <div class="dosen-course-empty">
            Belum ada kelompok yang terdaftar untuk kelas ini.
          </div>
        `;
      }
      return;
    }

    groups.forEach(function (group) {
      const card = document.createElement('article');
      card.className = 'dosen-kelompok-card';
      card.setAttribute('data-id', group.idKelompok);

      const nomor = group.nomorKelompok || 1;
      const topikRaw = group.topik || '';
      const kapasitas = group.batasKapasitas || 4;
      const durasi = (group.durasiMenit != null && group.durasiMenit > 0) ? `${group.durasiMenit} Menit` : '-';
      const anggota = group.anggota || [];
      const currentAnggota = anggota.length;
      const isLengkap = group.status === 'Lengkap' || currentAnggota >= kapasitas;

      const isDefaultOrBlankTopic = !topikRaw || 
        topikRaw.trim() === '' || 
        topikRaw.trim().toLowerCase() === 'belum ditentukan' || 
        new RegExp(`^Kelompok\\s+${nomor}$`, 'i').test(topikRaw.trim()) ||
        /^Kelompok\s+\d+$/i.test(topikRaw.trim());

      const displayTopic = isDefaultOrBlankTopic ? 'Belum ada topik tugas besar' : topikRaw.trim();
      const topicClass = isDefaultOrBlankTopic ? 'dosen-topic-text is-placeholder' : 'dosen-topic-text';

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
          <button type="button" class="dosen-card-action-btn btn-card-menu" aria-label="Menu Opsi Kelompok ${nomor}" aria-expanded="false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" width="18" height="18" aria-hidden="true">
              <circle cx="12" cy="12" r="1"></circle>
              <circle cx="12" cy="5" r="1"></circle>
              <circle cx="12" cy="19" r="1"></circle>
            </svg>
          </button>
          <div class="card-action-menu hidden">
            <button type="button" class="action-menu-item btn-edit-group" data-id="${group.idKelompok}">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" width="15" height="15" aria-hidden="true">
                <path d="M12 20h9"></path>
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
              </svg>
              <span>Edit Kelompok</span>
            </button>
            <button type="button" class="action-menu-item text-danger btn-delete-group" data-id="${group.idKelompok}">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" width="15" height="15" aria-hidden="true">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                <line x1="10" y1="11" x2="10" y2="17"></line>
                <line x1="14" y1="11" x2="14" y2="17"></line>
              </svg>
              <span>Hapus Kelompok</span>
            </button>
          </div>
        </div>

        <hr class="dosen-kelompok-card-divider">

        <div class="dosen-topic-container" title="${isDefaultOrBlankTopic ? 'Topik belum ditentukan' : displayTopic}">
          <svg class="dosen-topic-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
          <div class="dosen-topic-content">
            <p class="${topicClass}">${displayTopic}</p>
          </div>
        </div>

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
            <span>Durasi: ${durasi}</span>
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

      // Wire Dropdown Toggle on Three Dots
      const menuBtn = card.querySelector('.btn-card-menu');
      const actionMenu = card.querySelector('.card-action-menu');
      if (menuBtn && actionMenu) {
        menuBtn.addEventListener('click', function (e) {
          e.stopPropagation();
          const isCurrentlyHidden = actionMenu.classList.contains('hidden');
          closeAllCardDropdowns();
          if (isCurrentlyHidden) {
            actionMenu.classList.remove('hidden');
            menuBtn.setAttribute('aria-expanded', 'true');
          }
        });
      }

      // Wire Edit Action
      const editBtn = card.querySelector('.btn-edit-group');
      if (editBtn) {
        editBtn.addEventListener('click', function (e) {
          e.stopPropagation();
          closeAllCardDropdowns();
          openEditModal(group);
        });
      }

      // Wire Delete Action
      const deleteBtn = card.querySelector('.btn-delete-group');
      if (deleteBtn) {
        deleteBtn.addEventListener('click', function (e) {
          e.stopPropagation();
          closeAllCardDropdowns();
          openDeleteModal(group.idKelompok, nomor);
        });
      }

      containerKelompok.appendChild(card);
    });
  }

  // Handle Form Submission: Batch Group Generation
  if (formTambahKelompok) {
    formTambahKelompok.addEventListener('submit', async function (e) {
      e.preventDefault();

      const jumlahKelompokVal = parseInt(document.getElementById('input-jumlah-kelompok').value, 10);
      const kapasitasVal = parseInt(document.getElementById('input-kapasitas').value, 10);
      const durasiVal = parseInt(document.getElementById('input-durasi').value, 10);
      const topikDefaultInput = document.getElementById('input-topik-default');
      const topikVal = topikDefaultInput ? topikDefaultInput.value.trim() : '';

      const currentClassId = selectKelas ? parseInt(selectKelas.value, 10) : 102;

      if (isNaN(jumlahKelompokVal) || jumlahKelompokVal <= 0) {
        showModalError('tambah-modal-error', 'tambah-modal-error-text', 'Jumlah kelompok minimal 1.');
        return;
      }

      if (isNaN(kapasitasVal) || kapasitasVal <= 0) {
        showModalError('tambah-modal-error', 'tambah-modal-error-text', 'Kapasitas kelompok minimal 1 orang.');
        return;
      }

      if (isNaN(durasiVal) || durasiVal < 5) {
        showModalError('tambah-modal-error', 'tambah-modal-error-text', 'Durasi presentasi minimal 5 menit.');
        return;
      }

      hideModalError('tambah-modal-error');

      const payload = {
        idKelas: currentClassId,
        jumlahKelompok: jumlahKelompokVal,
        batasKapasitas: kapasitasVal,
        durasiMenit: durasiVal,
        defaultTopik: topikVal.length > 0 ? topikVal : ''
      };

      try {
        const response = await fetch('/api/dosen/kelompok/batch', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (response.status === 400) {
          const errBody = await response.json().catch(() => ({}));
          showModalError('tambah-modal-error', 'tambah-modal-error-text', errBody.message || 'Data kelompok tidak valid.');
          return;
        }

        if (!response.ok) {
          throw new Error(`Gagal membuat batch kelompok (${response.status})`);
        }

        closeModal();
        loadKelompokData(currentClassId);
      } catch (err) {
        console.error('Error generating batch kelompok, applying local fallback:', err);
        // Fallback generator if offline / mock mode active
        applyFallbackBatchCreate(currentClassId, jumlahKelompokVal, kapasitasVal, durasiVal, topikVal);
        closeModal();
      }
    });
  }

  function applyFallbackBatchCreate(classId, jumlah, kapasitas, durasi, defaultTopik) {
    let currentCards = Array.from(containerKelompok.querySelectorAll('.dosen-kelompok-card'));
    let maxNum = currentCards.reduce((acc, card) => {
      const titleEl = card.querySelector('.dosen-card-title');
      if (titleEl) {
        const match = titleEl.textContent.match(/\d+/);
        if (match) {
          const num = parseInt(match[0], 10);
          return Math.max(acc, num);
        }
      }
      return acc;
    }, 0);

    const newGroups = [];
    for (let i = 1; i <= jumlah; i++) {
      const nomorBaru = maxNum + i;
      const finalTopic = defaultTopik && defaultTopik.trim().length > 0
        ? defaultTopik.trim()
        : `Kelompok ${nomorBaru}`;

      newGroups.push({
        idKelompok: Date.now() + i,
        nomorKelompok: nomorBaru,
        topik: finalTopic,
        batasKapasitas: kapasitas,
        durasiMenit: durasi,
        status: 'Menunggu Mahasiswa',
        anggota: []
      });
    }

    // Append fallback created groups
    currentKelompokList = currentKelompokList.concat(newGroups);
    renderKelompokCards(newGroups, true);
  }

  // Handle Edit Group Form Submission
  if (formEditKelompok) {
    formEditKelompok.addEventListener('submit', async function (e) {
      e.preventDefault();

      const idVal = parseInt(editIdKelompokInput.value, 10);
      const topikVal = editTopikInput.value.trim();
      const kapasitasVal = parseInt(editKapasitasInput.value, 10);
      const durasiVal = parseInt(editDurasiInput.value, 10);

      if (!idVal) {
        showModalError('edit-modal-error', 'edit-modal-error-text', 'ID kelompok tidak ditemukan.');
        return;
      }

      if (!topikVal) {
        showModalError('edit-modal-error', 'edit-modal-error-text', 'Topik / Judul kelompok tidak boleh kosong.');
        return;
      }

      if (isNaN(kapasitasVal) || kapasitasVal <= 0) {
        showModalError('edit-modal-error', 'edit-modal-error-text', 'Kapasitas kelompok minimal 1 orang.');
        return;
      }

      if (isNaN(durasiVal) || durasiVal < 5) {
        showModalError('edit-modal-error', 'edit-modal-error-text', 'Durasi presentasi minimal 5 menit.');
        return;
      }

      hideModalError('edit-modal-error');

      const payload = {
        topik: topikVal,
        batasKapasitas: kapasitasVal,
        durasiMenit: durasiVal
      };

      const currentClassId = selectKelas ? parseInt(selectKelas.value, 10) : 102;

      try {
        const response = await fetch(`/api/dosen/kelompok/${idVal}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (response.status === 400) {
          const errBody = await response.json().catch(() => ({}));
          showModalError('edit-modal-error', 'edit-modal-error-text', errBody.message || 'Data kelompok tidak valid.');
          return;
        }

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        closeEditModal();
        loadKelompokData(currentClassId);
      } catch (err) {
        console.warn('API update failed, updating local fallback state:', err);
        const idx = currentKelompokList.findIndex(k => k.idKelompok === idVal);
        if (idx !== -1) {
          currentKelompokList[idx].topik = topikVal;
          currentKelompokList[idx].batasKapasitas = kapasitasVal;
          currentKelompokList[idx].durasiMenit = durasiVal;
          const currAnggota = currentKelompokList[idx].anggota ? currentKelompokList[idx].anggota.length : 0;
          currentKelompokList[idx].status = currAnggota >= kapasitasVal ? 'Lengkap' : 'Menunggu Mahasiswa';
        }
        closeEditModal();
        renderKelompokCards(currentKelompokList);
      }
    });
  }

  // Handle Delete Group Execution
  async function executeDeleteKelompok(idKelompok) {
    if (!idKelompok) return;
    const currentClassId = selectKelas ? parseInt(selectKelas.value, 10) : 102;

    try {
      const response = await fetch(`/api/dosen/kelompok/${idKelompok}`, {
        method: 'DELETE'
      });

      if (!response.ok && response.status !== 204) {
        throw new Error(`HTTP ${response.status}`);
      }

      // Refresh directly from backend so state reflects actual database state without re-indexing
      await loadKelompokData(currentClassId);
    } catch (err) {
      console.warn('API delete request failed or backend running in fallback mode:', err);
      // Offline fallback: preserve original group numbers, DO NOT re-number
      currentKelompokList = currentKelompokList.filter(k => String(k.idKelompok) !== String(idKelompok));
      renderKelompokCards(currentKelompokList);
    }
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

