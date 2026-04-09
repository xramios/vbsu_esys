package com.group5.paul_esys.modules.config;

public class Config {

  // Dito yung mga credentials para sa db connection
  public static final String DB_NAME = "university_db";
  public static final String DB_URL = "jdbc:derby://localhost:1527/" + DB_NAME;
  public static final String DB_SCHEMA = "APP";
  public static final String DB_USER = "app";
  public static final String DB_PASS = "derby";
  public static final ConnectionType CONNECTION_TYPE = ConnectionType.DERBY;
}