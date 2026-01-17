# 🔍 SonarQube Setup Guide - Book Social Network

## 📋 Overview

This guide explains how to set up SonarQube locally using Docker Compose for code quality analysis of the Book Social Network project.

---

## 🐳 Step 1: Setup SonarQube with Docker Compose

### Add SonarQube to docker-compose.yml

Add the following service to your `docker-compose.yml` file:

```yaml
services:
  # ... existing services (postgres, mail-dev, etc.)

  sonarqube:
    image: sonarqube:community
    container_name: sonarqube
    depends_on:
      - postgres
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://postgres:5432/sonarqube
      SONAR_JDBC_USERNAME: username
      SONAR_JDBC_PASSWORD: password
    ports:
      - "9000:9000"
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions
      - sonarqube_logs:/opt/sonarqube/logs
    networks:
      - book-social-network

volumes:
  # ... existing volumes
  sonarqube_data:
  sonarqube_extensions:
  sonarqube_logs:
```

### Update PostgreSQL database

If you want to use a separate database for SonarQube, update your PostgreSQL init script or use the existing database.

### Start SonarQube

```bash
# Start all services including SonarQube
docker-compose up -d

# Check if SonarQube is running
docker-compose ps

# View SonarQube logs
docker-compose logs -f sonarqube
```

**Note**: SonarQube takes 2-3 minutes to start. Wait until you see "SonarQube is operational" in the logs.

---

## 🌐 Step 2: Access SonarQube Web Interface

1. Open your browser and navigate to: **http://localhost:9000**

2. **Default credentials**:
   - Username: `admin`
   - Password: `admin`

3. You will be prompted to change the password on first login.
   - Choose a strong password (e.g., `admin123`)
   - Confirm the new password

---

## 🔑 Step 3: Generate Authentication Token

### Method 1: Via Web Interface

1. **Login** to SonarQube (http://localhost:9000)

2. Click on your **user avatar** (top right) → **My Account**

3. Go to the **Security** tab

4. In the "Generate Tokens" section:
   - **Name**: `book-social-network` (or any descriptive name)
   - **Type**: Select `Global Analysis Token`
   - **Expires in**: Choose duration (e.g., `90 days` or `No expiration`)

5. Click **Generate**

6. **Copy the token immediately** (it won't be shown again):
   ```
   Example: sqa_ca3cf09fc408a8face41f432924a5963a29999992
   ```

7. **Save the token securely** (you'll need it for Maven commands)

### Method 2: Via API (Alternative)

```bash
curl -u admin:your-password -X POST "http://localhost:9000/api/user_tokens/generate?name=book-social-network"
```

---

## ⚙️ Step 4: Configure Maven for SonarQube

### Option A: Using Command Line (Recommended)

Run Maven with SonarQube parameters:

```bash
mvn clean verify sonar:sonar \
  -Dsonar.token=YOUR_TOKEN_HERE \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.projectKey=book-social-network \
  -Dsonar.projectName='Book Social Network' \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
  -Dsonar.exclusions="**/dto/**,**/config/**,**/exception/**,**/enums/**,**/model/**,**/repository/**,**/controller/**,**/handler/**,**/security/**"
```

### Option B: Using Maven settings.xml

Add to `~/.m2/settings.xml`:

```xml
<settings>
  <profiles>
    <profile>
      <id>sonar</id>
      <properties>
        <sonar.host.url>http://localhost:9000</sonar.host.url>
        <sonar.token>YOUR_TOKEN_HERE</sonar.token>
      </properties>
    </profile>
  </profiles>
</settings>
```

Then run:

```bash
mvn clean verify sonar:sonar -Psonar
```

### Option C: Add to pom.xml (Not Recommended for tokens)

```xml
<properties>
  <sonar.host.url>http://localhost:9000</sonar.host.url>
  <sonar.projectKey>book-social-network</sonar.projectKey>
  <sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
  <!-- DO NOT commit tokens to version control -->
</properties>
```

---

## 🚀 Step 5: Run Analysis

### Full Analysis Command

```bash
cd book-network

# Clean, compile, test, and analyze
mvn clean verify sonar:sonar \
  -Dsonar.token=sqa_ca3cf09fc408a8face41f432924a5963a8adb462 \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.projectKey=book-social-network \
  -Dsonar.projectName='Book Social Network' \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
  -Dsonar.exclusions="**/dto/**,**/config/**,**/exception/**,**/enums/**,**/model/**,**/repository/**,**/controller/**,**/handler/**,**/security/**"
```

### Quick Test + Analysis

```bash
# Run only tests and analysis (skip packaging)
mvn clean test sonar:sonar \
  -Dsonar.token=YOUR_TOKEN_HERE \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.projectKey=book-social-network
```

### Analysis without running tests again

```bash
# If tests already ran (reuse existing coverage)
mvn sonar:sonar \
  -Dsonar.token=YOUR_TOKEN_HERE \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.projectKey=book-social-network
```

---

## 📊 Step 6: View Results

1. **Wait for analysis to complete** (shown in terminal output)

2. **Open SonarQube dashboard**: http://localhost:9000/dashboard?id=book-social-network

3. **Review metrics**:
   - **Bugs**: Potential bugs in code
   - **Vulnerabilities**: Security issues
   - **Code Smells**: Maintainability issues
   - **Coverage**: Test coverage percentage
   - **Duplications**: Duplicated code blocks
   - **Security Hotspots**: Security-sensitive code

4. **Click on issues** to see details and suggested fixes

---

## 🎯 Quality Gate Configuration

### Default Quality Gate

SonarQube comes with a default "Sonar way" quality gate:
- Coverage > 80%
- Duplications < 3%
- Maintainability Rating A
- Reliability Rating A
- Security Rating A

### Custom Quality Gate (Optional)

1. Go to **Quality Gates** in SonarQube
2. Click **Create**
3. Set your conditions:
   ```
   - Coverage on New Code > 80%
   - Bugs on New Code = 0
   - Code Smells on New Code < 5
   - Security Hotspots Reviewed > 100%
   ```
4. Assign it to your project

---

## 🔧 Troubleshooting

### Issue 1: "Not authorized" error

**Solution**: Check your token:
```bash
# Verify token is correct
curl -u YOUR_TOKEN: http://localhost:9000/api/authentication/validate
```

### Issue 2: SonarQube not starting

**Solution**: Check logs and increase memory:
```bash
# View logs
docker-compose logs sonarqube

# Increase memory in docker-compose.yml
environment:
  - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
  - sonar.es.heap.size=512m
```

### Issue 3: Coverage not showing

**Solution**: Ensure JaCoCo is configured correctly:
```bash
# Check if jacoco.xml exists
ls -la book-network/target/site/jacoco/jacoco.xml

# Verify JaCoCo plugin in pom.xml
mvn help:effective-pom | grep jacoco
```

### Issue 4: "Missing blame information" warnings

**Solution**: Commit your files to Git:
```bash
git add .
git commit -m "Add tests and SonarQube configuration"
```

### Issue 5: Port 9000 already in use

**Solution**: Change the port in docker-compose.yml:
```yaml
ports:
  - "9001:9000"  # Use port 9001 instead
```

Then use: `http://localhost:9001`

---

## 📝 Best Practices

### 1. Exclude Non-Business Code

Always exclude framework code from analysis:
```
-Dsonar.exclusions="**/dto/**,**/config/**,**/exception/**,**/enums/**,**/model/**,**/repository/**,**/controller/**,**/handler/**,**/security/**,**/Application.java"
```

### 2. Use .gitignore for tokens

**Never commit tokens to Git!** Add to `.gitignore`:
```
# SonarQube
.sonar/
sonar-project.properties
```

### 3. Run analysis regularly

```bash
# Before each commit
mvn clean test sonar:sonar

# Or integrate into CI/CD pipeline
```

### 4. Fix issues incrementally

Focus on:
1. **Critical/High severity** bugs first
2. **Security vulnerabilities** next
3. **Code smells** for maintainability
4. Improve **coverage** gradually

### 5. Monitor trends

- Use SonarQube to track **technical debt** over time
- Set up **notifications** for quality gate failures
- Review **new issues** introduced in each commit

---

## 📚 Resources

- **SonarQube Documentation**: https://docs.sonarqube.org/latest/
- **Maven Scanner**: https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-maven/
- **JaCoCo Plugin**: https://www.jacoco.org/jacoco/trunk/doc/maven.html
- **Quality Gates**: https://docs.sonarqube.org/latest/user-guide/quality-gates/

---

## ✅ Quick Reference

### Essential Commands

```bash
# Start SonarQube
docker-compose up -d sonarqube

# Stop SonarQube
docker-compose stop sonarqube

# Remove SonarQube (keep data)
docker-compose rm sonarqube

# Remove SonarQube (delete data)
docker-compose down -v sonarqube

# Full analysis
mvn clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN

# Quick check
mvn test sonar:sonar -Dsonar.token=YOUR_TOKEN

# View logs
docker-compose logs -f sonarqube
```

### URLs

- **SonarQube UI**: http://localhost:9000
- **Project Dashboard**: http://localhost:9000/dashboard?id=book-social-network
- **API Documentation**: http://localhost:9000/web_api

---

**Author**: SonarQube Setup Guide  
**Date**: January 2026  
**Version**: 1.0.0
