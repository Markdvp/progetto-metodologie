package gapp.ulg.play;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.util.Probe;

import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;
import java.util.concurrent.*;

class StrategiaOttimale<P> implements OptimalPlayerFactory.Strategy<P>, Serializable {

    StrategiaOttimale(GameFactory<? extends GameRuler<P>> gF, boolean parallel, Supplier<Boolean> interrupt) {

        interruptBeep = interrupt;
        meccanica = gF.newGame().mechanics();
        nome = gF.newGame().name();
        strategia = new HashMap<>();
        strategiaCONC = new ConcurrentHashMap<>();

       if (!parallel)
           ricorsiveOPT(meccanica.start);

       else {
           ricorsiveOPTParallel(meccanica.start);

           for (Map.Entry<Probe.EncS<P>, Integer> e : strategiaCONC.entrySet())
               strategia.put(e.getKey(), e.getValue());
       }

        strategiaCONC = null;

    }
    StrategiaOttimale(GameFactory<? extends GameRuler<P>> gF) {
        meccanica = gF.newGame().mechanics();
        strategia = new HashMap<>();
        nome = gF.newGame().name();
    }

    public String nome;
    Map<Probe.EncS<P>, Integer> strategia;
    private ConcurrentHashMap<Probe.EncS<P>, Integer> strategiaCONC;

    private GameRuler.Mechanics<P> meccanica;
    private Supplier<Boolean> interruptBeep;

    @Override
    public String gName() {
        return nome;
    }

    @Override
    public Move<P> move(GameRuler.Situation<P> s, GameRuler.Next<P> next) {

        Map<Move<P>, GameRuler.Situation<P>> subitoDopo = next.get(s);

        for (Map.Entry<Move<P>, GameRuler.Situation<P>> e : subitoDopo.entrySet()) {
            Probe.EncS<P> cod = new Probe.EncS<>(meccanica, e.getValue());
            if (strategia.get(cod) != null && strategia.get(cod) == -s.turn)
                return e.getKey();
        }

        for (Map.Entry<Move<P>, GameRuler.Situation<P>> e : subitoDopo.entrySet()) {
            Probe.EncS<P> cod = new Probe.EncS<>(meccanica, e.getValue());
            if (strategia.get(cod) != null && strategia.get(cod) == 0)
                return e.getKey();
        }

        for (Map.Entry<Move<P>, GameRuler.Situation<P>> e : subitoDopo.entrySet())
            return e.getKey();

        return null;
    }

    private Integer ricorsiveOPT(GameRuler.Situation<P> s) {

        if (interruptBeep != null && interruptBeep.get())
            s = null;

        if (s.turn <= 0) {
            strategia.put(new Probe.EncS<>(meccanica, s), s.turn);
            return s.turn;
        }

        else {
            Set<Integer> lista = new HashSet<>();

            for (GameRuler.Situation<P> e : new ArrayList<>(meccanica.next.get(s).values())) {
                Probe.EncS<P> cod = new Probe.EncS<>(meccanica, e);

                if (lista.contains(-s.turn)) {
                    strategia.put(new Probe.EncS<>(meccanica, s), -s.turn);
                    return -s.turn;
                }

                if (strategia.keySet().contains(cod))
                    lista.add(strategia.get(cod));

                else
                    lista.add(ricorsiveOPT(e));

            }

            strategia.put(new Probe.EncS<>(meccanica, s), best(lista, s.turn));
            return best(lista, s.turn);
        }

    }

    private Integer ricorsiveOPTParallel(GameRuler.Situation<P> s) {

        if (interruptBeep != null && interruptBeep.get())
            s = null;

        if (s.turn <= 0) {
            strategiaCONC.put(new Probe.EncS<>(meccanica, s), s.turn);
            return s.turn;
        }

        else {
            Set<Integer> lista = new HashSet<>();
            List<ForkJoinTask<Set<Integer>>> tasks = new ArrayList<>();

            for (GameRuler.Situation<P> e : new ArrayList<>(meccanica.next.get(s).values()))
                tasks.add(ForkJoinTask.adapt(() -> {
                    Probe.EncS<P> cod = new Probe.EncS<>(meccanica, e);

                    Set<Integer> lista2 = new HashSet<>();

                    if (strategiaCONC.keySet().contains(cod))
                        lista2.add(strategiaCONC.get(cod));

                    else
                        lista2.add(ricorsiveOPTParallel(e));

                    return lista2;
                }));

            for (ForkJoinTask<Set<Integer>> f : ForkJoinTask.invokeAll(tasks))
                lista.addAll(f.join());

            int value = best(lista, s.turn);
            strategiaCONC.put(new Probe.EncS<>(meccanica, s), value);
            return value;
        }

    }

    private Integer best(Set<Integer> listaSit, Integer statoSuperiore) {

        for (Integer s : listaSit)
            if (s == -statoSuperiore)
                return -statoSuperiore;

        for (Integer s : listaSit)
            if (s == 0)
                return 0;

        for (Integer s : listaSit)
            return s;

        return -3;
    }

}