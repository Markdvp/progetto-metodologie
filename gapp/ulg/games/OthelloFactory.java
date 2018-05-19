package gapp.ulg.games;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;

import static gapp.ulg.game.board.PieceModel.Species;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Una OthelloFactory è una fabbrica di {@link GameRuler} per giocare a Othello.
 * I {@link GameRuler} fabbricati dovrebbero essere oggetti {@link Othello}. */
public class OthelloFactory implements GameFactory<GameRuler<PieceModel<Species>>> {
    /** Crea una fattoria di {@link GameRuler} per giocare a Othello */
    public OthelloFactory() {

        nomiGiocatori = new ArrayList<>();

        nome1 = "Time";
        prompt1 = "Time limit for a move";
        values1 = new ArrayList<>();
        values1.addAll(Arrays.asList("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"));
        default1 = "No limit";
        valoreAttuale1 = "No limit";

        nome2 = "Board";
        prompt2 = "Board size";
        values2 = new ArrayList<>();
        values2.addAll(Arrays.asList("6x6","8x8","10x10","12x12"));
        default2 = "8x8";
        valoreAttuale2 = "8x8";

        specName = "Othello8x8";

    }

    public List<String> nomiGiocatori;
    private String nome1;
    private String prompt1;
    private List<String> values1;
    private String default1;
    private String valoreAttuale1;
    private String nome2;
    private String prompt2;
    private List<String> values2;
    private String default2;
    private String valoreAttuale2;

    private String specName;

    @Override
    public String name() { return "Othello"; }
    public String getSpecName() { return specName; }

    @Override
    public int minPlayers() { return 2; }

    @Override
    public int maxPlayers() { return 2; }

    /** Ritorna una lista con i seguenti due parametri:
     * <pre>
     * Primo parametro, valori di tipo String
     *     - name: "Time"
     *     - prompt: "Time limit for a move"
     *     - values: ["No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"]
     *     - default: "No limit"
     * Secondo parametro, valori di tipo String
     *     - name: "Board"
     *     - prompt: "Board size"
     *     - values: ["6x6","8x8","10x10","12x12"]
     *     - default: "8x8"
     * </pre>
     * @return la lista con i due parametri */
    @Override
    @SuppressWarnings("unchecked")
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
        Param<String> secondoP = new Param<String>() {
            @Override
            public String name() {
                return nome2;
            }

            @Override
            public String prompt() {
                return prompt2;
            }

            @Override
            public List<String> values() {
                return Collections.unmodifiableList(values2);
            }

            @Override
            public void set(Object v) {
                if (values2.contains(v))
                    valoreAttuale2 = (String) v;
                else
                    throw new IllegalArgumentException();

                specName = "Othello" + valoreAttuale2;

            }

            @Override
            public String get() {
                return valoreAttuale2;
            }
        };

        listaParametri.add(primoP);
        listaParametri.add(secondoP);

        return Collections.unmodifiableList(listaParametri);
    }

    @Override
    public void setPlayerNames(String... names) {

        //Se uno dei nomi è null;
        for (String n : names)
            if (n == null)
                throw new NullPointerException();

        //Se il numero di giocatori non rispetta le regole;
        if (names.length < 0 || names.length > maxPlayers())
            throw new IllegalArgumentException();

        //Aggiungo i nomi alla lista dei giocatori;
        for (String n : names)
            nomiGiocatori.add(n);

    }

    @Override
    public GameRuler<PieceModel<Species>> newGame() {
        long tempoMax = 0;
        int grandezza = 0;

        //Se i nomi dei giocatori non sono stati impostati;
        if (nomiGiocatori.size() == 0)
            throw new IllegalStateException("Non sono stati impostati i nomi dei giocatori!");

        if (valoreAttuale1.equals("No limit"))
            tempoMax = -1;
        else if (valoreAttuale1.equals("1s"))
            tempoMax = 1000;
        else if (valoreAttuale1.equals("2s"))
            tempoMax = 2000;
        else if (valoreAttuale1.equals("3s"))
            tempoMax = 3000;
        else if (valoreAttuale1.equals("5s"))
            tempoMax = 5000;
        else if (valoreAttuale1.equals("10s"))
            tempoMax = 10000;
        else if (valoreAttuale1.equals("20s"))
            tempoMax = 20000;
        else if (valoreAttuale1.equals("30s"))
            tempoMax = 30000;
        else if (valoreAttuale1.equals("1m"))
            tempoMax = 60000;
        else if (valoreAttuale1.equals("2m"))
            tempoMax = 120000;
        else if (valoreAttuale1.equals("5m"))
            tempoMax = 300000;

        if (valoreAttuale2.equals("6x6"))
            grandezza = 6;
        else if (valoreAttuale2.equals("8x8"))
            grandezza = 8;
        else if (valoreAttuale2.equals("10x10"))
            grandezza = 10;
        else if (valoreAttuale2.equals("12x12"))
            grandezza = 12;

        return new Othello(tempoMax, grandezza, nomiGiocatori.get(0), nomiGiocatori.get(1));
    }
}
