// auth-guard.js - Role-Based Access Control (RBAC) & bfcache session guard for protected portal pages
(function () {
  'use strict';

  function inferRequiredRole() {
    if (document.body) {
      const attr = document.body.getAttribute('data-required-role');
      if (attr) return attr.trim().toUpperCase();
    }
    const path = window.location.pathname || '';
    if (path.indexOf('/dosen/') !== -1) return 'DOSEN';
    if (path.indexOf('/admin/') !== -1) return 'ADMIN';
    if (path.indexOf('/mahasiswa/') !== -1) return 'MAHASISWA';
    return '';
  }

  function roleDashboardPath(role) {
    if (role === 'DOSEN') return '/dosen/dashboard.html';
    if (role === 'ADMIN') return '/admin/dashboard.html';
    if (role === 'MAHASISWA') return '/mahasiswa/dashboard.html';
    return 'index.html';
  }

  function syncUserDisplay(data) {
    if (!data) return;

    const topbarName = document.getElementById('topbar-lecturer-name');
    const welcomeName = document.getElementById('welcome-lecturer-name');
    const dropdownName = document.getElementById('dropdown-lecturer-name');
    const dropdownEmail = document.getElementById('dropdown-lecturer-email');
    const currentUserName = document.getElementById('current-user-name');

    if (data.nama) {
      if (topbarName) topbarName.textContent = data.nama;
      if (welcomeName) welcomeName.textContent = data.nama;
      if (dropdownName) dropdownName.textContent = data.nama;
      if (currentUserName) currentUserName.textContent = data.nama;
    }
    if (data.email && dropdownEmail) {
      dropdownEmail.textContent = data.email;
    }
  }

  async function verifySession() {
    const requiredRole = inferRequiredRole();
    try {
      const res = await fetch('/api/auth/me', {
        method: 'GET',
        credentials: 'include',
        cache: 'no-store',
        headers: { 'Accept': 'application/json' }
      });

      if (!res.ok) {
        if (typeof API !== 'undefined' && API.clearAuth) {
          API.clearAuth();
        }
        window.location.replace('index.html');
        return;
      }

      const data = await res.json();
      if (!data || !data.authenticated || !data.role) {
        if (typeof API !== 'undefined' && API.clearAuth) {
          API.clearAuth();
        }
        window.location.replace('index.html');
        return;
      }

      if (requiredRole && data.role !== requiredRole) {
        window.location.replace(roleDashboardPath(data.role));
        return;
      }

      if (typeof API !== 'undefined' && API.setAuth) {
        const existingToken = API.getToken ? API.getToken() : '';
        API.setAuth(existingToken || '', data.role, data);
      }

      syncUserDisplay(data);
    } catch (err) {
      if (typeof API !== 'undefined' && API.clearAuth) {
        API.clearAuth();
      }
      window.location.replace('index.html');
    }
  }

  function wirePortalLogoutButtons() {
    ['btn-admin-logout', 'btn-mahasiswa-logout'].forEach(function (id) {
      const btn = document.getElementById(id);
      if (btn) {
        btn.addEventListener('click', function () {
          if (typeof API !== 'undefined' && API.logout) {
            API.logout('index.html');
          } else {
            window.location.replace('index.html');
          }
        });
      }
    });
  }

  verifySession();

  window.addEventListener('pageshow', function () {
    verifySession();
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', wirePortalLogoutButtons);
  } else {
    wirePortalLogoutButtons();
  }
})();
