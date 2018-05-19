package gapp.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.games.GameFactories;
import gapp.ulg.play.PlayerFactories;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static gapp.gui.Variables.*;

/**
 * Un {@code ScreenChooser} è un oggetto in grado di rappresentare un menu grafico
 * per un qualsiasi gioco del framework che si vuole giocare tramite GUI.
 * Il gioco per essere visualizzato correttamente deve essere registrato in {@link PlayerFactories}
 * tramite {@link PlayerFactories#registerBoardFactory(Class)}
 */
public class ScreenChooser extends Parent {

    /**
     * Rappresenta il nodo riepilogativo delle impostazioni del gioco scelte dall' utente
     */
    private Parent gameInfo;

    /**
     * {@link HBox} contenitore dei parametri della {@link gapp.ulg.game.GameFactory} selezionata
     */
    private HBox paramLayout = new HBox();

    /**
     * La lista contenente il layout corrente, al cui interno sono contenuti i parametri del giocatore selezionato,
     * la sua grandezza non è mai > 1
     * @see ScreenChooser#playerParamChooser(Mapper)
     */
    private List<HBox> playerParamLayout = new ArrayList<>();

    /**
     * Il nodo principale della classe
     */
    private VBox mainLayout = new VBox();

    /**
     * Il nodo contenitore di tutti i layout dei giocatori selezionabili da parte dell utente
     */
    private VBox playerLayout = new VBox();

    /**
     * Rappresenta il nodo della selezione dei parametri di una {@link gapp.ulg.game.GameFactory} da parte dell' utente
     */
    private Parent factoryParam;

    /**
     * Rappresenta il nodo della selezione dei giocatori di una {@link gapp.ulg.game.PlayerFactory} da parte dell' utente
     */
    private Parent playerChooser;

    /**
     * Rappresenta il nodo che permette all' utente di scegliere i parametri generali della partita, come per esempio
     * i limiti computazionali che un player programma deve rispettare o piu in generale il tempo di attesa tra una mossa
     * ed un alta ecc.
     */
    private Parent genericParamChooser;

    /**
     * {@link Button} che permette all' utente di confermare le impostazioni scelte per il match che verrà giocato
     */
    private Button confirmButton;

    /**
     * Una lista contenente oggetti utili a mantenere delle corrispondenze per le informazioni di un
     * player artificiale, come indice di turnazione, nome, {@link PlayerFactory}, i suo parametri ed un eventuale stringa
     * rappresentante il path nel caso in cui la playerfactory interessata ne abbia necessità.
     * @see Mapper
     */
    private List<Mapper<Integer,String,PlayerFactory,List<DoubleMapper<String,Object>>,String>> pNamesIndex = new ArrayList<>();

    /**
     * Una lista contenente oggetti utili a mantenere delle corrispondenze per le informazioni
     * di un giocatore reale, come per esempio indice di turnazione e nome.
     * @see DoubleMapper
     */
    private List<DoubleMapper<Integer,String>> realPlayer = new ArrayList<>();

    /**
     * Una lista contente oggetti utili a mantenere una corrispondenza tra il giocatore di indice
     * I ed una lista contenente i layout dei parametri della {@link PlayerFactory} selezionata per il giocatore di quell' indice.
     * @see DoubleMapper
     */
    private List<DoubleMapper<Integer,List<HBox>>> mapList = new ArrayList<>();

    /**
     * Una lista contenente oggetti utili a mantenere una corrispondenza tra il giocatore di indice I e il layout
     * che lo contiene, piu in generale che contiene tutto ciò che riguarda quel giocatore.
     */
    private List<DoubleMapper<Integer,HBox>> list = new ArrayList<>();

    /**
     * Una mappa che indica se il numero minimo di giocatori per giocare al gioco selezionato è stato raggiunto
     */
    private Map<Integer,Boolean> checkerConfirm = new HashMap<>();

    /**
     * I nomi dei parametri generali della partita
     */
    private enum genericParams{
        TOL, TIMEOUT, MINTIME, MAXTH, FJPSIZE, BGEXECSIZE
    }

    /**
     * Una lista contenente oggetti utili a mantenere una corrispondenza tra
     * un parametro generale della partita ed il suo valore.
     */
    private List<DoubleMapper<Enum,String>> listMatchParam = new ArrayList<>();

    {
        listMatchParam.add(new DoubleMapper<>(genericParams.TOL, "0"));
        listMatchParam.add(new DoubleMapper<>(genericParams.TIMEOUT, "0"));
        listMatchParam.add(new DoubleMapper<>(genericParams.MINTIME, "0"));     // valori di default
        listMatchParam.add(new DoubleMapper<>(genericParams.MAXTH, "-1"));
        listMatchParam.add(new DoubleMapper<>(genericParams.FJPSIZE, "-1"));
        listMatchParam.add(new DoubleMapper<>(genericParams.BGEXECSIZE, "-1"));
    }

    /**
     * Crea un oggetto {@link ScreenChooser} per visualizzare tutti requisiti che si devono impostare
     * per giocare ad un gioco che si gioca tramite GUI.
     */
    public ScreenChooser() {
        Parent factoryChooser = factoryChooser();
        factoryParam = factoryParamChooser();
        playerChooser = playerChooser();
        genericParamChooser = genericMatchParamChooser();

        mainLayout.getChildren().addAll(factoryChooser,factoryParam,playerChooser,genericParamChooser);
        mainLayout.setSpacing(40);
    }

    /**
     * Ritorna un {@link Parent} al cui interno sono contenuti due oggetti, un {@link Text}
     * rappresentante la descrizione dell' oggetto che verrà ritornato ed un {@link HBox}
     * contenente la scelta dei giochi disponibili.
     * @see PlayerFactories#availableBoardFactories()
     * @return il layout che contiene il Text della descrizione e l' Hbox della scelta della GameFactory
     */
    private Parent factoryChooser(){
        HBox box = new HBox();
        VBox vert = new VBox();

        ComboBox<String> gameFactoryContainer = new ComboBox<>();

        gameFactoryContainer.setPrefWidth(100);

        gameFactoryContainer.getItems().addAll(GameFactories.availableBoardFactories());

        gameFactoryContainer.setValue(GameFactories.availableBoardFactories()[0]);

        match.setGameFactory(GameFactories.availableBoardFactories()[0]); // setto una factory di defaul nel PlayGUI

        gameFactoryContainer.setOnAction(event ->{  // quando viene selezionato un gioco nel combobox ...

            match.setGameFactory(gameFactoryContainer.getValue()); // ... lo setto nel PlayGUI ..

            Reset(); // .. e resetto tutte le impostazioni che l' utente puo avere ipoteticamente selezionato per un gioco precedente
        });

        box.getChildren().addAll(gameFactoryContainer);

        Text gameSelection = new Text("Game selection : ");
        gameSelection.setFont(Font.font(MAIN_FONT.getName(), FontWeight.BOLD, FONT_SIZE));
        gameSelection.setFill(SCREEN_TITLE_DEFAULT_COLOR);

        vert.getChildren().addAll(gameSelection, box);
        vert.setPadding(new Insets(10,0,0,10));
        vert.setSpacing(FACTORY_CHOOSER_LAYOUT_SPACING);

        return vert;
    }

    /**
     * Ritorna un {@link Parent} al cui interno sono contenuti due oggetti, un {@link Text}
     * rappresentante la descrizione dell' oggetto che verrà ritornato ed un {@link HBox}
     * contenente la scelta di tutti i parametri della {@link gapp.ulg.game.GameFactory}
     * precedentemente selezionata.
     * @return il layout che contiene il Text della descrizione e l' Hbox della scelta dei parametri della GameFactory
     */
    private Parent factoryParamChooser(){

        if(!paramLayout.getChildren().isEmpty()) {             // nel caso il layout contenente i parametri della factory non sia vuoto ...
            List<Node> n = new ArrayList<>();                 // all' invocazione del metodo lo svuoto per evitare che ci siano duplicati
            paramLayout.getChildren().forEach(e -> n.add(e));
            paramLayout.getChildren().removeAll(n);
        }

        VBox vert = new VBox();

        List<DoubleMapper<Text,ComboBox<Object>>> map = new ArrayList<>();

        for (Object p : match.getGameFactory().params()) {
            ComboBox<Object> box = new ComboBox<>();
            Param x = (Param) p;

            for(Object o : x.values()) {
                box.getItems().add(o);
            }

            box.setValue(x.get()); // setto il valore del combox con il valore di default del parametro

            Text param = new Text(x.name() + " :");
            param.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));

            param.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

            map.add(new DoubleMapper<>(param, box)); // associo al nome del parametro il combobox con i relativi valori

            match.setGameFactoryParamValue(x.name(), box.getValue()); // setto nel PlayGUI i valori di default della factory

            box.setOnAction(event -> { // nel caso venga selezionato il valore di un parametro nel combobox ..

                match.setGameFactoryParamValue(x.name(), box.getValue()); // .. setto quel valore nel PlayGUI

                pNamesIndex.clear();  // inizializzo tutte le impostazioni riguardanti i player ...
                realPlayer.clear();  //  che possono essere state scelte dall' utente

                playerParamLayout.clear();
                mapList.clear();

                list.clear();

                confirmButton.setDisable(true);
                checkerConfirm.clear();


                mainLayout.getChildren().remove(factoryParam);

                if(!playerLayout.getChildren().isEmpty()) {
                    List<Node> n = new ArrayList<>();
                    playerLayout.getChildren().forEach(e -> n.add(e));
                    playerLayout.getChildren().removeAll(n);
                }

                mainLayout.getChildren().remove(playerChooser);

                factoryParam = factoryParamChooser(); // ricreo i layout aggiornati (es. per giochi come MNK i valori di un parametro sono dipendenti da un altro quindi è necessario
                playerChooser = playerChooser();      // riaggiornare il layout con i nuovi valori dei parametri)

                mainLayout.getChildren().add(1,factoryParam); // e li setto nuovamente nelle loro rispettive posizioni
                mainLayout.getChildren().add(2,playerChooser);


            });

        }

        for (DoubleMapper<Text,ComboBox<Object>> dd : map)
            paramLayout.getChildren().addAll(dd.getFirst(), dd.getSecond()); // aggiungo i valori precedentemente associati nel layout che li conterrà

        paramLayout.setSpacing(FACTORY_CHOOSER_LAYOUT_SPACING * 2);

        Text gameParamsSelection = new Text("Game params selection : ");
        gameParamsSelection.setFont(Font.font(MAIN_FONT.getName(), FontWeight.BOLD, FONT_SIZE));
        gameParamsSelection.setFill(SCREEN_TITLE_DEFAULT_COLOR);

        vert.getChildren().addAll(gameParamsSelection, paramLayout);
        vert.setPadding(new Insets(0,0,0,10));
        vert.setSpacing(FACTORY_CHOOSER_LAYOUT_SPACING);

        return vert;
    }

    /**
     * Ritorna un {@link Parent} al cui interno sono contenuti due oggetti, un {@link Text}
     * rappresentante la descrizione dell' oggetto che verrà ritornato ed un {@link VBox}
     * contenente il massimo numero di giocatori selezionabili da parte dell' utente per
     * la {@link gapp.ulg.game.GameFactory} selezionata
     * @return il layout che contiene il Text della descrizione ed il Vbox della scelta dei player
     */
    private Parent playerChooser() {

        Text playerSelection = new Text("Players selection : (min " + match.getGameFactory().minPlayers() + ", max " + match.getGameFactory().maxPlayers() + " players)");
        playerSelection.setFont(Font.font(MAIN_FONT.getName(), FontWeight.BOLD, FONT_SIZE));
        playerSelection.setFill(SCREEN_TITLE_DEFAULT_COLOR);

        playerLayout.getChildren().addAll(playerSelection);

        for (int i = 1; i <= match.getGameFactory().maxPlayers(); i++) {

            final int c = i;

            mapList.add(new DoubleMapper<>(c, new ArrayList<>())); // per ogni giocatore associo il suo indice ad una lista che in futuro conterrà i layout dei paramtri dei giocatori che si possono alternare
                                                                  //  a quell indice

            CheckBox x = new CheckBox(String.valueOf(i));
            TextField playerName = new TextField();
            ComboBox<String> factoryPLayersContainer = new ComboBox<>();

            factoryPLayersContainer.setPrefWidth(100);

            for (String player : PlayerFactories.availableBoardFactories()){                                                                   // per ogni giocatore del framework ...
                if(PlayerFactories.getBoardFactory(player).canPlay(match.getGameFactory()).equals(PlayerFactory.Play.YES)                     // ... se il giocatore sa giocare o puo imparare ...
                        || PlayerFactories.getBoardFactory(player).canPlay(match.getGameFactory()).equals(PlayerFactory.Play.TRY_COMPUTE)) { // viene inserito nei giocatori selezionabili dall' utente
                    factoryPLayersContainer.getItems().add(player);
                }
            }


            factoryPLayersContainer.getItems().addAll("PlayerGUI"); // aggiungo la voce che fa riferimeto ad un player umano

            factoryPLayersContainer.setValue(null);

            HBox horizontal = new HBox();
            horizontal.setSpacing(30);

            x.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));

            Text name = new Text("Name : ");
            name.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));
            name.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

            Text kind = new Text("Kind : ");
            kind.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));
            kind.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

            horizontal.getChildren().addAll(x,name,playerName,kind,factoryPLayersContainer); // popolo il layout con le informazioni necessare per selezionare un player ...

            DoubleMapper<Integer, HBox> toAd = new DoubleMapper<>(c, horizontal); // .. ed associo al giocatore di indice x il layout che contiene le sue informazioni ..

            if(!list.contains(toAd)) // .. e se questo non è già contenuto nella lista che contiene queste associazioni lo aggiungo
                list.add(toAd);

                playerLayout.getChildren().addAll(horizontal); // aggiungo il layout del giocatore al layout che li contiene

                playerName.setDisable(true);
                factoryPLayersContainer.setDisable(true);

                x.setOnAction(event -> {

                    checkerConfirm.put(c, false);

                    playerName.setDisable(false);

                    factoryPLayersContainer.setDisable(false);

                    if (!x.isSelected()) { // se deseleziono il checkbox che mi indica se un giocatore è stato scelto ...

                        playerName.setDisable(true);
                        factoryPLayersContainer.setDisable(true);
                        playerName.clear();

                        factoryPLayersContainer.setValue(null);

                        z : for (DoubleMapper<Integer, HBox> mapper1 : list) {  // trovo il layout del giocatore corrente ..
                            if (mapper1.getFirst().equals(c)) {
                                for (DoubleMapper<Integer,List<HBox>> h : mapList) {
                                    if (h.getFirst().equals(c) && !h.getSecond().isEmpty()) { // .. trovo il layout dei parametri di quel giocatore ..
                                        for (HBox h2 : h.getSecond()) {
                                            for (Node node2 : mapper1.getSecond().getChildren()) {
                                                if (node2 instanceof HBox && node2.equals(h2)) {
                                                    mapper1.getSecond().getChildren().remove(h2); // ..e lo elimino
                                                    break z;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        if(!mapList.isEmpty()) {
                            DoubleMapper<Integer, List<HBox>> toClear = null;      // trovo il layout dei parametri del giocatore corrente nella lista che mantiene per ogni giocatore la lista ...
                            for (DoubleMapper<Integer, List<HBox>> r : mapList) {
                                if (r.getFirst().equals(c))
                                    toClear = r;
                            }                                                     // .. dei layout dei paramentri dei giocatori che si sono alternati a quell' indice ...

                            if(toClear != null) {
                                for (DoubleMapper<Integer, List<HBox>> r : mapList)
                                    if(r.equals(toClear))
                                        r.getSecond().clear();                   // .. e la svuoto
                            }
                        }

                        if(!list.isEmpty()){                              // trovo il layout contente le informazioni del giocatore corrente nella lista che mantiene tale associazione ...
                            DoubleMapper<Integer,HBox> toR = null;
                            for(DoubleMapper<Integer,HBox> k : list)
                                if(k.getFirst().equals(c))
                                    toR = k;

                            if (toR != null)
                                mapList.remove(toR);                     // .. e lo elimino
                        }

                        if (!pNamesIndex.isEmpty()) {

                            Mapper toDelete = null;                      // trovo se esiste un giocatore gia impostato per quell indice ..
                            for (Mapper m : pNamesIndex) {
                                if (m.getFirst().equals(c))
                                    toDelete = m;
                            }

                            if (toDelete != null)
                                pNamesIndex.remove(toDelete);           // .. e lo elimino
                        }


                    } else {  // altrimenti se il checkbox viene selezionato ...

                        boolean find = false;

                        for (Mapper map : pNamesIndex) {
                            if (map.getFirst().equals(c))  // se esiste già un giocatore a quell' indice non succede nulla ...
                                find = true;
                        }
                        if (!find) { // altrimenti creo un istanza che manterrà tutte le informazioni del nuovo giocatore e lo aggiungerò alla lista dei giocatori

                            Mapper<Integer,String,PlayerFactory,List<DoubleMapper<String,Object>>,String> m = new Mapper(c, null, null,new ArrayList<>(),null);

                            if (!pNamesIndex.contains(m))
                                pNamesIndex.add(m);

                        }

                    }

                });

                factoryPLayersContainer.setOnAction(event -> {  // se viene selezionato un giocatore nel combo box ...

                    if (factoryPLayersContainer.getValue() != null && factoryPLayersContainer.getValue().equals("PlayerGUI")) { // se è un player umano ...
                        checkerConfirm.put(c, true);

                        if(!list.isEmpty()) {
                            DoubleMapper<Integer,List<HBox>> deleted = null;
                            loopz:
                            for (Node node : playerLayout.getChildren()) {
                                if (node instanceof HBox) {
                                    for (DoubleMapper<Integer, HBox> mapper1 : list) {     // cerco il layout delle informazioni del player corrente
                                        if (mapper1.getFirst().equals(c)) {
                                            for (DoubleMapper<Integer, List<HBox>> h : mapList) {           // cerco se a quell' indice il layout in questione contenga un layout riguardante i parametri ..
                                                if (h.getFirst().equals(c) && !h.getSecond().isEmpty()) {  // .. di un player scelto precedemente per quell' indice ...
                                                    deleted = h;
                                                    for (HBox h2 : h.getSecond()) {
                                                        for (Node node2 : mapper1.getSecond().getChildren()) {
                                                            if (node2 instanceof HBox && node2.equals(h2)) {     // .. e se lo trovo, lo elimino ...
                                                                mapper1.getSecond().getChildren().remove(h2);
                                                                break loopz;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (deleted != null)
                                for (DoubleMapper<Integer, List<HBox>> h : mapList) // .. e svuoto la lista dei layout dei parametri per il giocatore di quell' indice
                                    if (deleted.equals(h))
                                        h.getSecond().clear();
                        }


                        Mapper toDelete = null;

                        for (Mapper m : pNamesIndex) {                               // se esiste un giocatore programma per quell' indice ...
                            if (m.getFirst() != null && m.getFirst().equals(c)) {
                                toDelete = m;
                            }
                        }

                        if (toDelete != null)
                            pNamesIndex.remove(toDelete);  // ... viene eliminato ...

                        boolean isPossible = false;

                        if (realPlayer.isEmpty()) {
                            isPossible = true;
                        } else {

                            for (DoubleMapper<Integer,String> mm : realPlayer) {
                                if (!mm.getFirst().equals(c))
                                        isPossible = true;
                            }
                        }


                        if (isPossible) {
                            DoubleMapper<Integer,String> toAdd2 = new DoubleMapper<>(c,null);  // ... e aggiunto alla lista dei giocatori reali, un giocatore reale all' indice x

                            if (!realPlayer.contains(toAdd2))
                                realPlayer.add(toAdd2);
                        }
                    } else if (factoryPLayersContainer.getValue() != null) { // altrimenti se è un giocatore programma ad essere selezionato nel combo box ...

                        checkerConfirm.put(c, true);

                        if (!realPlayer.isEmpty()) {    // se esiste già un giocatore umano per quell indice ...

                            DoubleMapper<Integer, String> delete = null;

                            for (DoubleMapper<Integer, String> integer : realPlayer) {
                                if (integer.getFirst().equals(c))
                                        delete = integer;
                            }

                            if (delete != null) {                                                   // ... viene eliminato e creata un nuovo giocatore nella lista dei giocaatori programmi all' indice x
                                realPlayer.remove(delete);
                                Mapper addi = new Mapper<>(c, null, null,new ArrayList<>(),null);
                                if (!pNamesIndex.contains(addi))
                                    pNamesIndex.add(addi);
                            }

                        }


                        for (Mapper<Integer,String,PlayerFactory,List<DoubleMapper<String,Object>>,String> m : pNamesIndex) {
                            if (m.getFirst().equals(c)) {
                                if (factoryPLayersContainer.getValue() != null) {
                                    m.setThird(PlayerFactories.getBoardFactory(factoryPLayersContainer.getValue()));  // trovato il nuovo giocatore all' indice x gli associo la PlayerFactory selezionata
                                    playerParamChooser(m);
                                }
                            }
                        }
                    }

                    final int[] counter = {0};
                    checkerConfirm.forEach((f,s) -> {  // se è stato raggiunto il numero minimo di giocatori per disputare il match ...
                        if (s)
                            counter[0]++;
                    });

                    confirmButton.setDisable(counter[0] < match.getGameFactory().minPlayers()); // ... viene abilitato il pulsante per confermare le impostazioni scelte fino a quel momento
                });


            }

        playerLayout.setPadding(new Insets(0,0,0,10));

        playerLayout.setSpacing(PLAYER_CHOOSER_LAYOUT_SPACING);

        return playerLayout;
    }

    /** 
     * Data le informazioni del giocatore selezionato in {@code mapper} visualizza i 
     * parametri della {@link PlayerFactory} scelta per il giocatore stesso.  *
     * @param mapper la corrispondenza del giocatore che è stato selezionato dall' utente 
     */
    private void playerParamChooser(Mapper<Integer,String,PlayerFactory,List<DoubleMapper<String,Object>>,String> mapper){

        List<DoubleMapper<Text,ComboBox>> doubleMapper = new ArrayList<>();

        Button path = new Button("...");  // il bottone per la selezione del path per un giocatore che ne ha bisogno

        path.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File selectedFile = chooser.showDialog(stage);

            if(selectedFile != null){
                String paths = selectedFile.getAbsolutePath();
                for(Mapper<Integer,String,PlayerFactory,List<DoubleMapper<String,Object>>,String> fourMapper : pNamesIndex) {
                    if (fourMapper.getFirst().equals(mapper.getFirst())) {
                        fourMapper.setFift(paths);
                    }
                }
            }
        });

        Text pathname = new Text("Select Path : ");
        pathname.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));
        pathname.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);


        mapper.setFour(new ArrayList<>()); // resetto i parametri del player passato in input (se per esempip fosse la seconda invocazione del metodo per lo stesso giocatore esso conterrebbe anche
                                          // i parametri di quello precedente

        mapper.setFift(null);   // per lo stesso esempio fatto precedentemente resetto il path del giocatore corrente


        if(!mapper.getThird().params().isEmpty()) { // se la playerfactory impostata per il giocatore corrente contiene dei parametri

            for (Object o : mapper.getThird().params()) {
                ComboBox<Object> comboBox = new ComboBox<>();
                Param p = (Param) o;

                for (Object x : p.values())
                    comboBox.getItems().add(x); // ... i loro valori ammissibili vengono aggiunti al combobox

                comboBox.setValue(p.get());

                for (Mapper<Integer, String, PlayerFactory, List<DoubleMapper<String, Object>>, String> fourMapper : pNamesIndex) {
                    if (fourMapper.getFirst().equals(mapper.getFirst())) {
                        DoubleMapper<String, Object> ad = new DoubleMapper<>(p.name(), p.get());  // ed aggiunti nelle informazioni riguardanti il player corrente con il loro valore di default
                        fourMapper.getFour().add(ad);
                    }
                }

                Text param = new Text(p.name() + " :");
                param.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));
                param.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

                doubleMapper.add(new DoubleMapper<>(param, comboBox));

                comboBox.setOnAction(event -> { // se viene selezionato il valore di un parametro del player corrente ...

                    for (Mapper<Integer, String, PlayerFactory, List<DoubleMapper<String, Object>>, String> fourMapper : pNamesIndex) {
                        if (fourMapper.getFirst().equals(mapper.getFirst()) && !fourMapper.getFour().isEmpty()) {
                            DoubleMapper<String, Object> foundBox = null;

                            for (DoubleMapper<String, Object> j : fourMapper.getFour()) {  // ... tra tutti i player si trova quello corrente con il parametro selezionato
                                if (j.getFirst().equals(p.name()))
                                    foundBox = j;

                            }

                            if (foundBox != null)
                                foundBox.setSecond(comboBox.getValue()); // ... e viene aggiornato il suo valore
                        }
                    }
                });
            }

            HBox horizontal = new HBox();
            horizontal.setSpacing(PLAYER_CHOOSER_LAYOUT_SPACING);


            if (playerParamLayout.isEmpty()) {
                playerParamLayout.add(horizontal);
            } else {                               // viene aggiunto il layout che conterrà le informazioni dei parametri, se ne era gia presente uno si elimina cosi da avere sempre quello del player corrente
                playerParamLayout.clear();
                playerParamLayout.add(horizontal);
            }


            for (DoubleMapper<Text, ComboBox> box : doubleMapper) {
                playerParamLayout.get(0).getChildren().addAll(box.getFirst(), box.getSecond()); // vengo aggiunte le informazioni al layout ...
            }

            if (mapper.getThird().canPlay(match.getGameFactory()).equals(PlayerFactory.Play.TRY_COMPUTE)) // ... e se la factory del giocatore corrente deve imparare a giocare anche la selezione del path
                playerParamLayout.get(0).getChildren().addAll(pathname, path);
        }

        else{                                 // se invece la playerfactory del giocatore corrente non contiene parametri ...
            playerParamLayout.clear();
            playerParamLayout.add(new HBox());   // se ne era gia presente uno si elimina

            if (mapper.getThird().canPlay(match.getGameFactory()).equals(PlayerFactory.Play.TRY_COMPUTE)) // si verifica che playerfactory debba imparare a giocare ed in caso di esito positivo si aggiunge
                playerParamLayout.get(0).getChildren().addAll(pathname, path);                           // la selezione del path per tale giocatore
        }

        Loop:
        for (Node node : playerLayout.getChildren()) {
            if (node instanceof HBox) {
                for (DoubleMapper<Integer,HBox> mapper1 : list) {                  // trovo il layout del giocatore corrente passato in input (list)
                    if (mapper1.getFirst().equals(mapper.getFirst())) {
                        for (Node node1 : mapper1.getSecond().getChildren()) {
                            if (node1 instanceof ComboBox && ((ComboBox) node1).getValue().equals(mapper.getThird().name())) {
                                for (DoubleMapper<Integer,List<HBox>> h : mapList) {                                              // trovo la lista dei layout dei parametri del giocatore corrente ... (maplist)
                                    if (h.getFirst().equals(mapper.getFirst()) && !h.getSecond().isEmpty()) {
                                        for (HBox h2 : h.getSecond()) {
                                            for (Node node2 : mapper1.getSecond().getChildren()) {
                                                if (node2 instanceof HBox && node2.equals(h2)) {
                                                                                                      // ... se il layout contenente le informazioni del giocatore ne contiene uno già presente
                                                                                                     // nella sua lista dei layout dei parametri lo elimina dal layout delle informazioni del giocatore ...
                                                    if(!playerParamLayout.isEmpty())
                                                        h.getSecond().add(playerParamLayout.get(0)); // ... aggiunge quello nuovo nella lista dei layout dei parametri per il giocatore di quell' indice ... (maplist)

                                                    mapper1.getSecond().getChildren().remove(h2);

                                                    mapper1.getSecond().getChildren().addAll(playerParamLayout.get(0)); // ... e lo aggiunge anche nel layout contenente le informazioni dei player (list)
                                                    break Loop;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!playerParamLayout.isEmpty()) {                                     // se invece nessuno dei layout dei parametri è gia presente nel layout dell info del giocatore corrente ...
                                    for (DoubleMapper<Integer, List<HBox>> doubleMapper1 : mapList) {
                                        if (doubleMapper1.getFirst().equals(mapper.getFirst())) {
                                            doubleMapper1.getSecond().add(playerParamLayout.get(0));              // aggiunge quello nuovo nella lista dei layout dei parametri del giocatore corrente ... (maplist)
                                            mapper1.getSecond().getChildren().addAll(playerParamLayout.get(0));  //  ... e lo aggiunge nel layout delle informazioni del giocatore corrente (list)
                                            break Loop;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Ritorna un {@link Parent} al cui interno sono contenuti due oggetti, un {@link Text}
     * rappresentante la descrizione dell' oggetto che verrà ritornato ed un {@link VBox}
     * contenente i parametri generali della partita e la possibilità di selezionarne i valori.
     * @return il layout che contiene il Text della descrizione ed il Vbox della scelta dei parametri generali della partita
     */
    private Parent genericMatchParamChooser() {

        VBox vertical = new VBox();

        List<DoubleMapper<CheckBox,TextField>> checkBoxTextFieldList = new ArrayList<>();
        List<Text> texts = new ArrayList<>();

        for (int i = 0; i <= genericParams.values().length - 1 ; i++) {
            final int z = i;
            CheckBox checkBox = new CheckBox();
            TextField textField = new TextField();

            if (genericParams.values()[z].equals(genericParams.MAXTH) || genericParams.values()[z].equals(genericParams.FJPSIZE) || genericParams.values()[z].equals(genericParams.BGEXECSIZE))
                textField.setText("-1");
            else
                textField.setText("0");

            checkBoxTextFieldList.add(new DoubleMapper<>(checkBox,textField));
            checkBox.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));

            Text text = new Text(genericParams.values()[z].toString().toLowerCase());
            text.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));
            text.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

            texts.add(text);

            textField.setDisable(true);

            checkBox.setOnAction(event -> {

                if (!checkBox.isSelected()) {
                    textField.setDisable(true);
                    if (genericParams.values()[z].equals(genericParams.MAXTH) || genericParams.values()[z].equals(genericParams.FJPSIZE) || genericParams.values()[z].equals(genericParams.BGEXECSIZE))
                        textField.setText("-1");
                    else
                        textField.setText("0");

                    if (!listMatchParam.isEmpty()) {
                        for (DoubleMapper<Enum, String> gP : listMatchParam) {
                            if(gP.getFirst().equals(genericParams.values()[z])) {
                                if (gP.getFirst().equals(genericParams.MAXTH) || gP.getFirst().equals(genericParams.FJPSIZE) || gP.getFirst().equals(genericParams.BGEXECSIZE))
                                    gP.setSecond("-1");
                                else
                                    gP.setSecond("0");
                            }
                        }
                    }
                }

                else
                    textField.setDisable(false);

            });

        }

        Text generalParams = new Text("General match params :");
        generalParams.setFont(Font.font(MAIN_FONT.getName(), FontWeight.BOLD, FONT_SIZE));
        generalParams.setFill(SCREEN_TITLE_DEFAULT_COLOR);
        vertical.getChildren().add(generalParams);

        int t = 0;
        HBox horizontal = new HBox();
        for(DoubleMapper doubleMapper : checkBoxTextFieldList) {
            t++;
            horizontal.setSpacing(PARAM_CHOOSER_LAYOUT_SPACING);
            horizontal.getChildren().addAll((Parent) doubleMapper.getFirst(), texts.get(t-1),(Parent) doubleMapper.getSecond());

            if(t % 3 == 0) {
                vertical.getChildren().addAll(horizontal);
                horizontal = new HBox();
            }

        }

        vertical.setSpacing(FONT_SIZE);

        confirmButton = new Button("Confirm");
        confirmButton.setFont(Font.font(MAIN_FONT.getName(), FontWeight.BOLD, FONT_SIZE));
        confirmButton.setDisable(true);

        Button backToMainMenu = new Button("Back");
        backToMainMenu.setFont(Font.font(MAIN_FONT.getName(), FontWeight.BOLD, FONT_SIZE));

        HBox confirm = new HBox(confirmButton, backToMainMenu);
        confirm.setSpacing(20);
        confirm.setPadding(new Insets(25,0,0,440));

        vertical.getChildren().addAll(confirm);

        confirmButton.setOnAction(event -> {

            for (int i = 1; i <= match.getGameFactory().maxPlayers(); i++) {

                for (Mapper<Integer,String,PlayerFactory,List<DoubleMapper<String,Object>>,String> m : pNamesIndex)
                    if (m.getFirst() != null && m.getFirst().equals(i)) {
                        int finalI = i;

                        list.forEach(hor -> {

                            if (hor.getFirst().equals(finalI))
                                m.setSecond(((TextField) hor.getSecond().getChildren().get(2)).getText());

                        });

                    }

                for (DoubleMapper<Integer,String> r : realPlayer)
                    if(r.getFirst().equals(i)) {
                        int finalI = i;

                        list.forEach(hor -> {

                            if (hor.getFirst().equals(finalI))
                                r.setSecond(((TextField) hor.getSecond().getChildren().get(2)).getText());

                        });

                    }

            }

            for (int i = 0; i < 6; i++) {

                for(DoubleMapper<Enum,String> mapper : listMatchParam) {
                    if(mapper.getFirst() != null && mapper.getFirst().equals(genericParams.values()[i])) {

                        switch (genericParams.values()[i]){
                            case TOL:
                                mapper.setSecond(((TextField) ((HBox) ((VBox) mainLayout.getChildren().get(3)).getChildren().get(1 + (i / 3))).getChildren().get(3 * (i % 3) + 2)).getText());
                                break;
                            case MINTIME:
                                mapper.setSecond(((TextField) ((HBox) ((VBox) mainLayout.getChildren().get(3)).getChildren().get(1 + (i / 3))).getChildren().get(3 * (i % 3) + 2)).getText());
                                break;
                            case TIMEOUT:
                                mapper.setSecond(((TextField) ((HBox) ((VBox) mainLayout.getChildren().get(3)).getChildren().get(1 + (i / 3))).getChildren().get(3 * (i % 3) + 2)).getText());
                                break;
                            case MAXTH:
                                mapper.setSecond(((TextField) ((HBox) ((VBox) mainLayout.getChildren().get(3)).getChildren().get(1 + (i / 3))).getChildren().get(3 * (i % 3) + 2)).getText());
                                break;
                            case FJPSIZE:
                                mapper.setSecond(((TextField) ((HBox) ((VBox) mainLayout.getChildren().get(3)).getChildren().get(1 + (i / 3))).getChildren().get(3 * (i % 3) + 2)).getText());
                                break;
                            case BGEXECSIZE:
                                mapper.setSecond(((TextField) ((HBox) ((VBox) mainLayout.getChildren().get(3)).getChildren().get(1 + (i / 3))).getChildren().get(3 * (i % 3) + 2)).getText());
                                break;
                        }

                    }

                }


            }

            final String[][] tryComputePlayers = {new String[0]};
            final boolean[] haveToCompute = {false};

            pNamesIndex.forEach(e -> {
                if (e.getThird().canPlay(match.getGameFactory()).equals(PlayerFactory.Play.TRY_COMPUTE)) {
                    tryComputePlayers[0] = Arrays.copyOf(tryComputePlayers[0], tryComputePlayers[0].length + 1);
                    tryComputePlayers[0][tryComputePlayers[0].length - 1] = e.getSecond();

                    haveToCompute[0] = true;
                }
            });

            if (tryComputePlayers[0].length != 0)
                warningBuilder(tryComputePlayers[0]);

            if (!haveToCompute[0]) {
                gameInfo = matchParamSettedInfo();

                menu.getRoot().getChildren().remove(mainLayout);
                menu.getRoot().getChildren().add(gameInfo);
            }

        });


        backToMainMenu.setOnAction(event -> {

            menu.getRoot().setBackground(new Background(new BackgroundImage(new Image(getClass().getResource("../../resources/First_Screen_View.jpg").toString()),
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                    BackgroundSize.DEFAULT)));

            menu.getRoot().getChildren().remove(mainLayout);
            menu.getRoot().getChildren().add(menu.getGameMenu());
            menu.getRoot().getChildren().add(textIntro);
        });

        vertical.setPadding(new Insets(0,0,0,10));
        return vertical;
    }

    /**
     * Ritorna un {@link Parent} che riepiloga tutte le impostazioni scelte dall' utente
     * dandogli la possibiltà di tornare indietro e modificare le impostazioni da lui scelte
     * o se confermare le impostazioni selezionate e quindi iniziare a giocare la partita.
     * @return il layout contenente tutte le informazioni del match con le impostazioni scelte dall' utente.
     */
    private Parent matchParamSettedInfo() {
        VBox vertical = new VBox();

        vertical.setSpacing(35);

        HBox gameNameBox = new HBox();
        Text gameName = new Text("Game : ");

        gameNameBox.setPadding(new Insets(10,0,0,10));

        Text factoryName = new Text(match.getGameFactory().name());
        factoryName.setFont(MAIN_FONT);
        factoryName.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);
        gameName.setFont(MAIN_FONT);
        gameName.setFill(SCREEN_TITLE_DEFAULT_COLOR);
        gameNameBox.getChildren().addAll(gameName, factoryName);
        vertical.getChildren().add(gameNameBox);

        List<VBox> PlayerBox = new ArrayList<>();

        HBox h = new HBox();
        h.setSpacing(15);

        h.setPadding(new Insets(0,0,0,10));

        for (Object p : match.getGameFactory().params()) {
            Param x = (Param) p;
            Text promptParam = new Text(match.getGameFactoryParamPrompt(x.name()));
            promptParam.setFont(MAIN_FONT);
            promptParam.setFill(SCREEN_TITLE_DEFAULT_COLOR);

            Text paramValue = new Text(": " + match.getGameFactoryParamValue(x.name()).toString());
            paramValue.setFont(MAIN_FONT);
            paramValue.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

            h.getChildren().addAll(promptParam,paramValue);
        }

        vertical.getChildren().add(h);

        if(!pNamesIndex.isEmpty()) {
            for (Mapper<Integer, String, PlayerFactory, List<DoubleMapper<String, Object>>,String> mapper : pNamesIndex) {
                VBox players = new VBox();
                players.setSpacing(8);

                if(mapper.getFirst() != null && mapper.getSecond() != null && mapper.getThird() != null) {
                    Text indexPlayer = new Text(mapper.getFirst() == 1 ?  mapper.getFirst() + "st player : " : mapper.getFirst() + "nd player : ");
                    Text playerName = new Text("Name : " + mapper.getSecond().toString() + " ");
                    Text playerKind = new Text("Kind : " + mapper.getThird().name());

                    indexPlayer.setFont(MAIN_FONT);
                    indexPlayer.setFill(SCREEN_TITLE_DEFAULT_COLOR);
                    playerName.setFont(MAIN_FONT);
                    playerName.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);
                    playerKind.setFont(MAIN_FONT);
                    playerKind.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

                    players.getChildren().addAll(indexPlayer, playerName, playerKind);

                    if (!mapper.getFour().isEmpty()) {
                        for (DoubleMapper<String, Object> params : mapper.getFour()) {
                            Text playerParams = new Text(params.getFirst() + " : " + params.getSecond());
                            playerParams.setFont(MAIN_FONT);
                            playerParams.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);
                            players.getChildren().add(playerParams);
                        }

                    }

                }

                PlayerBox.add(players);
            }
        }

        if(!realPlayer.isEmpty()){

            for(DoubleMapper<Integer,String> real : realPlayer) {
                VBox playerGuiBox = new VBox();
                playerGuiBox.setSpacing(8);

                Text realPlayerIndex = new Text(real.getFirst() == 1 ? real.getFirst() + "st player : " : real.getSecond() + "nd player : ");
                Text realPlayerName = new Text("Name : " + real.getSecond());
                Text realPlayerKind = new Text("Kind : PlayerGUI");

                realPlayerIndex.setFont(MAIN_FONT);
                realPlayerIndex.setFill(SCREEN_TITLE_DEFAULT_COLOR);

                realPlayerName.setFont(MAIN_FONT);
                realPlayerName.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

                realPlayerKind.setFont(MAIN_FONT);
                realPlayerKind.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);

                playerGuiBox.getChildren().addAll(realPlayerIndex, realPlayerName, realPlayerKind);

                PlayerBox.add(playerGuiBox);
            }

        }


        HBox horizontal = new HBox();
        horizontal.setSpacing(20);
        horizontal.setPadding(new Insets(0,0,0,10));

        PlayerBox.forEach(v -> horizontal.getChildren().add(v));

        vertical.getChildren().addAll(horizontal);

        HBox generalParams = new HBox();
        generalParams.setPadding(new Insets(0,0,0,10));

        for(DoubleMapper<Enum,String> generalMap : listMatchParam){
            Text gParamsName = new Text(generalMap.getFirst().name() );
            gParamsName.setFont(MAIN_FONT);
            gParamsName.setFill(SCREEN_TITLE_DEFAULT_COLOR);
            Text gParamsValue = new Text(" : " + generalMap.getSecond() + "   ");
            gParamsValue.setFont(MAIN_FONT);
            gParamsValue.setFill(SCREEN_SUBTITLE_DEFAULT_COLOR);
            generalParams.getChildren().addAll(gParamsName,gParamsValue);
        }

        vertical.getChildren().addAll(generalParams);

        Button back = new Button("Back");
        Button start = new Button("Start!");

        back.setFont(MAIN_FONT);
        start.setFont(MAIN_FONT);

        HBox backAndStart = new HBox(back,start);
        backAndStart.setSpacing(20);
        backAndStart.setPadding(new Insets(50,0,0,440));

        back.setOnAction(e -> {
            menu.getRoot().getChildren().remove(gameInfo);
            menu.getRoot().getChildren().add(mainLayout);
        });

        start.setOnAction(e -> {

            stage.setWidth(650);
            stage.setHeight(450);

            menu.getRoot().getChildren().remove(gameInfo);
            menu.getRoot().getChildren().add(game.getNode());

            for (Mapper<Integer, String, PlayerFactory, List<DoubleMapper<String, Object>>,String> mapper : pNamesIndex) {

                if (mapper.getFirst() != null && mapper.getSecond() != null && mapper.getThird() != null)
                    match.setPlayerFactory(mapper.getFirst(), mapper.getThird().name(), mapper.getSecond(), mapper.getFift() != null ? Paths.get(mapper.getFift()) : null);

                if (!mapper.getFour().isEmpty()) {
                    for (DoubleMapper<String, Object> paramValues : mapper.getFour())
                        match.setPlayerFactoryParamValue(mapper.getFirst(), paramValues.getFirst(), paramValues.getSecond());
                }

            }

            if (!realPlayer.isEmpty()) {
                for (DoubleMapper<Integer, String> real : realPlayer) {
                    match.setPlayerGUI(real.getFirst(),real.getSecond(), game.getMaster());
                }
            }

            match.play(Long.parseLong(listMatchParam.get(0).getSecond()), Long.parseLong(listMatchParam.get(1).getSecond()), Long.parseLong(listMatchParam.get(2).getSecond()),
                    Integer.parseInt(listMatchParam.get(3).getSecond()), Integer.parseInt(listMatchParam.get(4).getSecond()), Integer.parseInt(listMatchParam.get(5).getSecond()));

        });


        vertical.getChildren().addAll(backAndStart);

        return vertical;

    }

    /**
     * Resetta tutte le impostazioni scelte fino a quel momento dall' utente se quest' ultimo decide di
     * cambiare il gioco a cui giocare.
     */
    private void Reset() {

        pNamesIndex.clear(); // svuoto la lista contenente i player di un gioco che poteva essere precedentemente scelto
        realPlayer.clear();

        playerParamLayout.clear();
        mapList.clear();

        list.clear();

        checkerConfirm.clear();

        for(DoubleMapper<Enum, String> mapper : listMatchParam){ // resetto i valori dei parametri generali al valore di default
            mapper.setSecond("0");
        }

        if(!playerLayout.getChildren().isEmpty()) {
            List<Node> n = new ArrayList<>();
            playerLayout.getChildren().forEach(e -> n.add(e));
            playerLayout.getChildren().removeAll(n);
        }


        mainLayout.getChildren().remove(factoryParam); // rimuovo i vecchi layaout (per esempio se fosse gia stato scelto un gioco ma poi si cambia idea nella scelta)
        mainLayout.getChildren().remove(playerChooser);
        mainLayout.getChildren().remove(genericParamChooser);

        factoryParam = factoryParamChooser(); // ricreo i nuovi layout
        playerChooser = playerChooser();
        genericParamChooser = genericMatchParamChooser();

        mainLayout.getChildren().add(1,factoryParam); // li aggiungo al layout principale
        mainLayout.getChildren().add(2,playerChooser);
        mainLayout.getChildren().add(3,genericParamChooser);
    }

    /**
     * Avvisa l' utente che un qualche giocatore da lui selezionato necessita di calcolare la strategia
     * per giocare al gioco selezionato
     * @param names i nomi dei giocatori che hanno bisogno di imparare a giocare al gioco selezionato
     */
    private void warningBuilder(String... names) {
        Stage tryComputeStage = new Stage();

        List<Mapper<Integer, String, PlayerFactory, List<DoubleMapper<String, Object>>, String>> alreadyFoundMappers = new ArrayList<>();
        boolean ok = true;
        for (String name : names){
            for (Mapper<Integer, String, PlayerFactory, List<DoubleMapper<String, Object>>, String> mapper : pNamesIndex) {

                if(mapper.getSecond().equals(name) && !alreadyFoundMappers.contains(mapper) && mapper.getFift() == null
                        && mapper.getThird().canPlay(match.getGameFactory()).equals(PlayerFactory.Play.TRY_COMPUTE)) {

                    alreadyFoundMappers.add(mapper);
                    ok = false;
                    break;
                }
            }
        }

        VBox vb1 = new VBox();
        HBox hb1 = new HBox(), hb2 = new HBox();
        Button but1 = new Button("Compute"), but2 = new Button("Back"), but3 = new Button("Stop computing");
        but1.setFont(MAIN_FONT);
        but2.setFont(MAIN_FONT);
        but3.setFont(MAIN_FONT);

        Text tx1;
        if (ok) {
            tx1 = names.length == 1 ? new Text(names[0] + " needs to compute his strategy") : new Text(names[0] + " and " + names[1] + " need to compute their strategies");
            tx1.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FONT_SIZE));
        }

        else {
            but1.setDisable(true);

            tx1 = alreadyFoundMappers.size() == 1 ? new Text(alreadyFoundMappers.get(0).getSecond() + " must select a path") :
                    new Text(alreadyFoundMappers.get(0).getSecond() + " and " + alreadyFoundMappers.get(1).getSecond() + " must select a path");

            tx1.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FONT_SIZE));
        }

        but1.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));
        but2.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));
        but3.setFont(new Font(MAIN_FONT.getName(), FONT_SIZE));

        hb1.getChildren().addAll(but1,but2);
        hb2.getChildren().add(tx1);

        hb1.setAlignment(Pos.CENTER);
        hb2.setAlignment(Pos.CENTER);

        hb1.setSpacing(20);
        hb2.setPadding(new Insets(20,0,0,0));
        vb1.setSpacing(40);

        vb1.getChildren().addAll(hb2,hb1);

        but1.setOnAction(e -> {
            ((HBox) vb1.getChildren().get(1)).getChildren().removeAll(but1,but2);
            ((HBox) vb1.getChildren().get(1)).getChildren().add(but3);

            List<Integer> alreadyComputing = new ArrayList<>();
            final int[] done = {0};

            Class cl = null;
            String path = null;

            boolean samePathAndSameStrategy = false;
            for (int i = 0; i < names.length; i++) {

                for (Mapper<Integer, String, PlayerFactory, List<DoubleMapper<String, Object>>, String> m : pNamesIndex) {

                    if (m.getSecond().equals(names[i]) && !alreadyComputing.contains(m.getFirst())) {

                        if ((cl == null && path == null) || (cl.equals(m.getThird().getClass()) && !path.equals(m.getFift()))) {
                            alreadyComputing.add(m.getFirst());
                            final boolean[] execution = new boolean[1];

                            cl = m.getThird().getClass();
                            path = m.getFift();

                            for (DoubleMapper<String, Object> s : m.getFour()) {

                                if (s.getFirst().equals("Execution")) {
                                    execution[0] = !s.getSecond().equals("Sequential");
                                    break;
                                }

                            }

                            Thread th = new Thread(() -> {
                                m.getThird().setDir(Paths.get(m.getFift()));
                                m.getThird().tryCompute(match.getGameFactory(), execution[0], tryComputeStopper);
                                done[0]++;
                            });

                            th.setDaemon(true);
                            th.start();
                            break;
                        }

                        else if (cl.equals(m.getThird().getClass()) && path.equals(m.getFift()))
                            samePathAndSameStrategy = true;

                    }

                }

            }

            boolean finalSamePathAndSameStrategy = samePathAndSameStrategy;
            Thread hasFinished = new Thread(() -> {

                if (finalSamePathAndSameStrategy)
                    while (done[0] != 1)
                        try {Thread.sleep(50);} catch (InterruptedException ignored) {}

                else
                    while (done[0] != names.length)
                        try {Thread.sleep(50);} catch (InterruptedException ignored) {}

                if (!tryComputeInterrupt) {

                    Platform.runLater(() -> {
                        tryComputeStage.hide();

                        mainLayout.setDisable(false);
                        gameInfo = matchParamSettedInfo();

                        menu.getRoot().getChildren().remove(mainLayout);
                        menu.getRoot().getChildren().add(gameInfo);
                    });

                }

                tryComputeInterrupt = false;
            });

            hasFinished.setDaemon(true);
            hasFinished.start();
        });

        but2.setOnAction(e -> {
            tryComputeStage.hide();
            mainLayout.setDisable(false);
        });

        but3.setOnAction(e -> {
            tryComputeInterrupt = true;
            tryComputeStage.hide();
            mainLayout.setDisable(false);
        });

        tryComputeStage.setScene(new Scene(vb1, 430, 150));
        tryComputeStage.setTitle("Warning");
        tryComputeStage.setResizable(false);
        tryComputeStage.sizeToScene();
        tryComputeStage.show();

        mainLayout.setDisable(true);
    }

    /**
     * @return il nodo principale della classe
     */
    Parent getMainLayout() {
        return mainLayout;
    }

    /**
     * Un {@code DoubleMapper} è un oggetto che permette di associare due oggetti
     * di un qualsiasi tipo
     * @param <A> tipo del primo parametro
     * @param <B> tipo del secondo parametro
     */
    private static class DoubleMapper<A,B> {

        private A first;
        private B second;


        public DoubleMapper(A first, B second) {
            this.first = first;
            this.second = second;
        }


        public B getSecond() {
            return second;
        }

        public void setSecond(B second) {
            this.second = second;
        }

        public A getFirst() {
            return first;
        }

        public void setFirst(A first) {
            this.first = first;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DoubleMapper)) return false;

            DoubleMapper<?, ?> that = (DoubleMapper<?, ?>) o;

            if (first != null ? !first.equals(that.first) : that.first != null) return false;
            return second != null ? second.equals(that.second) : that.second == null;

        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            return result;
        }
    }

    /**
     * Un {@code Mapper} è un oggetto che permette di associare cinque oggetti
     * di un qualsiasi tipo
     * @param <A> il tipo del primo parametro
     * @param <B> il tipo del secondo parametro
     * @param <C> il tipo del terzo parametro
     * @param <D> il tipo del quarto parametro
     * @param <E> il tipo del quinto parametro
     */
    private static class Mapper<A,B,C,D,E> {

        private A first;
        private B second;
        private C third;
        private D four;
        private E fift;

        Mapper(A first, B second, C third,D four, E fift){
            this.first = first;
            this.second = second;
            this.third = third;
            this.four = four;
            this.fift = fift;
        }

        D getFour() {
            return four;
        }

        void setFour(D four) {
            this.four = four;
        }

        B getSecond() {
            return second;
        }

        void setSecond(B second) {
            this.second = second;
        }

        A getFirst() {
            return first;
        }

        public void setFirst(A first) {
            this.first = first;
        }

        C getThird() {
            return third;
        }

        void setThird(C third) {
            this.third = third;
        }

        E getFift() {
            return fift;
        }

        void setFift(E fift) {
            this.fift = fift;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Mapper<?, ?, ?, ?, ?> mapper = (Mapper<?, ?, ?, ?, ?>) o;

            if (first != null ? !first.equals(mapper.first) : mapper.first != null) return false;
            if (second != null ? !second.equals(mapper.second) : mapper.second != null) return false;
            if (third != null ? !third.equals(mapper.third) : mapper.third != null) return false;
            if (four != null ? !four.equals(mapper.four) : mapper.four != null) return false;
            return fift != null ? fift.equals(mapper.fift) : mapper.fift == null;

        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            result = 31 * result + (third != null ? third.hashCode() : 0);
            result = 31 * result + (four != null ? four.hashCode() : 0);
            result = 31 * result + (fift != null ? fift.hashCode() : 0);
            return result;
        }
    }

}

