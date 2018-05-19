package gapp.ulg.play;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Player;
import gapp.ulg.game.util.Probe;

import static gapp.ulg.game.board.GameRuler.Situation;
import static gapp.ulg.game.board.GameRuler.Next;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Una OptimalPlayerFactory è una fabbrica di {@link OptimalPlayer}
 * @param <P>  tipo del modello dei pezzi */
public class OptimalPlayerFactory<P> implements PlayerFactory<Player<P>,GameRuler<P>> {

    public OptimalPlayerFactory() {

        nome1 = "Execution";
        prompt1 = "Threaded execution";
        values1 = new ArrayList<>();
        values1.addAll(Arrays.asList("Sequential","Parallel"));
        default1 = "Sequential";
        valoreAttuale1 = "Sequential";

        strat = null;
        dirDelleStrategie = null;
        loadError = false;
        generateError = false;

    }

    public String nome1;
    public String prompt1;
    public List<String> values1;
    public String default1;
    public String valoreAttuale1;
    public Strategy<P> strat;
    public Path dirDelleStrategie;
    public boolean loadError;
    public boolean generateError;

    /** Una {@code Strategy} rappresenta una strategia ottimale per uno specifico
     * gioco.
     * @param <P>  tipo del modello dei pezzi */
    interface Strategy<P> {
        /** @return il nome del gioco di cui questa è una strategia ottimale */
        String gName();

        /** Ritorna la mossa (ottimale) nella situazione di gioco specificata. Se
         * {@code s} o {@code next} non sono compatibili con il gioco di questa
         * strategia, il comportamento del metodo è indefinito.
         * @param s  una situazione di gioco
         * @param next  la funzione delle mosse valide e prossime situazioni del
         *              gioco, cioè quella di {@link GameRuler.Mechanics#next}.
         * @return la mossa (ottimale) nella situazione di gioco specificata */
        Move<P> move(Situation<P> s, Next<P> next);
    }

    @Override
    public String name() { return "Optimal Player"; }

    /** Se la directory non è null, in essa salva e recupera file che contengono le
     * strategie ottimali per giochi specifici. Ogni strategia è salvata nella
     * directory in un file il cui nome rispetta il seguente formato:
     * <pre>
     *     strategy_<i>gameName</i>.dat
     * </pre>
     * dove <code><i>gameName</i></code> è il nome del gioco, cioè quello ritornato
     * dal metodo {@link GameRuler#name()}. La directory di default non è impostata
     * e quindi è null. */
    @Override
    public void setDir(Path dir) {
        dirDelleStrategie = dir;
    }

    /** Ritorna una lista con il seguente parametro:
     * <pre>
     *     - name: "Execution"
     *     - prompt: "Threaded execution"
     *     - values: ["Sequential","Parallel"]
     *     - default: "Sequential"
     * </pre>
     * @return la lista con il parametro */
    @Override
    public List<Param<?>> params() {
        List<Param<?>> listaParametri = new ArrayList<>();

        Param<String> primoP = new Param<String>() {
            @Override
            public String name() {
                return nome1;
            }

            @Override
            public String prompt() {
                return prompt1;
            }

            @Override
            public List<String> values() {
                return Collections.unmodifiableList(values1);
            }

            @Override
            public void set(Object v) {
                if (values1.contains(v))
                    valoreAttuale1 = (String) v;
                else
                    throw new IllegalArgumentException();
            }

            @Override
            public String get() {
                return valoreAttuale1;
            }
        };

        listaParametri.add(primoP);

        return Collections.unmodifiableList(listaParametri);
    }

    /** Ritorna {@link Play#YES} se conosce già la strategia ottimale per il gioco
     * specificato o perché è in un file (nella directory impostata con
     * {@link OptimalPlayerFactory#setDir(Path)}) o perché è in memoria, altrimenti
     * stima se può essere praticamente possibile imparare la strategia
     * ottimale e allora ritorna {@link Play#TRY_COMPUTE} altrimenti ritorna
     * {@link Play#NO}. Il gioco, cioè il {@link GameRuler}, valutato è quello
     * ottenuto dalla {@link GameFactory} specificata. Se non conosce già la
     * strategia ritorna sempre {@link Play#TRY_COMPUTE} eccetto che per i giochi
     * con i seguenti nomi che sa che è impossibile calcolarla:
     * <pre>
     *     Othello8x8, Othello10x10, Othello12x12
     * </pre>
     * Il controllo sull'esistenza di un file con la strategia è effettuato solamente
     * in base al nome (senza tentare di leggere il file, perché potrebbe richiedere
     * troppo tempo). */
    @Override
    public Play canPlay(GameFactory<? extends GameRuler<P>> gF) {
        String nomeGioco;

        Objects.requireNonNull(gF);

        try {

            GameRuler<P> giocoFit1 = gF.newGame();

        } catch (IllegalStateException e1) {

            gF.setPlayerNames("Tizio", "Caio");

            try {

                GameRuler<P> giocoFit2 = gF.newGame();

            } catch (IllegalStateException e2) { throw new IllegalStateException(); }

        }

        nomeGioco = gF.newGame().name();

        if (Arrays.asList("Othello8x8", "Othello10x10", "Othello12x12").contains(nomeGioco))
            return Play.NO;

        if (strat != null)
            return Play.YES;

        if (dirDelleStrategie != null) {

            try {
                List<String> nomiFile = new ArrayList<>(), nomiFile2 = new ArrayList<>(), nomiFile3 = new ArrayList<>();

                List<File> fileNellaCartella = Files.walk(dirDelleStrategie)
                        .filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .collect(Collectors.toList());

                for (File f : fileNellaCartella)
                    nomiFile.add(f.getName());

                for (String nome : nomiFile)
                    try {
                        nomiFile2.add(nome.substring(9, nome.length()));
                    } catch (StringIndexOutOfBoundsException e) {}

                for (String nome : nomiFile2)
                    try {
                        nomiFile3.add(nome.substring(0, nome.length() - 4));
                    } catch (StringIndexOutOfBoundsException e) {}


                if (nomiFile3.contains(nomeGioco)) {

                    FileInputStream fileIn = new FileInputStream(dirDelleStrategie.toString() +
                            "/strategy_" + nomeGioco + ".dat");

                    ObjectInputStream in = new ObjectInputStream(fileIn);

                    try {

                        Map<byte[], Integer> onlyByteStrategy = (Map<byte[], Integer>) in.readObject();
                        Map<Probe.EncS<P>, Integer> onlyStrategy = new HashMap<>();

                        for (Map.Entry<byte[], Integer> e : onlyByteStrategy.entrySet())
                            onlyStrategy.put(new Probe.EncS<>(e.getKey()), e.getValue());

                        Strategy<P> loadedStrategy = new StrategiaOttimale(gF);
                        ((StrategiaOttimale) loadedStrategy).strategia = onlyStrategy;
                        strat = loadedStrategy;

                    } catch (Exception e) { loadError = true; }

                    in.close();
                    fileIn.close();

                    return Play.YES;
                }

            } catch (IOException e) { throw new IllegalStateException(); }

        }

        return Play.TRY_COMPUTE;
    }

    /** Tenta di calcolare la strategia ottimale per il gioco specificato. Ovviamente
     * effettua il calcolo solo se il metodo
     * {@link OptimalPlayerFactory#canPlay(GameFactory)} ritorna {@link Play#TRY_COMPUTE}
     * per lo stesso gioco. Il gioco, cioè il {@link GameRuler}, da valutare è quello
     * ottenuto dalla {@link GameFactory} specificata. Se il calcolo ha successo e
     * una directory ({@link OptimalPlayerFactory#setDir(Path)} ) è impostata, tenta
     * di salvare il file con la strategia calcolata, altrimenti la mantiene in
     * memoria. */
    @Override
    public String tryCompute(GameFactory<? extends GameRuler<P>> gF, boolean parallel,
                             Supplier<Boolean> interrupt) {

        Objects.requireNonNull(gF);

        try {

            GameRuler<P> giocoFit1 = gF.newGame();

        } catch (IllegalStateException e1) {

            gF.setPlayerNames("Tizio", "Caio");

            try {

                GameRuler<P> giocoFit2 = gF.newGame();

            } catch (IllegalStateException e2) { throw new IllegalStateException(); }

        }

        if (canPlay(gF).equals(Play.YES))
            return null;

        else if (canPlay(gF).equals(Play.NO))
            return "CANNOT PLAY";

        else if (canPlay(gF).equals(Play.TRY_COMPUTE)) {

            try {
                strat = new StrategiaOttimale<>(gF, parallel, interrupt);
            } catch (OutOfMemoryError e) {
                generateError = true;
                return "OUT OF MEMORY";
            } catch (StackOverflowError e) {
                generateError = true;
                return "STACK OVERFLOW";
            } catch (NullPointerException e) {
                generateError = true;
                return "INTERRUPTED";
            }

            if (dirDelleStrategie != null) {
                String nomeGioco = gF.newGame().name();

                try {

                    FileOutputStream fileOut = new FileOutputStream(dirDelleStrategie.toString() +
                            "/strategy_" + nomeGioco + ".dat");

                    ObjectOutputStream out = new ObjectOutputStream(fileOut);

                    Map<Probe.EncS<P>, Integer> onlyStrategy = ((StrategiaOttimale) strat).strategia;
                    Map<byte[], Integer> onlyByteStrategy = new HashMap<>();

                    for (Map.Entry<Probe.EncS<P>, Integer> e : onlyStrategy.entrySet())
                        onlyByteStrategy.put(e.getKey().codedSit, e.getValue());

                    out.writeObject(onlyByteStrategy);
                    out.close();
                    fileOut.close();

                } catch (IOException i) {}
            }
        }

        return null;
    }

    /** Se il metodo {@link OptimalPlayerFactory#canPlay(GameFactory)} ritorna
     * {@link Play#YES} tenta di creare un {@link OptimalPlayer} con la strategia
     * per il gioco specificato cercandola tra quelle in memoria e se la directory
     * è impostata ({@link OptimalPlayerFactory#setDir(Path)}) anche nel file. */
    @Override
    public Player<P> newPlayer(GameFactory<? extends GameRuler<P>> gF, String name) {

        Objects.requireNonNull(gF);
        Objects.requireNonNull(name);

        if (!canPlay(gF).equals(Play.YES))
            throw new IllegalStateException(canPlay(gF).toString() + " invece di YES");

        if (loadError)
            throw new IllegalStateException("Errore caricamento da file");

        if (generateError)
            throw new IllegalStateException("Errore durante la creazione della strategia");

        return new OptimalPlayer<>(name, strat);
    }

}
