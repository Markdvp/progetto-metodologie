package gapp.ulg.game.util;

import gapp.ulg.game.board.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Un {@code Nodo} rappresenta un nodo in un {@link Tree}
 * @param <P> tipo del modello dei pezzi
 */
public class Nodo<P> {

    /**
     * lista di azioni del nodo
     */
    private List<Action<P>> action;

    /**
     * Crea un oggetto di tipo {@code Nodo} con valore la lista di azioni in input
     * @param azioni lista di azioni
     */
    public Nodo(List<Action<P>> azioni) {
        action = azioni;
    }

    /**
     * Crea un oggetto di tipo {@code Nodo} con nessun valore
     */
    public Nodo() {
        action = new ArrayList<>();
    }

    /**
     * @return lista di azioni del nodo
     */
    public List<Action<P>> getAction() {
        return action;
    }

    /**
     * Imposta il valore del nodo
     * @param action lista di azioni
     */
    public void setAction(List<Action<P>> action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Nodo nodo = (Nodo) o;

        return action.equals(nodo.action);

    }

    @Override
    public int hashCode() {
        return action.hashCode();
    }
}
