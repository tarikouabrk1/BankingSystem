package security.hashing;

public final class PrimitiveIntList {
    private final int INCREASE_RATE = 16;
    private int capacity;
    private int curr_num_elements;
    private int[] list;

    public PrimitiveIntList() {
        this.capacity = this.INCREASE_RATE;
        this.list = new int[capacity];
        this.curr_num_elements = 0;
    }

    public PrimitiveIntList(int capacity) {
        this.capacity = capacity;
        this.list = new int[capacity];
        this.curr_num_elements = 0;
    }

    public PrimitiveIntList(int[] elements) {
        this.capacity = (elements.length / this.INCREASE_RATE);
        if (elements.length % this.INCREASE_RATE != 0) {
            this.capacity += this.INCREASE_RATE;
        }
        this.list = new int[this.capacity];
        this.addAll(elements);
        this.curr_num_elements = elements.length;
    }

    public boolean isEmpty() {
        return this.curr_num_elements == 0;
    }

    public int size() {
        return this.curr_num_elements;
    }

    public int getCapacity() {
        return this.capacity;
    }

    private boolean increase_capacity() {
        try {
            int newCapacity = this.capacity + this.INCREASE_RATE;
            int[] newList = new int[newCapacity];

            // Copy old contents
            for (int i = 0; i < this.curr_num_elements; i++) {
                newList[i] = this.list[i];
            }

            this.list = newList;
            this.capacity = newCapacity;
            return true; // success

        } catch (OutOfMemoryError e) {
            return false; // could not allocate new array
        }
    }


    public boolean add(int element) {
        if (this.list == null) {
            return false;
        }

        if (this.curr_num_elements >= this.capacity) {
            boolean expansion_success = this.increase_capacity();
            if (!expansion_success) return false;
        }

        this.list[this.curr_num_elements++] = element;
        return true;
    }

    public boolean addAll(int[] elements) {
        for (int element : elements) {
            boolean add_success = this.add(element);
            if (!add_success) return false;
        }

        return true;
    }

    // remove
    // decrease_capacity (probably)

    @Override
    public String toString() {
        StringBuilder arr_string = new StringBuilder("[");

        for (int i = 0; i < this.curr_num_elements; i++) {
            arr_string.append(this.list[i]);
            if (i < this.curr_num_elements - 1) {
                arr_string.append(", ");
            }
        }
        arr_string.append("]");

        return arr_string.toString();
    }
}
