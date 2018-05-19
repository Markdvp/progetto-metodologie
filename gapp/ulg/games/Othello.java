package gapp.ulg.games;

import gapp.ulg.game.board.*;
import gapp.ulg.game.util.BoardOct;
import gapp.ulg.game.util.Utils;

import static gapp.ulg.game.board.PieceModel.Species;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Un oggetto Othello rappresenta un GameRuler per fare una partita a Othello. Il
 * gioco Othello si gioca su una board di tipo {@link Board.System#OCTAGONAL} 8x8.
 * Si gioca con pezzi o pedine di specie {@link Species#DISC} di due
 * colori "nero" e "bianco". Prima di inziare a giocare si posizionano due pedine
 * bianche e due nere nelle quattro posizioni centrali della board in modo da creare
 * una configurazione a X. Quindi questa è la disposzione iniziale (. rappresenta
 * una posizione vuota, B una pedina bianca e N una nera):
 * <pre>
 *     . . . . . . . .
 *     . . . . . . . .
 *     . . . . . . . .
 *     . . . B N . . .
 *     . . . N B . . .
 *     . . . . . . . .
 *     . . . . . . . .
 *     . . . . . . . .
 * </pre>
 * Si muove alternativamente (inizia il nero) appoggiando una nuova pedina in una
 * posizione vuota in modo da imprigionare, tra la pedina che si sta giocando e
 * quelle del proprio colore già presenti sulla board, una o più pedine avversarie.
 * A questo punto le pedine imprigionate devono essere rovesciate (da bianche a nere
 * o viceversa, azione di tipo {@link Action.Kind#SWAP}) e diventano
 * di proprietà di chi ha eseguito la mossa. È possibile incastrare le pedine in
 * orizzontale, in verticale e in diagonale e, a ogni mossa, si possono girare
 * pedine in una o più direzioni. Sono ammesse solo le mosse con le quali si gira
 * almeno una pedina, se non è possibile farlo si salta il turno. Non è possibile
 * passare il turno se esiste almeno una mossa valida. Quando nessuno dei giocatori
 * ha la possibilità di muovere o quando la board è piena, si contano le pedine e si
 * assegna la vittoria a chi ne ha il maggior numero. Per ulteriori informazioni si
 * può consultare
 * <a href="https://it.wikipedia.org/wiki/Othello_(gioco)">Othello</a> */
public class Othello implements GameRuler<PieceModel<Species>> {
    /** Crea un GameRuler per fare una partita a Othello, equivalente a
     * {@link Othello#Othello(long, int, String, String) Othello(0,8,p1,p2)}.
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se p1 o p2 è null */
    public Othello(String p1, String p2) {

        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);

        //Inizializzo la board;
        board = new BoardOct<>(8,8);

        //Metto i pezzi di partenza;
        board.put(new PieceModel<>(PieceModel.Species.DISC, "bianco"), new Pos(3, 4));
        board.put(new PieceModel<>(PieceModel.Species.DISC, "bianco"), new Pos(4, 3));
        board.put(new PieceModel<>(PieceModel.Species.DISC, "nero"), new Pos(3, 3));
        board.put(new PieceModel<>(PieceModel.Species.DISC, "nero"), new Pos(4, 4));

        //Lista con i nomi dei giocatori...
        nomiGiocatori = new ArrayList<>();
        nomiGiocatori.add(p1);
        nomiGiocatori.add(p2);

        //...ed i loro colori;
        colorFirstplayer = "";
        colorSecondPlayer = "";

        //Inizializzo lo "stato" del gioco a -1;
        statoGioco = -1;

        //Il primo giocatore della lista inizia per primo;
        turn = 1;

        //Inizializzo la lista delle mosse finora effettuate;
        cronologiaMosse = new ArrayList<>();

        //Il tempo per fare una mossa!
        tempo = -1;

        //...e la grandezza della board!
        grandezzaBoard = 8;

    }

    /** Crea un GameRuler per fare una partita a Othello.
     * @param time  tempo in millisecondi per fare una mossa, se <= 0 significa nessun
     *              limite
     * @param size  dimensione della board, sono accettati solamente i valori 6,8,10,12
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se {@code p1} o {@code p2} è null
     * @throws IllegalArgumentException se size non è uno dei valori 6,8,10 o 12 */
    public Othello(long time, int size, String p1, String p2) {
        List<Integer> sizeListAcc = Arrays.asList(6,8,10,12);

        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);

        //Se il valore size non è accettabile;
        if (!sizeListAcc.contains(size))
            throw new IllegalArgumentException();

        //Inizializzo la board;
        board = new BoardOct<>(size, size);

        //Metto i pezzi di partenza;
        board.put(new PieceModel<>(PieceModel.Species.DISC, "bianco"), new Pos((size / 2) - 1, (size / 2)));
        board.put(new PieceModel<>(PieceModel.Species.DISC, "bianco"), new Pos((size / 2), (size / 2) - 1));
        board.put(new PieceModel<>(PieceModel.Species.DISC, "nero"), new Pos((size / 2) - 1, (size / 2) - 1));
        board.put(new PieceModel<>(PieceModel.Species.DISC, "nero"), new Pos((size / 2), (size / 2)));

        //Lista con i nomi dei giocatori...
        nomiGiocatori = new ArrayList<>();
        nomiGiocatori.add(p1);
        nomiGiocatori.add(p2);

        //...ed i loro colori;
        colorFirstplayer = "";
        colorSecondPlayer = "";

        //Inizializzo lo "stato" del gioco a -1;
        statoGioco = -1;

        //Il primo giocatore della lista inizia per primo;
        turn = 1;

        //Inizializzo la lista delle mosse finora effettuate;
        cronologiaMosse = new ArrayList<>();

        //Il tempo massimo per eseguire una mossa!
        tempo = time;

        //...e la grandezza della board!
        grandezzaBoard = size;

    }

    public BoardOct<PieceModel<Species>> board;
    public List<String> nomiGiocatori;
    public String colorFirstplayer;
    public String colorSecondPlayer;
    public int statoGioco;
    public int turn;
    public List<Move<PieceModel<PieceModel.Species>>> cronologiaMosse;
    public long tempo;
    public int grandezzaBoard;

    public static List<Board.Dir> listaDirezioni = Arrays.asList(Board.Dir.UP, Board.Dir.UP_R, Board.Dir.RIGHT, Board.Dir.DOWN_R, Board.Dir.DOWN, Board.Dir.DOWN_L, Board.Dir.LEFT, Board.Dir.UP_L);

    /** Il nome rispetta il formato:
     * <pre>
     *     Othello<i>Size</i>
     * </pre>
     * dove <code><i>Size</i></code> è la dimensione della board, ad es. "Othello8x8". */
    @Override
    public String name() {

        switch (board.height()) {
            case 6:
                return "Othello6x6";
            case 8:
                return "Othello8x8";
            case 10:
                return "Othello10x10";
            case 12:
                return "Othello12x12";
        }

        return null;
    }

    @Override
    public <T> T getParam(String name, Class<T> c) {

        Objects.requireNonNull(name);
        Objects.requireNonNull(c);

        //Se il nome del parametro non è 'Time' oppure 'Board';
        if (!Arrays.asList("Time", "Board").contains(name))
            throw new IllegalArgumentException();

        //Se la classe del valore non è String;
        if (c != String.class)
            throw new ClassCastException();

        if (name.equals("Time")) {

            if (tempo <= 0)
                return (T) "No limit";
            else if (tempo == 1000)
                return (T) "1s";
            else if (tempo == 2000)
                return (T) "2s";
            else if (tempo == 3000)
                return (T) "3s";
            else if (tempo == 5000)
                return (T) "5s";
            else if (tempo == 10000)
                return (T) "10s";
            else if (tempo == 20000)
                return (T) "20s";
            else if (tempo == 30000)
                return (T) "30s";
            else if (tempo == 60000)
                return (T) "1m";
            else if (tempo == 120000)
                return (T) "2m";
            else if (tempo == 300000)
                return (T) "5m";

        } else if (name.equals("Board")) {

           if (grandezzaBoard == 6)
               return (T) "6x6";
           else if (grandezzaBoard == 8)
               return (T) "8x8";
           else if (grandezzaBoard == 10)
               return (T) "10x10";
           else if (grandezzaBoard == 12)
               return (T) "12x12";

        }

        return null;
    }

    @Override
    public List<String> players() { return nomiGiocatori; }

    /** Assegna il colore "nero" al primo giocatore e "bianco" al secondo. */
    @Override
    public String color(String name) {
        String color;

        Objects.requireNonNull(name);

        if (!nomiGiocatori.contains(name)) {

            throw new IllegalArgumentException();

        }

        //Se è il nome del primo giocatore;
        if (name.equals(nomiGiocatori.get(0))) {

            //Assegno il nero;
            colorFirstplayer = "nero";
            color = "nero";

        } else {

            //Assegno il bianco;
            colorSecondPlayer = "bianco";
            color = "bianco";

        }

        return color;
    }

    @Override
    public Board<PieceModel<Species>> getBoard() {
        return Utils.UnmodifiableBoard(board);
    }

    /** Se il giocatore di turno non ha nessuna mossa valida il turno è
     * automaticamente passato all'altro giocatore. Ma se anche l'altro giuocatore
     * non ha mosse valide, la partita termina. */
    @Override
    public int turn() {

        //Se il gioco è terminato;
        if (statoGioco != -1)
            return 0;

        return turn;
    }

    /** Se la mossa non è valida termina il gioco dando la vittoria all'altro
     * giocatore. */
    @Override
    public boolean move(Move<PieceModel<Species>> m) {
        Boolean moveIsOk = false;
        Set<Move<PieceModel<Species>>> mosseValide = validMoves();

        Objects.requireNonNull(m);

        //Se il gioco è terminato;
        if (statoGioco != -1)
            throw new IllegalStateException();

        //Se m è una mossa valida per l'attuale situazione di gioco;
        else if (mosseValide.contains(m)) {
            moveIsOk = true;

            //Se m è una mossa Action;
            if (m.kind.equals(Move.Kind.ACTION)) {
                String colore = null;

                if (turn == 1)
                    colore = "nero";
                else if (turn == 2)
                    colore = "bianco";

                //Metto il pezzo nella casella vuota...
                board.put(new PieceModel<>(PieceModel.Species.DISC, colore), m.actions.get(0).pos.get(0));

                //...e per ogni posizione SWAP....
                for (Pos p : m.actions.get(1).pos)
                    board.put(new PieceModel<>(PieceModel.Species.DISC, colore), p);

                turn = 3 - turn;
                cronologiaMosse.add(m);

            }

            //Se invece è una mossa RESIGN;
            else if (m.kind.equals(Move.Kind.RESIGN))
                cronologiaMosse.add(m);

        }

        //Se invece m non è una mossa valida;
        else if (!mosseValide.contains(m)) {

            //Il giocatore perde, quindi pongo il turno uguale a 0, e passo la vittoria all'altro giocatore;
            statoGioco = 3 - turn;
            turn = 0;

        }

        return moveIsOk;
    }

    @Override
    public boolean unMove() {

        //Se la partita è appena iniziata;
        if (cronologiaMosse.size() == 0) return false;

        //Se l'ultima mossa è stata un ACTION:
        if (cronologiaMosse.get(cronologiaMosse.size() - 1).kind.equals(Move.Kind.ACTION)) {

            //Rimuovo il pezzo che avevo messo nel turno precedente;
            PieceModel<Species> pezzoTolto = board.remove(cronologiaMosse.get(cronologiaMosse.size() - 1).actions.get(0).pos.get(0));

            //e per ogni posizione SWAP....
            for (Pos p : cronologiaMosse.get(cronologiaMosse.size() - 1).actions.get(1).pos) {
                String colore = board.get(p).color;

                //Inverto le pedine!
                if (colore.equals("nero"))
                    board.put(new PieceModel<>(PieceModel.Species.DISC, "bianco"), p);

                else if (colore.equals("bianco"))
                    board.put(new PieceModel<>(PieceModel.Species.DISC, "nero"), p);

            }

            //Ripasso il turno all'altro giocatore!
            if (pezzoTolto.color.equals("nero")) turn = 1;
            else turn = 2;

            //Se finora c'è stata una sola mossa;
            if (cronologiaMosse.size() == 1)
                cronologiaMosse = new ArrayList<>();

            //Altrimenti..
            else if (cronologiaMosse.size() > 1)
                cronologiaMosse.remove(cronologiaMosse.size() - 1);

        }

        //Se invece è stata un RESIGN:
        else if (cronologiaMosse.get(cronologiaMosse.size() - 1).kind.equals(Move.Kind.RESIGN)) {

            //Ripasso il turno all'altro giocatore!
            turn =  2 - (cronologiaMosse.size() % 2);

            //e riporto lo stato del gioco a -1!
            statoGioco = -1;

            //Se finora c'è stata una sola mossa;
            if (cronologiaMosse.size() == 1)
                cronologiaMosse = new ArrayList<>();

            //Altrimenti..
            else if (cronologiaMosse.size() > 1)
                cronologiaMosse.remove(cronologiaMosse.size() - 1);

        }

        return true;
    }

    @Override
    public boolean isPlaying(int i) {

        //Se il gioco è terminato;
        if (statoGioco != -1)
            return false;

        //Se i non è l'indice di turnazione di uno dei giocatori;
        if (i <= 0 || i > 2)
            throw new IllegalArgumentException();

        return i > 0 && i < 3;
    }

    @Override
    public int result() {
        int statoDelGioco = statoGioco;
        statoGioco = -1;
        Boolean fineGioco = false;

        //Se l'ultima mossa è RESIGN;
        if (cronologiaMosse.size() > 1 && cronologiaMosse.get(cronologiaMosse.size() - 1).kind.equals(Move.Kind.RESIGN)) {
            statoGioco = 3 - turn;
            turn = 0;
            fineGioco = true;
        }

        //Se non ci sono più mosse possibili;
        else if (validMoves().size() == 1) {

            //Trovo i punteggi;
            double punteggio1 = score(1), punteggio2 = score(2);

            if (punteggio1 > punteggio2) {

                statoGioco = 1;
                turn = 0;

            } else if (punteggio1 < punteggio2) {

                statoGioco = 2;
                turn = 0;

            } else {

                statoGioco = 0;
                turn = 0;

            }

            fineGioco = true;
        }

        if (!fineGioco)
            statoGioco = statoDelGioco;

        return statoGioco;
    }

    /** Ogni mossa, eccetto l'abbandono, è rappresentata da una {@link Action} di tipo
     * {@link Action.Kind#ADD} seguita da una {@link Action} di tipo
     * {@link Action.Kind#SWAP}. */
    @Override
    public Set<Move<PieceModel<Species>>> validMoves() {
        Set<Move<PieceModel<PieceModel.Species>>> insieme = new HashSet<>();
        insieme.add(new Move<>(Move.Kind.RESIGN));
        String player;

        //Associo il colore in base al turno;
        if (turn == 1) player = "nero";
        else player = "bianco";

        //Se il gioco è terminato;
        if (statoGioco != -1) throw new IllegalStateException();

        //Per ogni riga della board;
        for (Pos p : board.positions()) {

            //Se quella casella non è vuota, la ignoro;
                if (board.get(p) != null) continue;

            /*Inizializzo la lista delle possibili posizioni
              per lo SWAP!*/
            List<Pos> swapPositions = new ArrayList<>();

            //Lista per catturare le pos;
            List<Pos> swapTemp;

            //Per ogni direzione;
            for (Board.Dir direz : listaDirezioni) {
                swapTemp = swapPositionDIR(p, direz, turn);

                //Se ci sono pedine 'convertibili';
                if (swapTemp != null)
                    swapPositions.addAll(swapTemp);

            }

            //Se c'è almeno una posizione accettabile per lo SWAP;
            if (swapPositions.size() > 0) {

                //Aggiungo la mossa all'insieme!
                insieme.add(new Move<>(new Action<>(p, new PieceModel<>(PieceModel.Species.DISC, player)),
                        new Action<>(new PieceModel<>(PieceModel.Species.DISC, player), swapPositions.toArray(new Pos[swapPositions.size()]))));

            }

        }

        return insieme;
    }

    /**Metodi per la 'leggibilità' del validmoves(), e metodi minori*/
    public List<Pos> swapPositionDIR(Pos posizione, Board.Dir direzione, int turno) {
        List<Pos> listaPosSwapUP = new ArrayList<>();
        String colore1 = null, colore2 = null;

        if (turn == 1) {colore1 = "nero"; colore2 = "bianco";}
        else if (turn == 2) {colore1 = "bianco";colore2 = "nero";}

        while (true) {
            posizione = board.adjacent(posizione, direzione);

            //Se p1 non è una casella della board;
            if (posizione == null) return null;

            //Se trovo una pedina bianca in alto;
            if (board.get(posizione) != null && board.get(posizione).color.equals(colore2))
                listaPosSwapUP.add(posizione);

            //Se invece trovo una pedina nera in alto;
            else if (board.get(posizione) != null && board.get(posizione).color.equals(colore1)) break;

            //Se invece trovo una casella vuota;
            else if (board.get(posizione) == null) return null;

        }

        return listaPosSwapUP;
    }

    @Override
    public double score(int i) {
        int punteggio = 0;
        String colore = null;

        if (i == 1)
            colore = "nero";
        else if (i == 2)
            colore = "bianco";

        for (Pos p : board.positions()) {
            PieceModel<PieceModel.Species> pedina = board.get(p);

            if (pedina != null && pedina.color.equals(colore))
                punteggio++;
        }

        return punteggio;
    }

    @Override
    public GameRuler<PieceModel<Species>> copy() {

        //Inizializzo il gameruler;
        Othello partita = new Othello(tempo, board.height(), nomiGiocatori.get(0), nomiGiocatori.get(1));

        //Copio la disposizione dei pezzi;
        for (Map.Entry<Pos,PieceModel<Species>> e : board.mappaPosToPiece.entrySet()) {
            Pos p = e.getKey();
            PieceModel<PieceModel.Species> pm = e.getValue();

            if (pm == null)
                continue;

            partita.board.put(pm, p);
        }

        //Copio i loro colori;
        partita.colorFirstplayer = colorFirstplayer;
        partita.colorSecondPlayer = colorSecondPlayer;

        //Copio lo stato del gioco;
        partita.statoGioco = statoGioco;

        //Copio il turno;
        partita.turn = turn;

        //...e la cronologia!
        for (Move<PieceModel<PieceModel.Species>> mossa : cronologiaMosse)
            partita.cronologiaMosse.add(mossa);

        return partita;
    }

    /**Copia 'leggera' per il mechanics*/
    public GameRuler<PieceModel<Species>> lightCopy() {

        //Inizializzo il gameruler;
        Othello partita = new Othello(tempo, board.height(), nomiGiocatori.get(0), nomiGiocatori.get(1));

        //Copio la disposizione dei pezzi;
        for (Map.Entry<Pos,PieceModel<Species>> e : board.mappaPosToPiece.entrySet()) {
            PieceModel<PieceModel.Species> pm = e.getValue();

            if (pm == null)
                continue;

            partita.board.put(pm, e.getKey());
        }

        //Copio lo stato del gioco;
        partita.statoGioco = statoGioco;

        //Copio il turno;
        partita.turn = turn;

        return partita;
    }

    @Override
    public Mechanics<PieceModel<Species>> mechanics() {
        Map<Pos,PieceModel<Species>> mappa = new HashMap<>();

        //Creo una 'falsa' partita per crearmi il Situation;
        Othello pseudoOthello = new Othello(tempo, grandezzaBoard, "Andrea", "Gattuso");

        //Per ogni posizione della board, associo quella posizione
        // al pezzo che contiene;
        pseudoOthello.board.positions().stream().filter(p -> pseudoOthello.board.get(p) != null).forEach(p -> mappa.put(p, pseudoOthello.board.get(p)));

        Next<PieceModel<Species>> nextFunc = (s) -> {
            Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> mappona = new HashMap<>();

            Objects.requireNonNull(s);

            if (s.turn <= 0) return mappona;

            //Creo una 'falsa' partita per crearmi il Situation nel Next;
            Othello pseudoOthelloNext = (Othello) lightCopy();
            pseudoOthelloNext.turn = s.turn;

            //Porto la board alla Situation che mi interessa;
            for (Pos p : pseudoOthelloNext.board.positions()) {
                PieceModel<Species> pezzo = s.get(p);

                if (pezzo != null)
                    pseudoOthelloNext.board.put(pezzo, p);
            }

            //Creo l'insieme delle mosse valide;
            Set<Move<PieceModel<Species>>> mosseValide = pseudoOthelloNext.validMoves();

            ExecutorService exec = Executors.newFixedThreadPool(4);
            List<Future<Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>>>> futures = new ArrayList<>();

            //Per ogni mossa possibile da questa situazione di gioco:
            for (Move<PieceModel<Species>> mossa : mosseValide)
                futures.add(exec.submit(() -> trovaParteMappaNextOthello(mossa, pseudoOthelloNext, s)));

            for (Future<Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>>> f : futures) {
                Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> p = null;
                try { p = f.get(); } catch (InterruptedException | ExecutionException e) { exec.shutdown(); }

                if (p != null)
                    mappona.putAll(p);

            }

            exec.shutdownNow();
            return mappona;
        };

        return new Mechanics<>(tempo,
                Collections.unmodifiableList(Arrays.asList(new PieceModel<>(Species.DISC, "nero"), new PieceModel<>(Species.DISC, "bianco"))),
                Collections.unmodifiableList(board.positions()), 2, new Situation<>(mappa, 1), nextFunc);
    }

    public static Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> trovaParteMappaNextOthello(Move<PieceModel<Species>> mossa,
                                                                                                            Othello partita, Situation<PieceModel<Species>> s) {

        //Se è una mossa RESIGN, ignorala;
        if (mossa.kind.equals(Move.Kind.RESIGN))
            return null;

        Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> parteDelNext = new HashMap<>();

        //Altrimenti, inizializzo la mappa della Situation...
        Map<Pos,PieceModel<Species>> mappaRelativaAllaMossa = new HashMap<>();

        //Creo un'altra 'falsa' partita per crearmi il Situation nel Next;
        Othello pseudoOthelloNext2 = (Othello) partita.lightCopy();

        //...ed eseguo la mossa!
        pseudoOthelloNext2.move(mossa);

        //Per ogni posizione di QUESTA board, associo quella posizione al pezzo che contiene;
        for (Pos p : pseudoOthelloNext2.board.positions()) {
            PieceModel<Species> pezzo2 = pseudoOthelloNext2.board.get(p);

            if (pezzo2 != null)
                mappaRelativaAllaMossa.put(p, pezzo2);
        }

        //Creo la situazione a cui sono arrivato,
        // e la aggiungo alla mappa del Next;
        Situation<PieceModel<Species>> situazionePerQuestaMossa;

        //Prelevo lo stato del gioco in questa situazione;
        int statoAttuale = pseudoOthelloNext2.result();

        if (statoAttuale != -1)
            situazionePerQuestaMossa = new Situation<>(mappaRelativaAllaMossa, - statoAttuale);
        else
            situazionePerQuestaMossa = new Situation<>(mappaRelativaAllaMossa, pseudoOthelloNext2.turn);

        parteDelNext.put(mossa, situazionePerQuestaMossa);

        return parteDelNext;
    }

}
