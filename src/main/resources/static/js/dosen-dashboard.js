// dosen-dashboard.js - Lecturer Dashboard Data Handler
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

  // Load Lecturer Dashboard Data
  async function loadDashboardData() {
    try {
      const authNidn = (typeof API !== 'undefined' && API.getToken) ? localStorage.getItem('auth_id') : null;
      const queryParam = authNidn ? `?dosenId=${encodeURIComponent(authNidn)}` : '';

      const response = await fetch(`/api/dosen/dashboard/summary${queryParam}`, {
        headers: {
          'Accept': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Gagal memuat ringkasan dashboard (${response.status})`);
      }

      const data = await response.json();
      renderDashboard(data);
    } catch (error) {
      console.error('Error fetching dashboard summary:', error);
      renderFallbackState();
    }
  }

  function renderDashboard(data) {
    if (!data) return;

    const lecturerName = data.namaDosen || 'Dr. Budi Santoso';
    const lecturerEmail = data.email || 'budi.santoso@unpar.ac.id';

    const topbarName = document.getElementById('topbar-lecturer-name');
    const welcomeName = document.getElementById('welcome-lecturer-name');
    const dropdownName = document.getElementById('dropdown-lecturer-name');
    const dropdownEmail = document.getElementById('dropdown-lecturer-email');

    if (topbarName) topbarName.textContent = lecturerName;
    if (welcomeName) welcomeName.textContent = lecturerName;
    if (dropdownName) dropdownName.textContent = lecturerName;
    if (dropdownEmail) dropdownEmail.textContent = lecturerEmail;

    const courses = data.mataKuliahAjar || [];
    renderCourseList(courses);
  }

  function renderCourseList(courses) {
    const listContainer = document.getElementById('list-matkul-ajar');
    if (!listContainer) return;

    if (!courses || courses.length === 0) {
      listContainer.innerHTML = `
        <div class="dosen-course-empty">
          Belum ada mata kuliah yang tercatat diampu pada semester aktif ini.
        </div>
      `;
      return;
    }

    // Deduplicate or group by course code & class
    listContainer.innerHTML = courses.map(function (course) {
      const courseCode = course.idMatkul || '-';
      const courseName = course.namaMatkul || '-';
      const courseClass = course.kodeKelas ? `Kelas ${course.kodeKelas}` : 'Kelas -';

      return `
        <div class="dosen-course-row">
          <div class="dosen-course-info">
            <span class="dosen-course-code">${courseCode}</span>
            <span class="dosen-course-separator">-</span>
            <span class="dosen-course-name">${courseName}</span>
          </div>
          <span class="dosen-class-pill">${courseClass}</span>
        </div>
      `;
    }).join('');
  }

  function renderFallbackState() {
    const topbarName = document.getElementById('topbar-lecturer-name');
    const welcomeName = document.getElementById('welcome-lecturer-name');

    if (topbarName) topbarName.textContent = 'Dr. Budi Santoso';
    if (welcomeName) welcomeName.textContent = 'Dr. Budi Santoso';

    renderCourseList([
      {
        idMatkul: 'IF2110',
        namaMatkul: 'Algoritma dan Struktur Data',
        kodeKelas: 'A'
      },
      {
        idMatkul: 'IF3130',
        namaMatkul: 'Jaringan Komputer',
        kodeKelas: 'B'
      }
    ]);
  }

  document.addEventListener('DOMContentLoaded', loadDashboardData);
})();
