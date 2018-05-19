package gapp.ulg.game.util;

import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.Pos;

import static gapp.ulg.game.board.GameRuler.Situation;
import static gapp.ulg.game.board.GameRuler.Next;
import static gapp.ulg.game.board.GameRuler.Mechanics;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/** <b>IMPLEMENTARE I METODI INDICATI CON "DA IMPLEMENTARE" SECONDO LE SPECIFICHE
 * DATE NEI JAVADOC. Non modificare le intestazioni dei metodi.</b>
 * <br>
 * Metodi per analizzare giochi */
public class Probe {
    /** Un oggetto {@code EncS} è la codifica compatta di una situazione di gioco
     * {@link GameRuler.Situation}. È utile per mantenere in memoria insiemi con
     * moltissime situazioni minimizzando la memoria richiesta.
     * @param <P>  tipo del modello dei pezzi */
    public static class EncS<P> {
        /** Crea una codifica compatta della situazione data relativa al gioco la
         * cui meccanica è specificata. La codifica è compatta almeno quanto quella
         * che si ottiene codificando la situazione con un numero e mantenendo in
         * questo oggetto solamente l'array di byte che codificano il numero in
         * binario. Se i parametri di input sono null o non sono tra loro compatibili,
         * il comportamento è indefinito.
         * @param gM  la meccanica di un gioco
         * @param s  una situazione dello stesso gioco */
        public EncS(Mechanics<P> gM, Situation<P> s) {
            codedSit = code(gM, s);
        }
        public EncS(byte[] arrayByte) {
            codedSit = arrayByte;
        }

        public byte[] codedSit;

        public static <P> byte[] code(Mechanics<P> gM, Situation<P> s) {

            BigInteger codedSit = BigInteger.valueOf(0);
            Map<Pos,P> mappaSit = s.newMap();

            for (Pos p : gM.positions) {
                P pezzo = mappaSit.get(p);

                if (pezzo == null)
                    codedSit = codedSit.multiply(BigInteger.valueOf(3));
                else if (((PieceModel<PieceModel.Species>) pezzo).color.equals("nero"))
                    codedSit = codedSit.multiply(BigInteger.valueOf(3)).add(BigInteger.valueOf(1));
                else
                    codedSit = codedSit.multiply(BigInteger.valueOf(3)).add(BigInteger.valueOf(2));

                if (pezzo == null)
                    codedSit = codedSit.multiply(BigInteger.valueOf(9));
                else if (((PieceModel<PieceModel.Species>) pezzo).species.equals(PieceModel.Species.DISC))
                    codedSit = codedSit.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(1));
                else if (((PieceModel<PieceModel.Species>) pezzo).species.equals(PieceModel.Species.DAMA))
                    codedSit = codedSit.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(2));
                else if (((PieceModel<PieceModel.Species>) pezzo).species.equals(PieceModel.Species.PAWN))
                    codedSit = codedSit.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(3));
                else if (((PieceModel<PieceModel.Species>) pezzo).species.equals(PieceModel.Species.KNIGHT))
                    codedSit = codedSit.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(4));
                else if (((PieceModel<PieceModel.Species>) pezzo).species.equals(PieceModel.Species.BISHOP))
                    codedSit = codedSit.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(5));
                else if (((PieceModel<PieceModel.Species>) pezzo).species.equals(PieceModel.Species.ROOK))
                    codedSit = codedSit.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(6));
                else if (((PieceModel<PieceModel.Species>) pezzo).species.equals(PieceModel.Species.QUEEN))
                    codedSit = codedSit.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(7));
                else if (((PieceModel<PieceModel.Species>) pezzo).species.equals(PieceModel.Species.KING))
                    codedSit = codedSit.multiply(BigInteger.valueOf(9)).add(BigInteger.valueOf(8));

            }

            switch (s.turn) {
                case 0:
                    codedSit = codedSit.multiply(BigInteger.valueOf(5));
                break;
                case 1:
                    codedSit = codedSit.multiply(BigInteger.valueOf(5)).add(BigInteger.valueOf(1));
                break;
                case 2:
                    codedSit = codedSit.multiply(BigInteger.valueOf(5)).add(BigInteger.valueOf(2));
                break;
                case -1:
                    codedSit = codedSit.multiply(BigInteger.valueOf(5)).add(BigInteger.valueOf(3));
                break;
                case -2:
                    codedSit = codedSit.multiply(BigInteger.valueOf(5)).add(BigInteger.valueOf(4));
                break;
            }

            return codedSit.toByteArray();
        }

        /** Ritorna la situazione codificata da questo oggetto. Se {@code gM} è null
         * o non è la meccanica del gioco della situazione codificata da questo
         * oggetto, il comportamento è indefinito.
         * @param gM  la meccanica del gioco a cui appartiene la situazione
         * @return la situazione codificata da questo oggetto */
        public Situation<P> decode(Mechanics<P> gM) {
            BigInteger codifica = new BigInteger(codedSit);
            Map<Pos,P> mappaPerLaSit = new HashMap<>();

            //Prelevo il turno della Situation;
            int turnSit = codifica.remainder(BigInteger.valueOf(5)).intValue();
            codifica = (codifica.subtract(BigInteger.valueOf(turnSit)).divide(BigInteger.valueOf(5)));

            switch (turnSit) {
                case 0:
                    turnSit = 0;
                    break;
                case 1:
                    turnSit = 1;
                    break;
                case 2:
                    turnSit = 2;
                    break;
                case 3:
                    turnSit = -1;
                    break;
                case 4:
                    turnSit = -2;
                    break;
            }

            for (int i = gM.positions.size() - 1 ; i >= 0 ; i--) {

                int numToSpecies = codifica.remainder(BigInteger.valueOf(9)).intValue();
                codifica = (codifica.subtract(BigInteger.valueOf(numToSpecies))).divide(BigInteger.valueOf(9));

                int numToColor = codifica.remainder(BigInteger.valueOf(3)).intValue();
                codifica = (codifica.subtract(BigInteger.valueOf(numToColor))).divide(BigInteger.valueOf(3));

                String colore = null;
                PieceModel.Species specie = null;

                switch (numToColor) {
                    case 0:
                        break;
                    case 1:
                        colore = "nero";
                        break;
                    case 2:
                        colore = "bianco";
                        break;
                }

                switch (numToSpecies) {
                    case 0:
                        break;
                    case 1:
                        specie = PieceModel.Species.DISC;
                        break;
                    case 2:
                        specie = PieceModel.Species.DAMA;
                        break;
                    case 3:
                        specie = PieceModel.Species.PAWN;
                        break;
                    case 4:
                        specie = PieceModel.Species.KNIGHT;
                        break;
                    case 5:
                        specie = PieceModel.Species.BISHOP;
                        break;
                    case 6:
                        specie = PieceModel.Species.ROOK;
                        break;
                    case 7:
                        specie = PieceModel.Species.QUEEN;
                        break;
                    case 8:
                        specie = PieceModel.Species.KING;
                        break;
                }

                if (colore != null && specie != null)
                    mappaPerLaSit.put(gM.positions.get(i), (P) new PieceModel<>(specie, colore));
            }

            return new Situation<>(mappaPerLaSit, turnSit);
        }

        /** Questa oggetto è uguale a {@code x} se e solo se {@code x} è della stessa
         * classe e la situazione codificata è la stessa. Il test è effettuato senza
         * decodificare la situazione, altrimenti sarebbe troppo lento.
         * @param o  un oggetto
         * @return true se {@code x} rappresenta la stessa situazione di questo
         * oggetto */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EncS<?> encS = (EncS<?>) o;

            return Arrays.equals(codedSit, encS.codedSit);
        }

        /** Ridefinito coerentemente con la ridefinizione di {@link EncS#equals(Object)}.
         * @return l'hash code di questa situazione codificata */
        @Override
        public int hashCode() {
            return Arrays.hashCode(codedSit);
        }
    }

    /** Un oggetto per rappresentare il risultato del metodo
     * {@link Probe#nextSituations(boolean, Next, Function, Function, Set)}.
     * Chiamiamo grado di una situazione <i>s</i> il numero delle prossime situazioni
     * a cui si può arrivare dalla situazione <i>s</i>.
     * @param <S>  tipo della codifica delle situazioni */
    public static class NSResult<S> {
        /** Insieme delle prossime situazioni */
        public final Set<S> next;
        /** Statistiche: il minimo e il massimo grado delle situazioni di partenza
         * e la somma di tutti gradi */
        public final long min, max, sum;

        public NSResult(Set<S> nx, long mn, long mx, long s) {
            next = nx;
            min = mn;
            max = mx;
            sum = s;
        }
    }

    /** Ritorna l'insieme delle prossime situazioni dell'insieme di situazioni date.
     * Per ogni situazione nell'insieme {@code start} ottiene le prossime situazioni
     * tramite {@code nextF}, previa decodifica con {@code dec}, e le aggiunge
     * all'insieme che ritorna, previa codifica con {@code cod}. La computazione può
     * richiedere tempi lunghi per questo è sensibile all'interruzione del thread
     * in cui il metodo è invocato. Se il thread è interrotto, il metodo ritorna
     * immediatamente o quasi, sia che l'esecuzione è parallela o meno, e ritorna
     * null. Se qualche parametro è null o non sono coerenti (ad es. {@code dec} non
     * è il decodificatore del codificatore {@code end}), il comportamento è
     * indefinito.
     * @param parallel  se true il metodo cerca di sfruttare il parallelismo della
     *                  macchina
     * @param nextF  la funzione che ritorna le prossime situazioni di una situazione
     * @param dec  funzione che decodifica una situazione
     * @param enc  funzione che codifica una situazione
     * @param start  insieme delle situazioni di partenza
     * @param <P>  tipo del modello dei pezzi
     * @param <S>  tipo della codifica delle situazioni
     * @return l'insieme delle prossime situazioni dell'insieme di situazioni date o
     * null se l'esecuzione è interrotta. */
    public static <P,S> NSResult<S> nextSituations(boolean parallel, Next<P> nextF,
                                                   Function<S,Situation<P>> dec,
                                                   Function<Situation<P>,S> enc,
                                                   Set<S> start) {

        Set<S> insiemeNextSitCodificate = new HashSet<>();
        long minSit = 1_000_000, maxSit = 0, sommaSit = 0;

        if (parallel) {
            ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<Set<S>>> futures = new ArrayList<>();

            //Per ogni codifica dell'insieme;
            for (S codifica : start)
                futures.add(exec.submit(() -> nextSituationsMethod(nextF, dec, enc, codifica)));

            try {

                for (Future<Set<S>> f : futures) {
                    Set<S> nuovoInsieme = f.get();
                    long numDiSit = nuovoInsieme.size();

                    if (numDiSit < minSit) minSit = numDiSit;

                    if (numDiSit > maxSit) maxSit = numDiSit;

                    sommaSit += numDiSit;

                    insiemeNextSitCodificate.addAll(nuovoInsieme);

                }

            } catch (InterruptedException | ExecutionException e) {

                //Se sono giunto qui, allora c'è stata una interruzione, quindi 'tento' di bloccare ogni task;
                for (Future<Set<S>> f : futures)
                    f.cancel(true);

                exec.shutdown();
                return null;
            }

            exec.shutdown();
        }

        else {

            for (S codifica : start) {

                if (Thread.interrupted()) return null;

                Set<S> nuovoInsiemeSequential = nextSituationsMethod(nextF, dec, enc, codifica);
                long numDiSit = nuovoInsiemeSequential.size();

                if (numDiSit < minSit) minSit = numDiSit;

                if (numDiSit > maxSit) maxSit = numDiSit;

                sommaSit += numDiSit;

                insiemeNextSitCodificate.addAll(nuovoInsiemeSequential);
            }

        }

        return new NSResult<>(insiemeNextSitCodificate, minSit, maxSit, sommaSit);
    }

    public static <P,S> Set<S> nextSituationsMethod(Next<P> nextF,
                                                   Function<S,Situation<P>> dec,
                                                   Function<Situation<P>,S> enc,
                                                             S codifica) {

        Set<S> insiemeDaRitornare = new HashSet<>();

        //Decodifico la situazione;
        Situation<P> sit = dec.apply(codifica);

        //Ottengo la mappa (mossa -> Sit) tramite la funzione nextF;
        Map<Move<P>, Situation<P>> insiemeSit = nextF.get(sit);

        for (Map.Entry<Move<P>, Situation<P>> e : insiemeSit.entrySet())
            insiemeDaRitornare.add(enc.apply(e.getValue()));

        return insiemeDaRitornare;
    }

}
