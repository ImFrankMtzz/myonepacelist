// Usage: node src/sync.mjs [outFile...]
// Downloads the One Pace watch page, builds catalog.json and writes it to each outFile
// only when content changed. Exit code 0 = ok (changed or not), 1 = failure.
import { readFile, writeFile } from "node:fs/promises";
import { parsePage, validate } from "./parse.mjs";

const URL_ = "https://onepace.net/es-419/watch";
const outs = process.argv.slice(2);
if (!outs.length) outs.push("catalog.json");

const res = await fetch(URL_, { headers: { "User-Agent": "MyOnePaceList-catalog-sync (+github)" } });
if (!res.ok) throw new Error(`HTTP ${res.status} from ${URL_}`);
const cat = parsePage(await res.text());

let previous = null;
try { previous = JSON.parse(await readFile(outs[0], "utf8")); } catch { /* first run */ }
validate(cat, previous);

const episodes = cat.arcs.reduce((n, a) => n + a.episodes.length, 0);
if (previous?.version === cat.version) {
  console.log(`unchanged (version ${cat.version}, ${cat.arcs.length} arcs, ${episodes} episodes)`);
} else {
  const json = JSON.stringify(cat, null, 1) + "\n";
  for (const f of outs) await writeFile(f, json);
  console.log(`updated -> ${cat.version} (${cat.arcs.length} arcs, ${episodes} episodes)`);
}
