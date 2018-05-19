package gapp.ulg.games;

import gapp.ulg.game.board.*;
import gapp.ulg.game.util.BoardOct;
import gapp.ulg.game.util.Utils;

import java.util.*;

import static gapp.ulg.game.board.PieceModel.Species;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Un oggetto {@code MNKgame} rappresenta un GameRuler per fare una partita a un
 * (m,n,k)-game, generalizzazioni del ben conosciuto Tris o Tic Tac Toe.
 * <br>
 * Un gioco (m,n,k)-game si gioca su una board di tipo {@link Board.System#OCTAGONAL}
 * di larghezza (width) m e altezza (height) n. Si gioca con pezzi o pedine di specie
 * {@link Species#DISC} di due colori "nero" e "bianco". All'inizio la board è vuota.
 * Poi a turno ogni giocatore pone una sua pedina in una posizione vuota. Vince il
 * primo giocatore che riesce a disporre almeno k delle sue pedine in una linea di
 * posizioni consecutive orizzontale, verticale o diagonale. Chiaramente non è
 * possibile passare il turno e una partita può finire con una patta.
 * <br>
 * Per ulteriori informazioni si può consultare
 * <a href="https://en.wikipedia.org/wiki/M,n,k-game">(m,n,k)-game</a> */
public class MNKgame implements GameRuler<PieceModel<Species>> {
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
    public MNKgame(long time, int m, int n, int k, String p1, String p2) {

        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);

        if (!(k >= 1) && !(k <= Math.max(m,n)) && !(Math.max(m,n) <= 20) && !(Math.min(m,n) >= 1))
            throw new IllegalArgumentException();

        //Inizializzo la board;
        board = new BoardOct<>(m,n);

        //Lista con i nomi dei giocatori...
        nomiGiocatori = new ArrayList<>();
        nomiGiocatori.add(p1);
        nomiGiocatori.add(p2);

        //...ed i loro colori;
        colorFirstplayer = "";
        colorSecondPlayer = "";

        //Inizializzo le mosse rimanenti di entrambi i giocatori;
        if ((m * n) % 2 == 0) {

            mosseRimanentiNero = (m * n) / 2;
            mosseRimanentiBianco = (m * n) / 2;

        } else {

            mosseRimanentiNero = (m * n) / 2 + 1;
            mosseRimanentiBianco = (m * n) / 2;

        }

        //Inizializzo lo "stato" del gioco a -1;
        statoGioco = -1;

        //Il primo giocatore della lista inizia per primo;
        turn = 1;

        //Inizializzo la lista delle mosse finora effettuate;
        cronologiaMosse = new ArrayList<>();

        //Il tempo massimo per eseguire una mossa...
        tempo = time;

        //...e la lunghezza della linea!
        lunghezzaLinea = k;

    }

    public BoardOct<PieceModel<Species>> board;
    public List<String> nomiGiocatori;
    public String colorFirstplayer;
    public String colorSecondPlayer;
    public int mosseRimanentiNero;
    public int mosseRimanentiBianco;
    public int statoGioco;
    public int turn;
    public List<Move<PieceModel<PieceModel.Species>>> cronologiaMosse;
    public long tempo;
    public int lunghezzaLinea;

    public static List<Board.Dir> listaDirezioni = Arrays.asList(Board.Dir.UP, Board.Dir.UP_R, Board.Dir.RIGHT, Board.Dir.DOWN_R, Board.Dir.DOWN, Board.Dir.DOWN_L, Board.Dir.LEFT, Board.Dir.UP_L);


    /** Il nome rispetta il formato:
     * <pre>
     *     <i>M,N,K</i>-game
     * </pre>
     * dove <code><i>M,N,K</i></code> sono i valori dei parametri M,N,K, ad es.
     * "4,5,4-game". */
    @Override
    public String name() {

        //Creo la stringa raffigurante questo tipo di MNK:
        return "" + board.width() + "," + board.height() + "," + lunghezzaLinea + "-game";
    }

    @Override
    public <T> T getParam(String name, Class<T> c) {

        Objects.requireNonNull(name);
        Objects.requireNonNull(c);

        //Se il nome del parametro non è uno dei seguenti;
        if (!Arrays.asList("Time", "M", "N", "K").contains(name))
            throw new IllegalArgumentException();

        //Se la classe del valore non è String;
        if (!Arrays.asList(String.class, Integer.class, Long.class).contains(c))
            throw new ClassCastException();

        Object mm = board.width();
        Object nn = board.height();
        Object kk = lunghezzaLinea;

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

        }

        else if (name.equals("M"))
            return (T) mm;

        else if (name.equals("N"))
            return (T) nn;

        else if (name.equals("K"))
            return (T) kk;

        return null;
    }

    @Override
    public List<String> players() {
        return nomiGiocatori;
    }

    /** @return il colore "nero" per il primo giocatore e "bianco" per il secondo */
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
    public Board<PieceModel<Species>> getBoard() { return Utils.UnmodifiableBoard(board); }

    @Override
    public int turn() {

        //Se il gioco è terminato;
        if (statoGioco != -1)
            return 0;

        return turn;
    }

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

                //Se sta giocando il nero;
                if (turn == 1) {

                    //Metto il pezzo nero nella casella vuota...
                    board.put(new PieceModel<>(PieceModel.Species.DISC, "nero"), m.actions.get(0).pos.get(0));
                    mosseRimanentiNero--;

                }

                //Se invece è il turno del bianco;
                else if (turn == 2) {

                    //Metto il pezzo bianco nella casella vuota...
                    board.put(new PieceModel<>(PieceModel.Species.DISC, "bianco"), m.actions.get(0).pos.get(0));
                    mosseRimanentiBianco--;

                }

                turn = 3 - turn;
                cronologiaMosse.add(m);

            }

                //Se invece è una mossa RESIGN;
            else if (m.kind.equals(Move.Kind.RESIGN)) {

                //Aggiungo la mossa alla cronologia;
                cronologiaMosse.add(m);

            }

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
        if (cronologiaMosse.size() == 0)
            return false;

        //Se l'ultima mossa è stata un ACTION:
        if (cronologiaMosse.get(cronologiaMosse.size() - 1).kind.equals(Move.Kind.ACTION)) {

            //Rimuovo il pezzo che avevo messo nel turno precedente;
            PieceModel<Species> pezzoTolto = board.remove(cronologiaMosse.get(cronologiaMosse.size() - 1).actions.get(0).pos.get(0));

            if (pezzoTolto.color.equals("nero"))
                mosseRimanentiNero++;
            else
                mosseRimanentiBianco++;

            //Ripasso il turno all'altro giocatore!
            if (pezzoTolto.color.equals("nero"))
                turn = 1;
            else
                turn = 2;

            //Se finora c'è stata una sola mossa;
            if (cronologiaMosse.size() == 1) {

                //Svuoto la lista;
                cronologiaMosse = new ArrayList<>();
            }

            //Altrimenti..
            else if (cronologiaMosse.size() > 1) {

                //Elimino semplicemente l'ultima mossa;
                cronologiaMosse.remove(cronologiaMosse.size() - 1);
            }

        }

        //Se invece è stata un RESIGN:
        else if (cronologiaMosse.get(cronologiaMosse.size() - 1).kind.equals(Move.Kind.RESIGN)) {

            //Ripasso il turno all'altro giocatore!
            turn =  2 - (cronologiaMosse.size() % 2);

            //e riporto lo stato del gioco a -1!
            statoGioco = -1;

            //Se finora c'è stata una sola mossa;
            if (cronologiaMosse.size() == 1) {

                //Svuoto la lista;
                cronologiaMosse = new ArrayList<>();
            }

            //Altrimenti..
            else if (cronologiaMosse.size() > 1) {

                //Elimino semplicemente l'ultima mossa;
                cronologiaMosse.remove(cronologiaMosse.size() - 1);
            }

        }

        return true;
    }

    @Override
    public boolean isPlaying(int i) {

        //Se il gioco è terminato;
        if (statoGioco != -1) {

            return false;

        }

        //Se i non è l'indice di turnazione di uno dei giocatori;
        if (i <= 0 || i > 2) {

            throw new IllegalArgumentException();

        }

        if (i > 0 && i < 3) {

            return true;

        }

        return false;
    }

    @Override
    public int result() {

        //Se l'ultima mossa è RESIGN;
        if (cronologiaMosse.size() > 1 && cronologiaMosse.get(cronologiaMosse.size() - 1).kind.equals(Move.Kind.RESIGN)) {
            statoGioco = 3 - turn;
            turn = 0;
            return statoGioco;
        }

        //Per ogni posizione della board;
        for (Pos p : board.positions()) {
            PieceModel<Species> pedina = board.get(p);

            //Ignoro le caselle vuote;
            if (pedina == null)
                continue;

            //Creo un contatore per le pedine di uno stesso colore
            // impostato già ad 1 poiché mi trovo su una casella non vuota;
            String colorePedina = pedina.color;

            //Per ogni direzione;
            for (Board.Dir direzione : listaDirezioni) {
                Pos p1 = p;
                int counter = 1;

                //Inizio il ciclo;
                while (true) {

                    /*Prendo, ad ogni ciclo del while, la casella
                    adiacente a quella attuale, finche non trovo una
                    casella che sia dell'altro colore oppure null*/
                    p1 = board.adjacent(p1, direzione);

                    //Se p1 non è una casella della board, termino il ciclo!;
                    if (p1 == null)
                        break;

                    //Se trovo una pedina del colore delle pedina principale, aumento il counter di 1;
                    if (board.get(p1) != null && board.get(p1).color.equals(colorePedina))
                        counter++;

                        //Se invece trovo una pedina del colore opposto, termino il ciclo;
                    else if (board.get(p1) != null && !board.get(p1).color.equals(colorePedina))
                        break;

                        //Se invece trovo una casella vuota, termino lo stesso il ciclo;
                    else if (board.get(p1) == null)
                        break;

                    //Se ho trovato una linea che permette la vittoria;
                    if (counter == lunghezzaLinea) {

                        //Se la linea è nera:
                        if (colorePedina.equals("nero")) {
                            turn = 0;
                            statoGioco = 1;

                            return statoGioco;
                        }

                        //Se invece è bianca:
                        if (colorePedina.equals("bianco")) {
                            turn = 0;
                            statoGioco = 2;

                            return statoGioco;
                        }

                    }

                }

            }

        }

        //Se ho superato il controllo delle linee vincenti, e la board è piena, vuol dire che é necessariamente finita in patta;
        if (mosseRimanentiNero == 0 && mosseRimanentiBianco == 0) {
            statoGioco = 0;
            return statoGioco;
        }

        //Se invece sono arrivato fin qui, controllo il caso di parità certa a board non piena (Per i neri);
        for (Pos p : board.positions()) {
            int mosseDelNero = mosseRimanentiNero;

            //Ignoro le caselle piene;
            if (board.get(p) != null)
                continue;

            board.put(new PieceModel<>(PieceModel.Species.DISC, "nero"), p);
            mosseRimanentiNero--;
            PieceModel<Species> pedina = board.get(p);

            String colorePedina = pedina.color;

            //Per ogni direzione;
            for (Board.Dir direzione : listaDirezioni) {
                Pos p1 = p;
                int counter = 1;

                //Inizio il ciclo;
                while (true) {

                    /*Prendo, ad ogni ciclo del while, la casella
                    adiacente a quella attuale, finche non trovo una
                    casella che sia dell'altro colore oppure null*/
                    p1 = board.adjacent(p1, direzione);

                    //Se p1 non è una casella della board, termino il ciclo!;
                    if (p1 == null) {
                        mosseRimanentiNero = mosseDelNero - 1;

                        break;
                    }
                    //Se invece trovo una pedina del colore delle pedina principale, aumento il counter di 1;
                    else if (board.get(p1) != null && board.get(p1).color.equals(colorePedina))
                        counter++;

                        //Se invece trovo una pedina del colore opposto, termino il ciclo;
                    else if (board.get(p1) != null && !board.get(p1).color.equals(colorePedina)) {
                        mosseRimanentiNero = mosseDelNero - 1;

                        break;
                    }
                    //Se invece trovo una casella vuota ed il nero ha ancora a disposizione alcune pedine, metto una 'pedina immaginaria' nera al suo posto;
                    else if (board.get(p1) == null && mosseRimanentiNero > 0) {
                        mosseRimanentiNero--;
                        counter++;
                    }

                    //Se il nero non ha più pedine, esco;
                    else if (mosseRimanentiNero == 0) {
                        mosseRimanentiNero = mosseDelNero - 1;

                        break;
                    }

                    //Se ho trovato una linea che permette la vittoria, vuol dire che non siamo ancora in un caso di parita certa, quindi la partita puo continuare;
                    if (counter == lunghezzaLinea) {
                        mosseRimanentiNero = mosseDelNero;
                        board.remove(p);

                        return statoGioco;
                    }

                }

            }

            //Rimuovo il pezzo messo all'inizio per il test della parità, per fare spazio ad un altro!
            board.remove(p);
            mosseRimanentiNero = mosseDelNero;
        }

        //Se invece sono arrivato fin qui, controllo il caso di parità certa a board non piena (Per i neri);
        for (Pos p : board.positions()) {
            int mosseDelNero = mosseRimanentiNero;

            //Ignoro le caselle vuote;
            if (board.get(p) == null)
                continue;

            PieceModel<Species> pedina = board.get(p);
            String colorePedina = pedina.color;

            //Per ogni direzione;
            for (Board.Dir direzione : listaDirezioni) {
                Pos p1 = p;
                int counter = 1;

                //Inizio il ciclo;
                while (true) {

                    /*Prendo, ad ogni ciclo del while, la casella
                    adiacente a quella attuale, finche non trovo una
                    casella che sia dell'altro colore oppure null*/
                    p1 = board.adjacent(p1, direzione);

                    //Se p1 non è una casella della board, termino il ciclo!;
                    if (p1 == null) {
                        mosseRimanentiNero = mosseDelNero;

                        break;
                    }
                    //Se invece trovo una pedina del colore delle pedina principale, aumento il counter di 1;
                    else if (board.get(p1) != null && board.get(p1).color.equals(colorePedina))
                        counter++;

                        //Se invece trovo una pedina del colore opposto, termino il ciclo;
                    else if (board.get(p1) != null && !board.get(p1).color.equals(colorePedina)) {
                        mosseRimanentiNero = mosseDelNero;

                        break;
                    }
                    //Se invece trovo una casella vuota ed il nero ha ancora a disposizione alcune pedine, metto una 'pedina immaginaria' nera al suo posto;
                    else if (board.get(p1) == null && mosseRimanentiNero > 0) {
                        mosseRimanentiNero--;
                        counter++;
                    }

                    //Se il nero non ha più pedine, esco;
                    else if (mosseRimanentiNero == 0) {
                        mosseRimanentiNero = mosseDelNero;

                        break;
                    }

                    //Se ho trovato una linea che permette la vittoria, vuol dire che non siamo ancora in un caso di parita certa, quindi la partita puo continuare;
                    if (counter == lunghezzaLinea) {
                        mosseRimanentiNero = mosseDelNero;

                        return statoGioco;
                    }

                }

            }

            mosseRimanentiNero = mosseDelNero;
        }

        //Se invece sono arrivato fin qui, controllo il caso di parità certa a board non piena (Per i bianchi);
        for (Pos p : board.positions()) {
            int mosseDelBianco = mosseRimanentiBianco;

            //Ignoro le caselle piene;
            if (board.get(p) != null)
                continue;

            board.put(new PieceModel<>(PieceModel.Species.DISC, "bianco"), p);
            mosseRimanentiBianco--;
            PieceModel<Species> pedina = board.get(p);

            String colorePedina = pedina.color;

            //Per ogni direzione;
            for (Board.Dir direzione : listaDirezioni) {
                Pos p1 = p;
                int counter = 1;

                //Inizio il ciclo;
                while (true) {

                    /*Prendo, ad ogni ciclo del while, la casella
                    adiacente a quella attuale, finche non trovo una
                    casella che sia dell'altro colore oppure null*/
                    p1 = board.adjacent(p1, direzione);

                    //Se p1 non è una casella della board, termino il ciclo!;
                    if (p1 == null) {
                        mosseRimanentiBianco = mosseDelBianco - 1;

                        break;
                    }
                    //Se invece trovo una pedina del colore delle pedina principale, aumento il counter di 1;
                    else if (board.get(p1) != null && board.get(p1).color.equals(colorePedina))
                        counter++;

                        //Se invece trovo una pedina del colore opposto, termino il ciclo;
                    else if (board.get(p1) != null && !board.get(p1).color.equals(colorePedina)) {
                        mosseRimanentiBianco = mosseDelBianco - 1;

                        break;
                    }
                    //Se invece trovo una casella vuota ed il nero ha ancora a disposizione alcune pedine, metto una 'pedina immaginaria' nera al suo posto;
                    else if (board.get(p1) == null && mosseRimanentiNero > 0) {
                        mosseRimanentiBianco--;
                        counter++;
                    }

                    //Se il nero non ha più pedine, esco;
                    else if (mosseRimanentiNero == 0) {
                        mosseRimanentiBianco = mosseDelBianco - 1;

                        break;
                    }

                    //Se ho trovato una linea che permette la vittoria, vuol dire che non siamo ancora in un caso di parita certa, quindi la partita puo continuare;
                    if (counter == lunghezzaLinea) {
                        mosseRimanentiBianco = mosseDelBianco;
                        board.remove(p);

                        return statoGioco;
                    }

                }

            }

            //Rimuovo il pezzo messo all'inizio per il test della parità, per fare spazio ad un altro!
            board.remove(p);
            mosseRimanentiBianco = mosseDelBianco;
        }

        //Se invece sono arrivato fin qui, controllo il caso di parità certa a board non piena (Per i bianchi);
        for (Pos p : board.positions()) {
            int mosseDelBianco = mosseRimanentiBianco;

            //Ignoro le caselle vuote;
            if (board.get(p) == null)
                continue;

            PieceModel<Species> pedina = board.get(p);
            String colorePedina = pedina.color;

            //Per ogni direzione;
            for (Board.Dir direzione : listaDirezioni) {
                Pos p1 = p;
                int counter = 1;

                //Inizio il ciclo;
                while (true) {

                    /*Prendo, ad ogni ciclo del while, la casella
                    adiacente a quella attuale, finche non trovo una
                    casella che sia dell'altro colore oppure null*/
                    p1 = board.adjacent(p1, direzione);

                    //Se p1 non è una casella della board, termino il ciclo!;
                    if (p1 == null) {
                        mosseRimanentiBianco = mosseDelBianco;

                        break;
                    }
                    //Se invece trovo una pedina del colore delle pedina principale, aumento il counter di 1;
                    else if (board.get(p1) != null && board.get(p1).color.equals(colorePedina))
                        counter++;

                        //Se invece trovo una pedina del colore opposto, termino il ciclo;
                    else if (board.get(p1) != null && !board.get(p1).color.equals(colorePedina)) {
                        mosseRimanentiBianco = mosseDelBianco;

                        break;
                    }
                    //Se invece trovo una casella vuota ed il nero ha ancora a disposizione alcune pedine, metto una 'pedina immaginaria' nera al suo posto;
                    else if (board.get(p1) == null && mosseRimanentiNero > 0) {
                        mosseRimanentiBianco--;
                        counter++;
                    }

                    //Se il nero non ha più pedine, esco;
                    else if (mosseRimanentiBianco == 0) {
                        mosseRimanentiBianco = mosseDelBianco;

                        break;
                    }

                    //Se ho trovato una linea che permette la vittoria, vuol dire che non siamo ancora in un caso di parita certa, quindi la partita puo continuare;
                    if (counter == lunghezzaLinea) {
                        mosseRimanentiBianco = mosseDelBianco;

                        return statoGioco;
                    }

                }

            }

            mosseRimanentiBianco = mosseDelBianco;
        }

        //Se sono giunto fin qui, non c'è possibilità di vittoria per entrambi, ed è quindi patta;
        statoGioco = 0;
        return statoGioco;
    }

    /** Ogni mossa (diversa dall'abbandono) è rappresentata da una sola {@link Action}
     * di tipo {@link Action.Kind#ADD}. */
    @Override
    public Set<Move<PieceModel<Species>>> validMoves() {
        Set<Move<PieceModel<PieceModel.Species>>> insieme = new HashSet<>();
        insieme.add(new Move<>(Move.Kind.RESIGN));
        String player;

        //Associo il colore in base al turno;
        if (turn == 1)
            player = "nero";
        else
            player = "bianco";

        //Se il gioco è terminato;
        if (statoGioco != -1)
            throw new IllegalStateException();

        //Per ogni riga della board;
        for (Pos p : board.positions()) {

            //Se quella casella è vuota;
            if (board.get(p) == null) {

                //Creo l'azione ADD per quella casella;
                Action<PieceModel<Species>> azione = new Action<>(p, new PieceModel<>(PieceModel.Species.DISC, player));

                //Creo la mossa ACTION che fa quell'azione;
                Move<PieceModel<PieceModel.Species>> mossa = new Move(azione);

                //Aggiungo l'azione all'insieme delle mosse valide;
                insieme.add(mossa);

            }

        }

        return insieme;
    }

    @Override
    public GameRuler<PieceModel<Species>> copy() {

        //Inizializzo il gameruler;
        MNKgame partita = new MNKgame(tempo, board.width(), board.height(),
                lunghezzaLinea, nomiGiocatori.get(0), nomiGiocatori.get(1));

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

        //Copio le pedine rimanenti;
        partita.mosseRimanentiNero = mosseRimanentiNero;
        partita.mosseRimanentiBianco = mosseRimanentiBianco;

        //Copio lo stato del gioco;
        partita.statoGioco = statoGioco;

        //Copio il turno;
        partita.turn = turn;

        //Copio la lunghezza della linea;
        partita.lunghezzaLinea = lunghezzaLinea;

        //...e la cronologia!
        for (Move<PieceModel<PieceModel.Species>> mossa : cronologiaMosse)
            partita.cronologiaMosse.add(mossa);

        return partita;
    }

    @Override
    public Mechanics<PieceModel<Species>> mechanics() {
        Map<Pos,PieceModel<Species>> mappa = new HashMap<>();

        //Creo una 'falsa' partita per crearmi il Situation;
        MNKgame pseudoMNK = new MNKgame(tempo, board.width(), board.height(), lunghezzaLinea, "Andrea", "Gattuso");

        //Per ogni posizione della board, associo quella posizione
        // al pezzo che contiene;
        for (Pos p : pseudoMNK.board.positions())
            if (pseudoMNK.board.get(p) != null)
                mappa.put(p, pseudoMNK.board.get(p));

        Next<PieceModel<Species>> nextFunc2 = (s) -> {
            Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> mappona = new HashMap<>();

            Objects.requireNonNull(s);

            //Creo una 'falsa' partita per crearmi il Situation nel Next;
            MNKgame pseudoMNKNext = new MNKgame(tempo, board.width(), board.height(), lunghezzaLinea, "Andrea", "Gattuso");
            pseudoMNKNext.turn = s.turn;

            //Do ad ogni giocatore di questa partita le pedine rimaste;
            if (s.newMap().size() % 2 == 0) {

                pseudoMNKNext.mosseRimanentiNero = pseudoMNKNext.mosseRimanentiNero - (s.newMap().size()) / 2;
                pseudoMNKNext.mosseRimanentiBianco = pseudoMNKNext.mosseRimanentiBianco - (s.newMap().size()) / 2;

            } else if (s.newMap().size() % 2 == 1) {

                pseudoMNKNext.mosseRimanentiNero = pseudoMNKNext.mosseRimanentiNero - (s.newMap().size()) / 2 + 1;
                pseudoMNKNext.mosseRimanentiBianco = pseudoMNKNext.mosseRimanentiBianco - (s.newMap().size()) / 2;

            }

            //Porto la board alla Situation che mi interessa;
            for (Pos p : pseudoMNKNext.board.positions()) {
                PieceModel<Species> pezzo = s.get(p);

                if (pezzo != null)
                    pseudoMNKNext.board.put(pezzo, p);
            }

            //Creo l'insieme delle mosse valide;
            Set<Move<PieceModel<Species>>> mosseValide = pseudoMNKNext.validMoves();

            //Per ogni mossa possibile da questa situazione di gioco:
            for (Move<PieceModel<Species>> mossa : mosseValide) {

                //Se è una mossa RESIGN, ignorala;
                if (mossa.kind.equals(Move.Kind.RESIGN))
                    continue;

                //Altrimenti, inizializzo la mappa della Situation...
                Map<Pos,PieceModel<Species>> mappaRelativaAllaMossa = new HashMap<>();

                //Creo un'altra 'falsa' partita per crearmi il Situation nel Next;
                MNKgame pseudoMNKNext2 = (MNKgame) pseudoMNKNext.copy();

                //...ed eseguo la mossa!
                pseudoMNKNext2.move(mossa);

                //Per ogni posizione di QUESTA board, associo quella posizione al pezzo che contiene;
                for (Pos p : pseudoMNKNext2.board.positions()) {
                    PieceModel<Species> pezzo2 = pseudoMNKNext2.board.get(p);

                    if (pezzo2 != null)
                        mappaRelativaAllaMossa.put(p, pezzo2);
                }

                //Creo la situazione a cui sono arrivato,
                // e la aggiungo alla mappa del Next;
                Situation<PieceModel<Species>> situazionePerQuestaMossa = null;

                //Prelevo lo stato del gioco in questa situazione;
                int statoAttuale = pseudoMNKNext2.result();

                if (statoAttuale != -1)
                    situazionePerQuestaMossa = new Situation<>(mappaRelativaAllaMossa, - statoAttuale);
                else
                    situazionePerQuestaMossa = new Situation<>(mappaRelativaAllaMossa, pseudoMNKNext2.turn);

                //Aggiungo chiave e valore alla mappa principale del Next;
                mappona.put(mossa, situazionePerQuestaMossa);
            }

            return mappona;
        };

        return new Mechanics<>(tempo,
                Collections.unmodifiableList(Arrays.asList(new PieceModel<>(Species.DISC, "nero"), new PieceModel<>(Species.DISC, "bianco"))),
                Collections.unmodifiableList(board.positions()), 2, new Situation<>(mappa, 1), nextFunc2);
    }

}
