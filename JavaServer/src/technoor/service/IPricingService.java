/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package technoor.service;


/**
 *
 * @author salman
 */
public interface IPricingService {
    public class LoginProperty{
        String url;
        String login;
        String password;
        
        
        public LoginProperty(String url, String login, String password){
            
            this.url=url;
            this.login=login;
            this.password=password;
        }

        public String getUrl() {
            return url;
        }

        public String getLogin() {
            return login;
        }

        public String getPwd() {
            return password;
        }
    }
    
    public LoginProperty getLoginProperty();
}
