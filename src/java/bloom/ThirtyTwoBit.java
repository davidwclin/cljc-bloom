package bloom;

public class ThirtyTwoBit {
    /**
     * js bitwise ops are [32 bits](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Bitwise_Operators).
     * Use this in clj to be consistent with behavior of `bit-shift-left` in cljs.
     */

    public static int bitShiftLeft(long a, int b) {
        return ((int) a) << b;
    }

    public static int bitShiftRight(long a, int b) {
        return ((int) a) >> b;
    }
}