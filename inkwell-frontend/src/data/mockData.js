export const navLinks = [
  { label: "Home", to: "/" },
  { label: "Feed", to: "/feed" },
  { label: "Author", to: "/author" },
  { label: "Admin", to: "/admin" },
  { label: "Newsletter", to: "/newsletter" },
];

export const features = [
  {
    title: "Smart Publishing Flow",
    desc: "Draft, publish, unpublish, and archive with one streamlined lifecycle built for serious writing teams.",
  },
  {
    title: "Threaded Community Layer",
    desc: "Two-level threaded comments, likes, moderation controls, and notification-driven engagement.",
  },
  {
    title: "Role-Aware Platform Core",
    desc: "Reader, Author, and Admin experiences built into one connected ecosystem.",
  },
  {
    title: "Campaign + Alerts Engine",
    desc: "Double opt-in newsletter and in-app notifications for replies, mentions, and new-post triggers.",
  },
];

export const metrics = [
  { label: "Published Posts", value: "18.4K" },
  { label: "Monthly Readers", value: "1.2M" },
  { label: "Engagement Lift", value: "+167%" },
  { label: "Avg. Read Time", value: "6.8m" },
];

export const posts = [
  {
    id: 1,
    slug: "how-to-build-a-scalable-blog-platform",
    title: "How to Build a Scalable Blog Platform",
    excerpt:
      "Designing microservices for publishing, comments, media, newsletters, and notifications.",
    category: "Architecture",
    tags: ["microservices", "java", "spring"],
    author: "Arjun Malhotra",
    readTime: "9 min",
    likes: 847,
    comments: 114,
    featured: true,
  },
  {
    id: 2,
    slug: "reader-engagement-patterns-that-actually-work",
    title: "Reader Engagement Patterns That Actually Work",
    excerpt:
      "A practical blueprint for comment quality, notification cadence, and audience retention loops.",
    category: "Growth",
    tags: ["engagement", "newsletter", "analytics"],
    author: "Kira Sen",
    readTime: "7 min",
    likes: 512,
    comments: 79,
    featured: false,
  },
  {
    id: 3,
    slug: "future-of-rich-text-editing-for-technical-blogs",
    title: "Future of Rich-Text Editing for Technical Blogs",
    excerpt:
      "Comparing modern editor stacks, markdown bridges, and safe HTML sanitisation pipelines.",
    category: "Product",
    tags: ["editor", "security", "ux"],
    author: "Mina Kale",
    readTime: "6 min",
    likes: 429,
    comments: 52,
    featured: false,
  },
];

export const notifications = [
  { id: 1, text: "Kira replied to your comment on 'Scalable Blog Platform'.", time: "2m ago", read: false },
  { id: 2, text: "Your post reached 1,000 reads today.", time: "1h ago", read: false },
  { id: 3, text: "Admin approved your category request.", time: "5h ago", read: true },
];

export const adminStats = [
  { label: "Users", value: "24,199" },
  { label: "Posts", value: "18,402" },
  { label: "Comments", value: "192,840" },
  { label: "Active Subscribers", value: "71,306" },
];
