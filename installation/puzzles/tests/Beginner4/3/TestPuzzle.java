import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test() {
        Puzzle b = new Puzzle();
        assertEquals(8, b.run(1));
    }

}