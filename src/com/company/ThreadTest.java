package com.company;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class ThreadTest
{
    static int aCount = 0, bCount = 0;
    public static void mainTest(String[] args)
    {
        Runnable a = new Runnable() {
            @Override
            public void run()
            {
                while(true)
                {
                    aCount++;
                    if (aCount % 100 == 0)
                        System.out.println("a:"+aCount);
                }
            }
        };
        Runnable b = new Runnable() {
            @Override
            public void run()
            {
                while(true)
                {
                    bCount++;
                    if (bCount % 100 == 0)
                        System.out.println("b:"+bCount);
                }
            }
        };
        Thread aThread = new Thread(a);
        Thread bThread = new Thread(b);
        aThread.start();
        bThread.start();
    }
}
