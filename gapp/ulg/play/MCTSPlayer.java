package gapp.ulg.play;

import gapp.ulg.game.board.*;
import gapp.ulg.game.util.Utils;
import gapp.ulg.games.MNKgame;

import java.util.*;
import java.util.concurrent.*;


/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Un oggetto {@code MCTSPlayer} è un giocatore che gioca seguendo una strategia
 * basata su Monte-Carlo Tree Search e può giocare a un qualsiasi gioco.
 * <br>
 * La strategia che usa è una MCTS (Monte-Carlo Tree Search) piuttosto semplificata.
 * Tale strategia si basa sul concetto di <i>rollout</i> (srotolamento). Un
 * <i>rollout</i> a partire da una situazione di gioco <i>S</i> è l'esecuzione di
 * una partita fino all'esito finale a partire da <i>S</i> facendo compiere ai
 * giocatori mosse random.
 * <br>
 * La strategia adottata da un {@code MCTSPlayer}, è la seguente. In ogni situazione
 * di gioco <i>S</i> in cui deve muovere, prima di tutto ottiene la mappa delle
 * possibili mosse valide da <i>S</i> con le corrispondenti prossime situazioni. Per
 * ogni prossima situazione <i>NS</i> esegue <i>R</i> rollouts e calcola un punteggio
 * di <i>NS</i> dato dalla somma degli esiti dei rollouts. L'esito di un rollout è
 * rappresentato da un intero che è 0 se finisce in una patta, 1 se finisce con la
 * vittoria del giocatore e -1 altrimenti. Infine sceglie la mossa che porta nella
 * prossima situazione con punteggio massimo. Il numero <i>R</i> di rollouts da
 * compiere è calcolato così <i>R = ceil(RPM/M)</i>, cioè la parte intera superiore
 * della divisione decimale del numero di rollout per mossa <i>RPM</i> diviso il
 * numero <i>M</i> di mosse possibili (è sempre esclusa {@link Move.Kind#RESIGN}).
 * @param <P>  tipo del modello dei pezzi */
public class MCTSPlayer<P> implements Player<P> {
    /** Crea un {@code MCTSPlayer} con un limite dato sul numero di rollouts per
     * mossa.
     *
     * @param name  il nome del giocatore
     * @param rpm   limite sul numero di rollouts per mossa, se < 1 è inteso 1
     * @param parallel  se true la ricerca della mossa da fare è eseguita cercando
     *                  di sfruttare il parallelismo della macchina
     * @throws NullPointerException se {@code name} è null */
    public MCTSPlayer(String name, int rpm, boolean parallel) {

        Objects.requireNonNull(name);

        nome = name;
        partita = null;
        rPM = rpm;
        seqOrConc = parallel;
        maxTh = Runtime.getRuntime().availableProcessors();

    }

    private String nome;
    private GameRuler<P> partita;
    private int rPM;
    private Boolean seqOrConc;
    private int maxTh;
    private ForkJoinPool fjp;
    private ExecutorService bgExec;

    @Override
    public String name() { return nome; }

    @Override
    public void setGame(GameRuler<P> g) {
        Objects.requireNonNull(g);
        partita = g;
    }

    @Override
    public void moved(int i, Move<P> m) {
        Objects.requireNonNull(m);

        if (partita == null || partita.result() != -1)
            throw new IllegalStateException();

        if (i <= 0 || i > partita.players().size() || !partita.isValid(m))
            throw new IllegalArgumentException();

        if (partita.isPlaying(i))
            partita.move(m);

    }

    @Override
    public Move<P> getMove() {
        Map<Move<P>, GameRuler.Situation<P>> mappona;
        Map<Pos,P> mappaAttuale = new HashMap<>();
        GameRuler.Situation<P> situazioneAttuale;
        Map<Move<P>, Integer> mossaToPunteggio = new HashMap<>();
        Move<P> mossa = null;

        System.out.println("maxth mcts " + maxTh);
        final ExecutorService[] exec = {maxTh != 0 ? Executors.newFixedThreadPool(maxTh) : null};

        System.out.println(seqOrConc);

        List<Future<Map<Move<P>, Integer>>> futures = new ArrayList<>();

        int turnoMCTS = partita.turn();
        long tempoMAX;
        final boolean[] tempoScaduto = {false};
        Thread waitTime;

        tempoMAX = partita.mechanics().time;

        waitTime = new Thread(() -> {

            if (tempoMAX > 0) {

                synchronized (mossaToPunteggio) {

                    boolean hasFinished = false;

                    while (!hasFinished) {

                        try { mossaToPunteggio.wait(tempoMAX); } catch (InterruptedException e) {}

                        hasFinished = true;
                    }

                    tempoScaduto[0] = true;

                    if (seqOrConc) {
                        for (Future<Map<Move<P>, Integer>> f3 : futures)
                            f3.cancel(true);

                        exec[0].shutdownNow();
                    }

                }

            }

        });

        waitTime.setDaemon(true);
        waitTime.start();

        if (partita == null || partita.result() != -1 || !partita.isPlaying(partita.turn()))
            throw new IllegalStateException();

        //Per ogni posizione di QUESTA board, associo quella posizione al pezzo che contiene;
        for (Pos p : partita.getBoard().positions()) {
            P pezzo2 = partita.getBoard().get(p);

            if (pezzo2 != null)
                mappaAttuale.put(p, pezzo2);
        }

        situazioneAttuale = new GameRuler.Situation<>(mappaAttuale, partita.turn());
        mappona = partita.mechanics().next.get(situazioneAttuale);

        int numeroDiRollout = (int) Math.ceil((rPM)/(mappona.size()));

        if (!seqOrConc) {

            System.out.println("sequenziale");

            for (Map.Entry<Move<P>, GameRuler.Situation<P>> e : mappona.entrySet()) {
                Move<P> move = e.getKey();
                GameRuler.Situation<P> sit = e.getValue();

                if (sit.turn <= 0) {

                    if (sit.turn == -turnoMCTS) {
                        mossaToPunteggio.put(move, numeroDiRollout);
                        continue;
                    } else if (sit.turn != 0) {
                        mossaToPunteggio.put(move, -numeroDiRollout);
                        continue;
                    }

                }

                if (Arrays.asList("1","2","3","4","5","6","7","8","9","0").contains(partita.name().substring(0, 1)))
                    mossaToPunteggio.putAll(mnkSequential(sit, move, numeroDiRollout, turnoMCTS, tempoScaduto));
                else
                    mossaToPunteggio.putAll(genericSequential(move, numeroDiRollout, turnoMCTS, tempoScaduto));

                if (tempoScaduto[0]) {
                    waitTime.interrupt();

                    System.out.println("Non ho fatto in tempo! Finora ho trovato " + mossaToPunteggio.size() + " mosse!");

                    int b = -1_000_000;

                    for (Map.Entry<Move<P>, Integer> e2 : mossaToPunteggio.entrySet())
                        if (e2.getValue() > b) {
                            b = e2.getValue();
                            mossa = e2.getKey();
                        }

                    return mossa;
                }

            }

        }

        else {

            System.out.println("parallelo");

            for (Map.Entry<Move<P>, GameRuler.Situation<P>> e : mappona.entrySet())
                futures.add(exec[0].submit(() -> concurrentMCTS(e.getValue(), e.getKey(), numeroDiRollout, turnoMCTS, tempoScaduto)));

            for (Future<Map<Move<P>, Integer>> f : futures) {
                try {
                    mossaToPunteggio.putAll(f.get());
                } catch (InterruptedException | ExecutionException | CancellationException e) {

                    Move<P> mossa2;

                    List<Move<P>> lista2 = new ArrayList<>(partita.validMoves());

                    while (true) {
                        int randomINT = (int) (Math.random() * (lista2.size()));
                        Move<P> mossaRandom = lista2.get(randomINT);

                        if (!mossaRandom.kind.equals(Move.Kind.RESIGN)) {

                            mossa2 = mossaRandom;
                            break;

                        }
                    }

                    return mossa2;


                }

                if (tempoScaduto[0]) {
                    waitTime.interrupt();

                    int b = -1_000_000;

                    for (Map.Entry<Move<P>, Integer> e2 : mossaToPunteggio.entrySet())
                        if (e2.getValue() > b) {
                            b = e2.getValue();
                            mossa = e2.getKey();
                        }

                    for (Future<Map<Move<P>, Integer>> f2 : futures)
                        f2.cancel(true);

                    exec[0].shutdownNow();
                    return mossa;
                }

            }

            exec[0].shutdownNow();
        }

        int a = -1_000_000;

        for (Map.Entry<Move<P>, Integer> e : mossaToPunteggio.entrySet()) {

            if (e.getValue() > a) {
                a = e.getValue();
                mossa = e.getKey();
            }

        }

        synchronized (mossaToPunteggio) { mossaToPunteggio.notifyAll(); }

        if (tempoMAX > 0)
            waitTime.interrupt();

        tempoScaduto[0] = false;

        return mossa;
    }

    @Override
    public void threads(int maxTh, ForkJoinPool fjp, ExecutorService bgExec) {

        System.out.println(maxTh);

        if (maxTh < 0)
            this.maxTh = Runtime.getRuntime().availableProcessors();

        else if (maxTh == 0) {
            this.maxTh = 0;
            seqOrConc = false;
        }

        else
            this.maxTh = Math.min(maxTh,Runtime.getRuntime().availableProcessors());

        this.fjp = fjp;
        this.bgExec = bgExec;
    }

    public Map<Move<P>, Integer> mnkSequential(GameRuler.Situation<P> sit, Move<P> move,
                                               int numeroDiRollout, int turnoMCTS, boolean[] tempoScaduto) {
        Map<Move<P>, Integer> mappaDaRitornare = new HashMap<>();

        //Creo una partita per questa mossa;
        MNKgame giocata2 = new MNKgame(((MNKgame) partita).tempo, partita.getBoard().width(),
                partita.getBoard().height(), ((MNKgame) partita).lunghezzaLinea,
                ((MNKgame) partita).nomiGiocatori.get(0), ((MNKgame) partita).nomiGiocatori.get(1));

        giocata2.turn = sit.turn;

        //Do ad ogni giocatore di questa partita le pedine rimaste;
        if (sit.newMap().size() % 2 == 0) {

            giocata2.mosseRimanentiNero = giocata2.mosseRimanentiNero - (sit.newMap().size()) / 2;
            giocata2.mosseRimanentiBianco = giocata2.mosseRimanentiBianco - (sit.newMap().size()) / 2;

        } else if (sit.newMap().size() % 2 == 1) {

            giocata2.mosseRimanentiNero = giocata2.mosseRimanentiNero - (sit.newMap().size()) / 2 + 1;
            giocata2.mosseRimanentiBianco = giocata2.mosseRimanentiBianco - (sit.newMap().size()) / 2;

        }

        //Porto la board alla Situation che mi interessa;
        for (Pos p : giocata2.board.positions()) {
            P pezzo = sit.get(p);

            if (pezzo != null)
                giocata2.board.put((PieceModel<PieceModel.Species>) pezzo, p);
        }

        int valoreDiVittoria = 0;

        Player<P> py1 = new RandPlayer<>(((MNKgame) partita).nomiGiocatori.get(0));
        Player<P> py2 = new RandPlayer<>(((MNKgame) partita).nomiGiocatori.get(1));

        for (int i = 0 ; i < numeroDiRollout ; i++) {

            int risultato = Utils.playDuring(giocata2, (Player<PieceModel<PieceModel.Species>>) py1,
                    (Player<PieceModel<PieceModel.Species>>) py2);

            if (risultato == turnoMCTS)
                valoreDiVittoria++;

            else if (risultato != 0)
                valoreDiVittoria--;

            if (tempoScaduto[0])
                break;

        }

        mappaDaRitornare.put(move, valoreDiVittoria);

        return mappaDaRitornare;
    }

    public Map<Move<P>, Integer> genericSequential(Move<P> move,
                                                   int numeroDiRollout, int turnoMCTS, boolean[] tempoScaduto) {

        Map<Move<P>, Integer> mappaDaRitornare = new HashMap<>();

        //Creo una partita per questa mossa;
        GameRuler<P> giocata2 = partita.copy();

        giocata2.move(move);

        int valoreDiVittoria = 0;

        Player<P> py1 = new RandPlayer<>(giocata2.players().get(0));
        Player<P> py2 = new RandPlayer<>(giocata2.players().get(1));

        for (int i = 0 ; i < numeroDiRollout ; i++) {

            int risultato = Utils.playDuring(giocata2, py1, py2);

            if (risultato == turnoMCTS)
                valoreDiVittoria++;

            else if (risultato != 0)
                valoreDiVittoria--;

            if (tempoScaduto[0])
                break;

        }

        mappaDaRitornare.put(move, valoreDiVittoria);

        return mappaDaRitornare;
    }

    public Map<Move<P>, Integer> concurrentMCTS(GameRuler.Situation<P> sit, Move<P> move,
                                                int numeroDiRollout, int turnoMCTS, boolean[] tempoScaduto) {

        Map<Move<P>, Integer> mappaDaRitornare = new HashMap<>();

        if (sit.turn <= 0) {

            if (sit.turn == -turnoMCTS) {
                mappaDaRitornare.put(move, numeroDiRollout);
                return mappaDaRitornare;
            } else if (sit.turn != 0) {
                mappaDaRitornare.put(move, -numeroDiRollout);
                return  mappaDaRitornare;
            }

        }

        if (Arrays.asList("1","2","3","4","5","6","7","8","9","0").contains(partita.name().substring(0, 1)))
            mappaDaRitornare.putAll(mnkSequential(sit, move, numeroDiRollout, turnoMCTS, new boolean[] {false}));
        else
            mappaDaRitornare.putAll(genericSequential(move, numeroDiRollout, turnoMCTS, new boolean[] {false}));

        return mappaDaRitornare;
    }

}
