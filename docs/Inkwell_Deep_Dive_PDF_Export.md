# Inkwell Deep Dive PDF Export Guide

This repository already contains the PDF-ready study document:

- `docs/Inkwell_Deep_Dive_Document.md`

## How to export it to PDF

### Option 1: Microsoft Word / Google Docs
1. Open `Inkwell_Deep_Dive_Document.md`
2. Copy all content
3. Paste into Word or Google Docs
4. Add a title page if you want
5. Use **File → Export / Download → PDF**

### Option 2: VS Code + Markdown Preview
1. Open `Inkwell_Deep_Dive_Document.md` in VS Code
2. Use Markdown preview
3. Print from preview window
4. Save as PDF

### Option 3: Markdown-to-PDF tool
If you already have a markdown-to-PDF converter on your machine, point it to:

- input: `docs/Inkwell_Deep_Dive_Document.md`
- output: `docs/Inkwell_Deep_Dive_Document.pdf`

## Recommended document order for presentation

1. High-level architecture
2. Eureka Server
3. API Gateway
4. Auth Service
5. Post Service
6. Comment Service
7. Category Service
8. Notification Service
9. Newsletter Service
10. Media Service
11. Frontend React app
12. Security, Kafka, Feign, Logging, Error handling
13. Interview preparation

## Why this guide is useful

- It is written in simple Hinglish / easy English.
- It is aligned with the actual Inkwell codebase.
- It is organized for sprint review, viva, and interview prep.
- It can be directly exported into PDF.


