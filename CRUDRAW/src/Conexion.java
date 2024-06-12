import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Conexion {

        private static final String url="YourDatabase"; //url para entrar a la base de datos
        private static final String username = "YourUser";
        private static final String password = "yourPass"; //db username & pass;
    public static Connection getConnection() throws SQLException{
        
        //conexion
        return DriverManager.getConnection(url, username, password);
    
        
    }
}