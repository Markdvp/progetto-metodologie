package gapp.ulg.test_projgrader;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Un {@code ThreadsMonitor} monitora i tempi di esecuzione dei thread durante un
 * intervallo di tempo. Il monitoraggio inizia quando l'oggetto {@code ThreadsMonitor}
 * è creato e termina con l'invocazione del metodo {@link ThreadsMonitor#stop()} che
 * stampa anche i risultati del monitoraggio sul flusso di output specificato.
 * Esempio di uso:
 * <pre>
 * ThreadsMonitor monitor = new ThreadsMonitor(50, true, 5, System.out, s -> true);
 * ...
 * <i>codice che si vuole monitorare</i>
 * ...
 * monitor.stop();
 * </pre>
 * I risultati sono del seguente tipo
 * <pre>
 * Period 50ms  CPU time charts  Levels 5
 * main               ┤▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁╻▁▁▁▁▁▁▁▁▁▁▁▁╻▁▁▁▁▁╻▁▁▁╻▁▁╻▁▁╻╻▁
 *
 * Reference Handler  ┤▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁
 *
 * Finalizer          ┤▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁
 *
 * Signal Dispatcher  ┤▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁
 *
 * Monitor Ctrl-Break ┤▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁
 *
 * Thread-0           ┤╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻╻
 *
 *                    ┤           ┃
 *                    ┤  ┃┃┃┃ ┃┃  ┃┃ ┃  ┃┃┃┃
 *                    ┤ ┃┃┃┃┃┃┃┃┃┃┃┃┃┃ ┃┃┃┃┃
 *                    ┤ ┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃
 * pool-1-thread-1    ┤.┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁
 *
 *                    ┤                         ┃ ┃┃┃┃┃┃┃
 *                    ┤ ┃┃┃┃┃ ┃┃┃ ┃ ┃   ┃┃┃┃┃ ┃ ┃┃┃┃┃┃┃┃┃
 *                    ┤ ┃┃┃┃┃┃┃┃┃┃┃┃┃┃ ┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃
 *                    ┤ ┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃
 * pool-1-thread-2    ┤.┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁
 *
 *                    ┤                 ┃        ┃┃┃┃┃┃┃┃┃┃┃┃ ┃┃┃┃┃┃┃┃┃ ┃┃ ┃
 *                    ┤ ┃ ┃┃ ┃┃┃ ┃┃  ┃  ┃┃┃┃┃┃ ┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃
 *                    ┤ ┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃
 *                    ┤ ┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃
 * pool-1-thread-3    ┤.┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃┃
 *
 *                    ┤                      ┃┃ ┃┃┃┃┃┃ ┃┃
 *                    ┤                      ┃┃┃┃┃┃┃┃┃┃┃┃
 *                    ┤                      ┃┃┃┃┃┃┃┃┃┃┃┃
 *                    ┤                      ┃┃┃┃┃┃┃┃┃┃┃┃
 * T69449             ┤......................┃┃┃┃┃┃┃┃┃┃┃┃...................
 *
 * CPU time:     Total 14519ms  Percentage  61.8%
 * User time:    Total 14479ms  Percentage  61.7%
 * Machine time: Total 23484ms  Percentage 100.0%
 * Real time: 2936ms
 * </pre>
 * Il diagramma di un thread mostra i campionamenti del suo tempo di esecuzione,
 * nell'esempio ogni 50 ms. Ogni colonnina è proporzionale al rapporto tra il tempo
 * in cui il thread è stato effettivamente in esecuzione durante l'intervallino e la
 * durata dell'intervallino (nell'esempio 50 ms). Il valore di tali rapporti sono
 * mostrati in modo approssimativo con un numero fisso di livelli o tacche,
 * nell'esempio 5 livelli. Ad esempio una colonnina su 5 livelli con due tacche
 * <pre>
 *     ┤
 *     ┤
 *     ┤
 *     ┤    ┃
 *     ┤    ┃
 * </pre>
 * indica un rapporto approssimativamente uguale a 2/5. Il che significa che durante
 * l'intervallino il thread è stato in esecuzione per circa i 2/5 del tempo, cioè il
 * 40% del tempo. Se una colonnina riporta ╻ significa che il tempo di esecuzione è
 * diverso da zero ma inferiore a quello di una tacca. Se invece riporta ▁ significa
 * che il tempo di esecuzione nell'intervallino è zero. Infine se una colonnina
 * riporta . significa che il thread non è attivo, cioè o non è ancora iniziato o è
 * terminato.
 * <br>
 * Il tempo di esecuzione campionato può essere o il <i>tempo di CPU</i>
   (<i>CPU time</i>) o il <i>tempo utente</i> (<i>user time</i>) a seconda della
 * specifica al momento della creazione del {@code ThreadsMonitor}, nell'esempio
 * è il tempo di CPU. Il tempo di CPU è il tempo di esecuzione totale che comprende
 * il <i>tempo di sistema</i> (<i>sys time</i>) e il tempo utente. Il tempo di
 * sistema è il tempo in cui il thread è impegnato nell'esecuzione di codice nello
 * spazio di kernel (del sistema operativo), generalmente si tratta di esecuzione di
 * <i>chiamate di sistema</i> (<i>system calls</i>). Il tempo utente è invece il
 * tempo in cui il thread è impegnato nell'esecuzione di codice nello spazio utente,
 * cioè non di kernel.
 * <br>
 * Durante l'intervallo di monitoraggio sono campionati tutti i thread che risultano
 * attivi in almeno un periodo di monitoraggio. Però i dati riportati possono essere
 * ristretti ad un sottoinsieme di tali thread specificando un filtro al momento
 * della creazione.
 * <br>
 * I tempi riportati alla fine del report si riferiscono solamente ai thread
 * selezionati dal filtro:
 * <ul>
 *     <li><code>CPU time</code>: è il tempo di CPU totale di tutti i thread
 *     selezionati e la sua percentuale rispetto al Machine time (vedi dopo).</li>
 *     <li><code>User time</code>: è il tempo utente totale di tutti thread
 *     selezionati e la sua percentuale rispetto al Machine time.</li>
 *     <li><code>Machine time</code>: è il tempo totale di esecuzione disponibile
 *     sulla macchina durante l'intervallo di monitoraggio, cioè è la durata
 *     dell'intervallo di monitoraggio moltiplicato per il numero di processori
 *     disponibili.</li>
 *     <li><code>Real time</code>: è il tempo reale, cioè la durata dell'intervallo
 *     di monitoraggio.</li>
 * </ul> */
public class ThreadsMonitor {
    /** Crea un {@link ThreadsMonitor} che crea ed inizia un thread per monitorare i
     * thread campionando i tempi di esecuzione. Il monitoraggio termina invocando
     * il metodo {@link ThreadsMonitor#stop()}.
     * @param period  tempo in millisecondi del periodo di campionamento, cioè i
     *                tempi sono campionati ogni {@code period} millisecondi
     * @param cpu  se {@code true} riporta i tempi di CPU, altrimenti riporta i tempi
     *             utente
     * @param levels  numero livelli (o tacche) dei diagrammi
     * @param out  flusso in cui sono stampati i risultati del monitoraggio
     * @param filter  filtro che seleziona i thread in base al loro nome
     * @throws IllegalArgumentException se {@code period} < 1 o {@code levels} < 1
     * @throws NullPointerException se {@code out} o {@code filter} è {@code null}
     * @throws UnsupportedOperationException se la JVM non supporta la misurazione
     * del tempo di CPU dei thread */
    public ThreadsMonitor(int period, boolean cpu, int levels, PrintStream out,
                          Predicate<String> filter) {
        chkArg(period >= 1, "period must be >= 1");
        chkArg(levels >= 1, "levels must be >= 1");
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");
        if (!TSampler.THM.isThreadCpuTimeSupported())
            throw new UnsupportedOperationException("JVM doesn't support CPU time measurement");
        TSampler.THM.setThreadCpuTimeEnabled(true);
        monitor = new Thread(() -> {    // Crea il thread di monitoraggio o campionatura
            // Associa ad ogni id di un thread un oggetto TSampler per registrare i
            Map<Long, TSampler> samplers = new HashMap<>();    // campionamenti dei tempi
            int count = 0;    // Contatore dei campionamenti
            double realTime = System.nanoTime();    // Tempo di inizio della campionatura
            while (true) {    // Loop che termina non appena questo thread è interrotto
                    // Esamina i thread attualmente attivi per registrare quelli non
                for (long id : TSampler.THM.getAllThreadIds()) {  // ancora registrati
                    if (samplers.containsKey(id)) continue;       // Se è già registrato, continua
                    ThreadInfo tI = TSampler.THM.getThreadInfo(id);  // Se è nuovo, richiede le
                    if (tI == null) continue;    // info, fallisce se nel frattempo è terminato
                        // Lo aggiunge alla mappa dei thread registrati, creando un TSampler
                    samplers.put(id, new TSampler(tI.getThreadName(), id, count));
                }
                samplers.values().forEach(TSampler::sample);    // Campiona tutti i thread registrati
                count++;
                try {
                    Thread.sleep(period);    // Addormenta il thread fino al prossimo campionamento
                } catch (InterruptedException e) { break; }  // Se interrotto, termina il campionamento
            }
                // Stampa i risultati della campionatura
            print(samplers, System.nanoTime()-realTime, period, cpu, levels, out, filter);
        });
        monitor.setDaemon(true);   // Impostato come daemon thread non pone ostacoli alla
        monitor.start();           // terminazione del programma. Inizia l'esecuzione
    }

    /** Termina il monitoraggio iniziato con la creazione di questo
     * {@code ThreadsMonitor} e stampa sul flusso specificato (nel costruttore
     * {@link ThreadsMonitor#ThreadsMonitor(int, boolean, int, PrintStream, Predicate)})
     * i risultati.
     * @throws IllegalStateException se è invocato quando il monitoraggio è già
     * terminato tramite una precedente invocazione di questo metodo */
    public synchronized void stop() {    // È sincronizzato per evitare che sia eseguito
        if (monitor.getState() == Thread.State.TERMINATED)    // da più thread simultaneamente
            throw new IllegalStateException("Monitor is already terminated");
        monitor.interrupt();  // Interrompe il thread di monitoraggio
        try {
            monitor.join();   // Aspetta che sia terminato
        } catch (InterruptedException e) {}
    }


    /** Un {@code TSampler} registra i campionamenti dei tempi di esecuzione di un
     * thread. */
    private static class TSampler {
        /** L'oggetto {@link ThreadMXBean} fornisce info circa i thread attivi e le
         * loro esecuzioni */
        static final ThreadMXBean THM = ManagementFactory.getThreadMXBean();

        /** Nome del thread */
        final String name;

        /** Crea un {@code TSampler} per registrare i tempi di esecuzione del thread
         * con nome e id dati e con un ritardo specificato rispetto all'inizio del
         * monitoraggio.
         * @param nm  nome del thread
         * @param id  id del thread
         * @param d  ritardo rispetto all'inizio del monitoraggio, cioè numero di
         *           campionature avvenute prima della registrazione del thread */
        TSampler(String nm, long id, long d) {
            name = nm;
            this.id = id;
            delay = d;
        }

        /** Effettua un campionamento dei tempi di esecuzione del thread */
        void sample() {
            long time = System.nanoTime();
            long cpu = THM.getThreadCpuTime(id), user = THM.getThreadUserTime(id);
            samples.add(new long[]{time, cpu, user});
        }

        /** Ritorna in una stringa il diagramma dei tempi campionati.
         * @param cpu  se {@code true} riporta i tempi di CPU, altrimenti riporta i tempi
         *             utente
         * @param levels  numero livelli (o tacche) del diagramma
         * @param width  lunghezza della giustificazione del nome del thread
         * @return una stringa con il diagramma dei tempi campionati */
        String report(boolean cpu, int levels, int width) {
            List<Double> tLst = new ArrayList<>();   // Lista dei valori da riportare nel diagramma
            double pt = -1, pcu = -1, max = 0;
            for (long[] tt : samples) {         // Scorre tutti i campionamenti
                double cu = tt[cpu ? 1 : 2];    // Il tempo da riportare (CPU o utente)
                boolean terminated = cu < 0;    // Se il thread è terminato, il tempo è negativo
                if (cu < 0) cu = pcu >= 0 ? pcu : 0;
                if (pcu >= 0) {
                    double t = terminated ? -1 : (cu - pcu)/(tt[0] - pt);  // Calcola il rapporto
                    tLst.add(t);
                    if (t > max) max = t;    // Per calcolare il numero massimo di tacche
                }                            // necessarie per il diagramma
                pcu = cu;
                pt = tt[0];
            }
            Function<String,String> dy = c -> Stream.generate(() -> c).limit(delay)
                    .collect(Collectors.joining());
            String s = "";
            for (int i = 0; i < levels; i++) {
                double lev = (levels - i - 0.5)/levels;
                if (i < levels-1 && max < lev) continue;
                s += String.format("%-" + (width + 1) + "s\u2524%s", (i < levels-1 ? "" : name),
                        dy.apply(i < levels-1 ? " " : "."));
                for (double t : tLst)
                    s += t >= lev ? "\u2503" : (i < levels-1 ? " " :
                            (t > 0 ? "\u257b" : (t < 0 ? "." : "\u2581")));
                s += "\n";
            }
            return s;
        }

        /** @return il tempo di CPU totale */
        long totalCPU() { return total(1); }

        /** @return il tempo utente totale */
        long totalUser() { return total(2); }

        /** Ritorna il tempo totale, di CPU o utente a seconda dell'indice i.
         * @param i  se == 1 tempo di CPU, se == 2 tempo utente
         * @return il tempo totale, di CPU o utente */
        private long total(int i) {
            long min = -1, max = -1;
            for (long[] s : samples)
                if (s[i] >= 0) {
                    if (min == -1 || s[i] < min) min = s[i];
                    if (max == -1 || s[i] > max) max = s[i];
                }
            return min != -1 ? max - min : 0;
        }

        private final long id, delay;    // Id del thread e ritardo della registrazione.
            // Lista dei campionamenti del thread, ogni elemento è un array di tre
            // long che mantiene i tempi campionati in nanosecondi, il primo è il
            // tempo reale del campionamento, il secondo è il tempo di CPU e il
            // terzo è il tempo utente.
        private final List<long[]> samples = new ArrayList<>();
    }

    private static void chkArg(boolean cond, String msg) {
        if (!cond)
            throw new IllegalArgumentException(msg);
    }

    /** Stampa sul flusso specificato i risultati del monitoraggio.
     * @param samplers  mappa dei thread e delle relative campionature
     * @param realTime  tempo reale dell'intervallo di monitoraggio
     * @param period  priodo delle campionature
     * @param cpu  {@code true} se tempi di CPU, altrimenti tempi utente
     * @param levels  numero livelli dei diagrammi
     * @param out  flusso in cui stampare i risultati
     * @param filter  filtro che seleziona i thread da riportare */
    private static void print(Map<Long, TSampler> samplers, double realTime, int period,
                              boolean cpu, int levels, PrintStream out, Predicate<String> filter) {
        Predicate<TSampler> sF = r -> filter.test(r.name);
        int width = samplers.values().stream().filter(sF).map(r -> r.name.length())
                .max(Comparator.naturalOrder()).orElse(0);
        String s = String.format("Period %dms  %s time charts  Levels %d\n",
                period, (cpu ? "CPU" : "User"), levels);
        for (TSampler ts : samplers.values()) {
            if (!filter.test(ts.name)) continue;
            s += ts.report(cpu, levels, width) + "\n";
        }
        double machineTime = realTime * Runtime.getRuntime().availableProcessors();
        BiFunction<String, ToLongFunction<TSampler>, String> total = (nm, t) -> {
            long totalT = samplers.values().stream().filter(sF).mapToLong(t)
                    .reduce(Long::sum).orElse(0);
            return String.format(nm + "Total %dms  Percentage %5.1f%%\n",
                    totalT/1_000_000, 100.0*(totalT/machineTime));
        };
        s += total.apply("CPU time:     ", TSampler::totalCPU);
        s += total.apply("User time:    ", TSampler::totalUser);
        s += String.format("Machine time: Total %dms  Percentage %5.1f%%\n",
                Math.round(machineTime/1_000_000), 100.0);
        s += String.format("Real time: %dms\n", Math.round(realTime/1_000_000));
        out.println(s);
    }

    private final Thread monitor;
}
