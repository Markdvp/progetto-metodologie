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


public class G_rs_CAM implements GameRuler<PieceModel<Species>> {
    public G_rs_CAM(long time, String p1, String p2) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);
        board = newBoard();                           // La board
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
    public String name() { return "Camelot"; }

    @Override
    public <T> T getParam(String name, Class<T> c) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(c);
        String p;
        switch (name) {
            case "Time": p = millisToSM(gM.time); break;
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
        if (gResult == -1) {
            Set<Move<PieceModel<Species>>> vm = validMoves(board::get, currTurn);
            if (vm.isEmpty()) gResult = 3 - currTurn;
        }
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
    public GameRuler<PieceModel<Species>> copy() { return new G_rs_CAM(this); }

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

    private static final PieceModel<Species> P_NERO = new PieceModel<>(Species.PAWN, "nero"),
            K_NERO = new PieceModel<>(Species.KNIGHT, "nero"),
            P_BIANCO = new PieceModel<>(Species.PAWN, "bianco"),
            K_BIANCO = new PieceModel<>(Species.KNIGHT, "bianco");
    private static final List<PieceModel<Species>> PIECES = Collections.unmodifiableList(Arrays.asList(P_NERO, K_NERO, P_BIANCO, K_BIANCO));
    private static final List<List<Pos>> CASTLES = Arrays.asList(Arrays.asList(new Pos(5,15), new Pos(6,15)),
            Arrays.asList(new Pos(5,0), new Pos(6,0)));


    /** Crea una copia (profonda) dell'oggetto Camelot dato
     * @param o  un oggetto Camelot */
    private G_rs_CAM(G_rs_CAM o) {
        board = newBoard();
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

    private Board<PieceModel<Species>> newBoard() {
        Function<int[], List<Pos>> toP = cc -> {
            List<Pos> pp = new ArrayList<>();
            for (int i = 0; i < cc.length; i += 2)
                pp.add(new Pos(cc[i], cc[i + 1]));
            return pp;
        };
        int[] exc = {0,0, 1,0, 2,0, 3,0, 4,0, 7,0, 8,0, 9,0, 10,0, 11,0, 0,1, 1,1, 10,1, 11,1, 0,2, 11,2,
                0,15, 1,15, 2,15, 3,15, 4,15, 7,15, 8,15, 9,15, 10,15, 11,15, 0,14, 1,14, 10,14, 11,14, 0,13, 11,13};
        return new BoardOct<>(12, 16, toP.apply(exc));
    }

    private void start(BiConsumer<Pos,PieceModel<Species>> put) {
        for (int b = 3 ; b <= 8 ; b++) put.accept(new Pos(b, 10), P_NERO);
        for (int b = 4 ; b <= 7 ; b++) put.accept(new Pos(b, 9), P_NERO);
        for (int b = 3 ; b <= 8 ; b++) put.accept(new Pos(b, 5), P_BIANCO);
        for (int b = 4 ; b <= 7 ; b++) put.accept(new Pos(b, 6), P_BIANCO);
        put.accept(new Pos(2, 10), K_NERO); put.accept(new Pos(9, 10), K_NERO);
        put.accept(new Pos(3, 9), K_NERO); put.accept(new Pos(8, 9), K_NERO);
        put.accept(new Pos(2, 5), K_BIANCO); put.accept(new Pos(9, 5), K_BIANCO);
        put.accept(new Pos(3, 6), K_BIANCO); put.accept(new Pos(8, 6), K_BIANCO);
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
        String color = (turn == 1 ? P_NERO : P_BIANCO).color;
        List<Pos> ownCastle = CASTLES.get(turn-1), oppCastle = CASTLES.get(2-turn);
        boolean castle = false;
        List<Pos> pp = new ArrayList<>();
        for (Pos p : board.positions()) {
            PieceModel<Species> pm = c.apply(p);
            if (pm == null || !pm.color.equals(color)) continue;
            if (ownCastle.contains(p)) {
                pp.retainAll(Collections.singletonList(p));
                castle = true;
                break;
            }
            if (!oppCastle.contains(p)) pp.add(p);
        }
        for (Pos p : pp)
            vm.addAll(mJump(c, p));
        if (vm.isEmpty()) {    // cannot jump
            for (Pos p : pp) {
                vm.addAll(mCanter(c, turn, p));
                vm.addAll(mPlain(c, turn, p));
            }
            if (!castle)
                for (int i = 0 ; i < 2 ; i++) {
                    PieceModel<Species> pm = c.apply(oppCastle.get(i));
                    if (pm != null && pm.color.equals(color) && c.apply(oppCastle.get(1-i)) == null)
                        vm.add(new Move<>(new Action<>((i == 0 ? Dir.RIGHT : Dir.LEFT), 1, oppCastle.get(i))));
                }
        }
        for (Pos p : pp) vm.addAll(mKnightCharge(c, p));
        return vm;
    }


    /** A Knight (only) may combine a Canter and a Jump in a single move, called a
     * Knight's Charge. A Knight's Charge must follow the order of first the Canter(s)
     * and last the Jump(s). A Knight is never obliged to make a Knight's Charge. When
     * cantering over more than one piece during the cantering portion of a Knight's
     * Charge, the direction of the move may be varied after each Canter. If the
     * Canter of a Knight brings it next to an enemy piece that can be jumped, it
     * must do so, unless by a different route later in that same move it captures
     * one or more enemy pieces elsewhere. During a Knight's Charge, the directions
     * of the last Canter and first Jump need not be the same. Having jumped over one
     * enemy piece during the jumping portion of a Knight's Charge, the jumping must
     * continue as a part of that same move if the player's Knight reaches a square
     * next to another exposed enemy piece. When jumping over more than one piece
     * during the jumping portion of a Knight's Charge, the direction of the move
     * may be varied after each Jump.
     * @param c  configuration
     * @param start  position
     * @return list of knight-charge moves */
    private List<Move<PieceModel<Species>>> mKnightCharge(Function<Pos,PieceModel<Species>> c, Pos start) {
        class KC {
            private final List<Pos> canter, jump;
            private KC(List<Pos> c, List<Pos> j) { canter = c; jump = j; }
        }
        List<Move<PieceModel<Species>>> mm = new ArrayList<>();
        if (c.apply(start).species != Species.KNIGHT) return mm;
        String color = c.apply(start).color;
        Queue<List<Pos>> q = new ArrayDeque<>();
        q.offer(new ArrayList<>(Collections.singletonList(start)));
        Queue<KC> qq = new ArrayDeque<>();
        while (!q.isEmpty()) {
            List<Pos> can = q.poll();
            Pos p = can.get(can.size()-1);
            for (Dir d : Dir.values()) {      // Per ogni direzione d
                Pos pp = board.adjacent(p, d);
                if (pp == null) continue;
                PieceModel<Species> pm = c.apply(pp);
                if (pm == null) continue;
                Pos ppp = board.adjacent(pp, d);
                if (ppp == null || c.apply(ppp) != null || can.contains(ppp)) continue;
                if (pm.color.equals(color)) {    // Canter
                    List<Pos> can2 = new ArrayList<>(can);
                    can2.add(ppp);
                    q.offer(can2);
                } else if (can.size() > 1) {      // Jump
                    List<Pos> jump = new ArrayList<>(Arrays.asList(p,pp,ppp));
                    qq.offer(new KC(can, jump));
                }
            }
        }
        while (!qq.isEmpty()) {
            KC charge = qq.poll();
            List<Pos> jump = charge.jump, can = charge.canter;
            boolean jumping = false;
            Pos p = jump.get(jump.size()-1);
            for (Dir d : Dir.values()) {      // Per ogni direzione d
                Pos pp = board.adjacent(p, d);
                if (pp == null) continue;
                PieceModel<Species> pm = c.apply(pp);
                if (pm == null || pm.color.equals(color)) continue;
                Pos ppp = board.adjacent(pp, d);
                if (ppp == null || c.apply(ppp) != null || jump.contains(pp)) continue;
                List<Pos> jump2 = new ArrayList<>(jump);
                jump2.add(pp);
                jump2.add(ppp);
                qq.offer(new KC(can, jump2));
                jumping = true;
            }
            if (!jumping) {
                List<Action<PieceModel<Species>>> aa = new ArrayList<>();
                for (int i = 0 ; i < can.size()-1 ; i++)
                    aa.add(new Action<>(can.get(i), can.get(i+1)));
                for (int i = 0 ; i < jump.size()-2 ; i+=2) {
                    aa.add(new Action<>(jump.get(i), jump.get(i+2)));
                    aa.add(new Action<>(jump.get(i+1)));
                }
                mm.add(new Move<>(aa));
            }
        }
        return mm;
    }

    /** A piece (either Knight or Man) may leap in any direction (horizontally,
     * vertically, or diagonally) over an opposing piece (either Knight or Man) that
     * occupies an adjoining square, provided there is an unoccupied square
     * immediately beyond it in a direct line onto which the leap may be made. This
     * move is called a Jump. Each enemy piece jumped over is captured and immediately
     * removed from the board. A player is obliged to jump if any one of his pieces
     * is next to an exposed enemy piece. Having jumped over one enemy piece, the
     * jumping must continue as a part of that same move if the player's piece reaches
     * a square next to another exposed enemy piece. When jumping over more than one
     * piece, the direction of the move may be varied after each Jump. If presented
     * with capturing alternatives, a player may choose which opposing piece to
     * capture, and with which of his pieces to effect the capture. When compelled
     * to jump, a player may, if he can, capture by a Knight's Charge instead. The
     * only situation in which a player may ignore his obligation to jump is when,
     * on his previous move, he has jumped one of his pieces over an opponent's piece
     * into his own castle, ending his turn there, and must, on his next turn,
     * immediately move that piece out from his castle.
     * @param c  configuration
     * @param start  position
     * @return list of jump moves */
    private List<Move<PieceModel<Species>>> mJump(Function<Pos,PieceModel<Species>> c, Pos start) {
        List<Move<PieceModel<Species>>> mm = new ArrayList<>();
        String color = c.apply(start).color;
        Queue<List<Pos>> q = new ArrayDeque<>();
        q.offer(new ArrayList<>(Collections.singletonList(start)));
        while (!q.isEmpty()) {
            List<Pos> jump = q.poll();
            boolean jumping = false;
            Pos p = jump.get(jump.size()-1);
            for (Dir d : Dir.values()) {      // Per ogni direzione d
                Pos pp = board.adjacent(p, d);
                if (pp == null) continue;
                PieceModel<Species> pm = c.apply(pp);
                if (pm == null || pm.color.equals(color)) continue;
                Pos ppp = board.adjacent(pp, d);
                if (ppp == null || c.apply(ppp) != null || jump.contains(pp)) continue;
                List<Pos> jump2 = new ArrayList<>(jump);
                jump2.add(pp);
                jump2.add(ppp);
                q.offer(jump2);
                jumping = true;
            }
            if (!jumping && jump.size() >= 3) {
                List<Action<PieceModel<Species>>> aa = new ArrayList<>();
                for (int i = 0 ; i < jump.size()-2 ; i+=2) {
                    aa.add(new Action<>(jump.get(i), jump.get(i+2)));
                    aa.add(new Action<>(jump.get(i+1)));
                }
                mm.add(new Move<>(aa));
            }
        }
        return mm;
    }

    private boolean canJump(Function<Pos,PieceModel<Species>> c, Pos p, String color) {
        for (Dir d : Dir.values()) {      // Per ogni direzione d
            Pos pp = board.adjacent(p, d);
            if (pp == null) continue;
            PieceModel<Species> pm = c.apply(pp);
            if (pm == null || pm.color.equals(color)) continue;
            Pos ppp = board.adjacent(pp, d);
            if (ppp == null || c.apply(ppp) != null) continue;
            return true;
        }
        return false;
    }

    /** A piece (either Knight or Man) may leap in any direction (horizontally,
     * vertically, or diagonally) over a friendly piece (either Knight or Man) that
     * occupies an adjoining square, provided that there is an unoccupied square
     * immediately beyond it in a direct line onto which the leap may be made. This
     * move is called a Canter. Pieces cantered over are not removed from the board.
     * A player may canter over more than one piece during the same move, but may not
     * make a Canter that ends on the same square from which it began. When cantering
     * over more than one piece in a move, the direction of the move may be varied
     * after each Canter. A player is never compelled to canter, nor when cantering
     * is he compelled to canter as far as possible.
     * @param c  configuration
     * @param turn  turn index
     * @param start  position
     * @return list of canter moves */
    private List<Move<PieceModel<Species>>> mCanter(Function<Pos,PieceModel<Species>> c, int turn, Pos start) {
        List<Move<PieceModel<Species>>> mm = new ArrayList<>();
        String color = c.apply(start).color;
        boolean isK = c.apply(start).species == Species.KNIGHT;
        Queue<List<Pos>> q = new ArrayDeque<>();
        q.offer(new ArrayList<>(Collections.singletonList(start)));
        while (!q.isEmpty()) {
            List<Pos> can = q.poll();
            Pos p = can.get(can.size()-1);
            for (Dir d : Dir.values()) {      // Per ogni direzione d
                Pos pp = board.adjacent(p, d);
                if (pp == null) continue;
                PieceModel<Species> pm = c.apply(pp);
                if (pm == null || !pm.color.equals(color)) continue;
                Pos ppp = board.adjacent(pp, d);
                if (ppp == null || c.apply(ppp) != null || can.contains(ppp)) continue;
                if (isK && canJump(c, ppp, color)) continue;
                List<Pos> can2 = new ArrayList<>(can);
                can2.add(ppp);
                q.offer(can2);
                if (!CASTLES.get(turn-1).contains(ppp)) {
                    List<Action<PieceModel<Species>>> aa = new ArrayList<>();
                    for (int i = 0 ; i < can2.size()-1 ; i++)
                        aa.add(new Action<>(can2.get(i), can2.get(i+1)));
                    mm.add(new Move<>(aa));
                }
            }
        }
        return mm;
    }

    /** A piece (either Knight or Man) may move one square in any direction
     * (horizontally, vertically, or diagonally) to any adjoining unoccupied square.
     * This move is called a Plain Move.
     * @param c  configuration
     * @param turn  turn index
     * @param start  position
     * @return list of plain moves */
    private List<Move<PieceModel<Species>>> mPlain(Function<Pos,PieceModel<Species>> c, int turn, Pos start) {
        List<Move<PieceModel<Species>>> mm = new ArrayList<>();
        for (Dir d : Dir.values()) {      // Per ogni direzione d
            Pos pp = board.adjacent(start, d);
            if (pp != null && !CASTLES.get(turn-1).contains(pp) && c.apply(pp) == null) {
                Move<PieceModel<Species>> m = new Move<>(new Action<>(d, 1, start));
                mm.add(m);
            }
        }
        return mm;
    }

    private int check(Function<Pos,PieceModel<Species>> c) {
        int[] count = {0,0};
        for (Pos p : board.positions()) {
            PieceModel<Species> pm = c.apply(p);
            if (pm == null) continue;
            count[pm.color.equals(P_NERO.color) ? 0 : 1]++;
        }
        if (count[0] == 0 && count[1] >= 2) return 2;
        if (count[1] == 0 && count[0] >= 2) return 1;
        if (count[0] <= 1 && count[1] <= 1) return 0;
        for (int i = 0 ; i < 2 ; i++) {
            String color = (i == 0 ? P_NERO : P_BIANCO).color;
            PieceModel<Species> pm1 = c.apply(CASTLES.get(i).get(0)), pm2 = c.apply(CASTLES.get(i).get(1));
            if (pm1 != null && pm2 != null && !pm1.color.equals(color) && !pm2.color.equals(color))
                return 2 - i;
        }
        return -1;
    }

    private static String millisToSM(long millis) {
        if (millis <= 0) return "No limit";
        return millis < 60_000 ? (millis/1000)+"s" : (millis/60_000)+"m";
    }


    private final Board<PieceModel<Species>> board;
    private final Board<PieceModel<Species>> unModBoard;
    private final List<String> playerNames;
    private final List<Situation<PieceModel<Species>>> history;
    private final Mechanics<PieceModel<Species>> gM;
    private int currTurn;
    private int gResult;
}
