import java.util.Random;
import java.util.concurrent.CyclicBarrier;


public class ParallelBody {
    Point[] v;
    Point[] f;
    double[] m;
    final int DT = 1;
    int time = 0;
    int finishTime;
    Body[] bodies;
    int n, numWorkers;
    static Tree root;
    static double THETA;

    final int X_GRID = 10;
    final int Y_GRID = 10;
    double XMIN = Integer.MAX_VALUE;
    double XMAX = Integer.MIN_VALUE;
    double YMIN = Integer.MAX_VALUE;
    double YMAX = Integer.MIN_VALUE;
    // final AtomicInteger XMIN = new AtomicInteger(Integer.MAX_VALUE);
    // final AtomicInteger XMAX = new AtomicInteger(Integer.MIN_VALUE);
    // final AtomicInteger YMIN = new AtomicInteger(Integer.MAX_VALUE);
    // final AtomicInteger YMAX = new AtomicInteger(Integer.MIN_VALUE);

    
    public static void main(String[] args) throws InterruptedException{
        int n = Integer.parseInt(args[0]);
        int steps = Integer.parseInt(args[1]);
        int theta = Integer.parseInt(args[2]);
        int numWorkers = Integer.parseInt(args[3]);
        new ParallelBody(n, steps, theta, numWorkers);
        
    } 
    
    public ParallelBody(int n, int steps, int theta, int workers){
        this.n = n;
        numWorkers = workers;
        THETA = ((double)theta)/10;
        v = new Point[n];
        //f = new Point[numWorkers][n];
        f = new Point[n];
        m = new double[n];
        bodies = new Body[n];

        Worker[] w = new Worker[numWorkers];
        finishTime = steps*DT;

        int chunk = n/numWorkers;
        int rest = n%numWorkers;

        final CyclicBarrier barrier = new CyclicBarrier(numWorkers);

        init(n, 0);
        
        for(int i = 0; i<numWorkers; i++){
            w[i] = new Worker(i, barrier, bodies, v, f, m, chunk, finishTime, rest, numWorkers, XMIN, XMAX, YMIN, YMAX);
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
    }

    public void setMinMax(double x, double y) {
        XMIN = Math.min(XMIN, x);
        XMAX = Math.max(XMAX, x);
        YMIN = Math.min(YMIN, y);
        YMAX = Math.max(YMAX, y);
        /*
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
        */
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



