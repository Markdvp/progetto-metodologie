package gapp.ulg.play;

import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Un oggetto RandPlayer è un oggetto che può giocare un qualsiasi gioco regolato
 * da un {@link GameRuler} perché, ad ogni suo turno, sceglie in modo random una
 * mossa tra quelle valide esclusa {@link Move.Kind#RESIGN}.
 * @param <P>  tipo del modello dei pezzi */
public class RandPlayer<P> implements Player<P> {
    /** Crea un giocatore random, capace di giocare a un qualsiasi gioco, che ad
     * ogni suo turno fa una mossa scelta in modo random tra quelle valide.
     * @param name  il nome del giocatore random
     * @throws NullPointerException se name è null */
    public RandPlayer(String name) {

        Objects.requireNonNull(name);

        nome = name;
        partita = null;

    }

    public String nome;
    public GameRuler<P> partita;

    @Override
    public String name() {
        return nome;
    }

    @Override
    public void setGame(GameRuler<P> g) {
        Objects.requireNonNull(g);

        partita = g;
    }

    @Override
    public void moved(int i, Move<P> m) {
        Objects.requireNonNull(m);

        if (partita == null || partita.result() != -1)
            throw new IllegalStateException();

        if (i <= 0 || i > partita.players().size() || !partita.isValid(m))
            throw new IllegalArgumentException();

        if (partita.isPlaying(i))
            partita.move(m);

    }

    @Override
    public Move<P> getMove() {

        if (partita == null || partita.result() != -1 || !partita.isPlaying(partita.turn()))
            throw new IllegalStateException();

        Move<P> mossa;

        List<Move<P>> lista = new ArrayList<>(partita.validMoves());

        while (true) {
            int randomINT = (int) (Math.random() * (lista.size()));
            Move<P> mossaRandom = lista.get(randomINT);

            if (!mossaRandom.kind.equals(Move.Kind.RESIGN)) {

                mossa = mossaRandom;
                break;

            }
        }

        return mossa;
    }
}
