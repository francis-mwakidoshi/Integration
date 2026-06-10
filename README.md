# Country Info Service

Spring Boot REST API that imports country data from the Oorsprong SOAP service, stores it in MySQL, and exposes CRUD endpoints.

---

## Running and testing the application

**Full step-by-step guide for setup, execution, and testing:**

→ **[docs/RUNNING_AND_TESTING.md](docs/RUNNING_AND_TESTING.md)**

That guide covers:

1. Prerequisites (Java, Maven, XAMPP/WampServer)
2. Starting MySQL and creating the `countryinfo` database
3. Building and running the application
4. Health check verification
5. Automated tests (`mvn test`)
6. REST API testing with **Postman** and **curl**
7. Error-handling scenarios (400, 404, 409, 502)
8. Optional SoapUI SOAP testing and phpMyAdmin verification
9. Optional Kubernetes smoke test
10. Pre-submission checklist

---

## Quick start

```bash
# 1. Start MySQL in XAMPP Control Panel (port 3307)
# 2. Create database: countryinfo (phpMyAdmin or scripts/setup-xampp.ps1)
# 3. Run:
mvn spring-boot:run
# 4. Verify:
curl http://localhost:8080/actuator/health
```

Local database: `127.0.0.1:3307`, user `root`, password `todaytest1123today`. See [HELP.md](HELP.md) for details.

---

## Testing the API with Postman

Postman files are in the **`postman/`** folder:

| File | Description |
|------|-------------|
| [postman/Country-Info-Service.postman_collection.json](postman/Country-Info-Service.postman_collection.json) | All REST endpoints |
| [postman/Country-Info-Service.postman_environment.json](postman/Country-Info-Service.postman_environment.json) | Local environment |

Import both in Postman → select **Country Info Service - Local** → run requests in order listed in [RUNNING_AND_TESTING.md](docs/RUNNING_AND_TESTING.md#step-8-test-rest-api-with-postman).

---

## API endpoints

Base URL: `http://localhost:8080`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/countries/import` | Import country via SOAP |
| `GET` | `/api/v1/countries` | List all countries |
| `GET` | `/api/v1/countries/{id}` | Get country by ID |
| `PUT` | `/api/v1/countries/{id}` | Update country |
| `DELETE` | `/api/v1/countries/{id}` | Delete country |
| `GET` | `/actuator/health` | Health check |

---

## Project structure

```
CaseStudyAssessment/
├── postman/                 Postman collection & environment
├── docs/
│   └── RUNNING_AND_TESTING.md   ← Run & test guide
├── src/                     Application source code
├── k8s/                     Kubernetes manifests
├── scripts/                 MySQL / XAMPP setup scripts
├── HELP.md                  Local setup (XAMPP, Docker)
└── pom.xml
```

---

## Documentation

| Document | Purpose |
|----------|---------|
| [docs/RUNNING_AND_TESTING.md](docs/RUNNING_AND_TESTING.md) | **Run and test the application** |
| [HELP.md](HELP.md) | XAMPP setup, Docker, connection settings |
| [docs/SOAPUI_SETUP.md](docs/SOAPUI_SETUP.md) | SOAP testing with SoapUI |
| [docs/KUBERNETES_DEPLOYMENT.md](docs/KUBERNETES_DEPLOYMENT.md) | Kubernetes deployment |
| [docs/KUBERNETES_TROUBLESHOOTING.md](docs/KUBERNETES_TROUBLESHOOTING.md) | Troubleshooting |
| [docs/README.md](docs/README.md) | Documentation index |

---

## Run unit tests

```bash
mvn test
```
