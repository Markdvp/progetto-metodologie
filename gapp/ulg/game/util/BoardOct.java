package gapp.ulg.game.util;

import gapp.ulg.game.board.Board;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.Pos;

import java.util.*;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Gli oggetti BoardOct implementano l'interfaccia {@link Board} per rappresentare
 * board generali con sistema di coordinate {@link System#OCTAGONAL}
 * modificabili.
 * @param <P>  tipo del modello dei pezzi */
public class BoardOct<P> implements Board<P> {
    /** Crea una BoardOct con le dimensioni date (può quindi essere rettangolare).
     * Le posizioni della board sono tutte quelle comprese nel rettangolo dato e le
     * adiacenze sono tutte e otto, eccetto per le posizioni di bordo.
     * @param width  larghezza board
     * @param height  altezza board
     * @throws IllegalArgumentException se width <= 0 o height <= 0 */
    public BoardOct(int width, int height) {

        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException();

        mappaPosToPiece = new HashMap<>();
        listaPosizioni = new ArrayList<>();
        altezza = height;
        larghezza = width;

        for (int i = 0 ; i < height ; i++)
            for (int j = 0 ; j < width ; j++)
                mappaPosToPiece.put(new Pos(j, i), null);

        for (int i = 0 ; i < height ; i++)
            for (int j = 0 ; j < width ; j++)
                listaPosizioni.add(new Pos(j, i));

        listaPosizioni = Collections.unmodifiableList(listaPosizioni);
    }

    /** Crea una BoardOct con le dimensioni date (può quindi essere rettangolare)
     * escludendo le posizioni in exc. Le adiacenze sono tutte e otto, eccetto per
     * le posizioni di bordo o adiacenti a posizioni escluse. Questo costruttore
     * permette di creare board per giochi come ad es.
     * <a href="https://en.wikipedia.org/wiki/Camelot_(board_game)">Camelot</a>
     * @param width  larghezza board
     * @param height  altezza board
     * @param exc  posizioni escluse dalla board
     * @throws NullPointerException se exc è null
     * @throws IllegalArgumentException se width <= 0 o height <= 0 */
    public BoardOct(int width, int height, Collection<? extends Pos> exc) {

        Objects.requireNonNull(exc);

        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException();

        mappaPosToPiece = new HashMap<>();
        listaPosizioni = new ArrayList<>();
        altezza = height;
        larghezza = width;

        for (int i = 0 ; i < height ; i++)
            for (int j = 0 ; j < width ; j++)
                mappaPosToPiece.put(new Pos(j, i), null);

        for (int i = 0 ; i < height ; i++)
            for (int j = 0 ; j < width ; j++)
                listaPosizioni.add(new Pos(j, i));

        for (Pos p : exc)
            if (listaPosizioni.contains(p)) {
                listaPosizioni.remove(p);
                mappaPosToPiece.remove(p);
            }

        listaPosizioni = Collections.unmodifiableList(listaPosizioni);
    }

    public Map<Pos,PieceModel<PieceModel.Species>> mappaPosToPiece;
    public List<Pos> listaPosizioni;
    public int altezza;
    public int larghezza;

    @Override
    public System system() { return System.OCTAGONAL; }

    @Override
    public int width() { return larghezza; }

    @Override
    public int height() { return altezza; }

    @Override
    public Pos adjacent(Pos p, Dir d) {
        Pos posDaRit = null;

        Objects.requireNonNull(p);
        Objects.requireNonNull(d);

        if (d.equals(Dir.UP) && listaPosizioni.contains(new Pos(p.b, p.t + 1)))
            posDaRit = new Pos(p.b, p.t + 1);

        else if (d.equals(Dir.UP_R) && listaPosizioni.contains(new Pos(p.b + 1, p.t + 1)))
            posDaRit = new Pos(p.b + 1, p.t + 1);

        else if (d.equals(Dir.RIGHT) && listaPosizioni.contains(new Pos(p.b + 1, p.t)))
            posDaRit = new Pos(p.b + 1, p.t);

        else if (d.equals(Dir.DOWN_R) && p.t > 0 && listaPosizioni.contains(new Pos(p.b + 1, p.t - 1)))
            posDaRit = new Pos(p.b + 1, p.t - 1);

        else if (d.equals(Dir.DOWN) && p.t > 0 && listaPosizioni.contains(new Pos(p.b, p.t - 1)))
            posDaRit = new Pos(p.b, p.t - 1);

        else if (d.equals(Dir.DOWN_L) && p.b > 0 && p.t > 0 && listaPosizioni.contains(new Pos(p.b - 1, p.t - 1)))
            posDaRit = new Pos(p.b - 1, p.t - 1);

        else if (d.equals(Board.Dir.LEFT) && p.b > 0 && listaPosizioni.contains(new Pos(p.b - 1, p.t)))
            posDaRit = new Pos(p.b - 1, p.t);

        else if (d.equals(Dir.UP_L) && p.b > 0 && listaPosizioni.contains(new Pos(p.b - 1, p.t + 1)))
            posDaRit = new Pos(p.b - 1, p.t + 1);

        return posDaRit;
    }

    @Override
    public List<Pos> positions() {
        return listaPosizioni;
    }

    @Override
    public P get(Pos p) {

        Objects.requireNonNull(p);

        return (P) mappaPosToPiece.get(p);
    }

    @Override
    public boolean isModifiable() { return true; }

    @Override
    public P put(P pm, Pos p) {
        P piece;

        Objects.requireNonNull(pm);
        Objects.requireNonNull(p);

        if (!isModifiable())
            throw new UnsupportedOperationException();

        if (!positions().contains(p))
            throw new IllegalArgumentException();

        piece = (P) mappaPosToPiece.get(p);

        mappaPosToPiece.put(p,(PieceModel<PieceModel.Species>) pm);

        return piece;
    }

    @Override
    public P remove(Pos p) {
        P piece;

        Objects.requireNonNull(p);

        if (!isModifiable())
            throw new UnsupportedOperationException();

        if (!positions().contains(p))
            throw new IllegalArgumentException();

        piece = (P) mappaPosToPiece.get(p);

        mappaPosToPiece.put(p, null);

        return piece;
    }
}
