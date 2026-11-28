// mahasiswa-pendaftaran-kelompok.js - Pendaftaran Kelompok Tugas Besar: select course, view groups, join/leave
(function () {
  'use strict';

  const portal = window.MahasiswaPortal;
  const el = portal.el;
  const STORAGE_KEY_KELAS = 'mhs_kelompok_selected_kelas';

  const banner = document.getElementById('page-banner');
  const selectorCard = document.getElementById('course-selector-card');
  const courseSelect = document.getElementById('select-matkul-kelompok');
  const contentRoot = document.getElementById('kelompok-content');

  const modal = document.getElementById('modal-konfirmasi-kelompok');
  const modalDialog = modal.querySelector('.mhs-modal-dialog');
  const modalHeader = document.getElementById('modal-kelompok-header');
  const modalTitle = document.getElementById('modal-kelompok-title');
  const modalWarning = document.getElementById('modal-kelompok-warning');
  const modalWarningText = document.getElementById('modal-kelompok-warning-text');
  const modalSummary = document.getElementById('modal-kelompok-summary');
  const modalMembersBlock = document.getElementById('modal-kelompok-members-block');
  const joinModalMemberList = document.getElementById('joinModalMemberList');
  const closeButton = document.getElementById('btn-kelompok-tutup');
  const cancelButton = document.getElementById('btn-kelompok-batal');
  const actionButton = document.getElementById('btn-kelompok-aksi');

  let currentClassData = null;
  let pendingAction = null;
  let isSubmitting = false;
  let opener = null;

  // ---------------------------------------------------------------------
  // Empty and loading states
  // ---------------------------------------------------------------------
  function renderEmptyPanel(iconName, message, ctaLabel, ctaHref) {
    portal.clear(contentRoot);
    const card = el('section', 'mhs-card mhs-empty-panel');
    const iconHolder = el('span', 'mhs-empty-panel-icon');
    iconHolder.appendChild(portal.icon(iconName));
    card.append(iconHolder, el('p', 'mhs-empty-panel-text', message));
    if (ctaLabel && ctaHref) {
      const link = el('a', 'mhs-btn-cta', ctaLabel);
      link.href = ctaHref;
      card.appendChild(link);
    }
    contentRoot.appendChild(card);
  }

  function renderLoading(message) {
    portal.clear(contentRoot);
    const card = el('section', 'mhs-card');
    card.appendChild(el('p', 'mhs-loading', message));
    contentRoot.appendChild(card);
  }

  // ---------------------------------------------------------------------
  // Group card rendering
  // ---------------------------------------------------------------------
  function badgeClassForGroup(group) {
    if (group.isAnggotaSaya) return 'mhs-group-badge-mine';
    if (group.isPenuh) return 'mhs-group-badge-full';
    return 'mhs-group-badge-available';
  }

  function progressClassForGroup(group) {
    if (group.isAnggotaSaya) return 'mhs-progress mhs-progress-mine';
    if (group.isPenuh) return 'mhs-progress mhs-progress-full';
    return 'mhs-progress';
  }

  function buildTopic(topik) {
    const topicNode = el('p', 'mhs-group-topic');
    topicNode.appendChild(el('strong', null, 'Topik: '));
    const hasTopic = topik && topik.trim().length > 0;
    topicNode.appendChild(el('span', hasTopic ? null : 'mhs-group-topic-empty', hasTopic ? topik.trim() : 'Belum ditentukan'));
    return topicNode;
  }

  function buildCapacity(group) {
    const box = el('div', 'mhs-capacity-box');
    const row = el('div', 'mhs-capacity-row');
    row.append(
      el('span', null, 'Kapasitas'),
      el('span', 'mhs-capacity-value', group.jumlahAnggota + '/' + group.kapasitas + ' Terisi')
    );
    const progress = document.createElement('progress');
    progress.className = progressClassForGroup(group);
    progress.max = Math.max(group.kapasitas || 1, 1);
    progress.value = Math.min(group.jumlahAnggota || 0, progress.max);
    progress.setAttribute('aria-label', 'Kapasitas Kelompok ' + group.noKelompok);
    box.append(row, progress);
    return box;
  }

  function buildMembers(group) {
    const section = el('div', 'mhs-members-section');
    section.appendChild(el('h3', 'mhs-members-heading', 'Anggota'));
    if (!group.daftarAnggota || group.daftarAnggota.length === 0) {
      section.appendChild(el('p', 'mhs-member-empty', 'Belum ada anggota terdaftar'));
      return section;
    }

    const list = el('ul', 'mhs-member-list');
    group.daftarAnggota.forEach(function (member) {
      const li = el('li', 'mhs-member-row' + (member.isCurrentUser ? ' mhs-member-row-self' : ''));
      const displayName = member.isCurrentUser ? member.nama + ' (Anda)' : member.nama;
      li.append(el('span', null, displayName), el('span', 'mhs-member-npm', member.npm));
      list.appendChild(li);
    });
    section.appendChild(list);
    return section;
  }

  function buildActionButton(group, classData) {
    if (group.isAnggotaSaya) {
      if (group.sudahTerjadwal) {
        const lockedBtn = el('button', 'mhs-btn-locked mhs-btn-full');
        lockedBtn.type = 'button';
        lockedBtn.disabled = true;
        lockedBtn.append(portal.icon('lock'), el('span', null, 'Jadwal Presentasi Sudah Ditetapkan'));
        return lockedBtn;
      }
      const leaveBtn = el('button', 'mhs-btn-leave mhs-btn-full', 'Keluar dari Kelompok');
      leaveBtn.type = 'button';
      leaveBtn.addEventListener('click', function () {
        openConfirmModal('keluar', group, classData);
      });
      return leaveBtn;
    }

    if (group.isPenuh) {
      const fullBtn = el('button', 'mhs-btn-muted mhs-btn-full', 'Kelompok Penuh');
      fullBtn.type = 'button';
      fullBtn.disabled = true;
      return fullBtn;
    }

    if (classData.idKelompokTerdaftar !== null && classData.idKelompokTerdaftar !== undefined) {
      const alreadyBtn = el('button', 'mhs-btn-muted mhs-btn-full', 'Sudah Terdaftar Pada Kelompok');
      alreadyBtn.type = 'button';
      alreadyBtn.disabled = true;
      return alreadyBtn;
    }

    const joinBtn = el('button', 'dosen-btn-primary mhs-btn-full', 'Gabung Kelompok');
    joinBtn.type = 'button';
    joinBtn.addEventListener('click', function () {
      openConfirmModal('gabung', group, classData);
    });
    return joinBtn;
  }

  function buildGroupCard(group, classData) {
    const card = el('article', 'mhs-group-card' + (group.isAnggotaSaya ? ' mhs-group-card-mine' : ''));

    const header = el('div', 'mhs-group-card-header');
    header.append(
      el('h2', 'mhs-group-title', 'Kelompok ' + group.noKelompok),
      el('span', 'mhs-group-badge ' + badgeClassForGroup(group), group.statusLabel)
    );

    const footer = el('div', 'mhs-group-footer');
    footer.appendChild(buildActionButton(group, classData));

    card.append(header, buildTopic(group.topik), buildCapacity(group), buildMembers(group), footer);
    return card;
  }

  function renderGroups(classData) {
    currentClassData = classData;
    if (!classData.daftarKelompok || classData.daftarKelompok.length === 0) {
      renderEmptyPanel('users', 'Belum ada kelompok yang tersedia untuk mata kuliah ini.');
      return;
    }

    portal.clear(contentRoot);
    const grid = el('section', 'mhs-group-grid');
    grid.setAttribute('aria-label', 'Daftar kelompok mata kuliah');
    classData.daftarKelompok.forEach(function (group) {
      grid.appendChild(buildGroupCard(group, classData));
    });
    contentRoot.appendChild(grid);
  }

  // ---------------------------------------------------------------------
  // Confirmation modal (shared by gabung and keluar)
  // ---------------------------------------------------------------------
  function addSummaryRow(label, value) {
    modalSummary.append(el('dt', null, label), el('dd', null, value));
  }

  function fillModal(mode, group, classData) {
    const isLeave = mode === 'keluar';
    const topikLabel = group.topik && group.topik.trim() ? group.topik.trim() : 'Belum ditentukan';

    modalHeader.classList.toggle('mhs-modal-header-danger', isLeave);
    modalTitle.textContent = isLeave ? 'Konfirmasi Keluar Kelompok' : 'Konfirmasi Gabung Kelompok';
    modalWarning.classList.toggle('hidden', !isLeave);
    modalWarningText.textContent = isLeave ? 'Anda yakin ingin keluar dari kelompok ini?' : '';

    portal.clear(modalSummary);
    addSummaryRow('Mata Kuliah', classData.kodeMatkul + ' - ' + classData.namaMatkul);
    addSummaryRow('Kelas', 'Kelas ' + classData.kodeKelas);
    addSummaryRow('Kelompok', 'Kelompok ' + group.noKelompok);
    addSummaryRow('Topik', topikLabel);
    modalMembersBlock.classList.toggle('hidden', isLeave);
    portal.clear(joinModalMemberList);
    if (!isLeave) {
      addSummaryRow('Kapasitas', group.jumlahAnggota + '/' + group.kapasitas + ' Terisi');
      if (group.daftarAnggota && group.daftarAnggota.length > 0) {
        group.daftarAnggota.forEach(function (member) {
          const item = el('li', 'modal-member-item');
          item.append(
            el('span', 'modal-member-name', member.nama),
            el('span', 'modal-member-npm', member.npm)
          );
          joinModalMemberList.appendChild(item);
        });
      } else {
        joinModalMemberList.appendChild(el('li', 'modal-member-empty', 'Belum ada anggota terdaftar'));
      }
    }

    actionButton.className = isLeave ? 'mhs-btn-danger' : 'dosen-btn-primary';
    actionButton.textContent = isLeave ? 'Ya, Keluar' : 'Ya, Gabung Kelompok';
  }

  function openConfirmModal(mode, group, classData) {
    portal.clearBanner(banner);
    pendingAction = { mode: mode, group: group, classData: classData };
    fillModal(mode, group, classData);
    opener = document.activeElement;
    modal.classList.add('active');
    cancelButton.focus();
  }

  function closeModal() {
    if (isSubmitting) return;
    modal.classList.remove('active');
    pendingAction = null;
    if (opener) opener.focus();
  }

  function setSubmitting(submitting) {
    isSubmitting = submitting;
    actionButton.disabled = submitting;
    cancelButton.disabled = submitting;
    closeButton.disabled = submitting;
    if (submitting) {
      actionButton.textContent = 'Memproses...';
    } else if (pendingAction) {
      actionButton.textContent = pendingAction.mode === 'keluar' ? 'Ya, Keluar' : 'Ya, Gabung Kelompok';
    }
  }

  function trapFocus(event) {
    const focusable = Array.from(modalDialog.querySelectorAll('button:not(:disabled)'));
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  async function executeConfirmedAction() {
    if (isSubmitting || !pendingAction) return;
    const action = pendingAction;
    setSubmitting(true);
    try {
      const updated = action.mode === 'keluar'
        ? await API.delete('/api/mahasiswa/kelompok/' + action.group.idKelompok + '/keluar')
        : await API.post('/api/mahasiswa/kelompok/' + action.group.idKelompok + '/gabung', {});
      if (!updated) return;
      setSubmitting(false);
      closeModal();
      renderGroups(updated);
      const successMsg = action.mode === 'keluar'
        ? 'Anda berhasil keluar dari Kelompok ' + action.group.noKelompok + '.'
        : 'Berhasil bergabung ke Kelompok ' + action.group.noKelompok + '.';
      portal.showBanner(banner, 'success', successMsg);
    } catch (err) {
      setSubmitting(false);
      closeModal();
      portal.showBanner(banner, 'danger', portal.errorMessage(err));
      if (currentClassData && currentClassData.idKelas) {
        loadGroups(currentClassData.idKelas, true);
      }
    }
  }

  // ---------------------------------------------------------------------
  // Data loading & course persistence
  // ---------------------------------------------------------------------
  function saveSelectedClass(idKelas) {
    try {
      sessionStorage.setItem(STORAGE_KEY_KELAS, String(idKelas));
    } catch (ignored) {
      // sessionStorage may be unavailable in restricted contexts.
    }
  }

  function readSavedClass() {
    try {
      return sessionStorage.getItem(STORAGE_KEY_KELAS);
    } catch (ignored) {
      return null;
    }
  }

  function populateCourseDropdown(classes) {
    portal.clear(courseSelect);
    classes.forEach(function (item) {
      const option = el('option', null, item.label);
      option.value = String(item.idKelas);
      courseSelect.appendChild(option);
    });
    const saved = readSavedClass();
    const matched = classes.find(function (item) { return String(item.idKelas) === saved; });
    const initialId = matched ? matched.idKelas : classes[0].idKelas;
    courseSelect.value = String(initialId);
    saveSelectedClass(initialId);
    return initialId;
  }

  async function loadGroups(idKelas, keepBanner) {
    if (!keepBanner) {
      portal.clearBanner(banner);
    }
    renderLoading('Memuat daftar kelompok...');
    try {
      const data = await API.get('/api/mahasiswa/kelompok?idKelas=' + encodeURIComponent(idKelas));
      if (!data) return;
      renderGroups(data);
    } catch (err) {
      renderEmptyPanel('warning', 'Daftar kelompok tidak dapat dimuat.');
      portal.showBanner(banner, 'danger', portal.errorMessage(err), {
        label: 'Muat Ulang',
        onClick: function () { loadGroups(idKelas, false); }
      });
    }
  }

  async function init() {
    portal.clearBanner(banner);
    selectorCard.classList.add('hidden');
    renderLoading('Memuat daftar mata kuliah...');
    try {
      const data = await API.get('/api/mahasiswa/kelompok/mata-kuliah');
      if (!data) return;
      portal.showIdentity(data);

      if (!data.daftarKelasDiambil || data.daftarKelasDiambil.length === 0) {
        renderEmptyPanel(
          'warning',
          'Anda belum melakukan input mata kuliah.',
          'Input Mata Kuliah Sekarang',
          'input-matakuliah.html'
        );
        return;
      }

      const initialId = populateCourseDropdown(data.daftarKelasDiambil);
      selectorCard.classList.remove('hidden');
      await loadGroups(initialId, false);
    } catch (err) {
      renderEmptyPanel('warning', 'Data pendaftaran kelompok gagal dimuat.');
      portal.showBanner(banner, 'danger', portal.errorMessage(err), { label: 'Muat Ulang', onClick: init });
    }
  }

  courseSelect.addEventListener('change', function () {
    const selectedId = courseSelect.value;
    if (!selectedId) return;
    saveSelectedClass(selectedId);
    loadGroups(selectedId, false);
  });

  actionButton.addEventListener('click', executeConfirmedAction);
  cancelButton.addEventListener('click', closeModal);
  closeButton.addEventListener('click', closeModal);
  modal.addEventListener('click', function (event) {
    if (event.target === modal) closeModal();
  });
  document.addEventListener('keydown', function (event) {
    if (!modal.classList.contains('active')) return;
    if (event.key === 'Escape') closeModal();
    if (event.key === 'Tab') trapFocus(event);
  });

  init();
})();
