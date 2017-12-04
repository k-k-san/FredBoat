package fredboat.util;

import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

class TextUtilsTest {

    @TestFactory
    Stream<DynamicTest> simpleSplitSelect() {
        List<Integer> one_two_three = Arrays.asList(1, 2, 3);
        String[] testCases = {
                "1,2,3",
                "1 2 3",
                "1, 2, 3",
        };

        return DynamicTest.stream(Arrays.asList(testCases).iterator(),
                testCase -> String.format("split select of `%s`", testCase),
                testCase -> Assertions.assertIterableEquals(
                        one_two_three, TextUtils.getSplitSelect(testCase)
                )
        );
    }

    @Test
    void blanksInSplitSelect() {
        Assertions.assertIterableEquals(
                Arrays.asList(1, 2, 3, 4),
                TextUtils.getSplitSelect("1, ,2 ,, 3, 4")
        );
    }
}
