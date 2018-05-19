package gapp.ulg.games;

import gapp.ulg.game.board.*;
import gapp.ulg.game.util.BoardOct;
import gapp.ulg.game.util.Utils;

import java.util.*;
import java.util.function.Function;

import static gapp.ulg.game.board.Board.Dir;
import static gapp.ulg.game.board.PieceModel.Species;


class G_rs_OTH implements GameRuler<PieceModel<Species>> {
    /** Crea un GameRuler per fare una partita a Othello.
     * @param time  tempo in millisecondi per fare una mossa, se <= 0 significa nessun
     *              limite
     * @param size  dimensione della board, sono accettati solamente i valori 6,8,10,12
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se {@code p1} o {@code p2} è null
     * @throws IllegalArgumentException se size non è uno dei valori 6,8,10 o 12 */
    G_rs_OTH(long time, int size, String p1, String p2) {
        //throw new UnsupportedOperationException("DA IMPLEMENTARE");
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);
        if (!Arrays.asList(6,8,10,12).contains(size))
            throw new IllegalArgumentException();
        name = "Othello"+size+"x"+size;
        board = new BoardOct<>(size, size);            // La board
        int i = (size - 2)/2;
        board.put(BIANCO, new Pos(i, i+1));            // La configurazione iniziale
        board.put(BIANCO, new Pos(i+1, i));
        board.put(NERO, new Pos(i+1, i+1));
        board.put(NERO, new Pos(i, i));
        unModBoard = Utils.UnmodifiableBoard(board);  // La view immodificabile della board
        playerNames = Collections.unmodifiableList(Arrays.asList(p1, p2));
        history = new ArrayList<>();
        gResult = -1;
        currTurn = 1;
        gM = new Mechanics<>((time > 0 ? time : -1), PIECES, board.positions(), 2, start(), this::next);
    }

    /** Il nome rispetta il formato:
     * <pre>
     *     Othello<i>Size</i>
     * </pre>
     * dove <code><i>Size</i></code> è la dimensione della board, ad es. "Othello8x8". */
    @Override
    public String name() {
        return name;
    }

    @Override
    public <T> T getParam(String name, Class<T> c) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(c);
        String p;
        switch (name) {
            case "Time": p = millisToSM(gM.time); break;
            case "Board": p = board.width()+"x"+board.height(); break;
            default: throw new IllegalArgumentException();
        }
        return c.cast(p);
    }

    @Override
    public List<String> players() { return playerNames; }

    /** Assegna il colore "nero" al primo giocatore e "bianco" al secondo. */
    @Override
    public String color(String name) {
        Objects.requireNonNull(name);
        if (!playerNames.contains(name)) throw new IllegalArgumentException();
        return name.equals(playerNames.get(0)) ? "nero" : "bianco";
    }

    @Override
    public Board<PieceModel<Species>> getBoard() { return unModBoard; }

    /** Se il giocatore di turno non ha nessuna mossa valida il turno è
     * automaticamente passato all'altro giocatore. Ma se anche l'altro giuocatore
     * non ha mosse valide, la partita termina. */
    @Override
    public int turn() { return gResult == -1 ? currTurn : 0; }

    /** Se la mossa non è valida termina il gioco dando la vittoria all'altro
     * giocatore. */
    @Override
    public boolean move(Move<PieceModel<Species>> m) {
        Objects.requireNonNull(m);
        if (gResult != -1) throw new IllegalStateException();
        if (!validMoves().contains(m)) {        // Mossa non valida, vince l'altro giocatore
            gResult = 3 - currTurn;    // Indice dell'altro giocatore
            return false;
        }
        history.add(new TurnMove(currTurn, m));
        if (Move.Kind.RESIGN.equals(m.kind)) {  // Mossa di abbandono del gioco, vince l'altro giocatore
            gResult = 3 - currTurn;    // Indice dell'altro giocatore
            return true;
        }
        Action<PieceModel<Species>> add = m.actions.get(0), swap = m.actions.get(1);
        board.put(add.piece, add.pos.get(0));    // Esegue la mossa: aggiunge il disco e
        for (Pos p : swap.pos)                   // rovescia i dischi dell'altro giocatore
            board.put(swap.piece, p);
        currTurn = 3 - currTurn;      // Il turno passa all'altro giocatore
        if (validMoves().size() == 1) {    // Se non ha mosse valide (eccetto l'abbandono),
            currTurn = 3 - currTurn;       // il turno ripassa al giocatore
            if (validMoves().size() == 1) {               // Se neanche questo ha mosse valide,
                double sc1 = score(1), sc2 = score(2);    // la partita termina
                gResult = (sc1 > sc2 ? 1 : (sc2 > sc1 ? 2 : 0));
            }
        }
        return true;
    }

    @Override
    public boolean unMove() {
        if (history.isEmpty()) return false;
        TurnMove tm = history.remove(history.size()-1);
        Move<PieceModel<Species>> m = tm.move;     // L'ultima mossa
        if (!Move.Kind.RESIGN.equals(m.kind)) {    // Se non è l'abbandono,
            Action<PieceModel<Species>> add = m.actions.get(0), swap = m.actions.get(1);
            board.remove(add.pos.get(0));          // Fa l'undo della mossa
            PieceModel<Species> disc = (3 - tm.turn) == 1 ? NERO : BIANCO;
            for (Pos p : swap.pos)                 // rovescia i dischi
                board.put(disc, p);
        }
        gResult = -1;
        currTurn = tm.turn;
        return true;
    }

    @Override
    public boolean isPlaying(int i) {
        if (i < 1 || i > 2) throw new IllegalArgumentException();
        return gResult == -1;
    }

    @Override
    public int result() { return gResult; }

    /** Ogni mossa, eccetto l'abbandono, è rappresentata da una {@link Action} di tipo
     * {@link Action.Kind#ADD} seguita da una {@link Action} di tipo
     * {@link Action.Kind#SWAP}. */
    @Override
    public Set<Move<PieceModel<Species>>> validMoves() {
        if (gResult != -1)
            throw new IllegalStateException();
        Set<Move<PieceModel<Species>>> vm = validMoves(board::get, currTurn);
        vm.add(new Move<>(Move.Kind.RESIGN));  // È sempre possibile abbandonare il gioco
        return Collections.unmodifiableSet(vm);
    }

    @Override
    public double score(int i) {
        if (i < 1 || i > 2) throw new IllegalArgumentException();
        PieceModel<Species> disc = i == 1 ? NERO : BIANCO;
        int sc = 0;
        for (Pos p : board.positions())
            if (disc.equals(board.get(p))) sc++;
        return sc;
    }

    @Override
    public GameRuler<PieceModel<Species>> copy() {
        return new G_rs_OTH(this);    // Ritorna una copia di questo oggetto
    }

    @Override
    public Mechanics<PieceModel<Species>> mechanics() { return gM; }



    private static class Disp {
        private static final EnumMap<Dir, Disp> OCT = new EnumMap<>(Board.Dir.class);

        static final Function<Dir, Disp> toOctDisp = OCT::get;

        final int db, dt;
        Disp(int db, int dt) { this.db = db; this.dt = dt; }

        static {
            OCT.put(Board.Dir.UP, new Disp(0,1));
            OCT.put(Board.Dir.UP_R, new Disp(1,1));
            OCT.put(Board.Dir.RIGHT, new Disp(1,0));
            OCT.put(Board.Dir.DOWN_R, new Disp(1,-1));
            OCT.put(Board.Dir.DOWN, new Disp(0,-1));
            OCT.put(Board.Dir.DOWN_L, new Disp(-1,-1));
            OCT.put(Board.Dir.LEFT, new Disp(-1,0));
            OCT.put(Board.Dir.UP_L, new Disp(-1,1));
        }
    }

    private static class TurnMove {
        final int turn;
        final Move<PieceModel<Species>> move;

        TurnMove(int t, Move<PieceModel<Species>> m) {
            turn = t;
            move = m;
        }
    }

    private static String millisToSM(long millis) {
        if (millis <= 0) return "No limit";
        if (millis < 60_000)
            return (millis/1000)+"s";
        else
            return (millis/60_000)+"m";
    }

    private static final PieceModel<Species> NERO = new PieceModel<>(Species.DISC, "nero"),
            BIANCO = new PieceModel<>(Species.DISC, "bianco");
    private static final List<PieceModel<Species>> PIECES = Collections.unmodifiableList(Arrays.asList(NERO, BIANCO));



    /** Crea una copia (profonda) dell'oggetto Othello dato
     * @param o  un oggetto Othello */
    private G_rs_OTH(G_rs_OTH o) {
        name = o.name();
        int size = o.board.width();
        board = new BoardOct<>(size, size);
        for (Pos p : board.positions()) {             // Copia la disposizione dei dischi
            PieceModel<Species> pm = o.board.get(p);
            if (pm != null)
                board.put(pm, p);
        }
        unModBoard = Utils.UnmodifiableBoard(board);  // La view immodificabile della board
        playerNames = o.playerNames;     // Può essere condiviso perché immodificabile
        history = new ArrayList<>();     // Copia la history
        history.addAll(o.history);
        currTurn = o.currTurn;
        gResult = o.gResult;
        gM = new Mechanics<>(o.gM.time, PIECES, board.positions(), 2, start(), this::next);
    }

    private Situation<PieceModel<Species>> start() {
        Map<Pos,PieceModel<Species>> c = new HashMap<>();
        int i = (board.width() - 2)/2;
        c.put(new Pos(i, i+1), BIANCO);            // La configurazione iniziale
        c.put(new Pos(i+1, i), BIANCO);
        c.put(new Pos(i+1, i+1), NERO);
        c.put(new Pos(i, i), NERO);
        return new Situation<>(c, 1);
    }

    private Map<Move<PieceModel<Species>>,Situation<PieceModel<Species>>> next(Situation<PieceModel<Species>> s) {
        Objects.requireNonNull(s);
        Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> sMap = new HashMap<>();
        if (s.turn <= 0) return sMap;
        for (Move<PieceModel<Species>> m : validMoves(s::get, s.turn)) {
            Map<Pos, PieceModel<Species>> cNext = s.newMap();
            Action<PieceModel<Species>> add = m.actions.get(0), swap = m.actions.get(1);
            cNext.put(add.pos.get(0), add.piece);  // Esegue la mossa: aggiunge il disco e
            for (Pos p : swap.pos)                 // rovescia i dischi dell'altro giocatore
                cNext.put(p, swap.piece);
            int tNext = 3 - s.turn;         // Il turno passa all'altro giocatore
            if (validMoves(cNext::get, tNext).isEmpty()) {  // Se non ha mosse valide,
                tNext = 3 - tNext;                          // il turno ripassa al giocatore
                if (validMoves(cNext::get, tNext).isEmpty()) { // Se neanche questo ha mosse valide,
                    int sc1 = 0, sc2 = 0;                      // la partita termina
                    for (Pos p : board.positions()) {
                        if (NERO.equals(cNext.get(p))) sc1++;
                        else if (BIANCO.equals(cNext.get(p))) sc2++;
                    }
                    tNext = sc1 > sc2 ? -1 : (sc2 > sc1 ? -2 : 0);
                }
            }
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
        PieceModel<Species> curr = turn == 1 ? NERO : BIANCO;  // Disco del giocatore attuale
        PieceModel<Species> other = turn == 1 ? BIANCO : NERO; // Disco dell'altro giocatore
        for (Pos p : board.positions()) {        // Per ogni posizione della board
            if (c.apply(p) != null) continue;    // che non è occupata da un disco
            Set<Pos> toFlip = new HashSet<>();       // Insieme delle posizioni da rovesciare
            for (Dir d : Dir.values()) {      // Per ogni direzione d
                Disp disp = Disp.toOctDisp.apply(d);
                int db = disp.db, dt = disp.dt, count = 0;
                int b = p.b + db, t = p.t + dt;          // Prima posizione nella direzione d
                while (b >= 0 && t >= 0 && other.equals(c.apply(new Pos(b, t)))) {
                    count++;                  // Conta il numero di posizioni,
                    b += db;                  // nella direzione d, occupate da
                    t += dt;                  // dischi dell'altro giocatore
                }
                if (b >= 0 && t >= 0 && count > 0 && curr.equals(c.apply(new Pos(b, t)))) { // Se c'è una linea non vuota di
                    for (int i = 0 ; i < count ; i++) {                                     // dischi dell'altro giocatore
                        b -= db; t -= dt;                                                   // chiusa da un disco del giocatore,
                        toFlip.add(new Pos(b, t));                           // aggiungi le posizioni a
                    }                                                        // quelle da rovesciare
                }
            }
            if (!toFlip.isEmpty()) {            // Se ci sono posizioni da rovesciare, è una mossa valida
                vm.add(new Move<>(new Action<>(p,curr),
                        new Action<>(curr,toFlip.toArray(new Pos[toFlip.size()]))));
            }
        }
        return vm;
    }


    private final String name;
    private final Board<PieceModel<Species>> board;
    private final Board<PieceModel<Species>> unModBoard;
    private final List<String> playerNames;
    private final List<TurnMove> history;
    private final Mechanics<PieceModel<Species>> gM;
    private int currTurn;
    private int gResult;
}
