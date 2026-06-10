# Running and Testing the Application

This guide walks through everything needed to run the Country Info Service locally and verify that it works. Follow the steps in order for a first-time setup.

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|--------|
| Java JDK | 17+ | `java -version` |
| Maven | 3.9+ | Or use `./mvnw` in the project root |
| XAMPP | Any recent | MySQL/MariaDB on port **3307** |
| Postman | Any recent | Optional; collection provided in `postman/` |

---

## Part 1 — Run the application

### Step 1: Start MySQL (XAMPP)

1. Open **XAMPP Control Panel**.
2. Click **Start** next to **MySQL**.
3. Confirm MySQL is running (green status).

The application expects XAMPP MySQL on port **3307**. Verify in `D:\xampp\mysql\bin\my.ini`:

```ini
[mysqld]
port=3307
```

### Step 2: Create the database

Open phpMyAdmin at [http://localhost/phpmyadmin](http://localhost/phpmyadmin) and run:

```sql
CREATE DATABASE IF NOT EXISTS countryinfo;
```

### Step 3: Confirm application configuration

Defaults in `src/main/resources/application.yml`:

| Setting | Value |
|---------|--------|
| JDBC URL | `jdbc:mysql://127.0.0.1:3307/countryinfo?` |
| Username | `root` |
| Password | `todaytest1123today` |
| Server port | `8080` |

If your XAMPP password or port differs, update `application.yml`

### Step 4: Build the project

From the project root (`CaseStudyAssessment/`):

```bash
mvn clean package
```

Expected result: `BUILD SUCCESS` and JAR at `target/country-info-service-1.0.0.jar`.

### Step 5: Start the application

**Option A — Maven (development)**

```bash
mvn spring-boot:run
```

**Option B — JAR**

```bash
java -jar target/country-info-service-1.0.0.jar
```

Wait until the log shows:

```
Tomcat started on port 8080
Started CountryInfoServiceApplication
```

The API is available at **http://localhost:8080**.

### Step 6: Verify the application is up

Open a browser or terminal:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

If this fails, check that MySQL is running on port 3307 and credentials match `application.yml`

---

## Part 2 — Test the application

### Step 7: Run automated unit tests

With the app stopped or running (tests use their own in-memory H2 database):

```bash
mvn test
```

Expected result: all tests pass 

### Step 8: Test REST API with Postman

1. Open **Postman**.
2. Click **Import** and select both files from the `postman/` folder:
   - `Country-Info-Service.postman_collection.json`
   - `Country-Info-Service.postman_environment.json`
3. Select environment **Country Info Service - Local** (top-right).
4. Run requests in this order:

| # | Request | Expected result |
|---|---------|-----------------|
| 1 | **Health Check** | `200 OK`, `"status":"UP"` |
| 2 | **Import Country** | `201 Created`, JSON with country details (e.g. Kenya) |
| 3 | **Get All Countries** | `200 OK`, array with imported country |
| 4 | **Get Country by ID** | `200 OK`, same country (`countryId` set automatically after import) |
| 5 | **Update Country** | `200 OK`, updated fields in response |
| 6 | **Delete Country** | `204 No Content` |
| 7 | **Get Country by ID - Not Found** | `404 Not Found` (after delete) |

**Import Country** example response (`201`):

```json
{
  "id": 1,
  "isoCode": "KE",
  "name": "Kenya",
  "capitalCity": "Nairobi",
  "phoneCode": "254",
  "continentCode": "AF",
  "currencyCode": "KES",
  "countryFlagUrl": "...",
  "languages": ["English", "Swahili"]
}
```

### Step 9: Test error handling

| Test | Request | Expected |
|------|---------|----------|
| Validation error | `POST /import` with `{"name": ""}` | `400 Bad Request` |
| Not found | `GET /api/v1/countries/99999` | `404 Not Found` |
| Duplicate import | Import the same country twice | `409 Conflict` on second import |
| SOAP unavailable | Import with no internet | `502 Bad Gateway` |

Use the **Import Country - Validation Error** request in the Postman collection for the 400 case.
