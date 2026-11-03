import { test } from "node:test";
import assert from "node:assert/strict";
import { parsePage, parseDuration, formatTerms, sources, validate } from "../src/parse.mjs";

const U = "$undefined";
const group = (sub, dub, ids, cut = U) => ({
  sub_type: U, sub, dub, cut,
  items: ids.map((id, i) => ({ id, resolution: [480, 720, 1080][i] })),
});

const segment = {
  slug: "romance-dawn", title: "Romance Dawn", description: "Luffy parte {a la aventura} \"1\"",
  special: false, unfinished: false, duration: "1 h y 25 min",
  chapters: { terms: [{ from: 1, to: 7 }] },
  episodes: { terms: [{ from: 1, to: 3 }, { from: 19, to: 19 }, { text: "Episode of East Blue" }] },
  backdrops: [{ src: "/_next/static/media/rd.jpg" }], covers: [], unreleasedParts: [],
  parts: [
    {
      slug: "romance-dawn-01", part: 1, special: false, title: "El amanecer", description: "d",
      chapters: { terms: [{ from: 1, to: 1 }] }, episodes: { terms: [{ text: "Episode of East Blue" }] },
      duration: "17 min", toBeRedone: false, stills: [{ src: "/_next/static/media/s1.jpg" }],
      fileGroups: [group("es-419", "ja", ["S4", "S7", "S1"]), group(U, "es-419", ["D4", "D7", "D1"])],
    },
    {
      slug: "romance-dawn-02", part: 2, special: false, title: "Sombrero de Paja", description: U,
      chapters: { terms: [{ from: 2, to: 2 }] }, episodes: { terms: [{ from: 1, to: 1 }] },
      duration: "20 min", toBeRedone: true, stills: [],
      fileGroups: [group("es-419", "ja", ["T4", "T7", "T1"]), group("es-419", "ja", ["E4", "E7", "E1"], "extended")],
    },
  ],
};

function page(segments) {
  const payload = 'c:["$","$L20",null,' + JSON.stringify({ data: { timeline: { segments } } }) + "]\n";
  const mid = Math.floor(payload.length / 2); // split across two pushes like Next.js does
  const push = (s) => `<script>self.__next_f.push([1,${JSON.stringify(s)}])</script>`;
  return `<html><body>${push(payload.slice(0, mid))}${push(payload.slice(mid))}</body></html>`;
}

test("parses arcs, episodes and Spanish sources from the RSC payload", () => {
  const cat = parsePage(page([segment]), new Date("2026-11-03T00:00:00Z"));
  assert.equal(cat.schema, 1);
  assert.equal(cat.generatedAt, "2026-11-03T00:00:00.000Z");
  const [arc] = cat.arcs;
  assert.equal(arc.slug, "romance-dawn");
  assert.equal(arc.description, 'Luffy parte {a la aventura} "1"');
  assert.equal(arc.chapters, "1–7");
  assert.equal(arc.animeEpisodes, "1–3, 19, Episode of East Blue");
  assert.equal(arc.durationMin, 85);
  assert.equal(arc.image, "/_next/static/media/rd.jpg");

  const [e1, e2] = arc.episodes;
  assert.equal(e1.id, "romance-dawn-01");
  assert.deepEqual(e1.sub, { 480: "S4", 720: "S7", 1080: "S1" });
  assert.deepEqual(e1.dub, { 480: "D4", 720: "D7", 1080: "D1" });
  assert.equal(e1.durationMin, 17);
  assert.equal(e2.description, "");
  assert.equal(e2.dub, null);
  assert.equal(e2.image, null);
  assert.equal(e2.toBeRedone, true);
  assert.deepEqual(e2.extended, { sub: { 480: "E4", 720: "E7", 1080: "E1" }, dub: null });
});

test("version is stable for identical content and changes with content", () => {
  const a = parsePage(page([segment]), new Date(0));
  const b = parsePage(page([segment]), new Date(1e12));
  assert.equal(a.version, b.version);
  const changed = structuredClone(segment);
  changed.parts[0].title = "Otro";
  assert.notEqual(parsePage(page([changed])).version, a.version);
});

test("helpers", () => {
  assert.equal(parseDuration("2 h"), 120);
  assert.equal(parseDuration(U), 0);
  assert.equal(formatTerms(U), "");
  assert.deepEqual(sources([group("en", "ja", ["X"])]), { sub: null, dub: null, extended: null });
});

test("fails loudly when the page layout changes", () => {
  assert.throws(() => parsePage("<html></html>"), /RSC payload/);
});

test("validate rejects shrinking or duplicated catalogs", () => {
  const mk = (n, dup = false) => ({
    schema: 1,
    arcs: Array.from({ length: 10 }, (_, a) => ({
      slug: `a${a}`,
      episodes: Array.from({ length: n }, (_, e) => ({ id: dup ? "same" : `a${a}-${e}`, sub: { 720: "AbCd1234" } })),
    })),
  });
  assert.ok(validate(mk(10), mk(10)));
  assert.throws(() => validate(mk(5), mk(10)), /dropped/);
  assert.throws(() => validate(mk(2, true)), /duplicate/);
});

test("validate rejects values that would change the app's URLs", () => {
  const ok = () => ({
    schema: 1,
    arcs: Array.from({ length: 10 }, (_, a) => ({
      slug: `a${a}`, image: "/_next/static/media/a.jpg",
      episodes: [{ id: `a${a}-01`, image: null, sub: { 720: "AbCd1234" }, dub: null, extended: null }],
    })),
  });
  assert.ok(validate(ok()));
  const bad = [
    (c) => (c.arcs[0].episodes[0].sub["720"] = "../../user/x"),
    (c) => (c.arcs[0].episodes[0].sub["720"] = "abc?d=1"),
    (c) => (c.arcs[0].episodes[0].sub["4k"] = "AbCd1234"),
    (c) => (c.arcs[0].episodes[0].extended = { sub: { 720: "a/b" }, dub: null }),
    (c) => (c.arcs[0].image = "//evil.example/x.jpg"),
    (c) => (c.arcs[0].episodes[0].image = "https://evil.example/x.jpg"),
    (c) => (c.arcs[0].slug = "../x"),
    (c) => (c.arcs[0].episodes[0].id = "Bad Id"),
  ];
  for (const mutate of bad) {
    const c = ok();
    mutate(c);
    assert.throws(() => validate(c), /suspicious|bad|unexpected/);
  }
});

test("the published catalog passes validation", async () => {
  const { readFile } = await import("node:fs/promises");
  const cat = JSON.parse(await readFile(new URL("../../../catalog.json", import.meta.url), "utf8"));
  assert.ok(validate(cat));
});
