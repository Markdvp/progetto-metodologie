package gapp.ulg.game.util;

import gapp.ulg.game.board.*;

import java.util.List;
import java.util.Optional;
import java.util.*;

public class Tree<P> implements PlayerGUI.MoveChooser<P> {

    /**
     * Gameruler specificato
     */
    private GameRuler<P> currentGame;

    /**
     * Struttura ad albero delle mosse valide in una opportuna situazione di gioco
     */
    private List<Pair<P>> tree;

    /**
     * Lista di mappature delle disposizioni dei pezzi della board del {@link GameRuler} specificato associate ad una lista di pezzi
     * @see BoardToPieces
     */
    private List<BoardToPieces<P>> boardTree;

    /**
     * Insieme delle mosse valide in una opportuna situazione di gioco
     */
    private Set<Move<P>> validmoves;

    /**
     * Nodo corrente mantenuto da un oggetto che implementa MoveChooser 
     * (Per maggiori info consultare il Javadoc di {@link gapp.ulg.game.util.PlayerGUI.MoveChooser})
     */
    private Nodo<P> currentNode;


    /**
     * Selezione corrente mantenuta da un oggetto che implementa MoveChooser
     * (Per maggiori info consultare il Javadoc di {@link gapp.ulg.game.util.PlayerGUI.MoveChooser})
     */
    private Set<Pos> currentSelection;

    /**
     * Mossa scelta mantenuta da un oggetto che implementa MoveChooser
     * (Per maggiori info consultare il Javadoc di {@link gapp.ulg.game.util.PlayerGUI.MoveChooser})
     */
    private Move<P> choosenMove;

    /**
     * Lista delle sottomosse dei nodi selezionati
     */
    private List<Move<P>> currentSelectedMoves;

    /**
     * Booleano che indica se è stata scelta o meno una mossa
     */
    private volatile boolean moveHasBeenChoosen = false;

    /**
     * Booleano che indica se è ancora possibile utilizzare questo movechooser
     */
    private volatile boolean isInterrupted = false;

    /**
     * Crea un oggetto di tipo {@link Tree}, che in ogni situazione
     * di gioco facilita la scelta delle mosse da parte dell' utente.
     * @param gameRuler il gioco specificato
     * @throws NullPointerException se validMoves è null;
     */
    public Tree(GameRuler<P> gameRuler) {

        if(gameRuler == null)
            throw new NullPointerException("Nessun gioco impostato");

        currentGame = gameRuler;
        validmoves = currentGame.validMoves(); // Imposto l'insieme delle mosse possibili nella situazione gioco corrente
        boardTree = new ArrayList<>();

        Map<Pos,P> boardPosToPiece = new HashMap<>();

        for (Pos p: gameRuler.getBoard().positions())
           if (gameRuler.getBoard().get(p) != null)
               boardPosToPiece.put(p, gameRuler.getBoard().get(p));

        boardTree.add(new BoardToPieces<>(boardPosToPiece, new ArrayList<>()));

        tree = new ArrayList<>(); // inizializzo l' albero delle mosse;

        List<Nodo<P>> primaListaNodi = new ArrayList<>(); // inizializzo la prima lista dei nodi che saranno figli della radice

        List<List<Action<P>>> probablyOneListList = moveToListAction(validmoves); // inizializzo una lista che conterrà tutte le liste di azioni delle mosse valide nell' attuale situazione di gioco

        if (probablyOneListList.size() == 1) // se contiene solamente una lista di azioni ...
            tree.add(new Pair<>(new Nodo<>(probablyOneListList.get(0)), new ArrayList<>())); // ... essa sarà mappata come un nuovo nodo, in questo caso radice, che come padre ha la lista di azioni e nessun figlio.

        else {  // altrimenti ...

            for (List<Action<P>> list : probablyOneListList) // ... Creo un Nodo per per ogni lista di azioni delle mosse valide, aggiunti poi come figli della radice
                primaListaNodi.add(new Nodo<>(list));

            tree.add(new Pair<>(buildRoot(moveToListAction(validmoves)), primaListaNodi));  // aggiugo all' albero una prima mappatura che contiene come padre il nodo radice tra tutte le mosse valide
            // e come figli tutte le mosse valide, rappresentate da nodi ognuno dei quali rappresenta una singola mossa valida
            for (Nodo<P> n : tree.get(0).getSecond())
                if (n.equals(tree.get(0).getFirst())) {
                    tree.get(0).getSecond().remove(n);
                    break;
                }

            treeCreator(); // genera la struttura dell' albero completa
        }

        currentNode = tree.get(0).getFirst(); // nel momento in cui l' albero viene creato il nodo corrente è rappresentato dalla radice dell' albero ...
        currentSelection = new HashSet<>();  // ... la selezione corrente inizializzata...
        choosenMove = null; // e inizializzo la mossa che l'utente dovrà scegliere;
        currentSelectedMoves = new ArrayList<>();
    }

    /**
     * Ritorna un oggetto di tipo {link @Nodo } , radice dell' albero delle mosse valide
     * @param listsActions una lista contenente le liste di azioni delle mosse valide
     * @return la radice dell' albero delle mosse valide
     * @throws NullPointerException se listAction è null
     */
    private synchronized Nodo<P> buildRoot(List<List<Action<P>>> listsActions) {

        if (listsActions == null) throw new NullPointerException("La lista contenente le liste di azioni delle mosse valide non può mai essere null");

        Set<Action<P>> setPrefissi = new HashSet<>(); // inizializzo un set per i prefissi delle liste di Action
        
        List<Action<P>> listaAzioniRadice = new ArrayList<>(); // inizializzo la lista delle azioni della radice

        for (List<Action<P>> ll : listsActions) // per ogni lista di action aggiunfo al set dei prefissi la prima action di ogni lista ...
            setPrefissi.add(ll.get(0));

        
        if (setPrefissi.size() > 1) // ... se la grandezza del set che contiene i prefissi è > 1 vuol dire che esistono almeno due mosse che hanno la prima action differente ...
            return new Nodo<>();   // ... quindi la radice sarà un nuovo nodo la cui lista di azioni sarà vuota (prefisso vuoto)

        
        for (List<Action<P>> ll : listsActions)   // altrimenti per ogni lista di azioni, se la lista di azioni della radice è vuota ...
            
            if (listaAzioniRadice.size() == 0) { // ... aggiungo la prima action, poichè questa action è in comune a tutte le mosse, ed esco dal ciclo
                
                listaAzioniRadice.add(ll.get(0));
                
                break;
            }

        
        int sizeMinore = sizeMinore(listsActions); // calcolo tra le liste di azioni delle mosse quella di grandezza minore (lo faccio per prevenire eventuali eccezioni)

        
        if (sizeMinore == 1) // se la lista di grandezza minore è uguale a 1 ...
        
            return new Nodo<>(new ArrayList<>(setPrefissi)); // ... significa che tutte le mosse condividono solo la prima action e quindi abbiamo la nostra radice e la ritorniamo

        
        for (int i = 1; i < sizeMinore; i++) {  // altrimenti partendo dall' action all' indice 1 (poichè all' indice 0 gia sappiamo essere condivisa da tutte) fino alla grandezza della lista minore ...
            
            Action<P> azioneAttuale = null;  // ... inizializzo una mossa 

            for (List<Action<P>> ll : listsActions) {    // ... e per ogni lista di azioni

                if (azioneAttuale == null)
                    azioneAttuale = ll.get(i); // la prima volta prendo l' azione all indice i e la faccio diventare l' azione attuale che verrà confrontata con le altre ...

                else if (azioneAttuale.equals(ll.get(i)))  // se l' azione attuale è uguale a quella corrente all' indice i passiamo alla prossima iterazione ...
                    continue;

                else
                    return new Nodo<>(listaAzioniRadice); // ... altrimenti se sono diverse significa che non tutte le mosse hanno in comunque la seconda (o la terza ecc) e quindi ritorno la lista di azioni della radice attuale

            }

            listaAzioniRadice.add(azioneAttuale); // se il ciclo piu interno è terminato senza ritornare significa che l' azione attuale è comune a tutte le mosse quindi deve essere aggiunta nella lista di azioni della radice

            if (i == sizeMinore - 1) // se l' indice è proprio uguale alla grandezza dell lista minore ...
                return new Nodo<>(listaAzioniRadice); // ... ritorniamo la radice con la lista di azioni che abbiamo arrivati a questo punto

        }

        return null;
    }

    /**
     * Ritorna un oggetto di tipo {@link Pair} , che associa ad un nodo padre la lista
     * dei suoi nodi figli, che verrà aggiunto all' albero
     * @param padreFigli mappatura 'nodo padre -> lista dei nodi figli'
     * @param n nodo corrente
     * @return una mappatura 'nodo padre -> lista di nodi figli'
     * @throws NullPointerException se padreFigli o n è null
     */
    private synchronized Pair<P> addNode(Pair<P> padreFigli, Nodo<P> n) {
        
        if(padreFigli == null || n == null)
            throw new NullPointerException("La mappatura padreFiglio e/o il nodo corrente non puo/possono essere nul");

        List<Action<P>> nuovoPrefisso = new ArrayList<>(); // inizializzo una lista di action per creare un nuovo prefisso
        
        List<Pair<P>> possibiliCoppie = new ArrayList<>(); // inizializzo una lista di mappatura padre figlio 

        for (Action<P> a : padreFigli.getFirst().getAction()) // aggiungo ogni azione del nodo padre alla lista dei nuovi prefissi
            nuovoPrefisso.add(a);

        nuovoPrefisso.add(n.getAction().get(nuovoPrefisso.size())); //Creo un nuovo prefisso per questo nodo, e se trovo almeno un altro nodo che cominci in questo modo, allora sarà a sua volta un nuovo nodo che li ha come figli

        for (Nodo<P> n1 : padreFigli.getSecond()) { // per ogni nodo nella lista dei figli della radice 

            if (!(n1.equals(n))) {  // se il nodo in esame non è se stesso allora...
                
                int counter = 0; // ...inizializzo un contatore

                for (int i = 0; i < nuovoPrefisso.size(); i++)  // se il nodo in esame, all' indice i, ha la stessa action del nuovo prefisso all' indice i incremento il contatore
                    if (n1.getAction().get(i).equals(nuovoPrefisso.get(i)))
                        counter++;

                if (counter == nuovoPrefisso.size()) {  // uscito dal ciclo se il contatore è uguale alla grandezza del nuovo prefisso significa che tutte le mosse esaminate hanno il prefisso uguale al nuovo prefisso quindi ...
                    
                    Pair<P> newP = new Pair<>(new Nodo<>(nuovoPrefisso), new ArrayList<>(Arrays.asList(n, n1))); // ... creo nuova mappatura che ha come padre il nodo con il nuovo prefisso e come figli il nodo corrente e se stesso

                    if (!possibiliCoppie.contains(newP)) // se questa mappatura non è gia presente nella lista delle possibili mappature l' aggiungo
                        possibiliCoppie.add(newP);
                }

                
                else {   // altrimenti significa che il nuovo nodo è figlio diretto della radice
                    
                    Pair<P> newP2 = new Pair<>(n, new ArrayList<>()); // quindi creo una nuova mappatura che ha come padre il nodo e nessun figlio (foglia dell' albero)

                    if (!possibiliCoppie.contains(newP2)) // se non è gia contenuta nelle possibili coppie l' aggiungo
                        possibiliCoppie.add(newP2);
                }

            }

        }

        if (possibiliCoppie.size() == 1)  // se la lista contiene un solo elemento ritorniamo direttamente quella mappatura
            return possibiliCoppie.get(0);

        List<Pair<P>> coseDaEliminare = new ArrayList<>(); // altrimenti inizializziamo una lista di mappature da eliminare

        for (Pair<P> p1 : possibiliCoppie) // confrontando a coppie gli elementi nella lista tranne che con se stessi ...
            for (Pair<P> p2 : possibiliCoppie)
                if (!p1.equals(p2))
                    if (p1.getSecond().contains(p2.getFirst())) // ... se un nodo che risulta essere padre nella lista è allo stesso tempo figlio di un altro nodo ...
                        coseDaEliminare.add(p2);  // ... viene aggiunto nella lista delle mappature da eliminare ...

        possibiliCoppie.removeAll(coseDaEliminare); // ... ed eliminato definitivamente

        while (true) {
            boolean somethingToDelete = false;

            for (Pair<P> p1 : possibiliCoppie) { // confrontanto nuovamente gli elementi a coppie tranne che con se stessi ...
                for (Pair<P> p2 : possibiliCoppie) {
                    if (!p1.equals(p2)) {
                        if (p1.getFirst().equals(p2.getFirst())) {  // ... se due nodi differenti all' interno della stessa lista condividono lo stesso padre, allora ...
                            p1.getSecond().addAll(p2.getSecond()); //  ... le liste dei rispettivi figli verrano unite ed eliminati i duplicati ...
                            p1.setSecond(new ArrayList<>(new HashSet<>(p1.getSecond())));
                            possibiliCoppie.remove(p2);  // a questo punto possiamo eliminare il nodo superfluo ed uscire dal ciclo

                            somethingToDelete = true;
                            break;
                        }

                    }

                }

                if (somethingToDelete) break;
            }

            if (!somethingToDelete) break;
        }

        return possibiliCoppie.get(0);
    }

    /**
     * Genera la struttura ad albero a partire dalla prima mappatura
     * radice - figli della radice
     */
    private synchronized void treeCreator() {

        /**tree 'grezzo'*/
        int indice = 0;

        while (true) {

            for (Nodo<P> n : tree.get(indice).getSecond()) {
                Pair<P> p = addNode(tree.get(indice), n);

                boolean alreadyInTree = false;

                for (Pair<P> p1 : tree)
                    if (p1.equals(p))
                        alreadyInTree = true;

                if (!alreadyInTree)
                    tree.add(p);

            }

            if (indice == tree.size() - 1) break;
            indice++;
        }

        /**tree 'quasi ok'*/
        while (true) {
            boolean somethingToDelete = false;

            for (Pair<P> p1 : tree) {
                for (Pair<P> p2 : tree) {

                    /**Se le mappature sono diverse, hanno gli stessi figli, hanno almeno un figlio,
                     *  le azioni del nodo padre contengono già quelle dell'altro nodo padre, ed i padri non hanno lo stesso numero di azioni:*/
                    if (!p1.equals(p2) && p1.getSecond().equals(p2.getSecond()) && (p1.getSecond().size() != 0)
                            && p1.getFirst().getAction().containsAll(p2.getFirst().getAction())
                            && (p1.getFirst().getAction().size() >= p2.getFirst().getAction().size())) {

                        somethingToDelete = true;
                        tree.remove(p2);
                        break;
                    }

                }

                if (somethingToDelete) break;
            }

            if (!somethingToDelete) break;
        }

        /**tree 'corretto'*/
        for (Pair<P> p1 : tree) {
            for (Pair<P> p2 : tree) {
                if (!p1.equals(p2)) {

                    /**Se la lista dei nodi figli del primo contiene tutti i nodi figli del secondo, e p1,p2 non sono foglie:*/
                    if (p1.getSecond().containsAll(p2.getSecond()) && p1.getSecond().size() != 0 && p2.getSecond().size() != 0) {
                        List<Nodo<P>> nuovaLista = nodiNonInComune(p1.getSecond(), p2.getSecond());
                        nuovaLista.add(new Nodo<>(p2.getFirst().getAction()));
                        p1.setSecond(nuovaLista);
                    }
                }
            }
        }

    }

    /**
     * Ritorna la grandezza della lista di minor dimensione tra quelle passate in input
     * @param valid lista contenente le liste delle azioni delle mosse valide
     * @return la grandezza della lista della minor dimensione
     * @throws NullPointerException se valid è null
     */
    private synchronized int sizeMinore(List<List<Action<P>>> valid) {

        if(valid == null)
            throw new NullPointerException("valid non puo essere null");

        int size = 100_000_000;

        for (List<Action<P>> ll : valid)
            if (ll.size() < size)
                size = ll.size();

        return size;
    }

    /**
     * Trasforma un insieme di mosse in una lista contenente le liste delle azioni delle mosse
     * @param valid insieme delle mosse valide
     * @return una lista contenente le liste delle azioni delle mosse valide
     * @throws NullPointerException se valid è null
     */
    private synchronized List<List<Action<P>>> moveToListAction(Set<Move<P>> valid) {

        if(valid == null)
            throw new NullPointerException("valid non puo essere null");

        List<List<Action<P>>> listaDiListeDiAzioni = new ArrayList<>();

        for (Move<P> m : valid)
            if (!m.kind.equals(Move.Kind.PASS) && !m.kind.equals(Move.Kind.RESIGN))
                listaDiListeDiAzioni.add(m.actions);

        return listaDiListeDiAzioni;
    }

    /**
     * @return l' albero delle mosse valide
     */
    public synchronized List<Pair<P>> getTree() {
        return tree;
    }

    /**
     * @return la lista delle mappature posizione-pezzo, lista pezzi
     * @see BoardToPieces
     */
    public synchronized List<BoardToPieces<P>> getBoardTree() {
        return boardTree;
    }

    /**
     * @return il nodo corrente
     */
    public synchronized Nodo<P> getCurrentNode() {
        return currentNode;
    }

    /**
     * Setta il nodo corrente
     */
    public synchronized void setCurrentNode(Nodo<P> currentNode) {
        this.currentNode = currentNode;
    }

    /**
     * @return la selezione corrente delle posizioni
     */
    public synchronized Set<Pos> getCurrentSelection() {
        return currentSelection;
    }

    /**
     * @return la mossa scelta
     */
    public synchronized Move<P> getChoosenMove() {
        return choosenMove;
    }

    /**
     * @return true se la mossa finale è stata scelta
     */
    public synchronized boolean isMoveHasBeenChoosen() {
        return moveHasBeenChoosen;
    }

    /**
     * Imposta se è ancora possibile utilizzare questo movechooser
     */
    public synchronized void setInterrupted(boolean interrupted) {
        isInterrupted = interrupted;
    }

    /**
     * Crea una nuova lista che contiene la differenza dei nodi delle liste passate in input
     * @param l1 lista di nodi
     * @param l2 lista di nodi
     * @param <P> tipo del modello di pezzo
     * @return una lista di nodi
     * @throws NullPointerException se l1 o l2 è null
     */
    private static synchronized <P> List<Nodo<P>> nodiNonInComune(List<Nodo<P>> l1, List<Nodo<P>> l2) {

        if(l1 == null || l2 == null)
            throw new NullPointerException("Le liste non possono essere null");

        List<Nodo<P>> nuovaLista = new ArrayList<>();

        for (Nodo<P> n : l1)
            if (!l2.contains(n))
                nuovaLista.add(n);

        return nuovaLista;
    }

    /**
     * Crea una nuova lista che contiene la differenza delle azioni delle liste passate in input
     * @param n1 nodo
     * @param n2 nodo
     * @param <P> tipo del modello di pezzo
     * @return una lista di azioni
     * @throws NullPointerException se n1 o n2 è null
     */
    private static synchronized  <P> List<Action<P>> azioniNonInComune(Nodo<P> n1, Nodo<P> n2) {

        if(n1 == null)
            throw new NullPointerException("I nodi non possono essere null");

        else if (n2 == null)
            return n1.getAction();

        List<Action<P>> nuovaLista = n1.getAction();
        List<Action<P>> nuovaLista2 = new ArrayList<>();

        int minS = n2.getAction().size();

        for (int i = minS; i < nuovaLista.size(); i++)
            nuovaLista2.add(nuovaLista.get(i));

        return nuovaLista2;
    }

    /**
     * Ritorna il nodo padre del nodo passato in input
     * @param son nodo corrente
     * @return il nodo padre del nodo corrente
     * @throws NullPointerException se son è null
     */
    public synchronized Nodo<P> parent(Nodo<P> son) {

        if(son == null)
            throw new NullPointerException("Il nodo corrente non puo essere null");

        for (Pair<P> p : tree)
            if (p.getSecond().contains(son))
                return p.getFirst();

        return null;
    }

    /**
     * Crea una mappa della disposizione dei pezzi della board del Gameruler specificato ed esegue su di essa
     * la lista di azioni passata in input, fatto ciò ritorna un {@link BoardToPieces}
     * che associa la mappatura della board ad una lista di pezzi, il cui contenuto varierà
     * a seconda di quali azioni verranno eseguite, per esempio nel caso di una
     * {@link Action.Kind#REMOVE} conterrà i pezzi rimossi da tale azione.
     * @param board la board del gameruler specificato
     * @param list la lista delle azioni di una mossa
     * @return un {@link BoardToPieces}
     * @throws NullPointerException se la board o la lista di azioni è null
     * @see BoardToPieces
     */
    private synchronized BoardToPieces<P> boardModifier(BoardToPieces<P> board, List<Action<P>> list) {

        if(board == null || list == null)
            throw new NullPointerException("La board e/o la lista di azioni da eseguire non puo/possono essere null");


        Map<Pos,P> modifiedBoard = new HashMap<Pos, P>(board.getFirst());
        List<P> modifiedList = new ArrayList<P>(board.getSecond());

        for (Action<P> a : list) {

            switch (a.kind) {

                case ADD:
                   modifiedBoard.put(a.pos.get(0),a.piece);
                    break;

                case JUMP:
                   P pieceJUMP = modifiedBoard.get(a.pos.get(0));
                    modifiedBoard.remove(a.pos.get(0));
                    modifiedBoard.put(a.pos.get(1), pieceJUMP);
                    break;

                case MOVE:

                    for (Pos p : a.pos) {
                        P pieceMOVE = modifiedBoard.get(p);
                        modifiedBoard.remove(p);
                        modifiedBoard.put(backActionMove(p, a.dir, a.steps), pieceMOVE);
                    }

                    break;

                case REMOVE:

                    for (Pos p : a.pos) {
                        P pieceREMOVE = modifiedBoard.get(p);
                        modifiedBoard.remove(p);
                        modifiedList.add(pieceREMOVE);
                    }

                    break;

                case SWAP:

                    for (Pos p : a.pos) {
                        P pieceSWAP = modifiedBoard.get(p);
                        modifiedBoard.remove(p);
                        modifiedList.add(pieceSWAP);
                        modifiedBoard.put(p, a.piece);
                    }

                    break;

            }

        }

        return new BoardToPieces<P>(modifiedBoard, modifiedList);
    }

    /**
     * Ritorna la nuova posizione che si ottiene dalla posizione presa in input
     * traslata per np passi nella direzione d
     * @param p una posizione
     * @param d una direzione
     * @param np numero di passi
     * @return la nuova posizione
     */
    public synchronized Pos backActionMove(Pos p, Board.Dir d, int np) {
        Pos posDaRit = null;

        Objects.requireNonNull(p);
        Objects.requireNonNull(d);

        if (d.equals(Board.Dir.UP))
            posDaRit = new Pos(p.b, p.t + np);

        else if (d.equals(Board.Dir.UP_R))
            posDaRit = new Pos(p.b + np, p.t + np);

        else if (d.equals(Board.Dir.RIGHT))
            posDaRit = new Pos(p.b + np, p.t);

        else if (d.equals(Board.Dir.DOWN_R))
            posDaRit = new Pos(p.b + np, p.t - np);

        else if (d.equals(Board.Dir.DOWN))
            posDaRit = new Pos(p.b, p.t - np);

        else if (d.equals(Board.Dir.DOWN_L))
            posDaRit = new Pos(p.b - np, p.t - np);

        else if (d.equals(Board.Dir.LEFT))
            posDaRit = new Pos(p.b - np, p.t);

        else if (d.equals(Board.Dir.UP_L))
            posDaRit = new Pos(p.b - np, p.t + np);

        return posDaRit;
    }

    /**Setter della lista di mosse selezionate*/
    public void setCurrentSelectedMoves(List<Move<P>> currentSelectedMoves) {
        this.currentSelectedMoves = currentSelectedMoves;
    }

    @Override
    public synchronized Optional<Move<P>> subMove() {

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        if (tree.size() == 0)
            return null;

        else if (currentNode.getAction().size() == 0)
            return Optional.empty();

        Nodo<P> parent = parent(currentNode);
        Move<P> subMove = new Move<>(azioniNonInComune(currentNode, parent));

        return Optional.of(subMove);
    }

    @Override
    public synchronized List<Move<P>> childrenSubMoves() {

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        if (isFinal())
            return new ArrayList<>();

        List<Move<P>> moveList = new ArrayList<>();

        for (Pair<P> p : tree)
            if (p.getFirst().equals(currentNode))
                for (Nodo<P> n : p.getSecond())
                    moveList.add(new Move<>(azioniNonInComune(n, currentNode)));

        return moveList;
    }

    @Override
    public synchronized List<Move<P>> select(Pos... pp) {
        clearSelection();
        currentSelectedMoves.clear();

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        for (Pos p : pp) {
            if (p == null)
                throw new NullPointerException("Una delle pos in pp è null!");

            else if (!currentGame.getBoard().isPos(p))
                throw new IllegalArgumentException("Una delle pos in pp non fa parte della board");
        }

        if (pp.length == 0 || pp.length != new HashSet<>(Arrays.asList(pp)).size())
            throw new IllegalArgumentException("Non è presente alcuna posizione o sono presenti posizioni duplicate");

        currentSelection.addAll(Arrays.asList(pp));
        List<Move<P>> moveList = new ArrayList<>();

        for (Move<P> m : childrenSubMoves()) {
            if (!((Action) m.actions.get(0)).kind.equals(Action.Kind.JUMP) && new HashSet<>((m.actions.get(0)).pos).equals(currentSelection))
                moveList.add(m);

            else if (((Action) m.actions.get(0)).kind.equals(Action.Kind.JUMP) && currentSelection.equals(new HashSet<>(Collections.singletonList(((Action) m.actions.get(0)).pos.get(0)))))
                moveList.add(m);

        }

        currentSelectedMoves = moveList;
        return moveList;
    }

    @Override
    public synchronized List<Move<P>> quasiSelected() {
        List<Move<P>> moveList = new ArrayList<>();

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        else if (currentSelection.isEmpty())
            return moveList;

        else if (tree.isEmpty())
            return null;

        for (Move m : childrenSubMoves())
            if (!((Action) m.actions.get(0)).kind.equals(Action.Kind.JUMP) && new HashSet<>(((Action) m.actions.get(0)).pos).containsAll(currentSelection)
                    && new HashSet<>(((Action) m.actions.get(0)).pos).size() != currentSelection.size())
                moveList.add(m);

        return moveList;
    }

    @Override
    public synchronized List<P> selectionPieces() {

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        if (tree.isEmpty())
            return null;

        List<P> pieces = new ArrayList<>();

        if (!currentSelectedMoves.isEmpty()) {

            if (currentSelectedMoves.size() == 1) {
                if (((Action) currentSelectedMoves.get(0).actions.get(0)).kind.equals(Action.Kind.REMOVE))
                    return Collections.singletonList(null);

                else if (((Action) currentSelectedMoves.get(0).actions.get(0)).kind.equals(Action.Kind.ADD) || ((Action) currentSelectedMoves.get(0).actions.get(0)).kind.equals(Action.Kind.SWAP))
                    return new ArrayList<>(Collections.singletonList((currentSelectedMoves.get(0).actions.get(0)).piece));
            }

            else if (currentSelectedMoves.size() > 1) {
                Action.Kind kind = null;
                boolean stillOk = true;

                for (Move move : currentSelectedMoves) {

                    if (kind == null)
                        kind = ((Action) move.actions.get(0)).kind;

                    else if (!((Action) move.actions.get(0)).kind.equals(kind)) {
                        stillOk = false;
                        break;
                    }

                }

                if (kind.equals(Action.Kind.MOVE) || kind.equals(Action.Kind.REMOVE) || kind.equals(Action.Kind.JUMP))
                    stillOk = false;

                if (stillOk) {
                    for (Move<P> move : currentSelectedMoves)
                        pieces.add((move.actions.get(0)).piece);
                    return pieces;
                }
            }

        }

        return new ArrayList<>();
    }

    @Override
    public synchronized void clearSelection() {

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        currentSelection.clear();
    }

    @Override
    public synchronized Move<P> doSelection(P pm) {
        clearSelection();

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        if (!currentSelectedMoves.isEmpty()) {

            if (currentSelectedMoves.size() == 1) {

                if (((Action) currentSelectedMoves.get(0).actions.get(0)).kind.equals(Action.Kind.REMOVE) && pm == null)
                    for (Pair<P> pair : tree) {
                        if (pair.getFirst().equals(currentNode))
                            for (Nodo<P> n : pair.getSecond())
                                if (azioniNonInComune(n,currentNode).equals(currentSelectedMoves.get(0).actions)) {
                                    currentNode = n;

                                    boardTree.add(boardModifier(boardTree.get(boardTree.size() - 1), subMove().get().actions));

                                    clearSelection();
                                    return subMove().get();
                                }
                    }

                else if (((Action) currentSelectedMoves.get(0).actions.get(0)).kind.equals(Action.Kind.ADD) || ((Action) currentSelectedMoves.get(0).actions.get(0)).kind.equals(Action.Kind.SWAP))
                    if (((Action) currentSelectedMoves.get(0).actions.get(0)).piece.equals(pm))
                        for (Pair<P> pair : tree)
                            if (pair.getFirst().equals(currentNode))
                                for (Nodo<P> n : pair.getSecond())
                                    if (azioniNonInComune(n,currentNode).equals(currentSelectedMoves.get(0).actions)) {
                                        currentNode = n;

                                        boardTree.add(boardModifier(boardTree.get(boardTree.size() - 1), subMove().get().actions));

                                        clearSelection();
                                        return subMove().get();
                                    }

            }

            else if (currentSelectedMoves.size() > 1) {
                Action.Kind kind = null;
                boolean stillOk = true;

                for (Move move : currentSelectedMoves) {

                    if (kind == null)
                        kind = ((Action) move.actions.get(0)).kind;

                    else if (!((Action) move.actions.get(0)).kind.equals(kind)) {
                        stillOk = false;
                        break;
                    }

                }

                if (kind.equals(Action.Kind.MOVE) || kind.equals(Action.Kind.REMOVE) || kind.equals(Action.Kind.JUMP))
                    stillOk = false;

                if (stillOk)
                    for (Pair<P> pair : tree)
                        if (pair.getFirst().equals(currentNode))
                            for (Nodo<P> n : pair.getSecond()) {
                                for (int i = 0; i < currentSelectedMoves.size(); i++)
                                    if (azioniNonInComune(n,currentNode).equals(currentSelectedMoves.get(i).actions) && n.getAction().get(0).piece.equals(pm)) {
                                        currentNode = n;

                                        boardTree.add(boardModifier(boardTree.get(boardTree.size() - 1), subMove().get().actions));

                                        clearSelection();
                                        return subMove().get();
                                    }
                            }
            }

        }

        return null;
    }

    @Override
    public synchronized Move<P> jumpSelection(Pos p) {
        clearSelection();

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        for (Move m : currentSelectedMoves)
            if (((Action) m.actions.get(0)).kind.equals(Action.Kind.JUMP) && ((Action) m.actions.get(0)).pos.get(1).equals(p))
                for (Pair<P> pair : tree)
                    if (pair.getFirst().equals(currentNode))
                        for (Nodo<P> n : pair.getSecond())
                            if (n.getAction().containsAll(m.actions)) {
                                currentNode = n;

                                boardTree.add(boardModifier(boardTree.get(boardTree.size() - 1), subMove().get().actions));

                                clearSelection();
                                return subMove().get();
                            }

        return null;
    }

    @Override
    public synchronized Move<P> moveSelection(Board.Dir d, int ns) {
        clearSelection();

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        for (Move m : currentSelectedMoves)
            if (((Action) m.actions.get(0)).kind.equals(Action.Kind.MOVE) && ((Action) m.actions.get(0)).dir.equals(d) && ((Action) m.actions.get(0)).steps == (ns))
                for (Pair<P> pair : tree)
                    if (pair.getFirst().equals(currentNode))
                        for (Nodo<P> n : pair.getSecond())
                            if (n.getAction().containsAll(m.actions)) {
                                currentNode = n;

                                boardTree.add(boardModifier(boardTree.get(boardTree.size() - 1), subMove().get().actions));

                                clearSelection();
                                return subMove().get();
                            }

        return null;
    }

    @Override
    public synchronized Move<P> back() {
        clearSelection();

        EnumMap<Board.Dir,Board.Dir> toDisp = new EnumMap<>(Board.Dir.class);

        toDisp.put(Board.Dir.UP, Board.Dir.DOWN);
        toDisp.put(Board.Dir.UP_R, Board.Dir.DOWN_L);
        toDisp.put(Board.Dir.RIGHT, Board.Dir.LEFT);
        toDisp.put(Board.Dir.DOWN_R, Board.Dir.UP_L);
        toDisp.put(Board.Dir.DOWN, Board.Dir.UP);
        toDisp.put(Board.Dir.DOWN_L, Board.Dir.UP_R);
        toDisp.put(Board.Dir.LEFT, Board.Dir.RIGHT);
        toDisp.put(Board.Dir.UP_L, Board.Dir.DOWN_R);

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        if (parent(currentNode) == null || tree.isEmpty())
            return null;

        Move<P> move = new Move<>(subMove().get().actions);
        List<Action<P>> inverseActionsList = new ArrayList<>();

        Nodo<P> oldCurrent = currentNode;
        currentNode = parent(currentNode);

        for (int i = move.actions.size() - 1; i >= 0; i--) {

            switch (move.actions.get(i).kind) {

                case ADD:
                    inverseActionsList.add(new Action<>(move.actions.get(i).pos.get(0)));
                    break;

                case JUMP:
                    inverseActionsList.add(new Action<>(move.actions.get(i).pos.get(1),move.actions.get(i).pos.get(0)));
                    break;

                case MOVE:
                    List<Pos> reversePositions = new ArrayList<>();

                    for (Pos p : move.actions.get(i).pos)
                        reversePositions.add(backActionMove(p, move.actions.get(i).dir, move.actions.get(i).steps));

                    inverseActionsList.add(new Action<>(toDisp.get(move.actions.get(i).dir), move.actions.get(i).steps, reversePositions.toArray(new Pos[reversePositions.size()])));
                    break;

                case REMOVE:
                    List<Action<P>> reverseActionList = new ArrayList<>();

                    for (int p = move.actions.get(i).pos.size() - 1; p >= 0; p--) {
                        Pos newPos = move.actions.get(i).pos.get(p);
                        List<P> pieceList = boardTree.get(boardTree.size() - 1).getSecond();

                        reverseActionList.add(new Action<>(newPos, pieceList.get(pieceList.size() -1)));

                        boardTree.get(boardTree.size() - 1).getSecond().remove(boardTree.get(boardTree.size() - 1).getSecond().size() - 1);
                    }

                    inverseActionsList.addAll(reverseActionList);
                    break;

                case SWAP:
                    List<Action<P>> reverseActionList2 = new ArrayList<>();

                    for (int p = move.actions.get(i).pos.size() - 1; p >= 0; p--) {
                        Pos newPos = move.actions.get(i).pos.get(p);
                        List<P> pieceList = boardTree.get(boardTree.size() - 1).getSecond();

                        reverseActionList2.add(new Action<P>(pieceList.get(pieceList.size() -1), newPos));

                        boardTree.get(boardTree.size() - 1).getSecond().remove(boardTree.get(boardTree.size() - 1).getSecond().size() - 1);
                    }

                    inverseActionsList.addAll(reverseActionList2);
                    break;

            }

        }

        boardTree.remove(boardTree.size() - 1);
        return new Move<>(inverseActionsList);
    }

    @Override
    public synchronized boolean isFinal() {

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        for (Pair<P> p : tree)
            if (p.getFirst().equals(currentNode) && p.getSecond().size() == 0)
                return true;

        return false;
    }

    @Override
    public synchronized void move() {

        if (choosenMove != null || tree.size() == 0 || !isFinal() || isInterrupted)
            throw new IllegalStateException("Mossa già scelta, albero vuoto, nodo corrente non finale oppure scelta mossa interrotta!");

        moveHasBeenChoosen = true;
        choosenMove = new Move<>(currentNode.getAction());
        notifyAll();
    }

    @Override
    public synchronized boolean mayPass() {

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        return validmoves.contains(new Move<P>(Move.Kind.PASS));
    }

    @Override
    public synchronized void pass() {

        if (choosenMove != null || !mayPass() || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure non è possibile passare il turno oppure scelta mossa interrotta!");

        moveHasBeenChoosen = true;
        choosenMove = new Move<>(Move.Kind.PASS);
        notifyAll();
    }

    @Override
    public synchronized void resign() {

        if (choosenMove != null || isInterrupted)
            throw new IllegalStateException("Mossa già scelta oppure scelta della mossa interrotta!");

        moveHasBeenChoosen = true;
        choosenMove = new Move<>(Move.Kind.RESIGN);
        notifyAll();
    }


    /**
     * Un {@code BoardToPieces} è un oggetto che presa in input una mappa della disposizioni
     * dei pezzi, relativi ad una board di gioco, associa ad essa una lista di pezzi.
     * @param <P> tipo del modello dei pezzi
     */
    private static class BoardToPieces<P> {

        /**
         * Mappa posizione,pezzo
         */
        private Map<Pos,P> first;

        /**
         * Lista di pezzi
         */
        private List<P> second;

        /**
         * Crea un oggetto {@code BoardToPieces}
         * @param primo mappa posizione,pezzo
         * @param secondo lista di pezzi
         */
        public BoardToPieces(Map<Pos,P> primo, List<P> secondo) {
            first = primo;
            second = secondo;
        }

        /**
         * @return la mappa posizione,pezzo
         */
        public synchronized Map<Pos,P> getFirst() {
            return first;
        }

        /**
         * Imposta il valore della mappa posizione,pezzo con quella passato in input
         * @param first mappa posizione,pezzo
         */
        public synchronized void setFirst(Map<Pos,P> first) {
            this.first = first;
        }

        /**
         * @return la lista dei pezzi
         */
        public synchronized List<P> getSecond() {
            return second;
        }

        /**
         * Imposta il valore della lista di pezzi con quella passata in input
         * @param second lista di pezzi
         */
        public synchronized void setSecond(List<P> second) {
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BoardToPieces<?> that = (BoardToPieces<?>) o;

            if (first != null ? !first.equals(that.first) : that.first != null) return false;
            if (second != null ? !second.equals(that.second) : that.second != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            return result;
        }
    }

}
