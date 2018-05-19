package gapp.gui;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.util.PlayGUI;
import gapp.ulg.game.util.UpdaterGUI;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Raccolta delle variabili di tipo final e static
 */
public class Variables {

    /**
     * Il titolo della GUI
     */
    static final String ULG_TITLE = "UNIVERSAL LIBRARY FOR GAMES";

    /**
     * Le dimensioni del {@link javafx.scene.canvas.Canvas} in {@link BoardGame}
     */
    static final int CANVAS_WIDHT = 650, CANVAS_HEIGHT = 450;

    /**
     * Le dimensioni del nodo principale della GUI.
     */
    static final int MAIN_ROOT_WIDHT = 800, MAIN_ROOT_HEIGHT = 600;

    /**
     * Le dimensioni della scena principale
     */
    static final int MAIN_SCENE_WIDHT = 1070, MAIN_SCENE_HEIGHT = 600;

    /**
     * Il font generale della GUI
     */
    static final Font MAIN_FONT = new Font("Comic Sans MS",20);

    /**
     * La dimensione del font generale della GUI
     */
    static final double FONT_SIZE = 15;

    /**
     * La dimensione dello spazio tra un elemento e l' altro nei layout principali che compongono il menu di selezione di un gioco e le sue impostazioni
     */
    static final double FACTORY_CHOOSER_LAYOUT_SPACING = 15, PLAYER_CHOOSER_LAYOUT_SPACING = 15, PARAM_CHOOSER_LAYOUT_SPACING = 15;

    /**
     * Il colore principale dei titoli e delle descrizioni degli oggetti della GUI
     */
    static final Color SCREEN_TITLE_DEFAULT_COLOR = Color.RED, SCREEN_SUBTITLE_DEFAULT_COLOR = Color.LIGHTGREY;


    /**
     * Limite di tempo generale per un blocco nel thread di confinamento
     * @see {@link PlayGUI#PlayGUI(PlayGUI.Observer, long)}
     */
    static int MAXBLOCKTIME = - 1;

    /**
     * La velocità con cui vengono disegnati i pezzi sulla board di gioco
     */
    static long DRAW_ACTION_SPEED = 500;

    /**
     * Lo stage principale
     */
    static Stage stage;

    /**
     * Il menu navigabile
     * @see GameMenu
     */
    static GameMenu menu;

    /**
     * Il testo all' avvio dell' applicazione
     */
    static Text textIntro;

    /**
     * L' "arbitro" di un qualsiasi match
     * @see gapp.ulg.game.util.PlayGUI.Observer
     */
    static PlayGUI.Observer<PieceModel<PieceModel.Species>> arbitro = new UpdaterGUI<>();

    /**
     * L' oggetto che facilita la partita gestita dalla GUI
     * @see PlayGUI
     */
    public static PlayGUI<PieceModel<PieceModel.Species>> match = new PlayGUI<>(arbitro, 500);

    /**
     * Lo stage di fine partita
     */
    static Stage endingStage = null;

    /**
     * La mappa che associa ad ogni {@link gapp.ulg.game.board.PieceModel.Species} la stringa corrispondente
     */
    static Map<PieceModel.Species, String> pieceToString = new HashMap<>();

    /**
     * La mappa che associa ad ogni colore la stringa corrispondente
     */
    static Map<String, String> colorToString = new HashMap<>();

    /**
     * La stringa che rappresenta il messaggio del {@link gapp.ulg.game.util.PlayGUI.Observer} al termine di una partita
     */
    public static volatile String observerMessage = null;

    /**
     * Il booleano che comunica al {@link gapp.ulg.game.util.PlayGUI.Observer} ed al {@link gapp.ulg.game.util.PlayGUI},
     * in caso di utilizzo della GUI, che una partita è terminata o stata interrotta
     */
    public static volatile boolean interrupt = false;

    /**
     * Il booleano che comunica alla GUI che è stata interrotta la computazione della strategia per una o più {@link gapp.ulg.game.PlayerFactory}
     */
    static volatile boolean tryComputeInterrupt = false;

    /**
     * Il {@link Supplier} che cambia il valore di {@link Variables#tryComputeInterrupt}
     */
    static Supplier<Boolean> tryComputeStopper;

    /**
     * Il booleano che comunica se si sta giocando tramite GUI
     */
    public static volatile boolean isGUI = false;

    /**
     * Il booleano che mantiene sincronizzati GUI e {@link gapp.ulg.game.util.PlayGUI}
     */
    public static volatile boolean canContinuePlaying = true;

    /**
     * Il booleano che indica se un {@link gapp.ulg.game.util.PlayerGUI} ha effettuato una mossa di tipo {@link gapp.ulg.game.board.Move.Kind#RESIGN}
     */
    public static volatile boolean playerGUImanualRESIGN;

    /**
     * L'oggetto contenente la board del gioco, in cui vengono mossi pezzi ed eseguite mosse
     */
    public static BoardGame game;

    /**
     * Il menu dove scegliere il gioco e le impostazioni desiderate per il gioco selezionato
     * @see ScreenChooser
     */
    static ScreenChooser gameSettings;

}
