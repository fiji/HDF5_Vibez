// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   HDF5_GroupedVarnames.java

import java.io.PrintStream;
import java.util.*;
import java.util.regex.*;

public class HDF5_GroupedVarnames
{

    public HDF5_GroupedVarnames()
    {
        formatTokens = null;
        formatString = null;
        minFrameIndex = -1;
        maxFrameIndex = -1;
        minChannelIndex = -1;
        maxChannelIndex = -1;
        nChannels = -1;
    }

    public static String[] parseFormatString(String s, String s1)
        throws PatternSyntaxException
    {
        String as[] = null;
        as = s.split("([$]T|[$]C)");
        boolean flag = s.contains("$T") && s.contains("$C");
        boolean flag1 = s.indexOf("$T") < s.indexOf("$C");
        for(int i = 0; i < as.length; i++)
            System.out.println((new StringBuilder()).append("tok ").append(Integer.toString(i)).append(" : ").append(as[i]).toString());

        if(as.length < 2 || !flag || !flag1)
        {
            throw new PatternSyntaxException("Your format string has errors. You must provide $T and $C and also in correct order!", s, -1);
        } else
        {
            String s2 = s;
            s2 = s2.replace("$T", s1);
            s2 = s2.replace("$C", s1);
            System.out.println(s2);
            Pattern pattern = null;
            pattern = Pattern.compile(s2);
            return as;
        }
    }

    public void parseVarNames(String as[], String s, String s1)
    {
        formatString = s;
        try
        {
            formatTokens = parseFormatString(s, s1);
        }
        catch(PatternSyntaxException patternsyntaxexception)
        {
            String s3 = patternsyntaxexception.getMessage();
            System.out.println(s3);
            return;
        }
        String s2 = s;
        s2 = s2.replace("$T", s1);
        s2 = s2.replace("$C", s1);
        System.out.println(s2);
        Pattern pattern = null;
        pattern = Pattern.compile(s2);
        for(int i = 0; i < as.length; i++)
        {
            Matcher matcher = pattern.matcher(as[i]);
            boolean flag = matcher.matches();
            if(flag)
            {
                System.out.println(as[i]);
                matchedVarNames.add(as[i]);
            } else
            {
                unMatchedVarNames.add(as[i]);
            }
        }

        splitGroupedVarnames();
        Object aobj[] = frameList.toArray();
        Arrays.sort(aobj);
        for(int j = 0; j < aobj.length; j++)
            frameList.set(j, (TimeFrame)aobj[j]);

    }

    public TimeFrame getFrame(int i)
    {
        if(i < frameList.size() && i > -1)
            return (TimeFrame)frameList.get(i);
        else
            return null;
    }

    private void splitGroupedVarnames()
    {
        for(Iterator iterator = matchedVarNames.iterator(); iterator.hasNext();)
        {
            String s = (String)iterator.next();
            String as[] = null;
            if(formatTokens.length == 2)
                as = s.split(formatTokens[1]);
            else
            if(formatTokens.length == 3)
            {
                as = s.split(formatTokens[2]);
                s = as[0];
                as = s.split(formatTokens[1]);
            }
            if(as.length < 2 || as.length > 3)
            {
                System.out.println("Error parsing varname!");
            } else
            {
                Integer integer = new Integer(as[1]);
                System.out.println((new StringBuilder()).append("channelIndex: ").append(integer.toString()).toString());
                System.out.println((new StringBuilder()).append("left token: ").append(as[0]).toString());
                as = as[0].split("/t");
                Integer integer1 = new Integer(as[1]);
                System.out.println((new StringBuilder()).append("frameIndex: ").append(integer1.toString()).toString());
                if(minFrameIndex == -1)
                    minFrameIndex = integer1.intValue();
                minFrameIndex = Math.min(minFrameIndex, integer1.intValue());
                if(maxFrameIndex == -1)
                    maxFrameIndex = integer1.intValue();
                maxFrameIndex = Math.max(maxFrameIndex, integer1.intValue());
                if(minChannelIndex == -1)
                    minChannelIndex = integer.intValue();
                minChannelIndex = Math.min(minChannelIndex, integer.intValue());
                if(maxChannelIndex == -1)
                    maxChannelIndex = integer.intValue();
                maxChannelIndex = Math.max(maxChannelIndex, integer.intValue());
                TimeFrame timeframe = new TimeFrame(integer1.intValue());
                int i = frameList.indexOf(timeframe);
                if(i != -1)
                {
                    timeframe = (TimeFrame)frameList.get(i);
                    timeframe.addChannel(integer.intValue());
                } else
                {
                    timeframe.addChannel(integer.intValue());
                    frameList.add(timeframe);
                }
            }
        }

    }

    public int getMinFrameIndex()
    {
        return minFrameIndex;
    }

    public int getMaxFrameIndex()
    {
        return maxFrameIndex;
    }

    public int getMinChannelIndex()
    {
        return minChannelIndex;
    }

    public int getMaxChannelIndex()
    {
        return maxChannelIndex;
    }

    public int getNFrames()
    {
        return frameList.size();
    }

    public int getNChannels()
    {
        if(nChannels == -1)
            return (maxChannelIndex - minChannelIndex) + 1;
        else
            return nChannels;
    }

    public boolean hasAllFramesInRange()
    {
        return frameList.size() == (maxFrameIndex - minFrameIndex) + 1;
    }

    public String toString()
    {
        String s = "Data set statistics\n";
        s = (new StringBuilder()).append(s).append("----------------------------------\n").toString();
        s = (new StringBuilder()).append(s).append("nFrames: ").append(Integer.toString(frameList.size())).append("\n").toString();
        s = (new StringBuilder()).append(s).append("minFrameIndex: ").append(Integer.toString(minFrameIndex)).append("\n").toString();
        s = (new StringBuilder()).append(s).append("maxFrameIndex: ").append(Integer.toString(maxFrameIndex)).append("\n").toString();
        s = (new StringBuilder()).append(s).append("hasAllFramesInRange: ").append(Boolean.toString(hasAllFramesInRange())).append("\n").toString();
        s = (new StringBuilder()).append(s).append("minChannelIndex: ").append(Integer.toString(minChannelIndex)).append("\n").toString();
        s = (new StringBuilder()).append(s).append("maxChannelIndex: ").append(Integer.toString(maxChannelIndex)).append("\n").toString();
        for(Iterator iterator = frameList.iterator(); iterator.hasNext();)
        {
            TimeFrame timeframe = (TimeFrame)iterator.next();
            s = (new StringBuilder()).append(s).append(timeframe.toString()).append("\n").toString();
        }

        s = (new StringBuilder()).append(s).append("----------------------------------").toString();
        return s;
    }

    public List getUnmatchedVarNames()
    {
        return unMatchedVarNames;
    }

    public String[] getFormatTokens()
    {
        return formatTokens;
    }

    public String getFormatString()
    {
        return formatString;
    }

    public void setFrameAndChannelRange(int i, int j, int k, int l, int i1, int j1)
    {
        System.out.println((new StringBuilder()).append("Setting frame range: ").append(Integer.toString(i)).append(":").append(Integer.toString(j)).append(":").append(Integer.toString(k)).toString());
        System.out.println((new StringBuilder()).append("Setting channel range: ").append(Integer.toString(l)).append(":").append(Integer.toString(i1)).append(":").append(Integer.toString(j1)).toString());
        if(hasAllFramesInRange())
        {
            ArrayList arraylist = new ArrayList(frameList);
            frameList.clear();
            for(int k1 = i; k1 < k + 1; k1 += j)
            {
                TimeFrame timeframe = (TimeFrame)arraylist.get(k1);
                TimeFrame timeframe2 = new TimeFrame(timeframe.getFrameIndex());
                for(int j2 = l; j2 < j1 + 1; j2 += i1)
                    timeframe2.addChannel(j2);

                frameList.add(timeframe2);
            }

            nChannels = (j1 - l) / i1 + 1;
            System.out.println((new StringBuilder()).append("Adding nChannels: ").append(Integer.toString(nChannels)).toString());
        } else
        {
            System.out.println("-------------------------\nhasAllFramesInRange==false\n-------------------------");
            ArrayList arraylist1 = new ArrayList(frameList);
            frameList.clear();
            for(int l1 = i; l1 < k + 1; l1 += j)
            {
                TimeFrame timeframe1 = new TimeFrame(l1);
                int i2 = arraylist1.indexOf(timeframe1);
                if(i2 != -1)
                {
                    for(int k2 = l; k2 < j1 + 1; k2 += i1)
                        timeframe1.addChannel(k2);

                    frameList.add(timeframe1);
                } else
                {
                    System.out.println((new StringBuilder()).append("Timestep ").append(Integer.toString(l1)).append(" is missing!").toString());
                }
            }

            nChannels = (j1 - l) / i1 + 1;
            System.out.println((new StringBuilder()).append("Adding nChannels: ").append(Integer.toString(nChannels)).toString());
        }
    }

    private final List matchedVarNames = new ArrayList();
    private final List unMatchedVarNames = new ArrayList();
    private final List frameList = new ArrayList();
    private String formatTokens[];
    private String formatString;
    private int minFrameIndex;
    private int maxFrameIndex;
    private int minChannelIndex;
    private int maxChannelIndex;
    private int nChannels;
}
