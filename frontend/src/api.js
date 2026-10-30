const TOKEN_KEY = 'library.token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

async function request(method, path, body) {
  const headers = { Accept: 'application/json' };
  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  if (res.status === 401 && path !== '/api/auth/login') onUnauthorized();
  if (res.status === 204) return null;

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const fieldErrors = data?.errors?.map((e) => `${e.field}: ${e.defaultMessage}`).join(', ');
    throw new Error(fieldErrors || data?.message || `İstek başarısız (${res.status})`);
  }
  return data;
}

const qs = (params) => {
  const p = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '' || v === false) return;
    (Array.isArray(v) ? v : [v]).forEach((x) => p.append(k, x));
  });
  const s = p.toString();
  return s ? `?${s}` : '';
};

export const api = {
  login: (email, password) => request('POST', '/api/auth/login', { email, password }),
  me: () => request('GET', '/api/auth/me'),
  staff: () => request('GET', '/api/auth/users'),
  createStaff: (u) => request('POST', '/api/auth/users', u),
  setStaffActive: (id, value) => request('PATCH', `/api/auth/users/${id}/active?value=${value}`),

  members: (q) => request('GET', `/api/members${qs({ q })}`),
  member: (id) => request('GET', `/api/members/${id}`),
  createMember: (m) => request('POST', '/api/members', m),
  updateMember: (id, m) => request('PUT', `/api/members/${id}`, m),

  books: (params) => request('GET', `/api/books${qs(params)}`),
  book: (id) => request('GET', `/api/books/${id}`),
  categories: () => request('GET', '/api/books/categories'),
  createBook: (b) => request('POST', '/api/books', b),
  updateBook: (id, b) => request('PUT', `/api/books/${id}`, b),
  deleteBook: (id) => request('DELETE', `/api/books/${id}`),

  authors: (q) => request('GET', `/api/authors${qs({ q })}`),
  author: (id) => request('GET', `/api/authors/${id}`),
  createAuthor: (a) => request('POST', '/api/authors', a),
  updateAuthor: (id, a) => request('PUT', `/api/authors/${id}`, a),

  publishers: () => request('GET', '/api/publishers'),
  createPublisher: (p) => request('POST', '/api/publishers', p),

  loans: (params) => request('GET', `/api/loans${qs(params)}`),
  pendingLoans: () => request('GET', '/api/loans/pending'),
  activeLoans: (memberId) => request('GET', `/api/loans/active${qs({ memberId })}`),
  overdueLoans: () => request('GET', '/api/loans/overdue'),
  bookHolders: (bookId) => request('GET', `/api/loans/books/${bookId}/active`),
  memberStats: (memberId) => request('GET', `/api/loans/members/${memberId}/stats`),
  requestLoan: (l) => request('POST', '/api/loans', l),
  approveLoan: (id) => request('POST', `/api/loans/${id}/approve`),
  rejectLoan: (id, reason) => request('POST', `/api/loans/${id}/reject`, { reason }),
  requestReturn: (id) => request('POST', `/api/loans/${id}/return-request`),
  approveReturn: (id) => request('POST', `/api/loans/${id}/return-approve`),
  cancelReturn: (id) => request('POST', `/api/loans/${id}/return-cancel`),
  extendLoan: (id, days) => request('POST', `/api/loans/${id}/extend?days=${days}`),

  notifications: (params) => request('GET', `/api/notifications${qs(params)}`),
  sendReminder: (loanId) => request('POST', `/api/notifications/loans/${loanId}/reminder`),
  runNotifications: () => request('POST', '/api/notifications/run'),
};
