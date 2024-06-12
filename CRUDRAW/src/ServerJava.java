//imports
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;


public class ServerJava {

    public static void main(String[] args) throws IOException{

        HttpServer server = HttpServer.create(new InetSocketAddress(3072),0);
        server.createContext("/datos",new readHandler());
        server.createContext("/crear",new createHandler());
        server.createContext("/borrar",new deleteHandler());
        server.createContext("/actualizar", new updateHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("server empezado en puerto 3072");
        String result = getDatabaseData();
        

    }

        //GET
    static class readHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange t) throws IOException{
            if("GET".equals(t.getRequestMethod())){
                String response = getDatabaseData();
                t.sendResponseHeaders( 200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }else{
                t.sendResponseHeaders(405, -1);
            }
            
        }

    }
    private static String getDatabaseData(){
        StringBuilder result = new StringBuilder();
        try(Connection con = Conexion.getConnection()){
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("Select * from persons");
            while(rs.next()){
                result.append("ID: ").append(rs.getInt("personid")).append(", Name: ").append(rs.getString("firstname")).append(", City:").append(rs.getString("city")).append("\n");
            } 
            
        }catch(SQLException e){
            result.append("Error: ").append(e.getMessage());
        }

        return result.toString();
    }
        
        //POST
    static class createHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange t)throws IOException{
            if("POST".equals(t.getRequestMethod())){
                Map<String, String> params = queryToMap(t.getRequestURI().getQuery());

                Integer personid = null;
                String firstname = null;
                String city = null;

                try {
                    personid = Integer.parseInt(params.get("personid"));
                } catch (NumberFormatException e) {
                    personid = null;
                }

                firstname = params.get("firstname");
                city = params.get("city");

                



                String response;
                if (personid != null && firstname != null && !firstname.isEmpty() && city != null && !city.isEmpty()) {
                    if (createDatabaseEntry(personid, firstname, city)) {
                        response = "Entry created successfully.";
                        t.sendResponseHeaders(201, response.length());
                    } else {
                        response = "Failed to create entry.";
                        t.sendResponseHeaders(500, response.length());
                    }
                } else {
                    response = "Invalid input.";
                    t.sendResponseHeaders(400, response.length());
                }

                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                t.sendResponseHeaders(405, -1); // Method Not Allowed
            }


            }
        }
        private static boolean createDatabaseEntry(Integer personid,String firstname,String city){
            try (Connection connection = Conexion.getConnection()) {
                String sql = "INSERT INTO persons (personid, firstname, city) VALUES (?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setInt(1, personid);
                pstmt.setString(2, firstname);
                pstmt.setString(3, city);
                int affectedRows = pstmt.executeUpdate();
                return affectedRows > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        private static Map<String, String> queryToMap(String query) {
            Map<String, String> result = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1].replace("\"", ""));
                } else {
                    result.put(entry[0], "");
                }
            }
            return result;
        }

    }

        //DELETE
    class deleteHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange t) throws IOException {
            if("DELETE".equals(t.getRequestMethod())){
                Integer personid= null;
                Map<String,String> params =getQueryMap(t.getRequestURI().getQuery());

                try{
                    personid=Integer.parseInt(params.get("personid"));
                }catch(NumberFormatException e){
                    personid=null;
                }
                String response;
                if (personid != null) {
                    if (deleteDatabaaseEntry(personid)) {
                        response = "Entry deleted successfully.";
                        t.sendResponseHeaders(201, response.length());
                    } else {
                        
                        response = "Failed to delete entry.";
                        t.sendResponseHeaders(500, response.length());
                    }
                } else {
                    response = "Invalid input.";
                    t.sendResponseHeaders(400, response.length());
                }
                
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();

            }
        }

        private static boolean deleteDatabaaseEntry(Integer personid){
            try(Connection connection = Conexion.getConnection()){
                String sql="DELETE FROM persons WHERE personid=?";
                PreparedStatement pst = connection.prepareStatement(sql);
                pst.setInt(1,personid);

                int affectedRows = pst.executeUpdate();
                return affectedRows>0;
            }catch(SQLException e){
                e.printStackTrace();
                return false;

            }
            
            
        }


        public static Map<String, String> getQueryMap(String query) {  
            String[] params = query.split("&");  
            Map<String, String> map = new HashMap<String, String>();
        
            for (String param : params) {  
                String name = param.split("=")[0];  
                String value = param.split("=")[1];  
                map.put(name, value);  
            }  
            return map;  
        }
    }

        //PUT Y PATCH
    class updateHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange t) throws IOException{
          if("PUT".equals(t.getRequestMethod())){
            Map<String,String> params =getQueryMap(t.getRequestURI().getQuery());
            Integer personid=null;
            String firstname = null;
            String city = null;
            
            try{
                personid = Integer.parseInt(params.get("personid"));
            }catch(NumberFormatException e){
                personid=null;
            }

            firstname = params.get("firstname");
            city = params.get("city");

            String response;
            if(personid!=null && firstname!=null && !firstname.isEmpty() && city!=null && !city.isEmpty()){
                if(putDatabaseEntry(personid, firstname, city)){
                    response = "Entry updated succesfully";
                    t.sendResponseHeaders(200, response.length());
                }else{
                    response = "Failed to update entry";
                    t.sendResponseHeaders(500, response.length());
                }
            }
            else{
                response="Invalid input";
                t.sendResponseHeaders(400, response.length());
            }

            OutputStream os =  t.getResponseBody();
            os.write(response.getBytes());
            os.close();
          }

          if("PATCH".equals(t.getRequestMethod())){
            Map<String,String> params = getQueryMap(t.getRequestURI().getQuery());
            Integer personid=null;
            String firstname = null;
            String city = null;
            
            try{
                personid= Integer.parseInt((params.get("personid")));
            }catch(NumberFormatException e){
                personid=null;
            }
            firstname= params.get("firstname");
            city = params.get("city");

            String response;
            
            if(personid!=null){
                if(patchDatabaseEntry(personid, firstname, city)){
                    response= "Entry modified succesfully.";
                    t.sendResponseHeaders(200, response.length());
                }else{
                    response="Failed to modify entry.";
                    t.sendResponseHeaders(500, response.length());
                }
            }else{
                response="Invalid input";
                t.sendResponseHeaders(400, response.length());
            }

            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
          }else{
            t.sendResponseHeaders(405, -1);
          }

        }
        private static boolean putDatabaseEntry(Integer personid, String firstname, String city){
            //este metodo debe actualizar un recurso completo, y en caso de que el recurso no exista, crearlo
            

            try(Connection connection = Conexion.getConnection()){
                String sql="select COUNT(*) from persons where personid= ?";
                PreparedStatement pst = connection.prepareStatement(sql);
                pst.setInt(1,personid);
                ResultSet rs = pst.executeQuery();

                if(rs.next() && rs.getInt(1)>0){
                    sql = "Update persons SET firstname =?, city = ? WHere personid=?";
                    PreparedStatement pstmt = connection.prepareStatement(sql);
                    pstmt.setString(1, firstname);
                    pstmt.setString(2, city);
                    pstmt.setInt(3, personid);
                    int affectedRows = pstmt.executeUpdate();
                    return affectedRows>0;
                }
                else{
                    sql = "INSERT INTO persons (personid, firstname, city) VALUES (?, ?, ?)";
                    PreparedStatement pstmt = connection.prepareStatement(sql);
                    pstmt.setInt(1, personid);
                    pstmt.setString(2, firstname);
                    pstmt.setString(3,city);
                    int affectedRows= pstmt.executeUpdate();
                    return affectedRows>0;
                }
            }catch(SQLException e){
                e.printStackTrace();
                return false;
            }

        }
        private static boolean patchDatabaseEntry(Integer personid, String firstname, String city){
            //este metodo debe solo modificar el parametro enviado, es decir, si busco la persona de id 5, y la quiero modificar la ciudad, debo solo mandar la ciudad
            try(Connection connection = Conexion.getConnection()){
                StringBuilder sql = new StringBuilder("UPDATE persons SET ");
                boolean needComma=false;

                if(firstname !=null && !firstname.isEmpty()){
                    sql.append("firstname = ?");
                    needComma=true;
                }
                if(city!=null && !city.isEmpty()){
                    if(needComma){
                        sql.append(", ");
                    }
                    sql.append("city=?");
                }
                sql.append(" WHERE personid=?");
                
                PreparedStatement pst = connection.prepareStatement(sql.toString());
                
                int index = 1;
                if(firstname!= null && !firstname.isEmpty()){
                    pst.setString(index, firstname);
                    index++;
                    
                }
                if(city!=null && !city.isEmpty()){
                    pst.setString(index, city);
                    index++;
                    
                }

                pst.setInt(index, personid);
                int affectedRows = pst.executeUpdate();

                return affectedRows>0;
            }catch(SQLException e){
                e.printStackTrace();
                return false;
            }
            


        }
        public static Map<String,String> getQueryMap(String query){
            String []params= query.split("&");
            Map<String, String> map = new HashMap<String,String>();
            for (String param : params){
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name,value);
            }


            return map;
        }
    }


