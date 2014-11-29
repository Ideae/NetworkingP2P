package com.company;

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
}
