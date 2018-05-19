package gapp.gui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.util.PlayGUI;
import gapp.ulg.game.util.UpdaterGUI;

import static gapp.gui.Variables.*;

/**
 * L' oggetto {@code GameMenu} è il {@code Node} di avvio della GUI, esso
 * rappresenta un menu navigabile in cui l' utente selezionando
 * le varie voci del menu sarà reindirizzato nelle apposite aree
 * di destinazione.
 */
public class GameMenu extends Parent {

    /**
     * Il nodo principale della classe
     */
    private Pane root;

    /**
     * I menu navigabili della GUI
     * @see MenuBox
     */
    private MenuBox gameMenu,subGameMenu;

    /**
     * Rappresenta la pagina di selezione dei parametri generali di un gioco o
     * della GUI precedenti alla selezione di un gioco.
     */
    private GeneralSettings generalSettingsMenu;

    static {
        pieceToString.put(PieceModel.Species.DISC, "DISC");
        pieceToString.put(PieceModel.Species.DAMA, "DAMA");
        pieceToString.put(PieceModel.Species.PAWN, "PEDINA");
        pieceToString.put(PieceModel.Species.KNIGHT, "CAVALLO");
        pieceToString.put(PieceModel.Species.BISHOP, "ALFIERE");
        pieceToString.put(PieceModel.Species.ROOK, "TORRE");
        pieceToString.put(PieceModel.Species.QUEEN, "REGINA");
        pieceToString.put(PieceModel.Species.KING, "RE");

        colorToString.put("nero", "NERO");
        colorToString.put("bianco", "BIANCO");

        textIntro = new Text("PRESS ENTER");
        textIntro.setFont(Font.font("Comic Sans MS", 20));
        textIntro.setFill(Color.GREY);
        textIntro.setEffect(new GaussianBlur(1.0));
        textIntro.setTranslateX(450);
        textIntro.setTranslateY(500);

        tryComputeStopper = () -> tryComputeInterrupt;
    }

    /**
     * Crea un oggetto {@link GameMenu} per creare un menu navigabile
     * da parte dell' utente
     */
    public GameMenu() {

        root = new Pane();
        root.setPrefSize(MAIN_ROOT_WIDHT, MAIN_ROOT_HEIGHT);

        root.setBackground(new Background(new BackgroundImage(new Image(getClass().getResource("../../resources/First_Screen_View.jpg").toString()),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT)));

        MenuItems newGame = new MenuItems("NEW GAME");
        MenuItems options = new MenuItems("OPTIONS");
        MenuItems credits = new MenuItems("CREDITS");
        MenuItems exitGame = new MenuItems("EXIT GAME");

        MenuItems generalSettings = new MenuItems("GENERAL SETTINGS");
        MenuItems soundSettings = new MenuItems("SOUND");
        MenuItems backToMainMenu = new MenuItems("MAIN MENU");

        gameMenu = new MenuBox("ULG - Universal Library Games",newGame,options,credits,exitGame);
        subGameMenu = new MenuBox("ULG - OPTIONS",generalSettings,soundSettings,backToMainMenu);

        final int offset = - 400;

        subGameMenu.setTranslateX(offset);

        newGame.setOnMouseClicked(event -> {
            match = new PlayGUI<>(arbitro, MAXBLOCKTIME);
            gameSettings = new ScreenChooser();
            game = new BoardGame();
            interrupt = false;

            root.getChildren().remove(gameMenu);
            menu.getGameMenu().hideMenu();
            root.getChildren().add(gameSettings.getMainLayout());
        });

        options.setOnMouseClicked(event -> {
            root.getChildren().add(subGameMenu);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(0.25), gameMenu);
            tt.setToX(gameMenu.getTranslateX() + offset);

            TranslateTransition tt1 = new TranslateTransition(Duration.seconds(0.5),subGameMenu);
            tt1.setToX(gameMenu.getTranslateX());

            tt.play();
            tt1.play();

            tt.setOnFinished(evt -> root.getChildren().remove(gameMenu));
        });

        exitGame.setOnMouseClicked(event -> System.exit(0));

        backToMainMenu.setOnMouseClicked(event -> {

            root.getChildren().add(gameMenu);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(0.25), subGameMenu);
            tt.setToX(subGameMenu.getTranslateX() + offset);

            TranslateTransition tt1 = new TranslateTransition(Duration.seconds(0.5),gameMenu);
            tt1.setToX(subGameMenu.getTranslateX());

            tt.play();
            tt1.play();

            tt.setOnFinished(evt -> root.getChildren().remove(subGameMenu));
        });

        generalSettings.setOnMouseClicked(event -> {
            root.getChildren().remove(subGameMenu);
            root.getChildren().add(generalSettingsMenu.getMainLayout());
        });

        credits.setOnMouseClicked(event -> {
            root.getChildren().remove(gameMenu);
            root.getChildren().add(new Credits());
        });

        Thread textFade = new Thread(() -> {
            FadeTransition ft = new FadeTransition(Duration.millis(1000), textIntro);
            ft.setFromValue(1.0);
            ft.setToValue(0.2);
            ft.setCycleCount(100);
            ft.setAutoReverse(true);
            ft.play();
        });

        textFade.setDaemon(true);
        textFade.start();

        arbitro = new UpdaterGUI<>();
        interrupt = false;
        game = new BoardGame();
        gameSettings = new ScreenChooser();
        isGUI = true;
        generalSettingsMenu = new GeneralSettings();

        canContinuePlaying = true;

        root.getChildren().addAll(gameMenu,textIntro);
    }

    /**
     * @return il menu di gioco
     */
    MenuBox getGameMenu() {
        return gameMenu;
    }

    /**
     * @return il sotto-menu del gioco
     */
    MenuBox getSubGameMenu() {
        return subGameMenu;
    }

    /**
     * Il nodo principale della classe
     */
    Pane getRoot() {
        return root;
    }

}
