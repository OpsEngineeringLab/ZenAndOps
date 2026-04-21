import http from "k6/http";
import { check, group, sleep } from "k6";
import { Rate, Trend, Counter } from "k6/metrics";
import { SharedArray } from "k6/data";

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------
const loginFailRate  = new Rate("login_failures");
const apiErrorRate   = new Rate("api_errors");
const rateLimited    = new Counter("rate_limited_requests");
const loginDuration  = new Trend("login_duration", true);
const profileDuration    = new Trend("profile_duration", true);
const dashboardDuration  = new Trend("dashboard_duration", true);
const listUsersDuration  = new Trend("list_users_duration", true);
const listRolesDuration  = new Trend("list_roles_duration", true);
const listTagsDuration   = new Trend("list_tags_duration", true);
const refreshDuration    = new Trend("refresh_duration", true);

// ---------------------------------------------------------------------------
// Configuration — override via environment variables
// ---------------------------------------------------------------------------
const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";

const USERS = [
  { login: "admin", password: "admin" },
  { login: "user",  password: "user" },
  { login: "guest", password: "guest" },
];

// ---------------------------------------------------------------------------
// Scenarios
//
// No artificial rate-limit budgeting — the script pushes as much traffic as
// the VU count allows.  Adjust the server-side rate limit via .env:
//   GATEWAY_RATE_LIMIT_MAX_REQUESTS   (default 100)
//   GATEWAY_RATE_LIMIT_WINDOW_SECONDS (default 60)
//
// Strategy:
//   warmup    — 1 VU, slow pace, validates connectivity
//   sustained — ramp to 10 VUs with realistic think-time
//   spike     — burst to 30 VUs to stress the system
// ---------------------------------------------------------------------------
export const options = {
  scenarios: {
    warmup: {
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 5,
      maxDuration: "30s",
      gracefulStop: "5s",
      exec: "warmup",
    },
    sustained: {
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: "30s",  target: 5 },
        { duration: "30s",  target: 10 },
        { duration: "120s", target: 10 },
        { duration: "30s",  target: 0 },
      ],
      startTime: "30s",
      gracefulStop: "10s",
      exec: "sustained",
    },
    spike: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 30 },
        { duration: "30s", target: 30 },
        { duration: "10s", target: 0 },
      ],
      startTime: "245s",
      gracefulStop: "10s",
      exec: "spike",
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<3000"],
    login_failures:    ["rate<0.2"],
    // 429s are expected under heavy load; we track them separately via rateLimited counter
    api_errors:        ["rate<0.5"],
  },
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
const jsonHeaders = { "Content-Type": "application/json" };

function authenticate(user) {
  // 1% of attempts use wrong credentials to generate failure metrics
  const simulateFailure = Math.random() < 0.01;
  const payload = simulateFailure
    ? { login: user.login, password: "wrong-password-loadtest" }
    : { login: user.login, password: user.password };

  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify(payload),
    { headers: jsonHeaders, tags: { name: "POST /auth/login" } }
  );
  loginDuration.add(res.timings.duration);

  if (res.status === 429) {
    rateLimited.add(1);
    loginFailRate.add(false);
    return null;
  }

  if (simulateFailure) {
    check(res, { "simulated login failure 401": (r) => r.status === 401 });
    loginFailRate.add(false); // intentional failure, not a test failure
    return null;
  }

  const ok = check(res, {
    "login 200": (r) => r.status === 200,
    "login has tokens": (r) => {
      try { return !!r.json().accessToken; } catch { return false; }
    },
  });
  loginFailRate.add(!ok);
  if (!ok) return null;

  return {
    accessToken:  res.json().accessToken,
    refreshToken: res.json().refreshToken,
  };
}

function authHeaders(token) {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };
}

function pickRandom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function trackResponse(res, trend) {
  trend.add(res.timings.duration);
  if (res.status === 429) {
    rateLimited.add(1);
    return;
  }
  apiErrorRate.add(res.status >= 400);
}

// ---------------------------------------------------------------------------
// Warm-up: validate connectivity with minimal requests
// ---------------------------------------------------------------------------
export function warmup() {
  const user = pickRandom(USERS);
  const tokens = authenticate(user);
  if (!tokens) { sleep(5); return; }

  const hdrs = authHeaders(tokens.accessToken);

  const res = http.get(`${BASE_URL}/api/v1/profile`, {
    headers: hdrs,
    tags: { name: "GET /profile" },
  });
  trackResponse(res, profileDuration);
  check(res, { "warmup profile 200": (r) => r.status === 200 });

  sleep(5);
}

// ---------------------------------------------------------------------------
// Sustained: realistic user flow with natural think-time (no rate-limit budgeting)
// ---------------------------------------------------------------------------
export function sustained() {
  const user = pickRandom(USERS);
  const tokens = authenticate(user);
  if (!tokens) { sleep(2); return; }

  const hdrs = authHeaders(tokens.accessToken);

  group("profile", () => {
    const res = http.get(`${BASE_URL}/api/v1/profile`, {
      headers: hdrs,
      tags: { name: "GET /profile" },
    });
    trackResponse(res, profileDuration);
    check(res, { "profile 200": (r) => r.status === 200 });
  });

  sleep(1);

  group("dashboard", () => {
    const res = http.get(`${BASE_URL}/api/v1/dashboard`, {
      headers: hdrs,
      tags: { name: "GET /dashboard" },
    });
    trackResponse(res, dashboardDuration);
    check(res, { "dashboard 200": (r) => r.status === 200 });
  });

  sleep(1);

  // Admin-only endpoints
  if (user.login === "admin") {
    group("admin-list", () => {
      const page = Math.floor(Math.random() * 3);

      const usersRes = http.get(
        `${BASE_URL}/api/v1/users?page=${page}&size=10`,
        { headers: hdrs, tags: { name: "GET /users" } }
      );
      trackResponse(usersRes, listUsersDuration);

      sleep(0.5);

      const rolesRes = http.get(`${BASE_URL}/api/v1/roles?page=0&size=10`, {
        headers: hdrs,
        tags: { name: "GET /roles" },
      });
      trackResponse(rolesRes, listRolesDuration);

      sleep(0.5);

      const tagsRes = http.get(`${BASE_URL}/api/v1/tags?page=0&size=10`, {
        headers: hdrs,
        tags: { name: "GET /tags" },
      });
      trackResponse(tagsRes, listTagsDuration);
    });
  }

  sleep(1);

  // Token refresh
  group("refresh", () => {
    const res = http.post(
      `${BASE_URL}/api/v1/auth/refresh`,
      JSON.stringify({ refreshToken: tokens.refreshToken }),
      { headers: jsonHeaders, tags: { name: "POST /auth/refresh" } }
    );
    trackResponse(res, refreshDuration);
    check(res, { "refresh 200": (r) => r.status === 200 });
  });

  // Natural think-time between iterations
  sleep(Math.random() * 2 + 1);
}

// ---------------------------------------------------------------------------
// Spike: aggressive burst to stress the system under heavy load
// ---------------------------------------------------------------------------
export function spike() {
  const user = pickRandom(USERS);
  const tokens = authenticate(user);
  if (!tokens) { sleep(0.5); return; }

  const hdrs = authHeaders(tokens.accessToken);

  const endpoints = [
    { url: `${BASE_URL}/api/v1/profile`,   name: "GET /profile" },
    { url: `${BASE_URL}/api/v1/dashboard`, name: "GET /dashboard" },
  ];

  if (user.login === "admin") {
    endpoints.push(
      { url: `${BASE_URL}/api/v1/users?page=0&size=5`, name: "GET /users" },
      { url: `${BASE_URL}/api/v1/roles?page=0&size=5`, name: "GET /roles" },
      { url: `${BASE_URL}/api/v1/tags?page=0&size=5`,  name: "GET /tags" }
    );
  }

  // Fire 3-5 rapid requests
  for (let i = 0; i < 3 + Math.floor(Math.random() * 3); i++) {
    const ep = pickRandom(endpoints);
    const res = http.get(ep.url, { headers: hdrs, tags: { name: ep.name } });

    if (res.status === 429) {
      rateLimited.add(1);
      check(res, { "spike rate-limited (expected)": (r) => r.status === 429 });
    } else {
      apiErrorRate.add(res.status >= 400);
      check(res, { "spike response ok": (r) => r.status < 400 });
    }
  }

  sleep(0.2);
}
