package org.ClassicGames.services;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.ClassicGames.exceptions.*;
import org.ClassicGames.model.User;
import org.dizitart.no2.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

import static org.ClassicGames.services.FileSystemService.getPathToFile;

public class UserService {

    private static ObjectRepository<User> userRepository;

    public static void initDatabase() {
        FileSystemService.initDirectory();
        Nitrite database = Nitrite.builder()
                .filePath(getPathToFile("ClassicGames.db").toFile())
                .openOrCreate("admin", "admin");

        userRepository = database.getRepository(User.class);
    }

    public static void addUser(String username, String password, String role) throws UsernameAlreadyExistsException,
                                                                                    BlankFieldException{
        checkFieldsAreNotBlank(username, password, role);
        checkUserDoesNotAlreadyExist(username);
        userRepository.insert(new User(username, encodePassword(username, password), role));
    }

    public static List<User> getAllUsers() {
        return userRepository.find().toList();
    }

    public static void logIn(String username, String password, String role) throws LogInFailException,
                                                                                    BlankFieldException{
        checkFieldsAreNotBlank(username, password, role);
        validateUser(username, encodePassword(username, password), role);

    }

    public static void validateUser(String username, String password, String role) throws LogInFailException {
        for(User user : userRepository.find()) {
            if(Objects.equals(username, user.getUsername())){
                if(!(Objects.equals(password, user.getPassword()) && Objects.equals(role, user.getRole())))
                    throw new LogInFailException();

                return;
            }
        }

        throw new LogInFailException();
    }

    public static void checkFieldsAreNotBlank(String username, String password, String role) throws BlankFieldException {
        if(StringUtils.isNullOrEmpty(username) || StringUtils.isNullOrEmpty(password) || StringUtils.isNullOrEmpty(role))
            throw new BlankFieldException();
    }

    private static void checkUserDoesNotAlreadyExist(String username) throws UsernameAlreadyExistsException {
        for (User user : userRepository.find()) {
            if (Objects.equals(username, user.getUsername()))
                throw new UsernameAlreadyExistsException(username);
        }
    }

    public static String encodePassword(String salt, String password) {
        MessageDigest md = getMessageDigest();
        md.update(salt.getBytes(StandardCharsets.UTF_8));

        byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));

        // This is the way a password should be encoded when checking the credentials
        return new String(hashedPassword, StandardCharsets.UTF_8)
                .replace("\"", ""); //to be able to save in JSON format
    }

    private static MessageDigest getMessageDigest() {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 does not exist!");
        }
        return md;
    }

}
