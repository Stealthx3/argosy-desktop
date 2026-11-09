#!/usr/bin/env node
// postmortem.mjs — Post-Mortem Standard v1. Node >=18, no dependencies. Run from the repo root.
//   node scripts/postmortem.mjs mine   [--transcripts DIR]... [--since YYYY-MM-DD] [--no-transcripts]
//   node scripts/postmortem.mjs check  [docs/postmortem/findings.json]
//   node scripts/postmortem.mjs report [docs/postmortem/findings.json] [--html-only]
// mine   → docs/postmortem/metrics.json (sessions, tokens, tools, git, plan, journal, signals)
// check  → validates the findings register (every finding audited, every rec evaluated, every signal dispositioned)
// report → check, then docs/postmortem/report.html and docs/postmortem/POSTMORTEM-<date>-r<rev>.pdf
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const VERSION = 'v1';
const ROOT = process.cwd();
const PM_DIR = path.join(ROOT, 'docs/postmortem');
const METRICS = path.join(PM_DIR, 'metrics.json');
const FINDINGS = path.join(PM_DIR, 'findings.json');

const CATEGORIES = {
  efficiency: 'Process efficiency', logic: 'Logic & correctness', plan: 'Planning & scope',
  usage: 'Token & usage', tooling: 'Tooling & automation', environment: 'Environment & lab',
  communication: 'Communication & handoffs', quality: 'Quality & testing', standards: 'Standards & ai-assets',
};
const EVIDENCE_TYPES = ['commit', 'file', 'transcript', 'session', 'plan', 'journal', 'log', 'metric', 'signal', 'user', 'artifact'];
const VERDICTS = ['confirmed', 'plausible', 'rejected'];
const DECISIONS = ['proposed', 'accepted', 'declined', 'deferred', 'done'];
const TARGET_RE = /^(project|claude-md|workflow|user|standard:[A-Z0-9-]+|tool:[\w.-]+|new-standard:[A-Z0-9-]+)$/;

// ---------- helpers ----------
const args = process.argv.slice(2);
const cmd = args.shift();
const flag = (f) => args.includes(f);
const opt = (f) => { const i = args.indexOf(f); return i >= 0 ? args[i + 1] : undefined; };
const optAll = (f) => args.flatMap((a, i) => (a === f ? [args[i + 1]] : []));
const positional = () => args.find((a, i) => !a.startsWith('--') && !['--transcripts', '--since'].includes(args[i - 1]));
const readJSON = (p) => JSON.parse(fs.readFileSync(p, 'utf8'));
const run = (bin, a) => { const r = spawnSync(bin, a, { cwd: ROOT, encoding: 'utf8', maxBuffer: 64 << 20 }); return r.status === 0 ? r.stdout : null; };
const median = (xs) => { if (!xs.length) return 0; const s = [...xs].sort((a, b) => a - b); const m = s.length >> 1; return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2; };
const fmtInt = (n) => Math.round(n || 0).toLocaleString('en-US');
const day = (t) => (t ? new Date(t).toISOString().slice(0, 10) : '—');
const REDACT = [
  /-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----/g,
  /\b(?:sk|pk|rk)-[A-Za-z0-9_-]{16,}/g, /\bgh[pousr]_[A-Za-z0-9]{20,}/g, /\bAKIA[0-9A-Z]{16}\b/g,
  /\bxox[abpr]-[A-Za-z0-9-]{10,}/g, /\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g,
  /((?:password|passwd|secret|token|api[_-]?key)\s*[=:]\s*)\S+/gi,
];
const redact = (s) => REDACT.reduce((acc, re) => acc.replace(re, (m, p1) => (typeof p1 === 'string' && p1 && m.startsWith(p1) ? p1 + '[REDACTED]' : '[REDACTED]')), String(s ?? ''));
const esc = (s) => redact(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const rel = (f) => { const r = path.relative(ROOT, f); return !r || r.startsWith('..') ? f : r; };
const clip = (s, n = 160) => { s = String(s ?? '').replace(/\s+/g, ' ').trim(); return s.length > n ? s.slice(0, n - 1) + '…' : s; };

// ---------- mine ----------
function transcriptDirs() {
  if (flag('--no-transcripts')) return [];
  const given = optAll('--transcripts').filter(Boolean).map((d) => path.resolve(d.replace(/^~(?=\/|$)/, os.homedir())));
  if (given.length) return given;
  const base = path.join(os.homedir(), '.claude/projects');
  if (!fs.existsSync(base)) return [];
  const slug = ROOT.replace(/[^A-Za-z0-9]/g, '-');
  if (fs.existsSync(path.join(base, slug))) return [path.join(base, slug)];
  const tail = '-' + path.basename(ROOT).replace(/[^A-Za-z0-9]/g, '-');
  return fs.readdirSync(base).filter((d) => d.endsWith(tail)).map((d) => path.join(base, d));
}
function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out); else if (e.name.endsWith('.jsonl')) out.push(p);
  }
  return out;
}
function mineTranscript(file, since) {
  const s = { id: path.basename(file, '.jsonl'), file, surface: 'claude-code', subagent: /subagents?/.test(file), start: null, end: null,
    models: new Set(), tokens: { input: 0, output: 0, cacheRead: 0, cacheWrite: 0 }, tools: {}, toolErrors: 0, interrupts: 0, userMsgs: 0, calls: [], stamps: [] };
  const seenMsg = new Set(), seenTool = new Set(), results = new Map();
  for (const line of fs.readFileSync(file, 'utf8').split('\n')) {
    if (!line.trim()) continue;
    let o; try { o = JSON.parse(line); } catch { continue; }
    const ts = o.timestamp ? Date.parse(o.timestamp) : NaN;
    if (since && !Number.isNaN(ts) && ts < since) continue;
    if (!Number.isNaN(ts)) s.stamps.push(ts);
    if (o.isSidechain) s.subagent = true;
    if (o.sessionId && s.id === path.basename(file, '.jsonl')) s.id = o.sessionId;
    const m = o.message;
    if (!m) continue;
    const content = Array.isArray(m.content) ? m.content : [];
    if (o.type === 'assistant') {
      if (m.model) s.models.add(m.model);
      if (m.usage && !seenMsg.has(m.id)) {
        seenMsg.add(m.id);
        s.tokens.input += m.usage.input_tokens || 0; s.tokens.output += m.usage.output_tokens || 0;
        s.tokens.cacheRead += m.usage.cache_read_input_tokens || 0; s.tokens.cacheWrite += m.usage.cache_creation_input_tokens || 0;
      }
      for (const b of content) if (b.type === 'tool_use' && !seenTool.has(b.id)) {
        seenTool.add(b.id); s.tools[b.name] = (s.tools[b.name] || 0) + 1; s.calls.push({ id: b.id, name: b.name, input: b.input || {}, ts });
      }
    } else if (o.type === 'user') {
      const text = typeof m.content === 'string' ? m.content : content.filter((b) => b.type === 'text').map((b) => b.text).join('\n');
      for (const b of content) if (b.type === 'tool_result') results.set(b.tool_use_id, !!b.is_error);
      if (/\[Request interrupted by user/.test(text)) s.interrupts++;
      else if (text.trim() && !o.isMeta && !content.some((b) => b.type === 'tool_result')) s.userMsgs++;
    }
  }
  for (const c of s.calls) { c.error = results.get(c.id) === true; if (c.error) s.toolErrors++; }
  s.stamps.sort((a, b) => a - b);
  s.start = s.stamps[0] ?? null; s.end = s.stamps.at(-1) ?? null;
  return s;
}
function parsePlan(file) {
  if (!fs.existsSync(file)) return null;
  let phase = null; const phases = [];
  for (const line of fs.readFileSync(file, 'utf8').split('\n')) {
    const h = line.match(/^#{2,3}\s+Phase\s+(\d+)\s*[—–:-]\s*(.+)$/i);
    if (h) { phase = { n: +h[1], title: h[2].trim(), total: 0, done: 0 }; phases.push(phase); continue; }
    const t = line.match(/^- \[( |x|X)\]\s+/);
    if (t && phase) { phase.total++; if (t[1] !== ' ') phase.done++; }
  }
  const total = phases.reduce((a, p) => a + p.total, 0), done = phases.reduce((a, p) => a + p.done, 0);
  return { file: path.relative(ROOT, file), phases: phases.length, total, done, open: total - done };
}
function parseJournal(file) {
  if (!fs.existsSync(file)) return null;
  const entries = fs.readFileSync(file, 'utf8').split('\n').filter((l) => /^- \d{4}-\d{2}-\d{2}/.test(l)).map((l) => {
    const f = (k) => (l.match(new RegExp(`${k}:\\s*([^·]*)`, 'i')) || [])[1]?.trim() || '';
    return { line: l, date: l.slice(2, 12), surface: (l.split('·')[1] || '').trim(), issues: f('issues'), corrections: f('corrections') };
  });
  const isReal = (v) => !!v && !/^(none|n\/a|-|—)$/i.test(v);
  return { file: path.relative(ROOT, file), entries: entries.length, surfaces: [...new Set(entries.map((e) => e.surface).filter(Boolean))],
    withIssues: entries.filter((e) => isReal(e.issues) || isReal(e.corrections)).map((e) => ({ date: e.date, surface: e.surface, issues: isReal(e.issues) ? e.issues : '', corrections: isReal(e.corrections) ? e.corrections : '' })) };
}
function gitStats(sinceStr) {
  if (run('git', ['rev-parse', '--is-inside-work-tree']) === null) return null;
  const sinceArg = sinceStr ? [`--since=${sinceStr}`] : [];
  const log = run('git', ['log', ...sinceArg, '--date=iso-strict', '--pretty=format:%H%x09%ad%x09%s']) || '';
  const commits = log.split('\n').filter(Boolean).map((l) => { const [h, d, ...s] = l.split('\t'); return { hash: h.slice(0, 9), date: d, subject: s.join('\t') }; });
  const churn = {};
  for (const l of (run('git', ['log', ...sinceArg, '--numstat', '--pretty=tformat:']) || '').split('\n')) {
    const [a, d, f] = l.split('\t'); if (!f) continue; churn[f] = (churn[f] || 0) + (+a || 0) + (+d || 0);
  }
  return { commits: commits.length, first: commits.at(-1)?.date || null, last: commits[0]?.date || null,
    reverts: commits.filter((c) => /^Revert\b/.test(c.subject)), fixes: commits.filter((c) => /\b(fix(es|ed)?|fixup!?|hotfix|oops|typo|wip)\b/i.test(c.subject)).length,
    topChurn: Object.entries(churn).sort((a, b) => b[1] - a[1]).slice(0, 10).map(([file, lines]) => ({ file, lines })) };
}
function cmdMine() {
  const sinceStr = opt('--since');
  const since = sinceStr ? Date.parse(sinceStr) : null;
  const dirs = transcriptDirs();
  const sessions = dirs.flatMap((d) => (fs.existsSync(d) ? walk(d) : [])).map((f) => mineTranscript(f, since)).filter((s) => s.stamps.length);
  const signals = []; const add = (kind, weight, detail, session) => signals.push({ kind, weight, detail: clip(redact(detail), 200), session: session || null });
  const tools = {}; const edits = {};
  for (const s of sessions) {
    const reads = {}, cmds = {};
    for (const c of s.calls) {
      tools[c.name] ??= { calls: 0, errors: 0 }; tools[c.name].calls++; if (c.error) tools[c.name].errors++;
      const fp = c.input.file_path || c.input.notebook_path || c.input.path;
      if (c.name === 'Read' && fp) reads[fp] = (reads[fp] || 0) + 1;
      if (/^(Edit|Write|MultiEdit|NotebookEdit)$/.test(c.name) && fp) edits[fp] = (edits[fp] || 0) + 1;
      if (c.name === 'Bash' && c.input.command) { const k = c.input.command.trim(); cmds[k] = (cmds[k] || 0) + 1; }
    }
    for (const [f, n] of Object.entries(reads)) if (n >= 3) add('reread', n, `Read ${rel(f)} ${n}× in one session`, s.id);
    for (const [k, n] of Object.entries(cmds)) if (n >= 3) add('repeat-command', n, `Ran \`${k}\` ${n}× in one session`, s.id);
    let streak = 0, streakStart = null;
    for (const c of [...s.calls, { error: false }]) {
      if (c.error) { if (!streak) streakStart = c; streak++; continue; }
      if (streak >= 3) add('error-streak', streak, `${streak} consecutive failed tool calls starting with ${streakStart.name}: ${JSON.stringify(streakStart.input)}`, s.id);
      streak = 0;
    }
    for (let i = 1; i < s.stamps.length; i++) { const gap = (s.stamps[i] - s.stamps[i - 1]) / 60000; if (gap > 60) add('idle-gap', Math.round(gap / 60), `Idle ${Math.round(gap)} min mid-session (prompt cache expired) at ${new Date(s.stamps[i - 1]).toISOString()}`, s.id); }
    if (s.interrupts) add('interrupt', s.interrupts * 2, `User interrupted ${s.interrupts}× (possible wrong direction)`, s.id);
    const errRate = s.calls.length ? s.toolErrors / s.calls.length : 0;
    if (s.calls.length >= 20 && errRate > 0.15) add('error-rate', Math.round(errRate * 20), `${Math.round(errRate * 100)}% of ${s.calls.length} tool calls failed`, s.id);
    const agents = (s.tools.Task || 0) + (s.tools.Agent || 0);
    if (agents > 10) add('agent-fanout', Math.round(agents / 5), `${agents} subagents spawned in one session`, s.id);
  }
  for (const [f, n] of Object.entries(edits)) if (n >= 6) add('edit-churn', Math.round(n / 3), `${rel(f)} edited ${n}× across the project`);
  const tot = (s) => s.tokens.input + s.tokens.output + s.tokens.cacheRead + s.tokens.cacheWrite;
  const main = sessions.filter((s) => !s.subagent), med = median(main.map(tot));
  if (main.length >= 3) for (const s of main) if (tot(s) > 2 * med) add('heavy-session', Math.round(tot(s) / med), `Session used ${fmtInt(tot(s))} tokens (${(tot(s) / med).toFixed(1)}× median)`, s.id);
  const git = gitStats(sinceStr);
  for (const r of git?.reverts || []) add('revert', 3, `Revert commit ${r.hash}: ${r.subject}`);
  const plan = parsePlan(path.join(ROOT, 'docs/PLAN.md'));
  if (plan?.open) add('open-tasks', plan.open, `${plan.open} plan task(s) still unticked at post-mortem time`);
  const journal = parseJournal(path.join(PM_DIR, 'JOURNAL.md'));
  for (const e of journal?.withIssues || []) add('journal-issue', 3, `${e.date} ${e.surface}: ${[e.issues && 'issue: ' + e.issues, e.corrections && 'correction: ' + e.corrections].filter(Boolean).join(' / ')}`);
  if (!journal) add('no-journal', 5, 'docs/postmortem/JOURNAL.md missing: session history for non-Claude-Code work must be reconstructed');
  if (!sessions.length) add('no-transcripts', 2, 'No Claude Code transcripts found (Cowork-only project, or wrong --transcripts dir)');
  signals.sort((a, b) => b.weight - a.weight || a.kind.localeCompare(b.kind) || a.detail.localeCompare(b.detail));
  signals.forEach((s, i) => (s.id = `S-${String(i + 1).padStart(3, '0')}`));
  const totals = sessions.reduce((t, s) => { for (const k in s.tokens) t.tokens[k] += s.tokens[k]; t.calls += s.calls.length; t.errors += s.toolErrors; t.interrupts += s.interrupts; t.userMsgs += s.userMsgs; return t; },
    { tokens: { input: 0, output: 0, cacheRead: 0, cacheWrite: 0 }, calls: 0, errors: 0, interrupts: 0, userMsgs: 0 });
  const out = { standard: `POST-MORTEM-STANDARD ${VERSION}`, generated: new Date().toISOString(), root: ROOT, since: sinceStr || null, transcriptDirs: dirs,
    totals: { ...totals, sessions: main.length, subagentSessions: sessions.length - main.length, start: Math.min(...sessions.map((s) => s.start)) || null, end: Math.max(...sessions.map((s) => s.end)) || null },
    tools, git, plan, journal,
    sessions: sessions.map((s) => ({ id: s.id, surface: s.surface, subagent: s.subagent, start: s.start && new Date(s.start).toISOString(), end: s.end && new Date(s.end).toISOString(),
      minutes: s.start ? Math.round((s.end - s.start) / 60000) : 0, models: [...s.models], tokens: s.tokens, toolCalls: s.calls.length, toolErrors: s.toolErrors, interrupts: s.interrupts, userMsgs: s.userMsgs })),
    signals };
  if (!Number.isFinite(out.totals.start)) out.totals.start = out.totals.end = null;
  fs.mkdirSync(PM_DIR, { recursive: true });
  fs.writeFileSync(METRICS, JSON.stringify(out, null, 2));
  const T = totals.tokens;
  console.log(`mine: ${main.length} sessions (+${out.totals.subagentSessions} subagent) · ${fmtInt(totals.calls)} tool calls · ${fmtInt(totals.errors)} errors · ${fmtInt(T.input + T.output + T.cacheRead + T.cacheWrite)} tokens · git ${git ? git.commits + ' commits' : 'n/a'} · plan ${plan ? plan.done + '/' + plan.total : 'n/a'} · journal ${journal ? journal.entries : 'missing'}`);
  console.log(`mine: ${signals.length} signals → ${path.relative(ROOT, METRICS)}`);
  for (const s of signals.slice(0, 12)) console.log(`  ${s.id} ${s.kind.padEnd(15)} ${s.detail}`);
  if (signals.length > 12) console.log(`  … ${signals.length - 12} more in metrics.json`);
}

// ---------- check ----------
const score = (f, r) => Math.round(((f.impact.severity * r.benefit * (f.audit.confidence / 100)) / r.effort) * 10) / 10;
const priority = (s) => (s >= 5 ? 'P1' : s >= 2 ? 'P2' : 'P3');
function validate(F, M) {
  const errs = []; const e = (where, msg) => errs.push(`${where}: ${msg}`);
  const str = (v, min = 1) => typeof v === 'string' && v.trim().length >= min;
  const int = (v, lo, hi) => Number.isInteger(v) && v >= lo && v <= hi;
  if (!str(F.project)) e('root', 'project is required');
  if (!F.period || !/^\d{4}-\d{2}-\d{2}$/.test(F.period.start || '') || !/^\d{4}-\d{2}-\d{2}$/.test(F.period.end || '')) e('root', 'period.start/end must be YYYY-MM-DD');
  if (!Array.isArray(F.surfaces) || !F.surfaces.length) e('root', 'surfaces[] required (claude-code, cowork, …)');
  if (!str(F.summary, 80)) e('root', 'summary must be at least 80 characters');
  if (!int(F.revision ?? 1, 1, 99)) e('root', 'revision must be an integer 1–99');
  if (!Array.isArray(F.findings) || !F.findings.length) { e('root', 'findings[] must not be empty'); return errs; }
  const ids = new Set();
  for (const f of F.findings) {
    const w = f.id || '(no id)';
    if (!/^PM-\d{3}$/.test(f.id || '')) e(w, 'id must look like PM-001'); else if (ids.has(f.id)) e(w, 'duplicate id'); ids.add(f.id);
    if (!CATEGORIES[f.category]) e(w, `category must be one of ${Object.keys(CATEGORIES).join(', ')}`);
    for (const k of ['title', 'observation']) if (!str(f[k], k === 'observation' ? 40 : 5)) e(w, `${k} missing or too short`);
    if (!f.impact || !int(f.impact.severity, 1, 5) || !str(f.impact.cost)) e(w, 'impact.severity (1–5) and impact.cost required');
    const ev = Array.isArray(f.evidence) ? f.evidence : [];
    if (!ev.length) e(w, 'at least one evidence item required');
    if (f.impact?.severity >= 4 && ev.length < 2) e(w, 'severity ≥4 needs at least two independent evidence items');
    ev.forEach((x, i) => { if (!EVIDENCE_TYPES.includes(x.type)) e(`${w} evidence[${i}]`, `type must be one of ${EVIDENCE_TYPES.join(', ')}`); if (!str(x.ref)) e(`${w} evidence[${i}]`, 'ref required'); if (x.type === 'signal' && M && !M.signals.some((s) => s.id === x.ref)) e(`${w} evidence[${i}]`, `unknown signal ${x.ref}`); });
    const a = f.audit || {};
    if (!VERDICTS.includes(a.verdict)) e(w, `audit.verdict must be ${VERDICTS.join('/')}`);
    for (const k of ['auditor', 'method']) if (!str(a[k], 5)) e(w, `audit.${k} required`);
    if (!str(a.counterEvidence, 20)) e(w, 'audit.counterEvidence required: what was checked that could disprove this, and what was found');
    if (typeof a.independent !== 'boolean') e(w, 'audit.independent (true/false) required');
    if (!int(a.confidence, 0, 100)) e(w, 'audit.confidence must be an integer 0–100');
    if (a.independent === false && a.confidence > 70) e(w, 'self-audited findings are capped at confidence 70');
    if (a.verdict === 'confirmed' && a.confidence < 70) e(w, 'confirmed needs confidence ≥70 (else use plausible)');
    if (a.verdict === 'rejected') continue;
    if (!str(f.rootCause, 20)) e(w, 'rootCause required for confirmed/plausible findings');
    const recs = Array.isArray(f.recommendations) ? f.recommendations : [];
    if (!recs.length) e(w, 'at least one recommendation required');
    recs.forEach((r, i) => {
      const rw = r.id || `${w} rec[${i}]`;
      if (!new RegExp(`^${f.id}-R\\d+$`).test(r.id || '')) e(rw, `id must look like ${f.id}-R1`); else if (ids.has(r.id)) e(rw, 'duplicate id'); ids.add(r.id);
      if (!str(r.action, 15)) e(rw, 'action required');
      if (!TARGET_RE.test(r.target || '')) e(rw, 'target must be project | claude-md | workflow | user | standard:<NAME> | new-standard:<NAME> | tool:<name>');
      if (!int(r.benefit, 1, 5) || !int(r.effort, 1, 5)) e(rw, 'benefit and effort must be integers 1–5');
      for (const k of ['risk', 'alternatives', 'evaluation']) if (!str(r[k], 10)) e(rw, `${k} required (evaluate it, don't leave it blank)`);
      if (!DECISIONS.includes(r.decision)) e(rw, `decision must be ${DECISIONS.join('/')}`);
    });
  }
  if (M) {
    const disp = F.signalDispositions || {};
    for (const s of M.signals) {
      const d = disp[s.id];
      if (!d) e(s.id, `signal not dispositioned (${s.kind}: ${clip(s.detail, 60)})`);
      else if (/^PM-\d{3}$/.test(d)) { if (!ids.has(d)) e(s.id, `disposition points to unknown finding ${d}`); }
      else if (!/^(noise|expected|duplicate):\s*\S.{9,}/.test(d)) e(s.id, 'disposition must be a finding id or "noise: <reason>", "expected: <reason>", "duplicate: <reason>"');
    }
    for (const k of Object.keys(disp)) if (!M.signals.some((s) => s.id === k)) e(k, 'disposition for a signal that is not in metrics.json (re-run mine?)');
  }
  return errs;
}
function load(file) {
  const fp = path.resolve(file || FINDINGS);
  if (!fs.existsSync(fp)) { console.error(`postmortem: missing ${path.relative(ROOT, fp)} (see POST-MORTEM-STANDARD §9)`); process.exit(2); }
  const F = readJSON(fp);
  const mp = path.resolve(ROOT, F.metrics || path.relative(ROOT, METRICS));
  const M = fs.existsSync(mp) ? readJSON(mp) : null;
  return { F, M, fp };
}
function cmdCheck(quiet) {
  const { F, M, fp } = load(positional());
  const errs = validate(F, M);
  if (errs.length) { console.error(`check: ${errs.length} problem(s) in ${path.relative(ROOT, fp)}`); errs.slice(0, 40).forEach((x) => console.error('  ' + x)); if (errs.length > 40) console.error(`  … ${errs.length - 40} more`); process.exit(1); }
  if (!M) console.warn('check: warning: no metrics.json (signals not checked); run `mine` first');
  const recs = F.findings.flatMap((f) => f.recommendations || []);
  if (!quiet) console.log(`check: OK · ${F.findings.length} findings · ${recs.length} recommendations · ${M ? M.signals.length : 0} signals dispositioned`);
  return { F, M };
}

// ---------- report ----------
const cssStr = (s) => '"' + redact(s).replace(/[\\"]/g, '\\$&').replace(/[\n\r]/g, ' ').replace(/</g, '\\3C ') + '"';
const CSS = (project) => `
@page{size:Letter;margin:.75in .7in .8in;@bottom-left{content:${cssStr('Post-mortem · ' + project)};font:9px monospace;color:#6d655e}@bottom-right{content:counter(page) " / " counter(pages);font:9px monospace;color:#6d655e}}
@page:first{@bottom-left{content:none}@bottom-right{content:none}}
:root{--bg:#ffffff;--fg:#1d1a18;--muted:#6d655e;--line:#ddd5c4;--brand:#700908;--soft:#f3efe4;--done:#2f6b3a;--warn:#8a5a00;--bad:#8b1d1d;
--display:Georgia,"Times New Roman",serif;--body:"Helvetica Neue",Helvetica,Arial,"Liberation Sans",sans-serif;--mono:"DejaVu Sans Mono",Menlo,Consolas,monospace}
*{box-sizing:border-box}html{-webkit-print-color-adjust:exact;print-color-adjust:exact}
body{margin:0;background:var(--bg);color:var(--fg);font:10.5px/1.5 var(--body);font-variant-numeric:tabular-nums}
h1{font:italic 700 34px/1.15 var(--display);color:var(--brand);margin:0 0 10px}
h2{font:italic 700 20px/1.2 var(--display);color:var(--brand);margin:0 0 10px;padding-bottom:6px;border-bottom:1px solid var(--line)}
h3{font-size:12.5px;margin:18px 0 6px}h4{font:600 9px var(--mono);text-transform:uppercase;letter-spacing:.06em;color:var(--muted);margin:10px 0 3px}
p{margin:0 0 8px}.muted{color:var(--muted)}.mono{font-family:var(--mono)}
.eyebrow{font:500 10px var(--mono);letter-spacing:.08em;text-transform:uppercase;color:var(--muted);margin-bottom:14px}
section{break-before:page}section.cover{break-before:auto;padding-top:1.4in}
.kv{display:grid;grid-template-columns:150px 1fr;gap:5px 16px;margin:22px 0;padding:14px 16px;border:1px solid var(--line);border-radius:8px;background:var(--soft)}
.kv dt{font:500 9.5px var(--mono);text-transform:uppercase;color:var(--muted)}.kv dd{margin:0}
.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:14px 0}
.stat{border:1px solid var(--line);border-radius:8px;padding:10px 12px}.stat b{display:block;font-size:20px}.stat span{font:9px var(--mono);text-transform:uppercase;color:var(--muted)}
table{width:100%;border-collapse:collapse;margin:6px 0 12px;font-size:9.5px}th,td{text-align:left;padding:4px 6px;border-bottom:1px solid var(--line);vertical-align:top}
th{font:600 8.5px var(--mono);text-transform:uppercase;color:var(--muted);border-bottom:1.5px solid var(--fg)}tr{break-inside:avoid}td.n,th.n{text-align:right}td.mono{white-space:nowrap}
.cat{break-inside:avoid-page}.cat h3{margin-top:4px}
.pill{display:inline-block;font:600 8.5px var(--mono);text-transform:uppercase;padding:1px 7px;border-radius:9px;border:1px solid currentColor}
.confirmed{color:var(--done)}.plausible{color:var(--warn)}.rejected{color:var(--muted)}.P1{color:var(--bad)}.P2{color:var(--warn)}.P3{color:var(--muted)}
.finding{border:1px solid var(--line);border-radius:10px;padding:12px 14px;margin:0 0 14px;break-inside:avoid-page}
.finding header{display:flex;gap:8px;align-items:baseline;flex-wrap:wrap;margin-bottom:6px}.finding header b{font-size:12px}
.audit{background:var(--soft);border-left:3px solid var(--brand);padding:6px 10px;margin:8px 0;border-radius:0 6px 6px 0}
.rec{border-top:1px dashed var(--line);padding-top:6px;margin-top:8px;break-inside:avoid}
.rec table{margin:4px 0 0}.rec td:first-child{width:110px;font:500 8.5px var(--mono);text-transform:uppercase;color:var(--muted)}
ol.ev{margin:0 0 6px;padding-left:18px}ol.ev li{margin:1px 0}.tag{font:500 8.5px var(--mono);color:var(--brand);text-transform:uppercase;margin-right:4px}
.method li{margin:3px 0}`;
function buildHTML(F, M) {
  const live = F.findings.filter((f) => f.audit.verdict !== 'rejected');
  const rejected = F.findings.filter((f) => f.audit.verdict === 'rejected');
  const recs = live.flatMap((f) => f.recommendations.map((r) => ({ ...r, f, score: score(f, r) }))).sort((a, b) => b.score - a.score || a.id.localeCompare(b.id));
  recs.forEach((r) => (r.priority = priority(r.score)));
  const recById = Object.fromEntries(recs.map((r) => [r.id, r]));
  const count = (xs, k) => xs.filter(k).length;
  const T = M?.totals; const tok = T ? T.tokens.input + T.tokens.output + T.tokens.cacheRead + T.tokens.cacheWrite : 0;
  const pill = (cls, txt) => `<span class="pill ${cls}">${esc(txt ?? cls)}</span>`;
  const recTable = (rows) => `<table><tr><th>Pri</th><th>ID</th><th>Action</th><th>Target</th><th class="n">Score</th><th>Decision</th></tr>${rows.map((r) => `<tr><td>${pill(r.priority)}</td><td class="mono">${esc(r.id)}</td><td>${esc(r.action)}</td><td class="mono">${esc(r.target)}</td><td class="n">${r.score}</td><td>${esc(r.decision)}</td></tr>`).join('')}</table>`;
  const cover = `<section class="cover"><div class="eyebrow">Post-mortem · ${esc(F.project)}</div><h1>Project post-mortem</h1>
<p class="muted">${esc(F.subtitle || 'Process review after the final phase')}</p>
<dl class="kv"><dt>Project</dt><dd>${esc(F.project)}</dd><dt>Period</dt><dd>${esc(F.period.start)} → ${esc(F.period.end)}</dd><dt>Surfaces</dt><dd>${esc(F.surfaces.join(', '))}</dd>
<dt>Findings</dt><dd>${count(F.findings, (f) => f.audit.verdict === 'confirmed')} confirmed · ${count(F.findings, (f) => f.audit.verdict === 'plausible')} plausible · ${rejected.length} rejected</dd>
<dt>Recommendations</dt><dd>${count(recs, (r) => r.priority === 'P1')} P1 · ${count(recs, (r) => r.priority === 'P2')} P2 · ${count(recs, (r) => r.priority === 'P3')} P3</dd>
<dt>Signals reviewed</dt><dd>${M ? M.signals.length : 0}</dd><dt>Revision</dt><dd>r${F.revision ?? 1} · generated ${new Date().toISOString().slice(0, 10)}</dd><dt>Standard</dt><dd>POST-MORTEM-STANDARD ${VERSION}</dd></dl></section>`;
  const summary = `<section><h2>1. Executive summary</h2>${redact(F.summary).split(/\n{2,}/).map((p) => `<p>${esc(p)}</p>`).join('')}
<h3>Top recommendations</h3>${recTable(recs.slice(0, 6))}<p class="muted">Score = severity × benefit × confidence ÷ effort (P1 ≥ 5, P2 ≥ 2, else P3). Full register in §5.</p></section>`;
  let glance = `<section><h2>2. Project at a glance</h2>`;
  if (M) {
    glance += `<div class="stats"><div class="stat"><b>${fmtInt(T.sessions)}</b><span>Claude Code sessions</span></div><div class="stat"><b>${(tok / 1e6).toFixed(1)}M</b><span>tokens (all kinds)</span></div>
<div class="stat"><b>${fmtInt(T.calls)}</b><span>tool calls</span></div><div class="stat"><b>${T.calls ? Math.round((T.errors / T.calls) * 1000) / 10 : 0}%</b><span>tool error rate</span></div></div>
<table><tr><th>Measure</th><th>Value</th></tr>
<tr><td>Transcript span</td><td>${day(T.start)} → ${day(T.end)}</td></tr><tr><td>Subagent sessions</td><td>${fmtInt(T.subagentSessions)}</td></tr>
<tr><td>Tokens: input / output</td><td>${fmtInt(T.tokens.input)} / ${fmtInt(T.tokens.output)}</td></tr><tr><td>Tokens: cache read / write</td><td>${fmtInt(T.tokens.cacheRead)} / ${fmtInt(T.tokens.cacheWrite)}</td></tr>
<tr><td>User messages / interrupts</td><td>${fmtInt(T.userMsgs)} / ${fmtInt(T.interrupts)}</td></tr>
<tr><td>Git</td><td>${M.git ? `${M.git.commits} commits · ${M.git.reverts.length} reverts · ${M.git.fixes} fix-type commits · ${day(M.git.first)} → ${day(M.git.last)}` : 'not a git repo'}</td></tr>
<tr><td>Plan</td><td>${M.plan ? `${M.plan.done}/${M.plan.total} tasks · ${M.plan.phases} phases (${esc(M.plan.file)})` : 'no docs/PLAN.md'}</td></tr>
<tr><td>Journal</td><td>${M.journal ? `${M.journal.entries} entries · surfaces: ${esc(M.journal.surfaces.join(', ') || '—')}` : 'missing'}</td></tr></table>`;
    const tl = Object.entries(M.tools).sort((a, b) => b[1].calls - a[1].calls).slice(0, 12);
    if (tl.length) glance += `<h3>Tool use</h3><table><tr><th>Tool</th><th class="n">Calls</th><th class="n">Errors</th><th class="n">Error %</th></tr>${tl.map(([n, t]) => `<tr><td class="mono">${esc(n)}</td><td class="n">${fmtInt(t.calls)}</td><td class="n">${fmtInt(t.errors)}</td><td class="n">${Math.round((t.errors / t.calls) * 1000) / 10}</td></tr>`).join('')}</table>`;
    if (M.git?.topChurn?.length) glance += `<h3>Most-changed files (git)</h3><table><tr><th>File</th><th class="n">Lines changed</th></tr>${M.git.topChurn.slice(0, 8).map((c) => `<tr><td class="mono">${esc(c.file)}</td><td class="n">${fmtInt(c.lines)}</td></tr>`).join('')}</table>`;
  } else glance += `<p class="muted">No metrics.json was available; figures in this report come from the findings register only.</p>`;
  if (F.timeline?.length) glance += `<h3>Timeline</h3><table><tr><th>Date</th><th>Surface</th><th>Event</th></tr>${F.timeline.map((t) => `<tr><td class="mono">${esc(t.date)}</td><td>${esc(t.surface || '')}</td><td>${esc(t.event)}</td></tr>`).join('')}</table>`;
  glance += `</section>`;
  const method = `<section><h2>3. Method</h2><ol class="method">
<li><b>Evidence.</b> Claude Code transcripts, git history, the plan, the usage and lab records, and the session journal (the only record of Cowork sessions) were mined with <span class="mono">postmortem.mjs mine</span> into ${M ? M.signals.length : 0} machine signals.</li>
<li><b>Analysis.</b> The work was reviewed through nine lenses: ${Object.values(CATEGORIES).join(', ')}.</li>
<li><b>Audit.</b> Every observation was handed, with its evidence but not its conclusion, to an auditor who tried to disprove it. Verdicts: <b>confirmed</b> (evidence holds, confidence ≥ 70), <b>plausible</b> (supported but not proven), <b>rejected</b> (disproved or unsupported; listed in Appendix A). Self-audits are capped at confidence 70.</li>
<li><b>Evaluation.</b> Every recommendation states benefit and effort (1–5), risks, the alternatives considered and why this option was chosen. Score = severity × benefit × (confidence ÷ 100) ÷ effort.</li>
<li><b>Completeness.</b> Every machine signal is tied to a finding or explicitly dismissed with a reason (Appendix B). The report cannot be generated otherwise.</li></ol>
${F.methodNotes ? `<h3>Notes and limits</h3><p>${esc(F.methodNotes)}</p>` : ''}</section>`;
  let findings = `<section><h2>4. Findings</h2>`;
  for (const [cat, label] of Object.entries(CATEGORIES)) {
    const fs_ = live.filter((f) => f.category === cat); if (!fs_.length) continue;
    fs_.forEach((f, i) => {
      findings += (i === 0 ? `<div class="cat"><h3>${esc(label)}</h3>` : '') + `<article class="finding"><header><span class="mono">${esc(f.id)}</span><b>${esc(f.title)}</b>${pill(f.audit.verdict)}<span class="muted mono">severity ${f.impact.severity}/5 · confidence ${f.audit.confidence}%</span></header>
<h4>Observation</h4><p>${esc(f.observation)}</p><h4>Impact</h4><p>${esc(f.impact.cost)}</p>
<h4>Evidence</h4><ol class="ev">${f.evidence.map((x) => `<li><span class="tag">${esc(x.type)}</span><span class="mono">${esc(x.ref)}</span>${x.detail ? ` — ${esc(x.detail)}` : ''}</li>`).join('')}</ol>
<h4>Root cause</h4><p>${esc(f.rootCause)}</p>
<div class="audit"><h4>Audit · ${esc(f.audit.auditor)}${f.audit.independent ? ' (independent)' : ' (self-audit)'}</h4><p><b>Method:</b> ${esc(f.audit.method)}</p><p><b>Counter-evidence sought:</b> ${esc(f.audit.counterEvidence)}</p></div>
${f.recommendations.map((r0) => { const r = recById[r0.id]; return `<div class="rec"><b class="mono">${esc(r.id)}</b> ${pill(r.priority)} <b>${esc(r.action)}</b><table>
<tr><td>Target</td><td class="mono">${esc(r.target)}</td></tr><tr><td>Benefit / effort</td><td>${r.benefit}/5 · ${r.effort}/5 → score ${r.score}</td></tr>
<tr><td>Risk</td><td>${esc(r.risk)}</td></tr><tr><td>Alternatives</td><td>${esc(r.alternatives)}</td></tr><tr><td>Evaluation</td><td>${esc(r.evaluation)}</td></tr><tr><td>Decision</td><td>${esc(r.decision)}${r.decisionNote ? ` — ${esc(r.decisionNote)}` : ''}</td></tr></table></div>`; }).join('')}</article>` + (i === 0 ? '</div>' : '');
    });
  }
  findings += `</section>`;
  const register = `<section><h2>5. Recommendation register</h2><p class="muted">All recommendations from confirmed and plausible findings, ranked by score.</p>${recTable(recs)}</section>`;
  const apxA = `<section><h2>Appendix A — Rejected observations</h2>${rejected.length ? `<table><tr><th>ID</th><th>Observation</th><th>Why rejected</th><th class="n">Conf.</th></tr>${rejected.map((f) => `<tr><td class="mono">${esc(f.id)}</td><td><b>${esc(f.title)}</b><br>${esc(f.observation)}</td><td>${esc(f.audit.counterEvidence)}</td><td class="n">${f.audit.confidence}</td></tr>`).join('')}</table>` : '<p class="muted">No observations were rejected.</p>'}</section>`;
  const disp = F.signalDispositions || {};
  const apxB = `<section><h2>Appendix B — Signal dispositions</h2>${M?.signals.length ? `<table><tr><th>Signal</th><th>Kind</th><th>Detail</th><th>Disposition</th></tr>${M.signals.map((s) => `<tr><td class="mono">${esc(s.id)}</td><td class="mono">${esc(s.kind)}</td><td>${esc(s.detail)}</td><td>${esc(disp[s.id])}</td></tr>`).join('')}</table>` : '<p class="muted">No machine signals.</p>'}</section>`;
  const sess = (M?.sessions || []).filter((s) => !s.subagent).sort((a, b) => (a.start || '').localeCompare(b.start || ''));
  const tt = (s) => s.tokens.input + s.tokens.output + s.tokens.cacheRead + s.tokens.cacheWrite;
  const apxC = sess.length ? `<section><h2>Appendix C — Sessions</h2><table><tr><th>Start</th><th>Session</th><th class="n">Min</th><th class="n">Tokens</th><th class="n">Calls</th><th class="n">Errors</th><th>Model</th></tr>${sess.slice(0, 80).map((s) => `<tr><td class="mono">${esc((s.start || '').slice(0, 16).replace('T', ' '))}</td><td class="mono">${esc(s.id.slice(0, 8))}</td><td class="n">${s.minutes}</td><td class="n">${fmtInt(tt(s))}</td><td class="n">${s.toolCalls}</td><td class="n">${s.toolErrors}</td><td class="mono">${esc(s.models.join(', '))}</td></tr>`).join('')}</table>${sess.length > 80 ? `<p class="muted">${sess.length - 80} more sessions in metrics.json.</p>` : ''}</section>` : '';
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>${esc(F.project)} Post-mortem</title><style>${CSS(F.project)}</style></head><body>${cover}${summary}${glance}${method}${findings}${register}${apxA}${apxB}${apxC}</body></html>`;
}
function which(bin) { for (const d of (process.env.PATH || '').split(':')) { const p = path.join(d, bin); try { fs.accessSync(p, fs.constants.X_OK); return p; } catch {} } return null; }
async function renderPDF(htmlFile, pdfFile) {
  const req = createRequire(path.join(ROOT, 'noop.js'));
  let pw = null; try { pw = req(req.resolve('playwright')); } catch {}
  const bins = [process.env.PM_BROWSER, 'chromium', 'chromium-browser', 'google-chrome', 'google-chrome-stable', 'microsoft-edge', 'brave-browser'].filter(Boolean);
  const bin = bins.map((b) => (b.includes('/') ? b : which(b))).find(Boolean);
  if (!bin && pw) {
    const browser = await pw.chromium.launch();
    try { const page = await browser.newPage(); await page.goto(pathToFileURL(htmlFile).href); await page.pdf({ path: pdfFile, preferCSSPageSize: true, printBackground: true }); } finally { await browser.close(); }
    return 'playwright';
  }
  if (!bin) throw new Error('no Chromium-family browser found. Set PM_BROWSER=/path/to/chrome, or open report.html and Print → Save as PDF');
  // Render inside a visible home subfolder: snap/flatpak browsers can't read /mnt, /tmp or hidden dirs.
  const work = fs.mkdtempSync(path.join(os.homedir(), 'pm-render-'));
  try {
    const h = path.join(work, 'report.html'), p = path.join(work, 'report.pdf');
    fs.copyFileSync(htmlFile, h);
    const sandbox = process.getuid?.() === 0 ? ['--no-sandbox'] : []; // Chrome refuses to run as root with its sandbox
    const r = spawnSync(bin, [...sandbox, '--headless=new', '--disable-gpu', '--no-first-run', '--no-pdf-header-footer', '--print-to-pdf-no-header', '--run-all-compositor-stages-before-draw', `--print-to-pdf=${p}`, pathToFileURL(h).href], { encoding: 'utf8', timeout: 120000 });
    if (!fs.existsSync(p)) throw new Error(`${bin} did not produce a PDF: ${clip(r.stderr, 300)}`);
    fs.copyFileSync(p, pdfFile);
  } finally { fs.rmSync(work, { recursive: true, force: true }); }
  return path.basename(bin);
}
async function cmdReport() {
  const { F, M } = cmdCheck(true);
  const html = buildHTML(F, M);
  fs.mkdirSync(PM_DIR, { recursive: true });
  const htmlFile = path.join(PM_DIR, 'report.html');
  fs.writeFileSync(htmlFile, html);
  if (flag('--html-only')) { console.log(`report: ${path.relative(ROOT, htmlFile)} (html only)`); return; }
  const pdfFile = path.join(PM_DIR, `POSTMORTEM-${F.period.end}-r${F.revision ?? 1}.pdf`);
  const via = await renderPDF(htmlFile, pdfFile);
  const buf = fs.readFileSync(pdfFile, 'latin1');
  const pages = (buf.match(/\/Type\s*\/Page(?![s\w])/g) || []).length;
  const live = F.findings.filter((f) => f.audit.verdict !== 'rejected');
  const recs = live.flatMap((f) => f.recommendations.map((r) => priority(score(f, r))));
  console.log(`report: ${path.relative(ROOT, pdfFile)} · ${pages} pages · via ${via}`);
  console.log(`report: ${live.length} findings (${F.findings.length - live.length} rejected) · recs P1 ${recs.filter((p) => p === 'P1').length} / P2 ${recs.filter((p) => p === 'P2').length} / P3 ${recs.filter((p) => p === 'P3').length}`);
}

try {
  if (cmd === 'mine') cmdMine();
  else if (cmd === 'check') cmdCheck(false);
  else if (cmd === 'report') await cmdReport();
  else { console.log(fs.readFileSync(new URL(import.meta.url), 'utf8').split('\n').slice(1, 9).join('\n')); process.exit(2); }
} catch (err) { console.error(`postmortem: ${err.message}`); process.exit(1); }
