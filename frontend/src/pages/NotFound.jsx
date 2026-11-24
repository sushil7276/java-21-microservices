import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="container-page grid min-h-[60vh] place-items-center py-16 text-center">
      <div>
        <p className="text-gradient text-8xl font-extrabold">404</p>
        <h1 className="mt-4 text-2xl font-bold">This page took a wrong turn</h1>
        <p className="mt-2 text-slate-500">The page you're looking for doesn't exist.</p>
        <Link to="/" className="btn-primary mt-8">Back to home</Link>
      </div>
    </div>
  );
}
