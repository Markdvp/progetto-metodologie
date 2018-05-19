package gapp.ulg.game.util;

import gapp.ulg.game.*;
import gapp.ulg.game.board.*;
import gapp.ulg.games.GameFactories;
import gapp.ulg.play.PlayerFactories;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;

import static gapp.gui.Variables.*;
import static gapp.ulg.game.util.PlayerGUI.MoveChooser;

/** Un {@code PlayGUI} è un oggetto che facilita la gestione di partite in una
 * applicazione controllata da GUI. Un {@code PlayGUI} segue lo svolgimento di una
 * partita dalla scelta della {@link GameFactory} e dei {@link PlayerFactory} e di
 * tutte le mosse fino alla fine naturale della partita o alla sua interruzione.
 * Inoltre, un {@code PlayGUI} aiuta sia a mantenere la reattività della GUI che a
 * garantire la thread-safeness usando un thread di confinamento per le invocazioni
 * di tutti i metodi e costruttori degli oggetti coinvolti in una partita.
 * @param <P>  tipo del modello dei pezzi */
public class PlayGUI<P> {
    /** Un {@code Observer} è un oggetto che osserva lo svolgimento di una o più
     * partite. Lo scopo principale è di aggiornare la GUI che visualizza la board
     * ed eventuali altre informazioni a seguito dell'inizio di una nuova partita e
     * di ogni mossa eseguita.
     * @param <P>  tipo del modello dei pezzi */
    public interface Observer<P> {
        /** Comunica allo {@code Observer} il gioco (la partita) che sta iniziando.
         * Può essere nello stato iniziale o in uno stato diverso, ad es. se la
         * partita era stata sospesa ed ora viene ripresa. L'oggetto {@code g} è
         * una copia del {@link GameRuler} ufficiale del gioco. Lo {@code Observer}
         * può usare e modificare {@code g} a piacimento senza che questo abbia
         * effetto sul {@link GameRuler} ufficiale. In particolare lo {@code Observer}
         * può usare {@code g} per mantenersi sincronizzato con lo stato del gioco
         * riportando in {@code g} le mosse dei giocatori, vedi
         * {@link Observer#moved(int, Move)}. L'uso di {@code g} dovrebbe avvenire
         * solamente nel thread in cui il metodo è invocato.
         * <br>
         * <b>Il metodo non blocca, non usa altri thread e ritorna velocemente.</b>
         * @param g  un gioco, cioè una partita
         * @throws NullPointerException se {@code g} è null */
        void setGame(GameRuler<P> g);

        /** Comunica allo {@code Observer} la mossa eseguita da un giocatore. Lo
         * {@code Observer} dovrebbe usare tale informazione per aggiornare la sua
         * copia del {@link GameRuler}. L'uso del GameRuler dovrebbe avvenire
         * solamente nel thread in cui il metodo è invocato.
         * <br>
         * <b>Il metodo non blocca, non usa altri thread e ritorna velocemente.</b>
         * @param i  indice di turnazione di un giocatore
         * @param m  la mossa eseguita dal giocatore
         * @throws IllegalStateException se non c'è un gioco impostato o c'è ma è
         * terminato.
         * @throws NullPointerException se {@code m} è null
         * @throws IllegalArgumentException se {@code i} non è l'indice di turnazione
         * di un giocatore o {@code m} non è una mossa valida nell'attuale situazione
         * di gioco */
        void moved(int i, Move<P> m);

        /** Comunica allo {@code Observer} che il giocatore con indice di turnazione
         * {@code i} ha violato un vincolo sull'esecuzione (ad es. il tempo concesso
         * per una mossa). Dopo questa invocazione il giocatore {@code i} è
         * squalificato e ciò produce gli stessi effetti che si avrebbero se tale
         * giocatore si fosse arreso. Quindi lo {@code Observer} per sincronizzare
         * la sua copia con la partita esegue un {@link Move.Kind#RESIGN} per il
         * giocatore {@code i}. L'uso del GameRuler dovrebbe avvenire solamente nel
         * thread in cui il metodo è invocato.
         * @param i  indice di turnazione di un giocatore
         * @param msg  un messaggio che descrive il tipo di violazione
         * @throws NullPointerException se {@code msg} è null
         * @throws IllegalArgumentException se {@code i} non è l'indice di turnazione
         * di un giocatore */
        void limitBreak(int i, String msg);

        /** Comunica allo {@code Observer} che la partita è stata interrotta. Ad es.
         * è stato invocato il metodo {@link PlayGUI#stop()}.
         * @param msg  una stringa con una descrizione dell'interruzione */
        void interrupted(String msg);
    }

    /**
     * Il thread di confinamento
     */
    private ExecutorService gateThread;

    /**
     * Il thread controllore
     */
    private Thread checkerThread;

    /**
     * La GameFactory principale
     */
    private volatile GameFactory gameFactory;

    /**
     * La partita in corso
     */
    private volatile GameRuler<P> currentGame;

    /**
     * L'ultima mossa ritornata da uno dei giocatori
     */
    private volatile Move<P> lastMove;

    /**
     * Lista di {@link Triple}, utili a mantenere informazioni sui Player e le relative factory
     */
    private volatile List<Triple<P>> iPP;

    /**
     * L' osservatore della partita
     */
    private volatile Observer<P> observer;

    /**
     * Indica se è in corso una partita
     */
    public volatile boolean runningMatch;

    /**
     * Indica se la partita è stata interrotta
     */
    private volatile boolean interruptedMatch;

    /**
     * Indica il fallimento nell' impostazione della partita
     */
    private volatile boolean failure;

    /**
     * Tempo massimo in millisecondi di attesa per un blocco del thread di confinamento
     */
    private volatile long maxBlockTime;

    /**
     * Tempo massimo per l' esecuzione dei metodi {@link Player#setGame(GameRuler)}
     * e {@link Player#moved(int, Move)}, se anche timeout è > 0, il tempo è il minore tra i due, -1 se non c è limite.
     */
    private volatile long minBetweenBlockAndOut;

    /**
     * Tempo massimo per l' esecuzione dei metodi {@link Player#setGame(GameRuler)}
     * e {@link Player#moved(int, Move)}, se anche MaxBlockTime è > 0, il tempo è il minore tra i due, <= 0 se non c è limite
     */
    private volatile long timeout;

    /**
     * Minimo numero di millisecondi tra una mossa e quella successiva
     */
    private volatile long minTime;

    /**
     * Tolleranza, espressa in millisecondi, per il ritorno del metodo {@link Player#getMove()}
     */
    private volatile long tol;

    /**
     * Numero massimo di thread addizionali
     */
    private volatile int maxTh;

    /**
     * Pool per l' esecutore supplementare di tipo {@link ForkJoinPool}
     */
    private volatile int fjpSize;

    /**
     * Numero di thread per l' esecutore di tipo {@link ExecutorService} che lavora in background
     */
    private volatile int bgExecSize;

    /** Crea un oggetto {@link PlayGUI} per partite controllate da GUI. L'oggetto
     * {@code PlayGUI} può essere usato per giocare più partite anche con giochi e
     * giocatori diversi. Per garantire che tutti gli oggetti coinvolti
     * {@link GameFactory}, {@link PlayerFactory}, {@link GameRuler} e {@link Player}
     * possano essere usati tranquillamente anche se non sono thread-safe, crea un
     * thread che chiamiamo <i>thread di confinamento</i>, in cui invoca tutti i
     * metodi e costruttori di tali oggetti. Il thread di confinamento può cambiare
     * solo se tutti gli oggetti coinvolti in una partita sono creati ex novo. Se
     * durante una partita un'invocazione (ad es. a {@link Player#getMove()}) blocca
     * il thread di confinamento per un tempo superiore a {@code maxBlockTime}, la
     * partita è interrotta.
     * <br>
     * All'inizio e durante una partita invoca i metodi di {@code obs}, rispettando
     * le specifiche di {@link Observer}, sempre nel thread di confinamento.
     * <br>
     * <b>Tutti i thread usati sono daemon thread</b>
     * @param obs  un osservatore del gioco
     * @param maxBlockTime  tempo massimo in millisecondi di attesa per un blocco
     *                      del thread di confinamento, se < 0, significa nessun
     *                      limite di tempo
     * @throws NullPointerException se {@code obs} è null */
    public PlayGUI(Observer<P> obs, long maxBlockTime) {

        if (obs == null)
            throw new NullPointerException("l'observer è null");

        /**Inizializzo le variabili*/
        gateThread = Executors.newSingleThreadExecutor( t -> {
            Thread thread = Executors.defaultThreadFactory().newThread(t);
            thread.setDaemon(true);
            return thread;
        });

        this.maxBlockTime = maxBlockTime;
        checkerThread = null;
        gameFactory = null;
        currentGame = null;
        lastMove = null;
        iPP = new ArrayList<>();
        minBetweenBlockAndOut = 0;
        observer = obs;
        runningMatch = false;
        interruptedMatch = false;
        failure = false;
        maxTh = 0;
        fjpSize = 0;
        bgExecSize = 0;
        timeout = 0;
        tol = 0;
        minTime = 0;

    }

    /** Imposta la {@link GameFactory} con il nome dato. Usa {@link GameFactories}
     * per creare la GameFactory nel thread di confinamento. Se già c'era una
     * GameFactory impostata, la sostituisce con la nuova e se c'erano anche
     * PlayerFactory impostate le cancella. Però se c'è una partita in corso,
     * fallisce.
     * @param name  nome di una GameFactory
     * @throws NullPointerException se {@code name} è null
     * @throws IllegalArgumentException se {@code name} non è il nome di una
     * GameFactory
     * @throws IllegalStateException se la creazione della GameFactory fallisce o se
     * c'è una partita in corso. */
    public void setGameFactory(String name) {
        final boolean[] exceptions = new boolean[2];

        if(name == null)
            throw new NullPointerException("name è null");

        if (runningMatch)
            throw new IllegalStateException("Partita gia in corso!");

        if (!Arrays.asList(GameFactories.availableBoardFactories()).contains(name))
            throw new IllegalArgumentException("Nessun gioco con questo nome!");

        gateThread.submit(() -> {

            try {

                if (gameFactory != null)
                    iPP.clear();

                gameFactory = GameFactories.getBoardFactory(name);

            } catch (IllegalArgumentException e) {
                failure = true;
                exceptions[0] = true;
                exceptions[1] = true;
                return;
            }

            exceptions[1] = true;
        });

        while (!exceptions[1])
            try {Thread.sleep(10);} catch (Exception ignored) {}

        if (exceptions[0])
            throw new IllegalStateException("Fallita creazione della gameFactory");

    }

    /** Ritorna i nomi dei parametri della {@link GameFactory} impostata. Se la
     * GameFactory non ha parametri, ritorna un array vuoto.
     * @return i nomi dei parametri della GameFactory impostata
     * @throws IllegalStateException se non c'è una GameFactory impostata */
    public String[] getGameFactoryParams() {

        if (gameFactory == null)
            throw new IllegalStateException("Gioco non impostato");

        String[] arrayParamName = new String[gameFactory.params().size()];

        for (int i = 0; i < gameFactory.params().size(); i++) {
            arrayParamName[i] = ((Param) gameFactory.params().get(i)).name();
        }

        return arrayParamName;
    }

    /** Ritorna il prompt del parametro con il nome specificato della
     * {@link GameFactory} impostata.
     * @param paramName  nome del parametro
     * @return il prompt del parametro con il nome specificato della GameFactory
     * impostata.
     * @throws NullPointerException se {@code paramName} è null
     * @throws IllegalArgumentException se la GameFactory impostata non ha un
     * parametro di nome {@code paramName}
     * @throws IllegalStateException se non c'è una GameFactory impostata */
    public String getGameFactoryParamPrompt(String paramName) {

        if (paramName == null)
            throw new NullPointerException("Nome parametro null");

        if (gameFactory == null)
            throw new IllegalStateException("Gioco non impostato");

        for(Param p : ((List<Param>) gameFactory.params()))
            if (p.name().equals(paramName))
                return  p.prompt();

        throw new IllegalArgumentException("Nessuna corrispondenza tra i paramentri ottenuti dall'attuale gameFactory");

    }

    /** Ritorna i valori ammissibili per il parametro con nome dato della
     * {@link GameFactory} impostata.
     * @param paramName  nome del parametro
     * @return i valori ammissibili per il parametro della GameFactory impostata
     * @throws NullPointerException se {@code paramName} è null
     * @throws IllegalArgumentException se la GameFactory impostata non ha un
     * parametro di nome {@code paramName}
     * @throws IllegalStateException se non c'è una GameFactory impostata */
    public Object[] getGameFactoryParamValues(String paramName) {

        if (paramName == null)
            throw new NullPointerException("Nome parametro null");

        if (gameFactory == null)
            throw new IllegalStateException("Gioco non impostato");

        for(Param p : ((List<Param>) gameFactory.params()))
            if (p.name().equals(paramName))
                return p.values().toArray(new Object[p.values().size()]);

        throw new IllegalArgumentException("Nessuna corrispondenza tra i paramentri ottenuti dall'attuale gameFactory");
    }

    /** Ritorna il valore del parametro di nome dato della {@link GameFactory}
     * impostata.
     * @param paramName  nome del parametro
     * @return il valore del parametro della GameFactory impostata
     * @throws NullPointerException se {@code paramName} è null
     * @throws IllegalArgumentException se la GameFactory impostata non ha un
     * parametro di nome {@code paramName}
     * @throws IllegalStateException se non c'è una GameFactory impostata */
    public Object getGameFactoryParamValue(String paramName) {

        if (paramName == null)
            throw new NullPointerException("Nome parametro null");

        if (gameFactory == null)
            throw new IllegalStateException("Gioco non impostato");

        for(Param p : ((List<Param>) gameFactory.params()))
            if (p.name().equals(paramName))
                return p.get();

        throw new IllegalArgumentException("Nessuna corrispondenza tra i paramentri ottenuti dall'attuale gameFactory");
    }

    /** Imposta il valore del parametro di nome dato della {@link GameFactory}
     * impostata.
     * @param paramName  nome del parametro
     * @param value  un valore ammissibile per il parametro
     * @throws NullPointerException se {@code paramName} o {@code value} è null
     * @throws IllegalArgumentException se la GameFactory impostata non ha un
     * parametro di nome {@code paramName} o {@code value} non è un valore
     * ammissibile per il parametro
     * @throws IllegalStateException se non c'è una GameFactory impostata o è già
     * stato impostata la PlayerFactory di un giocatore */
    public void setGameFactoryParamValue(String paramName, Object value) {
        final boolean[] exceptions = new boolean[3];

        if (paramName == null || value == null)
            throw new NullPointerException("Nome parametro oppure valore null");

        if (gameFactory == null)
            throw new IllegalStateException("Gioco non impostato");

        for (Triple<P> t : iPP)
            if (t.getThird() != null)
                throw new IllegalStateException("C'è già almeno una playerFactory impostata");

        gateThread.submit(() -> {

            for(Param p : ((List<Param>) gameFactory.params()))
                if (p.name().equals(paramName)) {

                    if (p.values().contains(value)) {
                        p.set(value);
                        exceptions[2] = true;
                        return;
                    }

                    else {
                        exceptions[0] = true;
                        exceptions[2] = true;
                        return;
                    }
                }

            exceptions[1] = true;
            exceptions[2] = true;
        });

        while (!exceptions[2])
            try {Thread.sleep(10);} catch (Exception ignored) {}

        if (exceptions[0])
            throw new IllegalArgumentException("Il valore non è tra quelli ammissibili per l'attuale gameFactory");

        else if (exceptions[1])
            throw new IllegalArgumentException("Nessuna corrispondenza tra i paramentri ottenuti dall'attuale gameFactory");

    }

    /** Imposta un {@link PlayerGUI} con il nome e il master dati per il giocatore
     * di indice {@code pIndex}. Se c'era già un giocatore impostato per quell'indice,
     * lo sostituisce.
     * @param pIndex  indice di un giocatore
     * @param pName  nome del giocatore
     * @param master  il master
     * @throws NullPointerException se {@code pName} o {@code master} è null
     * @throws IllegalArgumentException se {@code pIndex} non è un indice di giocatore
     * valido per la GameFactory impostata
     * @throws IllegalStateException se non c'è una GameFactory impostata o se c'è
     * una partita in corso. */
    public void setPlayerGUI(int pIndex, String pName, Consumer<MoveChooser<P>> master) {
        final boolean[] exceptions = new boolean[1];

        if (pName == null || master == null)
            throw new NullPointerException("master o/e nome è/sono null");

        if (runningMatch)
            throw new IllegalStateException("Partita gia in corso!");

        if (pIndex < 0 || pIndex > gameFactory.maxPlayers())
            throw new IllegalArgumentException("pindex non è l indice di nessun giocatore");

        if (gameFactory == null)
            throw new IllegalStateException("Nessun gioco impostato");

        gateThread.submit(() -> {

            for (Triple<P> t : iPP) {
                if (t.getFirst().equals(pIndex)) {
                    t.setSecond(new PlayerGUI<>(pName, master));
                    t.setThird(null);
                    exceptions[0] = true;
                    return;
                }
            }

            iPP.add(new Triple<>(pIndex, new PlayerGUI<>(pName, master), null));
            exceptions[0] = true;
        });

        while (!exceptions[0])
            try {Thread.sleep(10);} catch (Exception ignored) {}

    }

    /** Imposta la {@link PlayerFactory} con nome dato per il giocatore di indice
     * {@code pIndex}. Usa {@link PlayerFactories} per creare la PlayerFactory nel
     * thread di confinamento. La PlayerFactory è impostata solamente se il metodo
     * ritorna {@link PlayerFactory.Play#YES}. Se c'era già un giocatore impostato
     * per quell'indice, lo sostituisce.
     * @param pIndex  indice di un giocatore
     * @param fName  nome di una PlayerFactory
     * @param pName  nome del giocatore
     * @param dir  la directory della PlayerFactory o null
     * @return un valore (vedi {@link PlayerFactory.Play}) che informa sulle
     * capacità dei giocatori di questa fabbrica di giocare al gioco specificato.
     * @throws NullPointerException se {@code fName} o {@code pName} è null
     * @throws IllegalArgumentException se {@code pIndex} non è un indice di giocatore
     * valido per la GameFactory impostata o se non esiste una PlayerFactory di nome
     * {@code fName}
     * @throws IllegalStateException se la creazione della PlayerFactory fallisce o
     * se non c'è una GameFactory impostata o se c'è una partita in corso. */
    public PlayerFactory.Play setPlayerFactory(int pIndex, String fName, String pName, Path dir) {
        final PlayerFactory.Play[] playerSkill = {null};
        final boolean[] exceptions = new boolean[2];

        if (pName == null || fName == null)
            throw new NullPointerException("pnmame o fname sono null");

        if (gameFactory == null || runningMatch)
            throw new IllegalStateException("Nessuna partita impostata o partita in corso!");

        if (pIndex < 0 || pIndex > gameFactory.maxPlayers())
            throw new IllegalArgumentException("pindex non è l indice di nessun giocatore");

        if (!Arrays.asList(PlayerFactories.availableBoardFactories()).contains(fName))
            throw new IllegalArgumentException("Nessun player di questo tipo!");

        gateThread.submit(() -> {

            PlayerFactory playerFactory;

            try {
                playerFactory = PlayerFactories.getBoardFactory(fName);

            } catch (IllegalArgumentException e) {
                failure = true;
                exceptions[0] = true;
                exceptions[1] = true;
                return;
            }

            playerFactory.setDir(dir);

            if (playerFactory.canPlay(gameFactory).equals(PlayerFactory.Play.YES)) {

                for (Triple<P> t : iPP) {
                    if (t.getFirst().equals(pIndex)) {
                        t.setSecond((Player) playerFactory.newPlayer(gameFactory, pName));
                        t.setThird(playerFactory);
                    }
                }

                iPP.add(new Triple<>(pIndex, (Player) playerFactory.newPlayer(gameFactory, pName), playerFactory));

                playerSkill[0] = PlayerFactory.Play.YES;
                exceptions[1] = true;
                return;
            }

            playerSkill[0] = playerFactory.canPlay(gameFactory);
            exceptions[1] = true;
        });

        while (!exceptions[1])
            try {Thread.sleep(10);} catch (Exception ignored) {}

        if (exceptions[0])
            throw new IllegalStateException("Creazione della PlayerFactory fallita!");

        return playerSkill[0];
    }

    /** Ritorna i nomi dei parametri della {@link PlayerFactory} di indice
     * {@code pIndex}. Se la PlayerFactory non ha parametri, ritorna un array vuoto.
     * @param pIndex  indice di un giocatore
     * @return i nomi dei parametri della PlayerFactory di indice dato
     * @throws IllegalArgumentException se non c'è una PlayerFactory di indice
     * {@code pIndex} */
    public String[] getPlayerFactoryParams(int pIndex) {

        for (Triple<P> t : iPP) {
            if (t.getFirst().equals(pIndex) && t.getThird() != null) {

                String[] arrayParamName = new String[t.getThird().params().size()];

                for (int i = 0; i < t.getThird().params().size(); i++) {
                    arrayParamName[i] = ((Param) t.getThird().params().get(i)).name();
                }

                return arrayParamName;
            }
        }

        throw new IllegalArgumentException("Nessuna playerFactory di indice pIndex");
    }

    /** Ritorna il prompt del parametro con il nome specificato della
     * {@link PlayerFactory} di indice {@code pIndex}.
     * @param pIndex  indice di un giocatore
     * @param paramName  nome del parametro
     * @return il prompt del parametro con il nome specificato della PlayerFactory
     * di indice dato
     * @throws NullPointerException se {@code paramName} è null
     * @throws IllegalArgumentException se la PlayerFactory non ha un parametro di
     * nome {@code paramName} o non c'è una PlayerFactory di indice {@code pIndex} */
    public String getPlayerFactoryParamPrompt(int pIndex, String paramName) {

        if (paramName == null)
            throw new NullPointerException();

        for (Triple<P> t : iPP)
            if (t.getFirst().equals(pIndex) && t.getThird() != null) {
                for (Param p : ((List<Param>) t.getThird().params()))
                    if (p.name().equals(paramName))
                        return p.prompt();

                throw new IllegalArgumentException("Nessun parametro di nome paramName");
            }

        throw new IllegalArgumentException("Nessuna playerFactory di indice pIndex");
    }

    /** Ritorna i valori ammissibili per il parametro di nome dato della
     * {@link PlayerFactory} di indice {@code pIndex}.
     * @param pIndex  indice di un giocatore
     * @param paramName  nome del parametro
     * @return i valori ammissibili per il parametro di nome dato della PlayerFactory
     * di indice dato.
     * @throws NullPointerException se {@code paramName} è null
     * @throws IllegalArgumentException se la PlayerFactory non ha un parametro di
     * nome {@code paramName} o non c'è una PlayerFactory di indice {@code pIndex} */
    public Object[] getPlayerFactoryParamValues(int pIndex, String paramName) {

        if (paramName == null)
            throw new NullPointerException();

        for (Triple<P> t : iPP)
            if (t.getFirst().equals(pIndex) && t.getThird() != null) {
                for (Param p : ((List<Param>) t.getThird().params()))
                    if (p.name().equals(paramName))
                        return p.values().toArray(new Object[p.values().size()]);

                throw new IllegalArgumentException("Nessun parametro di nome paramName");
            }

        throw new IllegalArgumentException("Nessuna playerFactory di indice pIndex");
    }

    /** Ritorna il valore del parametro di nome dato della {@link PlayerFactory} di
     * indice {@code pIndex}.
     * @param pIndex  indice di un giocatore
     * @param paramName  nome del parametro
     * @return il valore del parametro di nome dato della PlayerFactory di indice
     * dato
     * @throws NullPointerException se {@code paramName} è null
     * @throws IllegalArgumentException se la PlayerFactory non ha un parametro di
     * nome {@code paramName} o non c'è una PlayerFactory di indice {@code pIndex} */
    public Object getPlayerFactoryParamValue(int pIndex, String paramName) {

        if (paramName == null)
            throw new NullPointerException();

        for (Triple<P> t : iPP)
            if (t.getFirst().equals(pIndex) && t.getThird() != null) {
                for (Param p : ((List<Param>) t.getThird().params()))
                    if (p.name().equals(paramName))
                        return p.get();

                throw new IllegalArgumentException("Nessun parametro di nome paramName");
            }

        throw new IllegalArgumentException("Nessuna playerFactory di indice pIndex");

    }

    /** Imposta il valore del parametro di nome dato della {@link PlayerFactory}
     * di indice {@code pIndex}.
     * @param pIndex  indice di un giocatore
     * @param paramName  nome del parametro
     * @param value  un valore ammissibile per il parametro
     * @throws NullPointerException se {@code paramName} o {@code value} è null
     * @throws IllegalArgumentException se la PlayerFactory non ha un parametro di
     * nome {@code paramName} o {@code value} non è un valore ammissibile per il
     * parametro o non c'è una PlayerFactory di indice {@code pIndex}
     * @throws IllegalStateException se c'è una partita in corso */
    public void setPlayerFactoryParamValue(int pIndex, String paramName, Object value) {
        final boolean[] exceptions = new boolean[4];

        if (paramName == null || value == null)
            throw new NullPointerException("ParamName o value null");

        if (runningMatch)
            throw new IllegalStateException("Partita già in corso");

        gateThread.submit(() -> {

            for (Triple<P> t : iPP)
                if (t.getFirst().equals(pIndex) && t.getThird() != null) {
                    for (Param p : ((List<Param>) t.getThird().params()))
                        if (p.name().equals(paramName)) {

                            if (p.values().contains(value)) {
                                p.set(value);
                                t.setSecond((Player) t.getThird().newPlayer(gameFactory, t.getSecond().name()));
                                exceptions[3] = true;
                                return;
                            }

                            else {
                                exceptions[0] = true;
                                exceptions[3] = true;
                                return;
                            }
                        }

                    exceptions[1] = true;
                    exceptions[3] = true;
                    return;
                }

            exceptions[2] = true;
            exceptions[3] = true;
        });

        while (!exceptions[3])
            try {Thread.sleep(10);} catch (Exception ignored) {}

        if (exceptions[0])
            throw new IllegalArgumentException("Il valore non è tra quelli ammissibili per l'attuale gameFactory");

        else if (exceptions[1])
            throw new IllegalArgumentException("Nessun parametro di nome paramName");

        else if (exceptions[2])
            throw new IllegalArgumentException("Nessuna playerFactory di indice pIndex");

    }

    /** Inizia una partita con un gioco fabbricato dalla GameFactory impostata e i
     * giocatori forniti da {@link PlayerGUI} impostati o fabbricati dalle
     * PlayerFactory impostate. Se non c'è una GameFactory impostata o non ci sono
     * sufficienti giocatori impostati o c'è già una partita in corso, fallisce. Se
     * sono impostati dei vincoli sui thread per le invocazioni di
     * {@link Player#getMove}, allora prima di iniziare la partita invoca i metodi
     * {@link Player#threads(int, ForkJoinPool, ExecutorService)} di tutti i giocatori,
     * ovviamente nel thread di confinamento.
     * <br>
     * Il metodo ritorna immediatamente, non attende che la partita termini. Quindi
     * usa un thread per gestire la partita oltre al thread di confinamento usato
     * per l'invocazione di tutti i metodi del GameRuler e dei Player.
     * @param tol  massimo numero di millisecondi di tolleranza per le mosse, cioè se
     *             il gioco ha un tempo limite <i>T</i> per le mosse, allora il tempo di
     *             attesa sarà <i>T</i> + {@code tol}; se {@code tol} <= 0, allora
     *             nessuna tolleranza
     * @param timeout  massimo numero di millisecondi per le invocazioni dei metodi
     *                 dei giocatori escluso {@link Player#getMove()}, se <= 0,
     *                 allora nessun limite
     * @param minTime  minimo numero di millisecondi tra una mossa e quella successiva,
     *                 se <= 0, allora nessuna pausa
     * @param maxTh  massimo numero di thread addizionali permessi per
     *               {@link Player#getMove()}, se < 0, nessun limite è imposto
     * @param fjpSize  numero di thread per il {@link ForkJoinTask ForkJoin} pool,
     *                 se == 0, non è permesso alcun pool, se invece è < 0, non c'è
     *                 alcun vincolo e possono usare anche
     *                 {@link ForkJoinPool#commonPool() Common Pool}
     * @param bgExecSize  numero di thread permessi per esecuzioni in background, se
     *                    == 0, non sono permessi, se invece è < 0, non c'è alcun
     *                    vincolo
     * @throws IllegalStateException se non c'è una GameFactory impostata o non ci
     * sono sufficienti PlayerFactory impostate o la creazione del GameRuler o quella
     * di qualche giocatore fallisce o se già c'è una partita in corso. */
    public void play(long tol, long timeout, long minTime, int maxTh, int fjpSize, int bgExecSize) {
        this.tol = tol; this.timeout = timeout; this.minTime = minTime;
        this.maxTh = maxTh; this.fjpSize = fjpSize; this.bgExecSize = bgExecSize;
        final boolean[] exceptions = new boolean[2];

        if (runningMatch || gameFactory == null)
            throw new IllegalStateException("Partita gia in corso!");

        if (failure)
            throw new IllegalStateException("Fallimento della creazione di una PlayerFactory o del GameFactory");

        int players = iPP.size();

        if (players < gameFactory.minPlayers() || players > gameFactory.maxPlayers())
            throw new IllegalStateException("Numero di giocatori non consentito");

        /**Check finale*/
        gateThread.submit(() -> {

            List<Integer> list = new ArrayList<>();

            for (Triple<P> t : iPP)
                list.add(t.getFirst());

            for (int i = 1; i <= players; i++)
                if (!list.contains(i)) {
                    exceptions[0] = true;
                    exceptions[1] = true;
                    return;
                }

            list.clear();
            exceptions[1] = true;
        });

        while (!exceptions[1])
            try {Thread.sleep(10);} catch (Exception ignored) {}

        if (exceptions[0])
            throw new IllegalStateException("Manca qualche giocatore!");

        /**Inizio vero e proprio della partita*/
        gateThread.submit(matchManager());

        runningMatch = true;
    }

    /** Se c'è una partita in corso la termina immediatamente e ritorna true,
     * altrimenti non fa nulla e ritorna false.
     * @return true se termina la partita in corso, false altrimenti */
    public boolean stop() {

        if (runningMatch) {
            observer.interrupted("La partita e' stata interrotta!");

            if (isGUI) interrupt = true;

            gateThread.shutdownNow();
            checkerThread.interrupt();
            interruptedMatch = true;

            while (runningMatch)
                try {Thread.sleep(5);} catch (InterruptedException ignored) {}


            return true;
        }

        return false;
    }

    /**
     * Prepara l' oggetto {@code PlayGUI} per essere riutilizzabile una volta che la partita
     * è terminata, normalmente o a causa di un interruzione.
     */
    private void reset() {

        /**Garbage Collector, elimina dalla memoria oggetti "abbandonati", evitando i memory leak*/
        System.gc();

        /**Reinizializzo le variabili*/
        gateThread = Executors.newSingleThreadExecutor( t -> {
            Thread thread = Executors.defaultThreadFactory().newThread(t);
            thread.setDaemon(true);
            return thread;
        });

        checkerThread = null;
        gameFactory = null;
        currentGame = null;
        lastMove = null;
        iPP = new ArrayList<>();
        minBetweenBlockAndOut = 0;
        runningMatch = false;
        interruptedMatch = false;
        failure = false;
        maxTh = 0;
        fjpSize = 0;
        bgExecSize = 0;
        timeout = 0;
        tol = 0;
        minTime = 0;

    }

    /**
     * Ritorna un thread, che definiamo controllore, il quale monitora il tempo
     * di esecuzione dei metodi dell' oggetto interessato assicurandosi che quest' ultimi
     * non li violino. Gestisce il caso della violazione notificandola all' {@link Observer}
     * che immediatamente interrompe la partita comunicando il tipo di violazione avvenuta.
     * @param o l'oggetto controllato dal thread
     * @param millis il tempo massimo per l' esecuzione di un metodo dell' oggetto
     * @param gameSet true se il metodo ha rispettato il limite, false altrimenti
     * @param arbitro l' {@link Observer}
     * @param msg il messaggio che comunica la violazione avvenuta
     * @param index l' indice del giocatore che ha commesso la violazione
     * @return il thread controllore
     */
    private Thread checkerThread(Object o, long millis, boolean[] gameSet, Observer<P> arbitro, String msg, int index) {

        return new Thread(() -> {

            if (millis >= 0) {

                synchronized (o) {

                    if (millis == 0) {
                        arbitro.limitBreak(index, msg);
                        interrupt = true;
                        interruptedMatch = true;
                        return;
                    }

                    else
                        try { o.wait(millis); } catch (InterruptedException e) {}

                    if (!gameSet[0]) {
                        arbitro.limitBreak(index, msg);
                        interrupt = true;
                        interruptedMatch = true;
                    }

                }

            }

        });
    }

    /**
     * Ritorna un oggetto {@link Runnable} che verrà sottomesso al thread di confinamento,
     * nel runnable avviene l' intera esecuzione di una partita.
     * @return il task che gestisce l' esecuzione di una partita
     */
    private Runnable matchManager() {

        return () -> {

            /**Ordina i giocatori in base al turno*/
            iPP = tripleSort(iPP);

            String[] playerNames = new String[0];
            for (Triple<P> t : iPP) {
                playerNames = Arrays.copyOf(playerNames, playerNames.length+1);
                playerNames[playerNames.length-1] = t.getSecond().name();
            }

            gameFactory.setPlayerNames(playerNames);

            if (!(maxTh < 0 && fjpSize < 0 && bgExecSize < 0)) {
                TripleThread threadManager = new TripleThread(maxTh, fjpSize, bgExecSize);

                for (Triple<P> t : iPP)
                    t.getSecond().threads(threadManager.getFirst(), threadManager.getSecond(), threadManager.getThird());

            }

            currentGame = (GameRuler<P>) gameFactory.newGame();
            observer.setGame(currentGame.copy());

            /**Attende, se sto giocando tramite GUI, che la board si disegni*/
            if (isGUI) while (!canContinuePlaying) ;

            if (maxBlockTime < 0 && timeout <= 0)
                minBetweenBlockAndOut = -1;

            else if (maxBlockTime < 0 && timeout > 0)
                minBetweenBlockAndOut = timeout;

            else if (maxBlockTime >= 0 && timeout <= 0)
                minBetweenBlockAndOut = maxBlockTime;

            else if (maxBlockTime >= 0 && timeout > 0)
                minBetweenBlockAndOut = Math.min(maxBlockTime, timeout);

            /**Inizio della partita (NO TIMEOUT)*/
            if (minBetweenBlockAndOut == -1) {

                for (Triple<P> t : iPP)
                    t.getSecond().setGame(currentGame.copy());

                long time = currentGame.mechanics().time == -1 ? -1 : currentGame.mechanics().time + (tol > 0 ? tol : 0);

                /**Ciclo di gioco*/
                while (currentGame.result() == -1 && !interruptedMatch) {

                    if (isGUI)
                        if (time != -1)
                            game.counterThread();

                    final boolean[] gotMove = {false};

                    checkerThread = checkerThread(iPP, time, gotMove,
                            observer, iPP.get(currentGame.turn() - 1).getSecond().name() + " non ha rispettato i limiti di tempo!",
                            iPP.get(currentGame.turn() - 1).getFirst());

                    checkerThread.setDaemon(true);
                    checkerThread.start();


                    /**Prelevo la mossa del giocatore*/
                    lastMove = iPP.get(currentGame.turn() - 1).getSecond().getMove();

                    if (isGUI) {
                        canContinuePlaying = false;
                        game.stopCounting = true;
                    }

                    gotMove[0] = true;
                    synchronized (iPP) {
                        iPP.notifyAll();
                    }

                    checkerThread.interrupt();
                    if (interruptedMatch)
                        break;

                    /**Se la mossa è di tipo RESIGN, lo comunico all'observer e esco*/
                    if (lastMove.kind.equals(Move.Kind.RESIGN)) {

                        if (!playerGUImanualRESIGN)
                            observer.limitBreak(currentGame.turn(), iPP.get(currentGame.turn() - 1).getSecond().name() + " non ha rispettato i limiti di tempo!");
                        else
                            observer.limitBreak(currentGame.turn(), iPP.get(currentGame.turn() - 1).getSecond().name() + " si e' arreso!");

                        break;
                    }

                    /**Comunico ad ogni giocatore la mossa effettuata*/
                    for (Triple<P> t : iPP)
                        t.getSecond().moved(currentGame.turn(), lastMove);

                    observer.moved(currentGame.turn(), lastMove);

                    /**Aggiorno lo stato della partita principale*/
                    currentGame.move(lastMove);

                    if (isGUI && !interrupt) {
                        boolean[] done = {false};

                        game.infoUpdater(done);

                        while (!done[0] && !interrupt)
                            try {Thread.sleep(5);} catch (Exception ignored) {}

                    }

                    /**Garbage Collector, elimina dalla memoria oggetti "abbandonati", evitando i memory leak*/
                    System.gc();

                    try {Thread.sleep(minTime);} catch (InterruptedException ignored) {}

                    if (isGUI) while (!canContinuePlaying) try {Thread.sleep(5);} catch (InterruptedException ignored) {}
                }

                if (!interruptedMatch)
                    for (Triple<P> t : iPP)
                        if (t.getFirst() == currentGame.result())
                            break;

            }

            /**Inizio della partita (WITH TIMEOUT)*/
            else {

                for (Triple<P> t : iPP) {
                    final boolean[] gameSet = {false};

                    checkerThread = checkerThread(t, minBetweenBlockAndOut, gameSet, observer, "Impossibile creare una partita per " + t.getSecond().name(), t.getFirst());
                    checkerThread.setDaemon(true);
                    checkerThread.start();

                    t.getSecond().setGame(currentGame.copy());
                    gameSet[0] = true;
                    synchronized (t) { t.notifyAll(); }

                    if (interruptedMatch)
                        break;

                }

                checkerThread.interrupt();
                if (gateThread.isShutdown())
                    return;

                long time = currentGame.mechanics().time == -1 ? -1 : currentGame.mechanics().time + (tol > 0 ? tol : 0);

                /**Ciclo di gioco*/
                while (currentGame.result() == -1 && !interruptedMatch) {

                    if (isGUI)
                        if (time != -1)
                            game.counterThread();

                    final boolean[] gotMove = {false};

                    checkerThread = checkerThread(iPP, time, gotMove,
                            observer, iPP.get(currentGame.turn() - 1).getSecond().name() + " non ha rispettato i limiti di tempo!",
                            iPP.get(currentGame.turn() - 1).getFirst());

                    checkerThread.setDaemon(true);
                    checkerThread.start();


                    /**Prelevo la mossa del giocatore*/
                    lastMove = iPP.get(currentGame.turn() - 1).getSecond().getMove();

                    if (isGUI) {
                        canContinuePlaying = false;
                        game.stopCounting = true;
                    }

                    gotMove[0] = true;

                    synchronized (iPP) {
                        iPP.notifyAll();
                    }

                    checkerThread.interrupt();
                    if (interruptedMatch)
                        break;

                    /**Se la mossa è di tipo RESIGN, lo comunico all'observer e esco*/
                    if (lastMove.kind.equals(Move.Kind.RESIGN)) {

                        if (!playerGUImanualRESIGN)
                            observer.limitBreak(currentGame.turn(), iPP.get(currentGame.turn() - 1).getSecond().name() + " non ha rispettato i limiti di tempo!");
                        else
                            observer.limitBreak(currentGame.turn(), iPP.get(currentGame.turn() - 1).getSecond().name() + " si e' arreso!");

                        break;
                    }

                    /**Comunico ad ogni giocatore la mossa effettuata*/
                    for (Triple<P> x : iPP) {
                        final boolean[] gameSet = {false};

                        checkerThread = checkerThread(x, minBetweenBlockAndOut, gameSet, observer, "Impossibile aggiornare lo stato del gioco per " + x.getSecond().name(), x.getFirst());
                        checkerThread.setDaemon(true);
                        checkerThread.start();

                        x.getSecond().moved(currentGame.turn(), lastMove);
                        gameSet[0] = true;
                        synchronized (x) { x.notifyAll(); }

                        if (interruptedMatch)
                            break;

                    }

                    checkerThread.interrupt();
                    if (interruptedMatch)
                        break;

                    observer.moved(currentGame.turn(), lastMove);

                    /**Aggiorno lo stato della partita principale*/
                    currentGame.move(lastMove);

                    if (isGUI) {
                        boolean[] done = {false};

                        game.infoUpdater(done);

                        while (!done[0] && !interrupt)
                            try {Thread.sleep(5);} catch (Exception ignored) {}
                    }

                    /**Garbage Collector, elimina dalla memoria oggetti "abbandonati", evitando i memory leak*/
                    System.gc();

                    try {Thread.sleep(minTime);} catch (InterruptedException ignored) {}

                    if (isGUI) while (!canContinuePlaying) try {Thread.sleep(5);} catch (InterruptedException ignored) {}
                }

                if (!interruptedMatch)
                    for (Triple<P> t : iPP)
                        if (t.getFirst() == currentGame.result())
                            break;

            }

            if (isGUI) interrupt = true;
            runningMatch = false;

            reset();

        };
    }

    /**
     * @return la factory selezioanta
     */
    public GameFactory getGameFactory() {
        return gameFactory;
    }

    /**
     * @return il gioco corrente
     */
    public GameRuler<P> getCurrentGame() {
        return currentGame;
    }

    /**
     * @return la lista di triple contenenti le associazioni dei Player e delle PlayerFactory
     */
    public List<Triple<P>> getiPP() {
        return iPP;
    }

    /**
     * @return l' ultima mossa eseguita
     */
    public Move<P> getLastMove() {
        return lastMove;
    }

    /**
     * @return se la partita è in corso o meno
     */
    public boolean isRunningMatch() {
        return runningMatch;
    }

    /**
     * Presa in input una lista di {@code Triple} disordinata, la riordina
     * in maniera crescente secondo l' indice di turnazione dei giocatori.
     * @param list la lista da ordinare
     * @param <P> tipo del modello dei pezzi
     * @return la lista ordinata
     * @see Triple
     */
    private static <P> List<Triple<P>> tripleSort(List<Triple<P>> list) {
        List<Triple<P>> newList = new ArrayList<>();

        int index = 1;

        while (true) {
            for (Triple<P> t : list)
                if (t.getFirst() == index)
                    newList.add(t);

            index++;

            if (index == list.size()+1)
                break;
        }

        return newList;
    }

    /**
     * Un {@code Triple} associa tre differenti oggetti, un {@link Integer}
     * un {@link Player} ed un {@link PlayerFactory}; è utile per risalire,
     * a partire dall' indice di turnazione di un giocatore, a che tipo di
     * giocatore esso sia o alla relativa factory che l' ha creato.
     * @param <P> tipo del modello dei pezzi
     */
    public static class Triple<P> {

        /**
         * L'indice di turnazione di un giocatore
         */
        private Integer first;

        /**
         * Il tipo di giocatore
         */
        private Player<P> second;

        /**
         * La factory del giocatore
         */
        private PlayerFactory third;


        /**
         * Crea un oggetto {@code Triple} che associa tre differenti oggetti, un {@link Integer}
         * un {@link Player} ed un {@link PlayerFactory};
         * @param primo l'indice di turnazione di un giocatore
         * @param secondo il tipo di giocatore
         * @param terzo la factory del giocatore, null se non possiede nessuna factory
         */
        public Triple(Integer primo, Player<P> secondo, PlayerFactory terzo) {
            first = primo;
            second = secondo;
            third = terzo;
        }


        /**
         * @return l' indice di turnazione del giocatore
         */
        public Integer getFirst() {
            return first;
        }

        /**
         * Imposta il turno del giocatore
         * @param first il turno del giocatore
         */
        public void setFirst(Integer first) {
            this.first = first;
        }

        /**
         * @return il tipo di player
         */
        public Player<P> getSecond() {
            return second;
        }

        /**
         * Imposta il tipo di giocatore
         * @param second il giocatore
         */
        public void setSecond(Player<P> second) {
            this.second = second;
        }

        /**
         * @return la factory del giocatore
         */
        public PlayerFactory getThird() {
            return third;
        }

        /**
         * Imposta la factory del giocatore
         * @param third la factory del giocatore
         */
        public void setThird(PlayerFactory third) {
            this.third = third;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Triple<?> triple = (Triple<?>) o;

            if (first != null ? !first.equals(triple.first) : triple.first != null) return false;
            if (second != null ? !second.equals(triple.second) : triple.second != null) return false;
            return third != null ? third.equals(triple.third) : triple.third == null;

        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            result = 31 * result + (third != null ? third.hashCode() : 0);
            return result;
        }

    }

    /**
     * Un {@code TripleThread} è un oggetto che rappresenta i vincoli
     * computazionali che un player deve rispettare.
     * @see Player#threads(int, ForkJoinPool, ExecutorService)
     */
    private static class TripleThread {

        /**
         * Masssimo numero di thread addizionali per {@link Player#getMove()}
         */
        private int first;

        /**
         * Esecutore supplementare
         */
        private ForkJoinPool second;

        /**
         * Esecutore per calcoli in background
         */
        private ExecutorService third;

        /**
         * Crea un oggetto {@code TripleThread} che rappresenta i vincoli
         * computazionali che un player deve rispettare.
         * @param primo il massimo numero di thread addizionali per {@link Player#getMove()}
         * @param secondo pool per il framework {@link ForkJoinTask ForkJoin}, o null
         * @param terzo thread per l'esecuzione in background, può essere null
         */
        private TripleThread(int primo, int secondo, int terzo) {

            if (primo >= 0 && secondo < 0 && terzo < 0) {
                first = primo;
                second = null;
                third = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            }

            else if (primo >= 0 && secondo >= 0 && terzo < 0) {
                first = primo;

                if (secondo == 0)
                    second = null;

                else
                    second = new ForkJoinPool(secondo);

                third = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            }

            else if (primo >= 0 && secondo >= 0 && terzo >= 0) {
                first = primo;

                if (secondo == 0)
                    second = null;

                else
                    second = new ForkJoinPool(secondo);

                if (terzo == 0)
                    third = null;
                else
                    third = Executors.newFixedThreadPool(terzo);

            }

            else if (primo >= 0 && secondo < 0 && terzo >= 0) {
                first = primo;
                second = null;

                if (terzo == 0)
                    third = null;
                else
                    third = Executors.newFixedThreadPool(terzo);

            }

            else if (primo < 0 && secondo >= 0 && terzo < 0) {
                first = -1;

                if (secondo == 0)
                    second = null;

                else
                    second = new ForkJoinPool(secondo);

                third = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            }

            else if (primo < 0 && secondo >= 0 && terzo >= 0) {
                first = -1;

                if (secondo == 0)
                    second = null;
                else
                    second = new ForkJoinPool(secondo);

                if (terzo == 0)
                    third = null;
                else
                    third = Executors.newFixedThreadPool(terzo);

            }

            else if (primo < 0 && secondo < 0 && terzo >= 0) {
                first = -1;
                second = null;

                if (terzo == 0)
                    third = null;
                else
                    third = Executors.newFixedThreadPool(terzo);

            }

        }


        /**
         * @return il massimo numero di thread addizionali
         */
        private int getFirst() {
            return first;
        }

        /**
         * @return l' esecutore supplementare di tipo {@link ForkJoinPool}
         */
        private ForkJoinPool getSecond() {
            return second;
        }

        /**
         * @return l' esecutore per eventuali calcoli in background
         */
        private ExecutorService getThird() {
            return third;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TripleThread that = (TripleThread) o;

            if (first != that.first) return false;
            if (second != null ? !second.equals(that.second) : that.second != null) return false;
            return third != null ? third.equals(that.third) : that.third == null;

        }

        @Override
        public int hashCode() {
            int result = first;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            result = 31 * result + (third != null ? third.hashCode() : 0);
            return result;
        }
    }

}
