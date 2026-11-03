package com.ringvax.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;

/**
 * Builds realistic synthetic contact networks for demos and benchmarks.
 *
 * <p>The network is built in three layers, the way real contact surveys look:
 * <ol>
 *   <li><b>Households</b> of 2 to 6 people, everyone in contact with everyone,
 *       with long contacts (3 to 12 hours).</li>
 *   <li><b>Schools and workplaces</b> of 20 to 200 people. Each member meets a
 *       random subset of the group (15 minutes to 8 hours). This layer gets
 *       about 70% of the links left after households.</li>
 *   <li><b>Community</b> links between random people anywhere in the
 *       population (5 to 90 minutes), which connect the groups together.</li>
 * </ol>
 *
 * <p>Ages, roles, eligibility (infants under 1 and about 1% of others cannot
 * take a live vaccine), refusals and existing vaccination are assigned too.
 * Every random choice comes from one {@link Random} seeded by the caller, so the
 * same configuration always produces exactly the same network.
 *
 * <p>The average number of contacts per person is matched closely when it is
 * above the household baseline (about 3); below that, households alone already
 * give more contacts and are kept intact.
 *
 * <p>Time complexity: O(V + E) expected. Space complexity: O(V + E), including a
 * hash set of the links already created, which is used to skip duplicates.
 */
public final class SyntheticNetworkGenerator {

    public static final int MAX_POPULATION = 2_000_000;
    public static final double MAX_AVG_CONTACTS = 200;
    public static final LocalDate DEFAULT_REFERENCE_DATE = LocalDate.of(2026, 11, 1);

    private static final double GROUP_SHARE = 0.7;
    private static final int CONTACT_WINDOW_DAYS = 14;

    private static final String[] FIRST_NAMES = {
        "Aarav", "Aditi", "Akash", "Ananya", "Arjun", "Diya", "Farhan", "Gauri", "Harsh", "Isha",
        "Ishaan", "Kabir", "Kavya", "Krish", "Lakshmi", "Manav", "Meera", "Mohit", "Neha", "Nikhil",
        "Pooja", "Pranav", "Priya", "Rahul", "Riya", "Rohan", "Saanvi", "Sahil", "Sana", "Siddharth",
        "Sneha", "Tanvi", "Uday", "Vansh", "Varun", "Vikram", "Yash", "Zara", "Ayesha", "Imran",
    };
    private static final String[] SURNAMES = {
        "Sharma", "Patel", "Iyer", "Khan", "Nair", "Rao", "Gupta", "Joshi", "Das", "Reddy",
        "Mehta", "Kulkarni", "Desai", "Menon", "Sheikh", "Pillai", "Bose", "Mishra", "Kapoor", "Singh",
    };
    private static final String[] WORK_ROLES = {
        "office worker", "teacher", "nurse", "shopkeeper", "driver", "engineer", "cook",
        "factory worker", "cleaner", "security guard", "doctor", "pharmacist", "delivery rider",
    };

    /**
     * Generator settings.
     *
     * @param population         number of people, 2 to {@link #MAX_POPULATION}
     * @param avgContacts        target average contacts per person, above 0 and at most
     *                           {@link #MAX_AVG_CONTACTS} (and below the population)
     * @param seed               random seed; the same seed gives the same network
     * @param vaccinatedFraction share of eligible people already vaccinated, 0 to 1
     * @param refusedFraction    share of people who refuse vaccination, 0 to 1
     * @param referenceDate      contacts are dated within the 14 days up to this date
     */
    public record Config(int population, double avgContacts, long seed,
                         double vaccinatedFraction, double refusedFraction, LocalDate referenceDate) {

        public Config {
            if (population < 2 || population > MAX_POPULATION) {
                throw new IllegalArgumentException(
                        "population must be between 2 and " + MAX_POPULATION + ", got " + population);
            }
            if (!(avgContacts > 0) || avgContacts > MAX_AVG_CONTACTS || avgContacts >= population) {
                throw new IllegalArgumentException("avgContacts must be above 0, at most " + (int) MAX_AVG_CONTACTS
                        + " and below the population, got " + avgContacts);
            }
            if (!(vaccinatedFraction >= 0 && vaccinatedFraction <= 1)
                    || !(refusedFraction >= 0 && refusedFraction <= 1)) {
                throw new IllegalArgumentException("fractions must be between 0 and 1");
            }
            if (referenceDate == null) {
                throw new IllegalArgumentException("referenceDate is required");
            }
        }

        /** Default settings: 10% already vaccinated, 3% refusals. */
        public static Config of(int population, double avgContacts, long seed) {
            return new Config(population, avgContacts, seed, 0.10, 0.03, DEFAULT_REFERENCE_DATE);
        }
    }

    private final Config config;
    private final Random random;
    private final ContactGraph graph = new ContactGraph();
    private final Set<Long> existing = new HashSet<>();
    private final LocalDate[] days = new LocalDate[CONTACT_WINDOW_DAYS];

    private SyntheticNetworkGenerator(Config config) {
        this.config = config;
        this.random = new Random(config.seed());
        for (int i = 0; i < CONTACT_WINDOW_DAYS; i++) {
            days[i] = config.referenceDate().minusDays(i);
        }
    }

    /** Generates a network with the default settings. */
    public static ContactGraph generate(int population, double avgContacts, long seed) {
        return generate(Config.of(population, avgContacts, seed));
    }

    public static ContactGraph generate(Config config) {
        return new SyntheticNetworkGenerator(config).build();
    }

    private ContactGraph build() {
        long target = Math.round(config.population() * config.avgContacts() / 2);
        target = Math.min(target, (long) config.population() * (config.population() - 1) / 2);

        List<int[]> households = createPeopleInHouseholds();
        for (int[] household : households) {
            for (int i = 0; i < household.length; i++) {
                for (int j = i + 1; j < household.length; j++) {
                    link(household[i], household[j], 180, 720);
                }
            }
        }

        long remaining = target - graph.contactCount();
        if (remaining > 0) {
            addGroupContacts(Math.round(remaining * GROUP_SHARE));
            addCommunityContacts(target - graph.contactCount());
        }
        return graph;
    }

    /** Creates every person, household by household, and returns the households. */
    private List<int[]> createPeopleInHouseholds() {
        int width = Math.max(4, String.valueOf(config.population()).length());
        String idFormat = "P%0" + width + "d";
        List<int[]> households = new ArrayList<>();
        int created = 0;
        while (created < config.population()) {
            int left = config.population() - created;
            int size = 2 + random.nextInt(5);
            if (size > left || left - size == 1) {
                size = left <= 6 ? left : size - 1;
            }
            String surname = SURNAMES[random.nextInt(SURNAMES.length)];
            int[] members = new int[size];
            for (int m = 0; m < size; m++) {
                int age = householdMemberAge(m);
                String id = String.format(idFormat, created + 1);
                members[m] = graph.addPerson(newPerson(id, surname, age));
                created++;
            }
            households.add(members);
        }
        return households;
    }

    /** The first two members are usually adults; the rest are children or grandparents. */
    private int householdMemberAge(int member) {
        if (member < 2) {
            return random.nextDouble() < 0.15 ? 65 + random.nextInt(26) : 23 + random.nextInt(42);
        }
        return random.nextDouble() < 0.7 ? random.nextInt(18) : 65 + random.nextInt(26);
    }

    private Person newPerson(String id, String surname, int age) {
        String name = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + surname;
        String role;
        if (age < 1) {
            role = "infant";
        } else if (age < 5) {
            role = "child";
        } else if (age < 18 || (age <= 22 && random.nextBoolean())) {
            role = "student";
        } else if (age < 65) {
            role = WORK_ROLES[random.nextInt(WORK_ROLES.length)];
        } else {
            role = "retired";
        }
        boolean eligible = age >= 1 && random.nextDouble() >= 0.01;
        boolean refused = random.nextDouble() < config.refusedFraction();
        boolean vaccinated = eligible && !refused && random.nextDouble() < config.vaccinatedFraction();
        return new Person(id, name, age, role, vaccinated, eligible, refused);
    }

    /** Students go to schools, workers to workplaces; links are spread by group size. */
    private void addGroupContacts(long budget) {
        List<Integer> students = new ArrayList<>();
        List<Integer> workers = new ArrayList<>();
        for (int p = 0; p < graph.personCount(); p++) {
            String role = graph.person(p).role();
            if (role.equals("student")) {
                students.add(p);
            } else if (!role.equals("infant") && !role.equals("child") && !role.equals("retired")) {
                workers.add(p);
            }
        }
        List<int[]> groups = new ArrayList<>();
        splitIntoGroups(students, groups);
        splitIntoGroups(workers, groups);

        long members = students.size() + workers.size();
        if (members == 0) {
            return;
        }
        for (int[] group : groups) {
            long pairs = (long) group.length * (group.length - 1) / 2;
            long wanted = Math.min(pairs / 2, Math.round((double) budget * group.length / members));
            long attempts = 3 * wanted + 10;
            long added = 0;
            while (added < wanted && attempts-- > 0) {
                int a = group[random.nextInt(group.length)];
                int b = group[random.nextInt(group.length)];
                if (link(a, b, 15, 480)) {
                    added++;
                }
            }
        }
    }

    private void splitIntoGroups(List<Integer> pool, List<int[]> groups) {
        Collections.shuffle(pool, random);
        int start = 0;
        while (start < pool.size()) {
            int size = Math.min(20 + random.nextInt(181), pool.size() - start);
            int[] group = new int[size];
            for (int i = 0; i < size; i++) {
                group[i] = pool.get(start + i);
            }
            if (size >= 2) {
                groups.add(group);
            }
            start += size;
        }
    }

    private void addCommunityContacts(long wanted) {
        int n = graph.personCount();
        long attempts = 3 * wanted + 10;
        long added = 0;
        while (added < wanted && attempts-- > 0) {
            if (link(random.nextInt(n), random.nextInt(n), 5, 90)) {
                added++;
            }
        }
    }

    /** Adds a link unless it is a self-contact or already exists. */
    private boolean link(int a, int b, int minMinutes, int maxMinutes) {
        if (a == b) {
            return false;
        }
        long key = ((long) Math.min(a, b) << 32) | Math.max(a, b);
        if (!existing.add(key)) {
            return false;
        }
        LocalDate date = days[random.nextInt(CONTACT_WINDOW_DAYS)];
        int minutes = minMinutes + random.nextInt(maxMinutes - minMinutes + 1);
        graph.addContact(a, b, date, minutes);
        return true;
    }
}
