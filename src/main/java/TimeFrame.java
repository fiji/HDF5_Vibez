// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   TimeFrame.java

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class TimeFrame
    implements Comparable
{

    public TimeFrame(int i)
    {
        channels = new ArrayList();
        frameIndex = i;
    }

    public TimeFrame(String s)
    {
        channels = new ArrayList();
        frameIndex = Integer.parseInt(s);
    }

    public void addChannel(int i)
    {
        Integer integer = new Integer(i);
        if(!channels.contains(integer))
            channels.add(new Integer(i));
        else
            System.out.println((new StringBuilder()).append("channel").append(integer.toString()).append(" already in list!").toString());
    }

    public void addChannel(String s)
    {
        addChannel(Integer.parseInt(s));
    }

    public boolean equals(Object obj)
    {
        TimeFrame timeframe = (TimeFrame)obj;
        return timeframe.frameIndex == frameIndex;
    }

    public String toString()
    {
        String s = (new StringBuilder()).append("FrameIdx: ").append(Integer.toString(frameIndex)).append("; ").toString();
        s = (new StringBuilder()).append(s).append("nChannels: ").append(Integer.toString(channels.size())).append("; ").toString();
        s = (new StringBuilder()).append(s).append("channels: ").toString();
        for(int i = 0; i < channels.size(); i++)
            s = (new StringBuilder()).append(s).append(Integer.toString(((Integer)channels.get(i)).intValue())).append(";").toString();

        return s;
    }

    public int getNChannels()
    {
        return channels.size();
    }

    public int getFrameIndex()
    {
        return frameIndex;
    }

    public int[] getChannelIndices()
    {
        Object aobj[] = channels.toArray();
        Arrays.sort(aobj);
        int ai[] = new int[aobj.length];
        for(int i = 0; i < aobj.length; i++)
            ai[i] = ((Integer)aobj[i]).intValue();

        return ai;
    }

    public int compareTo(Object obj)
    {
        TimeFrame timeframe = (TimeFrame)obj;
        if(frameIndex < timeframe.frameIndex)
            return -1;
        return frameIndex <= timeframe.frameIndex ? 0 : 1;
    }

    private final int frameIndex;
    private final ArrayList channels;
}
