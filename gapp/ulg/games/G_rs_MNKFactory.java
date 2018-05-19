package gapp.ulg.games;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;

import java.util.*;

import static gapp.ulg.game.board.PieceModel.Species;


public class G_rs_MNKFactory implements GameFactory<GameRuler<PieceModel<Species>>> {
    @Override
    public String name() { return "m,n,k-game"; }
    @Override
    public int minPlayers() { return 2; }
    @Override
    public int maxPlayers() { return 2; }

    /** Ritorna una lista con i seguenti quattro parametri:
     * <pre>
     * Primo parametro, valori di tipo String
     *     - name: "Time"
     *     - prompt: "Time limit for a move"
     *     - values: ["No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"]
     *     - default: "No limit"
     * Secondo parametro, valori di tipo Integer
     *     - name: "M"
     *     - prompt: "Board width"
     *     - values: [1,2,3,...,20]
     *     - default: 3
     * Terzo parametro, valori di tipo Integer
     *     - name: "N"
     *     - prompt: "Board height"
     *     - values: [1,2,3,...,20]
     *     - default: 3
     * Quarto parametro, valori di tipo Integer
     *     - name: "K"
     *     - prompt: "Length of line"
     *     - values: [1,2,3]
     *     - default: 3
     * </pre>
     * Per i parametri "M","N" e "K" i valori ammissibili possono cambiare a seconda
     * dei valori impostati. Più precisamente occorre che i valori ammissibili
     * garantiscano sempre le seguenti condizioni
     * <pre>
     *     1 <= K <= max{M,N} <= 20   AND   1 <= min{M,N}
     * </pre>
     * dove M,N,K sono i valori impostati. Indicando con minX, maxX il minimo e il
     * massimo valore per il parametro X le condizioni da rispettare sono:
     * <pre>
     *     minM <= M <= maxM
     *     minN <= N <= maxN
     *     minK <= K <= maxK
     *     minK = 1  AND  maxK = max{M,N}  AND  maxM = 20  AND  maxN = 20
     *     N >= K  IMPLICA  minM = 1
     *     N < K   IMPLICA  minM = K
     *     M >= K  IMPLICA  minN = 1
     *     M < K   IMPLICA  minN = K
     * </pre>
     * @return la lista con i quattro parametri */
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
        return new G_rs_MNK(TO_MILLIS[time.values().indexOf(time.get())], pM.get(),
                pN.get(), pK.get(), names[0], names[1]);
    }



    private static final long[] TO_MILLIS = {-1,1000,2000,3000,5000,10_000,20_000,30_000,60_000,120_000,300_000};
    private static final int MIN = 1, MAX = 20, DEF = 3;

    private static List<Integer> range(int a, int b) {
        List<Integer> range = new ArrayList<>();
        for (int i = a ; i <= b ; i++) range.add(i);
        return Collections.unmodifiableList(range);
    }

    private static class ParamMNK implements Param<Integer> {
        private ParamMNK(G_rs_MNKFactory gF, String name, String prompt, int max) {
            this.gF = gF;
            this.name = name;
            this.prompt = prompt;
            values = range(MIN, max);
        }
        @Override
        public String name() { return name; }
        @Override
        public String prompt() { return prompt; }
        @Override
        public List<Integer> values() { return values; }
        @Override
        public void set(Object v) {
            if (!values.contains(v)) throw new IllegalArgumentException();
            value = (Integer)v;
            gF.check();
        }
        @Override
        public Integer get() { return value; }

        private final G_rs_MNKFactory gF;
        private final String name, prompt;
        private List<Integer> values;
        private Integer value = DEF;
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

    private void check() {
        int k = pK.get();
        if (pN.value < k) {
            if (pM.values.get(0) < k) pM.values = range(k, MAX);
        } else if (pM.values.get(0) > MIN)
            pM.values = range(MIN, MAX);
        if (pM.value < k) {
            if (pN.values.get(0) < k) pN.values = range(k, MAX);
        } else if (pN.values().get(0) > MIN)
            pN.values = range(MIN, MAX);
        if (pK.values.get(pK.values.size()-1) != Math.max(pM.value, pN.value))
            pK.values = range(MIN, Math.max(pM.value, pN.value));
    }

    private final Param<String> time = new SimpleParam<>("Time", "Time limit for a move",
            0, "No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
    private final ParamMNK pM = new ParamMNK(this, "M", "Board width", MAX);
    private final ParamMNK pN = new ParamMNK(this, "N", "Board height", MAX);
    private final ParamMNK pK = new ParamMNK(this, "K", "Length of line", DEF);
    private final List<Param<?>> params = Collections.unmodifiableList(Arrays.asList(time, pM, pN, pK));

    private String[] names = null;
}
