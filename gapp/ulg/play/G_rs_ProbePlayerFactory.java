package gapp.ulg.play;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Player;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;


class G_rs_ProbePlayerFactory<P> implements PlayerFactory<Player<P>,GameRuler<P>> {
    @Override
    public String name() { return "G_rs_ProbePlayer"; }
    @Override
    public void setDir(Path dir) { }
    @Override
    @SuppressWarnings("unchecked")
    public List<Param<?>> params() { return params; }

    @Override
    public Play canPlay(GameFactory<? extends GameRuler<P>> gF) {
        Objects.requireNonNull(gF);
        return Play.YES;
    }

    @Override
    public String tryCompute(GameFactory<? extends GameRuler<P>> gF, boolean parallel,
                             Supplier<Boolean> interrupt) {
        Objects.requireNonNull(gF);
        return null;
    }

    @Override
    public Player<P> newPlayer(GameFactory<? extends GameRuler<P>> gF, String name) {
        if (canPlay(gF) != Play.YES) throw new IllegalStateException();
        Objects.requireNonNull(name);
        return new G_rs_ProbePlayer<>(name, method.get(), time.get(), counter.get());
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


    private final Param<String> method = new SimpleParam<>("Method",
            "Method", 0, "","setGame","getMove","moved");
    private final Param<Long> time = new SimpleParam<>("TimeNotify",
            "Time to notify", 0, -1L,100L,150L,200L,250L,300L,1000L,1500L,2000L,2500L);
    private final Param<Integer> counter = new SimpleParam<>("Counter", "Counter", 0, -1,1,2,3,4,5,6,
            7,8,9,10,11,12,13,14,15,16,17,18,19,20);
    private final List<Param<?>> params = Collections.unmodifiableList(Arrays.asList(method, time, counter));
}
