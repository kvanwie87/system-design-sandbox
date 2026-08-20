package exercises.tier1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Exercise01_HashMapTest {

    @Test
    void correctUsage_shouldFindValueByEqualKey() {
        String result = Exercise01_HashMap.correctUsage();
        assertEquals("Engineer", result);
    }

    @Test
    void mutableKeyProblem_shouldReturnNull() {
        String result = Exercise01_HashMap.mutableKeyProblem();
        assertNull(result, "Mutating a key after insertion should orphan the entry");
    }

    @Test
    void person_equalsByContent() {
        var p1 = new Exercise01_HashMap.Person("Alice", 30);
        var p2 = new Exercise01_HashMap.Person("Alice", 30);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void person_notEqualsDifferentContent() {
        var p1 = new Exercise01_HashMap.Person("Alice", 30);
        var p2 = new Exercise01_HashMap.Person("Bob", 25);
        assertNotEquals(p1, p2);
    }
}
