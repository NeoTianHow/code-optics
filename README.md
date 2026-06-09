# Code Optics

Code Optics is a full-stack AI-assisted code review dashboard for GitLab projects. It helps inspect groups, projects, branches, merge requests, custom review rules, branch summaries, and generated project reports.

## Tech Stack

- Backend: Spring Boot, WebFlux, Spring AI, H2, JasperReports
- Frontend: Angular
- Optional local services: Chroma via Docker Compose

## Configuration

The backend reads sensitive values from environment variables. Do not commit real tokens or webhook secrets.

```bash
GITLAB_API_URL=https://gitlab.com/api/v4
GITLAB_API_PRIVATE_TOKEN=your_gitlab_token
GITLAB_WEBHOOK_SECRET=your_webhook_secret
APP_REPORT_STORAGE_LOCATION=./reports
SPRING_DATASOURCE_PASSWORD=
```

## Run Locally

Requirements:

- Node.js 20.19 or newer for the Angular frontend
- Java 17 or newer for the Spring Boot backend

Backend:

```bash
cd be
./mvnw spring-boot:run
```

Frontend:

```bash
cd fe
npm install
npm start
```

Optional Chroma service:

```bash
docker compose up -d
```

## Notes

Generated logs, local databases, reports, build artifacts, and environment files are intentionally ignored.
