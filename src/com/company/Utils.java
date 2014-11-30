package com.company;

import java.util.Scanner;

/**
 * Created by zacktibia on 2014-11-26.
 */
public class Utils
{
    public static int Hash(String contentName)
    {
        int total = 0;
        for(char c : contentName.toCharArray())
        {
            total += (int)c;
        }
        return (total % 4) + 1;
    }

    static boolean YesOrNo(Scanner sc) {
        while (true) {
            String response = sc.nextLine();
            if (response.isEmpty() ||response.equalsIgnoreCase("Y") || response.equalsIgnoreCase("Yes"))
                return true;
            else if (response.equalsIgnoreCase("N") || response.equalsIgnoreCase("No"))
                return false;
            else
                System.out.println("Incorrect input, try again");
        }
    }
}
