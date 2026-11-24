import { useSelector } from 'react-redux';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { selectIsAdmin, selectIsAuthed } from '../features/auth/authSlice';

/**
 * Route guard.
 *  - default: any signed-in user (an admin can open every page)
 *  - admin:   admin-only pages (customers are sent to the shop)
 */
export default function ProtectedRoute({ admin = false }) {
  const authed = useSelector(selectIsAuthed);
  const isAdmin = useSelector(selectIsAdmin);
  const location = useLocation();
  if (!authed) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  if (admin && !isAdmin) return <Navigate to="/" replace />;
  return <Outlet />;
}
