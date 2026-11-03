// Parses the One Pace watch page (Next.js RSC payload) into MyOnePaceList's catalog format.
import { createHash } from "node:crypto";

export const SCHEMA = 1;
const UNDEF = "$undefined";
const LANG = "es-419";

/** Concatenates every `self.__next_f.push([1,"..."])` chunk into the decoded RSC text. */
export function extractRsc(html) {
  const re = /self\.__next_f\.push\(\[1,("(?:[^"\\]|\\.)*")\]\)/g;
  let out = "";
  for (const m of html.matchAll(re)) out += JSON.parse(m[1]);
  if (!out) throw new Error("No RSC payload found: page layout changed?");
  return out;
}

/** Returns the JSON object literal that starts at `start` (string-aware brace matching). */
export function objectAt(text, start) {
  let depth = 0, inStr = false, esc = false;
  for (let i = start; i < text.length; i++) {
    const c = text[i];
    if (inStr) {
      if (esc) esc = false;
      else if (c === "\\") esc = true;
      else if (c === '"') inStr = false;
    } else if (c === '"') inStr = true;
    else if (c === "{") depth++;
    else if (c === "}" && --depth === 0) return text.slice(start, i + 1);
  }
  throw new Error("Unbalanced JSON object");
}

export function extractSegments(rsc) {
  const key = '{"timeline":';
  const at = rsc.indexOf(key);
  if (at < 0) throw new Error("timeline not found in RSC payload");
  const timeline = JSON.parse(objectAt(rsc, at)).timeline;
  if (!Array.isArray(timeline?.segments)) throw new Error("timeline.segments missing");
  return timeline.segments;
}

const def = (v) => (v === UNDEF || v === undefined || v === null ? null : v);

/** "1 h y 25 min" | "17 min" | "2 h" -> minutes */
export function parseDuration(s) {
  if (!s || s === UNDEF) return 0;
  const h = /(\d+)\s*h/.exec(s);
  const m = /(\d+)\s*min/.exec(s);
  return (h ? +h[1] * 60 : 0) + (m ? +m[1] : 0);
}

/** {terms:[{from,to}|{text}]} -> "1–7, 19, Episode of East Blue" */
export function formatTerms(t) {
  const terms = def(t)?.terms ?? [];
  return terms
    .map((x) => (x.text ? x.text : x.from === x.to ? `${x.from}` : `${x.from}–${x.to}`))
    .join(", ");
}

function files(group) {
  const out = {};
  for (const it of group.items ?? []) out[String(it.resolution)] = it.id;
  return Object.keys(out).length ? out : null;
}

/** Splits fileGroups into {sub, dub, extended:{sub,dub}} for Latin-American Spanish only. */
export function sources(groups = []) {
  const res = { sub: null, dub: null, extended: null };
  for (const g of groups) {
    const cut = def(g.cut);
    const lang = def(g.dub) === LANG ? "dub" : def(g.sub) === LANG ? "sub" : null;
    if (!lang) continue;
    const f = files(g);
    if (!f) continue;
    if (cut === null) res[lang] ??= f;
    else if (cut === "extended") (res.extended ??= { sub: null, dub: null })[lang] ??= f;
  }
  return res;
}

export function toCatalog(segments, now = new Date()) {
  const arcs = segments.map((a) => ({
    slug: a.slug,
    title: a.title,
    description: def(a.description) ?? "",
    special: !!a.special,
    unfinished: !!a.unfinished,
    chapters: formatTerms(a.chapters),
    animeEpisodes: formatTerms(a.episodes),
    durationMin: parseDuration(a.duration),
    image: def(a.backdrops?.[0]?.src) ?? def(a.covers?.[0]?.src),
    unreleased: (a.unreleasedParts ?? []).length,
    episodes: (a.parts ?? []).map((p) => ({
      id: p.slug,
      arc: a.slug,
      part: p.part,
      title: p.title,
      description: def(p.description) ?? "",
      special: !!p.special,
      toBeRedone: !!p.toBeRedone,
      chapters: formatTerms(p.chapters),
      animeEpisodes: formatTerms(p.episodes),
      durationMin: parseDuration(p.duration),
      image: def(p.stills?.[0]?.src),
      ...sources(p.fileGroups),
    })),
  }));
  const version = createHash("sha256").update(JSON.stringify(arcs)).digest("hex").slice(0, 16);
  return { schema: SCHEMA, version, generatedAt: now.toISOString(), source: "https://onepace.net/es-419/watch", arcs };
}

// These values end up inside URLs in the app (Pixeldrain, the onepace.net image optimizer).
// The app sanitizes them too (CatalogGuard.kt); here a surprise fails the run instead of publishing it.
const SLUG = /^[a-z0-9][a-z0-9.-]{0,95}$/;
const FILE_ID = /^[A-Za-z0-9]{4,32}$/;
const RESOLUTIONS = new Set(["480", "720", "1080"]);
const isImagePath = (s) => s.startsWith("/") && !s.startsWith("//") && !s.includes("..") && !/[\s\\]/.test(s);

function checkFiles(where, files) {
  if (files == null) return;
  for (const [res, id] of Object.entries(files)) {
    if (!RESOLUTIONS.has(res)) throw new Error(`unexpected resolution ${res} in ${where}`);
    if (typeof id !== "string" || !FILE_ID.test(id)) throw new Error(`suspicious file id ${JSON.stringify(id)} in ${where}`);
  }
}

function checkImage(where, src) {
  if (src != null && (typeof src !== "string" || !isImagePath(src))) throw new Error(`suspicious image ${JSON.stringify(src)} in ${where}`);
}

/** Throws if the catalog looks broken, carries unexpected values or shrank compared to the previous one. */
export function validate(cat, previous) {
  if (cat.schema !== SCHEMA) throw new Error("bad schema");
  if (cat.arcs.length < 10) throw new Error(`too few arcs: ${cat.arcs.length}`);
  const count = (c) => c.arcs.reduce((n, a) => n + a.episodes.length, 0);
  const ids = new Set();
  for (const a of cat.arcs) {
    if (typeof a.slug !== "string" || !SLUG.test(a.slug)) throw new Error(`bad arc slug ${JSON.stringify(a.slug)}`);
    checkImage(a.slug, a.image);
    for (const e of a.episodes) {
      if (typeof e.id !== "string" || !SLUG.test(e.id) || ids.has(e.id)) throw new Error(`bad/duplicate episode id ${e.id}`);
      ids.add(e.id);
      checkImage(e.id, e.image);
      checkFiles(e.id, e.sub);
      checkFiles(e.id, e.dub);
      checkFiles(`${e.id} (extended)`, e.extended?.sub);
      checkFiles(`${e.id} (extended)`, e.extended?.dub);
    }
  }
  if (previous && count(cat) < count(previous) * 0.9)
    throw new Error(`episode count dropped ${count(previous)} -> ${count(cat)}`);
  return true;
}

export const parsePage = (html, now) => toCatalog(extractSegments(extractRsc(html)), now);
