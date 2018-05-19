package gapp.ulg.games;

import gapp.ulg.game.Param;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.GameFactory;

import static gapp.ulg.game.board.PieceModel.Species;

import java.util.*;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Una {@code MNKgameFactory} è una fabbrica di {@link GameRuler} per giocare a
 * (m,n,k)-game. I {@link GameRuler} fabbricati dovrebbero essere oggetti
 * {@link MNKgame}. */
public class MNKgameFactory implements GameFactory<GameRuler<PieceModel<Species>>> {

    public MNKgameFactory() {

        nomiGiocatori = new ArrayList<>();

        nome1 = "Time";
        prompt1 = "Time limit for a move";
        values1 = new ArrayList<>();
        values1.addAll(Arrays.asList("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"));
        default1 = "No limit";
        valoreAttuale1 = "No limit";

        nome2 = "M";
        prompt2 = "Board width";
        values2 = new ArrayList<>();
        values2.addAll(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20));
        default2 = 3;
        valoreAttuale2 = 3;

        nome3 = "N";
        prompt3 = "Board height";
        values3 = new ArrayList<>();
        values3.addAll(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20));
        default3 = 3;
        valoreAttuale3 = 3;

        nome4 = "K";
        prompt4 = "Length of line";
        values4 = new ArrayList<>();
        values4.addAll(Arrays.asList(1,2,3));
        default4 = 3;
        valoreAttuale4 = 3;

        specName = "3,3,3-game";

    }

    //Tempo
    public List<String> nomiGiocatori;
    private String nome1;
    private String prompt1;
    private List<String> values1;
    private String default1;
    private String valoreAttuale1;

    //M
    private String nome2;
    private String prompt2;
    private List<Integer> values2;
    private Integer default2;
    private Integer valoreAttuale2;

    //N
    private String nome3;
    private String prompt3;
    private List<Integer> values3;
    private Integer default3;
    private Integer valoreAttuale3;

    //K
    private String nome4;
    private String prompt4;
    private List<Integer> values4;
    private Integer default4;
    private Integer valoreAttuale4;

    private String specName;

    @Override
    public String name() { return "m,n,k-game"; }
    public String getSpecName() { return specName; }

    @Override
    public int minPlayers() { return 2; }

    @Override
    public int maxPlayers() { return 2; }

    /** Ritorna una lista con i seguenti quattro parametri:
     * <pre>
     * Primo parametro, valori di tipo String
     *     - name: "Time"
     *     - prompt: "Time limit for a move"
     *     - values: ["No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"]
     *     - default: "No limit"
     * Secondo parametro, valori di tipo Integer
     *     - name: "M"
     *     - prompt: "Board width"
     *     - values: [1,2,3,...,20]
     *     - default: 3
     * Terzo parametro, valori di tipo Integer
     *     - name: "N"
     *     - prompt: "Board height"
     *     - values: [1,2,3,...,20]
     *     - default: 3
     * Quarto parametro, valori di tipo Integer
     *     - name: "K"
     *     - prompt: "Length of line"
     *     - values: [1,2,3]
     *     - default: 3
     * </pre>
     * Per i parametri "M","N" e "K" i valori ammissibili possono cambiare a seconda
     * dei valori impostati. Più precisamente occorre che i valori ammissibili
     * garantiscano sempre le seguenti condizioni
     * <pre>
     *     1 <= K <= max{M,N} <= 20   AND   1 <= min{M,N}
     * </pre>
     * dove M,N,K sono i valori impostati. Indicando con minX, maxX il minimo e il
     * massimo valore per il parametro X le condizioni da rispettare sono:
     * <pre>
     *     minM <= M <= maxM
     *     minN <= N <= maxN
     *     minK <= K <= maxK
     *     minK = 1  AND  maxK = max{M,N}  AND  maxN = 20  AND  maxN = 20
     *     N >= K  IMPLICA  minM = 1
     *     N < K   IMPLICA  minM = K
     *     M >= K  IMPLICA  minN = 1
     *     M < K   IMPLICA  minN = K
     * </pre>
     * @return la lista con i quattro parametri */
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
        Param<Integer> secondoP = new Param<Integer>() {
            @Override
            public String name() {
                return nome2;
            }

            @Override
            public String prompt() {
                return prompt2;
            }

            @Override
            public List<Integer> values() {
                return Collections.unmodifiableList(values2);
            }

            @Override
            public void set(Object v) {

                if (values2.contains(v))
                    valoreAttuale2 = (Integer) v;
                else
                    throw new IllegalArgumentException();

                for (Integer i = 1 ; i <= Math.max(valoreAttuale2, valoreAttuale3) ; i++) {
                    if (!values4.contains(i))
                        values4.add(i);
                }

                List<Integer> nDaRimuovere = new ArrayList<>();
                for (Integer i : values4)
                    if (i > Math.max(valoreAttuale2, valoreAttuale3))
                        nDaRimuovere.add(i);
                values4.removeAll(nDaRimuovere);

                specName = "" + valoreAttuale2 + "," + valoreAttuale3 + "," + valoreAttuale4 + "-game";
            }

            @Override
            public Integer get() {
                return valoreAttuale2;
            }
        };
        Param<Integer> terzoP = new Param<Integer>() {
            @Override
            public String name() {
                return nome3;
            }

            @Override
            public String prompt() {
                return prompt3;
            }

            @Override
            public List<Integer> values() {
                return Collections.unmodifiableList(values3);
            }

            @Override
            public void set(Object v) {

                if (values3.contains(v))
                    valoreAttuale3 = (Integer) v;
                else
                    throw new IllegalArgumentException();

                for (Integer i = 1 ; i <= Math.max(valoreAttuale2, valoreAttuale3) ; i++) {
                    if (!values4.contains(i))
                        values4.add(i);
                }

                List<Integer> nDaRimuovere = new ArrayList<>();
                for (Integer i : values4)
                    if (i > Math.max(valoreAttuale2, valoreAttuale3))
                        nDaRimuovere.add(i);
                values4.removeAll(nDaRimuovere);

                specName = "" + valoreAttuale2 + "," + valoreAttuale3 + "," + valoreAttuale4 + "-game";
            }

            @Override
            public Integer get() {
                return valoreAttuale3;
            }
        };
        Param<Integer> quartoP = new Param<Integer>() {
            @Override
            public String name() {
                return nome4;
            }

            @Override
            public String prompt() {
                return prompt4;
            }

            @Override
            public List<Integer> values() {
                return Collections.unmodifiableList(values4);
            }

            @Override
            public void set(Object v) {

                if (values4.contains(v))
                    valoreAttuale4 = (Integer) v;
                else
                    throw new IllegalArgumentException();

                for (Integer i = 1 ; i <= Math.min(valoreAttuale2, valoreAttuale3) ; i++) {
                    if (!values4.contains(i))
                        values4.add(i);
                }

                List<Integer> nDaRimuovere = new ArrayList<>();
                for (Integer i : values4)
                    if (i > Math.max(valoreAttuale2, valoreAttuale3))
                        nDaRimuovere.add(i);
                values4.removeAll(nDaRimuovere);

                specName = "" + valoreAttuale2 + "," + valoreAttuale3 + "," + valoreAttuale4 + "-game";
            }

            @Override
            public Integer get() {
                return valoreAttuale4;
            }
        };

        listaParametri.add(primoP);
        listaParametri.add(secondoP);
        listaParametri.add(terzoP);
        listaParametri.add(quartoP);

        return Collections.unmodifiableList(listaParametri);
    }

    @Override
    public void setPlayerNames(String... names) {

        for (String n : names)
            if (n == null)
                throw new NullPointerException();

        if (names.length < 0 || names.length > maxPlayers())
            throw new IllegalArgumentException();

        for (String n : names)
            nomiGiocatori.add(n);

    }

    @Override
    public GameRuler<PieceModel<Species>> newGame() {
        long tempoMax = 0;

        //Se i nomi dei giocatori non sono stati impostati;
        if (nomiGiocatori.size() == 0)
            throw new IllegalStateException();

        //Imposto il valore tempo;
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

        return new MNKgame(tempoMax, valoreAttuale2, valoreAttuale3, valoreAttuale4, nomiGiocatori.get(0), nomiGiocatori.get(1));
    }

}