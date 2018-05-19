package gapp.ulg.play;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Player;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Una MCTSPlayerFactory è una fabbrica di {@link MCTSPlayer}.
 * @param <P>  tipo del modello dei pezzi */
public class MCTSPlayerFactory<P> implements PlayerFactory<Player<P>,GameRuler<P>> {

    public MCTSPlayerFactory() {

        nome1 = "Rollouts";
        prompt1 = "Number of rollouts per move";
        values1 = new ArrayList<>();
        values1.addAll(Arrays.asList(1,10,50,100,200,500,1000));
        default1 = 50;
        valoreAttuale1 = 50;

        nome2 = "Execution";
        prompt2 = "Threaded execution";
        values2 = new ArrayList<>();
        values2.addAll(Arrays.asList("Sequential","Parallel"));
        default2 = "Sequential";
        valoreAttuale2 = "Sequential";

    }

    private String nome1;
    private String prompt1;
    private List<Integer> values1;
    private Integer default1;
    private Integer valoreAttuale1;

    private String nome2;
    private String prompt2;
    private List<String> values2;
    private String default2;
    private String valoreAttuale2;

    @Override
    public String name() { return "Monte-Carlo Tree Search Player"; }

    @Override
    public void setDir(Path dir) { }

    /** Ritorna una lista con i seguenti due parametri:
     * <pre>
     * Primo parametro
     *     - name: "Rollouts"
     *     - prompt: "Number of rollouts per move"
     *     - values: [1,10,50,100,200,500,1000]
     *     - default: 50
     * Secondo parametro
     *     - name: "Execution"
     *     - prompt: "Threaded execution"
     *     - values: ["Sequential","Parallel"]
     *     - default: "Sequential"
     * </pre>
     * @return la lista con i due parametri */
    @Override
    public List<Param<?>> params() {
        List<Param<?>> listaParametri = new ArrayList<>();

        Param<Integer> primoP = new Param<Integer>() {
            @Override
            public String name() {
                return nome1;
            }

            @Override
            public String prompt() {
                return prompt1;
            }

            @Override
            public List<Integer> values() {
                return Collections.unmodifiableList(values1);
            }

            @Override
            public void set(Object v) {

                if (values1.contains(v))
                    valoreAttuale1 = (Integer) v;
                else
                    throw new IllegalArgumentException();

            }

            @Override
            public Integer get() {
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
    public Play canPlay(GameFactory<? extends GameRuler<P>> gF) {

        Objects.requireNonNull(gF);
        return Play.YES;
    }

    @Override
    public String tryCompute(GameFactory<? extends GameRuler<P>> gF, boolean parallel,
                             Supplier<Boolean> interrupt) {

        Objects.requireNonNull(gF);
        return null;
    }

    /** Ritorna un {@link MCTSPlayer} che rispetta i parametri impostati
     * {@link MCTSPlayerFactory#params()} e il nome specificato. */
    @Override
    public Player<P> newPlayer(GameFactory<? extends GameRuler<P>> gF, String name) {

        if (canPlay(gF) != Play.YES)
            throw new IllegalStateException();

        Objects.requireNonNull(gF);
        Objects.requireNonNull(name);

        boolean parallel = false;

        switch (valoreAttuale2) {
            case "Sequential":
                parallel = false;
                break;
            case "Parallel":
                parallel = true;
                break;
        }

        return new MCTSPlayer<>(name, valoreAttuale1, parallel);
    }
}
