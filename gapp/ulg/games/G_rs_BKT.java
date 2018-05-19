package gapp.ulg.games;

import gapp.ulg.game.board.*;
import gapp.ulg.game.util.BoardOct;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static gapp.ulg.game.board.Board.Dir;
import static gapp.ulg.game.board.PieceModel.Species;
import static gapp.ulg.game.util.Utils.UnmodifiableBoard;


public class G_rs_BKT implements GameRuler<PieceModel<Species>> {
    public G_rs_BKT(long time, int w, int h, String p1, String p2) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);
        name = "Breakthrough"+w+"x"+h;
        board = new BoardOct<>(w, h);                 // La board
        start((p,pm) -> board.put(pm, p));
        unModBoard = UnmodifiableBoard(board);  // La view immodificabile della board
        playerNames = Collections.unmodifiableList(Arrays.asList(p1, p2));
        gResult = -1;
        currTurn = 1;
        history = new ArrayList<>();
        Map<Pos,PieceModel<Species>> c = new HashMap<>();
        start(c::put);
        gM = new Mechanics<>((time > 0 ? time : -1), PIECES, board.positions(), 2, new Situation<>(c,1), this::next);
    }

    @Override
    public String name() { return name; }

    @Override
    public <T> T getParam(String name, Class<T> c) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(c);
        Integer p;
        switch (name) {
            case "Time": return c.cast(millisToSM(gM.time));
            case "Width": p = unModBoard.width(); break;
            case "Height": p = unModBoard.height(); break;
            default: throw new IllegalArgumentException();
        }
        return c.cast(p);
    }

    @Override
    public List<String> players() { return playerNames; }

    /** Assegna il colore "nero" al primo giocatore e "bianco" al secondo */
    @Override
    public String color(String name) {
        Objects.requireNonNull(name);
        if (!playerNames.contains(name)) throw new IllegalArgumentException();
        return name.equals(playerNames.get(0)) ? "nero" : "bianco";
    }

    @Override
    public Board<PieceModel<Species>> getBoard() { return unModBoard; }

    @Override
    public int turn() { return gResult == -1 ? currTurn : 0; }

    /** Se la mossa non è valida termina il gioco dando la vittoria all'altro
     * giocatore. */
    @Override
    public boolean move(Move<PieceModel<Species>> m) {
        Objects.requireNonNull(m);
        if (gResult != -1) throw new IllegalStateException();
        history.add(toSituation(unModBoard, currTurn));
        if (!validMoves().contains(m)) {        // Mossa non valida, vince l'altro giocatore
            gResult = 3 - currTurn;    // Indice dell'altro giocatore
            return false;
        }
        if (Move.Kind.RESIGN.equals(m.kind)) {  // Mossa di abbandono del gioco, vince l'altro giocatore
            gResult = 3 - currTurn;    // Indice dell'altro giocatore
            return true;
        }
        move((p,pm) -> board.put(pm, p), board::remove, unModBoard::adjacent, m);
        currTurn = 3 - currTurn;      // Il turno passa all'altro giocatore
        gResult = check(board::get);
        return true;
    }

    @Override
    public boolean unMove() {
        if (history.isEmpty()) return false;
        Situation<PieceModel<Species>> s = history.remove(history.size()-1);
        toBoard(s, board);
        gResult = -1;
        currTurn = s.turn;
        return true;
    }

    @Override
    public boolean isPlaying(int i) {
        if (i < 1 || i > 2) throw new IllegalArgumentException();
        return gResult == -1;
    }

    @Override
    public int result() { return gResult; }

    @Override
    public Set<Move<PieceModel<Species>>> validMoves() {
        if (gResult != -1) throw new IllegalStateException();
        Set<Move<PieceModel<Species>>> vm = validMoves(board::get, currTurn);
        vm.add(new Move<>(Move.Kind.RESIGN));  // È sempre possibile abbandonare il gioco
        return Collections.unmodifiableSet(vm);
    }

    @Override
    public GameRuler<PieceModel<Species>> copy() { return new G_rs_BKT(this); }

    @Override
    public Mechanics<PieceModel<Species>> mechanics() { return gM; }



    private static <P> void move(BiConsumer<Pos,P> put, Function<Pos,P> remove, BiFunction<Pos,Dir,Pos> adj, Move<P> m) {
        if (m.kind == Move.Kind.RESIGN || m.kind == Move.Kind.PASS) return;
        for (Action<P> a : m.actions) {
            switch (a.kind) {
                case ADD: put.accept(a.pos.get(0), a.piece); break;
                case REMOVE: a.pos.forEach(remove::apply); break;
                case MOVE: {
                    List<Pos> newP = new ArrayList<>();
                    List<P> pcs = new ArrayList<>();
                    for (Pos p : a.pos) {
                        pcs.add(remove.apply(p));
                        Pos pp = p;
                        for (int i = 0; i < a.steps; i++) pp = adj.apply(pp, a.dir);
                        newP.add(pp);
                    }
                    for (int i = 0; i < newP.size(); i++)
                        put.accept(newP.get(i), pcs.get(i));
                } break;
                case JUMP: put.accept(a.pos.get(1), remove.apply(a.pos.get(0))); break;
                case SWAP: a.pos.forEach(p -> put.accept(p, a.piece)); break;
            }
        }
    }

    private static <P> Situation<P> toSituation(Board<P> b, int t) {
        Map<Pos,P> conf = new HashMap<>();
        b.get().forEach(p -> conf.put(p, b.get(p)));
        return new Situation<>(conf, t);
    }

    private static <P> void toBoard(Situation<P> s, Board<P> b) {
        for (Pos p : b.positions()) {
            P pm = s.get(p);
            if (pm != null) b.put(pm, p);
            else b.remove(p);
        }
    }

    private static String millisToSM(long millis) {
        if (millis <= 0) return "No limit";
        return millis < 60_000 ? (millis/1000)+"s" : (millis/60_000)+"m";
    }

    private static final PieceModel<Species> NERO = new PieceModel<>(Species.PAWN, "nero"),
            BIANCO = new PieceModel<>(Species.PAWN, "bianco");
    private static final List<PieceModel<Species>> PIECES = Collections.unmodifiableList(Arrays.asList(NERO, BIANCO));

    /** Crea una copia (profonda) dell'oggetto Breakthrough dato
     * @param o  un oggetto Breakthrough */
    private G_rs_BKT(G_rs_BKT o) {
        name = o.name;
        board = new BoardOct<>(o.board.width(), o.board.height());
        for (Pos p : board.positions()) {             // Copia la disposizione dei pezzi
            PieceModel<Species> pm = o.board.get(p);
            if (pm != null) board.put(pm, p);
        }
        unModBoard = UnmodifiableBoard(board);  // La view immodificabile della board
        playerNames = o.playerNames;     // Può essere condiviso perché immodificabile
        history = new ArrayList<>();     // Copia la history
        history.addAll(o.history);
        currTurn = o.currTurn;
        gResult = o.gResult;
        Map<Pos,PieceModel<Species>> c = new HashMap<>();
        start(c::put);
        gM = new Mechanics<>(o.gM.time, PIECES, board.positions(), 2, new Situation<>(c,1), this::next);
    }

    private void start(BiConsumer<Pos,PieceModel<Species>> put) {
        int w = board.width(), h = board.height();
        for (int t = 0 ; t <= 1 ; t++)
            for (int b = 0 ; b < w ; b++) put.accept(new Pos(b, t), BIANCO);
        for (int t = h-2 ; t <= h-1 ; t++)
            for (int b = 0 ; b < w ; b++) put.accept(new Pos(b, t), NERO);
    }

    private Map<Move<PieceModel<Species>>,Situation<PieceModel<Species>>> next(Situation<PieceModel<Species>> s) {
        Objects.requireNonNull(s);
        Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> sMap = new HashMap<>();
        if (s.turn <= 0) return sMap;
        for (Move<PieceModel<Species>> m : validMoves(s::get, s.turn)) {
            Map<Pos, PieceModel<Species>> cNext = s.newMap();
            move(cNext::put, cNext::remove, unModBoard::adjacent, m);
            int tNext = 3 - s.turn;
            int r = check(cNext::get);
            if (r != -1) tNext = -r;
            sMap.put(m, new Situation<>(cNext, tNext));
        }
        if (sMap.isEmpty()) return null;   // Situazione non valida
        return sMap;
    }

    /** Ritorna l'insieme delle mosse valide (esclusa {@link Move.Kind#RESIGN}) per
     * il giocatore con indice di turnazione dato e la disposizione dei pezzi data
     * dalla funzione specificata.
     * @param c  funzione che per ogni posizione della board ritorna il pezzo in
     *           quella posizione o null se non c'è
     * @param turn  indice di turnazione del giocatore che deve muovere
     * @return l'insieme delle mosse valide (esclusa {@link Move.Kind#RESIGN}) per
     * il giocatore con indice di turnazione dato e la disposizione dei pezzi data
     * dalla funzione specificata. */
    private Set<Move<PieceModel<Species>>> validMoves(Function<Pos,PieceModel<Species>> c, int turn) {
        Set<Move<PieceModel<Species>>> vm = new HashSet<>();
        PieceModel<Species> piece = PIECES.get(turn - 1);
        Dir[] dirs = turn == 1 ? new Dir[] {Dir.DOWN_L, Dir.DOWN, Dir.DOWN_R} : new Dir[] {Dir.UP_L, Dir.UP, Dir.UP_R};
        for (Pos p : board.positions()) {
            if (!piece.equals(c.apply(p))) continue;
            for (Dir d : dirs) {      // Per ognuna delle tre direzioni
                Pos pp = board.adjacent(p, d);
                if (pp == null) continue;
                PieceModel<Species> pm = c.apply(pp);
                if (pm == null) {    // Posizione libera
                    Move<PieceModel<Species>> m = new Move<>(new Action<>(d, 1, p));
                    vm.add(m);
                } else if (!pm.equals(piece) && d != Dir.DOWN && d != Dir.UP) {  // Posizione occupata da pedone avversario in diagonale
                    Move<PieceModel<Species>> m = new Move<>(new Action<>(pp), new Action<>(d, 1, p));
                    vm.add(m);
                }
            }
        }
        return vm;
    }

    private int check(Function<Pos,PieceModel<Species>> c) {
        int w = board.width(), h = board.height();
        for (int b = 0 ; b < w ; b++)
            if (NERO.equals(c.apply(new Pos(b, 0)))) return 1;
        for (int b = 0 ; b < w ; b++)
            if (BIANCO.equals(c.apply(new Pos(b, h-1)))) return 2;
        int[] count = {0,0};
        for (Pos p : board.positions()) {
            PieceModel<Species> pm = c.apply(p);
            if (pm == null) continue;
            count[pm.equals(NERO) ? 0 : 1]++;
        }
        if (count[0] == 0) return 2;
        if (count[1] == 0) return 1;
        return -1;
    }


    private final String name;
    private final Board<PieceModel<Species>> board;
    private final Board<PieceModel<Species>> unModBoard;
    private final List<String> playerNames;
    private final List<Situation<PieceModel<Species>>> history;
    private final Mechanics<PieceModel<Species>> gM;
    private int currTurn;
    private int gResult;
}
