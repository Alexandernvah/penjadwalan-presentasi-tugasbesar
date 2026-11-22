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

  function getLoginAlertContainer() {
    return document.getElementById('login-alert-container');
  }

  function isLoginAlertVisible() {
    const container = getLoginAlertContainer();
    return Boolean(container && !container.classList.contains('hidden') && container.textContent.trim());
  }

  function setLoginAlertRevalidating(revalidating) {
    const container = getLoginAlertContainer();
    if (!container || container.classList.contains('hidden')) return;
    container.classList.toggle('is-revalidating', Boolean(revalidating));
  }

  function showLoginError(message) {
    const container = getLoginAlertContainer();
    if (!container) return;
    container.classList.remove('hidden', 'is-revalidating');
    container.classList.add('alert', 'alert-danger', 'mb-3');
    container.setAttribute('role', 'alert');
    if (container.textContent !== message) {
      container.textContent = message;
    }
  }

  function hideLoginError() {
    const container = getLoginAlertContainer();
    if (!container) return;
    container.className = 'hidden';
    container.removeAttribute('role');
    container.textContent = '';
  }

  function wireLoginForm(config) {
    const form = document.getElementById(config.formId);
    if (!form) return;

    const identifierInput = document.getElementById(config.identifierId);
    const passwordInput = document.getElementById(config.passwordId);
    const submitBtn = document.getElementById(config.buttonId);
    const defaultButtonLabel = (submitBtn && submitBtn.textContent.trim()) ? submitBtn.textContent.trim() : 'Masuk';
    let isSubmitting = false;

    function resetSubmitButton() {
      isSubmitting = false;
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = defaultButtonLabel;
      }
    }

    function clearAlertIfInputsEmpty() {
      if (isSubmitting || !isLoginAlertVisible()) return;
      const hasIdentifier = identifierInput ? identifierInput.value.trim().length > 0 : false;
      const hasPassword = passwordInput ? passwordInput.value.length > 0 : false;
      if (!hasIdentifier && !hasPassword) {
        hideLoginError();
      }
    }

    if (identifierInput) {
      identifierInput.addEventListener('input', clearAlertIfInputsEmpty);
    }
    if (passwordInput) {
      passwordInput.addEventListener('input', clearAlertIfInputsEmpty);
    }

    window.addEventListener('pageshow', function (event) {
      if (event.persisted) {
        resetSubmitButton();
      }
    });

    form.addEventListener('submit', async function (e) {
      e.preventDefault();
      if (isSubmitting) {
        return;
      }

      const email = identifierInput ? identifierInput.value.trim() : '';
      const password = passwordInput ? passwordInput.value : '';

      if (!email || !password) {
        setLoginAlertRevalidating(false);
        showLoginError('Harap lengkapi identitas dan password Anda.');
        return;
      }

      isSubmitting = true;
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Memproses...';
      }

      if (isLoginAlertVisible()) {
        setLoginAlertRevalidating(true);
      }

      let keepLockedForNavigation = false;

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

        keepLockedForNavigation = true;
        hideLoginError();

        if (typeof API !== 'undefined' && API.setAuth) {
          API.setAuth(data.token, data.role, data);
        }

        window.location.replace(config.redirectUrl);
      } catch (err) {
        showLoginError('Tidak dapat terhubung ke server. Coba beberapa saat lagi.');
      } finally {
        if (!keepLockedForNavigation) {
          resetSubmitButton();
        }
      }
    });
  }

  function wirePasswordToggles() {
    const toggleButtons = document.querySelectorAll('.password-toggle-btn');
    toggleButtons.forEach(function (btn) {
      const group = btn.closest('.input-group');
      const input = group ? group.querySelector('input[name="password"]') : null;
      const iconEye = btn.querySelector('.icon-eye');
      const iconEyeOff = btn.querySelector('.icon-eye-off');
      if (!input) return;

      function syncVisibilityState() {
        const isVisible = input.type === 'text';
        btn.setAttribute('aria-pressed', String(isVisible));
        btn.setAttribute('aria-label', isVisible ? 'Sembunyikan password' : 'Tampilkan password');
        if (iconEye) {
          iconEye.classList.toggle('is-hidden', isVisible);
        }
        if (iconEyeOff) {
          iconEyeOff.classList.toggle('is-hidden', !isVisible);
        }
      }

      btn.addEventListener('click', function () {
        input.type = input.type === 'password' ? 'text' : 'password';
        syncVisibilityState();
      });

      syncVisibilityState();
    });
  }

  function init() {
    redirectIfAlreadyAuthenticated();
    wirePasswordToggles();

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
