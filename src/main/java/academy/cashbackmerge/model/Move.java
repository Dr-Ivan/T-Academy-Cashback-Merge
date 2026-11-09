package academy.cashbackmerge.model;

/** Направление хода и порядок обхода клеток поля. */
public enum Move {
    UP {
        @Override
        public Position position(int lineIndex, int offset, int size) {
            return new Position(offset, lineIndex);
        }
    },
    DOWN {
        @Override
        public Position position(int lineIndex, int offset, int size) {
            return new Position(size - 1 - offset, lineIndex);
        }
    },
    LEFT {
        @Override
        public Position position(int lineIndex, int offset, int size) {
            return new Position(lineIndex, offset);
        }
    },
    RIGHT {
        @Override
        public Position position(int lineIndex, int offset, int size) {
            return new Position(lineIndex, size - 1 - offset);
        }
    };

    public abstract Position position(int lineIndex, int offset, int size);
}
