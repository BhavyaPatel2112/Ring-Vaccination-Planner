# Ring Vaccination Planner

A planning tool for outbreak response teams. Given a contact network and the confirmed cases, it works out who should be vaccinated so that every risky contact link around the cases is protected, using as few doses as possible.

Advanced Algorithm, CIA 3 project, ATLAS SkillTech University (2026 to 27).
Team: Bhavya Patel and Tanushree.

> Work in progress. See [PROJECT_PLAN.md](PROJECT_PLAN.md) for the full design and [WORK_SPLIT.md](WORK_SPLIT.md) for who is building what.

## Prerequisites

- Java 17
- Node.js 20 or newer and npm

Maven is not needed; the backend ships with the Maven wrapper.

## Running locally

Backend (Spring Boot, port 8080):

```bash
cd backend
./mvnw spring-boot:run
```

Frontend (React with Vite, port 5173):

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The frontend forwards every `/api` request to the backend.

Run the backend tests:

```bash
cd backend
./mvnw test
```

## Repository layout

```
backend/    Spring Boot API and the algorithm engine (all algorithms are written by us in Java)
frontend/   React app (input, configuration and results only; no algorithm logic)
data/       Sample CSV networks and converted real-world data
docs/       Report, benchmark results and screenshots
```

## Input files

| File | Columns |
|---|---|
| `people.csv` | `person_id,name,age,role,vaccinated` plus optional `eligible,refused` |
| `contacts.csv` | `person_a,person_b,date,duration_minutes` |
| `checkins.csv` | `person_id,venue_id,start_time,end_time` |
| `cases.csv` | `person_id,confirmed_date` |

Sample networks are in [data/sample](data/sample).
