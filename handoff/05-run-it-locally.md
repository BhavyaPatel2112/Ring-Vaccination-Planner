# 5. Run it locally

## Install once

| Tool | Version | Check |
|---|---|---|
| Java (JDK) | 17 | `java -version` |
| Node.js | 20 or newer (built with 26) | `node -v` |
| npm | comes with Node | `npm -v` |
| Git | any | `git --version` |

Maven is not needed; the backend includes the Maven wrapper (`./mvnw`, or `mvnw.cmd` on Windows).

```bash
git clone https://github.com/BhavyaPatel2112/Ring-Vaccination-Planner.git
cd Ring-Vaccination-Planner/frontend
npm install
```

## Run the app (two terminals)

Backend: Spring Boot on **port 8090**. Start it from the `backend` folder, so it finds `../data/sample`.

```bash
cd backend
./mvnw spring-boot:run
```

Frontend: Vite on port 5173. It forwards every `/api` call to port 8090.

```bash
cd frontend
npm run dev
```

Open **http://localhost:5173**, choose Load data, then "Neighbourhood outbreak". Mark cases, generate a plan.

## Test, lint, build

```bash
cd backend && ./mvnw test          # 141 tests, all should pass
cd frontend && npx oxlint          # should print nothing
cd frontend && npm run build       # should end with "built in ..."
```

Run one test class: `./mvnw test -Dtest=ExactVertexCoverTest`. With `-q` the summary is hidden; the results are in `backend/target/surefire-reports/`.

## Run the benchmarks

```bash
cd backend
./mvnw -q compile
java -Xmx3g -cp target/classes com.ringvax.bench.BenchmarkRunner all ../docs/benchmarks
```

Use `quality`, `exact-growth` or `selection` instead of `all` to run one experiment. They take about a minute in total. Close other heavy apps first for steadier timings.

## Try the API without the frontend

```bash
curl -s -X POST localhost:8090/api/networks/samples/outbreak-small
# copy the networkId from the answer, then:
curl -s -X POST localhost:8090/api/networks/NET_ID/plan -H 'Content-Type: application/json' -d '{"caseIds":["P003","P023"]}'
```

## Common problems

| Symptom | Cause and fix |
|---|---|
| Red banner "The backend is not running" | Start the backend; check `curl localhost:8090/api/health` |
| Backend fails with "Port 8090 was already in use" | Another copy is running. Stop it (`lsof -i :8090`, then `kill <pid>`), or change `server.port` in `backend/src/main/resources/application.properties` **and** the proxy in `frontend/vite.config.js` |
| "No samples found" on the load screen | The backend was started from a different folder. Start it from `backend/`, or set `ringvax.sample-dir` to the full path of `data/sample` |
| The network you loaded is gone after a restart | Networks are kept in memory only (by design for now). Load the sample again. The frontend notices and goes back to Load data |
| Frontend shows a blank page after editing code | Hard-refresh the browser. Hot reload can get confused when a file with a React context changes |
| Network map looks frozen or grey in a background tab | Browsers pause animation frames in hidden tabs; it catches up when the tab is visible |
| Tests fail only on the sample data | They read `../data/sample`, so run them from `backend/` (the Maven wrapper does this) |
