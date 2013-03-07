/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package technoor.service.gftuk;

import static gft.api.example.ApiUsage.LOGIN_PROP_NAME;
import static gft.api.example.ApiUsage.PASSWORD_PROP_NAME;
import static gft.api.example.ApiUsage.URL_PROP_NAME;
import technoor.service.IPricingService;

/**
 *
 * @author salman
 */
public final class GFTUKLogin implements IPricingService {

    final String url = System.getProperty(URL_PROP_NAME);
    final String login = System.getProperty(LOGIN_PROP_NAME);
    final String password = System.getProperty(PASSWORD_PROP_NAME);

    @Override
    public LoginProperty getLoginProperty() {

        if (url == null) {
            System.out.println("\nError: " + URL_PROP_NAME + " property not set");
            return null;
        }
        if (login == null) {
            System.out.println("\nError: " + LOGIN_PROP_NAME + " property not set");
            return null;
        }

        if (password == null) {
            System.out.println("\nError: " + PASSWORD_PROP_NAME + " property not set");
            return null;
        }
        
        return new LoginProperty(url, login, password);
    }
}
