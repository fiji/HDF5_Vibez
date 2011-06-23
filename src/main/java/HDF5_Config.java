// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   HDF5_Config.java

import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.io.PrintStream;
import java.util.regex.PatternSyntaxException;

public class HDF5_Config
    implements PlugIn
{

    public HDF5_Config()
    {
    }

    public void run(String s)
    {
        setDefaultsIfNoValueExists();
        boolean flag = Boolean.getBoolean(getDefaultValue("HDF5.groupVarsByName"));
        flag = Prefs.get("HDF5.groupVarsByName", flag);
        boolean flag1 = Boolean.getBoolean(getDefaultValue("HDF5.showUnmatchedDataSetNames"));
        flag1 = Prefs.get("HDF5.showUnmatchedDataSetNames", flag1);
        String s1 = getDefaultValue("HDF5.groupVarsByNameFormatGroup");
        s1 = Prefs.get("HDF5.groupVarsByNameFormatGroup", s1);
        String s2 = getDefaultValue("HDF5.groupVarsByNameFormat");
        s2 = Prefs.get("HDF5.groupVarsByNameFormat", s2);
        String s3 = getDefaultValue("HDF5.dollarRegexpForGrouping");
        s3 = Prefs.get("HDF5.dollarRegexpForGrouping", s3);
        GenericDialog genericdialog = new GenericDialog("HDF5 Preferences");
        genericdialog.addMessage("Reader:");
        genericdialog.addCheckbox("Group data set names instead of showing a list of data set names.", flag);
        genericdialog.addCheckbox("Show unmatched data set names in a separate list", flag1);
        genericdialog.addStringField("HDF5 group containing pattern for data set grouping: ", s1, 15);
        genericdialog.addStringField("Pattern for grouping (if no attributes are found): ", s2, 15);
        genericdialog.addMessage("Writer:");
        String s4 = "Save";
        String s5 = "Reset";
        genericdialog.enableYesNoCancel(s4, s5);
        genericdialog.showDialog();
        if(genericdialog.wasCanceled())
            return;
        if(!genericdialog.wasOKed())
        {
            System.out.println("reset button was pressed");
            genericdialog.setVisible(false);
            run(s);
            return;
        }
        flag = genericdialog.getNextBoolean();
        System.out.println((new StringBuilder()).append("groupVarsByName: ").append(Boolean.toString(flag)).toString());
        flag1 = genericdialog.getNextBoolean();
        System.out.println((new StringBuilder()).append("showUnmatchedDataSetNames: ").append(Boolean.toString(flag1)).toString());
        s1 = genericdialog.getNextString();
        System.out.println((new StringBuilder()).append("groupVarsByNameFormatGroup: ").append(s1).toString());
        s2 = genericdialog.getNextString();
        System.out.println((new StringBuilder()).append("groupVarsByNameFormat: ").append(s2).toString());
        try
        {
            String as[] = HDF5_GroupedVarnames.parseFormatString(s2, s3);
            for(int i = 0; i < as.length; i++)
                System.out.println((new StringBuilder()).append("tok ").append(Integer.toString(i)).append(" : ").append(as[i]).toString());

        }
        catch(PatternSyntaxException patternsyntaxexception)
        {
            String s6 = patternsyntaxexception.getMessage();
            System.out.println(s6);
            genericdialog.setVisible(false);
            run(s);
            return;
        }
        System.out.println("Saving...");
        Prefs.set("HDF5.groupVarsByName", flag);
        Prefs.set("HDF5.showUnmatchedDataSetNames", flag1);
        Prefs.set("HDF5.groupVarsByNameFormatGroup", s1);
        Prefs.set("HDF5.groupVarsByNameFormat", s2);
        Prefs.set("HDF5.dollarRegexpForGrouping", s3);
    }

    public static void setDefaultsIfNoValueExists()
    {
        boolean flag = Boolean.getBoolean(getDefaultValue("HDF5.groupVarsByName"));
        flag = Prefs.get("HDF5.groupVarsByName", flag);
        Prefs.set("HDF5.groupVarsByName", flag);
        boolean flag1 = Boolean.getBoolean(getDefaultValue("HDF5.showUnmatchedDataSetNames"));
        flag1 = Prefs.get("HDF5.showUnmatchedDataSetNames", flag1);
        Prefs.set("HDF5.showUnmatchedDataSetNames", flag1);
        String s = getDefaultValue("HDF5.groupVarsByNameFormatGroup");
        s = Prefs.get("HDF5.groupVarsByNameFormatGroup", s);
        Prefs.set("HDF5.groupVarsByNameFormatGroup", s);
        String s1 = getDefaultValue("HDF5.groupVarsByNameFormat");
        s1 = Prefs.get("HDF5.groupVarsByNameFormat", s1);
        Prefs.set("HDF5.groupVarsByNameFormat", s1);
        String s2 = getDefaultValue("HDF5.dollarRegexpForGrouping");
        s2 = Prefs.get("HDF5.dollarRegexpForGrouping", s2);
        Prefs.set("HDF5.dollarRegexpForGrouping", s2);
    }

    public static String getDefaultValue(String s)
    {
        if(s.equals("HDF5.groupVarsByName"))
        {
            boolean flag = true;
            return Boolean.toString(flag);
        }
        if(s.equals("HDF5.showUnmatchedDataSetNames"))
        {
            boolean flag1 = true;
            return Boolean.toString(flag1);
        }
        if(s.equals("HDF5.groupVarsByNameFormatGroup"))
        {
            String s1 = "/hints";
            return s1;
        }
        if(s.equals("HDF5.groupVarsByNameFormat"))
        {
            String s2 = "/t$T/channel$C";
            return s2;
        }
        if(s.equals("HDF5.dollarRegexpForGrouping"))
        {
            String s3 = "[0-9]+";
            return s3;
        } else
        {
            System.out.println((new StringBuilder()).append("No default value for key: ").append(s).toString());
            return null;
        }
    }
}
