# Handoff: Ring Vaccination Planner

Written by Bhavya on Wednesday 4 November 2026 for Tanushree, who takes over from Monday 9 November until the final submission and demo on Friday 20 November.

Everything needed to continue the project is in this folder. If you only read one file, read [04-what-tanushree-does-next.md](04-what-tanushree-does-next.md).

## Read in this order

| # | File | What it answers |
|---|---|---|
| 1 | [01-the-idea.md](01-the-idea.md) | What problem we solve, for whom, and the key algorithm insight |
| 2 | [02-full-scope.md](02-full-scope.md) | Everything the finished project must have: features, deliverables, rubric, deadlines |
| 3 | [03-what-is-done.md](03-what-is-done.md) | What Bhavya built, with files, tests and results |
| 4 | [04-what-tanushree-does-next.md](04-what-tanushree-does-next.md) | Your tasks, day by day, with what "done" means for each |
| 5 | [05-run-it-locally.md](05-run-it-locally.md) | Install, run, test and benchmark on your laptop, plus fixes for common problems |
| 6 | [06-codebase-guide.md](06-codebase-guide.md) | How the code is organised, every class, the API, the frontend structure and design system |
| 7 | [07-where-your-code-plugs-in.md](07-where-your-code-plugs-in.md) | Exact steps to add each of your algorithms, endpoints and screens |
| 8 | [08-rules-and-gotchas.md](08-rules-and-gotchas.md) | Non-negotiable rules, code conventions and the traps already found |

If you use a coding tool to help you, point it at this folder first, especially files 6, 7 and 8. Together they hold the full context: the rules from the assessment, the conventions the code follows, and the problems already solved.

## Status in one paragraph

All of Bhavya's code is done, tested and pushed:
- **Backend:** the data layer (CSV parsing with full validation, sample networks, synthetic generator), the ring builder, the vertex cover 2-approximation and exact solver, randomized Quickselect and median-of-medians, the vaccination planner with budget mode, data quality warnings, the REST API, the benchmark harness and three experiments. 141 backend tests pass.
- **Frontend:** the design system, app shell, load screen and network console. You can load a sample, mark cases, see the rings and generate a plan in the browser today.

What is left is Tanushree's half:
- **Algorithms:** disjoint-set union, the indexed heap and greedy method, the Bloom filter, the outbreak simulator, and field features B, C and E.
- **The rest of the app:** their endpoints, the remaining screens, her benchmarks, real-world data, screenshots, the final README, and demo preparation.

The report and the proposal are on hold for now.

## Key links

- Repository: https://github.com/BhavyaPatel2112/Ring-Vaccination-Planner
- Original design and requirements: [PROJECT_PLAN.md](../PROJECT_PLAN.md) (the assessment guidelines PDF is kept off GitHub; ask Bhavya for a copy)
- Who does what: [WORK_SPLIT.md](../WORK_SPLIT.md)
- Benchmark results: [docs/benchmarks/README.md](../docs/benchmarks/README.md)
- Sample data explained: [data/sample/README.md](../data/sample/README.md)
