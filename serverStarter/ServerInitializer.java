package serverStarter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import firewallListener.FirewallListener;

public class ServerInitializer {

    public static void main (String[] args) {
        if (args.length != 1)
            System.exit(1);

        int firewallPort = 30000;
        int webPort = 20000;
        String serverName = args[0];

        if ("firewall".equals(serverName)) {
            FirewallListener firewallListener = new FirewallListener(firewallPort);

            firewallListener.startServer();
        }
        else if ("web".equals(serverName)) {
        }
        else {
            System.out.println("Usage\n\t java ServerInitializer firewall");
            System.out.println("\t java ServerInitializer web\n");
            System.exit(1);
        }
    }
}
