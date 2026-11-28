// mahasiswa-input-matakuliah.js - Input Mata Kuliah: pick one class per course, confirm, save once (locked)
(function () {
  'use strict';

  const portal = window.MahasiswaPortal;
  const el = portal.el;
  const NO_TEACHER = '-';
  const REDIRECT_DELAY_MS = 1500;

  const banner = document.getElementById('page-banner');
  const tableBody = document.getElementById('course-table-body');
  const saveButton = document.getElementById('btn-simpan-matakuliah');
  const modal = document.getElementById('modal-konfirmasi');
  const modalDialog = modal.querySelector('.mhs-modal-dialog');
  const modalName = document.getElementById('konfirmasi-nama');
  const modalNpm = document.getElementById('konfirmasi-npm');
  const modalList = document.getElementById('konfirmasi-daftar');
  const confirmButton = document.getElementById('btn-konfirmasi-simpan');
  const cancelButton = document.getElementById('btn-konfirmasi-batal');
  const closeButton = document.getElementById('btn-konfirmasi-tutup');

  let student = { namaMahasiswa: '', npm: '' };
  let rows = [];
  let isSubmitting = false;
  let pendingSelection = [];
  let opener = null;

  // ---------------------------------------------------------------------
  // Table
  // ---------------------------------------------------------------------
  function courseLabel(course) {
    const label = el('div');
    label.append(el('strong', null, course.kodeMatkul), document.createTextNode(' - ' + course.namaMatkul));
    return label;
  }

  function buildRow(course, locked) {
    const checkbox = el('input', 'mhs-checkbox');
    checkbox.type = 'checkbox';
    checkbox.setAttribute('aria-label', 'Pilih ' + course.kodeMatkul + ' ' + course.namaMatkul);

    const select = el('select', 'mhs-select');
    select.setAttribute('aria-label', 'Kelas untuk ' + course.kodeMatkul);
    const placeholder = el('option', null, 'Pilih Kelas');
    placeholder.value = '';
    select.appendChild(placeholder);
    const selectedKelas = locked ? course.daftarKelas.find(function (kelas) { return kelas.dipilih; }) : null;
    course.daftarKelas.forEach(function (kelas) {
      const option = el('option', null, 'Kelas ' + kelas.kodeKelas);
      option.value = String(kelas.idKelas);
      if (selectedKelas && selectedKelas.idKelas === kelas.idKelas) option.selected = true;
      select.appendChild(option);
    });

    const teacherCell = el('td', null, NO_TEACHER);
    const tr = el('tr');
    const row = { course: course, checkbox: checkbox, select: select, teacherCell: teacherCell, element: tr };

    checkbox.addEventListener('change', function () {
      if (!checkbox.checked) select.value = '';
      syncRowState(row);
      portal.clearBanner(banner);
    });
    select.addEventListener('change', function () {
      checkbox.checked = select.value !== '';
      syncRowState(row);
      portal.clearBanner(banner);
    });

    const pickCell = el('td', 'mhs-col-pick');
    pickCell.appendChild(checkbox);
    const classCell = el('td', 'mhs-col-class');
    classCell.appendChild(select);
    const courseCell = el('td');
    courseCell.appendChild(courseLabel(course));

    tr.append(pickCell, courseCell, teacherCell, classCell);
    if (locked) {
      checkbox.checked = Boolean(selectedKelas);
      checkbox.disabled = true;
      select.disabled = true;
    }
    syncRowState(row);
    return row;
  }

  function chosenClass(row) {
    return row.course.daftarKelas.find(function (kelas) { return String(kelas.idKelas) === row.select.value; });
  }

  function syncRowState(row) {
    const kelas = chosenClass(row);
    row.teacherCell.textContent = kelas ? kelas.namaDosenPengajar : NO_TEACHER;
    row.element.classList.toggle('mhs-row-selected', row.checkbox.checked);
  }

  function renderMessageRow(message) {
    portal.clear(tableBody);
    const cell = el('td', 'mhs-empty', message);
    cell.colSpan = 4;
    const tr = el('tr');
    tr.appendChild(cell);
    tableBody.appendChild(tr);
  }

  function renderCourses(data) {
    portal.clear(tableBody);
    rows = data.daftarMataKuliah.map(function (course) { return buildRow(course, data.sudahTerkunci); });
    if (rows.length === 0) {
      renderMessageRow('Belum ada mata kuliah yang dibuka pada semester ini.');
      return;
    }
    rows.forEach(function (row) { tableBody.appendChild(row.element); });
  }

  function applyState(data) {
    student = data;
    portal.showIdentity(data);
    renderCourses(data);
    if (data.sudahTerkunci) {
      portal.showBanner(banner, 'info', portal.LOCK_MESSAGE);
    }
    saveButton.disabled = data.sudahTerkunci || rows.length === 0;
  }

  // ---------------------------------------------------------------------
  // Validation and confirmation modal
  // ---------------------------------------------------------------------
  function collectSelection() {
    return rows
      .filter(function (row) { return row.checkbox.checked; })
      .map(function (row) { return { course: row.course, kelas: chosenClass(row) }; });
  }

  function validateSelection(selection) {
    if (selection.length === 0) return 'Pilih minimal satu mata kuliah beserta kelasnya.';
    const withoutClass = selection.filter(function (item) { return !item.kelas; });
    if (withoutClass.length > 0) {
      return 'Pilih kelas untuk: ' + withoutClass.map(function (item) { return item.course.kodeMatkul; }).join(', ') + '.';
    }
    return null;
  }

  function fillModal(selection) {
    modalName.textContent = student.namaMahasiswa;
    modalNpm.textContent = student.npm;
    portal.clear(modalList);
    selection.forEach(function (item) {
      const tr = el('tr');
      const courseCell = el('td');
      courseCell.appendChild(courseLabel(item.course));
      tr.append(courseCell, el('td', null, item.kelas.namaDosenPengajar), el('td', null, 'Kelas ' + item.kelas.kodeKelas));
      modalList.appendChild(tr);
    });
  }

  function openModal() {
    opener = document.activeElement;
    modal.classList.add('active');
    cancelButton.focus();
  }

  function closeModal() {
    if (isSubmitting) return;
    modal.classList.remove('active');
    if (opener) opener.focus();
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

  function onSaveClick() {
    portal.clearBanner(banner);
    const selection = collectSelection();
    const problem = validateSelection(selection);
    if (problem) {
      portal.showBanner(banner, 'danger', problem);
      return;
    }
    pendingSelection = selection;
    fillModal(selection);
    openModal();
  }

  // ---------------------------------------------------------------------
  // Save
  // ---------------------------------------------------------------------
  function setSubmitting(value) {
    isSubmitting = value;
    confirmButton.disabled = value;
    cancelButton.disabled = value;
    closeButton.disabled = value;
    confirmButton.querySelector('span').textContent = value ? 'Menyimpan...' : 'Konfirmasi & Simpan';
  }

  // After a failed save the server may already hold a locked selection (409); show it read-only.
  async function syncLockAfterError() {
    try {
      const latest = await API.get('/api/mahasiswa/matakuliah');
      if (latest && latest.sudahTerkunci) {
        applyState(latest);
      }
    } catch (ignored) {
      // The error banner for the failed save is already visible.
    }
  }

  async function confirmSave() {
    if (isSubmitting) return;
    setSubmitting(true);
    try {
      const saved = await API.post('/api/mahasiswa/matakuliah', {
        idKelas: pendingSelection.map(function (item) { return item.kelas.idKelas; })
      });
      if (!saved) return;
      setSubmitting(false);
      closeModal();
      applyState(saved);
      portal.showBanner(banner, 'success', 'Mata kuliah berhasil disimpan dan dikunci. Anda akan diarahkan ke Dashboard.');
      window.setTimeout(function () { window.location.assign('dashboard.html'); }, REDIRECT_DELAY_MS);
    } catch (err) {
      setSubmitting(false);
      closeModal();
      portal.showBanner(banner, 'danger', portal.errorMessage(err));
      syncLockAfterError();
    }
  }

  // ---------------------------------------------------------------------
  // Load
  // ---------------------------------------------------------------------
  async function load() {
    portal.clearBanner(banner);
    saveButton.disabled = true;
    renderMessageRow('Memuat daftar mata kuliah...');
    try {
      const data = await API.get('/api/mahasiswa/matakuliah');
      if (!data) return;
      applyState(data);
    } catch (err) {
      renderMessageRow('Daftar mata kuliah tidak dapat ditampilkan.');
      portal.showBanner(banner, 'danger', portal.errorMessage(err), { label: 'Muat Ulang', onClick: load });
    }
  }

  saveButton.addEventListener('click', onSaveClick);
  confirmButton.addEventListener('click', confirmSave);
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

  load();
})();
