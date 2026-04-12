package com.group5.paul_esys.modules.users.models.user;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.BCrypt.Result;
import at.favre.lib.crypto.bcrypt.BCrypt.Verifyer;
import com.group5.paul_esys.modules.admin.model.Admin;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.registrar.model.Registrar;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.users.models.enums.Role;

public class UserInformation<T> {
    private Long id;
    private String email;
    private String password;
    private Role role;
    private T user; // Can either be Student, Registrar, and Faculty

    public boolean verifyPassword(String plainTextPassword) {
        Verifyer verifyer = BCrypt.verifyer();
        Result result = verifyer.verify(plainTextPassword.toCharArray(), this.password);
        return result.verified;
    }

    public Role getAccountType() {
        if (user instanceof Student) {
            return Role.STUDENT;
        } else if (user instanceof Registrar) {
            return Role.REGISTRAR;
        } else if (user instanceof Faculty) {
            return Role.FACULTY;
        } else if (user instanceof Admin) {
            return Role.ADMIN;
        }

        return null;
    }

    public UserInformation() {
    }

    public UserInformation(Long id, String email, String password, Role role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public T getUser() {
        return user;
    }

    public UserInformation<T> setUser(T user) {
        this.user = user;
        return this;
    }

    public Long getId() {
        return id;
    }

    public UserInformation<T> setId(Long id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserInformation<T> setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserInformation<T> setPassword(String password) {
        this.password = password;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public UserInformation<T> setRole(Role role) {
        this.role = role;
        return this;
    }
}
