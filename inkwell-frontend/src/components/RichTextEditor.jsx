import { forwardRef, useEffect, useImperativeHandle } from "react";
import { EditorContent, useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";
import Link from "@tiptap/extension-link";
import Image from "@tiptap/extension-image";
import { Bold, Code, Heading2, ImagePlus, Italic, Link2, List, ListOrdered, Quote } from "lucide-react";

function ToolbarButton({ active, disabled, onClick, title, children }) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      title={title}
      className={`rounded-xl border px-2.5 py-1.5 text-xs transition disabled:cursor-not-allowed disabled:opacity-50 ${
        active
          ? "border-[var(--line-strong)] bg-[var(--surface-2)] text-[var(--text)]"
          : "border-[var(--line)] bg-[var(--surface-2)]/70 text-[var(--muted)] hover:text-[var(--text)]"
      }`}
    >
      {children}
    </button>
  );
}

const RichTextEditor = forwardRef(function RichTextEditor(
  { value, onChange, placeholder = "Write your story..." },
  ref
) {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        codeBlock: {
          HTMLAttributes: { class: "rounded-xl border border-white/10 bg-black/30 p-3 overflow-auto" },
        },
      }),
      Placeholder.configure({ placeholder }),
      Link.configure({
        openOnClick: false,
        autolink: true,
        protocols: ["http", "https", "mailto"],
      }),
      Image.configure({
        HTMLAttributes: { class: "my-3 rounded-xl border border-white/10 max-h-[420px] object-cover" },
      }),
    ],
    content: value || "",
    onUpdate({ editor: tiptapEditor }) {
      onChange(tiptapEditor.getHTML());
    },
    editorProps: {
      attributes: {
        class:
          "tiptap min-h-[220px] rounded-2xl border border-[var(--input-border)] bg-[var(--input-bg)] px-4 py-3 text-sm text-[var(--text)] outline-none",
      },
    },
  });

  useEffect(() => {
    if (!editor) {
      return;
    }
    const incoming = String(value || "");
    const current = editor.getHTML();
    if (incoming !== current && !editor.isFocused) {
      editor.commands.setContent(incoming, false);
    }
  }, [editor, value]);

  useImperativeHandle(
    ref,
    () => ({
      focus: () => editor?.chain().focus().run(),
      insertImage: (url, alt = "image") => {
        if (!editor || !url) {
          return;
        }
        editor.chain().focus().setImage({ src: url, alt }).run();
      },
      insertLink: (url, label = "Open media") => {
        if (!editor || !url) {
          return;
        }
        editor
          .chain()
          .focus()
          .insertContent(`<a href="${url}" target="_blank" rel="noreferrer">${label}</a>`)
          .run();
      },
      clear: () => editor?.commands.clearContent(true),
    }),
    [editor]
  );

  const setLink = () => {
    if (!editor) {
      return;
    }
    const previous = editor.getAttributes("link").href;
    const url = window.prompt("Enter URL", previous || "");
    if (url === null) {
      return;
    }
    if (!url.trim()) {
      editor.chain().focus().unsetLink().run();
      return;
    }
    editor.chain().focus().setLink({ href: url.trim(), target: "_blank", rel: "noreferrer" }).run();
  };

  const insertImage = () => {
    if (!editor) {
      return;
    }
    const url = window.prompt("Enter image URL");
    if (!url || !url.trim()) {
      return;
    }
    const alt = window.prompt("Enter alt text (optional)") || "image";
    editor.chain().focus().setImage({ src: url.trim(), alt }).run();
  };

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        <ToolbarButton
          title="Bold"
          disabled={!editor}
          active={editor?.isActive("bold")}
          onClick={() => editor?.chain().focus().toggleBold().run()}
        >
          <Bold size={13} />
        </ToolbarButton>
        <ToolbarButton
          title="Italic"
          disabled={!editor}
          active={editor?.isActive("italic")}
          onClick={() => editor?.chain().focus().toggleItalic().run()}
        >
          <Italic size={13} />
        </ToolbarButton>
        {/*<ToolbarButton*/}
        {/*  title="Heading"*/}
        {/*  disabled={!editor}*/}
        {/*  active={editor?.isActive("heading", { level: 2 })}*/}
        {/*  onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()}*/}
        {/*>*/}
        {/*  <Heading2 size={13} />*/}
        {/*</ToolbarButton>*/}
        {/*<ToolbarButton*/}
        {/*  title="Quote"*/}
        {/*  disabled={!editor}*/}
        {/*  active={editor?.isActive("blockquote")}*/}
        {/*  onClick={() => editor?.chain().focus().toggleBlockquote().run()}*/}
        {/*>*/}
        {/*  <Quote size={13} />*/}
        {/*</ToolbarButton>*/}
        <ToolbarButton
          title="Bullet list"
          disabled={!editor}
          active={editor?.isActive("bulletList")}
          onClick={() => editor?.chain().focus().toggleBulletList().run()}
        >
          <List size={13} />
        </ToolbarButton>
        <ToolbarButton
          title="Ordered list"
          disabled={!editor}
          active={editor?.isActive("orderedList")}
          onClick={() => editor?.chain().focus().toggleOrderedList().run()}
        >
          <ListOrdered size={13} />
        </ToolbarButton>
        <ToolbarButton
          title="Code block"
          disabled={!editor}
          active={editor?.isActive("codeBlock")}
          onClick={() => editor?.chain().focus().toggleCodeBlock().run()}
        >
          <Code size={13} />
        </ToolbarButton>
        <ToolbarButton
          title="Link"
          disabled={!editor}
          active={editor?.isActive("link")}
          onClick={setLink}
        >
          <Link2 size={13} />
        </ToolbarButton>
        <ToolbarButton
          title="Image"
          disabled={!editor}
          active={false}
          onClick={insertImage}
        >
          <ImagePlus size={13} />
        </ToolbarButton>
      </div>
      <EditorContent editor={editor} />
    </div>
  );
});

export default RichTextEditor;

