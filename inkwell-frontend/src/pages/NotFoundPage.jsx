import { Link } from "react-router-dom";

function NotFoundPage() {
  return (
    <div className="container-shell flex min-h-[65vh] items-center justify-center pb-16">
      <div className="glass edge-glow rounded-4xl p-8 text-center md:p-12">
        <p className="hero-title text-5xl">404</p>
        <h1 className="mt-3 text-2xl font-semibold">Page not found</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          The route you requested does not exist in this build.
        </p>
        <Link to="/" className="btn-primary mt-6 inline-flex rounded-full px-5 py-2.5 text-sm">
          Back Home
        </Link>
      </div>
    </div>
  );
}

export default NotFoundPage;
