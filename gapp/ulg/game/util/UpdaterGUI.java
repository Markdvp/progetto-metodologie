package gapp.ulg.game.util;

import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.PieceModel;

import java.util.Objects;

import static gapp.gui.Variables.*;

public class UpdaterGUI<P> implements PlayGUI.Observer<P> {

    /**
     * Il gioco specificato, copia dell'originale
     */
    private GameRuler<P> gameCurrent;

    public UpdaterGUI() {
        gameCurrent = null;
    }

    @Override
    public void setGame(GameRuler<P> g) {

        if (g == null)
            throw new NullPointerException();

        gameCurrent = g;

        if (isGUI)
            game.createBoard();
    }

    @Override
    public void moved(int i, Move<P> m) {

        Objects.requireNonNull(m);

        if (gameCurrent == null || gameCurrent.result() != -1)
            throw new IllegalStateException();

        if (i <= 0 || i > gameCurrent.players().size() || !gameCurrent.isValid(m))
            throw new IllegalArgumentException();

        if (gameCurrent.isPlaying(i)) {
            gameCurrent.move(m);

            if (isGUI && !interrupt) {

                game.stopCounting = false;

                if (!match.getiPP().get(match.getCurrentGame().turn() - 1).getSecond().getClass().equals(PlayerGUI.class))
                    game.updater();

                else
                    canContinuePlaying = true;


                game.result = gameCurrent.result();

                if (game.result > 0)
                    for (PlayGUI.Triple<PieceModel<PieceModel.Species>> t : match.getiPP())
                        if (t.getFirst() == game.result)
                            game.winner = t.getSecond().name();

            }

        }

    }

    @Override
    public void limitBreak(int i, String msg) {

        if (gameCurrent != null)
            moved(i,new Move<>(Move.Kind.RESIGN));

        if (observerMessage == null)
            observerMessage = msg;
    }

    @Override
    public void interrupted(String msg) {

        if (observerMessage == null)
            observerMessage = msg;

    }

}
