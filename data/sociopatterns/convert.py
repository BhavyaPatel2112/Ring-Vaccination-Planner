"""
Convert a SocioPatterns contact file into the app's people.csv and contacts.csv.

SocioPatterns files are in "tij" form: each line is a 20-second interval in
which two people were in contact, as  t i j  (time in seconds, two person ids),
sometimes followed by each person's class or group (the high school file has
t i j Ci Cj). An optional metadata file gives  id class gender  per line.

Each pair of people becomes one contact. Its duration is the number of
20-second intervals times 20 seconds; its date is the day of the last interval.
The data records contacts, not infections, so --cases picks hypothetical
confirmed cases with a fixed seed (2 by default).

This script only reformats the data. All the algorithms run in the Java backend.

Usage (from the repository root):
    python3 data/sociopatterns/convert.py High-School_data_2013.csv \
        --metadata metadata_2013.txt \
        --out data/sample/sociopatterns-high-school

The output folder then appears as a sample on the load screen.
"""

import argparse
import csv
import datetime as dt
import os
import random
import sys
from collections import defaultdict

INTERVAL_SECONDS = 20


def read_tij(path):
    """Yields (t, i, j, class_i, class_j) from a whitespace- or comma-separated tij file."""
    with open(path, newline="") as f:
        for line_number, line in enumerate(f, start=1):
            parts = line.replace(",", " ").split()
            if not parts or parts[0].startswith("#"):
                continue
            if len(parts) < 3:
                sys.exit(f"{path} line {line_number}: expected at least 't i j', got {line.strip()!r}")
            try:
                t = int(float(parts[0]))
            except ValueError:
                continue  # a header line
            class_i = parts[3] if len(parts) > 3 else ""
            class_j = parts[4] if len(parts) > 4 else ""
            yield t, parts[1], parts[2], class_i, class_j


def read_metadata(path):
    classes = {}
    if path:
        with open(path) as f:
            for line in f:
                parts = line.split()
                if len(parts) >= 2:
                    classes[parts[0]] = parts[1]
    return classes


def day_of(t, start_date, first_t):
    """Unix timestamps become their UTC date; small relative times count from --start-date."""
    if t > 1_000_000_000:
        return dt.datetime.fromtimestamp(t, dt.timezone.utc).date()
    return start_date + dt.timedelta(seconds=t - first_t)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("tij", help="SocioPatterns contact file (t i j [Ci Cj])")
    parser.add_argument("--metadata", help="optional metadata file: id class gender")
    parser.add_argument("--out", default="data/sample/sociopatterns-high-school", help="output folder")
    parser.add_argument("--start-date", default="2013-12-02", help="date of t = first time, for relative timestamps")
    parser.add_argument("--vaccinated-share", type=float, default=0.0,
                        help="share of people marked as already vaccinated (seeded), default 0")
    parser.add_argument("--cases", type=int, default=2,
                        help="number of hypothetical confirmed cases to pick (seeded), default 2")
    parser.add_argument("--seed", type=int, default=2026)
    args = parser.parse_args()

    classes = read_metadata(args.metadata)
    intervals = defaultdict(int)
    last_day = {}
    people = set()
    rows = list(read_tij(args.tij))
    if not rows:
        sys.exit("No contacts found in " + args.tij)
    first_t = min(r[0] for r in rows)
    start_date = dt.date.fromisoformat(args.start_date)
    for t, i, j, ci, cj in rows:
        if i == j:
            continue
        people.update((i, j))
        if ci:
            classes.setdefault(i, ci)
        if cj:
            classes.setdefault(j, cj)
        pair = (i, j) if i < j else (j, i)
        intervals[pair] += 1
        day = day_of(t, start_date, first_t)
        if pair not in last_day or day > last_day[pair]:
            last_day[pair] = day

    width = len(str(max(len(people), 1)))
    ids = {p: f"S{n:0{width}d}" for n, p in enumerate(sorted(people, key=lambda x: (len(x), x)), start=1)}
    random.seed(args.seed)
    vaccinated = set(random.sample(sorted(people), int(round(args.vaccinated_share * len(people)))))

    # The data has real contacts but no infections, so the cases are hypothetical:
    # seeded picks among people with at least 2 contacts who are not vaccinated,
    # confirmed the day after the last recorded contact.
    degree = defaultdict(int)
    for i, j in intervals:
        degree[i] += 1
        degree[j] += 1
    candidates = sorted((p for p in people if degree[p] >= 2 and p not in vaccinated), key=lambda x: ids[x])
    cases = sorted(random.sample(candidates, min(args.cases, len(candidates))), key=lambda x: ids[x])
    confirmed = max(last_day.values()) + dt.timedelta(days=1)

    os.makedirs(args.out, exist_ok=True)
    with open(os.path.join(args.out, "people.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["person_id", "name", "age", "role", "vaccinated"])
        for p in sorted(people, key=lambda x: ids[x]):
            w.writerow([ids[p], f"Student {p}", "", classes.get(p, ""), str(p in vaccinated).lower()])
    with open(os.path.join(args.out, "contacts.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["person_a", "person_b", "date", "duration_minutes"])
        for (i, j), count in sorted(intervals.items(), key=lambda kv: (ids[kv[0][0]], ids[kv[0][1]])):
            minutes = max(1, round(count * INTERVAL_SECONDS / 60))
            w.writerow([ids[i], ids[j], last_day[(i, j)].isoformat(), minutes])
    with open(os.path.join(args.out, "cases.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["person_id", "confirmed_date"])
        for p in cases:
            w.writerow([ids[p], confirmed.isoformat()])
    print(f"{len(people)} people, {len(intervals)} contacts and {len(cases)} hypothetical cases "
          f"({', '.join(ids[p] for p in cases)}) written to {args.out}")


if __name__ == "__main__":
    main()
