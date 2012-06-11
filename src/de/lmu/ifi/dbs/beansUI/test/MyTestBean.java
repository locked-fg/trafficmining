package de.lmu.ifi.dbs.beansUI.test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyTestBean {

    private boolean myBool;
    private double myDouble;
    private String myString;
    private int myInt;
    private Foo myFoo = Foo.C;
    private List<String> myListOptions = new ArrayList();
    final static List<String> stringOptions = Collections.unmodifiableList(
            new ArrayList() {

                {
                    add("opt1");
                    add("opt2");
                }
            });

    public enum Foo {

        A, B, C
    };
    private Color color = Color.red;

    public MyTestBean() {
    }

    public List<String> getMyListOptions() {
        return myListOptions;
    }

    public void setMyListOptions(List<String> myListOptions) {
        String s = "";
        for (String ss : myListOptions) {
            s += ss+"|";
        }
        System.out.println("set: " + s);
        this.myListOptions = myListOptions;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        System.out.println("set " + c);
        this.color = c;
    }

    public int getMyInt() {
        return myInt;
    }

    public void setMyInt(int myInt) {
        System.out.println("set " + myInt);
        this.myInt = myInt;
    }

    public String getMyString() {
        return myString;
    }

    public void setMyString(String string) {
        System.out.println("set " + string);
        this.myString = string;
    }

    public boolean isMyBool() {
        return myBool;
    }

    public void setMyBool(boolean myBool) {
        System.out.println("set " + myBool);
        this.myBool = myBool;
    }

    public double getMyDouble() {
        return myDouble;
    }

    public void setMyDouble(double myDouble) {
        System.out.println("set " + myDouble);
        this.myDouble = myDouble;
    }

    public Foo getMyFoo() {
        return myFoo;
    }

    public void setMyFoo(Foo myFoo) {
        System.out.println(myFoo);
        this.myFoo = myFoo;
    }
}
