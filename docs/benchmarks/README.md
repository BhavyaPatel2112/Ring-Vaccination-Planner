# Benchmarks

Raw results from `BenchmarkRunner` (backend package `com.ringvax.bench`). Every input is generated from a fixed seed (2026), so rerunning gives the same inputs; times will differ slightly by machine.

## How to reproduce

```bash
cd backend
./mvnw -q compile
java -Xmx3g -cp target/classes com.ringvax.bench.BenchmarkRunner all ../docs/benchmarks
```

Replace `all` with `quality`, `exact-growth` or `selection` to run one experiment.

**Method.** Each timing is the median of 5 or more measured runs, after 3 warm-up runs so the JIT compiler has optimised the code first. The median is taken with our own randomized Quickselect. Input preparation (for example copying the array before an in-place selection) is not timed. Machine details are in [environment.txt](environment.txt): Java 17.0.18, Apple Silicon, 8 cores.

| File | What it holds |
|---|---|
| [approx-vs-exact.csv](approx-vs-exact.csv) | One row per network: approximation size, exact minimum, lower bound, ratio |
| [approx-vs-exact-summary.csv](approx-vs-exact-summary.csv) | Mean and worst ratio per size and density |
| [exact-growth.csv](exact-growth.csv) | One row per network: exact solver time and search nodes, approximation time, brute force time |
| [exact-growth-summary.csv](exact-growth-summary.csv) | Medians per size |
| [selection.csv](selection.csv) | Quickselect, median-of-medians and full sort, per input type and size |

## 1. Approximation quality against the exact minimum

30 seeded random networks per size, at two densities. Both methods solve the full vertex cover of each network.

| People | Mean ratio (avg 3 contacts) | Worst ratio (avg 3) | Mean ratio (avg 6 contacts) | Worst ratio (avg 6) |
|---|---|---|---|---|
| 10 | 1.61 | 2.00 | 1.36 | 1.67 |
| 15 | 1.63 | 2.00 | 1.36 | 1.56 |
| 20 | 1.64 | 2.00 | 1.39 | 1.64 |
| 25 | 1.62 | 1.83 | 1.44 | 1.60 |
| 30 | 1.65 | 1.87 | 1.47 | 1.67 |
| 35 | 1.61 | 1.79 | 1.44 | 1.68 |
| 40 | 1.63 | 1.88 | 1.44 | 1.58 |

**Reading.**
- The guarantee holds on all 420 networks: the ratio never goes above 2.
- It reaches exactly 2 on some small sparse networks. That is the tight case from the proof (a matching where one endpoint per link would have been enough).
- On denser networks the approximation is closer to the minimum, at about 1.4×, because the matched pairs overlap more with the optimal cover.
- This is why the app shows "at most 2 times the minimum", plus the proven ratio for each plan, and never claims the plan is optimal.

## 2. Exact solver growth on hard networks

Random 4-regular networks: everyone has exactly 4 contacts. No one has a single contact, so the solver's "take the neighbour" shortcut never applies, which makes these harder than real contact networks. There are 5 networks per size; the figures are medians.

| People | Exact solver (ms) | Search tree nodes | Approximation (ms) | Brute force, all subsets (ms) |
|---|---|---|---|---|
| 16 | 0.002 | 15 | 0.0016 | 0.24 |
| 20 | 0.003 | 19 | 0.0024 | 2.35 |
| 24 | 0.004 | 33 | 0.0021 | 38.5 |
| 28 | 0.006 | 51 | 0.0026 | – |
| 32 | 0.010 | 85 | 0.0028 | – |
| 36 | 0.016 | 123 | 0.0030 | – |
| 40 | 0.023 | 145 | 0.0034 | – |
| 44 | 0.046 | 257 | 0.0036 | – |
| 48 | 0.059 | 323 | 0.0040 | – |
| 52 | 0.112 | 485 | 0.0044 | – |
| 56 | 0.222 | 827 | 0.0044 | – |
| 60 | 0.242 | 799 | 0.0048 | – |
| 64 | 0.415 | 1,309 | 0.0051 | – |

**Reading.**
- **Brute force** multiplies by about 16 every 4 people (0.24 → 2.35 → 38.5 ms). That is 2^n: 1.93× per extra person. At 40 people it would take hours, so it is only run up to 24.
- **Branch and bound** is still exponential, but much slower-growing. Its search tree grows about 1.10× per extra person, so it doubles roughly every 7 people (15 → 1,309 nodes from 16 to 64).
- **Where the savings come from:** branching on the most connected person, pruning with the matching lower bound, the degree-1 rule, and solving separate groups independently.
- **Why it is still exponential:** vertex cover is NP-complete, so no method is known to avoid exponential growth in the worst case. That is why the API refuses more than 40 undecided people and has a 5 second limit.
- **Approximation:** it grows linearly and stays at a few microseconds throughout.

## 3. Selection: Quickselect against median-of-medians

Finding the median of n integers. Each method gets a fresh copy of the same array.

| Input | Values | Randomized Quickselect (ms) | Median-of-medians (ms) | Full sort (ms) |
|---|---|---|---|---|
| random | 10,000 | 0.27 | 0.57 | 0.68 |
| random | 100,000 | 1.17 | 3.47 | 4.55 |
| random | 1,000,000 | 8.68 | 21.87 | 51.27 |
| random | 10,000,000 | 88.66 | 217.63 | 693.95 |
| sorted | 10,000 | 0.08 | 0.31 | 0.12 |
| sorted | 100,000 | 0.68 | 3.68 | 0.33 |
| sorted | 1,000,000 | 4.53 | 12.27 | 0.23 |
| sorted | 10,000,000 | 29.43 | 113.49 | 2.04 |

**Reading.**
- **Both selection methods grow linearly.** Ten times more values takes about ten times longer: Quickselect goes 8.7 → 88.7 ms and median-of-medians 21.9 → 217.6 ms. The full sort grows faster, at about 13.5× per 10× more values, as n log n predicts.
- **Quickselect is about 2.5× faster than median-of-medians.** Median-of-medians has the stronger guarantee (worst case O(n)), but pays for it with a large constant: sorting groups of 5 and a second recursive selection.
- **Sorted input does not slow Quickselect down.** A fixed first-element pivot would go quadratic here, but random pivots keep it linear.
- **Limitation:** on already-sorted input, Java's built-in sort wins, because it detects sorted runs and finishes in about one pass. Real contact counts are not sorted, so the random-input rows are the relevant ones. At 1,000,000 values (the budget mode and quality warning case), Quickselect takes under 9 ms.
