# Real contact data: SocioPatterns

The SocioPatterns collaboration records face-to-face contacts with wearable sensors, in 20-second intervals. We use the **high school network (Lycée Thiers, Marseille, December 2013)**: about 330 students in 9 classes over 5 days.

## Get and convert the data

1. Download the "High school contact and friendship networks" files from http://www.sociopatterns.org/datasets/ (the contact file is `High-School_data_2013.csv`, the metadata file `metadata_2013.txt`).
2. From the repository root:

   ```bash
   python3 data/sociopatterns/convert.py High-School_data_2013.csv \
       --metadata metadata_2013.txt \
       --out data/sample/sociopatterns-high-school
   ```

3. Restart the backend. The network appears on the load screen as "Real high school (SocioPatterns)". Mark any student as a confirmed case to plan around them.

The script only changes the file format: each pair of students becomes one contact, its duration is the number of 20-second intervals × 20 seconds, and its date is the day of the last interval. Ids become `S001`, `S002` and so on, and each student's class becomes their role. Use `--vaccinated-share 0.2` to mark a seeded 20% as already vaccinated.

## Citation and licence

R. Mastrandrea, J. Fournet, A. Barrat, *Contact patterns in a high school: a comparison between data collected using wearable sensors, contact diaries and friendship surveys*, PLoS ONE 10(9): e0136497 (2015). https://doi.org/10.1371/journal.pone.0136497

Data from SocioPatterns (http://www.sociopatterns.org). Before submitting, copy the exact licence shown on the dataset's download page into this section; SocioPatterns releases its datasets under a Creative Commons licence that requires attribution.
