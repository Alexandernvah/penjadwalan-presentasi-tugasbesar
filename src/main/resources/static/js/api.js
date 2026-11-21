/**
 * Centralized API & Authentication Client
 * Tubes Scheduler System
 */
const API = (() => {
  const BASE_URL = '/api/v1';

  const getToken = () => localStorage.getItem('jwt_token');
  const getRole = () => localStorage.getItem('user_role');

  const setAuth = (token, role, userDetails = {}) => {
    if (token !== undefined && token !== null) {
      localStorage.setItem('jwt_token', token);
    }
    if (role !== undefined && role !== null) {
      localStorage.setItem('user_role', role);
    }
    localStorage.setItem('user_profile', JSON.stringify(userDetails || {}));
    const authId = (userDetails && (userDetails.nidn || userDetails.id)) || '';
    if (authId) {
      localStorage.setItem('auth_id', String(authId));
    }
  };

  const clearAuth = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_role');
    localStorage.removeItem('user_profile');
    localStorage.removeItem('auth_id');
  };

  const logout = async (redirectUrl = 'index.html') => {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      });
    } catch (err) {
      console.warn('Logout request error:', err);
    } finally {
      clearAuth();
      window.location.replace(redirectUrl);
    }
  };

  const request = async (endpoint, options = {}) => {
    const token = getToken();
    const headers = { ...options.headers };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    const config = {
      credentials: 'include',
      ...options,
      headers
    };

    const url = endpoint.startsWith('/api/') ? endpoint : `${BASE_URL}${endpoint}`;

    try {
      const response = await fetch(url, config);

      if (response.status === 401 || response.status === 403) {
        clearAuth();
        const currentPath = window.location.pathname;
        if (currentPath.includes('/admin')) window.location.replace('/admin/index.html');
        else if (currentPath.includes('/dosen')) window.location.replace('/dosen/index.html');
        else window.location.replace('/mahasiswa/index.html');
        return null;
      }

      const contentType = response.headers.get('content-type');
      let data = null;
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      if (!response.ok) {
        const errorMsg = (data && data.message) ? data.message : 'API request execution failure';
        throw new Error(errorMsg);
      }
      return data;
    } catch (err) {
      console.error(`Fetch Error [${endpoint}]:`, err);
      throw err;
    }
  };

  return {
    getToken,
    getRole,
    setAuth,
    clearAuth,
    logout,
    get: (url) => request(url, { method: 'GET' }),
    post: (url, body) => request(url, {
      method: 'POST',
      body: body instanceof FormData ? body : JSON.stringify(body)
    }),
    put: (url, body) => request(url, {
      method: 'PUT',
      body: JSON.stringify(body)
    }),
    delete: (url) => request(url, { method: 'DELETE' })
  };
})();
