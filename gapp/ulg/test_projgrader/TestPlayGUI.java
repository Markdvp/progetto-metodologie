package gapp.ulg.test_projgrader;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.*;
import gapp.ulg.game.util.PlayGUI;
import gapp.ulg.game.util.PlayerGUI;

import static gapp.ulg.game.util.PlayerGUI.MoveChooser;
import static gapp.ulg.game.board.Board.Dir;
import static gapp.ulg.game.board.PieceModel.Species;
import static gapp.ulg.test_projgrader.ProjectGrader.Result;
import static gapp.ulg.test_projgrader.ProjectGrader.handleThrowable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;


public class TestPlayGUI {
    public static synchronized void outbreak(long c) {
        if (c != counter) return;
        currObserver.outbreak();
    }

    static Result test_PlayGUI(String method, String gmTime, long tol, long timeout, long timeNotify) {
        Observer<PieceModel<Species>> observer = new Observer<>();
        PlayGUI<PieceModel<Species>> pGUI = new PlayGUI<>(observer, -1);
        String pName1 = "G_rs_ProbePlayer", pName2 = "G_rs_ProbePlayer";
        pGUI.setGameFactory("Othello");
        if (gmTime != null)
            pGUI.setGameFactoryParamValue("Time", gmTime);
        PlayerFactory.Play p = pGUI.setPlayerFactory(1, pName1, PP[0], null);
        if (!Objects.equals(p, PlayerFactory.Play.YES))
            return new Result(String.format("setPlayerFactory(%d,%s,%s,%s) ritorna %s", 1, pName1, PP[0], null, p));
        pGUI.setPlayerFactoryParamValue(1, "Method", method);
        pGUI.setPlayerFactoryParamValue(1, "TimeNotify", timeNotify);
        pGUI.setPlayerFactoryParamValue(1, "Counter", getCounter());
        p = pGUI.setPlayerFactory(2, pName2, PP[1], null);
        if (!Objects.equals(p, PlayerFactory.Play.YES))
            return new Result(String.format("setPlayerFactory(%d,%s,%s,%s) ritorna %s", 2, pName2, PP[1], null, p));
        long time = System.currentTimeMillis();
        pGUI.play(tol, timeout, -1, -1, -1, -1);
        time = System.currentTimeMillis() - time;
        if (time > MAX_BLOCK_TIME)
            return new Result(String.format("play(%d,%d,-1,-1,-1,-1) blocca per %dms", tol, timeout, time));
        //ProjectGrader.REPORT.report("play: " + time);
        if (!observer.waitNotify()) return new Result();
        Pair<String, Exception> exc = observer.exception();
        if (exc != null)
            return new Result("Eccezione inattesa in Observer." + exc.a + " " + exc.b);
        Pair<Integer, String> lb = observer.limitBreak();
        if (observer.isOutBreak()) {
            if (lb != null) {
                if (lb.a != 1) return new Result("LimitBreak errato: (" + lb.a + "," + lb.b + ")");
                else return new Result("Mancata interruzione");
            } else return new Result("Mancato limitBreak (outbreak)");
        }
        if (observer.interrupted() != null) {
            if (lb == null) return new Result("Mancato limitBreak");
            else if (lb.a != 1) return new Result("LimitBreak errato: (" + lb.a + "," + lb.b + ")");
        }
        return new Result();
    }

    static Result test_PlayGUI(String gName, String[] gPN, Object[] gPV, String pName1, String[] pPN1, Object[] pPV1,
                               String pName2, String[] pPN2, Object[] pPV2, int n, Path dir, int index) {
        Observer<PieceModel<Species>> observer = new Observer<>();
        PlayGUI<PieceModel<Species>> pGUI = new PlayGUI<>(observer, -1);
        int[] rr = {0,0,0};
        for (int k = 0 ; k < n ; k++) {
            try {
                pGUI.setGameFactory(gName);
            } catch (IllegalStateException e) {
                try { Thread.sleep(50); } catch (InterruptedException e1) { break; }
                pGUI.setGameFactory(gName);
            }
            for (int i = 0; i < gPN.length; i++)
                pGUI.setGameFactoryParamValue(gPN[i], gPV[i]);
            PlayerFactory.Play p = pGUI.setPlayerFactory(1, pName1, PP[0], dir);
            if (!Objects.equals(p, PlayerFactory.Play.YES))
                return new Result(String.format("setPlayerFactory(%d,%s,%s,%s) ritorna %s", 1, pName1, PP[0], null, p));
            for (int i = 0; i < pPN1.length; i++)
                pGUI.setPlayerFactoryParamValue(1, pPN1[i], pPV1[i]);
            p = pGUI.setPlayerFactory(2, pName2, PP[1], dir);
            if (!Objects.equals(p, PlayerFactory.Play.YES))
                return new Result(String.format("setPlayerFactory(%d,%s,%s,%s) ritorna %s", 2, pName2, PP[1], null, p));
            for (int i = 0; i < pPN2.length; i++)
                pGUI.setPlayerFactoryParamValue(2, pPN2[i], pPV2[i]);
            long time = System.currentTimeMillis();
            pGUI.play(-1, -1, -1, -1, -1, -1);
            time = System.currentTimeMillis() - time;
            if (time > MAX_BLOCK_TIME)
                return new Result("play(-1,-1,-1,-1,-1,-1) blocca per " + time + "ms");
            //ProjectGrader.REPORT.report(" play: " + time);
            //time = System.currentTimeMillis();
            if (!observer.waitNotify()) return new Result();
            //ProjectGrader.REPORT.report(" wait: " + (System.currentTimeMillis() - time));
            Pair<String, Exception> exc = observer.exception();
            if (exc != null)
                return new Result("Eccezione inattesa in Observer." + exc.a + " " + exc.b);
            Pair<Integer, String> lb = observer.limitBreak();
            if (lb != null)
                return new Result("LimitBreak inatteso: (" + lb.a + "," + lb.b + ")");
            if (observer.interrupted() != null)
                return new Result("Interruzione inattesa: " + observer.interrupted());
            rr[observer.result()]++;
        }
        if ((index == 1 && rr[1] < n) || (index == 2 && rr[1] > 0)) return new Result("Non ottimale: "+Arrays.toString(rr));
        return new Result();
    }



    static <P> Result test_PlayerGUI(Supplier<GameFactory<? extends GameRuler<P>>> gFS, String[] pN, Object[] pV,
                                     Supplier<Player<P>> pS, BiFunction<MoveChooser<P>,MParams<P>,String> test, Random rnd, int maxMoves, int n) {
        for (int i = 0 ; i < n ; i++) {
            Master<P> master = new Master<>(test);
            Play<P> play = new Play<>(gFS, pN, pV, pS, master, rnd, maxMoves);
            FutureTask<Result> mTask = new FutureTask<>(master);
            FutureTask<Result> pTask = new FutureTask<>(play);
            Thread mTh = new Thread(mTask, "PlayerGUI Master");
            Thread pTh = new Thread(pTask, "PlayerGUI Play");
            mTh.setDaemon(true);
            pTh.setDaemon(true);
            mTh.start();
            pTh.start();
            Result res = null;
            long ms = 50;
            try {
                while (res == null) {
                    try {
                        res = mTask.get(ms, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException ignored) {}
                    if (res == null || (!res.fatal && res.err == null)) {
                        try {
                            res = pTask.get(ms, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException ignored) {}
                    }
                }
            } catch (CancellationException | InterruptedException | ExecutionException e) {
                return res;
            }
            mTask.cancel(true);
            pTask.cancel(true);
            if (res.fatal || res.err != null) return res;
        }
        return new Result();
    }

    static <P> String testMove(MoveChooser<P> mc, MParams<P> p) {
        if (p == null || p.move == null) return "ERR";
        String err = "ERRORE MoveChooser: "+toStr(p.move)+"   ";
        String e = goMove(mc, p.move, null, null);
        if (e != null) return err+e;
        mc.move();
        return null;
    }

    static <P> String testIncludeVM(MoveChooser<P> mc, MParams<P> p) {
        if (p == null || p.adjacent == null || p.validMoves == null || p.move == null)
            return "ERR";
        String err = "ERRORE MoveChooser: ";
        for (Move<P> m : p.validMoves) {
            if (!Move.Kind.ACTION.equals(m.kind)) continue;
            String e = goMove(mc, m, p.validMoves::contains, null);
            if (e != null) return err+e;
            int count = m.actions.size();
            while (mc.back() != null && count > 0) count--;
            if (mc.back() != null) return err+"radice back non ritorna null";
        }
        String e = goMove(mc, p.move, null, null);
        if (e != null) return err+e;
        mc.move();
        return null;
    }

    static <P> String testVM(MoveChooser<P> mc, MParams<P> p) {
        if (p == null || p.adjacent == null || p.validMoves == null || p.move == null)
            return "ERR";
        String err = "ERRORE MoveChooser: ";
        int maxL = 0;
        for (Move<P> m : p.validMoves) {
            if (!Move.Kind.ACTION.equals(m.kind)) continue;
            if (m.actions.size() > maxL) maxL = m.actions.size();
            String e = goMove(mc, m, p.validMoves::contains, null);
            if (e != null) return err+e;
            int count = m.actions.size();
            while (mc.back() != null && count > 0) count--;
            if (mc.back() != null) return err+"radice back non ritorna null";
        }
        Function<List<Action<P>>,String> visitor = u -> (mc.isFinal() && (u.size() == 0 || !p.validMoves.contains(new Move<>(u))) ? "nodo finale no mossa" : null);
        String e = visit(mc, maxL+1, visitor, null, null);
        if (e != null) return err+e;
        e = goMove(mc, p.move, null, null);
        if (e != null) return err+e;
        mc.move();
        return null;
    }

    static <P> String testVMBack(MoveChooser<P> mc, MParams<P> p) {
        if (p == null || p.adjacent == null || p.validMoves == null || p.move == null || p.conf == null)
            return "ERR";
        String err = "ERRORE MoveChooser: ";
        BiFunction<Map<Pos,P>,Move<P>,Map<Pos,P>> next = (c,m) -> move(c, p.adjacent, m);
        int maxL = 0;
        for (Move<P> m : p.validMoves) {
            if (!Move.Kind.ACTION.equals(m.kind)) continue;
            Deque<ConfSM<P>> confSM = new ArrayDeque<>();
            confSM.push(new ConfSM<>(p.conf, null));
            if (m.actions.size() > maxL) maxL = m.actions.size();
            String e = goMove(mc, m, p.validMoves::contains, (sub,r) -> confSM.push(new ConfSM<>(next.apply(confSM.peek().conf, sub), sub, r)));
            if (e != null) return err+e;
            while (confSM.size() > 1) {
                ConfSM<P> csm = confSM.pop();
                if (!csm.root) {
                    Move<P> inv = mc.back();
                    if (inv == null) return err + "back ritorna null";
                    if (!confSM.peek().conf.equals(next.apply(csm.conf, inv)))
                        return err+"back ritorna sotto-mossa inversa errata "+toStr(inv)+" di "+toStr(csm.sm);
                }
            }
            if (mc.back() != null) return err+"radice back non ritorna null";
        }
        Function<List<Action<P>>,String> visitor = u -> (mc.isFinal() && (u.size() == 0 || !p.validMoves.contains(new Move<>(u))) ? "nodo finale no mossa" : null);
        String e = visit(mc, maxL+1, visitor, p.conf, next);
        if (e != null) return err+e;
        e = goMove(mc, p.move, null, null);
        if (e != null) return err+e;
        mc.move();
        return null;
    }

    static class MParams<P> {
        final Move<P> move;
        final Set<Move<P>> validMoves;
        final BiFunction<Pos,Dir,Pos> adjacent;
        final Map<Pos,P> conf;

        MParams(Move<P> m, Set<Move<P>> vm, BiFunction<Pos,Dir,Pos> a, Map<Pos,P> c) {
            move = m;
            validMoves = vm;
            adjacent = a;
            conf = c;
        }
    }

    private static class Pair<A,B> {
        final A a;
        final B b;

        Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }

    private static final String[] PP = {"Alice","Bob"};

    private static class Observer<P> implements PlayGUI.Observer<P> {
        Observer() {
            setObserver(this);
        }

        @Override
        public synchronized void setGame(GameRuler<P> g) {
            try {
                Objects.requireNonNull(g);
                gR = g;
                result = gR.result();
                exc = null;
                limitBreak = null;
                interrupted = null;
                notified = false;
                outbreak = false;
            } catch (Exception e) {
                exc = new Pair<>("setGame", e);
                _notify();
                throw e;
            }
        }

        @Override
        public synchronized void moved(int i, Move<P> m) {
            try {
                if (gR == null) throw new IllegalStateException();
                gR.isPlaying(i);
                if (!gR.move(m)) throw new IllegalArgumentException();
                result = gR.result();
                if (result != -1) _notify();
            } catch (Exception e) {
                exc = new Pair<>("moved", e);
                _notify();
                throw e;
            }
        }

        @Override
        public synchronized void limitBreak(int i, String msg) {
            try {
                Objects.requireNonNull(msg);
                gR.isPlaying(i);
                gR.move(new Move<>(Move.Kind.RESIGN));
                result = gR.result();
                limitBreak = new Pair<>(i, msg);
            } catch (Exception e) {
                exc = new Pair<>("limitBreak", e);
                _notify();
                throw e;
            }
        }

        @Override
        public synchronized void interrupted(String msg) {
            interrupted = msg != null ? msg : "";
            _notify();
        }

        synchronized boolean waitNotify() {
            while (!notified()) {
                try {
                    wait();
                } catch (InterruptedException e) { return false; }
            }
            return true;
        }

        synchronized boolean notified() {
            boolean saved = notified;
            notified = false;
            return saved;
        }
        synchronized void outbreak() {
            outbreak = true;
            _notify();
        }
        synchronized int result() { return result; }
        synchronized Pair<String,Exception> exception() { return exc; }
        synchronized Pair<Integer,String> limitBreak() { return limitBreak; }
        synchronized boolean isOutBreak() { return outbreak; }
        synchronized String interrupted() { return interrupted; }


        private void _notify() {
            notified = true;
            notifyAll();
        }

        private volatile GameRuler<P> gR = null;
        private volatile Pair<String,Exception> exc = null;
        private volatile Pair<Integer,String> limitBreak = null;
        private volatile String interrupted = null;
        private volatile boolean notified = false, outbreak = false;
        private volatile int result = -1;
    }

    private static class ConfSM<P> {
        final Map<Pos,P> conf;
        final Move<P> sm;
        final boolean root;

        ConfSM(Map<Pos,P> c, Move<P> m) { this(c, m, false); }

        ConfSM(Map<Pos,P> c, Move<P> m, boolean r) {
            conf = c;
            sm = m;
            root = r;
        }
    }

    private static class Play<P> implements Callable<Result> {
        @Override
        public Result call() throws Exception {
            try {
                Player<P> pFirst = pS.get();
                Player<P> pGUI = new PlayerGUI<>("PlayerGUI", master);
                GameFactory<? extends GameRuler<P>> gF = gFS.get();
                for (int i = 0 ; i < pN.length ; i++)
                    for (Param<?> param : gF.params())
                        if (Objects.equals(param.name(), pN[i]))
                            param.set(pV[i]);
                gF.setPlayerNames(pFirst.name(), pGUI.name());
                GameRuler<P> gR = gF.newGame();
                Board<P> board = gR.getBoard();
                pFirst.setGame(gR.copy());
                pGUI.setGame(gR.copy());
                Map<Pos,Map<Dir,Pos>> adj = Collections.synchronizedMap(new HashMap<>());
                for (Pos p : gR.mechanics().positions) {
                    Map<Dir,Pos> map = Collections.synchronizedMap(new EnumMap<>(Dir.class));
                    for (Dir d : Dir.values())
                        map.put(d, board.adjacent(p, d));
                    adj.put(p, map);
                }
                BiFunction<Pos,Dir,Pos> adjacent = (p,d) -> adj.get(p) != null ? adj.get(p).get(d) : null;
                int count = 0;
                while (gR.result() == -1) {
                    int i = gR.turn();
                    Move<P> m;
                    if (i == 1) m = pFirst.getMove();
                    else {
                        Set<Move<P>> vm = gR.validMoves();
                        Map<Pos,P> c = Collections.synchronizedMap(new HashMap<>());
                        for (Pos p : gR.mechanics().positions) {
                            P pm = board.get(p);
                            if (pm != null) c.put(p, pm);
                        }
                        master.params(new MParams<>(randMove(vm, rnd), vm, adjacent, c));
                        m = pGUI.getMove();
                    }
                    gR.move(m);
                    pFirst.moved(i, m);
                    pGUI.moved(i, m);
                    count++;
                    if (count >= maxMoves) break;
                }
                master.stop();
            } catch (Throwable t) { return handleThrowable(t); }
            return new Result();
        }

        Play(Supplier<GameFactory<? extends GameRuler<P>>> gFS, String[] pN, Object[] pV,
             Supplier<Player<P>> pS, Master<P> m, Random rnd, int maxM) {
            this.gFS = gFS;
            this.pN = pN;
            this.pV = pV;
            this.pS = pS;
            master = m;
            this.rnd = rnd;
            maxMoves = maxM;
        }

        private final Supplier<GameFactory<? extends GameRuler<P>>> gFS;
        private final String[] pN;
        private final Object[] pV;
        private final Supplier<Player<P>> pS;
        private final Master<P> master;
        private final Random rnd;
        private final int maxMoves;
    }

    private static class Master<P> implements Callable<Result>, Consumer<MoveChooser<P>> {
        @Override
        public synchronized Result call() throws Exception {
            while (true) {
                while (moveChooser == null && !stop) {
                    try {
                        wait();
                    } catch (InterruptedException e) { return new Result(); }
                }
                if (stop) return new Result();
                try {
                    String err = test.apply(moveChooser, params);
                    moveChooser = null;
                    params = null;
                    if (err != null) return new Result(err);
                } catch (Throwable t) { return handleThrowable(t); }
            }
        }

        @Override
        public synchronized void accept(MoveChooser<P> mc) {
            moveChooser = mc;
            notifyAll();
        }

        Master(BiFunction<MoveChooser<P>,MParams<P>,String> t) {
            test = t;
            moveChooser = null;
            params = null;
            stop = false;
        }

        synchronized void params(MParams<P> p) { params = p; }
        synchronized void stop() {
            stop = true;
            notifyAll();
        }

        private final BiFunction<MoveChooser<P>,MParams<P>,String> test;
        private volatile MoveChooser<P> moveChooser;
        private volatile MParams<P> params;
        private volatile boolean stop;
    }

    private static String toStr(Pos p) {
        return "("+p.b+","+p.t+")";
    }

    private static String toStrPP(List<Pos> pp) {
        String s = "";
        for (Pos p : pp)
            s += (s.isEmpty() ? "" : ",")+toStr(p);
        return "["+s+"]";
    }

    private static <P> String toStrPM(P p) {
        if (p == null) return "null";
        else if (p instanceof PieceModel) {
            PieceModel<?> pm = (PieceModel<?>)p;
            return pm.species.name()+pm.color;
        } else return p.toString();
    }

    private static <P> String toStrPM(List<P> pcs) {
        String s = "";
        for (P p : pcs)
            s += (s.isEmpty() ? "" : ",")+toStrPM(p);
        return "["+s+"]";
    }

    private static <P> String toStr(Action<P> a) {
        switch (a.kind) {
            case ADD: return "ADD"+toStr(a.pos.get(0))+toStrPM(a.piece);
            case REMOVE: return "REMOVE"+toStrPP(a.pos);
            case MOVE: return "MOVE"+toStrPP(a.pos)+a.dir+a.steps;
            case JUMP: return "JUMP"+toStrPP(a.pos);
            case SWAP: return "SWAP"+toStrPP(a.pos)+toStrPM(a.piece);
        }
        return null;
    }

    private static <P> String toStr(List<Action<P>> aa) {
        String s = "";
        for (Action<P> a : aa)
            s += (s.isEmpty() ? "" : ",")+toStr(a);
        return "["+s+"]";
    }

    private static <P> String toStr(Move<P> m) {
        switch (m.kind) {
            case ACTION: return toStr(m.actions);
            case PASS: return "PASS";
            case RESIGN: return "RESIGN";
        }
        return null;
    }

    private static final long MAX_BLOCK_TIME = 100;



    private static <P> boolean isSM(Move<P> sm, List<Action<P>> aa, int index) {
        if (sm == null || sm.actions == null || sm.actions.size() == 0) return false;
        List<Action<P>> smAA = sm.actions;
        return index + smAA.size() <= aa.size() && smAA.equals(aa.subList(index, index + smAA.size()));
    }

    private static <P> Move<P> match(List<Move<P>> smm, List<Action<P>> aa, int index) {
        for (Move<P> sm : smm)
            if (isSM(sm, aa, index)) return sm;
        return null;
    }

    private static <P> Pos[] getSel(Move<P> sm) {
        Action<P> a = sm.actions.get(0);
        switch (a.kind) {
            case JUMP: return new Pos[] {a.pos.get(0)};
            default: return a.pos.toArray(new Pos[a.pos.size()]);
        }
    }

    private static <P> String go(MoveChooser<P> mc, Move<P> sm) {
        Function<String,String> e = s -> s+" "+toStr(sm);
        List<Move<P>> sel = mc.select(getSel(sm));
        if (sel == null || sel.isEmpty()) return e.apply("select ritorna null o lista vuota");
        if (!sel.contains(sm)) return e.apply("select");
        Action<P> a = sm.actions.get(0);
        switch (a.kind) {
            case ADD: case SWAP: case REMOVE:
                List<P> pcs = mc.selectionPieces();
                if (pcs == null) return e.apply("selectionPieces ritorna null");
                if (a.kind.equals(Action.Kind.REMOVE)) {
                    if (pcs.size() != 1 || pcs.get(0) != null) return e.apply("selectionPieces REMOVE "+toStrPM(pcs));
                    if (!sm.equals(mc.doSelection(null))) return e.apply("doSelection(null)");
                } else {
                    if (!pcs.contains(a.piece)) return e.apply("selectionPieces "+a.kind);
                    if (!sm.equals(mc.doSelection(a.piece))) return e.apply("doSelection(pm)");
                }
                break;
            case MOVE:
                if (!sm.equals(mc.moveSelection(a.dir, a.steps))) return e.apply("moveSelection");
                break;
            case JUMP:
                if (!sm.equals(mc.jumpSelection(a.pos.get(1)))) return e.apply("jumpSelection");
                break;
        }
        return null;
    }

    private static <P> String goMove(MoveChooser<P> mc, Move<P> m, Predicate<Move<P>> fm, BiConsumer<Move<P>,Boolean> update) {
        if (update == null) update = (o,r) -> {};
        List<Action<P>> aa = m.actions;
        Optional<Move<P>> r = mc.subMove();
        if (r == null) return "radice subMove ritorna null";
        int index = 0;
        if (r.isPresent()) {
            if (!isSM(r.get(), aa, 0)) return "radice subMove "+toStr(r.get());
            index = r.get().actions.size();
            if (fm != null && mc.isFinal() != fm.test(r.get())) return "radice isFinal "+toStr(r.get());
            update.accept(r.get(),true);
        } else if (fm != null && mc.isFinal()) return "radice vuota isFinal";
        while (index < aa.size()) {
            Move<P> sm = match(mc.childrenSubMoves(), aa, index);
            if (sm == null) return toStr(aa.subList(0, index))+" childrenSubMoves";
            String e = go(mc, sm);
            if (e != null) return toStr(aa.subList(0, index))+" "+e;
            index += sm.actions.size();
            if (fm != null && mc.isFinal() != fm.test(new Move<>(aa.subList(0, index))))
                return toStr(aa.subList(0, index))+" isFinal";
            update.accept(sm,false);
        }
        return null;
    }

    private static <P> String visit(MoveChooser<P> mc, int maxD, Function<List<Action<P>>,String> visitor,
                                    Map<Pos,P> conf, BiFunction<Map<Pos,P>,Move<P>,Map<Pos,P>> next) {
        class Vis {
            private String visit(List<Action<P>> prefix, Map<Pos,P> conf) {
                String err = toStr(prefix)+" ", e = visitor.apply(prefix);
                if (e != null) return err+e;
                depth++;
                if (depth > maxD) return err+"depth "+depth;
                List<Move<P>> children = mc.childrenSubMoves();
                if (children == null) return err+"childrenSubMoves ritorna null";
                if (!children.isEmpty()) {
                    for (Move<P> sm : children) {
                        Map<Pos,P> c = conf != null ? next.apply(conf,sm) : null;
                        e = go(mc, sm);
                        if (e != null) return err+e;
                        List<Action<P>> p = new ArrayList<>(prefix);
                        p.addAll(sm.actions);
                        e = visit(p, c);
                        if (e != null) return e;
                        Move<P> inv = mc.back();
                        if (inv == null) return toStr(p)+" "+"back ritorna null";
                        if (conf != null && !conf.equals(next.apply(c,inv)))
                            return err+"back ritorna sotto-mossa inversa errata "+toStr(inv)+" di "+toStr(sm);
                    }

                } else if (!mc.isFinal()) return err+"nodo foglia non finale";
                depth--;
                return null;
            }
            private int depth = 0;
        }
        Optional<Move<P>> r = mc.subMove();
        if (r == null) return "radice subMove ritorna null";
        List<Action<P>> rp = new ArrayList<>();
        if (r.isPresent()) {
            rp = r.get().actions;
            if (conf != null)
                conf = next.apply(conf, r.get());
        }
        return new Vis().visit(rp, conf);
    }

    private static <P> Map<Pos,P> move(Map<Pos,P> conf, BiFunction<Pos,Dir,Pos> adj, Move<P> m) {
        Map<Pos,P> next = new HashMap<>(conf);
        for (Action<P> a : m.actions) {
            switch (a.kind) {
                case ADD: next.put(a.pos.get(0), a.piece); break;
                case REMOVE: a.pos.forEach(next::remove); break;
                case MOVE: {
                    List<Pos> newP = new ArrayList<>();
                    List<P> pcs = new ArrayList<>();
                    for (Pos p : a.pos) {
                        pcs.add(next.remove(p));
                        Pos pp = p;
                        for (int i = 0; i < a.steps; i++) pp = adj.apply(pp, a.dir);
                        newP.add(pp);
                    }
                    for (int i = 0; i < newP.size(); i++)
                        next.put(newP.get(i), pcs.get(i));
                } break;
                case JUMP: next.put(a.pos.get(1), next.remove(a.pos.get(0))); break;
                case SWAP: a.pos.forEach(p -> next.put(p, a.piece)); break;
            }
        }
        return next;
    }

    private static <P> Move<P> randMove(Set<Move<P>> vm, Random rnd) {
        List<Move<P>> lst = new ArrayList<>(vm);
        lst.remove(new Move<P>(Move.Kind.RESIGN));
        lst.remove(new Move<P>(Move.Kind.PASS));
        return lst.get(rnd.nextInt(lst.size()));
    }

    private static synchronized void setObserver(Observer<?> o) {
        currObserver = o;
        counter++;
    }

    private static synchronized int getCounter() { return counter; }

    private static volatile Observer<?> currObserver = null;
    private static volatile int counter = 0;
}
