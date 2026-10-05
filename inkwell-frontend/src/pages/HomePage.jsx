import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import {
  ArrowRight,
  Sparkles,
  ShieldCheck,
  BellRing,
  BarChart3,
} from "lucide-react";
import AnimatedSection from "../components/AnimatedSection.jsx";
import { features } from "../data/mockData.js";
import { postService, taxonomyService } from "../lib/services.js";
import { useAuth } from "../context/AuthContext.jsx";

function HomePage() {
  const { isAuthenticated } = useAuth();
  const [posts, setPosts] = useState([]);
  const [trendingTags, setTrendingTags] = useState([]);
  const [error, setError] = useState("");
  const testimonials = [
    {
      quote: "The author studio feels like a real publishing desk with built-in moderation and analytics.",
      name: "Aarav S.",
      role: "Tech Author",
      image:
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=1000&q=80",
    },
    {
      quote: "Reader engagement jumped as soon as we moved comments and notifications into one place.",
      name: "Nina R.",
      role: "Community Lead",
      image:
        "https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?auto=format&fit=crop&w=1000&q=80",
    },
  ];

  useEffect(() => {
    let active = true;
    Promise.all([postService.getPublished(), taxonomyService.getTrendingTags()])
      .then(([postRows, tags]) => {
        if (!active) {
          return;
        }
        setPosts(Array.isArray(postRows) ? postRows : []);
        setTrendingTags(Array.isArray(tags) ? tags : []);
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err.message || "Failed to load homepage data");
      });
    return () => {
      active = false;
    };
  }, []);

  const topPosts = posts.slice(0, 3);
  const totalViews = posts.reduce((sum, item) => sum + (item.viewCount || 0), 0);
  const totalLikes = posts.reduce((sum, item) => sum + (item.likesCount || 0), 0);
  const featuredCount = posts.filter((item) => item.featured).length;
  const metrics = [
    { label: "Published Posts", value: String(posts.length) },
    { label: "Total Views", value: String(totalViews) },
    { label: "Total Likes", value: String(totalLikes) },
    { label: "Featured Posts", value: String(featuredCount) },
  ];

  return (
    <div className="pb-16">
      <section className="container-shell">
        <div className="relative overflow-hidden rounded-4xl border border-[var(--line)] bg-[var(--surface-2)]/75 p-6 md:p-10 lg:grid-cols-[1.1fr_0.9fr] lg:gap-10 lg:p-12">
          <div className="pointer-events-none absolute -left-24 -top-24 h-56 w-56 rounded-full bg-blue-500/15 blur-3xl dark:bg-cyan-500/15" />
          <div className="pointer-events-none absolute -bottom-28 right-0 h-64 w-64 rounded-full bg-fuchsia-500/15 blur-3xl" />
          <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="space-y-6">
            <span className="chip inline-flex rounded-full px-4 py-2 text-xs uppercase tracking-[0.1em]">
              AI-Powered Blogging Platform
            </span>
            <h1 className="hero-title text-3xl leading-tight md:text-5xl">
              Write smarter. <span className="gradient-text">Publish faster.</span> Build
              loyal readership.
            </h1>
            <p className="max-w-xl text-sm text-[var(--muted)] md:text-base">
              InkWell merges content production, engagement, moderation, and
              analytics into one workspace for readers, authors, and admins.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link
                to={isAuthenticated ? "/feed" : "/signup"}
                className="btn-primary inline-flex items-center rounded-full px-5 py-2.5 text-sm"
              >
                {isAuthenticated ? "Open Feed" : "Start Free"}
                <ArrowRight className="ml-2" size={16} />
              </Link>
              <Link
                to="/feed"
                className="btn-ghost inline-flex items-center rounded-full px-5 py-2.5 text-sm"
              >
                Explore Feed
              </Link>
            </div>
            <div className="grid grid-cols-2 gap-3 pt-2 md:grid-cols-4">
              <div className="glass rounded-2xl p-3 text-xs text-[var(--muted)]">
                <Sparkles size={14} className="mb-2 text-[var(--primary)]" />
                Rich editor + media
              </div>
              <div className="glass rounded-2xl p-3 text-xs text-[var(--muted)]">
                <ShieldCheck size={14} className="mb-2 text-[var(--primary)]" />
                RBAC security
              </div>
              <div className="glass rounded-2xl p-3 text-xs text-[var(--muted)]">
                <BellRing size={14} className="mb-2 text-[var(--primary)]" />
                Smart notifications
              </div>
              <div className="glass rounded-2xl p-3 text-xs text-[var(--muted)]">
                <BarChart3 size={14} className="mb-2 text-[var(--primary)]" />
                Platform analytics
              </div>
            </div>
          </div>

          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2, duration: 0.5 }}
            className="motion-float glass edge-glow relative overflow-hidden rounded-3xl p-4 md:p-6"
          >
            <img
              src="https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=1400&q=80"
              alt="AI creative workspace"
              className="h-44 w-full rounded-2xl object-cover"
            />
            <div className="pointer-events-none absolute inset-x-4 top-4 h-44 rounded-2xl bg-gradient-to-t from-slate-900/45 via-transparent to-transparent" />
            <div className="mt-4 rounded-2xl bg-gradient-to-br from-blue-500/12 via-violet-500/10 to-fuchsia-500/12 p-5">
              <p className="text-xs uppercase tracking-[0.09em] text-[var(--muted)]">
                Live platform stats
              </p>
              <h3 className="mt-3 text-xl font-semibold">Publishing Snapshot</h3>
              <div className="mt-5 space-y-3 text-sm text-[var(--muted)]">
                <div className="glass rounded-xl p-3">Posts: {posts.length}</div>
                <div className="glass rounded-xl p-3">Views: {totalViews}</div>
                <div className="glass rounded-xl p-3">Likes: {totalLikes}</div>
              </div>
            </div>
          </motion.div>
        </div>
        </div>
      </section>

      {error && (
        <section className="container-shell mt-6">
          <p className="rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-500">
            {error}
          </p>
        </section>
      )}

      <AnimatedSection className="container-shell mt-14">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {features.map((feature) => (
            <motion.div
              key={feature.title}
              whileHover={{ y: -4 }}
              className="glass edge-glow rounded-3xl p-5"
            >
              <h3 className="text-lg font-semibold">{feature.title}</h3>
              <p className="mt-2 text-sm text-[var(--muted)]">{feature.desc}</p>
            </motion.div>
          ))}
        </div>
      </AnimatedSection>

      <AnimatedSection className="container-shell mt-14">
        <div className="grid gap-4 rounded-4xl border border-[var(--line)] bg-[var(--surface-2)]/70 p-5 md:grid-cols-4">
          {metrics.map((metric) => (
            <div key={metric.label} className="glass rounded-2xl p-4">
              <p className="text-2xl font-semibold">{metric.value}</p>
              <p className="mt-1 text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                {metric.label}
              </p>
            </div>
          ))}
        </div>
      </AnimatedSection>

      <AnimatedSection className="container-shell mt-14">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="hero-title text-2xl">Trending Articles</h2>
          <Link to="/feed" className="btn-ghost rounded-full px-4 py-2 text-xs">
            View all
          </Link>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {topPosts.map((post) => (
            <article key={post.postId} className="glass rounded-3xl p-5">
              <span className="chip rounded-full px-2 py-1 text-[10px]">
                {post.status}
              </span>
              <h3 className="mt-3 text-lg font-semibold">{post.title}</h3>
              <p className="mt-1 text-xs text-[var(--muted)]">
                {post.authorName || `User #${post.authorId}`}
              </p>
              <p className="mt-2 text-sm text-[var(--muted)]">{post.excerpt}</p>
              <Link
                to={`/post/${post.slug}`}
                className="btn-primary mt-4 inline-flex rounded-full px-4 py-2 text-xs"
              >
                Open Post
              </Link>
            </article>
          ))}
        </div>
      </AnimatedSection>

      <AnimatedSection className="container-shell mt-14">
        <div className="glass rounded-3xl p-5 md:p-6">
          <h3 className="text-xl font-semibold">Trending Tags</h3>
          <div className="mt-4 flex flex-wrap gap-2">
            {trendingTags.length === 0 && (
              <span className="text-sm text-[var(--muted)]">No tags yet</span>
            )}
            {trendingTags.map((tag) => (
              <span key={tag.tagId} className="chip rounded-full px-3 py-1 text-xs">
                #{tag.name} ({tag.postCount || 0})
              </span>
            ))}
          </div>
        </div>
      </AnimatedSection>

      <AnimatedSection className="container-shell mt-14">
        <div className="grid gap-4 md:grid-cols-2">
          {testimonials.map((entry, index) => (
            <motion.article
              key={entry.name}
              initial={{ opacity: 0, y: 18 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.2 }}
              transition={{ delay: index * 0.12 }}
              className="glass edge-glow relative overflow-hidden rounded-3xl p-5"
            >
              <img src={entry.image} alt={entry.name} className="h-44 w-full rounded-2xl object-cover" />
              <div className="pointer-events-none absolute inset-x-5 top-5 h-44 rounded-2xl bg-gradient-to-t from-slate-900/50 to-transparent" />
              <p className="mt-4 text-sm text-[var(--muted)]">"{entry.quote}"</p>
              <p className="mt-3 text-sm font-semibold">{entry.name}</p>
              <p className="text-xs text-[var(--muted)]">{entry.role}</p>
            </motion.article>
          ))}
        </div>
      </AnimatedSection>

      <AnimatedSection className="container-shell mt-14">
        <div className="relative overflow-hidden rounded-4xl border border-[var(--line)] bg-[var(--surface-2)]/75 p-7 md:p-10">
          <div className="pointer-events-none absolute -right-16 -top-10 h-52 w-52 rounded-full bg-violet-500/20 blur-3xl" />
          <h3 className="hero-title text-2xl md:text-4xl">
            Build your next publishing workflow with <span className="gradient-text">InkWell</span>
          </h3>
          <p className="mt-3 max-w-2xl text-sm text-[var(--muted)] md:text-base">
            One platform for readers, authors, and admins with modern content tools and automated engagement.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to={isAuthenticated ? "/author" : "/signup"} className="btn-primary rounded-full px-5 py-2.5 text-sm">
              {isAuthenticated ? "Go to Author Studio" : "Create Account"}
            </Link>
            <Link to="/feed" className="btn-ghost rounded-full px-5 py-2.5 text-sm">
              Browse Stories
            </Link>
          </div>
        </div>
      </AnimatedSection>
    </div>
  );
}

export default HomePage;
