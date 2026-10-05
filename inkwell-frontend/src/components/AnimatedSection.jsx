import { motion } from "framer-motion";

function AnimatedSection({ className = "", children, delay = 0 }) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.25 }}
      transition={{ duration: 0.45, delay }}
      className={className}
    >
      {children}
    </motion.section>
  );
}

export default AnimatedSection;
