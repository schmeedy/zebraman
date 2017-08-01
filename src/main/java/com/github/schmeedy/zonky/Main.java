package com.github.schmeedy.zonky;

import com.github.schmeedy.zonky.java.JavaMain;
import com.github.schmeedy.zonky.scala.ScalaMain$;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0 || !args[0].equals("-scala")) {
            System.out.println("running Java version");
            JavaMain.main(new String[0]);
        } else {
            System.out.println("running Scala version");
            ScalaMain$.MODULE$.main(new String[0]);
        }
    }
}
