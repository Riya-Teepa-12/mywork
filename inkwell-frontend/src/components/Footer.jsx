import { Link } from "react-router-dom";

function Footer() {
  return (
    <footer className="mt-12 border-t border-[var(--line)] py-10">
      <div className="container-shell grid gap-8 md:grid-cols-4">
        <div>
          <h3 className="hero-title text-sm tracking-[0.14em]">INKWELL</h3>
          <p className="mt-3 text-sm text-[var(--muted)]">
            Write. Publish. Connect. Inspire.
          </p>
        </div>
        <div className="space-y-2 text-sm text-[var(--muted)]">
          <p className="font-semibold text-[var(--text)]">Platform</p>
          <Link to="/feed" className="block hover:text-[var(--text)]">
            Feed
          </Link>
          <Link to="/author" className="block hover:text-[var(--text)]">
            Author Studio
          </Link>
          <Link to="/admin" className="block hover:text-[var(--text)]">
            Admin Panel
          </Link>
        </div>
        <div className="space-y-2 text-sm text-[var(--muted)]">
          <p className="font-semibold text-[var(--text)]">Account</p>
          <Link to="/login" className="block hover:text-[var(--text)]">
            Login
          </Link>
          <Link to="/signup" className="block hover:text-[var(--text)]">
            Signup
          </Link>
          <Link to="/profile" className="block hover:text-[var(--text)]">
            Profile
          </Link>
        </div>
        <div className="space-y-2 text-sm text-[var(--muted)]">
          <p className="font-semibold text-[var(--text)]">Engagement</p>
          <Link to="/notifications" className="block hover:text-[var(--text)]">
            Notifications
          </Link>
          <Link to="/newsletter" className="block hover:text-[var(--text)]">
            Newsletter
          </Link>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
