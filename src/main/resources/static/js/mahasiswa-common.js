// mahasiswa-common.js - sidebar, top bar, profile menu and helpers shared by every Mahasiswa page.
// Shell markup reuses the .dosen-* classes so the Mahasiswa portal matches the Dosen portal.
(function () {
  'use strict';

  const LOCK_MESSAGE = 'Mata kuliah telah dikunci. Hubungi Admin Program Studi jika terdapat kesalahan pengisian data.';
  const NAMA_HARI = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
  const NAMA_BULAN = ['Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni', 'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'];

  const SVG_OPEN = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">';
  const ICON_PATHS = {
    grid: '<rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>',
    book: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>',
    users: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path>',
    calendar: '<rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line>',
    user: '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle>',
    chevronDown: '<polyline points="6 9 12 15 18 9"></polyline>',
    logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line>',
    info: '<circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line>',
    warning: '<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line>',
    checkCircle: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline>',
    check: '<polyline points="20 6 9 17 4 12"></polyline>',
    arrowRight: '<line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline>',
    clock: '<circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline>',
    mapPin: '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle>',
    lock: '<rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path>',
    close: '<line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>'
  };

  const NAV_ITEMS = [
    { key: 'dashboard', href: 'dashboard.html', label: 'Dashboard', icon: 'grid' },
    { key: 'input-matakuliah', href: 'input-matakuliah.html', label: 'Input Matakuliah', icon: 'book' },
    { key: 'pendaftaran-kelompok', href: 'pendaftaran-kelompok.html', label: 'Pendaftaran Kelompok', icon: 'users' },
    { key: 'lihat-jadwal', href: 'lihat-jadwal.html', label: 'Lihat Jadwal', icon: 'calendar' }
  ];

  // ---------------------------------------------------------------------
  // DOM helpers (all dynamic text goes through textContent)
  // ---------------------------------------------------------------------
  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  }

  function icon(name) {
    const holder = document.createElement('template');
    holder.innerHTML = SVG_OPEN + ICON_PATHS[name] + '</svg>';
    return holder.content.firstElementChild;
  }

  function clear(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  // ---------------------------------------------------------------------
  // Formatting
  // ---------------------------------------------------------------------
  // "2026-10-15" -> "Kamis, 15 Oktober 2026"
  function formatTanggalPanjang(isoDate) {
    const parts = String(isoDate || '').split('-').map(Number);
    if (parts.length !== 3 || parts.some(isNaN)) return '-';
    const date = new Date(parts[0], parts[1] - 1, parts[2]);
    return NAMA_HARI[date.getDay()] + ', ' + date.getDate() + ' ' + NAMA_BULAN[date.getMonth()] + ' ' + date.getFullYear();
  }

  // "11:00:00", "11:30:00" -> "11:00 - 11:30 WIB"
  function formatRentangJam(jamMulai, jamSelesai) {
    return String(jamMulai).slice(0, 5) + ' - ' + String(jamSelesai).slice(0, 5) + ' WIB';
  }

  function errorMessage(err) {
    if (err instanceof TypeError) return 'Tidak dapat terhubung ke server. Periksa koneksi Anda lalu coba lagi.';
    return (err && err.message) || 'Terjadi kesalahan. Silakan coba lagi.';
  }

  // ---------------------------------------------------------------------
  // Feedback banner: type is "danger", "success", "info" or "warning"
  // ---------------------------------------------------------------------
  function clearBanner(container) {
    clear(container);
  }

  function showBanner(container, type, message, action) {
    clear(container);
    const banner = el('div', 'alert alert-' + type);
    banner.setAttribute('role', type === 'danger' ? 'alert' : 'status');

    const iconHolder = el('span', 'alert-icon');
    iconHolder.appendChild(icon(type === 'success' ? 'checkCircle' : type === 'info' ? 'info' : 'warning'));
    banner.appendChild(iconHolder);
    banner.appendChild(el('div', 'flex-1', message));

    if (action) {
      const actionButton = el('button', 'mhs-banner-action', action.label);
      actionButton.type = 'button';
      actionButton.addEventListener('click', action.onClick);
      banner.appendChild(actionButton);
    }
    container.appendChild(banner);
  }

  // ---------------------------------------------------------------------
  // Shell: skip link, sidebar, top bar with profile menu
  // ---------------------------------------------------------------------
  function buildBrand() {
    const header = el('div', 'dosen-sidebar-header');
    const badge = el('div', 'dosen-brand-badge');
    [['../images/logo-unpar.png', 'Logo UNPAR'], ['../images/LogoInformatika2.jpg', 'Logo Informatika UNPAR']].forEach(function (logo) {
      const img = el('img', 'dosen-brand-logo-img');
      img.src = logo[0];
      img.alt = logo[1];
      img.width = 40;
      img.height = 40;
      badge.appendChild(img);
    });
    header.appendChild(badge);
    return header;
  }

  function buildSidebar(activeKey) {
    const sidebar = el('aside', 'dosen-sidebar');
    sidebar.setAttribute('aria-label', 'Sidebar Navigasi Mahasiswa');
    sidebar.appendChild(buildBrand());

    const nav = el('nav');
    nav.setAttribute('aria-label', 'Navigasi Utama');
    const list = el('ul', 'dosen-nav-list');
    NAV_ITEMS.forEach(function (item) {
      const link = el('a', 'dosen-nav-link' + (item.key === activeKey ? ' active' : ''));
      link.href = item.href;
      if (item.key === activeKey) link.setAttribute('aria-current', 'page');
      const iconHolder = el('span', 'dosen-nav-icon');
      iconHolder.setAttribute('aria-hidden', 'true');
      iconHolder.appendChild(icon(item.icon));
      link.appendChild(iconHolder);
      link.appendChild(el('span', null, item.label));
      const row = el('li', 'dosen-nav-item');
      row.appendChild(link);
      list.appendChild(row);
    });
    nav.appendChild(list);
    sidebar.appendChild(nav);
    return sidebar;
  }

  function buildTopbar() {
    const topbar = el('header', 'dosen-topbar');
    topbar.appendChild(el('div', 'dosen-topbar-spacer'));

    const menu = el('div', 'dosen-profile-menu');
    const button = el('button', 'dosen-profile-btn');
    button.type = 'button';
    button.id = 'btn-mahasiswa-profile-menu';
    button.setAttribute('aria-haspopup', 'true');
    button.setAttribute('aria-expanded', 'false');
    button.setAttribute('aria-controls', 'mahasiswa-dropdown-panel');
    const userIcon = icon('user');
    userIcon.classList.add('dosen-profile-icon');
    const nameLabel = el('span', null, 'Memuat...');
    nameLabel.id = 'current-user-name';
    const arrow = icon('chevronDown');
    arrow.classList.add('dosen-dropdown-arrow');
    button.append(userIcon, nameLabel, arrow);

    const panel = el('div', 'dosen-dropdown-menu');
    panel.id = 'mahasiswa-dropdown-panel';
    const panelHeader = el('div', 'dosen-dropdown-header');
    const panelName = el('div', 'dosen-dropdown-name', 'Mahasiswa');
    panelName.id = 'dropdown-user-name';
    const panelDetail = el('div', 'dosen-dropdown-email', 'Mahasiswa');
    panelDetail.id = 'dropdown-user-detail';
    panelHeader.append(panelName, panelDetail);

    const logout = el('button', 'dosen-dropdown-item text-danger');
    logout.type = 'button';
    logout.id = 'btn-student-logout';
    const logoutIcon = icon('logout');
    logoutIcon.classList.add('dosen-menu-svg');
    logout.append(logoutIcon, el('span', null, 'Keluar'));
    logout.addEventListener('click', function () {
      if (typeof API !== 'undefined' && API.logout) {
        API.logout('index.html');
        return;
      }
      window.location.replace('index.html');
    });

    panel.append(panelHeader, logout);
    menu.append(button, panel);
    topbar.appendChild(menu);

    wireProfileMenu(button, panel);
    return topbar;
  }

  function wireProfileMenu(button, panel) {
    function setOpen(open) {
      button.setAttribute('aria-expanded', String(open));
      panel.classList.toggle('show', open);
    }
    button.addEventListener('click', function (event) {
      event.stopPropagation();
      setOpen(button.getAttribute('aria-expanded') !== 'true');
    });
    document.addEventListener('click', function () { setOpen(false); });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && panel.classList.contains('show')) {
        setOpen(false);
        button.focus();
      }
    });
  }

  function buildShell() {
    const main = document.getElementById('main-content');
    const layout = el('div', 'dosen-layout');
    const container = el('div', 'dosen-main-container');
    container.appendChild(buildTopbar());
    container.appendChild(main);
    layout.append(buildSidebar(document.body.dataset.activeNav), container);

    const skipLink = el('a', 'mhs-skip-link', 'Lewati ke konten utama');
    skipLink.href = '#main-content';
    document.body.prepend(layout);
    document.body.prepend(skipLink);
  }

  // Shows the active academic semester label in the page header badge.
  function showSemester(label) {
    const semesterNode = document.getElementById('mhs-semester-label');
    if (semesterNode) {
      semesterNode.textContent = label || 'Semester Belum Tersedia';
    }
  }

  // Shows the authenticated student in the top bar and profile menu.
  function showIdentity(identity) {
    if (!identity) return;
    if (identity.namaMahasiswa) {
      document.getElementById('current-user-name').textContent = identity.namaMahasiswa;
      document.getElementById('dropdown-user-name').textContent = identity.namaMahasiswa;
    }
    if (identity.npm) {
      document.getElementById('dropdown-user-detail').textContent = 'NPM ' + identity.npm;
    }
    if (identity.semesterAkademikLabel) {
      showSemester(identity.semesterAkademikLabel);
    }
  }

  function hydrateCachedIdentity() {
    try {
      const raw = localStorage.getItem('user_profile');
      if (!raw) return;
      const profile = JSON.parse(raw);
      if (profile && (profile.nama || profile.id)) {
        showIdentity({ namaMahasiswa: profile.nama, npm: profile.id });
      }
    } catch (ignored) {
      // Malformed local cache is overwritten on the next session check.
    }
  }

  async function fetchPortalContext() {
    try {
      const context = await API.get('/api/mahasiswa/context');
      if (context) {
        showIdentity(context);
      }
    } catch (ignored) {
      showSemester('Semester tidak tersedia');
    }
  }

  buildShell();
  hydrateCachedIdentity();
  if (document.body.dataset.activeNav === 'lihat-jadwal') {
    fetchPortalContext();
  }

  window.MahasiswaPortal = {
    LOCK_MESSAGE: LOCK_MESSAGE,
    el: el,
    icon: icon,
    clear: clear,
    formatTanggalPanjang: formatTanggalPanjang,
    formatRentangJam: formatRentangJam,
    errorMessage: errorMessage,
    showBanner: showBanner,
    clearBanner: clearBanner,
    showSemester: showSemester,
    showIdentity: showIdentity
  };
})();
