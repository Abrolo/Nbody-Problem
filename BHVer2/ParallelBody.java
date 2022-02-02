

import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;


public class ParallelBody {
    Point[] v;
    Point[] f;
    double[] m;
    final int DT = 1;
    int time = 0;
    int finishTime;
    static Body[] bodies;
    int n, numWorkers;
    static Tree root;
    static double THETA;

    final int X_GRID = 100;
    final int Y_GRID = 100;

    final static AtomicInteger XMIN = new AtomicInteger(Integer.MAX_VALUE);
    final static AtomicInteger XMAX = new AtomicInteger(Integer.MIN_VALUE);
    final static AtomicInteger YMIN = new AtomicInteger(Integer.MAX_VALUE);
    final static AtomicInteger YMAX = new AtomicInteger(Integer.MIN_VALUE);

    static GatedExecutor newExecutor;
    
    public static void main(String[] args) throws InterruptedException{
        int n = Integer.parseInt(args[0]);
        int steps = Integer.parseInt(args[1]);
        int th = Integer.parseInt(args[2]);
        int numWorkers = Integer.parseInt(args[3]);

        new ParallelBody(n, steps, th, numWorkers);
        
    } 
    
    public ParallelBody(int n, int steps, int th, int workers){
        this.n = n;
        THETA = ((double) th)/10;
        numWorkers = workers;
        v = new Point[n];
        //f = new Point[numWorkers][n];
        f = new Point[n];
        m = new double[n];
        bodies = new Body[n];

        newExecutor = new GatedExecutor(numWorkers);

        Worker[] w = new Worker[numWorkers];
        finishTime = steps*DT;

        int chunk = n/numWorkers;
        int rest = n%numWorkers;

        final CyclicBarrier barrier = new CyclicBarrier(numWorkers);

        init(n, 0);
        
        for(int i = 0; i<numWorkers; i++){
            w[i] = new Worker(i, barrier, bodies, v, f, m, chunk, finishTime, rest, numWorkers);
            w[i].start();
        }

        long t1 = System.nanoTime();
        for(int i = 0; i<numWorkers; i++){
            try {
                w[i].join();
            } catch (InterruptedException e) {               
                 e.printStackTrace();
            }
        }
        long t2 = System.nanoTime();
        System.out.println("Completed in " + (t2 - t1) / 1E6 + "ms");
        System.exit(0);
    }

    public static FutureTask<Void> getNewTreeTask(Tree tree, Body[] treeBodies) {
        Runnable task = ()-> {
            Body[] childrenInBox = Arrays.stream(treeBodies).filter(child -> tree.isInBox(child)).toArray(Body[]::new);
            tree.assignChildren(childrenInBox);
        };
        //System.out.println("Created newTreeTask");
        return new FutureTask<Void>(task, null);
    }

    
    public static void createTree() {
        //idleTreeWorkers.set(0);
        root = new Tree(new Point(XMIN.get(), YMAX.get()), new Point(XMAX.get(), YMIN.get()));
        var stage = newExecutor.nextStage();
        newExecutor.addTask(getNewTreeTask(root, bodies));
        newExecutor.join(stage);
        //treeTasks.add(new TreeTask(root, bodies));
    }

   public void setMinMax(double x, double y) {
        int xmin = (int) Math.floor(x);
        int xmax = (int) Math.ceil(x);
        int ymin = (int) Math.floor(y);
        int ymax = (int) Math.ceil(y);
        //if (xmin < XMIN.get())
            XMIN.accumulateAndGet(xmin, Math::min);
        //if (xmax > XMAX.get())
            XMAX.accumulateAndGet(xmax, Math::max);
        //if (ymin < YMIN.get())
            YMIN.accumulateAndGet(ymin, Math::min);
        //if (ymax > YMAX.get())
            YMAX.accumulateAndGet(ymax, Math::max);		  
    }
    
    public void init(int n, int numWorkers){
        Random rand = new Random();
        double x, y;
        for(int i = 0; i<n; i++){
            //randomly spaces out bodies in a grid
            x = rand.nextDouble() * X_GRID;
            y = rand.nextDouble() * Y_GRID;
            setMinMax(x, y);
            bodies[i] = new Body(x, y, i);

            v[i] = new Point(0, 0);

            f[i] = new Point(0,0);

            m[i] = rand.nextDouble()*10e+6;

        }
    }   
}



