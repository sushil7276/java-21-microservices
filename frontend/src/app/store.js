import { configureStore } from '@reduxjs/toolkit';
import { api } from '../services/api';
import auth from '../features/auth/authSlice';
import ui from '../features/ui/uiSlice';

export const store = configureStore({
  reducer: {
    [api.reducerPath]: api.reducer,
    auth,
    ui,
  },
  middleware: (getDefault) => getDefault().concat(api.middleware),
});
