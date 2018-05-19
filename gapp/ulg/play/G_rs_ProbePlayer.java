package gapp.ulg.play;

import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Player;
import gapp.ulg.test_projgrader.TestPlayGUI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;


class G_rs_ProbePlayer<P> implements Player<P> {

    G_rs_ProbePlayer(String name, String method, long timeNotify, int c) {
        Objects.requireNonNull(name);
        this.name = name;
        this.method = method;
        this.timeNotify = timeNotify;
        counter = c;
        gR = null;
    }

    @Override
    public String name() { return name; }

    @Override
    public void setGame(GameRuler<P> g) {
        Objects.requireNonNull(g);
        gR = g;
        turn = 0;
        pause("setGame");
    }

    @Override
    public void moved(int i, Move<P> m) {
        if (gR == null || gR.result() != -1) throw new IllegalStateException();
        Objects.requireNonNull(m);
        gR.isPlaying(i);
        if (!gR.isValid(m)) throw new IllegalArgumentException();
        gR.move(m);
        pause("moved");
    }

    @Override
    public Move<P> getMove() {
        if (gR == null || gR.result() != -1 || (turn != 0 && gR.turn() != turn))
            throw new IllegalStateException();
        turn = gR.turn();
        List<Move<P>> lst = new ArrayList<>();
        lst.addAll(gR.validMoves());
        for (Iterator<Move<P>> it = lst.iterator(); it.hasNext() ; ) {
            Move<P> m = it.next();
            if (Move.Kind.RESIGN.equals(m.kind))
                it.remove();
        }
        pause("getMove");
        return lst.get((int)Math.floor(Math.random()*lst.size()));
    }

    private void pause(String mName) {
        if (!Objects.equals(method, mName)) return;
        long start = System.currentTimeMillis();
        while (true) {
            long pause = start + timeNotify - System.currentTimeMillis();
            if (pause > 0)
                try { Thread.sleep(pause); } catch (InterruptedException ignored) {}
            else break;
        }
        TestPlayGUI.outbreak(counter);
    }

    private final String name;
    private final String method;
    private final long timeNotify;
    private final int counter;
    private GameRuler<P> gR;
    private int turn;
}
