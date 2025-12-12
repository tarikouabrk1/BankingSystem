package security.hashing;

public class ObjectBitSet {
    public static PrimitiveIntList getBitSequence(Object hashable_object) {
        String hashable_string = hashable_object.toString();
        PrimitiveIntList bit_seq = new PrimitiveIntList(new int[]{hashable_string.length()});

        for (int i = 0; i < (hashable_string.length() / 4) + 1; i++) {
            int bit_seq32 = 0;
            int char_int;
            for (int j = 0; (j < 4 && j + i * 4 < hashable_string.length()); j++) {
                char_int = hashable_string.charAt(i * 4 + j);
                bit_seq32 = bit_seq32 << 8;
                bit_seq32 += char_int;
            }
            bit_seq.add(bit_seq32);
        }

        return bit_seq;
    }
}
