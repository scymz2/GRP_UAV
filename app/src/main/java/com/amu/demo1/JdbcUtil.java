package com.amu.demo1;

import java.sql.Connection;
import java.sql.DriverManager;

public class JdbcUtil {

    private static JdbcUtil instance;

    public static JdbcUtil getInstance(){
        if(instance == null){
            instance = new JdbcUtil();
        }
        return instance;
    }

    /**
     * set up the connection with database
     */
    public Connection getConnection(){
        try{
            Class.forName("com.mysql.jdbc.Driver");

            return DriverManager.getConnection("jdbc:mysql://120.78.198.16:3306/summer?useSSL=false","root","grp05");
        } catch (Exception e){
            return null;
        }
    }



}
