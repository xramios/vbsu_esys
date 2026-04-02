package com.group5.paul_esys.modules.users.models.user;

public class LoginData {

  private String email;
  private String password;

  public LoginData(String email, String password){
	  this.email = email;
	  this.password = password;
  }

  public boolean isValid() {
    return email != null && password != null;
  }

  public String getEmail() {
    return email;
  }

  public LoginData setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public LoginData setPassword(String password) {
    this.password = password;
    return this;
  }
}
