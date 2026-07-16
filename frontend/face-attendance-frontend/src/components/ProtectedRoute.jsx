import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Wraps a page and redirects the visitor to the right step if they haven't
 * finished the earlier ones yet:
 *   not logged in            -> /login
 *   logged in, no profile    -> /complete-profile
 *   profile done, no face    -> /face-register (only enforced when requireFaceRegistered)
 */
export default function ProtectedRoute({ children, requireProfile = false, requireFaceRegistered = false }) {
  const { user, isAuthenticated, isLoading } = useAuth();

  if (isLoading) return null;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requireProfile && !user.profileCompleted) {
    return <Navigate to="/complete-profile" replace />;
  }

  if (requireFaceRegistered && !user.faceRegistered) {
    return <Navigate to="/face-register" replace />;
  }

  return children;
}
