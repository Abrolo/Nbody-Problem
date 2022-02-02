import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class GatedExecutor {
    private ReentrantLock lock = new ReentrantLock();
    private Condition allIdle = lock.newCondition();
    private Condition notEmpty = lock.newCondition();
    private ArrayBlockingQueue<Runnable> tasks = new ArrayBlockingQueue<>(10000, true);
    private int idleCount = 0;
    private GatedWorker[] workers;
    private int numWorkers;
    private AtomicInteger stage = new AtomicInteger(0);
    private int tasksDone = 0;

    public GatedExecutor(int numWorkers) {
        workers = new GatedWorker[numWorkers];
        this.numWorkers = numWorkers;
        for(int i = 0; i<numWorkers; i++){
            workers[i] = new GatedWorker(this);
            workers[i].start();
        }
    }

    public int nextStage() {
        return stage.incrementAndGet();
    }

    public void join(int stage) {
        lock.lock();
        while (true) {
            try{allIdle.await();}catch (Exception e){}
            if (isIdle() || this.stage.get() > stage) {
                break;
            }
        }
        lock.unlock();
    }

    public boolean isIdle() {
        return (idleCount == numWorkers && tasks.isEmpty());
    }

    public void addTask(Runnable task) {
        //System.out.println("addLocking");
        lock.lock();
        // System.out.println("addLock acquired");
        tasks.add(task);
        notEmpty.signalAll();
        lock.unlock();
    }

    private Runnable getTask() {
        if (++idleCount == numWorkers && tasks.isEmpty()) {
            stage.incrementAndGet();
            //System.out.println("We are idle.");
            allIdle.signalAll();
            //System.out.println("Signalled idle.");
        }
        // System.out.println(String.format("idleCount: %d, numWorkers: %d, isEmpty: " + tasks.isEmpty(), idleCount, numWorkers));
        try {
            while (tasks.isEmpty()) {
                notEmpty.await();
            }
            Runnable task = tasks.take();
            // System.out.println("Took a task.");
            idleCount--;
            return task;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public void assist(Boolean blocking) {
        //System.out.println("Assist waiting for lock.");
        lock.lock();
        if (!blocking && tasks.isEmpty()) {
            lock.unlock();
            return;
        }
        //System.out.println("Getting a task!");
        Runnable task = getTask();
        //System.out.println("Got a task!");
        lock.unlock();
        task.run();
        //System.out.println(tasksDone++);
        
    }

}

class GatedWorker extends Thread {
    GatedExecutor executor;

    public GatedWorker(GatedExecutor executor) {
        this.executor = executor;
    }

    public void run() {
        while (true) {
            executor.assist(true);
        }

    }
}