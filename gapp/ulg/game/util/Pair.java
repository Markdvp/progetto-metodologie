package gapp.ulg.game.util;

import java.util.HashSet;
import java.util.List;

/**
 * Un {@code Pair} associa ad un oggetto {@link Nodo} una lista
 * di oggetti {@link Nodo}, figli del nodo a cui sono stati associati
 * @param <P> tipo del modello dei pezzi
 * @see Nodo
 */
public class Pair<P> {

    /**
     * nodo padre
     */
    Nodo<P> first;

    /**
     * lista dei nodi figli
     */
    List<Nodo<P>> second;

    /**
     * Crea un oggetto di tipo {@code Pair} che associa
     * al nodo la lista dei nodi passati in input
     * @param primo nodo
     * @param secondo lista di nodi
     */
    public Pair(Nodo<P> primo, List<Nodo<P>> secondo) {
        first = primo;
        second = secondo;
    }

    /**
     * @return il nodo padre
     */
    public Nodo<P> getFirst() {
        return first;
    }

    /**
     * Imposta il valore del nodo padre
     * @param first nodo
     */
    public void setFirst(Nodo<P> first) {
        this.first = first;
    }

    /**
     * @return la lista dei figli
     */
    public List<Nodo<P>> getSecond() {
        return second;
    }

    /**
     * Imposta la lista dei figli
     * @param second
     */
    public void setSecond(List<Nodo<P>> second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        return first.equals(pair.first) && (new HashSet<>(second)).equals(new HashSet<>(pair.second));
    }

}