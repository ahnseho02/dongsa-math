"use strict";

/* ════════════ 기본 도구 ════════════ */

const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g,
    (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

const SESSION_KEY = "dongsa.session.v1";
let session = null;          // { token, user }
let state = {};              // 화면마다 필요한 것
let toastTimer = null;

function loadSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) { return null; }
}
function saveSession(value) {
  session = value;
  try {
    if (value) localStorage.setItem(SESSION_KEY, JSON.stringify(value));
    else localStorage.removeItem(SESSION_KEY);
  } catch (e) { /* 사파리 시크릿 모드 등 — 저장 못 해도 이번 세션은 돌아간다 */ }
}

function toast(message, bad) {
  const old = document.querySelector(".toast");
  if (old) old.remove();
  const node = document.createElement("div");
  node.className = "toast" + (bad ? " bad" : "");
  node.textContent = message;
  document.body.appendChild(node);
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => node.remove(), 3800);
}

/** 서버가 준 한국어 메시지를 그대로 보여 준다. 우리가 다시 번역하지 않는다. */
class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || "잠시 문제가 생겼습니다. 다시 해 주세요.");
    this.status = status;
    this.code = body?.code;
    this.fields = body?.fields;
  }
}

async function api(method, path, body) {
  const options = { method, headers: {} };
  if (session?.token) options.headers["Authorization"] = "Bearer " + session.token;
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  let response;
  try {
    response = await fetch(path, options);
  } catch (e) {
    throw new ApiError(0, { message: "서버에 연결하지 못했습니다. 인터넷을 확인해 주세요." });
  }
  const text = await response.text();
  const parsed = text ? JSON.parse(text) : null;
  if (!response.ok) {
    if (response.status === 401 && session) {   // 토큰이 만료됐다
      saveSession(null);
      go({ screen: "auth", mode: "teacher" });
      throw new ApiError(401, { message: "다시 로그인해 주세요." });
    }
    throw new ApiError(response.status, parsed);
  }
  return parsed;
}

/** 화면 전환. 렌더는 한 곳에서만 일어난다. */
function go(next) {
  if (next.view !== "game" && typeof stopTimer === "function") stopTimer();
  state = next;
  render();
  window.scrollTo(0, 0);
}

async function guard(fn) {
  try { await fn(); }
  catch (e) { toast(e.message, true); }
}

const STEP_LABELS = { CUE: "단서", OPERATION: "연산", EXPRESSION: "식", ANSWER: "답" };
const STEP_ORDER = ["CUE", "OPERATION", "EXPRESSION", "ANSWER"];
const OPS = ["+", "−", "×", "÷"];

function barRow(label, percent) {
  const cls = percent >= 80 ? "high" : (percent < 50 ? "low" : "");
  return `<div class="bar-row"><span>${esc(label)}</span>
    <span class="bar-track"><span class="bar-fill ${cls}" style="width:${percent}%"></span></span>
    <span class="bar-pct">${percent}%</span></div>`;
}

function statusBadge(status) {
  if (status === "DONE") return '<span class="badge done">다 풀었어요</span>';
  if (status === "IN_PROGRESS") return '<span class="badge doing">푸는 중</span>';
  return '<span class="badge todo">아직</span>';
}

/* ════════════ 렌더 ════════════ */

function render() {
  const who = $("who");
  if (session) {
    who.hidden = false;
    who.innerHTML = `${esc(session.user.name)}<button type="button" id="logout">나가기</button>`;
    $("logout").onclick = () => { saveSession(null); go({ screen: "auth", mode: "teacher" }); };
  } else {
    who.hidden = true;
  }

  const screens = {
    auth: authScreen,
    teacher: teacherScreen,
    student: studentScreen
  };
  draw(screens[state.screen] || authScreen);
}

/**
 * 화면 하나를 그린다.
 * 그리다 터지면 직전 화면이 그대로 남아 "눌러도 아무 일도 안 일어난다"처럼 보인다.
 * 그게 제일 고치기 어려운 실패라서, 무엇이 잘못됐는지 화면에 내놓는다.
 */
function draw(view) {
  try {
    view();
  } catch (e) {
    console.error(e);
    $("main").innerHTML = `<div class="sec"><div class="empty" style="color:var(--bad)">
        화면을 그리지 못했습니다.<br><span class="mono" style="font-size:11px">${esc(e.message)}</span>
      </div></div>`;
    $("dock").innerHTML = `<button class="btn ghost" id="reload">처음으로</button>`;
    $("reload").onclick = () => location.reload();
  }
}

/* ──────── 로그인 ──────── */

function authScreen() {
  const mode = state.mode || "teacher";
  const tab = (key, label) =>
    `<button type="button" data-mode="${key}" aria-selected="${mode === key}">${label}</button>`;

  let form;
  if (mode === "signup") {
    form = `
      <div class="sec">
        <h1 style="font-size:20px;font-weight:900">학원 만들기</h1>
        <p class="lead" style="margin-top:8px">가입하면 학원이 같이 만들어지고, 아이들이 쓸 <b>학원 코드</b>가 나옵니다.</p>
      </div>
      <div class="field"><label for="f-academy">학원 이름</label>
        <input class="input" id="f-academy" placeholder="동사수학학원" autocomplete="organization"></div>
      <div class="field"><label for="f-name">선생님 이름</label>
        <input class="input" id="f-name" placeholder="고모" autocomplete="name"></div>
      <div class="field"><label for="f-email">이메일</label>
        <input class="input" id="f-email" type="email" placeholder="gomo@example.com" autocomplete="email"></div>
      <div class="field"><label for="f-password">비밀번호 (8자 이상)</label>
        <input class="input" id="f-password" type="password" autocomplete="new-password"></div>`;
  } else if (mode === "teacher") {
    form = `
      <div class="field"><label for="f-email">이메일</label>
        <input class="input" id="f-email" type="email" placeholder="gomo@example.com" autocomplete="email"></div>
      <div class="field"><label for="f-password">비밀번호</label>
        <input class="input" id="f-password" type="password" autocomplete="current-password"></div>
      <p class="hint">처음이신가요? <button type="button" class="chip" id="to-signup"
         style="padding:4px 10px;font-size:12px">학원 만들기</button></p>`;
  } else {
    form = `
      <div class="field"><label for="f-code">학원 코드</label>
        <input class="input code" id="f-code" maxlength="6" placeholder="AB12CD" autocapitalize="characters"
               autocomplete="off" spellcheck="false"></div>
      <div class="field"><label for="f-sname">내 이름</label>
        <input class="input" id="f-sname" placeholder="김민우" autocomplete="off"></div>
      <div class="field"><label for="f-pin">비밀번호 네 자리</label>
        <input class="input pin" id="f-pin" maxlength="4" inputmode="numeric" pattern="\\d{4}"
               placeholder="••••" autocomplete="off"></div>
      <p class="hint">학원 코드와 비밀번호는 선생님이 알려 줍니다.</p>`;
  }

  $("main").innerHTML = `
    <div class="tabs">${tab("teacher", "선생님")}${tab("student", "학생")}</div>
    ${form}`;

  $("main").querySelectorAll("[data-mode]").forEach((b) => {
    b.onclick = () => go({ screen: "auth", mode: b.dataset.mode });
  });
  const signupLink = $("to-signup");
  if (signupLink) signupLink.onclick = () => go({ screen: "auth", mode: "signup" });

  const labels = { teacher: "로그인", student: "들어가기", signup: "학원 만들고 시작하기" };
  $("dock").innerHTML = `<button class="btn" id="go">${labels[mode]}</button>`;
  $("go").onclick = () => guard(submitAuth);

  $("main").querySelectorAll(".input").forEach((input) => {
    input.onkeydown = (e) => { if (e.key === "Enter") $("go").click(); };
  });
}

async function submitAuth() {
  const mode = state.mode || "teacher";
  let result;
  if (mode === "signup") {
    result = await api("POST", "/api/auth/signup", {
      academyName: $("f-academy").value.trim(),
      name: $("f-name").value.trim(),
      email: $("f-email").value.trim(),
      password: $("f-password").value
    });
  } else if (mode === "teacher") {
    result = await api("POST", "/api/auth/login", {
      email: $("f-email").value.trim(),
      password: $("f-password").value
    });
  } else {
    result = await api("POST", "/api/auth/student/login", {
      academyCode: $("f-code").value.trim(),
      name: $("f-sname").value.trim(),
      pin: $("f-pin").value.trim()
    });
  }
  saveSession({ token: result.token, user: result.user });
  enterHome();
}

function enterHome() {
  if (session.user.kind === "TEACHER") {
    guard(() => openTeacher("students"));
  } else {
    guard(openStudentList);
  }
}

/* ════════════ 선생님 ════════════ */

async function openTeacher(tab) {
  if (tab === "students") {
    const [academy, students] = await Promise.all([
      api("GET", "/api/academy"),
      api("GET", "/api/students")
    ]);
    go({ screen: "teacher", tab, academy, students });
  } else if (tab === "scores") {
    const [rows, ranking] = await Promise.all([
      api("GET", "/api/students/report"),
      api("GET", "/api/games/ranking")
    ]);
    go({ screen: "teacher", tab, rows, ranking });
  } else {
    const assignments = await api("GET", "/api/assignments");
    go({ screen: "teacher", tab: "assignments", assignments });
  }
}

/** 학생 한 명 — 성적과 관리를 한 화면에서. */
async function openStudent(studentId) {
  const [academy, report] = await Promise.all([
    api("GET", "/api/academy"),
    api("GET", `/api/students/${studentId}/report`)
  ]);
  go({ screen: "teacher", tab: "student", academy, report });
}

function teacherScreen() {
  const views = {
    students: teacherStudents,
    assignments: teacherAssignments,
    scores: teacherScores,
    addStudent: teacherAddStudent,
    student: teacherStudentDetail,
    pinReveal: teacherPinReveal,
    confirmDelete: teacherConfirmDelete,
    newAssignment: teacherNewAssignment,
    detail: teacherAssignmentDetail
  };
  draw(views[state.tab] || teacherStudents);
}

function teacherTabs(current) {
  return `<div class="tabs">
      <button type="button" data-tab="students" aria-selected="${current === "students"}">학생</button>
      <button type="button" data-tab="assignments" aria-selected="${current === "assignments"}">과제</button>
      <button type="button" data-tab="scores" aria-selected="${current === "scores"}">성적</button>
    </div>`;
}
function wireTabs() {
  $("main").querySelectorAll("[data-tab]").forEach((b) => {
    b.onclick = () => guard(() => openTeacher(b.dataset.tab));
  });
}

function teacherStudents() {
  const { academy, students } = state;
  const rows = students.length ? students.map((s) => `
      <button class="item" data-student="${s.id}">
        <span><span class="t">${esc(s.name)}</span>
          <span class="s">초${s.grade}학년${s.classroomName ? " · " + esc(s.classroomName) : ""}${s.active ? "" : " · 중지됨"}</span></span>
        <span class="r muted" style="font-size:12px">성적 · 관리 ›</span>
      </button>`).join("")
    : `<div class="empty">아직 등록한 학생이 없습니다.<br>아래 버튼으로 추가하세요.</div>`;

  $("main").innerHTML = `
    ${teacherTabs("students")}
    <div class="sec">
      <span class="eyebrow">아이들에게 알려 줄 학원 코드</span>
      <div class="codebig"><div class="c">${esc(academy.code)}</div>
        <p class="hint" style="margin-top:8px">${esc(academy.name)} · 학생 ${academy.studentCount}명</p></div>
    </div>
    <div class="sec"><span class="eyebrow">학생</span><div class="list">${rows}</div></div>`;

  wireTabs();
  $("main").querySelectorAll("[data-student]").forEach((b) => {
    b.onclick = () => guard(() => openStudent(Number(b.dataset.student)));
  });

  $("dock").innerHTML = `<button class="btn" id="add">학생 추가</button>`;
  $("add").onclick = () => go({ ...state, tab: "addStudent" });
}

/**
 * 비밀번호는 만들 때 한 번만 보인다 (서버에 해시로만 남기 때문이다).
 * 그 사실을 화면으로 분명히 말해 준다.
 */
function teacherPinReveal() {
  const { student, pin } = state.reveal;

  $("main").innerHTML = `
    <div class="sec" style="margin-top:8px">
      <h1 style="font-size:20px;font-weight:900">${esc(student.name)} 학생의 비밀번호</h1></div>
    <div class="codebig"><div class="c">${esc(pin)}</div></div>
    <p class="hint" style="text-align:center;margin-top:14px">
      지금 적어 두세요. <b>이 화면을 벗어나면 다시 볼 수 없습니다.</b><br>
      잊어버리면 학생 화면에서 새로 만들어 주면 됩니다.</p>
    <div class="sec"><div class="flat">
      <span class="eyebrow">아이가 로그인할 때 넣을 것</span>
      <p class="lead" style="margin:0">학원 코드 <b class="mono">${esc(state.academy.code)}</b> ·
         이름 <b>${esc(student.name)}</b> · 비밀번호 <b class="mono">${esc(pin)}</b></p>
    </div></div>`;

  $("dock").innerHTML = `<button class="btn ok" id="done">적어 뒀습니다</button>`;
  $("done").onclick = () => guard(() => openTeacher("students"));
}

/** 학원 학생 전체를 한눈에. 약한 아이가 위에 온다. */
function teacherScores() {
  const rows = state.rows;
  const head = rows[0]?.bySteps.map((b) => `<th>${esc(b.label)}</th>`).join("") || "";
  const body = rows.map((r) => {
    const untouched = r.answeredSteps === 0;
    const cell = (p) => untouched ? '<span class="muted">—</span>' : p + "%";
    return `<tr data-student="${r.studentId}" style="cursor:pointer">
      <td>${esc(r.studentName)}${r.active ? "" : ' <span class="muted">(중지)</span>'}</td>
      <td>${cell(r.percent)}</td>
      ${r.bySteps.map((b) => `<td class="${untouched ? "" : (b.percent >= 80 ? "high" : (b.percent < 50 ? "low" : ""))}">${cell(b.percent)}</td>`).join("")}
    </tr>`;
  }).join("");

  $("main").innerHTML = `${teacherTabs("scores")}
    <div class="sec">
      <span class="eyebrow">학생별 성적 — 약한 아이가 위에</span>
      ${rows.length ? `<div class="tablewrap"><table class="grid">
          <tr><th>이름</th><th>총점</th>${head}</tr>${body}</table></div>
        <p class="hint">줄을 누르면 그 아이의 자세한 성적이 나옵니다.
          총점이 같아도 약한 단계가 다릅니다 — 그게 이 표를 보는 이유입니다.</p>`
        : '<div class="empty">아직 등록한 학생이 없습니다.</div>'}
    </div>
    ${rankingBlock(state.ranking)}`;

  wireTabs();
  $("main").querySelectorAll("[data-student]").forEach((tr) => {
    tr.onclick = () => guard(() => openStudent(Number(tr.dataset.student)));
  });
  $("dock").innerHTML = "";
}

/** 학생 한 명: 누적 성적 + 비밀번호·중지·삭제 */
function teacherStudentDetail() {
  const r = state.report;
  const isOwner = session.user.role === "OWNER";
  const solved = r.answeredSteps > 0;

  const assignmentRows = r.assignments.length ? r.assignments.map((a) => `
      <div class="item"><span><span class="t">${esc(a.title)}</span>
        <span class="s">${a.status === "NOT_STARTED" ? "아직 안 풀었어요" : a.correct + "/" + a.total + "칸"}</span></span>
        <span class="r"><span class="badge ${a.percent >= 80 ? "done" : (a.percent < 50 ? "closed" : "doing")}">${a.percent}%</span></span>
      </div>`).join("") : '<div class="empty">받은 과제가 없습니다.</div>';

  $("main").innerHTML = `
    <div class="sec" style="margin-top:8px">
      <h1 style="font-size:20px;font-weight:900">${esc(r.studentName)}</h1>
      <p class="lead" style="margin-top:6px">초${r.grade}학년 ·
        과제 ${r.assignmentCount}건 중 ${r.completedCount}건 완료${r.active ? "" : " · <b>중지됨</b>"}</p>
    </div>

    ${solved ? `
      <div class="sec" style="text-align:center">
        <div style="font-family:'Gothic A1',sans-serif;font-weight:900;font-size:46px;line-height:1;color:var(--accent)">${r.percent}%</div>
        <p class="hint">푼 ${r.answeredSteps}칸 중 ${r.correctSteps}칸 정답</p>
      </div>
      <div class="sec"><span class="eyebrow">단계별</span>
        <div class="bars">${r.bySteps.map((b) => barRow(b.label, b.percent)).join("")}</div>
        <p class="hint">${esc(r.advice)}</p></div>
      <div class="sec"><span class="eyebrow">유형별 — 약한 것부터</span>
        <div class="bars">${r.byCategories.map((b) => barRow(b.label, b.percent)).join("")}</div></div>`
    : '<div class="sec"><div class="empty">아직 푼 문제가 없어 성적이 없습니다.</div></div>'}

    <div class="sec"><span class="eyebrow">과제별</span><div class="list">${assignmentRows}</div></div>

    <div class="sec"><span class="eyebrow">관리</span>
      <div class="list">
        <button class="item" id="do-pin"><span><span class="t">비밀번호 새로 만들기</span>
          <span class="s">아이가 잊어버렸을 때. 성적은 그대로 남습니다.</span></span><span class="r muted">›</span></button>
        <button class="item" id="do-active"><span><span class="t">${r.active ? "다니지 않음으로 바꾸기" : "다시 다니게 하기"}</span>
          <span class="s">${r.active ? "로그인이 막히고 기록은 남습니다." : "다시 로그인할 수 있게 됩니다."}</span></span><span class="r muted">›</span></button>
        ${isOwner ? `<button class="item" id="do-delete"><span>
          <span class="t" style="color:var(--bad)">완전히 지우기</span>
          <span class="s">성적까지 같이 사라지고 되돌릴 수 없습니다.</span></span><span class="r muted">›</span></button>`
        : '<div class="item"><span><span class="t muted">완전히 지우기</span><span class="s">원장 선생님만 할 수 있습니다.</span></span></div>'}
      </div></div>`;

  $("dock").innerHTML = `<button class="btn ghost" id="back">학생 목록</button>`;
  $("back").onclick = () => guard(() => openTeacher("students"));

  $("do-pin").onclick = () => guard(async () => {
    const result = await api("PATCH", `/api/students/${r.studentId}/pin`, {});
    go({ ...state, tab: "pinReveal",
         reveal: { student: { id: r.studentId, name: r.studentName, grade: r.grade }, pin: result.pin } });
  });
  $("do-active").onclick = () => guard(async () => {
    await api("PATCH", `/api/students/${r.studentId}/active`, { active: !r.active });
    toast(r.active ? "이제 로그인할 수 없습니다." : "다시 로그인할 수 있습니다.");
    await openStudent(r.studentId);
  });
  const del = $("do-delete");
  if (del) del.onclick = () => go({ ...state, tab: "confirmDelete", typed: "" });
}

/**
 * 삭제 확인.
 * 되돌릴 수 없고 성적까지 사라지므로, 폰에서 잘못 눌러 지워지는 일이 없도록 이름을 적게 한다.
 */
function teacherConfirmDelete() {
  const r = state.report;
  const matched = (state.typed || "").trim() === r.studentName;

  $("main").innerHTML = `
    <div class="sec" style="margin-top:8px">
      <h1 style="font-size:20px;font-weight:900;color:var(--bad)">${esc(r.studentName)} 학생을 완전히 지웁니다</h1>
    </div>
    <div class="sec"><div class="flat">
      <span class="eyebrow">같이 사라지는 것</span>
      <p class="lead" style="margin:0">
        받은 과제 <b>${r.assignmentCount}건</b>과 푼 기록 <b>${r.answeredSteps}칸</b>,
        그리고 지금까지의 성적이 모두 사라집니다. <b>되돌릴 수 없습니다.</b></p>
    </div></div>
    <div class="sec"><div class="flat">
      <span class="eyebrow">지우지 않아도 되는 경우</span>
      <p class="lead" style="margin:0">학원을 그만둔 것뿐이라면 <b>'다니지 않음'</b>으로 바꾸세요.
        로그인만 막히고 성적은 남습니다.</p>
    </div></div>
    <div class="field" style="margin-top:20px">
      <label for="confirm">확인을 위해 <b>${esc(r.studentName)}</b> 라고 적어 주세요</label>
      <input class="input" id="confirm" value="${esc(state.typed || "")}" autocomplete="off"></div>`;

  const input = $("confirm");
  input.oninput = () => {
    state.typed = input.value;
    $("delete").disabled = input.value.trim() !== r.studentName;
  };

  $("dock").innerHTML = `<div class="row2">
      <button class="btn ghost" id="cancel">그만두기</button>
      <button class="btn" id="delete" style="background:var(--bad);color:#fff"${matched ? "" : " disabled"}>지웁니다</button>
    </div>`;
  $("cancel").onclick = () => guard(() => openStudent(r.studentId));
  $("delete").onclick = () => guard(async () => {
    const removed = await api("DELETE", "/api/students/" + r.studentId);
    toast(`${removed.name} 학생과 기록 ${removed.removedSubmissions}칸을 지웠습니다.`);
    await openTeacher("students");
  });
}

function teacherAddStudent() {
  $("main").innerHTML = `
    <div class="sec" style="margin-top:8px">
      <h1 style="font-size:20px;font-weight:900">학생 추가</h1>
      <p class="lead" style="margin-top:8px">비밀번호를 비워 두면 서버가 네 자리로 만들어 줍니다.
        만들어진 번호는 <b>이때 한 번만</b> 보입니다.</p>
    </div>
    <div class="field"><label for="s-name">이름</label>
      <input class="input" id="s-name" placeholder="김민우" autocomplete="off"></div>
    <div class="field"><label for="s-grade">학년</label>
      <select class="input" id="s-grade">
        ${[1,2,3,4,5,6].map((g) => `<option value="${g}"${g === 3 ? " selected" : ""}>초${g}학년</option>`).join("")}
      </select></div>
    <div class="field"><label for="s-pin">비밀번호 네 자리 (선택)</label>
      <input class="input pin" id="s-pin" maxlength="4" inputmode="numeric" placeholder="자동"></div>`;

  $("dock").innerHTML = `<div class="row2">
      <button class="btn ghost" id="cancel">그만두기</button>
      <button class="btn" id="save">추가</button></div>`;
  $("cancel").onclick = () => guard(() => openTeacher("students"));
  $("save").onclick = () => guard(async () => {
    const pin = $("s-pin").value.trim();
    const result = await api("POST", "/api/students", {
      name: $("s-name").value.trim(),
      grade: Number($("s-grade").value),
      pin: pin === "" ? null : pin
    });
    const academy = await api("GET", "/api/academy");
    go({ screen: "teacher", tab: "pinReveal", academy,
         reveal: { student: result.student, pin: result.pin } });
  });
}

function teacherAssignments() {
  const rows = state.assignments.length ? state.assignments.map((a) => {
    const done = a.studentCount ? Math.round(a.completedCount * 100 / a.studentCount) : 0;
    return `<button class="item" data-assignment="${a.id}">
        <span><span class="t">${esc(a.title)}</span>
          <span class="s">초${a.grade}학년 · ${a.problemCount}문제${a.classroomName ? " · " + esc(a.classroomName) : ""}</span></span>
        <span class="r">${a.closed ? '<span class="badge closed">마감</span>'
          : `<span class="badge ${done === 100 ? "done" : "doing"}">${a.completedCount}/${a.studentCount} 완료</span>`}</span>
      </button>`;
  }).join("") : `<div class="empty">아직 낸 과제가 없습니다.<br>아래 버튼으로 과제를 내주세요.</div>`;

  $("main").innerHTML = `${teacherTabs("assignments")}
    <div class="sec"><span class="eyebrow">과제</span><div class="list">${rows}</div></div>`;
  wireTabs();
  $("main").querySelectorAll("[data-assignment]").forEach((b) => {
    b.onclick = () => guard(() => openAssignment(Number(b.dataset.assignment)));
  });

  $("dock").innerHTML = `<button class="btn" id="new">과제 내기</button>`;
  $("new").onclick = () => guard(async () => {
    const [students, options] = await Promise.all([
      api("GET", "/api/students"),
      api("GET", "/api/worksheets/options?grade=3")
    ]);
    go({
      screen: "teacher", tab: "newAssignment", students, options,
      form: {
        grade: 3, categories: options.categories.map((c) => c.name).slice(0, 4),
        count: 5, carry: true, sameForEveryone: false,
        studentIds: students.filter((s) => s.active).map((s) => s.id)
      }
    });
  });
}

function teacherNewAssignment() {
  const { options, students, form } = state;
  const categoryChips = options.categories.map((c) => `
      <button type="button" class="chip" data-cat="${c.name}"
        aria-pressed="${form.categories.includes(c.name)}">${esc(c.label)}<span class="op">${esc(c.operation)}</span></button>`).join("");
  const studentChips = students.filter((s) => s.active).map((s) => `
      <button type="button" class="chip" data-sid="${s.id}"
        aria-pressed="${form.studentIds.includes(s.id)}">${esc(s.name)}</button>`).join("");

  $("main").innerHTML = `
    <div class="sec" style="margin-top:8px"><h1 style="font-size:20px;font-weight:900">과제 내기</h1></div>
    <div class="field"><label for="a-title">제목</label>
      <input class="input" id="a-title" placeholder="3학년 덧뺄셈 숙제" value="${esc(form.title || "")}"></div>

    <div class="sec"><span class="eyebrow">학년 — ${esc(options.numberRange)}</span>
      <div class="chips" id="grades">${[1,2,3,4,5,6].map((g) =>
        `<button type="button" class="chip" data-grade="${g}" aria-pressed="${form.grade === g}">초${g}</button>`).join("")}</div></div>

    <div class="sec"><span class="eyebrow">문제 유형 — ${form.categories.length}가지 선택됨</span>
      <div class="chips" style="margin-bottom:9px">
        <button type="button" class="chip" id="pick-all"
          aria-pressed="${form.categories.length === options.categories.length}">종합 — 전체 유형 섞기</button>
        <button type="button" class="chip" id="pick-add">덧셈 쪽만</button>
        <button type="button" class="chip" id="pick-sub">뺄셈 쪽만</button>
      </div>
      <div class="chips" id="cats">${categoryChips}</div>
      <p class="hint">여러 유형을 고르면 한 학습지에 섞여 나옵니다.
        '종합'은 그 학년에 낼 수 있는 유형을 전부 섞습니다 — 실전에 가장 가깝습니다.</p></div>

    <div class="sec"><span class="eyebrow">문항 수</span>
      <div class="chips" id="counts">${[3,5,10,20].map((n) =>
        `<button type="button" class="chip" data-count="${n}" aria-pressed="${form.count === n}">${n}문항</button>`).join("")}</div></div>

    <div class="sec"><span class="eyebrow">받아올림 · 받아내림</span>
      <div class="chips" id="carry">
        <button type="button" class="chip" data-carry="true" aria-pressed="${form.carry}">있음</button>
        <button type="button" class="chip" data-carry="false" aria-pressed="${!form.carry}">없음</button></div></div>

    <div class="sec"><span class="eyebrow">문제를 어떻게</span>
      <div class="chips" id="same">
        <button type="button" class="chip" data-same="false" aria-pressed="${!form.sameForEveryone}">아이마다 다르게</button>
        <button type="button" class="chip" data-same="true" aria-pressed="${form.sameForEveryone}">모두 같게</button></div>
      <p class="hint">다르게 하면 옆자리를 봐도 소용없습니다. 같은 시험지를 돌릴 때만 '모두 같게'.</p></div>

    <div class="sec"><span class="eyebrow">받을 학생 (${form.studentIds.length}명)</span>
      <div class="chips" id="sids">${studentChips || '<span class="hint">등록된 학생이 없습니다.</span>'}</div></div>`;

  const toggle = (container, attr, handler) => {
    const node = $(container);
    if (node) node.onclick = (e) => {
      const b = e.target.closest("[data-" + attr + "]");
      if (b) { handler(b.dataset[attr]); go({ ...state }); }
    };
  };
  toggle("cats", "cat", (v) => {
    const i = form.categories.indexOf(v);
    if (i < 0) form.categories.push(v); else form.categories.splice(i, 1);
  });
  toggle("counts", "count", (v) => { form.count = Number(v); });
  toggle("carry", "carry", (v) => { form.carry = v === "true"; });
  toggle("same", "same", (v) => { form.sameForEveryone = v === "true"; });
  toggle("sids", "sid", (v) => {
    const id = Number(v), i = form.studentIds.indexOf(id);
    if (i < 0) form.studentIds.push(id); else form.studentIds.splice(i, 1);
  });

  const allNames = options.categories.map((c) => c.name);
  const preset = (id, keep) => {
    const node = $(id);
    if (node) node.onclick = () => {
      const picked = allNames.filter(keep);
      form.categories = picked.length ? picked : allNames.slice();
      form.title = $("a-title").value;
      go({ ...state });
    };
  };
  preset("pick-all", () => true);
  preset("pick-add", (n) => ["MERGE", "INCREASE", "MULTI", "COMPARE"].includes(n));
  preset("pick-sub", (n) => ["DECREASE", "COMPARE"].includes(n));

  // 학년이 바뀌면 낼 수 있는 유형도 바뀐다 — 서버에 다시 물어본다
  $("grades").onclick = (e) => {
    const b = e.target.closest("[data-grade]");
    if (!b) return;
    guard(async () => {
      const grade = Number(b.dataset.grade);
      const options2 = await api("GET", "/api/worksheets/options?grade=" + grade);
      const allowed = options2.categories.map((c) => c.name);
      form.grade = grade;
      form.categories = form.categories.filter((c) => allowed.includes(c));
      if (!form.categories.length) form.categories = allowed.slice(0, 4);
      form.title = $("a-title").value;
      go({ ...state, options: options2 });
    });
  };

  $("dock").innerHTML = `<div class="row2">
      <button class="btn ghost" id="cancel">그만두기</button>
      <button class="btn" id="save">내주기</button></div>`;
  $("cancel").onclick = () => guard(() => openTeacher("assignments"));
  $("save").onclick = () => guard(async () => {
    const created = await api("POST", "/api/assignments", {
      title: $("a-title").value.trim() || `초${form.grade}학년 숙제`,
      grade: form.grade,
      categories: form.categories,
      count: form.count,
      carry: form.carry,
      sameForEveryone: form.sameForEveryone,
      studentIds: form.studentIds
    });
    toast("과제를 냈습니다.");
    await openAssignment(created.assignment.id);
  });
}

async function openAssignment(id) {
  const [detail, report] = await Promise.all([
    api("GET", "/api/assignments/" + id),
    api("GET", `/api/assignments/${id}/report`)
  ]);
  go({ screen: "teacher", tab: "detail", detail, report });
}

function teacherAssignmentDetail() {
  const { detail, report } = state;
  const a = detail.assignment;

  const progress = detail.students.map((s) => `
      <div class="item"><span><span class="t">${esc(s.studentName)}</span>
        <span class="s">${s.submitted}/${s.total}칸 · ${s.percent}점</span></span>
        <span class="r">${statusBadge(s.status)}</span></div>`).join("");

  const anyAnswer = report.bySteps.some((r) => r.total > 0);
  const stepBars = report.bySteps.map((r) => barRow(r.label, r.percent)).join("");
  const categoryBars = report.byCategories.length
    ? report.byCategories.map((r) => barRow(r.label, r.percent)).join("")
    : '<p class="hint">아직 푼 문제가 없습니다.</p>';

  const head = report.students[0]?.bySteps.map((s) => `<th>${esc(s.label)}</th>`).join("") || "";
  const grid = report.students.map((s) => `
      <tr><td>${esc(s.studentName)}</td><td>${s.percent}%</td>
        ${s.bySteps.map((b) => `<td class="${b.percent >= 80 ? "high" : (b.percent < 50 ? "low" : "")}">${b.percent}%</td>`).join("")}
      </tr>`).join("");

  $("main").innerHTML = `
    <div class="sec" style="margin-top:8px">
      <h1 style="font-size:20px;font-weight:900">${esc(a.title)}</h1>
      <p class="lead" style="margin-top:6px">초${a.grade}학년 · ${a.problemCount}문제 ·
        ${a.sameForEveryone ? "모두 같은 문제" : "아이마다 다른 문제"} ·
        학습지 코드 <span class="mono">${esc(a.code)}</span>
        ${a.closed ? ' · <b style="color:var(--bad)">마감됨</b>' : ""}</p>
    </div>

    <div class="sec"><span class="eyebrow">진행</span><div class="list">${progress}</div></div>

    <div class="sec"><span class="eyebrow">단계별 정답률</span>
      <div class="bars">${stepBars}</div>
      <p class="hint">${esc(report.advice)}</p></div>

    <div class="sec"><span class="eyebrow">유형별 정답률</span><div class="bars">${categoryBars}</div></div>

    ${anyAnswer ? `<div class="sec"><span class="eyebrow">학생별 — 총점이 같아도 약한 자리가 다릅니다</span>
      <div class="tablewrap"><table class="grid">
        <tr><th>이름</th><th>총점</th>${head}</tr>${grid}</table></div></div>` : ""}`;

  $("dock").innerHTML = `<div class="row2">
      <button class="btn ghost" id="back">과제 목록</button>
      <button class="btn ${a.closed ? "" : "ghost"}" id="toggle">${a.closed ? "다시 열기" : "마감하기"}</button></div>`;
  $("back").onclick = () => guard(() => openTeacher("assignments"));
  $("toggle").onclick = () => guard(async () => {
    await api("PATCH", `/api/assignments/${a.id}/closed`, { closed: !a.closed });
    await openAssignment(a.id);
  });
}

/* ════════════ 학생 ════════════ */

async function openStudentList() {
  const assignments = await api("GET", "/api/my/assignments");
  go({ screen: "student", view: "list", assignments });
}

function studentScreen() {
  const views = { list: studentList, solve: studentSolve, done: studentDone,
                  game: gameScreen, gameDone: gameDoneScreen, ranking: rankingScreen };
  draw(views[state.view] || studentList);
}

function studentList() {
  const rows = state.assignments.length ? state.assignments.map((a) => `
      <button class="item" data-open="${a.id}" ${a.status === "DONE" || !a.open ? "" : ""}>
        <span><span class="t">${esc(a.title)}</span>
          <span class="s">${a.problemCount}문제 · ${a.answeredSteps}/${a.totalSteps}칸${a.open ? "" : " · 마감"}</span></span>
        <span class="r">${statusBadge(a.status)}</span>
      </button>`).join("")
    : `<div class="empty">아직 받은 숙제가 없어요.<br>선생님이 내주시면 여기에 나타납니다.</div>`;

  $("main").innerHTML = `
    <div class="sec" style="margin-top:8px">
      <h1 style="font-size:20px;font-weight:900">${esc(session.user.name)}의 숙제</h1>
      <p class="lead" style="margin-top:6px">문장을 읽고 <b>단서 → 연산 → 식 → 답</b> 순서로 풀어요.</p>
    </div>
    <div class="list">${rows}</div>

    <div class="sec">
      <span class="eyebrow">연산 게임</span>
      <div class="list">
        <button class="item" id="go-game">
          <span><span class="t">60초 계산 대결</span>
            <span class="s">시간 안에 몇 개나 맞힐 수 있을까요?</span></span>
          <span class="r muted">›</span></button>
        <button class="item" id="go-rank">
          <span><span class="t">순위 보기</span>
            <span class="s">우리 학원에서 몇 등인지 확인해요</span></span>
          <span class="r muted">›</span></button>
      </div>
    </div>`;
  $("dock").innerHTML = "";

  $("main").querySelectorAll("[data-open]").forEach((b) => {
    b.onclick = () => guard(() => openSolve(Number(b.dataset.open)));
  });
  $("go-game").onclick = () => guard(startGame);
  $("go-rank").onclick = () => guard(openRanking);
}

async function openSolve(id) {
  const detail = await api("GET", "/api/my/assignments/" + id);
  const position = firstUnfinished(detail);
  if (!position) {
    go({ screen: "student", view: "done", detail });
    return;
  }
  go({ screen: "student", view: "solve", detail, pi: position.pi, si: position.si, feedback: null });
}

/** 이어서 풀기 — 아직 안 낸 첫 단계를 찾는다. */
function firstUnfinished(detail) {
  for (let pi = 0; pi < detail.problems.length; pi++) {
    const steps = detail.problems[pi].steps;
    for (let si = 0; si < steps.length; si++) {
      if (!steps[si].submitted) return { pi, si };
    }
  }
  return null;
}

function studentSolve() {
  const { detail, pi, si, feedback } = state;
  const problem = detail.problems[pi];
  const stepKey = STEP_ORDER[si];

  const sentence = problem.sentence.split(/(«[^»]+»)/).map((part) => {
    if (!part.startsWith("«")) return esc(part);
    const word = part.slice(1, -1);
    return `<button type="button" class="tap" data-w="${esc(word)}">${esc(word)}</button>`;
  }).join("");

  const dots = STEP_ORDER.map((key, i) => {
    let on = "";
    if (i === si && !feedback) on = "now";
    else if (problem.steps[i].submitted) on = problem.steps[i].correct ? "ok" : "no";
    else if (i === si) on = feedback ? (feedback.correct ? "ok" : "no") : "now";
    return `<div class="stepdot" data-on="${on}">${STEP_LABELS[key]}</div>`;
  }).join("");

  $("main").innerHTML = `
    <div class="steps">${dots}</div>
    <div class="card">
      <div class="qmeta"><span class="qnum">${problem.no} / ${detail.problems.length}</span>
        <span class="qcat">${esc(problem.categoryLabel)}</span></div>
      <div class="qtext ${stepKey === "CUE" && !feedback ? "" : "locked"}" id="qtext">${sentence}</div>
    </div>
    <div id="stage"></div>
    <div id="fb"></div>`;

  if (feedback) {
    renderFeedback(feedback);
    return;
  }
  ({ CUE: stepCue, OPERATION: stepOperation, EXPRESSION: stepExpression, ANSWER: stepAnswer }[stepKey])();
}

const ask = (n, text) => `<div class="ask"><i>${n}</i>${text}</div>`;

function stepCue() {
  $("stage").innerHTML = ask(1, "연산을 알려 주는 <b>단서 단어</b>를 눌러 보세요.");
  $("dock").innerHTML = "";
  $("qtext").querySelectorAll(".tap").forEach((b) => {
    b.onclick = () => guard(async () => {
      $("qtext").querySelectorAll(".tap").forEach((x) => { x.disabled = true; });
      await submitStep({ value: b.dataset.w }, b);
    });
  });
}

function stepOperation() {
  $("stage").innerHTML = ask(2, "그럼 어떤 <b>연산</b>일까요?") +
    `<div class="ops">${OPS.map((o) => `<button class="op-btn" data-o="${o}">${o}</button>`).join("")}</div>`;
  $("dock").innerHTML = "";
  $("stage").querySelectorAll(".op-btn").forEach((b) => {
    b.onclick = () => guard(async () => {
      $("stage").querySelectorAll(".op-btn").forEach((x) => { x.disabled = true; });
      b.setAttribute("aria-pressed", "true");
      await submitStep({ value: b.dataset.o });
    });
  });
}

function stepExpression() {
  const problem = state.detail.problems[state.pi];
  const n = problem.numbers.length;
  const sign = problem.steps[1].submittedValue || "?";
  const shuffled = problem.numbers.slice().sort(() => Math.random() - 0.5);
  const blanks = new Array(n).fill(null);

  let html = ask(3, "숫자를 눌러 <b>식</b>을 완성하세요.") + '<div class="expr" id="expr">';
  for (let i = 0; i < n; i++) {
    html += `<button class="blank" data-i="${i}"></button>`;
    if (i < n - 1) html += `<span class="sign">${esc(sign)}</span>`;
  }
  html += `<span class="sign">=</span><span class="blank" style="border-style:dotted;color:var(--ink3)">?</span></div>
    <div class="bank" id="bank">${shuffled.map((v, i) =>
      `<button class="numchip" data-v="${v}" data-k="${i}">${v}</button>`).join("")}</div>`;
  $("stage").innerHTML = html;
  $("dock").innerHTML = "";

  const sync = () => {
    $("stage").querySelectorAll(".blank[data-i]").forEach((b) => {
      const slot = blanks[Number(b.dataset.i)];
      b.textContent = slot === null ? "" : slot.value;
      b.classList.toggle("filled", slot !== null);
    });
    const used = blanks.filter(Boolean).map((x) => x.k);
    $("stage").querySelectorAll(".numchip").forEach((c) => {
      c.dataset.used = used.includes(Number(c.dataset.k)) ? "1" : "0";
    });
    if (blanks.every((x) => x !== null)) {
      setTimeout(() => guard(async () => {
        $("stage").querySelectorAll(".blank,.numchip").forEach((x) => { x.disabled = true; });
        await submitStep({ numbers: blanks.map((x) => x.value) });
      }), 180);
    }
  };
  $("bank").onclick = (e) => {
    const chip = e.target.closest(".numchip");
    if (!chip || chip.dataset.used === "1") return;
    const slot = blanks.indexOf(null);
    if (slot < 0) return;
    blanks[slot] = { value: Number(chip.dataset.v), k: Number(chip.dataset.k) };
    sync();
  };
  $("expr").onclick = (e) => {
    const b = e.target.closest(".blank[data-i]");
    if (!b) return;
    blanks[Number(b.dataset.i)] = null;
    sync();
  };
}

function stepAnswer() {
  const problem = state.detail.problems[state.pi];
  const sign = problem.steps[1].submittedValue || "?";
  let typed = "";

  $("stage").innerHTML = ask(4, `<span class="mono">${esc(problem.numbers.join(" " + sign + " "))}</span> 의 답은?`) +
    `<div class="ansbox"><span class="ansval mono" id="ansval"></span><span class="ansunit">${esc(problem.unit)}</span></div>
     <div class="pad">${[1,2,3,4,5,6,7,8,9].map((n) => `<button class="key" data-d="${n}">${n}</button>`).join("")}
       <button class="key fn" data-x="del">지우기</button><button class="key" data-d="0">0</button>
       <button class="key fn" data-x="ok">확인</button></div>`;
  $("dock").innerHTML = "";

  $("stage").querySelector(".pad").onclick = (e) => {
    const key = e.target.closest(".key");
    if (!key) return;
    if (key.dataset.d !== undefined) {
      if (typed.length < 7) typed += key.dataset.d;
    } else if (key.dataset.x === "del") {
      typed = typed.slice(0, -1);
    } else if (key.dataset.x === "ok") {
      if (!typed.length) return;
      guard(async () => {
        $("stage").querySelectorAll(".key").forEach((x) => { x.disabled = true; });
        await submitStep({ value: typed });
      });
      return;
    }
    $("ansval").textContent = typed;
  };
}

/** 한 단계 제출. 채점은 서버가 한다 — 이 화면은 정답을 모른다. */
async function submitStep(payload, tappedButton) {
  const { detail, pi, si } = state;
  const problem = detail.problems[pi];
  const result = await api("POST", `/api/my/assignments/${detail.assignment.id}/answers`, {
    problemNo: problem.no,
    step: STEP_ORDER[si],
    value: payload.value ?? null,
    numbers: payload.numbers ?? null
  });

  problem.steps[si] = {
    ...problem.steps[si],
    submitted: true,
    correct: result.correct,
    submittedValue: payload.value ?? (payload.numbers || []).join(","),
    correctValue: result.correctValue
  };
  if (tappedButton) tappedButton.dataset.state = result.correct ? "right" : "wrong";
  go({ ...state, feedback: result });
}

function renderFeedback(result) {
  const { detail, pi, si } = state;
  const problem = detail.problems[pi];

  if (si === 0) {
    // 단서 단계는 정답 단어를 문장 위에 표시해 준다
    const correctWords = (result.correctValue || "").split(",").map((w) => w.trim());
    $("qtext").querySelectorAll(".tap").forEach((b) => {
      b.disabled = true;
      if (correctWords.includes(b.dataset.w)) b.dataset.state = "right";
      else if (b.dataset.w === problem.steps[0].submittedValue) b.dataset.state = "wrong";
    });
  }

  const message = result.correct
    ? "잘했어요"
    : `정답은 <b style="display:inline">${esc(result.correctValue)}</b> 였어요.`;
  $("fb").innerHTML = `<div class="fb ${result.correct ? "y" : "n"}">
      <span class="mk">${result.correct ? "✓" : "✗"}</span>
      <span><b>${result.correct ? "맞았어요" : "다시 볼까요"}</b>${result.correct ? "" : message}</span></div>`;

  const lastProblem = pi === detail.problems.length - 1;
  const label = si < 3 ? "다음 단계" : (lastProblem ? "다 풀었어요" : "다음 문제");
  $("dock").innerHTML = `<button class="btn ${result.correct ? "ok" : ""}" id="next">${label}</button>`;
  $("next").onclick = () => {
    if (si < 3) {
      go({ ...state, si: si + 1, feedback: null });
    } else if (!lastProblem) {
      go({ ...state, pi: pi + 1, si: 0, feedback: null });
    } else {
      guard(async () => {
        const fresh = await api("GET", "/api/my/assignments/" + detail.assignment.id);
        go({ screen: "student", view: "done", detail: fresh });
      });
    }
  };
}

function studentDone() {
  const detail = state.detail;
  const problems = detail.problems;
  const totals = STEP_ORDER.map((key, i) => {
    const correct = problems.filter((p) => p.steps[i].correct).length;
    return { label: STEP_LABELS[key], correct, percent: Math.round(correct * 100 / problems.length) };
  });
  const all = totals.reduce((sum, t) => sum + t.correct, 0);
  const totalSteps = problems.length * 4;

  const rows = problems.map((p) => `
      <div class="item"><span><span class="t" style="font-weight:500;font-size:13px">${p.no}. ${esc(p.sentence.replace(/[«»]/g, "").slice(0, 22))}…</span></span>
        <span class="r" style="display:flex;gap:3px">${p.steps.map((s) =>
          `<span class="badge ${s.correct ? "done" : "closed"}" style="padding:2px 6px">${s.correct ? "✓" : "✗"}</span>`).join("")}</span>
      </div>`).join("");

  const weakest = totals.reduce((a, b) => (b.percent < a.percent ? b : a));

  $("main").innerHTML = `
    <div class="sec" style="text-align:center;padding-block:20px 4px">
      <div style="font-family:'Gothic A1',sans-serif;font-weight:900;font-size:54px;line-height:1;color:var(--accent)">
        ${Math.round(all * 100 / totalSteps)}점</div>
      <p class="hint">${problems.length}문제 × 4단계 = ${totalSteps}칸 중 ${all}칸 정답</p>
    </div>
    <div class="sec"><span class="eyebrow">단계별</span><div class="bars">${totals.map((t) => barRow(t.label, t.percent)).join("")}</div>
      <p class="hint"><b>${esc(weakest.label)}</b>가 가장 약해요. 선생님과 이 부분을 다시 봐요.</p></div>
    <div class="sec"><span class="eyebrow">문제별 (단서·연산·식·답)</span><div class="list">${rows}</div></div>`;

  $("dock").innerHTML = `<button class="btn" id="back">숙제 목록으로</button>`;
  $("back").onclick = () => guard(openStudentList);
}


/* ════════════ 연산 게임 ════════════ */

let gameTimer = null;

function stopTimer() {
  if (gameTimer) {
    clearInterval(gameTimer);
    gameTimer = null;
  }
}

async function startGame() {
  const round = await api("POST", "/api/my/games");
  stopTimer();
  go({
    screen: "student", view: "game",
    round,
    index: 0,
    typed: "",
    answers: [],
    endsAt: Date.now() + round.limitSeconds * 1000,
    sending: false
  });
}

function gameScreen() {
  const g = state;
  const problem = g.round.problems[g.index];

  // 다 풀었으면 시간이 남아도 끝낸다
  if (!problem) {
    submitGame();
    $("main").innerHTML = '<div class="spin">채점하는 중…</div>';
    $("dock").innerHTML = "";
    return;
  }

  $("main").innerHTML = `
    <div class="timer"><span class="n" id="t-left">--</span><span class="u">초 남음</span></div>
    <div class="timer-track"><span class="timer-fill" id="t-fill" style="width:100%"></span></div>

    <div class="gq">
      <div class="no">${g.index + 1} / ${g.round.problems.length}</div>
      <div class="ex">${esc(problem.expression)}</div>
      <div class="typed mono" id="typed">${esc(g.typed)}</div>
    </div>

    <div class="gscore"><span>맞힌 수 <b id="g-solved">—</b></span><span>푼 문제 <b>${g.answers.length}</b></span></div>

    <div class="pad">
      ${[1,2,3,4,5,6,7,8,9].map((n) => `<button class="key" data-d="${n}">${n}</button>`).join("")}
      <button class="key fn" data-x="skip">건너뛰기</button>
      <button class="key" data-d="0">0</button>
      <button class="key fn" data-x="ok">확인</button>
    </div>`;
  $("dock").innerHTML = `<button class="btn ghost" id="stop">그만하고 채점</button>`;
  $("stop").onclick = () => guard(submitGame);
  // 맞힌 수는 게임 중에 알려 주지 않는다 — 정답이 새면 게임이 아니다
  $("g-solved").textContent = "끝나고";

  $("main").querySelector(".pad").onclick = (e) => {
    const key = e.target.closest(".key");
    if (!key) return;
    if (key.dataset.d !== undefined) {
      if (state.typed.length < 6) {
        state.typed += key.dataset.d;
        $("typed").textContent = state.typed;
      }
      return;
    }
    if (key.dataset.x === "skip") {
      next(null);
    } else if (key.dataset.x === "ok") {
      if (!state.typed.length) return;
      next(state.typed);
    }
  };

  tick();
  stopTimer();
  gameTimer = setInterval(tick, 500);

  function next(value) {
    if (value !== null) {
      state.answers.push({ no: problem.no, value });
    }
    go({ ...state, index: state.index + 1, typed: "" });
  }

  function tick() {
    const left = Math.max(0, Math.ceil((state.endsAt - Date.now()) / 1000));
    const leftNode = $("t-left");
    if (!leftNode) {           // 화면이 바뀌었다
      stopTimer();
      return;
    }
    leftNode.textContent = left;
    leftNode.classList.toggle("low", left <= 10);
    const fill = $("t-fill");
    fill.style.width = (left / state.round.limitSeconds * 100) + "%";
    fill.classList.toggle("low", left <= 10);
    if (left <= 0) {
      stopTimer();
      guard(submitGame);
    }
  }
}

async function submitGame() {
  if (state.sending) return;
  state.sending = true;
  stopTimer();
  try {
    const result = await api("POST", `/api/my/games/${state.round.roundId}/finish`,
                             { answers: state.answers });
    const ranking = await api("GET", "/api/my/games/ranking");
    go({ screen: "student", view: "gameDone", result, ranking });
  } catch (e) {
    state.sending = false;
    throw e;
  }
}

function gameDoneScreen() {
  const r = state.result;
  const wrong = r.wrong.length ? r.wrong.map((w) => `
      <div class="item"><span><span class="t mono" style="font-weight:600">${esc(w.expression)}</span>
        <span class="s">내 답 ${esc(w.submitted)}</span></span>
        <span class="r"><span class="badge done mono">${w.answer}</span></span>
      </div>`).join("")
    : '<div class="empty">푼 문제를 다 맞혔어요!</div>';

  $("main").innerHTML = `
    <div class="sec" style="text-align:center;padding-block:20px 4px">
      <div style="font-family:'Gothic A1',sans-serif;font-weight:900;font-size:56px;line-height:1;color:var(--accent)">
        ${r.solved}<span style="font-size:24px;color:var(--ink3)"> / ${r.attempted}</span></div>
      <p class="hint">${r.elapsedSeconds}초 동안 ${r.attempted}문제를 풀어 ${r.solved}개 맞혔어요</p>
      ${r.newBest ? '<p class="hint" style="color:var(--good);font-weight:700">새 최고 기록!</p>'
                  : `<p class="hint">내 최고 기록 ${r.best}개</p>`}
      ${r.rank ? `<p class="hint">우리 학원에서 <b>${r.rank}등</b></p>` : ""}
    </div>
    <div class="sec"><span class="eyebrow">틀린 문제</span><div class="list">${wrong}</div></div>
    ${rankingBlock(state.ranking)}`;

  $("dock").innerHTML = `<div class="row2">
      <button class="btn ghost" id="back">숙제로</button>
      <button class="btn" id="again">한 번 더</button></div>`;
  $("back").onclick = () => guard(openStudentList);
  $("again").onclick = () => guard(startGame);
}

async function openRanking() {
  const ranking = await api("GET", "/api/my/games/ranking");
  go({ screen: "student", view: "ranking", ranking });
}

function rankingScreen() {
  $("main").innerHTML = `
    <div class="sec" style="margin-top:8px">
      <h1 style="font-size:20px;font-weight:900">연산 게임 순위</h1>
      <p class="lead" style="margin-top:6px">가장 잘한 한 판만 올라갑니다. 많이 한다고 유리하지 않아요.</p>
    </div>
    ${rankingBlock(state.ranking)}`;
  $("dock").innerHTML = `<div class="row2">
      <button class="btn ghost" id="back">숙제로</button>
      <button class="btn" id="play">게임 하기</button></div>`;
  $("back").onclick = () => guard(openStudentList);
  $("play").onclick = () => guard(startGame);
}

/** 순위표 한 덩어리. 게임 결과 화면과 순위 화면이 같이 쓴다. */
function rankingBlock(ranking) {
  if (!ranking || !ranking.rows.length) {
    return '<div class="sec"><div class="empty">아직 아무도 게임을 하지 않았어요.<br>첫 번째가 되어 보세요!</div></div>';
  }
  const medals = ["🥇", "🥈", "🥉"];
  const rows = ranking.rows.map((r) => `
      <div class="rank ${r.me ? "me" : ""}">
        <span class="no ${r.rank <= 3 ? "medal" : ""}">${r.rank <= 3 ? medals[r.rank - 1] : r.rank}</span>
        <span class="nm">${esc(r.studentName)}${r.me ? " (나)" : ""}
          <small>초${r.grade}학년 · ${r.plays}판</small></span>
        <span class="sc">${r.best}개</span>
      </div>`).join("");

  const mine = ranking.myRank && ranking.myRank > ranking.rows.length
    ? `<p class="hint">내 순위는 ${ranking.players}명 중 <b>${ranking.myRank}등</b> (최고 ${ranking.myBest}개)</p>`
    : "";

  return `<div class="sec"><span class="eyebrow">우리 학원 순위 · ${ranking.players}명 참가</span>
      ${rows}${mine}</div>`;
}

/* ════════════ 시작 ════════════ */

/** 화면 목록에 적어 놓고 함수를 안 만든 경우를 부팅할 때 바로 잡는다. */
function checkViews() {
  const required = [
    "authScreen", "teacherScreen", "studentScreen",
    "teacherStudents", "teacherAssignments", "teacherScores", "teacherAddStudent",
    "teacherStudentDetail", "teacherPinReveal", "teacherConfirmDelete",
    "teacherNewAssignment", "teacherAssignmentDetail",
    "studentList", "studentSolve", "studentDone",
    "gameScreen", "gameDoneScreen", "rankingScreen"
  ];
  const missing = required.filter((name) => typeof window[name] !== "function");
  if (missing.length) {
    throw new Error("화면 함수가 없습니다: " + missing.join(", "));
  }
}

document.addEventListener("scroll", () => {
  $("topbar").classList.toggle("stuck", window.scrollY > 6);
}, { passive: true });

(async function boot() {
  try {
    checkViews();
  } catch (e) {
    $("main").innerHTML = `<div class="empty" style="color:var(--bad)">${esc(e.message)}</div>`;
    return;
  }
  session = loadSession();
  if (!session) {
    go({ screen: "auth", mode: "teacher" });
    return;
  }
  try {
    // 토큰이 아직 살아 있는지 확인하고, 이름 같은 것도 최신으로 맞춘다
    const me = await api("GET", "/api/auth/me");
    saveSession({ token: session.token, user: me });
    enterHome();
  } catch (e) {
    saveSession(null);
    go({ screen: "auth", mode: "teacher" });
  }
})();
