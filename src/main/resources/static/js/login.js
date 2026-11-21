// login.js - Portal authentication handler for Dosen, Admin, and Mahasiswa login pages
(function () {
  'use strict';

  function roleDashboardPath(role) {
    if (role === 'DOSEN') return '/dosen/dashboard.html';
    if (role === 'ADMIN') return '/admin/dashboard.html';
    if (role === 'MAHASISWA') return '/mahasiswa/dashboard.html';
    return null;
  }

  async function redirectIfAlreadyAuthenticated() {
    try {
      const res = await fetch('/api/auth/me', {
        method: 'GET',
        credentials: 'include',
        cache: 'no-store',
        headers: { 'Accept': 'application/json' }
      });
      if (!res.ok) return;
      const data = await res.json();
      if (data && data.authenticated && data.role) {
        const target = roleDashboardPath(data.role);
        if (target) {
          window.location.replace(target);
        }
      }
    } catch (err) {
      // Remain on login page if session check fails
    }
  }

  function showLoginError(message) {
    const container = document.getElementById('login-alert-container');
    if (!container) return;
    container.className = 'alert alert-danger mb-3';
    container.setAttribute('role', 'alert');
    container.textContent = message;
  }

  function hideLoginError() {
    const container = document.getElementById('login-alert-container');
    if (!container) return;
    container.className = 'hidden';
    container.textContent = '';
  }

  function wireLoginForm(config) {
    const form = document.getElementById(config.formId);
    if (!form) return;

    const identifierInput = document.getElementById(config.identifierId);
    const passwordInput = document.getElementById(config.passwordId);
    const submitBtn = document.getElementById(config.buttonId);

    form.addEventListener('submit', async function (e) {
      e.preventDefault();
      hideLoginError();

      const email = identifierInput ? identifierInput.value.trim() : '';
      const password = passwordInput ? passwordInput.value : '';

      if (!email || !password) {
        showLoginError('Harap lengkapi identitas dan password Anda.');
        return;
      }

      if (submitBtn) submitBtn.disabled = true;

      try {
        const response = await fetch('/api/auth/login', {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify({
            email: email,
            password: password,
            expectedRole: config.expectedRole
          })
        });

        let data = null;
        try {
          data = await response.json();
        } catch (parseErr) {
          data = null;
        }

        if (!response.ok || !data) {
          const msg = (data && data.message)
            ? data.message
            : 'Login gagal. Periksa kembali kredensial Anda.';
          showLoginError(msg);
          return;
        }

        if (typeof API !== 'undefined' && API.setAuth) {
          API.setAuth(data.token, data.role, data);
        }

        window.location.replace(config.redirectUrl);
      } catch (err) {
        showLoginError('Tidak dapat terhubung ke server. Coba beberapa saat lagi.');
      } finally {
        if (submitBtn) submitBtn.disabled = false;
      }
    });
  }

  function init() {
    redirectIfAlreadyAuthenticated();

    wireLoginForm({
      formId: 'dosen-login-form',
      identifierId: 'dosen-identifier',
      passwordId: 'dosen-password',
      buttonId: 'btn-dosen-login',
      expectedRole: 'DOSEN',
      redirectUrl: 'dashboard.html'
    });

    wireLoginForm({
      formId: 'admin-login-form',
      identifierId: 'admin-email',
      passwordId: 'admin-password',
      buttonId: 'btn-admin-login',
      expectedRole: 'ADMIN',
      redirectUrl: 'dashboard.html'
    });

    wireLoginForm({
      formId: 'mahasiswa-login-form',
      identifierId: 'mahasiswa-identifier',
      passwordId: 'mahasiswa-password',
      buttonId: 'btn-mahasiswa-login',
      expectedRole: 'MAHASISWA',
      redirectUrl: 'dashboard.html'
    });
  }

  window.addEventListener('pageshow', function () {
    redirectIfAlreadyAuthenticated();
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
