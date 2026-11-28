// mahasiswa-lihat-jadwal.js - Lihat Jadwal Presentasi: schedule cards grid, unscheduled courses notice, and slide-over detail drawer
(function () {
  'use strict';

  const portal = window.MahasiswaPortal;
  const el = portal.el;

  const banner = document.getElementById('page-banner');
  const contentRoot = document.getElementById('jadwal-content');

  const drawerOverlay = document.getElementById('drawer-detail-jadwal');
  const drawerPanel = drawerOverlay.querySelector('.mhs-drawer-panel');
  const closeDrawerButton = document.getElementById('btn-tutup-drawer');
  const drawerFields = document.getElementById('drawer-detail-fields');
  const drawerMemberList = document.getElementById('drawer-anggota-list');
  const drawerCatatanBox = document.getElementById('drawer-catatan-box');
  const drawerCatatanText = document.getElementById('drawer-catatan-text');

  let drawerOpener = null;

  // ---------------------------------------------------------------------
  // Status pill mapping
  // ---------------------------------------------------------------------
  function pillClassForStatus(statusKode) {
    if (statusKode === 'HARI_INI') return 'mhs-jadwal-pill mhs-jadwal-pill-today';
    if (statusKode === 'AKAN_DATANG') return 'mhs-jadwal-pill mhs-jadwal-pill-upcoming';
    return 'mhs-jadwal-pill mhs-jadwal-pill-done';
  }

  // ---------------------------------------------------------------------
  // Empty and loading views
  // ---------------------------------------------------------------------
  function renderLoading() {
    portal.clear(contentRoot);
    const card = el('section', 'mhs-card');
    card.appendChild(el('p', 'mhs-loading', 'Memuat jadwal presentasi...'));
    contentRoot.appendChild(card);
  }

  function renderEmptySchedule() {
    portal.clear(contentRoot);
    const card = el('section', 'mhs-card mhs-empty-panel');
    const iconHolder = el('span', 'mhs-empty-panel-icon');
    iconHolder.appendChild(portal.icon('calendar'));

    const heading = el('h2', 'mhs-empty-panel-title', 'Belum Ada Jadwal Presentasi');
    const desc = el(
      'p',
      'mhs-empty-panel-desc',
      'Jadwal presentasi tugas besar akan muncul di sini setelah Anda bergabung dalam kelompok dan dosen menetapkan jadwal.'
    );
    const action = el('a', 'dosen-btn-primary', 'Ke Pendaftaran Kelompok');
    action.href = 'pendaftaran-kelompok.html';

    card.append(iconHolder, heading, desc, action);
    contentRoot.appendChild(card);
  }

  // ---------------------------------------------------------------------
  // Schedule cards & unscheduled courses card
  // ---------------------------------------------------------------------
  function buildMetaRow(iconName, text, isMuted) {
    const row = el('li', 'mhs-jadwal-meta-row');
    row.append(
      portal.icon(iconName),
      el('span', isMuted ? 'mhs-jadwal-meta-muted' : null, text)
    );
    return row;
  }

  function buildScheduleCard(item) {
    const card = el('article', 'mhs-jadwal-card');

    const top = el('div', 'mhs-jadwal-card-top');
    const dateBlock = el('div', 'mhs-jadwal-date-block');
    dateBlock.append(
      el('span', 'mhs-jadwal-day-num', item.tanggalAngka),
      el('span', 'mhs-jadwal-month-year', item.bulanTahunLabel)
    );
    top.append(dateBlock, el('span', pillClassForStatus(item.statusKode), item.statusLabel));

    const body = el('div', 'mhs-jadwal-card-body');
    const tagText = item.kodeMatkul + ' \u00B7 Kelas ' + item.kodeKelas + ' \u00B7 Kelompok ' + item.noKelompok;
    const title = el('h2', 'mhs-jadwal-course-title', item.namaMatkul);

    const meta = el('ul', 'mhs-jadwal-meta');
    const hasRoom = Boolean(item.ruangan && item.ruangan.trim());
    meta.append(
      buildMetaRow('clock', item.rentangWaktuLabel, false),
      buildMetaRow('mapPin', hasRoom ? item.ruangan : 'Ruangan belum ditentukan', !hasRoom)
    );
    body.append(el('div', 'mhs-jadwal-tag', tagText), title, meta);

    const footer = el('div', 'mhs-jadwal-card-footer');
    const detailBtn = el('button', 'mhs-btn-outline mhs-btn-full');
    detailBtn.type = 'button';
    detailBtn.append(portal.icon('eye'), el('span', null, 'Lihat Detail'));
    detailBtn.addEventListener('click', function () {
      openDrawer(item, detailBtn);
    });
    footer.appendChild(detailBtn);

    card.append(top, body, footer);
    return card;
  }

  function buildUnscheduledCard(unscheduledList) {
    const card = el('section', 'mhs-card mhs-unscheduled-card');
    card.setAttribute('aria-labelledby', 'heading-menunggu-penjadwalan');

    const header = el('div', 'mhs-card-header');
    const headingWrap = el('div', 'mhs-card-heading');
    const title = el('h2', 'dosen-courses-card-title', 'Menunggu Penjadwalan Dosen');
    title.id = 'heading-menunggu-penjadwalan';
    headingWrap.appendChild(title);
    header.appendChild(headingWrap);

    const subtitle = el(
      'p',
      'mhs-unscheduled-sub',
      'Mata kuliah berikut belum memiliki jadwal presentasi yang ditetapkan.'
    );

    const list = el('div', 'dosen-course-list');
    unscheduledList.forEach(function (course) {
      const row = el('div', 'dosen-course-row');
      const info = el('div', 'dosen-course-info');
      info.append(
        el('strong', 'dosen-course-code', course.kodeMatkul),
        el('span', 'dosen-course-separator', '-'),
        el('span', 'dosen-course-name', course.namaMatkul + ' (Kelas ' + course.kodeKelas + ')')
      );

      let statusControl;
      if (course.sudahPunyaKelompok) {
        statusControl = el(
          'span',
          'mhs-unscheduled-badge',
          'Kelompok ' + course.noKelompok + ' \u00B7 Menunggu Jadwal'
        );
      } else {
        statusControl = el('a', 'mhs-unscheduled-link', 'Belum Pilih Kelompok');
        statusControl.href = 'pendaftaran-kelompok.html';
      }

      row.append(info, statusControl);
      list.appendChild(row);
    });

    card.append(header, subtitle, list);
    return card;
  }

  function renderSchedulePage(data) {
    if (!data.daftarJadwal || data.daftarJadwal.length === 0) {
      renderEmptySchedule();
      return;
    }

    portal.clear(contentRoot);
    const grid = el('section', 'mhs-jadwal-grid');
    grid.setAttribute('aria-label', 'Daftar jadwal presentasi');
    data.daftarJadwal.forEach(function (item) {
      grid.appendChild(buildScheduleCard(item));
    });
    contentRoot.appendChild(grid);

    if (data.daftarKelasBelumTerjadwal && data.daftarKelasBelumTerjadwal.length > 0) {
      contentRoot.appendChild(buildUnscheduledCard(data.daftarKelasBelumTerjadwal));
    }
  }

  // ---------------------------------------------------------------------
  // Slide-over detail drawer
  // ---------------------------------------------------------------------
  function appendDrawerField(label, value, isMuted) {
    const wrapper = el('div', 'mhs-drawer-field');
    const dt = el('dt', null, label);
    const dd = el('dd', isMuted ? 'mhs-drawer-field-muted' : null, value);
    wrapper.append(dt, dd);
    drawerFields.appendChild(wrapper);
  }

  function populateDrawer(item) {
    portal.clear(drawerFields);
    const hasTopik = Boolean(item.topik && item.topik.trim());
    const hasRuangan = Boolean(item.ruangan && item.ruangan.trim());

    appendDrawerField(
      'MATA KULIAH',
      item.kodeMatkul + ' - ' + item.namaMatkul + ' (Kelas ' + item.kodeKelas + ')',
      false
    );
    appendDrawerField('DOSEN PENGAMPU', item.namaDosen || '-', false);
    appendDrawerField('KELOMPOK', 'Kelompok ' + item.noKelompok, false);
    appendDrawerField('TOPIK TUGAS BESAR', hasTopik ? item.topik : 'Belum ditentukan', !hasTopik);
    appendDrawerField('RUANGAN', hasRuangan ? item.ruangan : 'Belum ditentukan', !hasRuangan);
    appendDrawerField('WAKTU', item.hariTanggalFormatted + ' (' + item.rentangWaktuLabel + ')', false);

    portal.clear(drawerMemberList);
    (item.daftarAnggota || []).forEach(function (member) {
      const li = el('li', 'mhs-drawer-member-row');
      const left = el('span', 'mhs-drawer-member-left');
      left.appendChild(portal.icon('user'));
      const nameNode = el('span', member.isCurrentUser ? 'mhs-drawer-member-self' : null, member.nama);
      left.appendChild(nameNode);
      if (member.isCurrentUser) {
        left.appendChild(el('span', 'mhs-drawer-self-tag', '(Anda)'));
      }
      li.append(left, el('span', 'mhs-drawer-member-npm', member.npm));
      drawerMemberList.appendChild(li);
    });

    const hasCatatan = Boolean(item.catatan && item.catatan.trim());
    drawerCatatanBox.classList.toggle('hidden', !hasCatatan);
    drawerCatatanText.textContent = hasCatatan ? item.catatan : '';
  }

  function openDrawer(item, triggerEl) {
    drawerOpener = triggerEl || document.activeElement;
    populateDrawer(item);
    drawerOverlay.classList.add('active');
    drawerOverlay.setAttribute('aria-hidden', 'false');
    document.body.classList.add('mhs-drawer-open');
    closeDrawerButton.focus();
  }

  function closeDrawer() {
    if (!drawerOverlay.classList.contains('active')) return;
    drawerOverlay.classList.remove('active');
    drawerOverlay.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('mhs-drawer-open');
    if (drawerOpener) drawerOpener.focus();
  }

  function trapDrawerFocus(event) {
    const focusable = Array.from(drawerPanel.querySelectorAll('button:not(:disabled), a[href]'));
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

  // ---------------------------------------------------------------------
  // Data loading
  // ---------------------------------------------------------------------
  async function load() {
    portal.clearBanner(banner);
    renderLoading();
    try {
      const data = await API.get('/api/mahasiswa/jadwal');
      if (!data) return;
      portal.showIdentity(data);
      renderSchedulePage(data);
    } catch (err) {
      portal.clear(contentRoot);
      portal.showSemester('Semester tidak tersedia');
      portal.showBanner(banner, 'danger', portal.errorMessage(err), { label: 'Muat Ulang', onClick: load });
    }
  }

  closeDrawerButton.addEventListener('click', closeDrawer);
  drawerOverlay.addEventListener('click', function (event) {
    if (event.target === drawerOverlay) closeDrawer();
  });
  document.addEventListener('keydown', function (event) {
    if (!drawerOverlay.classList.contains('active')) return;
    if (event.key === 'Escape') closeDrawer();
    if (event.key === 'Tab') trapDrawerFocus(event);
  });

  load();
})();
