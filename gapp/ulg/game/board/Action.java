package gapp.ulg.game.board;

import java.util.*;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi nè i campi pubblici.</b>
 * <br>
 * Un oggetto Action rappresenta un'azione che può essere parte della mossa di
 * un giocatore nel suo turno di gioco. Gli oggetti Action sono immutabili.
 * <br>
 * Un Action è di uno dei seguenti quattro tipi:
 * <ul>
 *     <li>{@link Kind#ADD}: Aggiunge un pezzo di un modello dato, ad es. aggiungere
 *     un pezzo in giochi come Go e Othello.</li>
 *     <li>{@link Kind#REMOVE}: Rimuove uno o più pezzi, ad es. catturare uno o più
 *     pezzi in Go o mangiare l'alfiere nero con la regina bianca negli Scacchi.</li>
 *     <li>{@link Kind#MOVE}: Muove uno o più pezzi in una direzione, ad es. muovere
 *     la torre bianca negli Scacchi, una pedina nella Dama o muovere una fila di
 *     tre pezzi nel gioco Abalone.</li>
 *     <li>{@link Kind#JUMP}: Muove un pezzo, eventualmente saltando sopra altri
 *     pezzi, ad es. muovere un cavallo negli Scacchi o una pedina nella Dama
 *     saltando e mangiando una pedina avversaria</li>
 *     <li>{@link Kind#SWAP}: Sostituisce uno o più pezzi con pezzi di un modello
 *     dato, ad es. rovesciare pezzi da bianchi a neri in Othello o promuovere a
 *     dama una pedina bianca nella Dama.</li>
 * </ul>
 * In generale la mossa di un giocatore è costituita da una o più azioni, cioè,
 * oggetti Action. Ad es. in Go una mossa senza cattura è costituita solamente
 * da un Action di tipo {@link Kind#ADD} ma una mossa con cattura consiste di un
 * Action di tipo {@link Kind#ADD} seguita da una Action di tipo
 * {@link Kind#REMOVE}.
 * @param <P>  tipo del modello dei pezzi
 * @see Move */
public class Action<P> {
    public enum Kind {
        /** Aggiunge un pezzo di un modello dato */
        ADD,
        /** Rimuove uno o più pezzi */
        REMOVE,
        /** Muove uno o più pezzi in una direzione */
        MOVE,
        /** Muove un pezzo, eventualmente saltando sopra altri pezzi */
        JUMP,
        /** Sostituisce uno o più pezzi con pezzi di un modello dato */
        SWAP
    }

    /** Tipo dell'azione, non è mai null */
    public final Kind kind;
    /** Modello di pezzo, è diverso da null solamente per {@link Kind#ADD} e
     * {@link Kind#SWAP} */
    public final P piece;
    /** Lista immodificabile di posizioni, non è mai null ed ha sempre
     * lunghezza >= 1. Ha lunghezza esattamente 1 per {@link Kind#ADD}. */
    public final List<Pos> pos;
    /** Direzione di movimento, è diversa da null solamente per {@link Kind#MOVE} */
    public final Board.Dir dir;
    /** Numero passi nella direzione {@link Action#dir}, ha significato solamente
     * quando il tipo dell'azione è {@link Kind#MOVE} */
    public final int steps;

    /** Crea un'azione di tipo {@link Kind#ADD} che aggiunge nella posizione p un
     * pezzo di modello pm.
     * @param p  posizione del nuovo pezzo
     * @param pm  modello del pezzo da aggiungere
     * @throws NullPointerException se p o pm è null */
    public Action(Pos p, P pm) {

        Objects.requireNonNull(p);
        Objects.requireNonNull(pm);

        kind = Kind.ADD;
        piece = pm;
        pos = Collections.unmodifiableList(Arrays.asList(p));
        dir = null;
        steps = 0;

    }

    /** Crea un'azione di tipo {@link Kind#REMOVE} che rimuove i pezzi nelle
     * posizioni date.
     * @param pp  posizioni dei pezzi da rimuovere
     * @throws NullPointerException se una delle posizioni è null
     * @throws IllegalArgumentException se non è data almeno una posizione o sono
     * date posizioni duplicate */
    public Action(Pos...pp) {

        for (Pos p : pp) {

            if (p == null) {

                throw new NullPointerException();

            }

        }

        if (pp.length == 0 || pp.length != new HashSet<>(Arrays.asList(pp)).size()) {

            throw new IllegalArgumentException();

        }

        kind = Kind.REMOVE;
        piece = null;
        pos = Collections.unmodifiableList(Arrays.asList(pp));
        dir = null;
        steps = 0;

    }

    /** Crea un'azione di tipo {@link Kind#MOVE} che muove tutti i pezzi nelle
     * posizioni pp nella direzione d per ns passi.
     * @param d  direzione di movimento
     * @param ns  numero passi
     * @param pp  posizioni di partenza dei pezzi da muovere
     * @throws NullPointerException se d è null o una delle posizioni è null
     * @throws IllegalArgumentException se ns < 1 o non è data almeno una posizione
     * o sono date posizioni duplicate */
    public Action(Board.Dir d, int ns, Pos...pp) {

        Objects.requireNonNull(d);

        for (Pos p : pp)
            if (p == null)
                throw new NullPointerException();

        if (ns < 1 || pp.length == 0 || pp.length != new HashSet<>(Arrays.asList(pp)).size())
            throw new IllegalArgumentException();

        kind = Kind.MOVE;
        piece = null;
        pos = Collections.unmodifiableList(Arrays.asList(pp));
        dir = d;
        steps = ns;

    }

    /** Crea un'azione di tipo {@link Kind#JUMP} che muove il pezzo dalla posizione
     * p1 alla posizione p2. Le posizioni p1 e p2 sono riportate in quest'ordine
     * nella lista di {@link Action#pos}.
     * @param p1  posizione di partenza
     * @param p2  posizione di arrivo
     * @throws NullPointerException se p1 o p2 è null
     * @throws IllegalArgumentException se p1 è uguale a p2 */
    public Action(Pos p1, Pos p2) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);

        if (p1.equals(p2))
            throw new IllegalArgumentException();

        kind = Kind.JUMP;
        piece = null;
        pos = Collections.unmodifiableList(Arrays.asList(p1, p2));
        dir = null;
        steps = 0;

    }

    /** Crea un'azione di tipo {@link Kind#SWAP} che sostituisce tutti i pezzi
     * nelle posizioni pp con pezzi di modello pm.
     * @param pm  modello del nuovo pezzo
     * @param pp  posizioni dei pezzi da sostituire
     * @throws NullPointerException se pm è null o una delle posizioni è null
     * @throws IllegalArgumentException se non è data almeno una posizione o sono
     * date posizioni duplicate */
    public Action(P pm, Pos...pp) {
        Objects.requireNonNull(pm);

        for (Pos p : pp)
            if (p == null)
                throw new NullPointerException();


        if (pp.length == 0 || pp.length != new HashSet<>(Arrays.asList(pp)).size()) {

            throw new IllegalArgumentException();

        }

        kind = Kind.SWAP;
        piece = pm;
        pos = Collections.unmodifiableList(Arrays.asList(pp));
        dir = null;
        steps = 0;

    }

    /** Ritorna true se e solo se x è un oggetto di tipo {@link Action} ed ha gli
     * stessi valori dei campi {@link Action#kind}, {@link Action#piece},
     * {@link Action#pos}, {@link Action#dir} e {@link Action#steps}.
     * <br>
     * <b>ATTENZIONE: due liste {@link Action#pos} sono considerate uguali se
     * contengono le stesse posizioni indipendentemente dall'ordine per tutti i tipi
     * di azione eccetto che per {@link Action.Kind#JUMP}.</b>
     * @param x  un oggetto (o null)
     * @return true se x è uguale a questa azione */
    @Override
    public boolean equals(Object x) {
        if (this == x) return true;
        if (x == null || getClass() != x.getClass()) return false;

        Action<?> action = (Action<?>) x;

        if (steps != action.steps) return false;
        if (kind != action.kind) return false;
        if (piece != null ? !piece.equals(action.piece) : action.piece != null) return false;

        if (!(new HashSet<>(pos)).equals(new HashSet<>(action.pos))) {

            return false;

        }

        return dir == action.dir;
    }

    /** Ridefinito coerentemente alla ridefinizione di {@link Action#equals(Object)}.
     * @return hash code di questa azione */
    @Override
    public int hashCode() {
        int result = kind != null ? kind.hashCode() : 0;
        result = 31 * result + (piece != null ? piece.hashCode() : 0);
        result = 31 * result + (pos != null ? (new HashSet<>(pos)).hashCode() : 0);
        result = 31 * result + (dir != null ? dir.hashCode() : 0);
        result = 31 * result + steps;
        return result;
    }

}
