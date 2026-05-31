#!/usr/bin/env bash
# graphify 로컬 스택 (PostgreSQL + Spring Boot API + Vite) 일괄 제어
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"

BACKEND_PORT="${GRAPHIFY_BACKEND_PORT:-8081}"
FRONTEND_PORT="${GRAPHIFY_FRONTEND_PORT:-5173}"
PGADMIN_PORT="${GRAPHIFY_PGADMIN_PORT:-5050}"
POSTGRES_CONTAINER="${GRAPHIFY_POSTGRES_CONTAINER:-graphify-postgres}"
PGADMIN_CONTAINER="${GRAPHIFY_PGADMIN_CONTAINER:-graphify-pgadmin}"
PGADMIN_EMAIL="${GRAPHIFY_PGADMIN_EMAIL:-admin@graphify.dev}"
PGADMIN_PASSWORD="${GRAPHIFY_PGADMIN_PASSWORD:-admin}"

BACKEND_PID_FILE="$RUN_DIR/backend.pid"
FRONTEND_PID_FILE="$RUN_DIR/frontend.pid"
BACKEND_LOG="$RUN_DIR/backend.log"
FRONTEND_LOG="$RUN_DIR/frontend.log"

mkdir -p "$RUN_DIR"

log() { printf '[init] %s\n' "$*"; }
warn() { printf '[init] WARN: %s\n' "$*" >&2; }
die() { warn "$*"; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' 명령을 찾을 수 없습니다."
}

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose -f "$COMPOSE_FILE" "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f "$COMPOSE_FILE" "$@"
  else
    die "docker compose 또는 docker-compose가 필요합니다."
  fi
}

port_pids() {
  local port="$1"
  lsof -ti "tcp:${port}" -sTCP:LISTEN 2>/dev/null || true
}

is_running_pid() {
  local pid="$1"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

kill_port() {
  local port="$1"
  local label="$2"
  local pids
  pids="$(port_pids "$port")"
  if [[ -z "$pids" ]]; then
    return 0
  fi
  log "${label} 포트 ${port} 프로세스 종료 중..."
  # shellcheck disable=SC2086
  kill -15 $pids 2>/dev/null || true
  sleep 2
  pids="$(port_pids "$port")"
  if [[ -n "$pids" ]]; then
    # shellcheck disable=SC2086
    kill -9 $pids 2>/dev/null || true
  fi
}

stop_pid_file() {
  local pid_file="$1"
  local label="$2"
  if [[ ! -f "$pid_file" ]]; then
    return 0
  fi
  local pid
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if is_running_pid "$pid"; then
    log "${label} 프로세스(PID ${pid}) 종료 중..."
    kill -15 "$pid" 2>/dev/null || true
    sleep 2
    if is_running_pid "$pid"; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  fi
  rm -f "$pid_file"
}

wait_for_postgres() {
  log "PostgreSQL 준비 대기 중..."
  local i=0
  local max=60
  while (( i < max )); do
    if docker inspect -f '{{.State.Health.Status}}' "$POSTGRES_CONTAINER" 2>/dev/null | grep -q '^healthy$'; then
      log "PostgreSQL 준비 완료"
      return 0
    fi
    if compose exec -T postgres pg_isready -U graphify -d graphify >/dev/null 2>&1; then
      log "PostgreSQL 준비 완료"
      return 0
    fi
    sleep 1
    (( i += 1 )) || true
  done
  die "PostgreSQL이 ${max}초 내에 준비되지 않았습니다. docker compose logs postgres 를 확인하세요."
}

wait_for_pgadmin() {
  log "pgAdmin 준비 대기 중..."
  local i=0
  local max=60
  local url="http://localhost:${PGADMIN_PORT}/"
  while (( i < max )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "pgAdmin 준비 완료"
      return 0
    fi
    sleep 1
    (( i += 1 )) || true
  done
  warn "pgAdmin 헬스 체크 시간 초과 — docker compose logs pgadmin 확인"
}

http_ok() {
  local url="$1"
  curl -fsS "$url" >/dev/null 2>&1
}

wait_for_url() {
  local url="$1"
  local label="$2"
  local i=0
  local max=120
  while (( i < max )); do
    if http_ok "$url"; then
      log "${label} 준비 완료 (${url})"
      return 0
    fi
    sleep 2
    (( i += 2 )) || true
  done
  warn "${label} 헬스 체크 시간 초과 — 로그: ${BACKEND_LOG}"
}

start_db() {
  require_cmd docker
  require_cmd curl
  log "PostgreSQL + pgAdmin (docker compose) 시작 중..."
  compose up -d postgres pgadmin
  wait_for_postgres
  wait_for_pgadmin
}

stop_db() {
  require_cmd docker
  local running=0
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$POSTGRES_CONTAINER"; then
    running=1
  fi
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$PGADMIN_CONTAINER"; then
    running=1
  fi
  if (( running )); then
    log "PostgreSQL · pgAdmin 중지 중..."
    compose stop postgres pgadmin >/dev/null 2>&1 || true
  else
    log "PostgreSQL · pgAdmin 이미 중지됨"
  fi
}

load_env_file() {
  if [[ -f "$ROOT_DIR/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$ROOT_DIR/.env"
    set +a
  fi
}

start_backend() {
  load_env_file
  if [[ -n "$(port_pids "$BACKEND_PORT")" ]]; then
    warn "백엔드 포트 ${BACKEND_PORT}가 이미 사용 중입니다. 건너뜁니다."
    return 0
  fi
  require_cmd java
  [[ -x "$ROOT_DIR/backend/gradlew" ]] || die "backend/gradlew 가 없습니다."

  log "백엔드 시작 중 (포트 ${BACKEND_PORT})..."
  if [[ -z "${DART_API_KEY:-}" ]]; then
    warn "DART_API_KEY가 비어 있습니다. 루트 .env 를 확인하세요. (실시간 기업 검색 enrich 비활성)"
  else
    log "DART_API_KEY 로드됨 (Open DART 연동 활성)"
  fi
  (
    cd "$ROOT_DIR/backend"
    # Gradle 데몬은 이전 환경 변수를 유지할 수 있어 --no-daemon + env 명시 전달
    nohup env \
      DART_API_KEY="${DART_API_KEY:-}" \
      NEWS_API_KEY="${NEWS_API_KEY:-}" \
      SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/graphify}" \
      SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-graphify}" \
      SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-graphify}" \
      ./gradlew --no-daemon bootRun --quiet --args="--server.port=${BACKEND_PORT}" >>"$BACKEND_LOG" 2>&1 &
    echo $! >"$BACKEND_PID_FILE"
  )
  wait_for_url "http://localhost:${BACKEND_PORT}/actuator/health" "백엔드"
}

stop_backend() {
  stop_pid_file "$BACKEND_PID_FILE" "백엔드"
  kill_port "$BACKEND_PORT" "백엔드"
  # Gradle 데몬이 남는 경우 대비
  pkill -f "gradle.*bootRun.*server.port=${BACKEND_PORT}" 2>/dev/null || true
}

start_frontend() {
  load_env_file
  if [[ -n "$(port_pids "$FRONTEND_PORT")" ]]; then
    warn "프론트 포트 ${FRONTEND_PORT}가 이미 사용 중입니다. 건너뜁니다."
    return 0
  fi
  require_cmd npm
  if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
    log "프론트 의존성 설치 중 (npm install)..."
    (cd "$ROOT_DIR/frontend" && npm install)
  fi

  local vite_api_base="${VITE_API_BASE_URL:-http://localhost:${BACKEND_PORT}}"
  if [[ "$vite_api_base" == *":8080"* ]]; then
    warn "VITE_API_BASE_URL이 8080입니다. 백엔드는 ${BACKEND_PORT} 입니다 → ${vite_api_base//8080/${BACKEND_PORT}} 로 사용합니다."
    vite_api_base="${vite_api_base//8080/${BACKEND_PORT}}"
  fi
  log "프론트엔드 시작 중 (포트 ${FRONTEND_PORT}, API ${vite_api_base})..."
  (
    cd "$ROOT_DIR/frontend"
    nohup env VITE_API_BASE_URL="$vite_api_base" \
      npm run dev -- --host localhost --port "$FRONTEND_PORT" >>"$FRONTEND_LOG" 2>&1 &
    echo $! >"$FRONTEND_PID_FILE"
  )
  sleep 2
  log "프론트엔드: http://localhost:${FRONTEND_PORT}"
}

stop_frontend() {
  stop_pid_file "$FRONTEND_PID_FILE" "프론트엔드"
  kill_port "$FRONTEND_PORT" "프론트엔드"
  pkill -f "vite.*--port ${FRONTEND_PORT}" 2>/dev/null || true
}

cmd_start() {
  start_db
  start_backend
  start_frontend
  echo ""
  log "=== graphify 로컬 스택 실행 중 ==="
  log "  DB      : localhost:5432 (graphify / graphify)"
  log "  pgAdmin : http://localhost:${PGADMIN_PORT} (웹 로그인: ${PGADMIN_EMAIL} / ${PGADMIN_PASSWORD})"
  log "            → DB 서버: graphify (local) — PostgreSQL 비밀번호: graphify"
  log "            → 비밀번호 창이 뜨면 graphify 입력 후 Save password"
  log "  API     : http://localhost:${BACKEND_PORT}"
  log "  Web     : http://localhost:${FRONTEND_PORT}"
  log "  로그    : ${BACKEND_LOG} | ${FRONTEND_LOG}"
}

cmd_stop() {
  stop_frontend
  stop_backend
  stop_db
  log "모든 서비스를 중지했습니다."
}

cmd_restart() {
  cmd_stop
  sleep 1
  cmd_start
}

cmd_status() {
  local db_state="중지"
  local pgadmin_state="중지"
  local be_state="중지"
  local fe_state="중지"

  if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$POSTGRES_CONTAINER"; then
    db_state="실행 (:5432)"
  fi
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$PGADMIN_CONTAINER"; then
    pgadmin_state="실행 (http://localhost:${PGADMIN_PORT})"
  fi
  if [[ -n "$(port_pids "$BACKEND_PORT")" ]]; then
    be_state="실행 (:${BACKEND_PORT})"
  fi
  if [[ -n "$(port_pids "$FRONTEND_PORT")" ]]; then
    fe_state="실행 (:${FRONTEND_PORT})"
  fi

  printf 'PostgreSQL : %s\n' "$db_state"
  printf 'pgAdmin    : %s\n' "$pgadmin_state"
  printf 'Backend    : %s\n' "$be_state"
  printf 'Frontend   : %s\n' "$fe_state"
}

usage() {
  cat <<EOF
사용법: ./init.sh <command>

  start    DB + pgAdmin + 백엔드(:${BACKEND_PORT}) + 프론트(:${FRONTEND_PORT}) 시작
  stop     프론트 → 백엔드 → DB·pgAdmin 순서로 중지
  restart  stop 후 start
  status   실행 상태 확인

환경 변수 (선택):
  GRAPHIFY_BACKEND_PORT=8081
  GRAPHIFY_FRONTEND_PORT=5173
  GRAPHIFY_PGADMIN_PORT=5050
  GRAPHIFY_PGADMIN_EMAIL=admin@graphify.dev
  GRAPHIFY_PGADMIN_PASSWORD=admin
EOF
}

main() {
  local cmd="${1:-}"
  case "$cmd" in
    start) cmd_start ;;
    stop) cmd_stop ;;
    restart) cmd_restart ;;
    status) cmd_status ;;
    -h|--help|help|"") usage ;;
    *) die "알 수 없는 명령: ${cmd}. ./init.sh help 참고" ;;
  esac
}

main "$@"
