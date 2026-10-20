#!/usr/bin/env python3
"""The clubs collector's offline parts (.github/tools/team_ids.py): its transliteration is the screen's translit()
(run in node from src/template.html), Wikidata labels are cleaned, no two teams keep one Arabic name, and the
monthly diff names what changed. Run: python3 tests/team_ids_test.py"""
import importlib.util
import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("team_ids", ROOT / ".github/tools/team_ids.py")
ti = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ti)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


# transliteration: the same as the screen's, on every English name in the list and a few hard ones
names = [c["en"] for c in json.loads((ROOT / "assets/shared/teams.json").read_text(encoding="utf-8"))]
names += ["Al-Hilal", "PSG", "St. Louis City SC", "Brighton & Hove Albion", "1. FC Köln", "Schalke 04", "AC Milan",
          "Ceará", "Cercle Brugge", "Beşiktaş", "Fenerbahçe", "İstanbul Başakşehir", "Kasımpaşa", "Atlético Madrid", "Al Ain", "  Two  Spaces ", "Queens Park Rangers", "Chelsea"]
node = shutil.which("node")
if not node:
    print("SKIP translit against the screen: no node here")
else:
    page = (ROOT / "src/template.html").read_text(encoding="utf-8")
    start = page.index("const TL_WORDS")
    end = page.index("async function wikidataArabic")
    js = ("const hasArabic = s => /[؀-ۿ]/.test(s);\nconst ar = d => String(d).replace(/[0-9]/g, c => "
          "String.fromCharCode(0x660 + +c));\n" + page[start:end]
          + "\nprocess.stdout.write(JSON.stringify(JSON.parse(require('fs').readFileSync(0, 'utf8')).map(translit)));")
    r = subprocess.run([node, "-e", js], input=json.dumps(names), capture_output=True, text=True, encoding="utf-8")
    check("node runs the screen's translit()", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        screen = json.loads(r.stdout)
        differ = [(n, s, ti.translit(n)) for n, s in zip(names, screen) if s != ti.translit(n)]
        check(f"translit: the screen's on all {len(names)} names", not differ, differ[:6])

# the name key: the collector's team_key() is the screen's teamKey(), accents folded, so teams-ar.json's
# "Atletico Madrid" names TheSportsDB's "Atlético Madrid"
for name, want in [("Atlético Madrid", "atletico madrid"), ("Borussia Mönchengladbach", "borussia monchengladbach"),
                   ("Beşiktaş", "besiktas"), ("Fenerbahçe", "fenerbahce"), ("İstanbul Başakşehir", "istanbul basaksehir"),
                   ("Kasımpaşa", "kasimpasa"), ("Al-Hilal SFC", "alhilal sfc"), ("Al Hilal FC", "alhilal")]:
    check(f"team_key({name!r}) = {want!r}", ti.team_key(name) == want, ti.team_key(name))
if node:
    key_js = page[page.index("const teamKey ="):page.index("\n", page.index(".replace(/^al /,'al');"))]
    r = subprocess.run([node, "-e", key_js + "\nprocess.stdout.write(JSON.stringify(JSON.parse(require('fs')"
                        ".readFileSync(0, 'utf8')).map(teamKey)));"], input=json.dumps(names), capture_output=True,
                       text=True, encoding="utf-8")
    differ = [(n, s, ti.team_key(n)) for n, s in zip(names, json.loads(r.stdout or "[]")) if s != ti.team_key(n)]
    check(f"team_key: the screen's teamKey() on all {len(names)} names", r.returncode == 0 and not differ,
          differ[:6] or r.stderr[-300:])
first, last, _ = ti.curated()
for name, want in [("Atlético Madrid", "أتلتيكو مدريد"), ("Borussia Mönchengladbach", "بوروسيا مونشنغلادباخ"),
                   ("Beşiktaş", "بشكتاش"), ("Fenerbahçe", "فنربخشة")]:
    check(f"curated name for {name}: {want}", last.get(ti.team_key(name)) == want, last.get(ti.team_key(name)))

for label, want in [("نادي العين (الإمارات)", "العين"), ("النادي الأهلي (قطر)", "الأهلي"), ("فريق الوصل", "الوصل"),
                    ("نادي ريال مدريد لكرة القدم", "ريال مدريد"), ("ريَال", "ريال"), ("گوتنبرغ", "جوتنبرغ"),
                    ("Al Ain", ""), ("", ""), ("ا", ""), ("نادي (قطر)", "")]:
    check(f"clean_ar({label!r}) = {want!r}", ti.clean_ar(label) == want, ti.clean_ar(label))

# names told apart: the Saudi club and the national team keep theirs
teams = {
    "1": {"en": "Al-Ain", "ar": "العين", "leagues": ["Saudi First Division League"], "arFrom": "curated"},
    "2": {"en": "Al Ain", "ar": "العين", "leagues": ["UAE Pro League"], "arFrom": "curated"},
    "3": {"en": "Qatar", "ar": "قطر", "leagues": ["National teams"], "arFrom": "curated"},
    "4": {"en": "Qatar SC", "ar": "قطر", "leagues": ["Qatar Stars League"], "arFrom": "curated"},
    "5": {"en": "Al-Arabi", "ar": "العربي", "leagues": ["Qatar Stars League"], "arFrom": "wikidata"},
    "6": {"en": "Al-Arabi Al-Saudi", "ar": "العربي", "leagues": ["Saudi First Division League"], "arFrom": "curated"},
    "7": {"en": "Al Ahly", "ar": "الأهلي المصري", "leagues": ["Egyptian Premier League"], "arFrom": "curated"},
    "8": {"en": "Al-Ahly Benghazi", "ar": "الاهلي المصري", "leagues": ["Egyptian Premier League"], "arFrom": "wikidata"},
    "9": {"en": "Al-Ahli", "ar": "الأهلي", "leagues": ["Saudi-Arabian Pro League"], "arFrom": "curated"},
}
report, problems = {"names told apart": []}, []
ti.unique_names(teams, report, problems)
got = {i: t["ar"] for i, t in teams.items()}
check("Saudi Al-Ain keeps «العين», the Emirati one gets its country", got["1"] == "العين" and got["2"] == "العين الإماراتي", got)
check("the national team keeps «قطر», the club becomes «نادي قطر»", got["3"] == "قطر" and got["4"] == "نادي قطر", got)
check("Saudi Al-Arabi keeps «العربي», Qatar's gets «العربي القطري»", got["6"] == "العربي" and got["5"] == "العربي القطري", got)
check("two names that differ only in hamza are told apart too", ti.plain_ar(got["7"]) != ti.plain_ar(got["8"]), got)
check("no names left shared", len({ti.plain_ar(a) for a in got.values()}) == len(got) and not problems, (got, problems))

# the monthly diff
old = [{"id": "1", "en": "Al-Ain", "ar": "العين", "leagues": ["Saudi First Division League"]},
       {"id": "2", "en": "Gone FC", "ar": "غون", "leagues": ["UAE Pro League"]},
       {"id": "3", "en": "Hellas", "ar": "", "leagues": ["Italian Serie A"]}]
new = [{"id": "1", "en": "Al-Ain", "ar": "العين", "leagues": ["Saudi-Arabian Pro League"]},
       {"id": "3", "en": "Hellas Verona", "ar": "هيلاس فيرونا", "leagues": ["Italian Serie A"]},
       {"id": "4", "en": "New FC", "ar": "نيو", "leagues": ["UAE Pro League"]}]
d = ti.diff(old, new)
check("diff: added, removed, renamed, moved", [t["id"] for t in d["added"]] == ["4"] and [t["id"] for t in d["removed"]] == ["2"]
      and [t["id"] for t in d["renamed"]] == ["3"] and [t["id"] for t in d["leagues changed"]] == ["1"], d)
md = ti.diff_md(d, len(old), len(new))
check("diff.md lists them", all(s in md for s in ("New FC", "Gone FC", "Hellas -> Hellas Verona", "Saudi-Arabian Pro League")), md)


# ---- a whole run on a fake TheSportsDB and Wikidata: a full one, then an incremental one that looks up only
# the club that is new and the one renamed
import contextlib   # noqa: E402
import datetime   # noqa: E402
import io   # noqa: E402
import tempfile   # noqa: E402
import urllib.parse as up   # noqa: E402

LEAGUE_NAMES = list(ti.LEAGUES)
SEASONS = {n: ti.seasons_to_try(ti.LEAGUES[n], datetime.date.today())[:2] for n in LEAGUE_NAMES}
PINNED = {("Saudi-Arabian Pro League", 0): ("136013", "Al-Hilal"), ("Saudi-Arabian Pro League", 1): ("136022", "Al-Nassr"),
          ("Spanish La Liga", 0): ("133738", "Real Madrid"), ("English Premier League", 0): ("133604", "Arsenal")}


def fake_world(extra=None, renamed=None):
    clubs = {}
    for k, n in enumerate(LEAGUE_NAMES):
        clubs[n] = [PINNED.get((n, j), (f"9{k:02d}{j:03d}", f"Club {k} {j}")) for j in range(ti.LEAGUES[n]["min"] + 1)]
    for n, team in (extra or {}).items():
        clubs[n].append(team)
    names = {i: en for v in clubs.values() for i, en in v}
    names.update(renamed or {})
    national = {en: ("136137" if en == "Saudi Arabia" else f"8{j:03d}") for j, en in enumerate(ti.NATIONAL)}
    names.update({i: en for en, i in national.items()})
    return clubs, names, national


def fake_fetch(world, calls):
    clubs, names, national = world

    def fetch(url, data=None, headers=None, pause=0, tries=6, count="thesportsdb"):
        calls.append(url)
        ti.STATS[count] += 1   # as the real fetch() counts
        if not url.startswith(ti.API):
            return {}   # Wikidata and Wikipedia: nothing known, so names come from our lists or transliteration
        path, _, query = url[len(ti.API):].partition("?")
        q = dict(up.parse_qsl(query))
        if path == "all_leagues.php":
            return {"leagues": [{"strLeague": n, "idLeague": f"L{k}"} for k, n in enumerate(LEAGUE_NAMES)]}
        if path == "eventsseason.php":
            n = LEAGUE_NAMES[int(q["id"][1:])]
            if q["s"] not in SEASONS[n]:
                return {"events": None}
            v = clubs[n]
            return {"events": [{"idHomeTeam": a[0], "strHomeTeam": names[a[0]], "idAwayTeam": b[0],
                                "strAwayTeam": names[b[0]]} for a, b in zip(v, v[1:] + v[:1])]}
        if path == "searchteams.php":
            if q["t"] == "Jordan":
                return {"teams": None}   # as TheSportsDB answers: found in its matches instead
            i = national.get(q["t"])
            return {"teams": [{"idTeam": i, "strTeam": q["t"], "strSport": "Soccer", "strGender": "Male"}] if i else None}
        if path == "searchevents.php":
            a, _, b = q["e"].replace("_", " ").partition(" vs ")
            return {"event": [{"idHomeTeam": national[a], "strHomeTeam": a, "idAwayTeam": national[b],
                               "strAwayTeam": b}]} if a in national and b in national else {"event": None}
        if path == "lookupteam.php":
            i = q["id"]
            return {"teams": [{"idTeam": i, "strTeam": names[i], "strGender": "Male"}]} if i in names else {"teams": None}
        return None   # an empty answer

    return fetch


def run_once(world, out, cache, full):
    calls = []
    ti.fetch, ti.FULL, ti.OUT, ti.CACHE = fake_fetch(world, calls), full, out, cache
    ti.STATS.update({k: 0 for k in ti.STATS})
    try:
        with contextlib.redirect_stdout(io.StringIO()):
            ti.main()
    except SystemExit:
        pass   # a check failed (the fake Saudi clubs have no Arabic names): the files are written all the same
    return calls, json.loads((out / "report.json").read_text(encoding="utf-8"))


real_fetch = ti.fetch
with tempfile.TemporaryDirectory() as tmp:
    tmp = Path(tmp)
    (tmp / "one").mkdir()
    (tmp / "two").mkdir()
    world = fake_world()
    calls, report = run_once(world, tmp / "one", tmp / "none.json", full=True)
    lookups = [c for c in calls if "lookupteam.php" in c]
    every = len(json.loads((tmp / "one/teams.json").read_text(encoding="utf-8")))
    check("full run: every team looked up (and the known ids first)", len(lookups) == every + len(ti.KNOWN),
          (len(lookups), every))
    check("full run: Jordan found in its matches", any("Jordan" in x for x in report["national teams found in their matches"]),
          report["national teams not found"])
    check("full run: every league above its minimum", not [p for p in report["problems"] if "expected" in p], report["problems"])
    check("full run: the report counts the requests", report["requests"]["made"]["thesportsdb"] == len(
        [c for c in calls if c.startswith(ti.API)]), report["requests"])
    new_club, renamed = ("777001", "Brand New FC"), next(iter(world[0]["Dutch Eredivisie"]))[0]
    world2 = fake_world(extra={"Dutch Eredivisie": new_club}, renamed={renamed: "Renamed Eagles"})
    calls, report = run_once(world2, tmp / "two", tmp / "one/teams-cache.json", full=False)
    lookups = sorted(up.parse_qs(up.urlparse(c).query)["id"][0] for c in calls if "lookupteam.php" in c)
    check("incremental: only the new and the renamed club looked up (after the known ids)",
          lookups == sorted([new_club[0], renamed] + list(ti.KNOWN)), lookups)
    check("incremental: no league or national team searched again",
          not [c for c in calls if "all_leagues.php" in c or "searchteams.php" in c or "searchevents.php" in c],
          [c for c in calls if "search" in c][:4])
    check("incremental: Wikidata asked about the two clubs only", report["wikidata"].get("looked up before") ==
          report["teams"] - 2, report["wikidata"])
    out = {t["id"]: t for t in json.loads((tmp / "two/teams.json").read_text(encoding="utf-8"))}
    check("incremental: the new club is in, the renamed one has its new name",
          new_club[0] in out and out[renamed]["en"] == "Renamed Eagles", out.get(renamed))
    check("incremental: far fewer requests", report["requests"]["made"]["thesportsdb"] < 100, report["requests"])
ti.fetch = real_fetch

print("\nall clubs collector checks passed" if not failed else f"\n{failed} failed")
sys.exit(1 if failed else 0)
