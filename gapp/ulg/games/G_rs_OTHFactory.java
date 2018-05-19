package gapp.ulg.games;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static gapp.ulg.game.board.PieceModel.Species;


public class G_rs_OTHFactory implements GameFactory<GameRuler<PieceModel<Species>>> {
    /** Crea una fattoria di {@link GameRuler} per giocare a Othello */
    public G_rs_OTHFactory() { }

    @Override
    public String name() { return "Othello"; }
    @Override
    public int minPlayers() { return 2; }
    @Override
    public int maxPlayers() { return 2; }

    /** Ritorna una lista con i seguenti due parametri:
     * <pre>
     * Primo parametro, valori di tipo String
     *     - name: "Time"
     *     - prompt: "Time limit for a move"
     *     - values: ["No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"]
     *     - default: "No limit"
     * Secondo parametro, valori di tipo String
     *     - name: "Board"
     *     - prompt: "Board size"
     *     - values: ["6x6","8x8","10x10","12x12"]
     *     - default: "8x8"
     * </pre>
     * @return la lista con i due parametri */
    @Override
    @SuppressWarnings("unchecked")
    public List<Param<?>> params() { return params; }

    @Override
    public void setPlayerNames(String... names) {
        if (names.length != 2) throw new IllegalArgumentException();
        for (String name : names)
            Objects.requireNonNull(name);
        this.names = names.clone();
    }

    @Override
    public GameRuler<PieceModel<Species>> newGame() {
        if (names == null) throw new IllegalStateException();
        return new G_rs_OTH(TO_MILLIS[time.values().indexOf(time.get())],
                TO_SIZE[board.values().indexOf(board.get())], names[0], names[1]);
    }


    private static class SimpleParam<T> implements Param<T> {
        @SafeVarargs
        SimpleParam(String nm, String p, int defIndex, T...vv) {
            name = nm;
            prompt = p;
            values = Collections.unmodifiableList(Arrays.asList(vv));
            valueIndex = defIndex;
        }

        @Override
        public String name() { return name; }
        @Override
        public String prompt() { return prompt; }
        @Override
        public List<T> values() { return values; }

        @Override
        public void set(Object v) {
            int i = values.indexOf(v);
            if (i < 0) throw new IllegalArgumentException();
            valueIndex = i;
        }

        @Override
        public T get() { return values.get(valueIndex); }

        private final String name, prompt;
        private final List<T> values;
        private volatile int valueIndex;
    }

    private static final long[] TO_MILLIS = {-1,1000,2000,3000,5000,10_000,20_000,30_000,60_000,120_000,300_000};
    private static final int[] TO_SIZE = {6,8,10,12};

    private String[] names = null;
    private final Param<String> time = new SimpleParam<>("Time", "Time limit for a move",
            0, "No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
    private final Param<String> board = new SimpleParam<>("Board", "Board size",
            1, "6x6","8x8","10x10","12x12");
    private final List<Param<?>> params = Collections.unmodifiableList(Arrays.asList(time, board));

}
