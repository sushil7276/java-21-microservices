import { createSlice } from '@reduxjs/toolkit';

const initialTheme = () => {
  try {
    const t = localStorage.getItem('theme');
    if (t) return t;
  } catch { /* ignore */ }
  return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

const slice = createSlice({
  name: 'ui',
  initialState: { theme: initialTheme() },
  reducers: {
    toggleTheme(state) {
      state.theme = state.theme === 'dark' ? 'light' : 'dark';
      document.documentElement.classList.toggle('dark', state.theme === 'dark');
      try { localStorage.setItem('theme', state.theme); } catch { /* ignore */ }
    },
  },
});

export const { toggleTheme } = slice.actions;
export default slice.reducer;
