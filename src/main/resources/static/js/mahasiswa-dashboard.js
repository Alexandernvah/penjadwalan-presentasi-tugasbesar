// mahasiswa-dashboard.js - Dashboard Mahasiswa: course input status and nearest presentation
(function () {
  'use strict';

  const portal = window.MahasiswaPortal;
  const el = portal.el;
  const root = document.getElementById('dashboard-content');
  const banner = document.getElementById('page-banner');

  function iconHeading(iconName, headingId, title) {
    const heading = el('div', 'mhs-card-heading');
    const iconNode = portal.icon(iconName);
    iconNode.classList.add('dosen-section-icon');
    const titleNode = el('h2', 'dosen-courses-card-title', title);
    titleNode.id = headingId;
    heading.append(iconNode, titleNode);
    return heading;
  }

  function buildCard(headingId) {
    const card = el('section', 'mhs-card');
    card.setAttribute('aria-labelledby', headingId);
    return card;
  }

  function buildPendingCard() {
    const card = buildCard('heading-status-matkul');
    const header = el('div', 'mhs-card-header');
    header.append(
      iconHeading('info', 'heading-status-matkul', 'Status Mata Kuliah'),
      el('span', 'mhs-status mhs-status-pending', 'Belum Diinput')
    );

    const callout = el('div', 'mhs-callout');
    const warning = el('span', 'mhs-callout-icon');
    warning.appendChild(portal.icon('warning'));
    const cta = el('a', 'mhs-btn-cta', 'Input Mata Kuliah Sekarang');
    cta.href = 'input-matakuliah.html';
    callout.append(warning, el('p', 'mhs-callout-text', 'Anda belum melakukan input mata kuliah'), cta);

    card.append(header, callout);
    return card;
  }

  function buildCourseRow(course) {
    const row = el('div', 'dosen-course-row');
    const info = el('div', 'dosen-course-info');
    info.append(
      el('strong', 'dosen-course-code', course.kodeMatkul),
      el('span', 'dosen-course-separator', '-'),
      el('span', 'dosen-course-name', course.namaMatkul)
    );
    row.append(info, el('span', 'mhs-class-badge', 'Kelas ' + course.kodeKelas));
    return row;
  }

  function buildEnrolledCard(courses) {
    const card = buildCard('heading-status-matkul');
    const done = el('span', 'mhs-status mhs-status-done');
    done.append(portal.icon('checkCircle'), document.createTextNode('Sudah Diinput'));
    const header = el('div', 'mhs-card-header');
    header.append(iconHeading('info', 'heading-status-matkul', 'Status Mata Kuliah'), done);

    const list = el('div', 'dosen-course-list');
    courses.forEach(function (course) { list.appendChild(buildCourseRow(course)); });

    const lockInfo = el('div', 'mhs-info-bar');
    lockInfo.append(portal.icon('info'), el('span', null, portal.LOCK_MESSAGE));

    card.append(header, list, lockInfo);
    return card;
  }

  function buildMetaItem(iconName, text) {
    const item = el('li', 'mhs-meta-item');
    item.append(portal.icon(iconName), el('span', null, text));
    return item;
  }

  function buildScheduleDetail(schedule) {
    const box = el('div', 'mhs-schedule-card');
    const head = el('div', 'mhs-schedule-head');
    const badgeClass = schedule.status === 'Akan Datang' ? 'mhs-status-badge-upcoming' : 'mhs-status-badge-finished';
    head.append(
      el('strong', 'mhs-schedule-title', schedule.kodeMatkul + ' - ' + schedule.namaMatkul),
      el('span', 'mhs-status-badge ' + badgeClass, schedule.status)
    );

    const meta = el('ul', 'mhs-meta-list');
    meta.append(
      buildMetaItem('calendar', portal.formatTanggalPanjang(schedule.tanggal) + ' (' + portal.formatRentangJam(schedule.jamMulai, schedule.jamSelesai) + ')'),
      buildMetaItem('mapPin', schedule.ruangan),
      buildMetaItem('users', 'Kelompok ' + schedule.noKelompok)
    );
    box.append(head, meta);
    return box;
  }

  function buildScheduleCard(schedule) {
    const card = buildCard('heading-jadwal-terdekat');
    const header = el('div', 'mhs-card-header');
    header.appendChild(iconHeading('calendar', 'heading-jadwal-terdekat', 'Jadwal Presentasi Terdekat'));
    card.appendChild(header);

    card.appendChild(schedule
      ? buildScheduleDetail(schedule)
      : el('p', 'mhs-empty', 'Jadwal presentasi belum ditetapkan. Cek kembali setelah dosen menjadwalkan presentasi kelompok Anda.'));

    const footer = el('div', 'mhs-card-footer');
    const link = el('a', 'mhs-btn-outline');
    link.href = 'lihat-jadwal.html';
    link.append(document.createTextNode('Lihat Semua Jadwal'), portal.icon('arrowRight'));
    footer.appendChild(link);
    card.appendChild(footer);
    return card;
  }

  function render(dashboard) {
    portal.clear(root);
    if (!dashboard.sudahInputMatkul) {
      root.appendChild(buildPendingCard());
      return;
    }
    root.append(buildEnrolledCard(dashboard.daftarMatkulDiambil), buildScheduleCard(dashboard.jadwalPresentasiTerdekat));
  }

  async function load() {
    portal.clearBanner(banner);
    portal.clear(root);
    root.appendChild(el('p', 'mhs-loading', 'Memuat dashboard...'));
    try {
      const dashboard = await API.get('/api/mahasiswa/dashboard');
      if (!dashboard) return;
      portal.showIdentity(dashboard);
      render(dashboard);
    } catch (err) {
      portal.clear(root);
      portal.showBanner(banner, 'danger', portal.errorMessage(err), { label: 'Muat Ulang', onClick: load });
    }
  }

  load();
})();
