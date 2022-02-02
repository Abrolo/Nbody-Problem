

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Worker extends Thread{
    CyclicBarrier barrier;
    Long barrierTime = (long) 0.0;
    int tid;
    int chunk, finishTime, myFirst, myLast;
    Body[] bodies;
    Point[] v; 
    Point [] f;
    double[] m;
    Tree root;

    AtomicInteger XMIN, XMAX, YMIN, YMAX;
    int numWorkers;

    final int DT = 1;
    public Worker(int tid, CyclicBarrier barrier, Body[] bodies, Point[] v, Point[] f, double[] m, int chunk, 
    int finishTime, int rest, int numWorkers){
        this.barrier = barrier;
        this.tid = tid;
        this.chunk = chunk;
        this.bodies = bodies;
        this.v = v;
        this.f = f;
        this.m = m;
        this.finishTime = finishTime;
        this.numWorkers = numWorkers;
        myFirst = tid*chunk;
        myLast = myFirst + chunk -1;
        if(tid == numWorkers-1) myLast+=rest; //adds leftover bodies to the last thread
    }

    public void run(){
        runSims5();
    }

    public void runSims5(){

        for (int time = 0; time < finishTime; time += DT) {
            awaitBarrier();
            if (tid == 0) {
                ParallelBody.createTree();
            }
            awaitBarrier();
            calculateForces();
            awaitBarrier();
            moveBodies();
        }


    }

    //TODO: Divide work
    public void calculateForces(){
        for (int bodyID = myFirst; bodyID < myLast; bodyID++) {
            ParallelBody.root.calculateForce(bodies[bodyID], f[bodyID]);
        }
    }
    
    public void moveBodies() {
        Point deltav, deltap;
        for (int i = myFirst; i < myLast; i++) {


            deltav = new Point(f[i].x / m[i] * DT, f[i].y / m[i] * DT);
            deltap = new Point((v[i].x + deltav.x/2) * DT, 
                                    (v[i].y + deltav.y/2) * DT);
            v[i].x = v[i].x + deltav.x;
            v[i].y = v[i].y + deltav.y;
            bodies[i].x = bodies[i].x + deltap.x;
            bodies[i].y = bodies[i].y + deltap.y;
            f[i].x = 0;
            f[i].y = 0;
            //System.out.println("Body " + i + " moved (" + deltap.x + "," + deltap.y + ")");
        }
    }



    public void awaitBarrier() {
        long t1 = System.nanoTime();
        try {
            barrier.await();
        } catch (Exception e) {}
        barrierTime += System.nanoTime() - t1;
    }
}