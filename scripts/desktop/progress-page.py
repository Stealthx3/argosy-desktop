# Builds the Argosy Desktop progress page (published as an Artifact) from the repo's own records:
# phase table in docs/desktop/handoff/README.md, phase scopes in PORT_PLAN.md's Roadmap, each phase's
# numbered step plan in DEVLOG.md, "Steps 1-N done" in the current handoff, and .git/argosy-pacer.json.
# Usage: python3 -I scripts/desktop/progress-page.py [out.html]   (default build/progress/argosy-progress.html)
import datetime, html, json, os, re, sys

root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(root, 'build/progress/argosy-progress.html')
read = lambda p: open(os.path.join(root, p), encoding='utf-8').read()

index = read('docs/desktop/handoff/README.md')
plan = read('PORT_PLAN.md')
devlog = read('DEVLOG.md')
try:
    pacer = json.loads(read('.git/argosy-pacer.json'))
except (OSError, ValueError):
    pacer = {}

scopes = {int(m.group(1)): m.group(2).strip() for m in re.finditer(r'^\| (\d+) · [^|]+\| [^|]+\| ([^|]+)\|', plan, re.M)}
gates = {int(m.group(1)): m.group(2).strip() for m in re.finditer(r'^\d+\. \*\*Phase (\d+):\*\* (.+)$', plan, re.M)}

phases = []
for m in re.finditer(r'^\| (\d+) · ([^|]+?) \| [^|]+\| ([^|]+?) \|$', index, re.M):
    st = m.group(3)
    state = 'done' if st.lower().startswith('closed') else 'active' if st.lower().startswith('current') else 'todo'
    date = re.search(r'\d{4}-\d{2}-\d{2}', st)
    phases.append({'n': int(m.group(1)), 'title': m.group(2), 'state': state, 'date': date.group(0) if date else None})


def devlog_section(n):
    m = re.search(rf'^## Phase {n} · .*$', devlog, re.M)
    if not m:
        return ''
    nxt = re.search(r'^## Phase \d+ · ', devlog[m.end():], re.M)
    return devlog[m.end(): m.end() + nxt.start() if nxt else len(devlog)]


def step_plan(n):
    sec = devlog_section(n)
    start = re.search(r'^#### Order of moves\s*$', sec, re.M) or re.search(rf'^(?:### |\*\*)Phase {n} plan\b.*$', sec, re.M)
    if not start:
        return []
    steps = []
    for line in sec[start.end():].splitlines():
        if re.match(r'#{2,4} |\*\*(Per-step|Action|Reasoning)\b', line):
            break
        s = re.match(r'(\d+)\. \*\*(.+?)\*\*\s*(.*)', line)
        if s:
            title = s.group(2).rstrip('.:')
            steps.append({'id': f'{n}.{s.group(1)}', 'text': title})
    return steps


def scope_items(n):
    parts = []
    for p in (p.strip() for p in re.split(r'[;,]\s*', scopes.get(n, ''))):
        if parts and re.fullmatch(r'[A-Z0-9]+', parts[-1]):
            parts[-1] += ', ' + p
        elif p:
            parts.append(p)
    return [{'id': f'{n}.{i + 1}', 'text': p[0].upper() + p[1:]} for i, p in enumerate(parts)]


active = next((p for p in phases if p['state'] == 'active'), None)
steps_done = 0
if active:
    try:
        h = read(f'docs/desktop/handoff/phase-{active["n"]}.md')
        r = re.search(r'Steps 1\s*[–-]\s*(\d+) done', h) or re.search(r'Step (\d+) done', h)
        steps_done = int(r.group(1)) if r else 0
    except OSError:
        pass

for p in phases:
    p['tasks'] = step_plan(p['n'])
    p['planned'] = bool(p['tasks'])
    if not p['tasks']:
        p['tasks'] = scope_items(p['n'])
    for i, t in enumerate(p['tasks']):
        t['done'] = p['state'] == 'done' or (p is active and i < steps_done)

current_id = None
if active:
    current_id = next((t['id'] for t in active['tasks'] if not t['done']), None)
working = pacer.get('status') == 'working'


def fmt(s):
    s = html.escape(s)
    s = re.sub(r'`([^`]+)`', r'<code>\1</code>', s)
    return s


total = sum(len(p['tasks']) for p in phases)
done = sum(t['done'] for p in phases for t in p['tasks'])
pct = round(100 * done / total, 1) if total else 0
label = {'done': 'Complete', 'active': 'In progress', 'todo': 'Not started'}

markers, sections = [], []
cum = 0
for p in phases:
    n_done = sum(t['done'] for t in p['tasks'])
    n = len(p['tasks'])
    pos = round(100 * cum / total, 2) if total else 0
    cum += n
    markers.append(
        f'<li class="m {p["state"]}" style="--at:{pos}%"><span class="dot" aria-hidden="true"></span>'
        f'<span class="mk">P{p["n"]}</span><span class="sr">Phase {p["n"]}, {label[p["state"]]}</span></li>')
    rows = ''.join(
        f'<li class="t {"done" if t["done"] else ("now" if t["id"] == current_id else "")}"><span class="box" aria-hidden="true"></span>'
        f'<span class="tid">{t["id"]}</span><span class="tx">{fmt(t["text"])}</span>'
        f'{("<span class=nowtag>" + ("Working on it" if working else "Up next") + "</span>") if t["id"] == current_id else ""}</li>'
        for t in p['tasks'])
    note = '' if p['planned'] else '<p class="note">Roadmap scope. The step plan is written when the phase starts.</p>'
    gate = f'<p class="gate"><span>Exit criteria</span>{fmt(re.sub(r"[*]", "", gates[p["n"]]))}</p>' if p['n'] in gates else ''
    when = f'<span class="when">Closed {p["date"]}</span>' if p['state'] == 'done' and p['date'] else ''
    sections.append(
        f'<details class="ph {p["state"]}" {"open" if p["state"] == "active" else ""}><summary>'
        f'<span class="pn">Phase {p["n"]}</span><span class="pt">{html.escape(p["title"])}{when}</span>'
        f'<span class="pill {p["state"]}">{label[p["state"]]}</span><span class="cnt">{n_done}/{n}</span>'
        f'<span class="mini" aria-hidden="true"><span style="width:{round(100 * n_done / n) if n else 0}%"></span></span></summary>'
        f'<ol class="tasks">{rows}</ol>{gate}{note}</details>')

now = datetime.datetime.now().strftime('%b %-d, %Y · %-I:%M %p')
active_txt = f'Phase {active["n"]}: {html.escape(active["title"])}' if active else 'All phases complete'
pacer_txt = html.escape(pacer.get('status', '—').replace('-', ' '))
page = f'''<title>Argosy Desktop Progress</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Chakra+Petch:wght@600;700&family=Roboto:wght@400;500;700&family=Roboto+Mono:wght@400;500&display=swap">
<style>
/* Layout: one column; a single progress rail with phase markers spaced by step count, then collapsible phase ledgers. Palette: Argosy's own tokens.json scheme (teal primary). */
:root {{
  --bg: #fbfbfd; --surface: #f3f4f8; --fg: #1c1b1f; --muted: #5d6270; --line: #dee1e9;
  --brand: #007c91; --brand-soft: #d6e7ea; --done: #388e3c; --todo: #a9aebb;
  --display: "Chakra Petch", "Roboto", system-ui, sans-serif; --body: "Roboto", system-ui, sans-serif; --mono: "Roboto Mono", ui-monospace, monospace;
}}
@media (prefers-color-scheme: dark) {{ :root:not([data-theme="light"]) {{
  --bg: #050507; --surface: #13141a; --fg: #e6e8ec; --muted: #9399a6; --line: #262834;
  --brand: #40c6d6; --brand-soft: #16343a; --done: #66bb6a; --todo: #4a4f5c; color-scheme: dark }} }}
:root[data-theme="dark"] {{
  --bg: #050507; --surface: #13141a; --fg: #e6e8ec; --muted: #9399a6; --line: #262834;
  --brand: #40c6d6; --brand-soft: #16343a; --done: #66bb6a; --todo: #4a4f5c; color-scheme: dark }}
body {{ background: var(--bg); color: var(--fg); font: 15px/1.55 var(--body); }}
.wrap {{ max-width: 920px; margin: 0 auto; padding-block: 40px 64px;
  padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); }}
.eyebrow {{ font: 500 12px/1 var(--mono); letter-spacing: .08em; text-transform: uppercase; color: var(--muted); }}
h1 {{ font: 700 clamp(26px, 4vw, 36px)/1.15 var(--display); color: var(--brand); margin: 8px 0 6px; text-wrap: balance; letter-spacing: .01em; }}
.sub {{ color: var(--muted); margin: 0; max-width: 65ch; }}
.stats {{ display: flex; flex-wrap: wrap; gap: 10px 28px; margin: 28px 0 18px; align-items: baseline; }}
.big {{ font: 700 44px/1 var(--display); font-variant-numeric: tabular-nums; }}
.big small {{ font-size: 20px; color: var(--muted); font-weight: 600; }}
.stat {{ display: flex; flex-direction: column; gap: 2px; min-width: 0; }}
.stat b {{ font: 500 15px/1.3 var(--body); }}
.stat span {{ font: 12px/1.2 var(--mono); color: var(--muted); text-transform: uppercase; letter-spacing: .06em; }}
.rail {{ position: relative; margin: 8px 0 54px; }}
.track {{ height: 14px; border-radius: 7px; background: var(--brand-soft); overflow: hidden; position: relative; }}
.fill {{ height: 100%; width: var(--pct); background: repeating-linear-gradient(-45deg, var(--brand) 0 10px, color-mix(in srgb, var(--brand) 78%, var(--bg)) 10px 20px);
  background-size: 28px 28px; border-radius: 7px; transform-origin: left; animation: grow 1.4s cubic-bezier(.2,.8,.2,1) both, stripe 1.2s linear infinite; }}
@keyframes grow {{ from {{ transform: scaleX(.02); }} to {{ transform: scaleX(1); }} }}
@keyframes stripe {{ to {{ background-position: 28px 0; }} }}
.markers {{ list-style: none; margin: 0; padding: 0; position: absolute; inset: 0; }}
.m {{ position: absolute; left: var(--at); top: -5px; transform: translateX(-50%); display: flex; flex-direction: column; align-items: center; gap: 8px; }}
.m:first-child {{ transform: none; align-items: flex-start; }}
.dot {{ width: 24px; height: 24px; border-radius: 50%; background: var(--bg); border: 3px solid var(--todo); box-sizing: border-box; }}
.m.done .dot {{ background: var(--done); border-color: var(--done); }}
.m.active .dot {{ border-color: var(--brand); background: var(--bg); box-shadow: 0 0 0 0 color-mix(in srgb, var(--brand) 55%, transparent); animation: pulse 1.8s ease-out infinite; }}
@keyframes pulse {{ 70% {{ box-shadow: 0 0 0 10px transparent; }} 100% {{ box-shadow: 0 0 0 0 transparent; }} }}
.mk {{ font: 600 12px/1 var(--mono); color: var(--muted); }}
.m.active .mk {{ color: var(--brand); }}
.m.done .mk {{ color: var(--done); }}
.sr {{ position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }}
.legend {{ display: flex; gap: 18px; flex-wrap: wrap; font-size: 13px; color: var(--muted); margin-bottom: 26px; }}
.legend i {{ display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; vertical-align: 0; }}
.ph {{ background: var(--surface); border: 1px solid var(--line); border-radius: 10px; margin-bottom: 10px; }}
.ph.active {{ border-color: color-mix(in srgb, var(--brand) 45%, var(--line)); }}
summary {{ display: grid; grid-template-columns: auto 1fr auto auto; gap: 4px 14px; align-items: center; padding: 14px 18px; cursor: pointer; list-style: none; }}
summary::-webkit-details-marker {{ display: none; }}
summary:focus-visible {{ outline: 2px solid var(--brand); outline-offset: 2px; border-radius: 10px; }}
.pn {{ font: 500 12px/1 var(--mono); color: var(--muted); text-transform: uppercase; letter-spacing: .06em; }}
.pt {{ font-weight: 500; min-width: 0; display: flex; flex-wrap: wrap; gap: 2px 10px; align-items: baseline; }}
.when {{ font: 12px var(--mono); color: var(--muted); }}
.pill {{ font: 600 11px/1 var(--body); padding: 5px 9px; border-radius: 99px; text-transform: uppercase; letter-spacing: .05em; }}
.pill.done {{ background: color-mix(in srgb, var(--done) 16%, transparent); color: var(--done); }}
.pill.active {{ background: color-mix(in srgb, var(--brand) 14%, transparent); color: var(--brand); }}
.pill.todo {{ background: color-mix(in srgb, var(--todo) 22%, transparent); color: var(--muted); }}
.cnt {{ font: 500 13px/1 var(--mono); font-variant-numeric: tabular-nums; color: var(--muted); }}
.mini {{ grid-column: 1 / -1; height: 4px; background: var(--brand-soft); border-radius: 2px; overflow: hidden; }}
.mini span {{ display: block; height: 100%; background: var(--done); }}
.ph.active .mini span {{ background: var(--brand); }}
.tasks {{ list-style: none; margin: 0; padding: 0 18px 10px; display: grid; gap: 2px; }}
.t {{ display: grid; grid-template-columns: 16px 40px 1fr auto; gap: 10px; align-items: baseline; padding: 6px 0; border-top: 1px solid var(--line); font-size: 14px; }}
.t .tx {{ min-width: 0; overflow-wrap: anywhere; }}
.box {{ width: 12px; height: 12px; border: 2px solid var(--todo); border-radius: 3px; transform: translateY(1px); }}
.t.done .box {{ background: var(--done); border-color: var(--done); }}
.t.done .tx {{ color: var(--muted); }}
.t.now .box {{ border-color: var(--brand); }}
.tid {{ font: 500 12px var(--mono); color: var(--muted); font-variant-numeric: tabular-nums; }}
.nowtag {{ font: 600 11px/1 var(--body); color: var(--brand); white-space: nowrap; }}
.gate, .note {{ margin: 0; padding: 0 18px 14px; font-size: 13px; color: var(--muted); max-width: 75ch; }}
.gate span {{ display: block; font: 500 11px/1.6 var(--mono); text-transform: uppercase; letter-spacing: .06em; }}
code {{ font: 12.5px var(--mono); background: color-mix(in srgb, var(--brand-soft) 60%, transparent); padding: 1px 4px; border-radius: 4px; }}
footer {{ margin-top: 28px; font: 12px var(--mono); color: var(--muted); }}
@media (max-width: 560px) {{ .mk {{ font-size: 10px; }} .dot {{ width: 18px; height: 18px; }} summary {{ grid-template-columns: 1fr auto; }} .pn {{ grid-column: 1 / -1; }} .t {{ grid-template-columns: 16px 34px 1fr; }} .nowtag {{ grid-column: 3; }} }}
@media (prefers-reduced-motion: reduce) {{ .fill, .m.active .dot {{ animation: none; }} }}
</style>
<div class="wrap">
  <div class="eyebrow">rommapp/argosy-launcher · Android → Linux Flatpak</div>
  <h1>Argosy Desktop progress</h1>
  <p class="sub">Tracks the phase table in <code>docs/desktop/handoff/README.md</code> and each phase's step plan in <code>DEVLOG.md</code>. Updated at the end of each work unit.</p>
  <div class="stats">
    <div class="big">{pct:g}<small>%</small></div>
    <div class="stat"><span>Steps</span><b>{done} of {total} done</b></div>
    <div class="stat"><span>Current phase</span><b>{active_txt}</b></div>
    <div class="stat"><span>Next step</span><b>{current_id or '—'}</b></div>
    <div class="stat"><span>Pacer</span><b>{pacer_txt}</b></div>
  </div>
  <div class="rail" role="img" aria-label="{pct:g} percent of steps complete">
    <div class="track"><div class="fill" style="--pct:{pct}%"></div></div>
    <ol class="markers">{''.join(markers)}</ol>
  </div>
  <div class="legend"><span><i style="background:var(--done)"></i>Complete</span><span><i style="background:var(--brand)"></i>In progress</span><span><i style="background:var(--todo)"></i>Not started</span></div>
  {''.join(sections)}
  <footer>Last updated {now}</footer>
</div>
'''
os.makedirs(os.path.dirname(out), exist_ok=True)
open(out, 'w', encoding='utf-8').write(page)
print(f'{done}/{total} = {pct}% · active: {active_txt} · next: {current_id} · {out}')
for p in phases:
    print(f'  P{p["n"]} {p["state"]:6} {sum(t["done"] for t in p["tasks"])}/{len(p["tasks"])} {"plan" if p["planned"] else "scope"}')
