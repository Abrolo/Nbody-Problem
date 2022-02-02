import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class N2Parallel{
    
    static final double G = 6.67e-11;
    final int COLUMNS = 10;
    final int ROWS = 10;
    Point[] p;
    Point[] v;
    Point[][] f;
    double[] m;
    double time;
    double start = 0.0;
    double finish;
    double DT;
    int moves = 0;
    int interactions = 0;
    

    public N2Parallel(int n, int steps, int numWorkers){
        Worker w;
        ReentrantLock lock = new ReentrantLock();
        Condition cond = lock.newCondition();

        int stripSize = n/numWorkers;
        int rest = n % numWorkers;
        p = new Point[n];
        v = new Point[n];
        f = new Point[numWorkers][n];
        m = new double[n];
        Worker[] workers = new Worker[numWorkers];
        DT = 1;
        finish = steps*DT;

        init(n, numWorkers);
        long t1 = System.nanoTime();
        int first = 0;

        AtomicInteger sharedi = new AtomicInteger(0);
        AtomicInteger waits = new AtomicInteger(numWorkers);
        
        final CyclicBarrier barrier = new CyclicBarrier(numWorkers, 
            new Runnable() {
                public void run() {
                    sharedi.set(0);
                }
            }
        );
        
        //CyclicBarrier barrier = new CyclicBarrier(numWorkers);
        for(int i = 0; i<numWorkers; i++) {
            int last = first + stripSize;
            if (rest-- > 0) {
                last++;
            }
            w = new Worker(p, v, f, m, steps, n, i, first, last, numWorkers, barrier, sharedi);
            first = last;
            w.start();
            workers[i] = w;
        }
        
        for(int i = 0; i<numWorkers; i++){
            try {
                workers[i].join();
            } catch (Exception javasux) {}
        }        
        long t2 = System.nanoTime();
        System.out.println("Completed in " + (t2 - t1) / 1E6 + "ms");


    }

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        // CyclicBarrier b = new CyclicBarrier(1);
        // long t1 = System.nanoTime();
        // for (int i=0; i < 10_000_000; i++) {
        //     b.await();
        // }
        // long t2 = System.nanoTime();
        // System.out.println("Completed in " + (t2 - t1) / 1E6 + "ms");
        // if (true) {return;}
        int n = Integer.parseInt(args[0]);
        int steps = Integer.parseInt(args[1]);
        int numWorkers = Integer.parseInt(args[2]);
        new N2Parallel(n, steps, numWorkers);
    }


    public void init(int n, int numWorkers){
        Random rand = new Random();
        double x, y;
        for(int i = 0; i<n; i++){
            //randomly spaces out bodies in a grid
            x = rand.nextDouble() * COLUMNS;
            y = rand.nextDouble() * ROWS;
            p[i] = new Point(x, y, i);

            x = rand.nextDouble()*5;
            y = rand.nextDouble()*5;
            v[i] = new Point(0, 0);

            for (int numWorker=0; numWorker < numWorkers; numWorker++) {
                x = rand.nextDouble();
                y = rand.nextDouble();
                f[numWorker][i] = new Point(0, 0);
            }

            m[i] = rand.nextDouble()*10e+6;

        }
    }

    //double G = 6.67e-11;
    /*
    initialize the positions, velocities, forces, and masses;
    # calculate total force for every pair of bodies
    */



}

class Worker extends Thread{
    static final double G = 6.67e-11;
    Point[] p;
    Point[] v;
    Point[][] f;
    double[] m;
    int n, steps;
    int DT = 1;
    int startTime, time, finishTime, tid, numWorkers, myFirst, myLast;
    int actionsCalc = 0;
    int actionsMove = 0;
    int loops = 0;
    int barriers = 0;
    final AtomicInteger sharedi;
    final CyclicBarrier barrier;
    long barrierTime = 0;

    public Worker(Point[] p, Point[] v, Point[][] f, double[] m, int steps, int n, int tid, int myFirst, int myLast, int numWorkers, CyclicBarrier barrier, AtomicInteger sharedi){
        this.p= p;
        this.v = v;
        this.f= f;
        this.m= m;
        this.n = n;
        startTime = 0;
        finishTime = DT*steps;
        this.tid = tid;
        this.sharedi = sharedi;
        this.barrier = barrier;
        this.myFirst = myFirst;
        this.myLast = myLast;
        this.numWorkers = numWorkers;
        //System.out.println("Thread " + tid + " started. Range " + myFirst + "-" + myLast + " finishTime: " + finishTime + ", DT: " + DT);
    }

    public void calculateForces() {
        double distance, magnitude; 
        Point direction = new Point(0, 0);
        int i;
        int goal = n-1;
        long t1 = System.nanoTime();
        while ((i = sharedi.getAndIncrement()) < goal) {
            for (int j = i+1; j < n; j++) {
                actionsCalc++;
                distance = Math.sqrt(Math.pow((p[i].x - p[j].x),2) +
                Math.pow((p[i].y - p[j].y),2));
                distance = Math.max(distance, 0.1);
                magnitude = (G*m[i]*m[j]) / Math.pow(distance,2);
                direction.x = p[j].x-p[i].x;
                direction.y = p[j].y-p[i].y;
                // direction = new Point(p[j].x-p[i].x, p[j].y-p[i].y);
                f[tid][i].add(magnitude*direction.x/distance, magnitude*direction.y/distance); // This is supposed to be synchronized, but disabled for testing purposes
                f[tid][j].add(-magnitude*direction.x/distance, -magnitude*direction.y/distance);
                // f[i].x = f[i].x + magnitude*direction.x/distance;
                // f[j].x = f[j].x - magnitude*direction.x/distance;
                // f[i].y = f[i].y + magnitude*direction.y/distance;
                // f[j].y = f[j].y - magnitude*direction.y/distance;

            }
        }
        long delta = System.nanoTime() - t1;
        if (delta > 100000) {
            //System.out.println("[" + tid + "]: " + delta);
        }
    }

    public void moveBodies() {
        Point deltav;
        Point deltap;
        Point force = new Point(0, 0);
        for (int i = myFirst; i < myLast; i++) {
            actionsMove++;
            //System.out.println("Body " + p[i].id + ": x: " + p[i].x + " y: " + p[i].y);
            for(int k = 0; k<numWorkers; k++){
                force.x += f[k][i].x;
                force.y += f[k][i].y;
                f[k][i].x = f[k][i].y = 0;
            }

            deltav = new Point(force.x / m[i] * DT, force.y / m[i] * DT);
            deltap = new Point((v[i].x + deltav.x/2) * DT, 
                                    (v[i].y + deltav.y/2) * DT);
            //System.out.println("Body " + i + " moved (" + deltap.x + "," + deltap.y + ")");
            v[i].x = v[i].x + deltav.x;
            v[i].y = v[i].y + deltav.y;
            p[i].x = p[i].x + deltap.x;
            p[i].y = p[i].y + deltap.y;

            force.x = force.y = 0.0;
            //System.out.println("deltap.x: " + deltap.x + " deltap.y: " + deltap.y);
            //System.out.println("force.x: " + force.x + " force.y: " + force.y);
            //System.out.println("[" + tid + "]: Body " + p[i].id + ": x: " + p[i].x + " y: " + p[i].y);
        }
    }

    public void run(){
        // Run the simulation with time steps of DT
        for (time = startTime; time < finishTime; time += DT) {
            loops++;
            awaitBarrier(); // Must be first in loop.
            calculateForces();
            //System.out.println("thread: " + tid + " completed calculations");
            awaitBarrier();

            moveBodies();
        }
        //System.out.println("[" + tid + "]: Finished. Calcs: " + actionsCalc + ", Moves: " + actionsMove + ", Loops: " + loops + ", Barriers: " + barriers + ", BarrierTime: " + barrierTime / 1E6 + "ms.");
    }

    public void awaitBarrier() {
        long t1 = System.nanoTime();
        barriers++;
        try {
            barrier.await();
        } catch (Exception e) {}
        barrierTime += System.nanoTime() - t1;
    }
    
}
