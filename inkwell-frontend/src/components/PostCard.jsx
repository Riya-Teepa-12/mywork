import { Link } from "react-router-dom";
import { Heart, MessageCircle, Clock3 } from "lucide-react";
import { motion } from "framer-motion";

function initialsFromName(value) {
  const text = String(value || "").trim();
  if (!text) {
    return "U";
  }
  const parts = text.split(/\s+/).filter(Boolean);
  if (parts.length === 1) {
    return parts[0].slice(0, 1).toUpperCase();
  }
  return `${parts[0].slice(0, 1)}${parts[1].slice(0, 1)}`.toUpperCase();
}

function PostCard({ post }) {
  const tags = post.tags || [];
  const category = post.category || "General";
  const readTime = post.readTimeMin ? `${post.readTimeMin} min` : "1 min";
  const authorId = Number(post.authorId);
  const hasAuthorProfile = Number.isFinite(authorId) && authorId > 0;
  const authorName = post.authorName || post.author || (hasAuthorProfile ? `User #${authorId}` : "Unknown author");
  const authorAvatarUrl = String(post.authorAvatarUrl || "").trim();

  return (
    <motion.article
      whileHover={{ y: -5, scale: 1.01 }}
      transition={{ duration: 0.2 }}
      className="glass edge-glow rounded-3xl p-5"
    >
      <div className="mb-4 flex items-center gap-2">
        <span className="chip rounded-full px-2.5 py-1 text-[10px] uppercase tracking-[0.08em]">
          {category}
        </span>
        {post.featured && (
          <span className="rounded-full bg-[var(--primary)]/15 px-2.5 py-1 text-[10px] uppercase tracking-[0.08em] text-[var(--primary)]">
            Featured
          </span>
        )}
      </div>
      <h3 className="text-lg font-semibold leading-tight">{post.title}</h3>
      <p className="mt-3 text-sm text-[var(--muted)]">{post.excerpt}</p>
      {tags.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-2">
          {tags.map((tag) => (
            <span
              key={tag}
              className="rounded-full border border-white/10 px-2.5 py-1 text-xs text-[var(--muted)]"
            >
              #{tag}
            </span>
          ))}
        </div>
      )}
      <div className="mt-5 flex items-center justify-between text-xs text-[var(--muted)]">
        <div className="flex items-center gap-3">
          {authorAvatarUrl ? (
            <img
              src={authorAvatarUrl}
              alt={authorName}
              className="h-7 w-7 rounded-full border border-[var(--line)] object-cover"
              loading="lazy"
            />
          ) : (
            <span className="inline-flex h-7 w-7 items-center justify-center rounded-full border border-[var(--line)] bg-[var(--surface-2)] text-[10px] font-semibold text-[var(--muted)]">
              {initialsFromName(authorName)}
            </span>
          )}
          {hasAuthorProfile ? (
            <Link to={`/authors/${authorId}`} className="text-[var(--primary)] hover:opacity-85">
              {authorName}
            </Link>
          ) : (
            <span>{authorName}</span>
          )}
          {/*<span className="flex items-center gap-1">*/}
          {/*  <Clock3 size={13} /> {readTime}*/}
          {/*</span>*/}
        </div>
        <div className="flex items-center gap-2">
          <span className="icon-action inline-flex items-center gap-1 rounded-full px-2.5 py-1.5">
            <Heart size={13} /> {post.likesCount || 0}
          </span>
          <span className="icon-action inline-flex items-center gap-1 rounded-full px-2.5 py-1.5">
            <MessageCircle size={13} /> {post.commentCount || 0}
          </span>
        </div>
      </div>
      <Link
        to={`/post/${post.slug}`}
        className="btn-primary mt-5 inline-flex rounded-full px-4 py-2 text-xs"
      >
        Read Article
      </Link>
    </motion.article>
  );
}

export default PostCard;
