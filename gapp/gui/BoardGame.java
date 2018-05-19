package gapp.gui;

import gapp.ulg.play.PlayerFactories;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import gapp.ulg.game.board.*;
import gapp.ulg.game.util.*;

import java.util.*;
import java.util.function.Consumer;

import static gapp.gui.Variables.*;

/**
 * Un {@code BoardGame} è un oggetto in grado di rappresentare graficamente
 * una partita di un qualsiasi gioco del framework che si vuole giocare tramite GUI.
 * Il gioco per essere visualizzato correttamente deve essere registrato in {@link PlayerFactories}
 * tramite {@link PlayerFactories#registerBoardFactory(Class)}
 */
public class BoardGame {

    /**
     * Rappresenta la voce di un oggetto {@link MenuBar} che permette di tornare al menu principale dell' applicazione
     */
    private MenuItem mainMenu;

    /**
     * Rappresenta la voce di un oggetto {@link MenuBar} che permette di interrompere il match in corso e tornare al menu principale dell' applicazione
     */
    private MenuItem stopMatch;

    /**
     * Rappresenta la voce di un oggetto {@link MenuBar} che permette ad un {@link PlayerGUI} di annullare l' ultima mossa eseguita
     */
    private MenuItem backAction;

    /**
     * Il {@link Node} principale della classe
     */
    private VBox area;

    /**
     * Il {@link Node} in cui viene visualizzato tutto ciò che riguarda la partita (aggiornamento informazioni player, posizionamento pezzi, ecc..)
     */
    private final Group arena;

    /**
     * La dimensione del lato del quadrato,varia al variare della grandezza della {@link Board}
     */
    private int squareSide;

    /**
     * Indica se la partita che si sta giocando presenta un sistema di punteggio
     */
    private boolean noScoreGame;

    /**
     * Indica se è stato dato l' input da parte dell' utente di terminare il match prematuramente
     */
    private boolean stopMatchBUTTON;

    /**
     * Il risultato della partita
     */
    public int result;

    /**
     * Il nome del vincitore della partita
     */
    public String winner;

    /**
     * Rappresenta una figura geometrica utile per dare l' effetto di selezione di un quadrato della {@link Board}
     */
    private Shape layer;

    /**
     * La posizione del layer
     */
    private Pos layerPos;

    /**
     * L' oggetto {@link Text} rappresentante il nome del giocatore di turno
     */
    private Text playerName;

    /**
     * L' oggetto {@link Text} rappresentante il tempo rimanente ad un giocatore per eseguire una mossa
     */
    private Text timeFlow;

    /**
     * Lista contente gli oggetti {@link Text} rappresentanti i punteggi dei giocatori che stanno giocando la partita
     */
    private List<Text> playerScores;

    /**
     * La lista contenente oggetti di tipo {@code ImageViewMap}
     * @see ImageViewMap
     */
    private List<ImageViewMap> map;

    /**
     * Comunica al {@link BoardGame#timeFlow} che il giocatore di turno ha scelto la mossa
     */
    public volatile boolean stopCounting;

    /**
     * Il master per un giocatore di tipo {@link PlayerGUI}
     */
    private Consumer<PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>>> master;

    /**
     * Lista delle posizioni selezionate da un giocatore di tipo {@link PlayerGUI}
     */
    private List<Pos> currentBoardSelection = new ArrayList<>();

    /**
     * Lista delle possibili mosse eseguibili in relazione alle posizioni selezionate
     */
    private List<Move<PieceModel<PieceModel.Species>>> selectedMoves = new ArrayList<>();

    /**
     * Lista dei pezzi ammissibili in relazioni alle possibili mosse eseguibili
     */
    private List<PieceModel<PieceModel.Species>> piecesList = new ArrayList<>();

    /**
     * {@link Map} che associa ad ogni oggetto di tipo {@link PieceModel} la corrispondente {@link ImageView}
     */
    private Map<PieceModel.Species, ImageView> speciesToImageView = new HashMap<>();

    /**
     * La sottomossa scelta da un giocatore di tipo {@link PlayerGUI}
     */
    private Move<PieceModel<PieceModel.Species>>[] subMove = genericArrayCreation((Move<PieceModel<PieceModel.Species>>) null);

    /**
     * {@link ImageView} rappresentante il pulsante che conferma la scelta di una mossa per un giocatore di tipo {@link PlayerGUI}
     */
    private ImageView tickCompleteMove;

    /**
     * {@link ImageView} rappresentante il pulsante che esegue un {@link gapp.ulg.game.board.Action.Kind#REMOVE} per un giocatore di tipo {@link PlayerGUI}
     */
    private ImageView crossRemovePiece;

    /**
     * {@link ImageView} rappresentante il pulsante che esegue un {@link gapp.ulg.game.board.Action.Kind#MOVE} per un giocatore di tipo {@link PlayerGUI}
     */
    private ImageView arrowMovePiece;

    /**
     * {@link ImageView} rappresentante il pulsante che esegue un {@link gapp.ulg.game.board.Action.Kind#JUMP} per un giocatore di tipo {@link PlayerGUI}
     */
    private ImageView arrowJumpPiece;

    /**
     * {@link Button} che permette al giocatore di arrendersi
     */
    private Button resign;

    /**
     * {@link Button} che permette al giocatore di passare il turno
     */
    private Button pass;


    /**
     * Crea un oggetto {@link BoardGame} per partite giocate tramite una GUI
     */
    public BoardGame() {
        Canvas canvas = new Canvas(CANVAS_WIDHT, CANVAS_HEIGHT);
        map = new ArrayList<>();
        squareSide = 0;
        noScoreGame = false;
        playerGUImanualRESIGN = false;
        stopMatchBUTTON = false;
        layer = null;
        layerPos = null;
        result = -1;
        playerName = null;
        winner = null;
        timeFlow = null;
        playerScores = new ArrayList<>();
        tickCompleteMove = null;
        crossRemovePiece = null;
        arrowMovePiece = null;
        arrowJumpPiece = null;
        resign = null;
        pass = null;

        master = master();

        mainMenu = new MenuItem("Return to the main Menu");
        stopMatch = new MenuItem("Stop current match");
        backAction = new MenuItem("Undo last move");

        mainMenu.setOnAction(e -> {

            if (game != null)
                menu.getRoot().getChildren().remove(game.getNode());

            if (match != null)
                match.stop();

            match = null;
            arbitro = new UpdaterGUI<>();

            canContinuePlaying = true;

            menu.getRoot().setBackground(new Background(new BackgroundImage(new Image(getClass().getResource("../../resources/First_Screen_View.jpg").toString()),
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                    BackgroundSize.DEFAULT)));

            menu.getRoot().getChildren().add(menu.getGameMenu());
            menu.getRoot().getChildren().add(textIntro);

            stage.sizeToScene();

            System.gc();
        });

        stopMatch.setOnAction(e -> {
            stopMatchBUTTON = true;

            if (game != null)
                menu.getRoot().getChildren().remove(game.getNode());

            menu.getRoot().setBackground(new Background(new BackgroundFill(Color.FLORALWHITE,null,null)));

            if (match != null)
                match.stop();

            match = null;
            arbitro = new UpdaterGUI<>();
            canContinuePlaying = true;

            System.gc();
        });

        Menu menu = new Menu("Game", null, mainMenu);
        Menu matchMenu = new Menu("Match", null, stopMatch, backAction);
        MenuBar mBar = new MenuBar(menu, matchMenu);
        mBar.setUseSystemMenuBar(true);

        mainMenu.setDisable(true);
        stopMatch.setDisable(false);
        backAction.setDisable(true);

        arena = new Group(canvas);
        area = new VBox(mBar, arena);
    }

    /**
     * Metodo utillizato dal {@link gapp.ulg.game.util.PlayGUI.Observer} per disegnare la board
     * su cui verrà giocata la partita.
     */
    public void createBoard() {
        canContinuePlaying = false;

        menu.getRoot().setBackground(new Background(new BackgroundImage(new Image(getClass().getResource("../../resources/SFONDO.jpg").toString()),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT)));

        while (match.getCurrentGame() == null) ;

        /**Metto sullo sfondo un quadrato trasparente, mi servirà per far sparire il layer quando non sono sui quadrati della board*/
        Shape transparent = new Rectangle(0, 0, 395, 395);
        transparent.setFill(Color.TRANSPARENT);
        transparent.setOnMouseEntered(e -> {
            if (layer != null && arena.getChildren().contains(layer))
                Platform.runLater(() ->arena.getChildren().remove(layer));

        });
        Platform.runLater(() -> arena.getChildren().add(transparent));

        /**Regolo la grandezza dei quadrati della board in base alla grandezza della board stessa,
         * per riuscire a farli entrare tutti nella schermata*/
        int altezza = match.getCurrentGame().getBoard().height();
        int larghezza = match.getCurrentGame().getBoard().width();
        int maxSquare = Math.max(larghezza, altezza);

        squareSide = (380 - maxSquare + 1) / maxSquare;
        int cornerX = (400 - (larghezza * squareSide) - (larghezza - 1)) / 2;
        int cornerY = (400 - (altezza * squareSide) - (altezza - 1)) / 2;
        boolean color = true;

        for (int i = 0; i < altezza; i++) {
            for (int j = 0; j < larghezza; j++) {
                ImageView img;

                if (match.getCurrentGame().getBoard().isPos(new Pos(j, match.getCurrentGame().getBoard().height() - i - 1))) {

                    if (color)
                        img = woodPiece("LEGNOCHIARO", squareSide, cornerX, cornerY);
                    else
                        img = woodPiece("LEGNOSCURO", squareSide, cornerX, cornerY);

                    Pos p = new Pos(j, match.getCurrentGame().getBoard().height() - i - 1);
                    map.add(new ImageViewMap(p, cornerX, cornerY, img, graphicPiece(match.getCurrentGame().getBoard().get(p), squareSide, cornerX, cornerY), null));

                }

                else {
                    img = woodPiece("QUADRATOTRASPARENTE", squareSide, cornerX, cornerY);

                    Pos p = new Pos(j, match.getCurrentGame().getBoard().height() - i - 1);
                    map.add(new ImageViewMap(p, cornerX, cornerY, img, null, null));
                }

                color = !color;
                cornerX = cornerX + squareSide + 1;
            }

            if ((match.getCurrentGame().getBoard().width()) % 2 == 0)
                color = !color;

            cornerX = (400 - (larghezza * squareSide) - (larghezza - 1)) / 2;
            cornerY = cornerY + squareSide + 1;
        }

        map.forEach(e -> {
            Platform.runLater(() -> arena.getChildren().add(e.getFourth()));

            if (e.getFifth() != null)
                Platform.runLater(() -> arena.getChildren().add(e.getFifth()));

            e.getFourth().setOnMouseEntered(layerMethodNOPLAYERGUI(e));
        });

        /**Gli sfondi semi-trasparenti che conterranno i valori di gioco (Tempo per effettuare una mossa, punteggi, etc)
         * e, nel caso di un PlayerGUI, i pezzi che potranno essere usati in determinate situazioni*/
        ImageView img = new ImageView(getClass().getResource("../../resources/LEGNOVALORI.png").toString());
        img.setX(412);
        img.setY(12);

        ImageView img1 = new ImageView(getClass().getResource("../../resources/LEGNOPEZZI.png").toString());
        img1.setX(412);
        img1.setY(250);

        /**I vari Node di tipo 'Text' che conterranno i valori di gioco*/
        Text desc1 = new Text(420, 33, "Current player:");
        desc1.setFont(new Font("Comic Sans MS", 17));

        playerName = new Text(420, 57, match.getiPP().get(match.getCurrentGame().turn() - 1).getSecond().name());
        playerName.setFont(new Font("Comic Sans MS", 20));

        Shape s = new Rectangle(423,68,190,5);
        s.setFill(Color.BLACK);

        Shape s1 = new Rectangle(423, 200, 190, 5);
        s1.setFill(Color.BLACK);

        Text desc3 = new Text(420, 93, "Scores:");
        desc3.setFont(new Font("Comic Sans MS", 17));

        Text time = new Text(420, 225, "Time left: ");
        time.setFont(new Font("Comic Sans MS", 17));

        long matchTime = match.getCurrentGame().mechanics().time;
        timeFlow = matchTime == -1 ? new Text(510, 225, "--:--") : new Text(510, 225,
                "" + ((matchTime / 60000) < 10 ? "0" + (matchTime / 60000) : (matchTime / 60000)) + ":" +
                        (((matchTime % 60000) / 1000) < 10 ? "0" + ((matchTime % 60000) / 1000) : ((matchTime % 60000) / 1000)));

        timeFlow.setFont(new Font("Comic Sans MS", 17));

        /**Se è un gioco che non prevede punteggi, allora non dovranno essere mostrati durante la partita*/
        Text desc4 = null;
        try {
            match.getCurrentGame().score(1);
        } catch (UnsupportedOperationException e) {
            noScoreGame = true;
            desc4 = new Text(420, 115, "This game has no scores!");
            desc4.setFont(new Font("Comic Sans MS", 17));
        }

        if (desc4 != null) {
            Text finalDesc = desc4;
            Platform.runLater(() -> arena.getChildren().addAll(img, img1, desc1, playerName, s, desc3, finalDesc, s1, time, timeFlow));
        }

        else {
            int space = 120;

            for (int i = 0; i < match.getiPP().size(); i++) {
                Text tx = new Text(420, space, match.getiPP().get(i).getSecond().name() + ": " + ((int) match.getCurrentGame().score(i+1)));
                tx.setFont(new Font("Comic Sans MS", 20));

                playerScores.add(tx);
                space += 24;
            }

            Platform.runLater(() -> {
                arena.getChildren().addAll(img, img1, desc1, playerName, s, desc3, s1, time, timeFlow);
                playerScores.forEach(e -> arena.getChildren().add(e));
            });
        }

        masterButtons();

        /**Non appena la board è disegnata, la partita può iniziare*/
        canContinuePlaying = true;

        exitThread(desc1);
    }

    /**
     * Metodo utillizato dal {@link gapp.ulg.game.util.PlayGUI.Observer} per aggiornare la board,
     * all' invocazione del metodo {@link gapp.ulg.game.util.PlayGUI.Observer#moved(int, Move)},
     * su cui si sta giocando la partita
     */
    public void updater() {

        try {
            boardUpdaterNOPLAYERGUI();
            canContinuePlaying = true;
        } catch (NullPointerException ignored) {}

    }

    /**
     * Metodo richiamato all' interno del metodo {@link BoardGame#updater()} che aggiorna la board
     * nel caso in cui la partita non sia giocata da un giocatore umano.
     */
    private void boardUpdaterNOPLAYERGUI() {

        while (canContinuePlaying) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }

        boolean[] done = {false};
        int counter = 0;
        for (Action<PieceModel<PieceModel.Species>> a : match.getLastMove().actions) {
            counter++;

            int finalCounter = counter;
            actionDraw(a, finalCounter, done, false);

            /**Attesa tra una azione ed un altra*/
            try {Thread.sleep(DRAW_ACTION_SPEED);} catch (InterruptedException ignored) {}

            if (interrupt) {
                done[0] = true;
                canContinuePlaying = true;
                return;
            }

        }

        while (!done[0] && !interrupt)
            try {Thread.sleep(5);} catch (Exception ignored) {}

    }

    /**
     * Inizializza le {@link ImageView} rappresentanti i pulsanti che guidano l' utente nella scelta di una mossa.
     */
    private void masterButtons() {

        tickCompleteMove = new ImageView(getClass().getResource("../../resources/V.png").toString());
        tickCompleteMove.setX(452);
        tickCompleteMove.setY(264);
        tickCompleteMove.setVisible(false);

        crossRemovePiece = new ImageView(getClass().getResource("../../resources/X.png").toString());
        crossRemovePiece.setX(415);
        crossRemovePiece.setY(260);
        crossRemovePiece.setVisible(false);

        arrowMovePiece = new ImageView(getClass().getResource("../../resources/MOVE.png").toString());
        arrowMovePiece.setX(421);
        arrowMovePiece.setY(259);
        arrowMovePiece.setVisible(false);

        arrowJumpPiece = new ImageView(getClass().getResource("../../resources/JUMP.png").toString());
        arrowJumpPiece.setX(423);
        arrowJumpPiece.setY(261);
        arrowJumpPiece.setVisible(false);

        resign = new Button("Resign");
        resign.setFont(new Font("Comic Sans MS", 17));
        resign.setLayoutX(435);
        resign.setLayoutY(155);

        pass = new Button("Pass");
        pass.setFont(new Font("Comic Sans MS", 17));
        pass.setLayoutX(535);
        pass.setLayoutY(155);

    }

    /**
     * Disegna l' azione che gli viene passata in input sulla {@link Board}
     * @param a l' azione da disegnare
     * @param finalCounter l'intero utilizzato per controllare se sono state eseguite tutte le {@link Action} contenute in una {@link Move}
     * @param done indica se l' azione passata in input è stata disegnata
     * @param isPlayerGUI indica se ad eseguire l' azione passata in input è stato un {@link PlayerGUI}
     */
    private void actionDraw(Action<PieceModel<PieceModel.Species>> a, int finalCounter, boolean[] done, boolean isPlayerGUI) {

            try {

                if (!interrupt) {

                    switch (a.kind) {

                        case ADD:
                            for (ImageViewMap q : map)
                                if (a.pos.get(0).equals(q.getFirst())) {
                                    q.setFifth(graphicPiece(a.piece, squareSide, q.getSecond(), q.getThird()));
                                    boolean[] drawn = {false};

                                    Platform.runLater(() -> {
                                        arena.getChildren().add(q.getFifth());
                                        drawn[0] = true;
                                    });

                                    while (!drawn[0] && !interrupt)
                                        try {Thread.sleep(10);} catch (InterruptedException ignored) {}

                                    break;
                                }

                            break;

                        case SWAP:
                            for (ImageViewMap q : map)
                                for (Pos p : a.pos)
                                    if (p.equals(q.getFirst())) {
                                        boolean[] drawn = {false};

                                        Platform.runLater(() -> {
                                            arena.getChildren().remove(q.getFifth());
                                            drawn[0] = true;
                                        });

                                        while (!drawn[0] && !interrupt)
                                            try {Thread.sleep(10);} catch (InterruptedException ignored) {}

                                        q.setFifth(graphicPiece(a.piece, squareSide, q.getSecond(), q.getThird()));

                                        boolean[] drawn2 = {false};

                                        Platform.runLater(() -> {
                                            arena.getChildren().add(q.getFifth());
                                            drawn2[0] = true;
                                        });

                                        while (!drawn2[0] && !interrupt)
                                            try {Thread.sleep(10);} catch (InterruptedException ignored) {}

                                        break;
                                    }

                            break;

                        case REMOVE:
                            for (ImageViewMap q : map)
                                for (Pos p : a.pos)
                                    if (p.equals(q.getFirst())) {
                                        boolean[] drawn = {false};

                                        Platform.runLater(() -> {
                                            arena.getChildren().remove(q.getFifth());
                                            drawn[0] = true;
                                        });

                                        while (!drawn[0] && !interrupt)
                                            try {Thread.sleep(10);} catch (InterruptedException ignored) {}

                                        q.setFifth(null);
                                        break;
                                    }

                            break;

                        case JUMP:
                            ImageView imgJUMP = null;

                            for (ImageViewMap q : map)
                                if (a.pos.get(0).equals(q.getFirst())) {
                                    imgJUMP = q.getFifth();
                                    boolean[] drawn = {false};

                                    Platform.runLater(() -> {
                                        arena.getChildren().remove(q.getFifth());
                                        drawn[0] = true;
                                    });

                                    while (!drawn[0] && !interrupt)
                                        try {Thread.sleep(10);} catch (InterruptedException ignored) {}

                                    q.setFifth(null);
                                    break;
                                }

                            for (ImageViewMap q : map)
                                if (a.pos.get(1).equals(q.getFirst())) {
                                    imgJUMP.setX(q.getSecond());
                                    imgJUMP.setY(q.getThird());

                                    q.setFifth(imgJUMP);
                                    boolean[] drawn = {false};

                                    Platform.runLater(() -> {
                                        arena.getChildren().add(q.getFifth());
                                        drawn[0] = true;
                                    });

                                    while (!drawn[0] && !interrupt)
                                        try {Thread.sleep(10);} catch (InterruptedException ignored) {}

                                    break;
                                }

                            break;

                        case MOVE:
                            ImageView imgMOVE;

                            for (ImageViewMap q : map)
                                for (Pos p : a.pos)
                                    if (p.equals(q.getFirst())) {
                                        imgMOVE = q.getFifth();
                                        boolean[] drawn = {false};

                                        Platform.runLater(() -> {
                                            arena.getChildren().remove(q.getFifth());
                                            drawn[0] = true;
                                        });

                                        while (!drawn[0] && !interrupt)
                                            try {Thread.sleep(10);} catch (InterruptedException ignored) {}

                                        q.setFifth(null);

                                        Pos newPos = new Pos(p.b, p.t);
                                        for (int i = 0; i < a.steps; i++)
                                            newPos = match.getCurrentGame().getBoard().adjacent(newPos, a.dir);

                                        for (ImageViewMap q1 : map)
                                            if (newPos.equals(q1.getFirst())) {
                                                imgMOVE.setX(q1.getSecond());
                                                imgMOVE.setY(q1.getThird());

                                                q1.setFifth(imgMOVE);
                                                boolean[] drawn2 = {false};

                                                Platform.runLater(() -> {
                                                    arena.getChildren().add(q1.getFifth());
                                                    drawn2[0] = true;
                                                });

                                                while (!drawn2[0] && !interrupt)
                                                    try {Thread.sleep(10);} catch (InterruptedException ignored) {}

                                                break;
                                            }

                                        break;
                                    }

                            break;
                    }
                }

                if (!isPlayerGUI && finalCounter == match.getLastMove().actions.size())
                    done[0] = true;

                else if (isPlayerGUI)
                    try {Thread.sleep(DRAW_ACTION_SPEED);} catch (InterruptedException ignored) {}

            } catch (NullPointerException ignored) {}

    }

    /**
     * Metodo che aggiorna le informazioni riguardanti la partita in corso, come per esempio
     * turno e nome del giocatore, punteggi ecc.
     * @param done indica se le informazioni sono state aggiornate correttamente
     */
    public void infoUpdater(boolean[] done) {

        Platform.runLater(() -> {

            try {

                if (!interrupt) {
                    arena.getChildren().remove(playerName);

                    playerName = new Text(420, 57, match.getiPP().get(match.getCurrentGame().turn() - 1).getSecond().name());
                    playerName.setFont(new Font("Comic Sans MS", 20));
                    arena.getChildren().add(playerName);

                    if (!noScoreGame) {
                        playerScores.forEach(e -> arena.getChildren().remove(e));

                        int space = 120;
                        for (int i = 0; i < match.getiPP().size(); i++) {
                            Text tx = new Text(420, space, match.getiPP().get(i).getSecond().name() + ": " + ((int) match.getCurrentGame().score(i + 1)));
                            tx.setFont(new Font("Comic Sans MS", 20));
                            arena.getChildren().add(tx);
                            playerScores.add(tx);
                            space += 24;
                        }

                    }

                }

            } catch (NullPointerException | ArrayIndexOutOfBoundsException ignored) {}

            done[0] = true;
        });

    }

    /**
     * Ritorna {@link ImageView} per il giocatore di turno rappresentante il pezzo di {@link gapp.ulg.game.board.PieceModel.Species} sp passata in input
     * @param sp la specie4 del pezzo
     * @param x la traslazione sull' asse x del pezzo
     * @param y la traslazione sull' asse y del pezzo
     * @return {@link ImageView} del pezzo
     */
    private ImageView playerGUIpiece(PieceModel.Species sp, int x, int y) {
        return match.getCurrentGame().turn() == 1 ? graphicPiece(new PieceModel<>(sp, "nero"), 55, x, y) :
                graphicPiece(new PieceModel<>(sp, "bianco"), 55, x, y);
    }

    /**
     * Metodo che aggiorna la selezione corrente e le mosse possibili con tale selezione
     * @param tree il {@link gapp.ulg.game.util.PlayerGUI.MoveChooser} attuale
     * @param currentBoardSelection le posizioni selezionate
     * @param selectedMoves le mosse possibili data la selezione
     * @param piecesList i pezzi ammissibili date le mosse possibili
     * @param noJUMP_MOVE indica se l' azione è di tipo {@link gapp.ulg.game.board.Action.Kind#JUMP} o {@link gapp.ulg.game.board.Action.Kind#MOVE}
     */
    private void csANDsmUpdater(PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>> tree, List<Pos> currentBoardSelection,
                                List<Move<PieceModel<PieceModel.Species>>> selectedMoves, List<PieceModel<PieceModel.Species>> piecesList,
                                boolean noJUMP_MOVE) {

        if (!noJUMP_MOVE) {
            Pos[] posArray = new Pos[currentBoardSelection.size()];
            posArray = currentBoardSelection.toArray(posArray);
            selectedMoves.clear();
            piecesList.clear();

            if (posArray.length > 0)
                selectedMoves.addAll(tree.select(posArray));

            else
                ((Tree<PieceModel<PieceModel.Species>>) tree).setCurrentSelectedMoves(new ArrayList<>());

            piecesList.addAll(tree.selectionPieces());
        }

        else {

            final Move<PieceModel<PieceModel.Species>>[] posFound = genericArrayCreation((Move<PieceModel<PieceModel.Species>>) null);
            selectedMoves.forEach(m -> {
                Action<PieceModel<PieceModel.Species>> a = m.actions.get(0);

                if (a.kind.equals(Action.Kind.MOVE) && currentBoardSelection.get(1).equals(((Tree<PieceModel<PieceModel.Species>>) tree).backActionMove(a.pos.get(0), a.dir, a.steps))) {
                    posFound[0] = m;
                    arrowMovePiece.setVisible(true);
                }

                else if (a.kind.equals(Action.Kind.JUMP) && currentBoardSelection.get(1).equals(a.pos.get(1))) {
                    posFound[0] = m;
                    arrowJumpPiece.setVisible(true);
                }

            });

            if (posFound[0] != null) {
                selectedMoves.clear();
                selectedMoves.add(posFound[0]);
            }

            else {
                Pos[] posArray = new Pos[currentBoardSelection.size()];
                posArray = currentBoardSelection.toArray(posArray);
                selectedMoves.clear();
                piecesList.clear();

                if (posArray.length > 0)
                    selectedMoves.addAll(tree.select(posArray));

                else
                    ((Tree<PieceModel<PieceModel.Species>>) tree).setCurrentSelectedMoves(new ArrayList<>());

                piecesList.addAll(tree.selectionPieces());
            }

        }

    }

    /**
     * Crea il master per un giocatore di tipo {@link PlayerGUI}
     * @return il master
     */
    private Consumer<PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>>> master() {

        return tree -> {

            if (tree.subMove().isPresent()) {

                ((Tree<PieceModel<PieceModel.Species>>) tree).getTree().add(0, new Pair<>(new Nodo<>(new ArrayList<>()),
                        Collections.singletonList(((Tree<PieceModel<PieceModel.Species>>) tree).getTree().get(0).getFirst())));

                ((Tree<PieceModel<PieceModel.Species>>) tree).setCurrentNode(((Tree<PieceModel<PieceModel.Species>>) tree).getTree().get(0).getFirst());
            }

            currentBoardSelection = new ArrayList<>();
            selectedMoves = new ArrayList<>();
            piecesList = new ArrayList<>();
            speciesToImageView = new HashMap<>();
            subMove = genericArrayCreation((Move<PieceModel<PieceModel.Species>>) null);

            tickCompleteMove.setVisible(false);
            crossRemovePiece.setVisible(false);
            arrowMovePiece.setVisible(false);
            arrowJumpPiece.setVisible(false);

            resign.setOnAction(e -> {
                playerGUImanualRESIGN = true;
                tree.resign();
                interrupt = true;

                for (ImageView img : speciesToImageView.values())
                    img.setVisible(false);

                crossRemovePiece.setVisible(false);
                arrowMovePiece.setVisible(false);
                arrowJumpPiece.setVisible(false);

                Platform.runLater(() -> {
                    menu.getRoot().getChildren().remove(game.getNode());
                    menu.getRoot().setBackground(new Background(new BackgroundFill(Color.FLORALWHITE,null,null)));
                });

            });

            pass.setDisable(!tree.mayPass());

            pass.setOnAction(e -> {
                backAction.setDisable(true);

                Platform.runLater(() -> arena.getChildren().remove(layer));

                map.forEach(e1 -> Platform.runLater(() -> {
                    e1.getFourth().setOnMouseEntered(layerMethodNOPLAYERGUI(e1));

                    if (e1.getSixth() != null)
                        arena.getChildren().remove(e1.getSixth());

                }));

                for (ImageView img : speciesToImageView.values())
                    img.setVisible(false);

                Platform.runLater(() -> arena.getChildren().removeAll(resign, pass, tickCompleteMove, crossRemovePiece, arrowMovePiece, arrowJumpPiece));
                tree.pass();
            });

            tickCompleteMove.setOnMouseClicked(e -> {
                backAction.setDisable(true);

                Platform.runLater(() -> arena.getChildren().remove(layer));

                map.forEach(e1 -> Platform.runLater(() -> {
                    e1.getFourth().setOnMouseEntered(layerMethodNOPLAYERGUI(e1));

                    if (e1.getSixth() != null)
                        arena.getChildren().remove(e1.getSixth());

                }));

                Platform.runLater(() -> arena.getChildren().removeAll(resign, pass, tickCompleteMove, crossRemovePiece, arrowMovePiece, arrowJumpPiece));
                tree.move();
            });

            crossRemovePiece.setOnMouseClicked(remove_move__jumpPieces(tree, selectedMoves, Action.Kind.REMOVE));
            arrowMovePiece.setOnMouseClicked(remove_move__jumpPieces(tree, selectedMoves, Action.Kind.MOVE));
            arrowJumpPiece.setOnMouseClicked(remove_move__jumpPieces(tree, selectedMoves, Action.Kind.JUMP));

            Platform.runLater(() -> arena.getChildren().addAll(resign, pass, tickCompleteMove, crossRemovePiece, arrowMovePiece, arrowJumpPiece));

            /**Metto gli 8 pezzi di gioco in basso a destra, mostrandoli in caso di bisogno*/
            ImageView disc = playerGUIpiece(PieceModel.Species.DISC, 418, 260);
            ImageView dama = playerGUIpiece(PieceModel.Species.DAMA, 473, 260);
            ImageView pawn = playerGUIpiece(PieceModel.Species.PAWN, 518, 260);
            ImageView knight = playerGUIpiece(PieceModel.Species.KNIGHT, 563, 260);
            ImageView bishop = playerGUIpiece(PieceModel.Species.BISHOP, 418, 325);
            ImageView rook = playerGUIpiece(PieceModel.Species.ROOK, 473, 325);
            ImageView queen = playerGUIpiece(PieceModel.Species.QUEEN, 518, 325);
            ImageView king = playerGUIpiece(PieceModel.Species.KING, 563, 325);

            speciesToImageView.put(PieceModel.Species.DISC, disc);
            speciesToImageView.put(PieceModel.Species.DAMA, dama);
            speciesToImageView.put(PieceModel.Species.PAWN, pawn);
            speciesToImageView.put(PieceModel.Species.KNIGHT, knight);
            speciesToImageView.put(PieceModel.Species.BISHOP, bishop);
            speciesToImageView.put(PieceModel.Species.ROOK, rook);
            speciesToImageView.put(PieceModel.Species.QUEEN, queen);
            speciesToImageView.put(PieceModel.Species.KING, king);

            for (ImageView img : speciesToImageView.values()) {
                img.setVisible(false);
                Platform.runLater(() -> arena.getChildren().add(img));

                /**Se qualcuna delle 8 immagini dei pezzi gioco è cliccabile, vuol dire che il metodo selectionPieces() ritorna una lista
                 * non vuota, eseguo quindi un doSelection() del pezzo cliccato, sposto il nodo nell'albero delle mosse,
                 * elimino tutti i quadrati verdi della precedente selezione, e eseguo la mossa sulla board*/
                img.setOnMouseClicked(clickedPieces(img, tree));

            }

            map.forEach(e -> Platform.runLater(() -> {
                e.getFourth().setOnMouseEntered(layerMethodPLAYERGUI(e, tree, false));

                if (e.getFifth() != null)
                    e.getFifth().setOnMouseEntered(layerMethodPLAYERGUI(e, tree, false));

            }));

            Platform.runLater(() -> arena.getChildren().remove(layer));
        };

    }

    /**
     * Il metodo principale utilizzato da un {@link gapp.ulg.game.util.PlayerGUI} per comunicare
     * con la GUI: permette, alla pressione di un oggetto della board, di mostrare sulla GUI quali sono
     * le {@link Move} possibili con le posizioni selezionate, abilitando e disabilitando
     * eventuali pulsanti
     * @param tree il {@link gapp.ulg.game.util.PlayerGUI.MoveChooser} attuale
     * @param currentBoardSelection le posizioni selezionate
     * @param selectedMoves le mosse possibili data la selezione
     * @param piecesList i pezzi ammissibili date le mosse possibili
     * @param speciesToImageView l'oggetto che mappa la {@link gapp.ulg.game.board.PieceModel.Species} con la corrispondente {@link ImageView}
     * @return lo {@link EventHandler} attivato alla pressione del {@link BoardGame#layer}
     */
    private EventHandler<? super MouseEvent> boardMaster(PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>> tree, List<Pos> currentBoardSelection,
                                                         List<Move<PieceModel<PieceModel.Species>>> selectedMoves, List<PieceModel<PieceModel.Species>> piecesList,
                                                         Map<PieceModel.Species, ImageView> speciesToImageView) {
        return e1 -> {

            try {

                map.forEach(e3 -> {

                    if (e3.getFirst().equals(layerPos)) {
                        Shape shp = new Rectangle(e3.getSecond(), e3.getThird(), squareSide, squareSide);
                        shp.setFill(new Color(0.0f, 0.5019608f, 0.0f, 0.5f));

                        e3.setSixth(shp);
                        e3.getSixth().setOnMouseClicked(e2 -> {
                            Platform.runLater(() -> arena.getChildren().remove(e3.getSixth()));

                            currentBoardSelection.remove(e3.getFirst());
                            csANDsmUpdater(tree, currentBoardSelection, selectedMoves, piecesList, false);

                            speciesToImageView.values().forEach(img -> img.setVisible(false));
                            crossRemovePiece.setVisible(false);
                            arrowMovePiece.setVisible(false);
                            arrowJumpPiece.setVisible(false);
                            piecesList.forEach(p -> {

                                if (p == null)
                                    crossRemovePiece.setVisible(true);

                                if (p != null)
                                    speciesToImageView.get(p.species).setVisible(true);

                            });

                        });

                        e3.getSixth().setOnMouseEntered(e2 -> arena.getChildren().remove(layer));

                        Platform.runLater(() -> {
                            arena.getChildren().remove(layer);
                            arena.getChildren().add(shp);
                        });

                        currentBoardSelection.add(e3.getFirst());

                        speciesToImageView.values().forEach(img -> img.setVisible(false));
                        crossRemovePiece.setVisible(false);
                        arrowMovePiece.setVisible(false);
                        arrowJumpPiece.setVisible(false);

                        /**Se ci sono solo mosse MOVE/JUMP disponibili, passo ad una diversa gestione delle mosse possibili*/
                        if (currentBoardSelection.size() != 2)
                            csANDsmUpdater(tree, currentBoardSelection, selectedMoves, piecesList, false);

                        else {
                            boolean jump_move = true;

                            for (Move<PieceModel<PieceModel.Species>> m : selectedMoves)
                                if (!Arrays.asList(Action.Kind.JUMP, Action.Kind.MOVE).contains(m.actions.get(0).kind)) {
                                    jump_move = false;
                                    break;
                                }

                            csANDsmUpdater(tree, currentBoardSelection, selectedMoves, piecesList, jump_move);
                        }

                        piecesList.forEach(p -> {

                            if (p == null)
                                crossRemovePiece.setVisible(true);

                            else
                                speciesToImageView.get(p.species).setVisible(true);

                        });

                    }

                });

            } catch (NullPointerException ignored) {}

        };

    }

    /**
     * Metodo che permette, al passaggio del mouse su un oggetto della board, di mostrare una
     * {@link Shape} gialla per giocatori non di tipo {@link gapp.ulg.game.util.PlayerGUI}
     * @param e La {@link ImageViewMap} della posizione su cui il mouse è presente
     * @return lo {@link EventHandler} attivato al passaggio del mouse
     */
    private EventHandler<? super MouseEvent> layerMethodNOPLAYERGUI(ImageViewMap e) {

        return e1 -> {
            arena.getChildren().remove(layer);

            try {
                if (match.getCurrentGame().getBoard().isPos(e.getFirst())) {
                    layerPos = new Pos(e.getFirst().b, e.getFirst().t);

                    if (e.getFifth() == null) {
                        layer = new Rectangle(e.getSecond(), e.getThird(), squareSide, squareSide);
                        layer.setFill(new Color(1.0f, 0.85f, 0.0f, 1.0f));
                        arena.getChildren().add(layer);

                    } else {
                        layer = new Rectangle(e.getSecond(), e.getThird(), squareSide, squareSide);
                        layer.setFill(new Color(1.0f, 0.85f, 0.0f, 0.4f));
                        arena.getChildren().add(layer);

                    }

                }

            } catch (NullPointerException ignored) {}

        };
    }

    /**
     * Metodo che permette, al passaggio del mouse su un oggetto della board, di mostrare una
     * {@link Shape} gialla per giocatori di tipo {@link gapp.ulg.game.util.PlayerGUI}
     * @param e La {@link ImageViewMap} della posizione su cui il mouse è presente
     * @param tree il {@link gapp.ulg.game.util.PlayerGUI.MoveChooser} attuale
     * @param pause il booleano che disattiva temporaneamente gli eventi ssulla board
     * @return lo {@link EventHandler} attivato al passaggio del mouse
     */
    private EventHandler<? super MouseEvent> layerMethodPLAYERGUI(ImageViewMap e, PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>> tree, boolean pause) {

        return e1 -> {
            arena.getChildren().remove(layer);

            try {
                if (match.getCurrentGame().getBoard().isPos(e.getFirst())) {
                    layerPos = new Pos(e.getFirst().b, e.getFirst().t);

                    if (e.getFifth() == null) {
                        layer = new Rectangle(e.getSecond(), e.getThird(), squareSide, squareSide);
                        layer.setFill(new Color(1.0f, 0.85f, 0.0f, 1.0f));
                        arena.getChildren().add(layer);

                    } else {
                        layer = new Rectangle(e.getSecond(), e.getThird(), squareSide, squareSide);
                        layer.setFill(new Color(1.0f, 0.85f, 0.0f, 0.4f));
                        arena.getChildren().add(layer);

                    }

                }

                if (!pause)
                    layer.setOnMouseClicked(boardMaster(tree, currentBoardSelection, selectedMoves, piecesList, speciesToImageView));

            } catch (NullPointerException ignored) {}

        };
    }

    /**
     * Metodo che permette di eseguire una {@link Move} tramite il metodo {@link Tree#doSelection(Object)}
     * @param img La {@link ImageView} del pulsante cliccato
     * @param tree il {@link gapp.ulg.game.util.PlayerGUI.MoveChooser} attuale
     * @return lo {@link EventHandler} attivato al click del mouse
     */
    private EventHandler<? super MouseEvent> clickedPieces(ImageView img, PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>> tree) {

        return e -> {

            /**Disattivo tutti i pezzi e le caselle, poichè non deve essere possibile
             * fare ulteriori mosse mentre se ne sta gia eseguendo un'altra*/
            map.forEach(e1 -> Platform.runLater(() -> {
                e1.getFourth().setOnMouseEntered(layerMethodPLAYERGUI(e1, tree, true));

                if (e1.getFifth() != null)
                    e1.getFifth().setOnMouseEntered(layerMethodPLAYERGUI(e1, tree, true));

                resign.setDisable(true);
                pass.setDisable(true);
            }));

            for (PieceModel.Species p : speciesToImageView.keySet())
                if (speciesToImageView.get(p).equals(img)) {
                    subMove[0] = tree.doSelection(new PieceModel<>(p, match.getCurrentGame().turn() == 1 ? "nero" : "bianco"));
                    currentBoardSelection.clear();
                    selectedMoves.clear();
                    piecesList.clear();


                    map.forEach(e1 -> {
                        if (e1.getSixth() != null)
                            arena.getChildren().remove(e1.getSixth());
                    });

                    for (ImageView img2 : speciesToImageView.values())
                        img2.setVisible(false);

                    Thread drawSubMove = new Thread(() -> {

                        for (Action<PieceModel<PieceModel.Species>> a : subMove[0].actions) {
                            actionDraw(a, 0, null, true);

                            if (interrupt) {
                                canContinuePlaying = true;
                                return;
                            }

                        }

                    });

                    drawSubMove.setDaemon(true);
                    drawSubMove.start();

                    /**Eseguita l'intera mossa, riattivo i pulsanti*/
                    Thread newPiecesActivator = new Thread(() -> {
                        try {Thread.sleep((subMove[0].actions.size() * DRAW_ACTION_SPEED) + 100);} catch (InterruptedException ignored) {}

                        map.forEach(e1 -> Platform.runLater(() -> {
                            e1.getFourth().setOnMouseEntered(layerMethodPLAYERGUI(e1, tree, false));

                            if (e1.getFifth() != null)
                                e1.getFifth().setOnMouseEntered(layerMethodPLAYERGUI(e1, tree, false));

                        }));

                        layer.setOnMouseClicked(boardMaster(tree, currentBoardSelection, selectedMoves, piecesList, speciesToImageView));

                        if (tree.isFinal())
                            tickCompleteMove.setVisible(true);

                        resign.setDisable(false);
                        pass.setDisable(!tree.mayPass());

                        /**Abilito il pulsante per fare l'undo della precedente mossa, nel caso voglia sceglierne un'altra*/
                        Platform.runLater(() -> {
                            backAction.setDisable(false);
                            backAction.setOnAction(undoAction(tree));
                        });

                    });

                    newPiecesActivator.setDaemon(true);
                    newPiecesActivator.start();
                    break;
                }

        };

    }

    /**
     * Metodo che permette di eseguire {@link Move} tramite i metodi {@link Tree#jumpSelection(Pos)}, {@link Tree#moveSelection(Board.Dir, int)}}
     * e, in caso di {@link gapp.ulg.game.board.Action.Kind#REMOVE}, tramite il metodo {@link Tree#doSelection(Object)}
     * @param tree il {@link gapp.ulg.game.util.PlayerGUI.MoveChooser} attuale
     * @param selectedMoves le mosse possibili data la selezione
     * @param kind il tipo di {@link Action} da eseguire
     * @return lo {@link EventHandler} attivato al click del mouse
     */
    private EventHandler<? super MouseEvent> remove_move__jumpPieces(PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>> tree,
                                                                List<Move<PieceModel<PieceModel.Species>>> selectedMoves, Action.Kind kind) {

        return e -> {
            crossRemovePiece.setVisible(false);
            arrowMovePiece.setVisible(false);
            arrowJumpPiece.setVisible(false);

            /**Disattivo tutti i pezzi e le caselle, poichè non deve essere possibile
             * fare ulteriori mosse mentre se ne sta gia eseguendo un'altra*/
            map.forEach(e1 -> Platform.runLater(() -> {
                e1.getFourth().setOnMouseEntered(layerMethodPLAYERGUI(e1, tree, true));

                if (e1.getFifth() != null)
                    e1.getFifth().setOnMouseEntered(layerMethodPLAYERGUI(e1, tree, true));

                resign.setDisable(true);
                pass.setDisable(true);
            }));

            switch (kind) {

                case REMOVE:
                    subMove[0] = tree.doSelection(null);
                    break;

                case MOVE:
                    subMove[0] = tree.moveSelection(selectedMoves.get(0).actions.get(0).dir, selectedMoves.get(0).actions.get(0).steps);
                    break;

                case JUMP:
                    subMove[0] = tree.jumpSelection(selectedMoves.get(0).actions.get(0).pos.get(1));
                    break;

            }

            currentBoardSelection.clear();
            selectedMoves.clear();
            piecesList.clear();

            map.forEach(e1 -> {
                if (e1.getSixth() != null)
                    arena.getChildren().remove(e1.getSixth());
            });

            for (ImageView img2 : speciesToImageView.values())
                img2.setVisible(false);

            Thread drawSubMove = new Thread(() -> {

                for (Action<PieceModel<PieceModel.Species>> a : subMove[0].actions) {
                    actionDraw(a, 0, null, true);

                    if (interrupt) {
                        canContinuePlaying = true;
                        return;
                    }

                }

            });

            drawSubMove.setDaemon(true);
            drawSubMove.start();

            /**Eseguita l'intera mossa, riattivo i pulsanti*/
            Thread newPiecesActivator = new Thread(() -> {
                try {Thread.sleep((subMove[0].actions.size() * DRAW_ACTION_SPEED) + 100);} catch (InterruptedException ignored) {}

                map.forEach(e1 -> Platform.runLater(() -> {
                    e1.getFourth().setOnMouseEntered(layerMethodPLAYERGUI(e1, tree, false));

                    if (e1.getFifth() != null)
                        e1.getFifth().setOnMouseEntered(layerMethodPLAYERGUI(e1, tree, false));

                }));

                layer.setOnMouseClicked(boardMaster(tree, currentBoardSelection, selectedMoves, piecesList, speciesToImageView));

                if (tree.isFinal())
                    tickCompleteMove.setVisible(true);

                resign.setDisable(false);
                pass.setDisable(!tree.mayPass());

                /**Abilito il pulsante per fare l'undo della precedente mossa, nel caso voglia sceglierne un'altra*/
                backAction.setDisable(false);
                backAction.setOnAction(undoAction(tree));
            });

            newPiecesActivator.setDaemon(true);
            newPiecesActivator.start();

        };

    }

    /**
     * Metodo che permette ad un {@link gapp.ulg.game.util.PlayerGUI} di eseguire un {@link Tree#back()}
     * tramite GUI
     * @param tree il {@link gapp.ulg.game.util.PlayerGUI.MoveChooser} attuale
     * @return lo {@link EventHandler} attivato al click del mouse
     */
    private EventHandler<ActionEvent> undoAction(PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>> tree) {

        return e1 -> {

            /**Disattivo tutti i pezzi e le caselle, poichè non deve essere possibile
             * fare ulteriori mosse mentre se ne sta gia eseguendo un'altra*/
            map.forEach(e2 -> Platform.runLater(() -> {
                e2.getFourth().setOnMouseEntered(layerMethodPLAYERGUI(e2, tree, true));

                if (e2.getFifth() != null)
                    e2.getFifth().setOnMouseEntered(layerMethodPLAYERGUI(e2, tree, true));

                resign.setDisable(true);
                pass.setDisable(true);
            }));

            /**Se mi trovavo alla fine di una mossa valida, nascondo il tick verde*/
            if (tree.isFinal())
                tickCompleteMove.setVisible(false);

            Move<PieceModel<PieceModel.Species>> back = tree.back();
            currentBoardSelection.clear();
            selectedMoves.clear();
            piecesList.clear();

            /**Elimino i quadrati verdi*/
            map.forEach(e2 -> {
                if (e2.getSixth() != null)
                    arena.getChildren().remove(e2.getSixth());
            });

            /**Nascondo le immagini dei pezzi di gioco e l'eventuale croce del REMOVE*/
            crossRemovePiece.setVisible(false);

            for (ImageView img2 : speciesToImageView.values())
                img2.setVisible(false);

            /**Disegno su board la mossa inversa*/
            Thread drawSubMove = new Thread(() -> {

                for (Action<PieceModel<PieceModel.Species>> a : back.actions) {
                    actionDraw(a, 0, null, true);

                    if (interrupt) {
                        canContinuePlaying = true;
                        return;
                    }

                }

            });

            drawSubMove.setDaemon(true);
            drawSubMove.start();

            /**Eseguita l'intera mossa inversa, riattivo i pulsanti*/
            Thread newPiecesActivator = new Thread(() -> {
                try {Thread.sleep((back.actions.size() * DRAW_ACTION_SPEED) + 100);} catch (InterruptedException ignored) {}

                map.forEach(e2 -> Platform.runLater(() -> {
                    e2.getFourth().setOnMouseEntered(layerMethodPLAYERGUI(e2, tree, false));

                    if (e2.getFifth() != null)
                        e2.getFifth().setOnMouseEntered(layerMethodPLAYERGUI(e2, tree, false));

                }));

                layer.setOnMouseClicked(boardMaster(tree, currentBoardSelection, selectedMoves, piecesList, speciesToImageView));

                if (tree.isFinal())
                    tickCompleteMove.setVisible(true);

                resign.setDisable(false);
                pass.setDisable(!tree.mayPass());
            });

            newPiecesActivator.setDaemon(true);
            newPiecesActivator.start();

            if (((Tree<PieceModel<PieceModel.Species>>) tree).parent(((Tree<PieceModel<PieceModel.Species>>) tree).getCurrentNode()) == null)
                backAction.setDisable(true);

        };

    }

    /**
     * Metodo che crea il {@link Thread} finale, in cui viene inizializzato e mostrato
     * lo {@link Stage} finale, contenente il messaggio dello {@link gapp.ulg.game.util.PlayGUI.Observer}
     * riguardo a come sia terminata la partita
     * @param desc1 il {@link Text} contenente il giocatore di turno
     */
    private void exitThread(Text desc1) {

        Thread comp = new Thread(() -> {

            while (match != null && match.runningMatch && !interrupt)
                try {Thread.sleep(30);} catch (InterruptedException ignored) {}

            while (!canContinuePlaying && !interrupt)
                try {Thread.sleep(30);} catch (InterruptedException ignored) {}

            map.forEach(e -> {
                e.getFourth().setOnMouseEntered(Event::consume);

                if (e.getSixth() != null)
                    Platform.runLater(() -> arena.getChildren().remove(e.getSixth()));

            });

            Platform.runLater(() -> arena.getChildren().remove(layer));

            /**Se sono presenti anche i pulsanti 'resign', 'pass' e il tick per scegliere una mossa, li rimuovo*/
            if (arena.getChildren().containsAll(Arrays.asList(resign, pass, tickCompleteMove, crossRemovePiece, arrowMovePiece, arrowJumpPiece)))
                Platform.runLater(() -> arena.getChildren().removeAll(Arrays.asList(resign, pass, tickCompleteMove, crossRemovePiece, arrowMovePiece, arrowJumpPiece)));

            Platform.runLater(() -> {

                backAction.setDisable(true);
                stopMatch.setDisable(true);

                if (!stopMatchBUTTON)
                    mainMenu.setDisable(false);
            });

            for (ImageView img : speciesToImageView.values())
                img.setVisible(false);

            /**Creazione dello Stage di fine partita*/
            Text tx1 = observerMessage == null ? new Text("Game Over") : new Text(observerMessage);
            observerMessage = null;

            if (tx1.getText().equals("Game Over")) {

                if (result == 0) {
                    desc1.setText("It's a draw");
                    desc1.setX(430);
                    desc1.setY(46);
                    playerName.setText("");
                }
                else {
                    desc1.setText("The winner is:");
                    playerName.setText(winner);
                }

            }

            tx1.setFont(new Font("Comic Sans MS", 14));
            VBox hb1 = new VBox(tx1);
            hb1.setSpacing(15);
            hb1.setAlignment(javafx.geometry.Pos.CENTER);

            Platform.runLater(() -> {
                endingStage = new Stage();
                endingStage.setScene(new Scene(hb1, 290, 80));
                endingStage.setTitle("Pop-up");
                endingStage.setResizable(false);
                endingStage.sizeToScene();
                endingStage.show();
            });

            try {Thread.sleep(2000);} catch (InterruptedException ignored) {}

            Platform.runLater(() -> endingStage.hide());

            if (stopMatchBUTTON || playerGUImanualRESIGN) {
                Platform.runLater(() -> {
                    menu.getRoot().getChildren().add(menu.getGameMenu());
                    menu.getRoot().getChildren().add(textIntro);

                    menu.getRoot().setBackground(new Background(new BackgroundImage(new Image(getClass().getResource("../../resources/First_Screen_View.jpg").toString()),
                            BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                            BackgroundSize.DEFAULT)));

                    stage.sizeToScene();
                });

            }

        });

        comp.setDaemon(true);
        comp.start();
    }

    /**
     * Metodo che, all'inizio di ogni turno, crea un {@link Thread} in cui viene fatto scorrere il tempo,
     * a sua volta mostrato sulla board di gioco
     */
    public void counterThread() {

        Thread contaSecondi = new Thread(() -> {

            try {
                long matchTime = match.getCurrentGame().mechanics().time;

                while (canContinuePlaying && !interrupt && match.isRunningMatch()) {

                    long finalMatchTime = matchTime;
                    Platform.runLater(() -> {
                        arena.getChildren().remove(timeFlow);

                        timeFlow = finalMatchTime == -1 ? new Text(510, 225, "--:--") : new Text(510, 225,
                                "" + ((finalMatchTime / 60000) < 10 ? "0" + (finalMatchTime / 60000) : (finalMatchTime / 60000)) + ":" +
                                        (((finalMatchTime % 60000) / 1000) < 10 ? "0" + ((finalMatchTime % 60000) / 1000) : ((finalMatchTime % 60000) / 1000)));

                        timeFlow.setFont(new Font("Comic Sans MS", 17));

                        if (finalMatchTime <= 5000)
                            timeFlow.setFill(Color.RED);

                        arena.getChildren().add(timeFlow);
                    });

                    int c = 0;
                    boolean stopcounting = false;
                    while (c < 100) {

                        if (stopCounting || !canContinuePlaying || interrupt) {
                            stopcounting = true;
                            break;
                        }

                        try {Thread.sleep(10);} catch (InterruptedException ignored) {}
                        c++;
                    }

                    if (stopcounting)
                        break;

                    matchTime = matchTime - 1000;
                }

                Platform.runLater(() -> {
                    arena.getChildren().remove(timeFlow);
                    timeFlow = new Text(510, 225, "--:--");
                    timeFlow.setFont(new Font("Comic Sans MS", 17));
                    arena.getChildren().add(timeFlow);
                });

            } catch (NullPointerException ignored) {}

        });

        contaSecondi.setDaemon(true);
        contaSecondi.start();
    }

    /**
     * Metodo che permette di ritornare un array generico contenente oggetti di tipo {@link Move}
     * @param moves le {@link Move} che riempiranno l'array
     * @return l'array 'generico'
     */
    @SafeVarargs
    private final Move<PieceModel<PieceModel.Species>>[] genericArrayCreation(Move<PieceModel<PieceModel.Species>>... moves) {
        return moves;
    }

    /**
     * Metodo che, preso in input un {@link PieceModel}, ritorna la {@link ImageView} corrispondente
     * @param pezzo il {@link PieceModel}
     * @param latoQuadrato la lunghezza in pixel del quadrato della board che conterrà la {@link ImageView}
     * @param cornerX la x dell'angolo superiore sinistro della {@link ImageView}
     * @param cornerY la y dell'angolo superiore sinistro della {@link ImageView}
     * @return la {@link ImageView} corrispondente
     */
    private ImageView graphicPiece(PieceModel<PieceModel.Species> pezzo, int latoQuadrato, int cornerX, int cornerY) {

        if (pezzo != null) {

            ImageView img = new ImageView(getClass().getResource("../../resources/" +
                    pieceToString.get(pezzo.species) + colorToString.get(pezzo.color) + ".png").toString());

            img.setFitHeight(latoQuadrato);
            img.setFitWidth(latoQuadrato);
            img.setX(cornerX);
            img.setY(cornerY);

            return img;
        }

        return null;
    }

    /**
     * Metodo che ritorna la {@link ImageView} marrone che verrà poi posizionata sulla board,
     * prima dell'inizio della partita
     * @param color la stringa che indica se la {@link ImageView} dovrà essere marrone scura o marrone chiara
     * @param latoQuadrato la lunghezza in pixel del quadrato della board
     * @param cornerX la x dell'angolo superiore sinistro della sua posizione
     * @param cornerY la y dell'angolo superiore sinistro della sua posizione
     * @return la {@link ImageView} corrispondente
     */
    private ImageView woodPiece(String color, int latoQuadrato, int cornerX, int cornerY) {
        ImageView img = new ImageView(getClass().getResource("../../resources/" + color + ".png").toString());

        img.setFitHeight(latoQuadrato);
        img.setFitWidth(latoQuadrato);
        img.setX(cornerX);
        img.setY(cornerY);

        return img;
    }

    /**
     * @return il nodo principale della classe
     */
    Node getNode() { return area; }

    /**
     * @return il master per un oggetto {@link PlayerGUI}
     */
    Consumer<PlayerGUI.MoveChooser<PieceModel<PieceModel.Species>>> getMaster() {
        return master;
    }

    /**
     * @return la lista contenente tutte le ImageViewMap
     * @see ImageViewMap
     */
    public List<ImageViewMap> getMap() {
        return map;
    }

}
