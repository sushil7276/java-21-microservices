import { createSlice } from '@reduxjs/toolkit';

const KEY = 'novashop.auth';

const load = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY));
    return saved?.token ? saved : { token: null, user: null };
  } catch {
    return { token: null, user: null };
  }
};

const persist = (state) => {
  try {
    if (state.token) localStorage.setItem(KEY, JSON.stringify({ token: state.token, user: state.user }));
    else localStorage.removeItem(KEY);
  } catch { /* storage unavailable: session only */ }
};

const slice = createSlice({
  name: 'auth',
  initialState: load(),
  reducers: {
    setCredentials(state, { payload }) {
      state.token = payload.token;
      state.user = payload.user;
      persist(state);
    },
    setUser(state, { payload }) {
      state.user = payload;
      persist(state);
    },
    logout(state) {
      state.token = null;
      state.user = null;
      persist(state);
    },
  },
});

export const { setCredentials, setUser, logout } = slice.actions;
export const selectToken = (s) => s.auth.token;
export const selectUser = (s) => s.auth.user;
export const selectIsAuthed = (s) => Boolean(s.auth.token);
export const selectIsAdmin = (s) => s.auth.user?.role === 'ADMIN';
export default slice.reducer;
