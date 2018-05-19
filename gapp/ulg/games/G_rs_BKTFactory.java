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


public class G_rs_BKTFactory implements GameFactory<GameRuler<PieceModel<Species>>> {
    @Override
    public String name() { return "Breakthrough"; }
    @Override
    public int minPlayers() { return 2; }
    @Override
    public int maxPlayers() { return 2; }

    /** Ritorna una lista con i seguenti tre parametri:
     * <pre>
     * Primo parametro, valori di tipo String
     *     - name: "Time"
     *     - prompt: "Time limit for a move"
     *     - values: ["No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"]
     *     - default: "No limit"
     * Secondo parametro, valori di tipo Integer
     *     - name: "Width"
     *     - prompt: "Board width"
     *     - values: [2,...,20]
     *     - default: 8
     * Terzo parametro, valori di tipo Integer
     *     - name: "Height"
     *     - prompt: "Board height"
     *     - values: [4,...,20]
     *     - default: 8
     * </pre>
     * @return la lista con i tre parametri */
    @Override
    public List<Param<?>> params() { return params; }

    @Override
    public void setPlayerNames(String... names) {
        if (names.length != 2) throw new IllegalArgumentException();
        for (String name : names) Objects.requireNonNull(name);
        this.names = names.clone();
    }

    @Override
    public GameRuler<PieceModel<Species>> newGame() {
        if (names == null) throw new IllegalStateException();
        return new G_rs_BKT(TO_MILLIS[time.values().indexOf(time.get())], pW.get(), pH.get(), names[0], names[1]);
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

    private final Param<String> time = new SimpleParam<>("Time", "Time limit for a move",
            0, "No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
    private final Param<Integer> pW = new SimpleParam<>("Width", "Board width", 6, 2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20);
    private final Param<Integer> pH = new SimpleParam<>("Height", "Board height", 4, 4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20);
    private final List<Param<?>> params = Collections.unmodifiableList(Arrays.asList(time, pW, pH));

    private String[] names = null;
}
