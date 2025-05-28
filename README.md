# iam-demo

This project demonstrates **Identity and Access Management (IAM)** using **Keycloak**, **Spring Boot**, and **OAuth2/OIDC** for authentication and authorization. It provides a ready-to-run demo with hierarchical roles (admin → user → guest) and API access control.

## Design Philosophy

1. **Role-Based Access Control (RBAC)**
   Three-tier role hierarchy:
   - **Admin**: Full access (associated user + guest roles)
   - **User**: Limited access (associated guest role)
   - **Guest**: Base access only

2. **OIDC Integration**
   Uses OpenID Connect for standardized identity claims (`preferred_username`, `email`, etc.) alongside custom `resource_access` roles.

3. **Dockerized Keycloak**
   Pre-configured realm with client and roles for instant setup via `docker-compose.yml`.

---

## Installation & Running

### Prerequisites
- Docker and Docker Compose
- Java 17+
- Maven

### Setup Steps
1. **Clone the repository**
   ```bash
   git clone https://github.com/linkenmin/iam-demo.git
   cd iam-demo
   ```

2. **Start Keycloak and PostgreSQL**
   Launch the services:
   ```bash
   docker-compose up -d
   ```

3. **Import Realm Configuration**
   Execute the script to import the pre-configured realm (includes client settings):
   ```bash
   sh import-realm.sh
   ```

4. **Import User Data**
   Create users and assign roles from `user.csv`:
   ```bash
   sh import-user.sh
   ```
   - The script will output the `client-secret` (copy this for the next step)

5. **Configure Application**
   Paste the obtained `client-secret` into `application.yml`:
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             keycloak:
               client-secret: "PASTE_YOUR_CLIENT_SECRET_HERE"  # Replace this
   keycloak:
     credentials:
       secret: "PASTE_YOUR_CLIENT_SECRET_HERE"  # Replace this
   ```

6. **Build and Run**
   - Compile and package:
     ```bash
     mvn clean install
     ```
   - Run the application:
     ```bash
     mvn spring-boot:run
     ```

7. **Access URLs**
   - App: http://localhost:8081
   - Keycloak Admin: http://localhost:8080/admin (user: `admin`, pass: `admin`)

8. **Cleanup**
   To stop and remove all Docker containers and volumes:
   ```bash
   docker-compose down -v
   ```

---

## Key Features

- **Role Testing**
  Test endpoints with automatic role validation:
  ```
  /api/guest
  /api/user
  /api/admin
  ```

- **OIDC Claims**
  Displays standard and custom claims from Keycloak.

- **Pre-configured Client**
  Includes Keycloak client with role mappings.

---

## Testing with pytest

To run tests, first set up your environment:

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Install Playwright browsers:
```bash
playwright install
```

3. Run tests:
```bash
pytest -n auto
```

### Parallel Execution
The test suite supports parallel execution using `pytest-xdist`. Use the `-n auto` flag to distribute tests across all available CPU cores for faster execution.

### Multi-Browser Testing
Playwright tests are configured to run on three browsers by default:
- **Chromium**
- **Firefox**
- **WebKit**

To specify browsers, use the `--browsers` flag:
```bash
pytest --browsers="chromium,firefox"
```

Key features of the test suite:
- Tests role-based API permissions
- Verifies successful login for each role
- Runs in parallel for efficiency
- Supports headless mode for CI/CD pipelines

Configuration options:
- `--browsers`: Comma-separated list of browsers to test (default: all)
- `--headless`: Run browsers in headless mode (default: false)

---

## Project Structure

```
├── src/main/java/demo/keycloak/
│   ├── config/              # Security configuration (OAuth2/OIDC)
│   │   └── SecurityConfig.java
│   ├── controller/          # API endpoints and Thymeleaf controllers
│   │   ├── HomeController.java
│   │   └── ApiTestController.java
│   └── IamDemoApplication.java
├── src/main/resources/
│   ├── templates/           # Thymeleaf HTML with OIDC security tags
│   │   └── home.html
│   └── application.yml      # Spring Boot config
├── tests/                   # End-to-end test suite
│   ├── e2e/                 # Browser automation tests
│   │   └── test_roles.py
│   ├── utils/               # Browser management
│   │   └── browser_utils.py
│   └── conftest.py          # Pytest fixtures
├── docker-compose.yml       # Keycloak with realm import
└── realm-export.json        # Pre-configured realm
```
