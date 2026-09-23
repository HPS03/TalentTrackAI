# Deployment Guide

You **don't need to know Python** to deploy this project. The AI service ships as a Docker image, so Docker
installs Python and every library for you. The only commands you run are `git` and `docker`.

The platform has four parts:

| Service | Tech | Port inside Docker |
|---|---|---|
| `frontend` | React build served by Nginx (also proxies `/api` to the backend) | 80 |
| `backend` | Spring Boot API | 8080 |
| `ai-service` | FastAPI (Python) | 8000 |
| `mysql` | MySQL 8.4 | 3306 |

Pick one option:

- **Option A – One cloud server + Docker Compose (recommended).** Everything runs exactly as it does on your laptop. Best for interviews ("I deployed it on AWS EC2 with Docker Compose").
- **Option B – Free managed hosting (Render + Aiven).** No server to manage, $0, but free services sleep when idle.

Before you start, push the project to your own GitHub repository.

---

## Option 0: Run it on your own computer first

1. Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) and start it.
2. In the project folder, run:
   ```bash
   docker compose up --build
   ```
3. Open http://localhost:3000 and log in with `recruiter@talenttrack.ai` / `Recruiter@123`.

If this works, deployment is the same thing on a server.

---

## Option A: Cloud VM with Docker Compose (AWS EC2, Oracle Cloud, DigitalOcean…)

### 1. Create the server
- **AWS EC2**: launch an **Ubuntu 24.04** instance. `t3.small` (2 GB RAM) is recommended. The free-tier `t2.micro`/`t3.micro` (1 GB) works only if you add swap (step 3).
- **Oracle Cloud Always Free**: an Ampere (ARM) VM with 4 CPUs and 24 GB RAM is free forever. All images used here support ARM.
- In the **security group / firewall**, allow inbound **SSH (22)** and **HTTP (80)**, plus **HTTPS (443)** if you'll add a domain.
  Do **not** open 3306, 8080 or 8000 to the internet.

### 2. Connect to the server
```bash
ssh -i your-key.pem ubuntu@YOUR_SERVER_IP
```

### 3. Install Docker (and add swap on small machines)
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker
docker compose version            # should print a version

# Only for 1 GB RAM servers: add 2 GB swap so Java + MySQL fit
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 4. Get the code and configure secrets
```bash
git clone https://github.com/<you>/<your-repo>.git TalentTrackAI
cd TalentTrackAI
cp .env.example .env
nano .env
```
Change these values in `.env`:
```dotenv
DB_PASSWORD=<a strong password>
MYSQL_ROOT_PASSWORD=<another strong password>
JWT_SECRET=<paste output of: openssl rand -base64 48>
ADMIN_PASSWORD=<your admin password>
WEB_PORT=80
CORS_ALLOWED_ORIGINS=http://YOUR_SERVER_IP
SEED_DEMO_DATA=true        # set to false if you don't want demo users/jobs
```

### 5. Start everything
```bash
docker compose up -d --build     # -d = run in the background
docker compose ps                # all services should become "healthy"
docker compose logs -f backend   # watch the logs (Ctrl+C to stop watching)
```
Open `http://YOUR_SERVER_IP`. The React app calls `/api` on the same domain and Nginx forwards it to the backend, so only port 80 is public.

### 6. Day-2 operations
| Task | Command |
|---|---|
| Deploy a new version | `git pull && docker compose up -d --build` |
| View logs | `docker compose logs -f backend` (or `ai-service`, `frontend`, `mysql`) |
| Restart | `docker compose restart backend` |
| Stop | `docker compose down` (data is kept in Docker volumes) |
| Back up the DB | `docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" talenttrack' > backup.sql` |

### 7. (Optional) Domain + free HTTPS
1. Point an `A` record for your domain (e.g. `talenttrack.yourname.dev`) at the server IP.
2. Set `WEB_PORT=8081` in `.env` and run `docker compose up -d`.
3. Install [Caddy](https://caddyserver.com/docs/install#debian-ubuntu-raspbian). Put this in `/etc/caddy/Caddyfile`:
   ```
   talenttrack.yourname.dev {
       reverse_proxy localhost:8081
   }
   ```
   Then run `sudo systemctl reload caddy`. Caddy gets and renews a Let's Encrypt certificate automatically.
4. Update `CORS_ALLOWED_ORIGINS=https://talenttrack.yourname.dev` in `.env` and run `docker compose up -d`.

---

## Option B: Free managed hosting (Render + Aiven MySQL)

Render runs the Docker images and the static site. Aiven hosts a free MySQL database.

### 1. Create a free MySQL database on Aiven
1. Sign up at https://aiven.io → **Create service** → **MySQL** → **Free plan**.
2. When it's running, open the service **Overview** and copy the **Host**, **Port**, **User** (`avnadmin`), **Password** and **Database** (`defaultdb`).
3. Your JDBC URL is:
   ```
   jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&serverTimezone=UTC
   ```

### 2. Deploy on Render with the blueprint
1. Sign up at https://render.com with GitHub.
2. **New → Blueprint** → select your repository. Render reads [`render.yaml`](../render.yaml) and proposes 3 services:
   `talenttrack-ai`, `talenttrack-api`, `talenttrack-web`.
3. Fill in the variables it asks for. You can leave a URL blank for now and edit it after the first deploy, once you know the `.onrender.com` addresses:

| Service | Variable | Value |
|---|---|---|
| talenttrack-api | `DB_URL` | the JDBC URL from Aiven |
| talenttrack-api | `DB_USERNAME` / `DB_PASSWORD` | from Aiven |
| talenttrack-api | `AI_SERVICE_URL` | `https://talenttrack-ai.onrender.com` (your AI service URL) |
| talenttrack-api | `CORS_ALLOWED_ORIGINS` | `https://talenttrack-web.onrender.com` (your web URL) |
| talenttrack-api | `ADMIN_PASSWORD` | your admin password |
| talenttrack-web | `VITE_API_URL` | `https://talenttrack-api.onrender.com` (your API URL, no trailing slash) |

4. After changing `VITE_API_URL`, trigger **Manual Deploy** on `talenttrack-web`. Vite bakes the variable in at build time.
5. Open the web URL and log in with the demo accounts.

**Free-tier caveats (fine for a portfolio demo):**
- Free services **sleep after ~15 minutes idle**. The first request then takes 30–60 s while they wake up. Open the site a minute before a demo or interview.
- Render's free disk is **ephemeral**, so uploaded resume files are lost when the service redeploys. Everything in MySQL is kept.
  For permanent file storage, use a paid disk or move storage to S3.

### Alternative: Railway
Railway can run all four services, including MySQL, from this repository. Create a project, add a **MySQL** database, then add three
services from GitHub with root directories `ai-service`, `backend` and `frontend`. Give the backend the same variables as
the table above, using Railway's MySQL connection values. For the frontend, leave `VITE_API_URL` unset and instead set
`BACKEND_URL` to the backend's private URL (e.g. `http://backend.railway.internal:8080`). Nginx then proxies `/api` on the same domain.

---

## Configuration reference (backend)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/talenttrack…` | JDBC connection string |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | DB credentials |
| `JWT_SECRET` | dev value | **Must** be ≥ 32 characters and secret in production |
| `JWT_ACCESS_MINUTES` / `JWT_REFRESH_DAYS` | `60` / `7` | Token lifetimes |
| `AI_SERVICE_URL` | `http://localhost:8000` | Where the FastAPI service lives |
| `CORS_ALLOWED_ORIGINS` | localhost dev URLs | Comma-separated list of allowed frontend origins |
| `UPLOAD_DIR` | `./uploads` | Resume storage directory |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@talenttrack.ai` / `Admin@123` | Admin account created on first start |
| `SEED_DEMO_DATA` | `true` | Create demo recruiters, candidates, jobs and a populated board |
| `PORT` | `8080` | HTTP port (set automatically by Render/Railway) |

## Troubleshooting

| Symptom | Fix |
|---|---|
| `backend` keeps restarting | `docker compose logs backend`. Usually the DB isn't reachable or the credentials are wrong. |
| `JWT secret must be at least 32 characters` | Set a longer `JWT_SECRET`. |
| Browser shows CORS errors (Option B) | `CORS_ALLOWED_ORIGINS` must exactly match the web URL (scheme + host, no trailing slash). |
| ATS summary says "AI service unavailable" | The backend can't reach `AI_SERVICE_URL`. Check that URL and the ai-service logs. The app keeps working with the built-in matcher. |
| Out of memory on a 1 GB server | Add swap (Option A, step 3) or use a 2 GB instance. |
| Frontend shows 502 on `/api` | The backend is still starting (Spring Boot takes ~20–40 s) or unhealthy. Check `docker compose ps`. |
