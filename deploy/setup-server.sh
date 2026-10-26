#!/usr/bin/env bash
# =====================================================================
#  Munot Hospital backend - ONE-TIME server setup (Oracle Cloud VM)
#  Chalvaycha:   sudo bash setup-server.sh
#  Supports: Ubuntu 22.04/24.04 (apt)  and  Oracle Linux 8/9 (dnf)
#
#  Optional variables (sudo VAR=value bash setup-server.sh  OR  sudo -E):
#    CORS_ALLOWED_ORIGINS  frontend URL, e.g. https://munot.example.com
#    ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_NAME   first admin login
#    DOMAIN                api domain (HTTPS Let's Encrypt, Ubuntu only)
#    CERT_EMAIL            email for Let's Encrypt (default: ADMIN_EMAIL)
# =====================================================================
set -euo pipefail

APP_USER=munot
APP_DIR=/opt/munot
ENV_DIR=/etc/munot
ENV_FILE=$ENV_DIR/munot.env
DB_NAME=munot_hospital
DB_USER=munot_app

log()  { echo -e "\n\033[1;32m==> $*\033[0m"; }
warn() { echo -e "\033[1;33m[WARN] $*\033[0m"; }
die()  { echo -e "\033[1;31m[ERROR] $*\033[0m" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "Root ne chalva:  sudo bash setup-server.sh"

# Windows line-endings (CRLF) cha protection
if grep -q $'\r' "$0" 2>/dev/null; then
  die "Script madhe Windows line endings aahet. Server var run kara:  sed -i 's/\\r\$//' setup-server.sh   mag parat chalva."
fi

if command -v apt-get >/dev/null 2>&1; then PM=apt
elif command -v dnf >/dev/null 2>&1;     then PM=dnf
else die "apt kinva dnf sapadla nahi - Ubuntu kinva Oracle Linux vapra."; fi

DOMAIN="${DOMAIN:-}"
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-}"
ADMIN_EMAIL="${ADMIN_EMAIL:-}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
ADMIN_NAME="${ADMIN_NAME:-Hospital Admin}"
CERT_EMAIL="${CERT_EMAIL:-$ADMIN_EMAIL}"

# ---------- questions (only when env file is not created yet) ----------
if [ ! -f "$ENV_FILE" ] && [ -t 0 ]; then
  if [ -z "$CORS_ALLOWED_ORIGINS" ]; then
    read -r -p "Frontend URL (CORS), e.g. https://munot.example.com  [Enter = http://localhost:5173]: " CORS_ALLOWED_ORIGINS
  fi
  if [ -z "$ADMIN_EMAIL" ]; then
    read -r -p "First admin email: " ADMIN_EMAIL
  fi
  if [ -z "$ADMIN_PASSWORD" ]; then
    read -r -s -p "First admin password (8-72 chars, letter + digit; allowed: A-Z a-z 0-9 @ _ . ! + = -): " ADMIN_PASSWORD; echo
  fi
fi
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:5173}"

if [ ! -f "$ENV_FILE" ]; then
  if [ -n "$ADMIN_EMAIL" ] || [ -n "$ADMIN_PASSWORD" ]; then
    [[ "$ADMIN_EMAIL" == *@*.* ]] || die "Admin email barobar nahi."
    [[ "$ADMIN_PASSWORD" =~ ^[A-Za-z0-9@_.!+=-]{8,72}$ ]] || die "Admin password: 8-72 chars, fakt A-Z a-z 0-9 @ _ . ! + = - vapra."
    [[ "$ADMIN_PASSWORD" =~ [A-Za-z] && "$ADMIN_PASSWORD" =~ [0-9] ]] || die "Admin password madhe kamit ek letter ani ek digit pahije."
  else
    warn "Admin email/password dile nahit - admin account tayar honar nahi. Nantar $ENV_FILE madhe ADMIN_EMAIL/ADMIN_PASSWORD takun 'sudo systemctl restart munot' kara."
  fi
fi
CERT_EMAIL="${CERT_EMAIL:-$ADMIN_EMAIL}"

# ---------- 1. packages ----------
log "Packages install hot aahet (Java 21, MySQL, nginx)..."
if [ "$PM" = apt ]; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -y
  apt-get install -y openjdk-21-jre-headless mysql-server nginx curl openssl ca-certificates
  MYSQL_SVC=mysql
else
  dnf install -y java-21-openjdk-headless mysql-server nginx curl openssl ca-certificates
  MYSQL_SVC=mysqld
fi

JAVA_BIN="$(ls -d /usr/lib/jvm/java-21-openjdk*/bin/java 2>/dev/null | head -n 1 || true)"
[ -n "$JAVA_BIN" ] && [ -x "$JAVA_BIN" ] || die "Java 21 sapadla nahi. 'apt install openjdk-21-jre-headless' (kinva dnf) manually kara."
"$JAVA_BIN" -version 2>&1 | head -n 1

# ---------- 2. swap + memory based JVM options ----------
MEM_MB="$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)"
if [ "$MEM_MB" -lt 2000 ] && [ "$(swapon --show --noheadings | wc -l)" -eq 0 ]; then
  log "RAM kami (${MEM_MB} MB) - 2 GB swap banavat aahe..."
  fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile; mkswap /swapfile; swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
if   [ "$MEM_MB" -lt 1500 ]; then JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseSerialGC"
elif [ "$MEM_MB" -lt 4000 ]; then JAVA_OPTS="-Xms256m -Xmx768m"
else                              JAVA_OPTS="-Xms512m -Xmx2g"; fi

# ---------- 3. app user + folders ----------
log "App user ani folders..."
id "$APP_USER" >/dev/null 2>&1 || useradd --system --home-dir "$APP_DIR" --shell /usr/sbin/nologin "$APP_USER"
install -d -o "$APP_USER" -g "$APP_USER" -m 750 "$APP_DIR" "$APP_DIR/uploads" "$APP_DIR/backups"
install -d -o root -g "$APP_USER" -m 750 "$ENV_DIR"

# ---------- 4. MySQL ----------
log "MySQL start ani database tayar hot aahe..."
systemctl enable --now "$MYSQL_SVC"
for i in $(seq 1 30); do mysql -e 'SELECT 1' >/dev/null 2>&1 && break; sleep 2; done
mysql -e 'SELECT 1' >/dev/null 2>&1 || die "MySQL start jhala nahi: journalctl -u $MYSQL_SVC -n 50"

if [ -f "$ENV_FILE" ]; then
  DB_PASSWORD="$(grep -E '^DB_PASSWORD=' "$ENV_FILE" | head -n1 | cut -d= -f2-)"
else
  DB_PASSWORD="$(openssl rand -hex 16)"
fi
mysql <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

# ---------- 5. environment file ----------
if [ -f "$ENV_FILE" ]; then
  warn "$ENV_FILE aadhich aahe - badalle nahi."
else
  log "Environment file banavat aahe: $ENV_FILE"
  cat > "$ENV_FILE" <<ENV
DB_URL=jdbc:mysql://127.0.0.1:3306/$DB_NAME?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$(openssl rand -hex 32)
JWT_EXPIRATION_MINUTES=480
CORS_ALLOWED_ORIGINS=$CORS_ALLOWED_ORIGINS
PORT=8080
APP_TIMEZONE=Asia/Kolkata
UPLOAD_DIR=$APP_DIR/uploads
SEED_DEMO=false
SWAGGER_ENABLED=false
ADMIN_EMAIL=$ADMIN_EMAIL
ADMIN_PASSWORD=$ADMIN_PASSWORD
ADMIN_NAME=$ADMIN_NAME
JAVA_OPTS=$JAVA_OPTS
ENV
  chown root:"$APP_USER" "$ENV_FILE"; chmod 640 "$ENV_FILE"
fi

# ---------- 6. deploy helper (GitHub Actions ha vaparto) ----------
log "Deploy helper: /usr/local/bin/munot-deploy"
cat > /usr/local/bin/munot-deploy <<'DEPLOY_EOF'
#!/usr/bin/env bash
# usage: sudo munot-deploy /tmp/munot-new.jar   -> install, restart, health check, auto-rollback
set -uo pipefail
NEW="${1:?usage: munot-deploy <jar>}"
APP=/opt/munot/app.jar
ENV_FILE=/etc/munot/munot.env
[ -s "$NEW" ] || { echo "Jar file sapadli nahi: $NEW"; exit 1; }

PORT="$(grep -E '^PORT=' "$ENV_FILE" 2>/dev/null | head -n1 | cut -d= -f2 || true)"
PORT="${PORT:-8080}"

[ -f "$APP" ] && cp -f "$APP" "$APP.bak"
install -o munot -g munot -m 0640 "$NEW" "$APP"
rm -f "$NEW"
systemctl restart munot

healthy() {
  for _ in $(seq 1 45); do                 # max ~2.5 minute (pahilya veli Flyway migrations chalatat)
    out="$(curl -fsS "http://127.0.0.1:$PORT/actuator/health" 2>/dev/null || true)"
    [[ "$out" == *'"status":"UP"'* ]] && return 0
    sleep 3
  done
  return 1
}

if healthy; then echo "Deploy OK - app UP"; exit 0; fi

echo "Health check FAIL. Last logs:"
journalctl -u munot -n 80 --no-pager || true
if [ -f "$APP.bak" ]; then
  echo "Junya version var rollback kartoy..."
  cp -f "$APP.bak" "$APP"; chown munot:munot "$APP"
  systemctl restart munot
fi
exit 1
DEPLOY_EOF
chmod 755 /usr/local/bin/munot-deploy

# ---------- 7. systemd service ----------
log "systemd service: munot"
cat > /etc/systemd/system/munot.service <<UNIT_EOF
[Unit]
Description=Munot Hospital backend (Spring Boot)
After=network-online.target mysql.service mysqld.service
Wants=network-online.target

[Service]
User=$APP_USER
Group=$APP_USER
WorkingDirectory=$APP_DIR
EnvironmentFile=$ENV_FILE
ExecStart=$JAVA_BIN \$JAVA_OPTS -jar $APP_DIR/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
TimeoutStopSec=40
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
UNIT_EOF
systemctl daemon-reload
systemctl enable munot

# ---------- 8. daily backup (DB + uploads) ----------
log "Daily backup timer (02:30)"
cat > /usr/local/bin/munot-backup <<'BK_EOF'
#!/usr/bin/env bash
set -euo pipefail
D=/opt/munot/backups; TS="$(date +%F)"
mysqldump --single-transaction --routines munot_hospital | gzip > "$D/db-$TS.sql.gz"
tar -czf "$D/uploads-$TS.tar.gz" -C /opt/munot uploads
find "$D" -type f -mtime +14 -delete
BK_EOF
chmod 755 /usr/local/bin/munot-backup
cat > /etc/systemd/system/munot-backup.service <<'EOF'
[Unit]
Description=Munot daily backup
[Service]
Type=oneshot
ExecStart=/usr/local/bin/munot-backup
EOF
cat > /etc/systemd/system/munot-backup.timer <<'EOF'
[Unit]
Description=Munot daily backup timer
[Timer]
OnCalendar=*-*-* 02:30:00
Persistent=true
[Install]
WantedBy=timers.target
EOF
systemctl daemon-reload
systemctl enable --now munot-backup.timer

# ---------- 9. nginx reverse proxy ----------
log "nginx reverse proxy (80 -> 8080)"
[ -e /etc/nginx/sites-enabled/default ] && rm -f /etc/nginx/sites-enabled/default
cat > /etc/nginx/conf.d/munot.conf <<NGINX_EOF
server {
    listen 80 default_server;
    server_name ${DOMAIN:-_};
    client_max_body_size 20m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$remote_addr;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }
    location = /actuator/health {
        proxy_pass http://127.0.0.1:8080;
    }
    location / { return 404; }
}
NGINX_EOF
nginx -t
systemctl enable --now nginx
systemctl reload nginx

# SELinux (Oracle Linux): nginx la localhost:8080 la jodu dya
if command -v setsebool >/dev/null 2>&1; then setsebool -P httpd_can_network_connect 1 || true; fi

# ---------- 10. server firewall (Oracle Ubuntu images 80/443 block kartat) ----------
log "Firewall madhe 80/443 ughadat aahe..."
if command -v firewall-cmd >/dev/null 2>&1 && systemctl is-active --quiet firewalld; then
  firewall-cmd --permanent --add-service=http --add-service=https
  firewall-cmd --reload
elif command -v iptables >/dev/null 2>&1; then
  for p in 80 443; do
    if ! iptables -C INPUT -p tcp --dport "$p" -j ACCEPT 2>/dev/null; then
      N="$(iptables -L INPUT --line-numbers -n | awk '/REJECT|DROP/ {print $1; exit}')"
      if [ -n "$N" ]; then iptables -I INPUT "$N" -p tcp --dport "$p" -j ACCEPT
      else iptables -A INPUT -p tcp --dport "$p" -j ACCEPT; fi
    fi
  done
  if [ "$PM" = apt ]; then
    command -v netfilter-persistent >/dev/null 2>&1 || DEBIAN_FRONTEND=noninteractive apt-get install -y iptables-persistent
    netfilter-persistent save || true
  fi
fi

# ---------- 11. HTTPS (optional, Ubuntu) ----------
if [ -n "$DOMAIN" ]; then
  if [ "$PM" = apt ] && [ -n "$CERT_EMAIL" ]; then
    log "HTTPS certificate ($DOMAIN)..."
    apt-get install -y certbot python3-certbot-nginx
    certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$CERT_EMAIL" --redirect \
      || warn "Certbot fail jhala. DNS ($DOMAIN -> server IP) ani Oracle security list madhe 80/443 check kara, mag:  sudo certbot --nginx -d $DOMAIN"
  else
    warn "HTTPS auto-setup fakt Ubuntu var + CERT_EMAIL/ADMIN_EMAIL sobat chalte. Manually certbot vapra."
  fi
fi

log "SETUP PURNA JHALA"
cat <<DONE

Aata:
  1. Oracle Cloud Console > VCN > Security List madhe Ingress rule ughada: TCP 80 (ani HTTPS pahije tar 443), source 0.0.0.0/0
     (8080 ani 3306 ughadu naka).
  2. GitHub var code push kara (main branch) - Actions jar upload karun app start karel.
  3. Check:  curl http://localhost/actuator/health     (server var)
             curl http://<SERVER-IP>/actuator/health   (baher kadun)
  4. Logs:   sudo journalctl -u munot -f
  5. Settings badalayche:  sudo nano $ENV_FILE  ->  sudo systemctl restart munot
  6. Pahila admin login jhalyavar ADMIN_PASSWORD $ENV_FILE madhun kadhun taka ani restart kara.
  7. Backups: $APP_DIR/backups (roj 02:30). Hya serverchya baher pan copy thevayla visru naka.
DONE
