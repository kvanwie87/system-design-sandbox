package exercises.tier1;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Exercise 1: HashMap Internals — Custom Key
 *
 * Demonstrates correct hashCode()/equals() contract and what breaks when violated.
 */
public class Exercise01_HashMap {

    public static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    /**
     * Demonstrates correct usage: lookup by equal key works.
     */
    public static String correctUsage() {
        Map<Person, String> map = new HashMap<>();
        Person alice = new Person("Alice", 30);
        map.put(alice, "Engineer");
        return map.get(new Person("Alice", 30));
    }

    /**
     * Demonstrates the mutable key problem: mutating a key after insertion orphans the entry.
     */
    public static String mutableKeyProblem() {
        Map<Person, String> map = new HashMap<>();
        Person bob = new Person("Bob", 25);
        map.put(bob, "Designer");
        bob.setName("Robert"); // Mutate the key!
        return map.get(bob); // Returns null — hash changed, wrong bucket
    }

    public static void main(String[] args) {
        System.out.println("Correct usage: " + correctUsage());
        System.out.println("Mutable key (should be null): " + mutableKeyProblem());
    }
}
