package serverStarter;

import java.io.File;
import java.util.LinkedList;

import java.net.Socket;

import firewallListener.FirewallListener;

public class ServerInitializer {

    public static void main (String[] args) {
        int firewallPort = 30000;
        int webPort = 20000;

        System.out.println("Firewall, Web Listener now operate.");
        System.out.println("Firewall Listener Port: " + Integer.toString(firewallPort));
        System.out.println("Web Listener Port: " + Integer.toString(webPort));

        FirewallListener firewallListener = new FirewallListener(firewallPort);
        firewallListener.startServer();
        /*
        Thread firewallListenerThread = new Thread() {
            public void run() {
                FirewallListener firewallListener = new FirewallListener(firewallPort);
                firewallListener.startServer();
            }
        };
        firewallListenerThread.start();

        WebListener webListener = new WebListener();
        webListener.startServer();
        */
    }
}
