package gapp.ulg.games;

import gapp.ulg.game.board.*;
import gapp.ulg.game.util.BoardOct;
import gapp.ulg.game.util.Utils;

import java.util.*;
import java.util.function.Function;

import static gapp.ulg.game.board.PieceModel.Species;


class G_rs_MNK implements GameRuler<PieceModel<Species>> {
    /** Crea un {@code MNKgame} con le impostazioni date.
     * @param time  tempo in millisecondi per fare una mossa, se <= 0 significa nessun
     *              limite
     * @param m  larghezza (width) della board
     * @param n  altezza (height) della board
     * @param k  lunghezza della linea
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se {@code p1} o {@code p2} è null
     * @throws IllegalArgumentException se i valori di {@code m,n,k} non soddisfano
     * le condizioni 1 <= {@code k} <= max{{@code M,N}} <= 20 e 1 <= min{{@code M,N}} */
    G_rs_MNK(long time, int m, int n, int k, String p1, String p2) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);
        /// 1 <= K <= max{M,N} <= 20   AND   1 <= min{M,N}
        if (!(k >= 1 && k <= Math.max(m,n) && Math.max(m,n) <= 20 && Math.min(m,n) >= 1))
            throw new IllegalArgumentException();
        M = m;
        N = n;
        K = k;
        board = new BoardOct<>(M, N);                 // La board
        unModBoard = Utils.UnmodifiableBoard(board);  // La view immodificabile della board
        playerNames = Collections.unmodifiableList(Arrays.asList(p1, p2));
        history = new ArrayList<>();
        gResult = -1;
        currTurn = 1;
        gM = new Mechanics<>((time > 0 ? time : -1), PIECES, board.positions(), 2, START, this::next);
    }

    /** Il nome rispetta il formato:
     * <pre>
     *     <i>M,N,K</i>-game
     * </pre>
     * dove <code><i>M,N,K</i></code> sono i valori dei parametri M,N,K, ad es.
     * "4,5,4-game". */
    @Override
    public String name() { return String.format("%d,%d,%d-game",M,N,K); }

    @Override
    public <T> T getParam(String name, Class<T> c) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(c);
        Integer p;
        switch (name) {
            case "Time": return c.cast(millisToSM(gM.time));
            case "M": p = M; break;
            case "N": p = N; break;
            case "K": p = K; break;
            default: throw new IllegalArgumentException();
        }
        return c.cast(p);
    }

    @Override
    public List<String> players() { return playerNames; }

    /** @return il colore "nero" per il primo giocatore e "bianco" per il secondo */
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
     * giocatore.
     * Se dopo la mossa la situazione è tale che nessuno dei due giocatori può
     * vincere, si tratta quindi di una situazione che può portare solamente a una
     * patta, termina immediatamente il gioco con una patta. Per determinare se si
     * trova in una tale situazione controlla che nessun dei due giocatori può
     * produrre una linea di K pedine con le mosse rimanenti (in qualsiasi modo siano
     * disposte le pedine rimanenti di entrambi i giocatori). */
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
        Action<PieceModel<Species>> add = m.actions.get(0);
        board.put(add.piece, add.pos.get(0));    // Esegue la mossa: aggiunge il disco
        currTurn = 3 - currTurn;                 // Il turno passa all'altro giocatore
        switch (check(board::get, currTurn)) {
            case NERO_VINCE: gResult = 1; break;
            case BIANCO_VINCE: gResult = 2; break;
            case PATTA: gResult = 0; break;
            case PATTA_OBBLIGATA: gResult = 0; break;
            case NERO_NONVINCE: case BIANCO_NONVINCE: case INDECISA: break;
        }
        return true;
    }

    @Override
    public boolean unMove() {
        if (history.isEmpty()) return false;
        TurnMove tm = history.remove(history.size()-1);
        Move<PieceModel<Species>> m = tm.move;     // L'ultima mossa
        if (!Move.Kind.RESIGN.equals(m.kind)) {    // Se non è l'abbandono,
            Action<PieceModel<Species>> add = m.actions.get(0);
            board.remove(add.pos.get(0));          // Fa l'undo della mossa
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

    /** Ogni mossa (diversa dall'abbandono) è rappresentata da una sola {@link Action}
     * di tipo {@link Action.Kind#ADD}. */
    @Override
    public Set<Move<PieceModel<Species>>> validMoves() {
        if (gResult != -1) throw new IllegalStateException();
        Set<Move<PieceModel<Species>>> vm = validMoves(board::get, currTurn);
        vm.add(new Move<>(Move.Kind.RESIGN));   // È sempre possibile abbandonare il gioco
        return Collections.unmodifiableSet(vm);
    }

    @Override
    public GameRuler<PieceModel<Species>> copy() {
        return new G_rs_MNK(this);       // Ritorna una copia di questo oggetto
    }

    @Override
    public Mechanics<PieceModel<Species>> mechanics() { return gM; }



    private static class Disp {
        private static final EnumMap<Board.Dir, Disp> OCT = new EnumMap<>(Board.Dir.class);

        static final Function<Board.Dir, Disp> toOctDisp = OCT::get;

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

    private enum CValue {
        NERO_VINCE,
        BIANCO_VINCE,
        PATTA,
        PATTA_OBBLIGATA,
        NERO_NONVINCE,
        BIANCO_NONVINCE,
        INDECISA
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
    private static final Situation<PieceModel<Species>> START = new Situation<>(new HashMap<>(), 1);



    /** Crea una copia (profonda) dell'oggetto MNKgame dato
     * @param o  un oggetto MNKgame */
    private G_rs_MNK(G_rs_MNK o) {
        M = o.M;
        N = o.N;
        K = o.K;
        board = new BoardOct<>(M, N);                 // La board
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
        gM = new Mechanics<>(o.gM.time, PIECES, board.positions(), 2, START, this::next);
    }

    private Map<Move<PieceModel<Species>>,Situation<PieceModel<Species>>> next(Situation<PieceModel<Species>> s) {
        Objects.requireNonNull(s);
        Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> sMap = new HashMap<>();
        if (s.turn <= 0) return sMap;
        for (Move<PieceModel<Species>> m : validMoves(s::get, s.turn)) {
            Map<Pos, PieceModel<Species>> cNext = s.newMap();
            Action<PieceModel<Species>> add = m.actions.get(0);
            cNext.put(add.pos.get(0), add.piece);  // Esegue la mossa: aggiunge il disco
            int tNext = 3 - s.turn;         // Il turno passa all'altro giocatore
            switch (check(cNext::get, tNext)) {
                case NERO_VINCE: tNext = -1; break;
                case BIANCO_VINCE: tNext = -2; break;
                case PATTA: tNext = 0; break;
                case PATTA_OBBLIGATA: tNext = 0; break;
                case NERO_NONVINCE: case BIANCO_NONVINCE: case INDECISA: break;
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
        for (Pos p : board.positions()) {        // Per ogni posizione della board
            if (c.apply(p) != null) continue;    // che non è occupata da un disco
            vm.add(new Move<>(new Action<>(p, curr)));
        }
        return vm;
    }

    private CValue check(Function<Pos,PieceModel<Species>> c, int turn) {
        int[] min = {-1,Integer.MAX_VALUE,Integer.MAX_VALUE};
        int empty = 0;
        for (Pos p : board.positions()) {        // Per ogni posizione della board
            if (c.apply(p) == null) empty++;
            for (Board.Dir d : Board.Dir.values()) {         // Per ogni direzione d
                Disp disp = Disp.toOctDisp.apply(d);
                int[] count = {0,0,0};
                int db = disp.db, dt = disp.dt, len = K;
                int b = p.b, t = p.t;             // Prima posizione della fila
                while (b >= 0 && b < M && t >= 0 && t < N && len > 0) {
                    count[PIECES.indexOf(c.apply(new Pos(b, t)))+1]++;
                    len--;
                    b += db;
                    t += dt;
                }
                if (len == 0) {                  // Se la fila ha lunghezza K
                    for (int i = 1 ; i <= 2 ; i++)
                        if (count[3-i] == 0 && min[i] > count[0])
                            min[i] = count[0];
                }
            }
        }
        if (min[1] == 0) return CValue.NERO_VINCE;
        else if (min[2] == 0) return CValue.BIANCO_VINCE;
        else if (empty == 0) return CValue.PATTA;
        else {
            int[] mm = new int[3];
            mm[3-turn] = empty/2;           // Numero mosse del giocatore non di turno
            mm[turn] = empty - mm[3-turn];  // Numero mosse del giocatore di turno
            if (min[1] > mm[1] && min[2] > mm[2]) return CValue.PATTA_OBBLIGATA;
            else if (min[1] <= mm[1] && min[2] <= mm[2]) return CValue.INDECISA;
            else return min[1] > mm[1] ? CValue.NERO_NONVINCE : CValue.BIANCO_NONVINCE;
        }
    }


    private final int M,N,K;
    private final Board<PieceModel<Species>> board;
    private final Board<PieceModel<Species>> unModBoard;
    private final List<String> playerNames;
    private final List<TurnMove> history;
    private final Mechanics<PieceModel<Species>> gM;
    private int currTurn;
    private int gResult;
}
