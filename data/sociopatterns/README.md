# Real contact data: SocioPatterns

The SocioPatterns collaboration records face-to-face contacts with wearable sensors, in 20-second intervals. We use the **high school network (Lycée Thiers, Marseille, 2 to 6 December 2013)**: 327 students in 9 classes, 5,818 pairs who met. The converted files are already in `data/sample/sociopatterns-high-school`, so the network is on the load screen as "Real high school (SocioPatterns)"; the steps below are only needed to rebuild them.

## Get and convert the data

1. Download "Dynamical contact list" and "Metadata" from http://www.sociopatterns.org/datasets/high-school-contact-and-friendship-networks/ and unzip the contact list (`HighSchool2013_proximity_net.csv.gz`).
2. From the repository root:

   ```bash
   python3 data/sociopatterns/convert.py HighSchool2013_proximity_net.csv \
       --metadata HighSchool2013_metadata.txt \
       --out data/sample/sociopatterns-high-school
   ```

3. Restart the backend. The network appears on the load screen as "Real high school (SocioPatterns)".

The script only changes the file format: each pair of students becomes one contact, its duration is the number of 20-second intervals × 20 seconds, and its date is the day of the last interval. Ids become `S001`, `S002` and so on, and each student's class becomes their role. Use `--vaccinated-share 0.2` to mark a seeded 20% as already vaccinated.

**The cases are hypothetical.** The data records who met whom, not who was infected, so `--cases 2` (the default) picks two students with a fixed seed (S061 and S164) and marks them confirmed the day after the last recorded contact. You can mark any other student as a case in the app. Using real measured contacts with simulated outbreaks is the standard way this dataset is used in epidemic research.

**Contact definition.** Sensors record every encounter, even 20 seconds in a corridor, so the app opens this network with the usual close-contact rule: 15 minutes or more. The simulation screen also turns on "risk grows with time spent together" for it.

## Citation and licence

R. Mastrandrea, J. Fournet, A. Barrat, *Contact patterns in a high school: a comparison between data collected using wearable sensors, contact diaries and friendship surveys*, PLoS ONE 10(9): e0136497 (2015). https://doi.org/10.1371/journal.pone.0136497

Data from the SocioPatterns collaboration (http://www.sociopatterns.org), distributed to the public under a Creative Commons Attribution-NonCommercial-ShareAlike license (as stated on the dataset's download page). The converted files in `data/sample/sociopatterns-high-school` are shared under the same licence; only the format and the student ids were changed, and two hypothetical cases were added.
